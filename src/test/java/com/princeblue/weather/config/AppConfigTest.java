package com.princeblue.weather.config;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class AppConfigTest {

    @Test
    void shouldLoadValidApiKeyFromProperties() {
        Properties props = new Properties();
        props.setProperty("api.key", "valid-test-key-12345");

        AppConfig config = new AppConfig(props);
        assertEquals("valid-test-key-12345", config.getApiKey());
    }

    @Test
    void shouldThrowExceptionWhenApiKeyIsMissing() {
        Properties emptyProps = new Properties();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new AppConfig(emptyProps)
        );
        assertTrue(ex.getMessage().contains("Missing required configuration property"));
    }

    @Test
    void shouldThrowExceptionWhenApiKeyIsBlank() {
        Properties blankProps = new Properties();
        blankProps.setProperty("api.key", "   ");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new AppConfig(blankProps)
        );
        assertTrue(ex.getMessage().contains("Missing required configuration property"));
    }
}

