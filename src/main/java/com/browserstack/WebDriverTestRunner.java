package com.browserstack;

import com.browserstack.webdriver.WebDriverManager;
import io.cucumber.core.options.CommandlineOptionsParser;
import io.cucumber.core.options.CucumberProperties;
import io.cucumber.core.options.CucumberPropertiesParser;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.plugin.Plugin;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class WebDriverTestRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebDriverTestRunner.class);

    private WebDriverTestRunner() {
    }

    public static void run(boolean isRerunEnabled, String... argv) {
        run(isRerunEnabled, argv, Thread.currentThread().getContextClassLoader(), new WebDriverManager());
    }

    public static WebDriver getWebDriver() {
        return WebDriverManager.getWebDriver();
    }


    private static void run(boolean isRerunEnabled, String[] argv, ClassLoader classLoader, Plugin... additionalPlugins) {
        RuntimeOptions propertiesFileOptions = (new CucumberPropertiesParser()).parse(CucumberProperties.fromPropertiesFile()).build();
        RuntimeOptions environmentOptions = (new CucumberPropertiesParser()).parse(CucumberProperties.fromEnvironment()).build(propertiesFileOptions);
        RuntimeOptions systemOptions = (new CucumberPropertiesParser()).parse(CucumberProperties.fromSystemProperties()).build(environmentOptions);
        CommandlineOptionsParser commandlineOptionsParser = new CommandlineOptionsParser(System.out);
        RuntimeOptions runtimeOptions = commandlineOptionsParser.parse(argv).addDefaultGlueIfAbsent().addDefaultFeaturePathIfAbsent().addDefaultFormatterIfAbsent().addDefaultSummaryPrinterIfAbsent().enablePublishPlugin().build(systemOptions);
        Optional<Byte> exitStatus = commandlineOptionsParser.exitStatus();
        if (exitStatus.isPresent()) {
            return;
        } else {
            WebDriverRuntime runtime = (isRerunEnabled ?
                    WebDriverRuntime
                            .builder()
                            .withRerunEnabled() : WebDriverRuntime.builder().withRerunDisabled())
                    .withRuntimeOptions(runtimeOptions).withClassLoader(() -> classLoader)
                    .withAdditionalPlugins(additionalPlugins)
                    .withRerunDisabled()
                    .build();
            LOGGER.info("Cucumber runtime created.");
            //REPORT_DATA.startReporting();
            //REPORT_DATA.setConcurrency(runtimeOptions.getThreads());
            //REPORT_DATA.setFeatureFilePath(runtimeOptions.getGlue().toString());
            //REPORT_DATA.setCucumberRunTags(runtimeOptions.getTagExpressions().toString());
            runtime.run();
            //REPORT_DATA.buildReport();
        }
    }
}
