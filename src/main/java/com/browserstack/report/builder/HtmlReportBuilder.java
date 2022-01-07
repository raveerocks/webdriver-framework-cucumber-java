package com.browserstack.report.builder;

import com.browserstack.report.models.Feature;
import com.browserstack.report.models.Result;
import com.browserstack.report.models.Scenario;
import com.browserstack.report.models.Step;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import io.cucumber.plugin.event.HookType;
import org.apache.commons.lang3.EnumUtils;

import java.io.*;
import java.util.*;
import java.util.function.Function;

public class HtmlReportBuilder {

    private static final String PASSED = "Passed";
    private static final String FAILED = "Failed";
    private static final String PASSED_AFTER_RERUN = "Passed in Rerun";
    private static final String FAILED_AFTER_RERUN = "Failed in Rerun";
    private static final String PRIMARY = "primary";
    private static final String INFO = "info";
    private static final String SUCCESS = "success";
    private static final String DANGER = "danger";
    private static final String WARNING = "warning";
    private static final String DATA_TARGET = "data_target";
    private static final String FEATURE_NAME = "feature_name";
    private static final String FEATURE_BADGE = "feature_badge";
    private static final String FEATURE_RESULT = "feature_result";
    private static final String FEATURE_SCENARIOS = "feature_scenarios";
    private static final String SCENARIO_NAME = "scenario_name";
    private static final String SCENARIO_BADGE = "scenario_badge";
    private static final String SCENARIO_RESULT = "scenario_result";
    private static final String SCENARIO_PLATFORM = "scenario_platform";
    private static final String SCENARIO_TAGS = "scenario_tags";
    private static final String RERUN_INDEX = "scenario_rerun_index";
    private static final String MODAL_TARGET = "modal_target";
    private static final String MODAL_HEADING = "modal_heading";
    private static final String MODAL_FEATURE_LINE = "modal_feature_line";
    private static final String MODAL_BODY = "modal_body";
    private static final String STEP_KEYWORD = "step_keyword";
    private static final String STEP_TYPE = "step_type";
    private static final String STEP_NAME = "step_name";
    private static final String STEP_DURATION = "step_duration";
    private static final String STEP_BADGE = "step_badge";
    private static final String STEP_RESULT = "step_result";
    private static final String STEP_DATATABLE = "step_datatable";
    private static final String DATATABLE = "datatable";
    private static final String STEP_EXCEPTION = "step_exception";
    private static final String EXCEPTION = "exception";
    private static final String STEP_OUTPUT = "step_output";
    private static final String OUTPUT = "output";
    private static final String STEP_EMBEDDING_TEXT = "step_embedding_text";
    private static final String TEXT = "text";
    private static final String STEP_EMBEDDING_IMAGE = "step_embedding_image";
    private static final String IMAGE_ID = "img_id";
    private static final String ROW_INFO = "row_info";
    private static final String TAG = "tag";
    private static final Function<Result, String> statusLabel = (result) -> result.getStatus().substring(0, 1).toUpperCase() + result.getStatus().substring(1);
    private static final Function<Result, String> statusBadge = (result) -> {
        String status = result.getStatus();
        return status.equalsIgnoreCase(PASSED) ? SUCCESS : status.equalsIgnoreCase(FAILED) ? DANGER : WARNING;
    };
    private final String reportPath;
    private final List<Feature> featureList;
    private final Mustache featureTemplate;
    private final Mustache modalTemplate;
    private final Mustache modalStepTemplate;
    private final Mustache modalEnvironmentTemplate;
    private final Mustache modalRowTemplate;
    private final Mustache scenarioTemplate;
    private final Mustache scenarioTagTemplate;

    private HtmlReportBuilder(String reportPath, List<Feature> featureList) {
        this.reportPath = reportPath;
        this.featureList = featureList;
        this.featureTemplate = readTemplate(String.format("%s/feature.mustache", ReporterConstants.MUSTACHE_TEMPLATES_DIR));
        this.modalTemplate = readTemplate(String.format("%s/modal.mustache", ReporterConstants.MUSTACHE_TEMPLATES_DIR));
        this.modalStepTemplate = readTemplate(String.format("%s/modal_step.mustache", ReporterConstants.MUSTACHE_TEMPLATES_DIR));
        this.modalEnvironmentTemplate = readTemplate(String.format("%s/modal_environment.mustache", ReporterConstants.MUSTACHE_TEMPLATES_DIR));
        this.modalRowTemplate = readTemplate(String.format("%s/modal_row.mustache", ReporterConstants.MUSTACHE_TEMPLATES_DIR));
        this.scenarioTemplate = readTemplate(String.format("%s/scenario.mustache", ReporterConstants.MUSTACHE_TEMPLATES_DIR));
        this.scenarioTagTemplate = readTemplate(String.format("%s/scenario_tag.mustache", ReporterConstants.MUSTACHE_TEMPLATES_DIR));
    }

