package com.browserstack.report;

import io.cucumber.core.options.RuntimeOptions;

public class RunTimeInfo {

    private final int total;
    private final int passed;
    private final int failed;
    private final int rerun;
    private final long duration;
    private final int concurrency;
    private final String cucumberTags;
    private final String glue;
    private final String os;
    private final String osArch;
    private final String javaVersion;
    private final String cucumberVersion;


    public RunTimeInfo(int total, int passed, int failed, int rerun, long duration, RuntimeOptions runtimeOptions) {
        this.total = total;
        this.passed = passed;
        this.failed = failed;
        this.rerun = rerun;
        this.duration = duration;
        this.concurrency = runtimeOptions.getThreads();
        this.cucumberTags = runtimeOptions.getTagExpressions().toString();
        this.glue = runtimeOptions.getGlue().toString();
        this.os = System.getProperty("os.name");
        this.osArch = System.getProperty("os.arch");
        this.javaVersion = System.getProperty("java.version");
        this.cucumberVersion = System.getProperty("cucumber.version", "Not Defined");
    }

    public int getTotal() {
        return total;
    }

    public int getPassed() {
        return passed;
    }

    public int getFailed() {
        return failed;
    }

    public int getRerun() {
        return rerun;
    }

    public long getDuration() {
        return duration;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public String getCucumberTags() {
        return cucumberTags;
    }

    public String getGlue() {
        return glue;
    }

    public String getOs() {
        return os;
    }

    public String getOsArch() {
        return osArch;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getCucumberVersion() {
        return cucumberVersion;
    }
}
