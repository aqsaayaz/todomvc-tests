package com.todomvc.config;

import com.todomvc.enums.Framework;

/**
 * Resolves run configuration from system properties (set via Maven -D flags
 * or testng.xml <parameter> tags), with sane defaults.
 *
 * Usage:
 *   mvn test -Dframework=vue -Dseed=42 -Dheadless=true
 */
public class TestConfig {

    public static Framework getFramework() {
        String id = System.getProperty("framework", "react");
        return Framework.fromId(id);
    }

    public static long getSeed() {
        String seedProp = System.getProperty("seed");
        if (seedProp != null && !seedProp.isBlank()) {
            return Long.parseLong(seedProp.trim());
        }
        // Default fixed seed keeps local runs reproducible by default too.
        return 20260811L;
    }

    public static int getActionCount() {
        String prop = System.getProperty("actionCount");
        return prop != null ? Integer.parseInt(prop) : 30;
    }

    public static String getBaseUrl(Framework framework) {
        String override = System.getProperty("baseUrl");
        return (override != null && !override.isBlank()) ? override : framework.getDefaultUrl();
    }

    public static boolean isHeadless() {
        return Boolean.parseBoolean(System.getProperty("headless", "true"));
    }

    public static int getImplicitWaitSeconds() {
        return 10;
    }
}
