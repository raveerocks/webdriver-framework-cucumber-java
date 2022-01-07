package com.browserstack.rerun;

import com.browserstack.*;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestCaseFinished;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class RerunExecutionManager implements ConcurrentEventListener {

    private static List<Execution> failedExecutions = new ArrayList<>();
    private final ThreadLocal<Execution> executionThreadLocal;
    private final int maximumRepetition;
    private int currentRepetition = 0;

    public RerunExecutionManager(String maximumRepetition) {
        this.executionThreadLocal = new ThreadLocal<>();
        this.maximumRepetition = Integer.parseInt(maximumRepetition);
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(ExecutionStarted.class, this::grabExecution);
        publisher.registerHandlerFor(TestCaseFinished.class, this::pushFailedTests);
        publisher.registerHandlerFor(BatchExecutionCompleted.class, this::fireFailedTests);
    }

    private void grabExecution(ExecutionStarted executionStarted) {
        Objects.requireNonNull(executionStarted.getExecution());
        executionThreadLocal.set(executionStarted.getExecution());
    }

    private void pushFailedTests(TestCaseFinished testCaseFinished) {
        if (!testCaseFinished.getResult().getStatus().isOk()) {
            failedExecutions.add(executionThreadLocal.get());
        }
    }

    private void fireFailedTests(BatchExecutionCompleted batchExecutionCompleted) {
        if (++currentRepetition < maximumRepetition && failedExecutions.size() > 0) {
            List<Execution> newExecutions = new ArrayList<>();
            failedExecutions.forEach(failedExecution -> newExecutions.add(new Execution(failedExecution.getPlatform(),
                    failedExecution.getFeature(),
                    failedExecution.getPickle())));
            BatchExecutionRunner.getInstance().submitExecutions(currentRepetition, newExecutions);
            failedExecutions = new ArrayList<>();
        } else {
            BatchExecutionRunner.getInstance().closeExecutionContext();
        }
    }
}
