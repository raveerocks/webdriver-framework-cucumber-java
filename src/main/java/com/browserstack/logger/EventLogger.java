package com.browserstack.logger;

import com.browserstack.*;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventLogger implements ConcurrentEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventLogger.class);

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(BuildStarted.class,this::logBuild);
        publisher.registerHandlerFor(RuntimeCreated.class,this::logRuntimeCreation);
        publisher.registerHandlerFor(BatchExecutionStarted.class,this::logBatchStart);
        publisher.registerHandlerFor(ExecutionStarted.class, this::logExecutionStart);
        publisher.registerHandlerFor(WebDriverCreated.class, this::logWebDriverCreation);
        publisher.registerHandlerFor(ExecutionCompleted.class,this::logExecutionCompletion);
        publisher.registerHandlerFor(BatchExecutionCompleted.class,this::logBatchCompletion);
        publisher.registerHandlerFor(BuildCompleted.class, this::logBuildCompletion);
    }

    private void logBuild(BuildStarted buildStarted) {
        LOGGER.debug("Build started at {}.",buildStarted.getInstant());
    }

    private void logRuntimeCreation(RuntimeCreated runtimeCreated) {
        LOGGER.debug("Successfully created runtime at {}.",runtimeCreated.getInstant());
    }

    private void logBatchStart(BatchExecutionStarted batchExecutionStarted) {
        LOGGER.debug("Batch {} started at {}.",batchExecutionStarted.getBatch(),batchExecutionStarted.getInstant());
    }

    private void logExecutionStart(ExecutionStarted executionStarted) {
        LOGGER.debug("Execution started at {}.",executionStarted.getInstant());
    }

    private void logWebDriverCreation(WebDriverCreated webDriverCreated) {
        LOGGER.debug("WebDriver created at {}.",webDriverCreated.getInstant());
    }

    private void logExecutionCompletion(ExecutionCompleted executionCompleted) {
        LOGGER.debug("Execution completed at {}.",executionCompleted.getInstant());
    }

    private void logBatchCompletion(BatchExecutionCompleted batchExecutionCompleted) {
        LOGGER.debug("Batch {} started at {}.",batchExecutionCompleted.getBatch(),batchExecutionCompleted.getInstant());
    }

    private void logBuildCompletion(BuildCompleted buildCompleted) {
        LOGGER.debug("Build Completed at {}.",buildCompleted.getInstant());
    }
}
