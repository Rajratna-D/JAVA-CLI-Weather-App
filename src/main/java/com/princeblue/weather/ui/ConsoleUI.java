package com.princeblue.weather.ui;

import com.princeblue.weather.exception.ApiUnavailableException;
import com.princeblue.weather.exception.CityNotFoundException;
import com.princeblue.weather.exception.InvalidApiKeyException;
import com.princeblue.weather.exception.WeatherAppException;
import com.princeblue.weather.model.Location;
import com.princeblue.weather.model.Weather;
import com.princeblue.weather.service.WeatherService;

import java.io.PrintStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

public class ConsoleUI {

    // ANSI Color Codes
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String CYAN = "\u001B[36m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED = "\u001B[31m";
    public static final String BLUE = "\u001B[94m";
    public static final String GRAY = "\u001B[90m";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm:ss a");

    private final WeatherService weatherService;
    private final Scanner scanner;
    private final PrintStream out;
    private boolean isMetric = true; // true = Celsius (°C), false = Fahrenheit (°F)

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
            String unitBadge = isMetric ? CYAN + "[°C]" + RESET : YELLOW + "[°F]" + RESET;
            out.print("\n" + BLUE + "[Weather " + unitBadge + BLUE + "]" + RESET + " Enter city (or 'unit', 'exit') > ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("q")) {
                printExitMessage();
                break;
            }

            if (input.equalsIgnoreCase("unit") || input.equalsIgnoreCase("units")) {
                isMetric = !isMetric;
                out.println(GREEN + "✅ Temperature unit switched to: " + BOLD
                        + (isMetric ? "Celsius (°C)" : "Fahrenheit (°F)") + RESET);
                continue;
            }

            if (input.equalsIgnoreCase("unit c") || input.equalsIgnoreCase("c")) {
                isMetric = true;
                out.println(GREEN + "✅ Temperature unit set to: " + BOLD + "Celsius (°C)" + RESET);
                continue;
            }

            if (input.equalsIgnoreCase("unit f") || input.equalsIgnoreCase("f")) {
                isMetric = false;
                out.println(GREEN + "✅ Temperature unit set to: " + BOLD + "Fahrenheit (°F)" + RESET);
                continue;
            }

            if (input.isEmpty()) {
                out.println(YELLOW + "⚠️ Please enter a non-empty city name." + RESET);
                continue;
            }

            fetchAndDisplayWeather(input);
        }
    }

    private void fetchAndDisplayWeather(String cityName) {
        try {
            out.println(GRAY + "\n⏳ Fetching live weather for \"" + cityName + "\"..." + RESET);
            Weather weather = weatherService.getWeather(cityName);
            displayWeatherCard(weather);

        } catch (CityNotFoundException e) {
            out.println(RED + "❌ " + e.getMessage() + RESET);
            out.println(YELLOW + "💡 Tip: Check for typos or try specifying country code (e.g., \"Paris, FR\")." + RESET);

        } catch (InvalidApiKeyException e) {
            out.println(RED + "🔑 API Key Error: " + e.getMessage() + RESET);
            out.println(YELLOW + "💡 Tip: Verify your key in 'src/main/resources/config.properties'." + RESET);

        } catch (ApiUnavailableException e) {
            out.println(RED + "📡 Network/Service Error: " + e.getMessage() + RESET);
            out.println(YELLOW + "💡 Tip: Please check your internet connection and try again." + RESET);

        } catch (IllegalArgumentException e) {
            out.println(YELLOW + "⚠️ Invalid input: " + e.getMessage() + RESET);

        } catch (WeatherAppException e) {
            out.println(RED + "⚠️ Unexpected application error: " + e.getMessage() + RESET);
        }
    }

    private void displayWeatherCard(Weather weather) {
        Location loc = weather.getLocation();
        String countryStr = loc.getCountry() != null ? ", " + loc.getCountry() : "";
        String coordsStr = String.format(Locale.US, "(%.2f°, %.2f°)", loc.getLatitude(), loc.getLongitude());
        String weatherIcon = getWeatherIcon(weather.getDescription());

        double tempC = weather.getTemperature();
        double feelsC = weather.getFeelsLike();
        String tempColor = getTemperatureColor(tempC);

        String tempStr = formatTemperature(tempC);
        String feelsStr = formatTemperature(feelsC);
        String windStr = formatWindSpeed(weather.getWindSpeed());
        String timestamp = LocalTime.now().format(TIME_FORMATTER);

        out.println(BLUE + "╔══════════════════════════════════════════════════════╗" + RESET);
        out.println(BLUE + "║  📍 " + BOLD + String.format(Locale.US, "%-49s", loc.getCity() + countryStr + " " + coordsStr) + RESET + BLUE + "║" + RESET);
        out.println(BLUE + "╠══════════════════════════════════════════════════════╣" + RESET);
        out.println(BLUE + "║  🌡️  Temperature : " + tempColor + BOLD + String.format(Locale.US, "%-10s", tempStr) + RESET
                + " (Feels like: " + tempColor + String.format(Locale.US, "%-10s", feelsStr) + RESET + ")  " + BLUE + "║" + RESET);
        out.println(BLUE + "║  " + weatherIcon + "  Condition   : " + BOLD + String.format(Locale.US, "%-34s", capitalize(weather.getDescription())) + RESET + BLUE + "║" + RESET);
        out.println(BLUE + "║  💧 Humidity    : " + CYAN + String.format(Locale.US, "%-3d%%", weather.getHumidity()) + RESET + "                              " + BLUE + "║" + RESET);
        out.println(BLUE + "║  💨 Wind Speed  : " + String.format(Locale.US, "%-35s", windStr) + BLUE + "║" + RESET);
        out.println(BLUE + "╟──────────────────────────────────────────────────────╢" + RESET);
        out.println(BLUE + "║  🕒 Fetched at   : " + GRAY + String.format(Locale.US, "%-35s", timestamp + " | OpenWeather") + RESET + BLUE + "║" + RESET);
        out.println(BLUE + "╚══════════════════════════════════════════════════════╝" + RESET);
    }

    private String formatTemperature(double celsius) {
        if (isMetric) {
            return String.format(Locale.US, "%.1f°C", celsius);
        } else {
            double fahrenheit = (celsius * 9.0 / 5.0) + 32.0;
            return String.format(Locale.US, "%.1f°F", fahrenheit);
        }
    }

    private String formatWindSpeed(double mps) {
        if (isMetric) {
            return String.format(Locale.US, "%.1f m/s", mps);
        } else {
            double mph = mps * 2.23694;
            return String.format(Locale.US, "%.1f mph", mph);
        }
    }

    private String getTemperatureColor(double celsius) {
        if (celsius < 10.0) {
            return CYAN;   // Cold
        } else if (celsius <= 25.0) {
            return GREEN;  // Mild / Pleasant
        } else if (celsius <= 32.0) {
            return YELLOW; // Warm
        } else {
            return RED;    // Hot
        }
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
        out.println(BLUE + "╔══════════════════════════════════════════════════════╗" + RESET);
        out.println(BLUE + "║         " + YELLOW + "🌤️  JAVA CLI WEATHER APPLICATION" + BLUE + "             ║" + RESET);
        out.println(BLUE + "║      " + GRAY + "Real-time weather powered by OpenWeather API" + BLUE + "    ║" + RESET);
        out.println(BLUE + "╚══════════════════════════════════════════════════════╝" + RESET);
        out.println(BOLD + "Commands:" + RESET);
        out.println("  • " + CYAN + "<city name>" + RESET + "  : View weather (e.g., 'London', 'Tokyo', 'New York')");
        out.println("  • " + YELLOW + "'unit'" + RESET + " / " + YELLOW + "'f'" + RESET + "   : Toggle Celsius (°C) / Fahrenheit (°F)");
        out.println("  • " + RED + "'exit'" + RESET + " / " + RED + "'q'" + RESET + "   : Exit the application");
    }

    private void printExitMessage() {
        out.println("\n" + GREEN + "👋 Goodbye! Thank you for using Java Weather CLI. Have a wonderful day!" + RESET);
    }
}
