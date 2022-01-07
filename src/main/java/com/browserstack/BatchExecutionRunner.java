package com.browserstack;

import com.browserstack.webdriver.core.WebDriverFactory;
import io.cucumber.core.eventbus.EventBus;
import io.cucumber.core.exception.CucumberException;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.core.runtime.CucumberExecutionContext;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchExecutionRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchExecutionRunner.class);
    private static BatchExecutionRunner instance;
    private final EventBus eventBus;
    private final CucumberExecutionContext context;
    private final RuntimeOptions runtimeOptions;
    private final WebDriverFactory webDriverFactory = WebDriverFactory.getInstance();

    private BatchExecutionRunner(EventBus eventBus, CucumberExecutionContext context, RuntimeOptions runtimeOptions) {
        this.eventBus = eventBus;
        this.context = context;
        this.runtimeOptions = runtimeOptions;
    }

    public static BatchExecutionRunner createBatchExecutionRunner(EventBus eventBus, CucumberExecutionContext context, RuntimeOptions runtimeOptions) {
        if (instance == null) {
            synchronized (BatchExecutionRunner.class) {
                if (instance == null) {
                    instance = new BatchExecutionRunner(eventBus, context, runtimeOptions);
                }
            }
        }
        return instance;
    }

    public static BatchExecutionRunner getInstance() {
        if (instance == null) {
            throw new RuntimeException("Batch Executor initialisation failed");
        }
        return instance;
    }

    public void submitExecutions(int batch, List<Execution> executions) {
        eventBus.send(new BatchExecutionStarted(Instant.now(),batch));
        ExecutorService executor = this.runtimeOptions.isMultiThreaded() ? Executors.newFixedThreadPool(this.runtimeOptions.getThreads(), new WebDriverThreadFactory()) : new SameThreadExecutorService();
        List<Future<?>> executingPickles = new ArrayList<>();
        executions.forEach(execution -> executingPickles.add(executor.submit(executeForPlatform(execution))));

        //Execution submission Done
        executor.shutdown();

        // Waiting and iterating for pickle execution completion
        Iterator picklesIterator = executingPickles.iterator();
        while (picklesIterator.hasNext()) {
            Future executingPickle = (Future) picklesIterator.next();
            try {
                executingPickle.get();
            } catch (ExecutionException executionException) {
                LOGGER.error("Exception while executing pickle", executionException);
            } catch (InterruptedException interruptedException) {
                executor.shutdownNow();
                LOGGER.debug("Interrupted while executing pickle", interruptedException);
            }
        }
        eventBus.send(new BatchExecutionCompleted(Instant.now(), batch));
    }

    private Runnable executeForPlatform(Execution execution) {
        return () -> this.context.runTestCase((runner) -> {

            // Starting Execution
            eventBus.send(new ExecutionStarted(Instant.now(), execution));

            // WebDriver Creation
            WebDriver webDriver = webDriverFactory.createWebDriverForPlatform(execution.getPlatform(), execution.getPickle().getName());
            eventBus.send(new WebDriverCreated(Instant.now(), webDriver));

            // Pickle Execution
            runner.runPickle(execution.getPickle());

            // Completed Execution
            //LOGGER.debug("Completed {} on {}", execution.getPickle().getName(), e.getName());
            eventBus.send(new ExecutionCompleted(Instant.now(), execution, webDriver));
        });
    }

    public void closeExecutionContext() {
        try {
            this.context.finishTestRun();
            CucumberException exception = this.context.getException();
            if (exception != null) {
                throw exception;
            }
            LOGGER.debug("Cucumber feature execution completed");
        } finally {
            eventBus.send(new BuildCompleted(Instant.now()));
        }
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

}
