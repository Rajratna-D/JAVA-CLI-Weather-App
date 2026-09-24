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
}
