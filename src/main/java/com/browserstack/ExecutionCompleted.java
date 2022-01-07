package com.browserstack;

import io.cucumber.plugin.event.Event;
import org.openqa.selenium.WebDriver;

import java.time.Instant;
import java.util.Objects;

public final class ExecutionCompleted implements Event {

    private final Instant instant;
    private final Execution execution;
    private final WebDriver webDriver;

    ExecutionCompleted(Instant timeInstant, Execution execution, WebDriver webDriver) {
        this.instant = Objects.requireNonNull(timeInstant);
        this.execution = execution;
        this.webDriver = webDriver;
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    public Execution getExecution() {
        return execution;
    }

    public WebDriver getWebDriver() {
        return webDriver;
    }
}
