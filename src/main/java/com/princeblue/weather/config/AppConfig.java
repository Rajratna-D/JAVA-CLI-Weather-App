package com.princeblue.weather.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {

    private static final String CONFIG_FILE = "config.properties";
    private static final String PLACEHOLDER_KEY = "YOUR_API_KEY_HERE";

    private final Properties properties;
    private final String resolvedApiKey;

    public AppConfig() {
        this(CONFIG_FILE);
    }

    public AppConfig(String resourcePath) {
        properties = new Properties();

        // 1. Try loading from classpath resource (if available)
        try (InputStream input = AppConfig.class
                .getClassLoader()
                .getResourceAsStream(resourcePath)) {

            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
            // Proceed to check environment variables and system properties
        }

        // 2. Resolve key with precedence:
        // System Property -> Environment Variable -> config.properties
        String key = System.getProperty("api.key");
        if (key == null || key.isBlank()) {
            key = System.getenv("OPENWEATHER_API_KEY");
        }
        if (key == null || key.isBlank()) {
            key = System.getenv("WEATHER_API_KEY");
        }
        if (key == null || key.isBlank()) {
            key = properties.getProperty("api.key");
        }

        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "Missing required configuration property: api.key. "
                            + "Please set the OPENWEATHER_API_KEY environment variable, "
                            + "system property -Dapi.key, or configure 'config.properties'.");
        }

        key = key.trim();
        if (PLACEHOLDER_KEY.equalsIgnoreCase(key)) {
            throw new IllegalStateException(
                    "OpenWeather API key is set to default placeholder '" + PLACEHOLDER_KEY
                            + "'. Please set a valid key in 'config.properties' or via the OPENWEATHER_API_KEY environment variable.");
        }

        this.resolvedApiKey = key;
    }

    public AppConfig(Properties customProperties) {
        this.properties = customProperties != null ? customProperties : new Properties();
        String key = properties.getProperty("api.key");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Missing required configuration property: api.key");
        }
        this.resolvedApiKey = key.trim();
    }

    public String getApiKey() {
        return resolvedApiKey;
    }
}
