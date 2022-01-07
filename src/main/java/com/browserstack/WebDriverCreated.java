package com.browserstack;

import io.cucumber.plugin.event.Event;
import org.openqa.selenium.WebDriver;

import java.time.Instant;
import java.util.Objects;

public class WebDriverCreated implements Event {

    private final Instant instant;
    private final WebDriver webDriver;

    WebDriverCreated(Instant timeInstant, WebDriver webDriver) {
        this.instant = Objects.requireNonNull(timeInstant);
        this.webDriver = webDriver;
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    public WebDriver getWebDriver() {
        return webDriver;
    }
}
