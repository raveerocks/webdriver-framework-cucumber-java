package com.browserstack;

import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.plugin.event.Event;

import java.time.Instant;

public class RuntimeCreated implements Event {

    private final Instant instant;
    private final RuntimeOptions runtimeOptions;

    public RuntimeCreated(Instant instant, RuntimeOptions runtimeOptions) {
        this.instant = instant;
        this.runtimeOptions = runtimeOptions;
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    public RuntimeOptions getRuntimeOptions() {
        return runtimeOptions;
    }
}
