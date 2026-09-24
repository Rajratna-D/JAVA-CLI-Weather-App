package com.princeblue.weather.ui;

import com.princeblue.weather.exception.ApiUnavailableException;
import com.princeblue.weather.exception.CityNotFoundException;
import com.princeblue.weather.exception.InvalidApiKeyException;
import com.princeblue.weather.exception.WeatherAppException;
import com.princeblue.weather.model.Location;
import com.princeblue.weather.model.Weather;
import com.princeblue.weather.service.WeatherService;

import java.io.PrintStream;
import java.util.Objects;
import java.util.Scanner;

public class ConsoleUI {

    private final WeatherService weatherService;
    private final Scanner scanner;
    private final PrintStream out;

    public ConsoleUI(WeatherService weatherService) {
        this(weatherService, new Scanner(System.in), System.out);
    }

    public ConsoleUI(WeatherService weatherService, Scanner scanner, PrintStream out) {
        this.weatherService = Objects.requireNonNull(weatherService, "weatherService must not be null");
        this.scanner = Objects.requireNonNull(scanner, "scanner must not be null");
        this.out = Objects.requireNonNull(out, "out must not be null");
    }

    public void start() {
        printBanner();

        while (true) {
            out.print("\n[Weather] Enter city name (or 'exit' to quit) > ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("q")) {
                printExitMessage();
                break;
            }

            if (input.isEmpty()) {
                out.println("⚠️ Please enter a non-empty city name.");
                continue;
            }

            fetchAndDisplayWeather(input);
        }
    }

    private void fetchAndDisplayWeather(String cityName) {
        try {
            out.println("\n⏳ Fetching live weather for \"" + cityName + "\"...");
            Weather weather = weatherService.getWeather(cityName);
            displayWeatherCard(weather);

        } catch (CityNotFoundException e) {
            out.println("❌ " + e.getMessage());
            out.println("💡 Tip: Check for typos or try specifying country code (e.g., \"Paris, FR\").");

        } catch (InvalidApiKeyException e) {
            out.println("🔑 API Key Error: " + e.getMessage());
            out.println("💡 Tip: Verify your key in 'src/main/resources/config.properties'.");

        } catch (ApiUnavailableException e) {
            out.println("📡 Network/Service Error: " + e.getMessage());
            out.println("💡 Tip: Please check your internet connection and try again.");

        } catch (IllegalArgumentException e) {
            out.println("⚠️ Invalid input: " + e.getMessage());

        } catch (WeatherAppException e) {
            out.println("⚠️ Unexpected application error: " + e.getMessage());
        }
    }

    private void displayWeatherCard(Weather weather) {
        Location loc = weather.getLocation();
        String countryStr = loc.getCountry() != null ? ", " + loc.getCountry() : "";
        String coordsStr = String.format(java.util.Locale.US, "(%.2f°, %.2f°)", loc.getLatitude(), loc.getLongitude());
        String weatherIcon = getWeatherIcon(weather.getDescription());

        out.println("╔══════════════════════════════════════════════════════╗");
        out.println(String.format(java.util.Locale.US, "║  📍 %-49s║", loc.getCity() + countryStr + " " + coordsStr));
        out.println("╠══════════════════════════════════════════════════════╣");
        out.println(String.format(java.util.Locale.US, "║  🌡️  Temperature : %-5.1f°C (Feels like: %-5.1f°C)     ║",
                weather.getTemperature(), weather.getFeelsLike()));
        out.println(String.format(java.util.Locale.US, "║  %s  Condition   : %-34s║",
                weatherIcon, capitalize(weather.getDescription())));
        out.println(String.format(java.util.Locale.US, "║  💧 Humidity    : %-3d%%                              ║",
                weather.getHumidity()));
        out.println(String.format(java.util.Locale.US, "║  💨 Wind Speed  : %-5.1f m/s                          ║",
                weather.getWindSpeed()));
        out.println("╚══════════════════════════════════════════════════════╝");
    }

    private String getWeatherIcon(String description) {
        if (description == null) return "🌤️";
        String lower = description.toLowerCase();
        if (lower.contains("thunder") || lower.contains("storm")) return "⛈️";
        if (lower.contains("drizzle") || lower.contains("rain")) return "🌧️";
        if (lower.contains("snow") || lower.contains("ice") || lower.contains("sleet")) return "❄️";
        if (lower.contains("clear") || lower.contains("sun")) return "☀️";
        if (lower.contains("cloud")) return "⛅";
        if (lower.contains("mist") || lower.contains("fog") || lower.contains("haze") || lower.contains("smoke")) return "🌫️";
        return "🌤️";
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return "";
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private void printBanner() {
        out.println("╔══════════════════════════════════════════════════════╗");
        out.println("║                🌤️  JAVA WEATHER CLI                   ║");
        out.println("║      Real-time weather powered by OpenWeather API    ║");
        out.println("╚══════════════════════════════════════════════════════╝");
        out.println("Commands: Type a city name, or 'exit' / 'quit' to leave.");
    }

    private void printExitMessage() {
        out.println("\n👋 Goodbye! Thank you for using Java Weather CLI. Have a wonderful day!");
    }
}
