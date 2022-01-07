package com.browserstack;

import com.browserstack.webdriver.core.WebDriverFactory;
import io.cucumber.core.eventbus.EventBus;
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

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

final class WebDriverRuntime {

    private final EventBus eventBus;
    private final WebDriverFactory webDriverFactory = WebDriverFactory.getInstance();
    private final Predicate<Pickle> filter;
    private final int limit;
    private final FeatureSupplier featureSupplier;
    private final PickleOrder pickleOrder;
    private final CucumberExecutionContext context;
    private final BatchExecutionRunner batchExecutionRunner;
    private final boolean isRerunEnabled;

    private WebDriverRuntime(EventBus eventBus, CucumberExecutionContext context, Predicate<Pickle> filter, int limit, FeatureSupplier featureSupplier, PickleOrder pickleOrder, BatchExecutionRunner batchExecutionRunner, boolean isRerunEnabled, RuntimeOptions runtimeOptions) {
        this.eventBus = eventBus;
        this.context = context;
        this.filter = filter;
        this.limit = limit;
        this.featureSupplier = featureSupplier;
        this.pickleOrder = pickleOrder;
        this.batchExecutionRunner = batchExecutionRunner;
        this.isRerunEnabled = isRerunEnabled;
        eventBus.send(new RuntimeCreated(Instant.now(), runtimeOptions));
    }

    public static WebDriverRuntime.Builder builder() {
        return new WebDriverRuntime.Builder();
    }

    public void run() {

        eventBus.send(new BuildStarted(Instant.now()));

        // Feature Scanning
        this.context.startTestRun();
        List<Feature> features = this.featureSupplier.get();
        CucumberExecutionContext cucumberExecutionContext = this.context;
        Objects.requireNonNull(cucumberExecutionContext);
        features.forEach(cucumberExecutionContext::beforeFeature);
        List<Execution> executions = new ArrayList<>();
        features.stream()
                .flatMap((feature) -> {
                    List<Pickle> pickles = feature.getPickles().stream()
                            .filter(this.filter)
                            .collect(Collectors.collectingAndThen(Collectors.toList(), (list) -> this.pickleOrder.orderPickles(list)))
                            .stream().limit(this.limit > 0 ? (long) this.limit : 2147483647L)
                            .collect(Collectors.toList());
                    return pickles.stream().map(pickle -> new PickleFeature(pickle, feature));
                })
                .forEach(pickleFeature -> webDriverFactory.getPlatforms().forEach(platform -> executions.add(new Execution(platform, pickleFeature.getFeature(), pickleFeature.getPickle()))));

        // Pickle execution
        batchExecutionRunner.submitExecutions(0, executions);

        // Context closure for single execution
        if (!isRerunEnabled) {
            batchExecutionRunner.closeExecutionContext();
        }

    }

    public static class Builder {
        private EventBus eventBus;
        private Supplier<ClassLoader> classLoader;
        private RuntimeOptions runtimeOptions;
        private List<Plugin> additionalPlugins;
        private boolean isRerunEnabled;

        private Builder() {
            this.eventBus = new TimeServiceEventBus(Clock.systemUTC(), UUID::randomUUID);
            this.classLoader = ClassLoaders::getDefaultClassLoader;
            this.runtimeOptions = RuntimeOptions.defaultOptions();
            this.additionalPlugins = Collections.emptyList();
            this.isRerunEnabled = false;
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

        public WebDriverRuntime.Builder withRerunEnabled() {
            isRerunEnabled = true;
            return this;
        }

        public WebDriverRuntime.Builder withRerunDisabled() {
            isRerunEnabled = false;
            return this;
        }

        public WebDriverRuntime build() {
            ObjectFactoryServiceLoader objectFactoryServiceLoader = new ObjectFactoryServiceLoader(this.classLoader, this.runtimeOptions);
            ObjectFactorySupplier objectFactorySupplier = this.runtimeOptions.isMultiThreaded() ? new ThreadLocalObjectFactorySupplier(objectFactoryServiceLoader) : new SingletonObjectFactorySupplier(objectFactoryServiceLoader);
            BackendSupplier backendSupplier = new BackendServiceLoader(this.classLoader, objectFactorySupplier);
            Plugins plugins = new Plugins(new PluginFactory(), this.runtimeOptions);
            Iterator pluginIterator = this.additionalPlugins.iterator();
            while (pluginIterator.hasNext()) {
                Plugin plugin = (Plugin) pluginIterator.next();
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
            RunnerSupplier runnerSupplier = this.runtimeOptions.isMultiThreaded() ? new ThreadLocalRunnerSupplier(this.runtimeOptions, this.eventBus, backendSupplier, objectFactorySupplier, typeRegistryConfigurerSupplier) : new SingletonRunnerSupplier(this.runtimeOptions, this.eventBus, (BackendSupplier) backendSupplier, (ObjectFactorySupplier) objectFactorySupplier, typeRegistryConfigurerSupplier);
            FeatureParser parser = new FeatureParser(eventBus::generateId);
            FeatureSupplier featureSupplier = new FeaturePathFeatureSupplier(this.classLoader, this.runtimeOptions, parser);
            Predicate<Pickle> filter = new Filters(this.runtimeOptions);
            int limit = this.runtimeOptions.getLimitCount();
            PickleOrder pickleOrder = this.runtimeOptions.getPickleOrder();
            CucumberExecutionContext context = new CucumberExecutionContext(this.eventBus, new ExitStatus(runtimeOptions), runnerSupplier);
            BatchExecutionRunner batchExecutionRunner = BatchExecutionRunner.createBatchExecutionRunner(eventBus, context, runtimeOptions);
            return new WebDriverRuntime(eventBus, context, filter, limit, featureSupplier, pickleOrder, batchExecutionRunner, isRerunEnabled, runtimeOptions);
        }
    }

    public static class PickleFeature {

        private final Pickle pickle;
        private final Feature feature;

        public PickleFeature(Pickle pickle, Feature feature) {
            this.pickle = pickle;
            this.feature = feature;
        }

        public Pickle getPickle() {
            return pickle;
        }

        public Feature getFeature() {
            return feature;
        }
    }


}