    public static HtmlReportBuilder create(String reportPath, List<Feature> featureList) {
        return new HtmlReportBuilder(reportPath, featureList);
    }

    private static void addNestedMap(HashMap<String, Object> source, String sourceKey,
                                     String childKey, Object childValue) {

        HashMap<String, Object> map = new HashMap<>();
        map.put(childKey, childValue);

        source.put(sourceKey, map);
    }

    public List<String> getHtmlTableFeatureRows() {
        final List<String> featureRows = new ArrayList<>(featureList.size());
        featureList.forEach(feature -> featureRows.add(createFeatureRow(feature)));
        return featureRows;
    }

    public List<String> getHtmlModals() {
        final int modalCapacity = (int) featureList.stream().map(Feature::getScenarios).count() + 1;
        final List<String> modals = new ArrayList<>(modalCapacity);
        modals.add(createEnvironmentInfoModal());
        featureList
                .forEach(feature -> {
                    List<Scenario> scenarios = feature.getScenarios();
                    scenarios.forEach(scenario -> modals.add(createScenarioModal(feature, scenario)));
                });

        return modals;
    }

    private String createFeatureRow(Feature feature) {

        final LinkedHashMap<String, Object> featureData = new LinkedHashMap<>();

        String featureId = feature.getFeatureId();
        String featureName = feature.getName();
        String featureBadge = feature.passed() ? SUCCESS : DANGER;
        String featureResult = featureBadge.equals(SUCCESS) ? PASSED : FAILED;

        LinkedList<String> scenarioRows = new LinkedList<>();
        createScenarios(feature, scenarioRows);

        featureData.put(DATA_TARGET, featureId);
        featureData.put(FEATURE_NAME, featureName);
        featureData.put(FEATURE_BADGE, featureBadge);
        featureData.put(FEATURE_RESULT, featureResult);
        featureData.put(FEATURE_SCENARIOS, scenarioRows);

        return createFromTemplate(featureTemplate, featureData);
    }

    private void createScenarios(Feature feature, LinkedList<String> scenarioRows) {
        feature.getScenarios().forEach(scenario -> {
            if (!scenario.getKeyword().equalsIgnoreCase("Background")) {
                scenarioRows.add(createScenarioRow(feature.getFeatureId(), scenario));
            }
        });
    }

    private List<String> createScenarioTags(Scenario scenario) {

        final LinkedList<String> scenarioTags = new LinkedList<>();

        scenario.getTags().forEach(tag -> {
            final LinkedHashMap<String, Object> scenarioTagData = new LinkedHashMap<>();
            scenarioTagData.put(TAG, tag.getName());
            scenarioTags.add(createFromTemplate(scenarioTagTemplate, scenarioTagData));
        });

        return scenarioTags;
    }

    private String createScenarioRow(String featureId, Scenario scenario) {

        final LinkedHashMap<String, Object> scenarioData = new LinkedHashMap<>();

        String scenarioId = scenario.getScenarioId();
        String scenarioName = scenario.getName();
        String scenarioBadge = scenario.passed() ? SUCCESS : DANGER;
        String scenarioResult = scenarioBadge.equals(SUCCESS) ? PASSED : FAILED;

        if (scenario.isRerun()) {
            if (scenario.passed()) {
                scenarioResult = PASSED_AFTER_RERUN;
            } else {
                scenarioResult = FAILED_AFTER_RERUN;
            }
            scenarioData.put(RERUN_INDEX, scenario.getRerunIndex());
        }

        scenarioData.put(DATA_TARGET, featureId);
        scenarioData.put(MODAL_TARGET, scenarioId);
        scenarioData.put(SCENARIO_NAME, scenarioName);
        scenarioData.put(SCENARIO_BADGE, scenarioBadge);
        scenarioData.put(SCENARIO_RESULT, scenarioResult);
        scenarioData.put(SCENARIO_TAGS, createScenarioTags(scenario));
        return createFromTemplate(scenarioTemplate, scenarioData);
    }

