package com.princeblue.weather.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {

    private static final String CONFIG_FILE = "config.properties";

    private final Properties properties;

    public AppConfig() {
        properties = new Properties();

        try (InputStream input = AppConfig.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (input == null) {
                throw new IllegalStateException(
                        "Configuration file '" + CONFIG_FILE + "' not found on classpath.");
            }

            properties.load(input);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load configuration file: " + CONFIG_FILE,
                    e);
        }
    }

    public String getApiKey() {
        String apiKey = properties.getProperty("api.key");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Missing required configuration property: api.key");
        }

        return apiKey.trim();
    }
}