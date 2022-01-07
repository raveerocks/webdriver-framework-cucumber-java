package com.browserstack.report.models;

import java.util.ArrayList;
import java.util.List;

import static io.cucumber.core.backend.Status.PASSED;
import static io.cucumber.core.backend.Status.SKIPPED;

public class Step {

    private String name;
    private String keyword;
    private Result result;
    private List<Embedding> embeddings = new ArrayList<>();
    private List<String> output = new ArrayList<>();
    private List<String> rowData = new ArrayList<>();

    public Step() {
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

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public List<Embedding> getEmbeddings() {
        return embeddings;
    }

    public List<String> getOutput() {
        return output;
    }

    public List<String> getRowData() {
        return rowData;
    }

    public boolean passed() {
        return result.getStatus().equalsIgnoreCase(PASSED.toString()) || result.getStatus().equalsIgnoreCase(SKIPPED.toString());
    }

}