    private String createScenarioModal(Feature feature, Scenario scenario) {
        final String featureName = feature.getUri().substring(feature.getUri().lastIndexOf("/") + 1);

        final LinkedHashMap<String, Object> modalData = new LinkedHashMap<>();

        modalData.put(MODAL_TARGET, scenario.getScenarioId());
        modalData.put(MODAL_HEADING, scenario.getName());
        modalData.put(MODAL_FEATURE_LINE, featureName + " - line " + scenario.getLine());

        List<String> modalBody = new ArrayList<>();

        scenario.getSteps().forEach(step -> modalBody.add(createRowFromStep(step)));

        modalData.put(MODAL_BODY, modalBody);

        return createFromTemplate(modalTemplate, modalData);
    }

    private String createRowFromStep(Step step) {

        final LinkedHashMap<String, Object> stepData = new LinkedHashMap<>();

        String stepStatusBadge = statusBadge.apply(step.getResult());

        String stepResult = statusLabel.apply(step.getResult());

        stepData.put(STEP_KEYWORD, step.getKeyword());
        if (EnumUtils.isValidEnum(HookType.class, step.getKeyword())) {
            stepData.put(STEP_TYPE, PRIMARY);
        } else {
            stepData.put(STEP_TYPE, INFO);
        }
        stepData.put(STEP_NAME, step.getName());
        stepData.put(STEP_DURATION, step.getResult().getDuration());
        stepData.put(STEP_BADGE, stepStatusBadge);
        stepData.put(STEP_RESULT, stepResult);

        if (step.getRowData() != null) {
            addNestedMap(stepData, STEP_DATATABLE, DATATABLE, step.getRowData());
        }

        if (step.getResult().getErrorMessage() != null) {
            addNestedMap(stepData, STEP_EXCEPTION, EXCEPTION, step.getResult().getErrorMessage());
        }

        if (!step.getOutput().isEmpty()) {
            addNestedMap(stepData, STEP_OUTPUT, OUTPUT, step.getOutput());
        }

        step.getEmbeddings().forEach(embedding -> {

            if (embedding.getMimeType().equals("text/html")) {
                String htmlData = new String(Base64.getDecoder().decode(embedding.getData()));

                addNestedMap(stepData, STEP_EMBEDDING_TEXT, TEXT, htmlData);

            } else if (embedding.getMimeType().startsWith("image")) {
                addNestedMap(stepData, STEP_EMBEDDING_IMAGE, IMAGE_ID, embedding.getEmbeddingId());
            }
        });

        return createFromTemplate(modalStepTemplate, stepData);
    }

    private String createModalRow(String rowInfo) {

        final LinkedHashMap<String, Object> rowInfoData = new LinkedHashMap<>();
        rowInfoData.put(ROW_INFO, rowInfo);

        return createFromTemplate(modalRowTemplate, rowInfoData);
    }

    private String createEnvironmentInfoModal() {
        final List<String> envData = new ArrayList<>();

        //final String envInfo = courgetteProperties.getCourgetteOptions().environmentInfo().trim();

//        final String[] values = envInfo.split(";");
//
//        for (String value : values) {
//            String[] keyValue = value.trim().split("=");
//
//            if (keyValue.length == 2) {
//                envData.add(keyValue[0].trim() + " = " + keyValue[1].trim());
//            }
//        }

//        if (envData.isEmpty()) {
        envData.add("No additional environment information provided.");
        //      }

        final LinkedHashMap<String, Object> environmentInfoData = new LinkedHashMap<>();

        final LinkedList<String> rowInfo = new LinkedList<>();
        envData.forEach(info -> rowInfo.add(createModalRow(info)));

        environmentInfoData.put(MODAL_BODY, rowInfo);

        return createFromTemplate(modalEnvironmentTemplate, environmentInfoData);
    }

    private String createFromTemplate(Mustache template, Object data) {
        Writer writer = new StringWriter();
        template.execute(writer, data);
        return writer.toString();
    }

    private Mustache readTemplate(String template) {
        StringBuilder templateContent = new StringBuilder();

        try {
            final InputStream in = getClass().getClassLoader().getResourceAsStream(template);
            assert in != null;
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = reader.readLine()) != null) {
                templateContent.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new DefaultMustacheFactory().compile(new StringReader(templateContent.toString()), "");
    }
}