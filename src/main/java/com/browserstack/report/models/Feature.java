package com.browserstack.report.models;

import java.util.ArrayList;
import java.util.List;

public class Feature {

    private String featureId;
    private String name;
    private String uri;
    private List<Scenario> scenarios = new ArrayList<>();

    public Feature(String featureId, String name, String uri) {
        this.featureId = featureId;
        this.name = name;
        this.uri = uri;
    }

    public String getFeatureId() {
        return featureId;
    }

    public String getName() {
        return name;
    }

    public String getUri() {
        return uri;
    }


    public List<Scenario> getScenarios() {
        return scenarios;
    }

    public void addScenario(Scenario scenario) {
        this.scenarios.add(scenario);
    }

    public boolean passed() {
        return scenarios.stream().allMatch(Scenario::passed);
    }

}