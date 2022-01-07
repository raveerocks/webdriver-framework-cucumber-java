package com.browserstack.report;

import com.browserstack.*;
import com.browserstack.report.builder.CustomReportBuilder;
import com.browserstack.report.models.Feature;
import com.browserstack.report.models.ModelUtil;
import com.browserstack.report.models.Scenario;
import com.browserstack.report.models.Step;
import com.browserstack.webdriver.core.WebDriverFactory;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestStepFinished;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomReportListener implements ConcurrentEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomReportBuilder.class);
    private static Map<String, List<Scenario>> scenarioMap = new HashMap<>();
    private static Map<String, String> featureNames =new HashMap<>();
    private static RuntimeOptions runtimeOptions;
    private static int batch=0;
    private static Instant buildStart;
    private static Instant buildEnd;

    private String reportPath;
    private ThreadLocal<Execution> currentExecution=new ThreadLocal<>();
    private ThreadLocal<List<Step>> currentSteps = new ThreadLocal<>();

    public CustomReportListener(String reportPath) {
        this.reportPath = reportPath;
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(BuildStarted.class, this::recordBuildStart);
        publisher.registerHandlerFor(RuntimeCreated.class, this::handleRuntimeCreatedEvent);
        publisher.registerHandlerFor(BatchExecutionStarted.class, this::recordCurrentBatch);
        publisher.registerHandlerFor(ExecutionStarted.class, this::recordFeatures);
        publisher.registerHandlerFor(TestStepFinished.class, this::recordStep);
        publisher.registerHandlerFor(TestCaseFinished.class, this::recordTestCase);
        publisher.registerHandlerFor(BuildCompleted.class, this::generateReports);
    }

    private void recordBuildStart(BuildStarted buildStarted) {
        this.buildStart = buildStarted.getInstant();
    }

    private void handleRuntimeCreatedEvent(RuntimeCreated runtimeCreated) {
        runtimeOptions = runtimeCreated.getRuntimeOptions();
    }

    private void recordCurrentBatch(BatchExecutionStarted batchExecutionStarted) {
        batch = batchExecutionStarted.getBatch();
    }

    private void recordFeatures(ExecutionStarted executionStarted) {
        Execution execution = executionStarted.getExecution();
        currentExecution.set(execution);
        featureNames.putIfAbsent(execution.getFeature().getUri().toString(), execution.getFeature().getName().get());
    }

    private void recordStep(TestStepFinished testStepFinished) {
        if (currentSteps.get()==null){
            currentSteps.set(new ArrayList<>());
        }
        currentSteps.get().add(ModelUtil.convertStep(testStepFinished));
    }

    private void recordTestCase(TestCaseFinished testCaseFinished) {
        Scenario scenario = ModelUtil.convertScenario(batch, currentExecution.get());
        scenario.getSteps().addAll(currentSteps.get());
        scenarioMap.putIfAbsent(currentExecution.get().getPlatform().getName(), new ArrayList<>());
        scenarioMap.get(currentExecution.get().getPlatform().getName()).add(scenario);
        currentSteps.remove();
    }

    private void generateReports(BuildCompleted buildCompleted) {
        buildEnd = buildCompleted.getInstant();
        buildReport();
    }

    public void buildReport() {
        Map<String, Report> reports = new HashMap<>();
        WebDriverFactory.getInstance().getPlatforms().forEach(platform -> {
            reports.put(platform.getName(),new Report());
        });
        scenarioMap.forEach((platform, scenarios) -> {
            scenarios.stream().forEach(scenario -> {
                Report platFormReport = reports.get(platform);

                if (scenario.passed()){
                    platFormReport.addPassed();
                }
                else{
                    platFormReport.addFailed();
                }
                if (scenario.isRerun()){
                    platFormReport.addRerun();
                }
                platFormReport.addScenario(scenario);
            });
        });
        try {
            reports.forEach((platform,report)->{
                RunTimeInfo runTimeInfo = new RunTimeInfo(report.total,
                        report.passed,
                        report.failed,
                        report.rerun,Duration.between(buildStart,buildEnd).toMillis(),runtimeOptions);
                try {
                    CustomReportBuilder.createReport(reportPath,platform, runTimeInfo, report.getFeatures());
                } catch (IOException e) {
                    LOGGER.error("Report Generation Failed", e);
                }
            });
        } finally {
            LOGGER.info("Report Generation Completed");
        }
    }

    private static class Report{

        private int total;
        private int passed;
        private int failed;
        private int rerun;
        Map<String,Feature> features;

        public Report() {
            total=0;
            passed=0;
            failed=0;
            rerun=0;
            features = new HashMap<>();
        }

        public void addPassed(){
            ++passed;
            ++total;
        }
        public void addFailed(){
            ++failed;
            ++total;
        }
        public void addRerun(){
            ++rerun;
        }

        public void addScenario(Scenario scenario){
            if (!features.containsKey(scenario.getFeatureId())){
                Feature feature = new Feature(scenario.getFeatureId(),featureNames.get(scenario.getFeatureId()),scenario.getFeatureId());
                features.put(feature.getFeatureId(),feature);
            }
            features.get(scenario.getFeatureId()).addScenario(scenario);
        }

        public List<Feature> getFeatures(){
            return new ArrayList(features.values());
        }
    }
}
