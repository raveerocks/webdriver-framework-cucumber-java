package com.browserstack;

import io.cucumber.plugin.event.Event;

import java.time.Instant;

public class BuildStarted implements Event {

    private Instant instant;

    public BuildStarted(Instant instant) {
        this.instant = instant;
    }

    @Override
    public Instant getInstant() {
        return instant;
    }
}
