package com.princeblue.weather.ui;

import com.princeblue.weather.model.Location;
import com.princeblue.weather.model.Weather;
import com.princeblue.weather.service.WeatherService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConsoleUITest {

    @Test
    void start_withExitCommand_terminatesGracefully() {
        WeatherService mockService = mock(WeatherService.class);
        Scanner scanner = new Scanner("exit\n");
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outBytes, true, StandardCharsets.UTF_8);

        ConsoleUI ui = new ConsoleUI(mockService, scanner, printStream);
        ui.start();

        String output = outBytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("WEATHER"));
        assertTrue(output.contains("Goodbye"));
    }

    @Test
    void start_withValidCity_displaysWeatherCard() throws Exception {
        WeatherService mockService = mock(WeatherService.class);
        Location location = new Location("London", "GB", 51.5, -0.1);
        Weather weather = new Weather(location, 18.0, 17.5, 75, "light rain", 4.0);

        when(mockService.getWeather("London")).thenReturn(weather);

        Scanner scanner = new Scanner("London\nexit\n");
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outBytes, true, StandardCharsets.UTF_8);

        ConsoleUI ui = new ConsoleUI(mockService, scanner, printStream);
        ui.start();

        String output = outBytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("London, GB"));
        assertTrue(output.contains("18.0"));
        assertTrue(output.contains("Light rain"));
        assertTrue(output.contains("Fetched at"));
    }

    @Test
    void start_withUnitToggleCommand_switchesToFahrenheit() throws Exception {
        WeatherService mockService = mock(WeatherService.class);
        Location location = new Location("Tokyo", "JP", 35.6, 139.6);
        Weather weather = new Weather(location, 20.0, 20.0, 60, "clear sky", 2.0);

        when(mockService.getWeather("Tokyo")).thenReturn(weather);

        Scanner scanner = new Scanner("unit\nTokyo\nexit\n");
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outBytes, true, StandardCharsets.UTF_8);

        ConsoleUI ui = new ConsoleUI(mockService, scanner, printStream);
        ui.start();

        String output = outBytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Fahrenheit (°F)"));
        assertTrue(output.contains("68.0°F"));
    }

    @Test
    void start_withHistoryCommand_displaysAlignedHistoryTable() {
        WeatherService mockService = mock(WeatherService.class);
        java.time.Instant now = java.time.Instant.parse("2026-09-25T01:25:00Z");
        java.util.List<com.princeblue.weather.model.SearchRecord> records = java.util.List.of(
                new com.princeblue.weather.model.SearchRecord(1L, "Rajasthan", 27.1, "clear sky", now),
                new com.princeblue.weather.model.SearchRecord(2L, "France", 23.9, "scattered clouds", now.minusSeconds(600)),
                new com.princeblue.weather.model.SearchRecord(3L, "Tokyo", 18.0, "light rain", now.minusSeconds(1200))
        );

        when(mockService.getRecentSearches(30)).thenReturn(records);

        Scanner scanner = new Scanner("history\nexit\n");
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outBytes, true, StandardCharsets.UTF_8);

        ConsoleUI ui = new ConsoleUI(mockService, scanner, printStream);
        ui.start();

        String output = outBytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("RECENT SEARCH HISTORY"));
        assertTrue(output.contains("Rajasthan"));
        assertTrue(output.contains("France"));
        assertTrue(output.contains("Tokyo"));

        // Verify that every line of the history table has matching visual width
        String[] lines = output.split("\\r?\\n");
        boolean inHistoryTable = false;
        int expectedTableWidth = 74;

        for (String line : lines) {
            String stripped = line.replaceAll("\u001B\\[[;\\d]*m", "").trim();
            if (stripped.startsWith("╔") && stripped.endsWith("╗") && stripped.length() == expectedTableWidth) {
                inHistoryTable = true;
            }
            if (inHistoryTable) {
                // Calculate display width
                int width = 0;
                for (int i = 0; i < stripped.length(); i++) {
                    char c = stripped.charAt(i);
                    if (c == '\uFE0F') continue;
                    if (Character.isHighSurrogate(c)) {
                        width += 2;
                        i++;
                    } else if ((c >= 0x2600 && c <= 0x27BF) || (c >= 0x2B50 && c <= 0x2B55)) {
                        width += 2;
                    } else {
                        width += 1;
                    }
                }
                org.junit.jupiter.api.Assertions.assertEquals(expectedTableWidth, width,
                        "Mismatch in table line: [" + stripped + "]");

                if (stripped.startsWith("╚") && stripped.endsWith("╝")) {
                    inHistoryTable = false;
                }
            }
        }
    }

    @Test
    void start_withHomePredictions_displaysAlignedPredictionsTable() throws Exception {
        WeatherService mockService = mock(WeatherService.class);
        com.princeblue.weather.service.HomeCityService mockHomeService = mock(com.princeblue.weather.service.HomeCityService.class);

        com.princeblue.weather.model.HomeCityConfig cfg = new com.princeblue.weather.model.HomeCityConfig("Pune", 18.52, 73.86, java.time.Instant.now());
        Location loc = new Location("Pune", "IN", 18.52, 73.86);
        Weather weather = new Weather(loc, 28.0, 27.5, 55, "clear sky", 2.5);
        java.util.List<com.princeblue.weather.model.WeatherPrediction> preds = java.util.List.of(
                new com.princeblue.weather.model.WeatherPrediction(java.time.LocalDate.now(), 26.5, 21.2, 31.8, "Clear sky", java.time.Instant.now()),
                new com.princeblue.weather.model.WeatherPrediction(java.time.LocalDate.now().plusDays(1), 27.0, 22.0, 32.5, "Partly cloudy", java.time.Instant.now())
        );

        when(mockHomeService.getHomeCityConfig()).thenReturn(java.util.Optional.of(cfg));
        when(mockHomeService.getRecordCount()).thenReturn(87672);
        when(mockHomeService.getPredictions(org.mockito.ArgumentMatchers.any())).thenReturn(preds);
        when(mockService.getWeather("Pune")).thenReturn(weather);

        Scanner scanner = new Scanner("home\n3\n4\nexit\n");
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outBytes, true, StandardCharsets.UTF_8);

        ConsoleUI ui = new ConsoleUI(mockService, mockHomeService, scanner, printStream);
        ui.start();

        String output = outBytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("AI WEATHER PREDICTIONS"));
        assertTrue(output.contains("Pune"));

        String[] lines = output.split("\\r?\\n");
        boolean inPredTable = false;
        int expectedTableWidth = 74;

        for (String line : lines) {
            String stripped = line.replaceAll("\u001B\\[[;\\d]*m", "").trim();
            if (stripped.startsWith("╔") && stripped.endsWith("╗") && stripped.length() == expectedTableWidth) {
                inPredTable = true;
            }
            if (inPredTable) {
                int width = 0;
                for (int i = 0; i < stripped.length(); i++) {
                    char c = stripped.charAt(i);
                    if (c == '\uFE0F') continue;
                    if (Character.isHighSurrogate(c)) {
                        width += 2;
                        i++;
                    } else if ((c >= 0x2600 && c <= 0x27BF) || (c >= 0x2B50 && c <= 0x2B55)) {
                        width += 2;
                    } else {
                        width += 1;
                    }
                }
                org.junit.jupiter.api.Assertions.assertEquals(expectedTableWidth, width,
                        "Mismatch in predictions table line: [" + stripped + "]");

                if (stripped.startsWith("╚") && stripped.endsWith("╝")) {
                    inPredTable = false;
                }
            }
        }
    }

    @Test
    void start_withUnconfiguredHome_autoConfiguresAndSyncs() throws Exception {
        WeatherService mockService = mock(WeatherService.class);
        com.princeblue.weather.service.HomeCityService mockHomeService = mock(com.princeblue.weather.service.HomeCityService.class);

        com.princeblue.weather.model.HomeCityConfig cfg = new com.princeblue.weather.model.HomeCityConfig("Pune", 18.52, 73.86, java.time.Instant.now());
        Location loc = new Location("Pune", "IN", 18.52, 73.86);
        Weather weather = new Weather(loc, 28.0, 27.5, 55, "clear sky", 2.5);
        java.util.List<com.princeblue.weather.model.WeatherPrediction> preds = java.util.List.of(
                new com.princeblue.weather.model.WeatherPrediction(java.time.LocalDate.now(), 26.5, 21.2, 31.8, "Clear sky", java.time.Instant.now())
        );

        when(mockHomeService.getHomeCityConfig())
                .thenReturn(java.util.Optional.empty())
                .thenReturn(java.util.Optional.of(cfg));
        when(mockHomeService.configureHomeCity("Pune")).thenReturn(cfg);
        when(mockHomeService.syncHistoricalData(10)).thenReturn(87672);
        when(mockHomeService.startAiPipelineProcess()).thenReturn(null);
        when(mockHomeService.getPredictions(org.mockito.ArgumentMatchers.any())).thenReturn(preds);
        when(mockService.getWeather("Pune")).thenReturn(weather);

        Scanner scanner = new Scanner("home\nPune\n4\nexit\n");
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outBytes, true, StandardCharsets.UTF_8);

        ConsoleUI ui = new ConsoleUI(mockService, mockHomeService, scanner, printStream);
        ui.start();

        String output = outBytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("No Home City configured yet"));
        assertTrue(output.contains("Location resolved"));
        assertTrue(output.contains("Pune"));
        assertTrue(output.contains("Fetching 10 years of hourly climate data"));
        assertTrue(output.contains("Training Facebook Prophet AI model"));
        assertTrue(output.contains("AI WEATHER PREDICTIONS"));

        org.mockito.Mockito.verify(mockHomeService).configureHomeCity("Pune");
        org.mockito.Mockito.verify(mockHomeService).syncHistoricalData(10);
        org.mockito.Mockito.verify(mockHomeService).startAiPipelineProcess();
    }
}
