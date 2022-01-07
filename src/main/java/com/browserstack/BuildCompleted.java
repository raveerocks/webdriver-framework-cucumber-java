package com.browserstack;

import io.cucumber.plugin.event.Event;

import java.time.Instant;
import java.util.Objects;

public class BuildCompleted implements Event {

    private final Instant instant;

    BuildCompleted(Instant timeInstant) {
        this.instant = Objects.requireNonNull(timeInstant);
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

}
