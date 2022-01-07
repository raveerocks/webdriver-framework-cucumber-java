package com.browserstack.report.models;

import com.browserstack.Execution;
import io.cucumber.plugin.event.HookTestStep;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.TestStep;
import io.cucumber.plugin.event.TestStepFinished;

import java.util.List;

public class ModelUtil {

    public static Tag convertTag(String tag) {
        return new Tag(tag);
    }

    public static Result convertResult(TestStepFinished testStepFinished) {
        io.cucumber.plugin.event.Result stepResult = testStepFinished.getResult();
        Result result = new Result();
        result.setDuration(stepResult.getDuration().toMillis());
        result.setStatus(stepResult.getStatus().toString());
        result.setErrorMessage(stepResult.getError() == null ? "" : stepResult.getError().getMessage());
        return result;
    }

    public static Step convertStep(TestStepFinished testStepFinished) {

        Step step = new Step();
        TestStep testStep = testStepFinished.getTestStep();

        if (testStep instanceof PickleStepTestStep) {
            PickleStepTestStep pickleStepTestStep = (PickleStepTestStep) testStep;
            step.setKeyword(pickleStepTestStep.getStep().getKeyword());
            step.setName(pickleStepTestStep.getStep().getText());
        } else if (testStep instanceof HookTestStep) {
            HookTestStep hookTestStep = (HookTestStep) testStep;
            step.setKeyword(hookTestStep.getHookType().toString());
            step.setName(hookTestStep.getCodeLocation());
        }
        //step.setName(testStepFinished.getTestStep().getCodeLocation());
        step.setResult(convertResult(testStepFinished));
        step.getOutput().add("Sample Output");
        //step.getEmbeddings().add(new Embedding());
        step.getRowData().add("Sample Raw Data");
        return step;
    }

    public static Scenario convertScenario(int batch, Execution execution) {
        Scenario scenario = new Scenario();
        scenario.setScenarioId(execution.getPickle().getId() + "-" + batch);
        scenario.setName(execution.getPickle().getName());
        scenario.setLine(execution.getPickle().getScenarioLocation().getLine());
        scenario.setKeyword(execution.getPickle().getKeyword());
        scenario.setFeatureId(execution.getFeature().getUri().toString());
        scenario.setRerunIndex(batch);
        List<Tag> tags = scenario.getTags();
        List<String> pickleTags = execution.getPickle().getTags();
        pickleTags.forEach(tag -> tags.add(convertTag(tag)));
        scenario.setTags(tags);
        return scenario;
    }


}
