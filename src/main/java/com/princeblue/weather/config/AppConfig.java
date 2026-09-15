package com.princeblue.weather.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private final String apiKey;

    public AppConfig() {
        Properties props = new Properties();

        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new IllegalStateException("config.properties file not found in resources!");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read config.properties", e);
        }

        this.apiKey = props.getProperty("api.key");

        if (this.apiKey == null || this.apiKey.trim().isEmpty() || this.apiKey.equals("YOUR_API_KEY_HERE")) {
            throw new IllegalStateException("API key is missing or not configured in config.properties!");
        }
    }

    public String getApiKey() {
        return apiKey;
    }
}
