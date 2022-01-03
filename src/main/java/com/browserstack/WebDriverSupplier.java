package com.browserstack;

import org.openqa.selenium.WebDriver;

final class WebDriverSupplier {

    private static final ThreadLocal<WebDriver> WEB_DRIVER_THREAD_LOCAL = new ThreadLocal<>();

    static void putWebDriver(WebDriver webDriver) {
        WEB_DRIVER_THREAD_LOCAL.set(webDriver);
    }

    static void popWebDriver() {
        WEB_DRIVER_THREAD_LOCAL.remove();
    }

    public static WebDriver getWebDriver(){
        return WEB_DRIVER_THREAD_LOCAL.get();
    }

}
