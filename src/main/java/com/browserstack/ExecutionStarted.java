package com.browserstack;

import io.cucumber.plugin.event.Event;

import java.time.Instant;
import java.util.Objects;

public final class ExecutionStarted implements Event {

    private final Instant instant;
    private final Execution execution;

    ExecutionStarted(Instant timeInstant, Execution execution) {
        this.instant = Objects.requireNonNull(timeInstant);
        this.execution = execution;
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    public Execution getExecution() {
        return execution;
    }
}
