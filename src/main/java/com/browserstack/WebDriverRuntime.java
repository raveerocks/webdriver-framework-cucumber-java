package com.browserstack;

import com.browserstack.webdriver.config.Platform;
import com.browserstack.webdriver.core.WebDriverFactory;
import io.cucumber.core.eventbus.EventBus;
import io.cucumber.core.exception.CucumberException;
import io.cucumber.core.feature.FeatureParser;
import io.cucumber.core.filter.Filters;
import io.cucumber.core.gherkin.Feature;
import io.cucumber.core.gherkin.Pickle;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.core.order.PickleOrder;
import io.cucumber.core.plugin.PluginFactory;
import io.cucumber.core.plugin.Plugins;
import io.cucumber.core.resource.ClassLoaders;
import io.cucumber.core.runtime.*;
import io.cucumber.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class WebDriverRuntime {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebDriverRuntime.class);
    private final WebDriverFactory webDriverFactory = WebDriverFactory.getInstance();
    private final Predicate<Pickle> filter;
    private final int limit;
    private final FeatureSupplier featureSupplier;
    private final ExecutorService executor;
    private final PickleOrder pickleOrder;
    private final CucumberExecutionContext context;

    private WebDriverRuntime(CucumberExecutionContext context, Predicate<Pickle> filter, int limit, FeatureSupplier featureSupplier, ExecutorService executor, PickleOrder pickleOrder) {
        this.filter = filter;
        this.context = context;
        this.limit = limit;
        this.featureSupplier = featureSupplier;
        this.executor = executor;
        this.pickleOrder = pickleOrder;
    }

    public static WebDriverRuntime.Builder builder() {
        return new WebDriverRuntime.Builder();
    }

    public void run() {
        this.context.startTestRun();
        List<Feature> features = this.featureSupplier.get();
        CucumberExecutionContext cucumberExecutionContext = this.context;
        Objects.requireNonNull(cucumberExecutionContext);
        features.forEach(cucumberExecutionContext::beforeFeature);
        List<Future<?>> executingPickles = new ArrayList<>();
        ((Stream) features.stream()
                                .flatMap((feature) -> feature.getPickles().stream())
                                .filter(this.filter)
                                .collect(Collectors.collectingAndThen(Collectors.toList(), (list) -> this.pickleOrder.orderPickles(list).stream()))
                ).limit(this.limit > 0 ? (long)this.limit : 2147483647L)
                .forEach((pickle) -> webDriverFactory.getPlatforms().forEach(platform -> executingPickles.add(this.executor.submit(this.executeForPlatform(platform, (Pickle) pickle)))));
        this.executor.shutdown();
        Iterator picklesIterator = executingPickles.iterator();

        while(picklesIterator.hasNext()) {
            Future executingPickle = (Future)picklesIterator.next();
            try {
                executingPickle.get();
            } catch (ExecutionException executionException) {
                LOGGER.error("Exception while executing pickle",executionException);
            } catch (InterruptedException interruptedException) {
                this.executor.shutdownNow();
                LOGGER.debug("Interrupted while executing pickle",interruptedException);
            }
        }
        LOGGER.debug("Cucumber feature execution completed");

        this.context.finishTestRun();
        CucumberException exception = this.context.getException();
        if (exception != null) {
            throw exception;
        }
    }

    private Runnable executeForPlatform(Platform platform, Pickle pickle) {
        return () -> this.context.runTestCase((runner) -> {
            LOGGER.debug("Starting {} on {}",pickle.getName(),platform.getName());
            WebDriverSupplier.putWebDriver(webDriverFactory.createWebDriverForPlatform(platform,pickle.getName()));
            runner.runPickle(pickle);
            LOGGER.debug("Completed {} on {}",pickle.getName(),platform.getName());
        });
    }

    private static final class SameThreadExecutorService extends AbstractExecutorService {
        private SameThreadExecutorService() {
        }

        public void execute(Runnable command) {
            command.run();
        }

        public void shutdown() {
        }

        public List<Runnable> shutdownNow() {
            return Collections.emptyList();
        }

        public boolean isShutdown() {
            return true;
        }

        public boolean isTerminated() {
            return true;
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }
    }

    private static final class WebDriverThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        WebDriverThreadFactory() {
            this.namePrefix = "web-driver-runner-" + poolNumber.getAndIncrement() + "-thread-";
        }

        public Thread newThread(Runnable r) {
            return new Thread(r, this.namePrefix + this.threadNumber.getAndIncrement());
        }
    }

    public static class Builder {
        private EventBus eventBus;
        private Supplier<ClassLoader> classLoader;
        private RuntimeOptions runtimeOptions;
        private List<Plugin> additionalPlugins;

        private Builder() {
            this.eventBus = new TimeServiceEventBus(Clock.systemUTC(), UUID::randomUUID);
            this.classLoader = ClassLoaders::getDefaultClassLoader;
            this.runtimeOptions = RuntimeOptions.defaultOptions();
            this.additionalPlugins = Collections.emptyList();
        }

        public WebDriverRuntime.Builder withRuntimeOptions(RuntimeOptions runtimeOptions) {
            this.runtimeOptions = runtimeOptions;
            return this;
        }

        public WebDriverRuntime.Builder withClassLoader(Supplier<ClassLoader> classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public WebDriverRuntime.Builder withAdditionalPlugins(Plugin... plugins) {
            this.additionalPlugins = Arrays.asList(plugins);
            return this;
        }

        public WebDriverRuntime build() {
            ObjectFactoryServiceLoader objectFactoryServiceLoader = new ObjectFactoryServiceLoader(this.classLoader, this.runtimeOptions);
            ObjectFactorySupplier objectFactorySupplier = this.runtimeOptions.isMultiThreaded() ? new ThreadLocalObjectFactorySupplier(objectFactoryServiceLoader) : new SingletonObjectFactorySupplier(objectFactoryServiceLoader);
            BackendSupplier backendSupplier = new BackendServiceLoader(this.classLoader, objectFactorySupplier);
            Plugins plugins = new Plugins(new PluginFactory(), this.runtimeOptions);
            Iterator pluginIterator = this.additionalPlugins.iterator();
            while(pluginIterator.hasNext()) {
                Plugin plugin = (Plugin)pluginIterator.next();
                plugins.addPlugin(plugin);
            }
            ExitStatus exitStatus = new ExitStatus(this.runtimeOptions);
            plugins.addPlugin(exitStatus);
            if (this.runtimeOptions.isMultiThreaded()) {
                plugins.setSerialEventBusOnEventListenerPlugins(this.eventBus);
            } else {
                plugins.setEventBusOnEventListenerPlugins(this.eventBus);
            }
            EventBus eventBus = this.eventBus;
            TypeRegistryConfigurerSupplier typeRegistryConfigurerSupplier = new ScanningTypeRegistryConfigurerSupplier(this.classLoader, this.runtimeOptions);
            RunnerSupplier runnerSupplier = this.runtimeOptions.isMultiThreaded() ? new ThreadLocalRunnerSupplier(this.runtimeOptions, this.eventBus, backendSupplier, objectFactorySupplier, typeRegistryConfigurerSupplier) : new SingletonRunnerSupplier(this.runtimeOptions, this.eventBus, (BackendSupplier)backendSupplier, (ObjectFactorySupplier)objectFactorySupplier, typeRegistryConfigurerSupplier);
            ExecutorService executor = this.runtimeOptions.isMultiThreaded() ? Executors.newFixedThreadPool(this.runtimeOptions.getThreads(), new WebDriverThreadFactory()) : new WebDriverRuntime.SameThreadExecutorService();
            FeatureParser parser = new FeatureParser(eventBus::generateId);
            FeatureSupplier featureSupplier = new FeaturePathFeatureSupplier(this.classLoader, this.runtimeOptions, parser);
            Predicate<Pickle> filter = new Filters(this.runtimeOptions);
            int limit = this.runtimeOptions.getLimitCount();
            PickleOrder pickleOrder = this.runtimeOptions.getPickleOrder();
            CucumberExecutionContext context = new CucumberExecutionContext(this.eventBus, new ExitStatus(runtimeOptions), runnerSupplier);
            return new WebDriverRuntime(context, filter, limit, featureSupplier, executor, pickleOrder);
        }
    }

}
