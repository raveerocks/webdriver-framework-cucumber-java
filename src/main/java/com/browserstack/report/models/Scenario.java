package com.browserstack.report.models;

import java.util.ArrayList;
import java.util.List;

public class Scenario {

    private String scenarioId;
    private String featureId;
    private String name;
    private int line;
    private String keyword;
    private List<Step> steps = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();
    private int rerunIndex;

    public Scenario() {

    }

    public String getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(String scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public String getFeatureId() {
        return featureId;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public boolean isRerun() {
        return rerunIndex != 0;
    }

    public int getRerunIndex() {
        return rerunIndex;
    }

    public void setRerunIndex(int rerunIndex) {
        this.rerunIndex = rerunIndex;
    }

    public boolean passed() {
        return steps.stream().allMatch(Step::passed);
    }

}
