package com.browserstack;

import com.browserstack.webdriver.config.Platform;
import io.cucumber.core.gherkin.Feature;
import io.cucumber.core.gherkin.Pickle;


public final class Execution {

    private final Platform platform;
    private final Feature feature;
    private final Pickle pickle;

    public Execution(Platform platform, Feature feature, Pickle pickle) {
        this.platform = platform;
        this.feature = feature;
        this.pickle = pickle;
    }

    public Platform getPlatform() {
        return platform;
    }

    public Feature getFeature() {
        return feature;
    }

    public Pickle getPickle() {
        return pickle;
    }

}
