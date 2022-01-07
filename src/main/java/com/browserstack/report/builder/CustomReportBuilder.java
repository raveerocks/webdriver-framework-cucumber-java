package com.browserstack.report.builder;

import com.browserstack.report.RunTimeInfo;
import com.browserstack.report.models.Feature;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;

public class CustomReportBuilder {

    String SOURCE_ASSETS_DIRECTORY = "target/test-classes/reporter";

    private CustomReportBuilder() {
    }

    public static void createReport(String reportPath,String platform, RunTimeInfo runTimeInfo, List<Feature> features) throws IOException {
        CustomReportBuilder customReportBuilder = new CustomReportBuilder();
        customReportBuilder.create(reportPath,platform, runTimeInfo, features);
    }

    private void create(String reportPath, String platform, RunTimeInfo runTimeInfo, List<Feature> features) throws IOException {
        createDirectories(reportPath);
        copyAssetsDirectory(SOURCE_ASSETS_DIRECTORY + "/css", reportPath + "/css");
        copyAssetsDirectory(SOURCE_ASSETS_DIRECTORY + "/js", reportPath + "/js");
        reportPath = reportPath+"/"+platform;
        createDirectories(reportPath);
        generateHtmlReport(reportPath, runTimeInfo, features);
    }

    public void copyAssetsDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation) {
        File sourceDirectory = new File(sourceDirectoryLocation);
        File destinationDirectory = new File(destinationDirectoryLocation);
        try {
            FileUtils.copyDirectory(sourceDirectory, destinationDirectory);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void generateHtmlReport(String reportPath, RunTimeInfo runTimeInfo, List<Feature> features) throws IOException {
        final HtmlReportBuilder htmlReportBuilder = HtmlReportBuilder.create(reportPath, features);
        final List<String> results = htmlReportBuilder.getHtmlTableFeatureRows();
        final List<String> modals = htmlReportBuilder.getHtmlModals();
        final HashMap<String, Object> reportData = new HashMap<>();
        Instant now = Instant.now();
        reportData.put("reportTitle", now.toString().replace("/","-"));
        reportData.put("total", runTimeInfo.getTotal());
        reportData.put("passed", runTimeInfo.getPassed());
        reportData.put("failed", runTimeInfo.getFailed());
        reportData.put("rerun", runTimeInfo.getRerun());
        reportData.put("timestamp", now.toString());
        reportData.put("duration", runTimeInfo.getDuration());
        reportData.put("threads", runTimeInfo.getConcurrency());
        reportData.put("os_name", runTimeInfo.getOs());
        reportData.put("os_arch", runTimeInfo.getOsArch());
        reportData.put("java_version", runTimeInfo.getJavaVersion());
        reportData.put("cucumber_version", runTimeInfo.getCucumberVersion());
        reportData.put("tags", runTimeInfo.getCucumberTags());
        reportData.put("features", runTimeInfo.getGlue());
        reportData.put("results", results);
        reportData.put("modals", modals);


        File thisFile = new File(reportPath + "/" + now.toString() + ".html");
        BufferedWriter writer = new BufferedWriter(new FileWriter(thisFile, false));
        final InputStream in = getClass().getClassLoader().getResourceAsStream(String.format("%s/index.mustache", ReporterConstants.MUSTACHE_TEMPLATES_DIR));
        assert in != null;
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        final Mustache report = new DefaultMustacheFactory().compile(reader, "");
        report.execute(writer, reportData);

        createImageScript(writer, features);

        writer.close();
    }

    private void createImageScript(Writer writer, List<Feature> reportFeatures) throws IOException {
/*
        final List<Embedding> embeddings = new ArrayList<>();

        reportFeatures.stream().map(Feature::getScenarios)
                .flatMap(Collection::stream)
                .flatMap(t -> t.getBefore().stream())
                .flatMap(t -> t.getEmbeddings().stream())
                .forEach(embeddings::add);

        final List<Embedding> imageEmbeddings = embeddings.stream().filter(e -> e.getMimeType().startsWith("image")).collect(Collectors.toList());

        writer.write("\n<script>\n");

        for (Embedding embedding : imageEmbeddings) {
            writer.write("document.getElementById('");
            writer.write(embedding.getEmbeddingId());
            writer.write("').src='data:image;base64,");
            writer.write(embedding.getData());
            writer.write("'\n\n");
        }

        writer.write("</script>");*/
    }

    private void createDirectories(String dirPath) throws IOException {

        final File reportDir = new File(dirPath);

        if (!reportDir.exists()) {
            if (!reportDir.mkdirs()) {
                throw new IOException(String.format("Unable to create the %s directory", reportDir));
            }
        }
    }
}