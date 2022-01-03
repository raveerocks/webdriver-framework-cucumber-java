package com.browserstack;

import io.cucumber.core.options.CommandlineOptionsParser;
import io.cucumber.core.options.CucumberProperties;
import io.cucumber.core.options.CucumberPropertiesParser;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.plugin.Plugin;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

public final class WebDriverTestRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebDriverTestRunner.class);

    private WebDriverTestRunner(){}

    public  static void run(String... argv){
        run(new Plugin[0],argv);
    }

    public  static void run(Plugin[] additionalPlugins,String... argv) {
        Plugin[] additionalPluginsCopy = Arrays.copyOf(additionalPlugins, additionalPlugins.length + 1);
        additionalPluginsCopy[additionalPlugins.length] = new WebDriverListener();
        run(argv, Thread.currentThread().getContextClassLoader(),additionalPluginsCopy);
    }

    public static WebDriver getWebDriver(){
        return WebDriverSupplier.getWebDriver();
    }

    private static void run(String[] argv, ClassLoader classLoader, Plugin... additionalPlugins) {
        RuntimeOptions propertiesFileOptions = (new CucumberPropertiesParser()).parse(CucumberProperties.fromPropertiesFile()).build();
        RuntimeOptions environmentOptions = (new CucumberPropertiesParser()).parse(CucumberProperties.fromEnvironment()).build(propertiesFileOptions);
        RuntimeOptions systemOptions = (new CucumberPropertiesParser()).parse(CucumberProperties.fromSystemProperties()).build(environmentOptions);
        CommandlineOptionsParser commandlineOptionsParser = new CommandlineOptionsParser(System.out);
        RuntimeOptions runtimeOptions = commandlineOptionsParser.parse(argv).addDefaultGlueIfAbsent().addDefaultFeaturePathIfAbsent().addDefaultFormatterIfAbsent().addDefaultSummaryPrinterIfAbsent().enablePublishPlugin().build(systemOptions);
        Optional<Byte> exitStatus = commandlineOptionsParser.exitStatus();
        if (exitStatus.isPresent()) {
            return;
        } else {
            WebDriverRuntime runtime  = WebDriverRuntime
                    .builder()
                    .withRuntimeOptions(runtimeOptions).withClassLoader(() -> classLoader)
                    .withAdditionalPlugins(additionalPlugins)
                    .build();
            LOGGER.info("Cucumber runtime created.");
            runtime.run();
        }
    }
}
