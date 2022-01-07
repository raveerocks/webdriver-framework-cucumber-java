About

The repository is a web driver manager framework for java-cucumber based tests. 
It contains APIs for running your tests, injecting web driver and plugin for reporting and rerun.

Events
=========

BuildStarted => Instant

RuntimeCreated => Instant,RuntimeOptions

BatchExecutionStarted => Instant, Batch (int)

ExecutionStarted => Instant,Execution

WebDriverCreated => Instant, WebDriver

ExecutionFinished => Instant,Execution,WebDriver

BatchExecutionCompleted => Instant, Batch (int)

BuildCompleted => Instant

Plugins
=======

**WebDriverManager**
======================
| Plugin | Purpose |
|--- | --- | 
|WebDriverCreated| Copies and delivers web driver to the tests
|TestCaseFinished|MarkAndCloseWebDriver

**RerunExecutionManager**
======================
| Plugin | Purpose |
|--- | --- | 
|ExecutionStarted|Copies execution for rerun
|TestCaseFinished|Push execution for rerun in next batch
|BatchExecutionCompleted|Fires next batch failed tests for run

Note : This plugin takes a parameter for maximum reruns


**CustomReportListener**
======================
| Plugin | Purpose |
|--- | --- | 
|BuildStarted|Record build start time
|RuntimeCreated|Record runtime options
|BatchExecutionStarted|Records the current batch
|ExecutionStarted|Record the features executed
|TestStepFinished|Record the step
|TestCaseFinished|Record the test case
|BuildCompleted|Record build end time & Generate reports

Note : This plugin takes a parameter for report path.
Ensure you have the mustache templates, js and css ready in `src/test/resources/reporter` directory.


**Other Classes**

| Plugin | Purpose |
|--- | --- |
BatchExecutionRunner|Executes tests on provided list of platforms
Execution|Current combination of pickle & Platform
WebDriverRuntime|Entry point for running tests
WebDriverTestRunner|External Facing API


*How to run a test with this library?*

1.Clone this repository

``
git clone git@github.com:raveerocks/webdriver-framework-cucumber-java.git
``

2. Install it in your local repository
```sh
mvn clean install
```

3. Use within your Java project
```xml
<dependency>
    <groupId>com.browserstack</groupId>
    <artifactId>webdriver-framework-cucumber-java</artifactId>
    <version>0.0.1</version>
</dependency>
```

4. Run the tests within your framework with ``WebDriverTestRunner`` and required ``CucumberOptions``

```java
public class Test{
    public void testMethod(){
        String[] argv = new String[]{
                CommandlineOptions.GLUE, "", "src/test/resources/com/browserstack"};
        WebDriverTestRunner.run(true,argv);
    }
}
```

5. Enable required plugins and options you need

```java
public class Test{
    public void testMethod(){
        String[] argv = new String[]{
                CommandlineOptions.THREADS,"25",
                CommandlineOptions.PLUGIN,"com.browserstack.rerun.RerunExecutionManager:2",
                CommandlineOptions.PLUGIN,"com.browserstack.report.CustomReportListener:target/reports",
                CommandlineOptions.NAME,"End to End Scenario",
                CommandlineOptions.GLUE, "", "src/test/resources/com/browserstack"};
        WebDriverTestRunner.run(true,argv);
    }
}
```

6. Within your test steps are a base class  use ``WebDriverTestRunner`` to get the web driver
```java
public class BaseStep {
    public WebDriver getWebDriver(){
        return WebDriverTestRunner.getWebDriver();
    }
    
}

```
