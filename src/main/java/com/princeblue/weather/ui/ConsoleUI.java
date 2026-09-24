package com.princeblue.weather.ui;

import com.princeblue.weather.exception.ApiUnavailableException;
import com.princeblue.weather.exception.CityNotFoundException;
import com.princeblue.weather.exception.InvalidApiKeyException;
import com.princeblue.weather.exception.WeatherAppException;
import com.princeblue.weather.model.HomeCityConfig;
import com.princeblue.weather.model.Location;
import com.princeblue.weather.model.SearchRecord;
import com.princeblue.weather.model.Weather;
import com.princeblue.weather.model.WeatherPrediction;
import com.princeblue.weather.service.HomeCityService;
import com.princeblue.weather.service.WeatherService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
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
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

    private static final String CARD_BAR_DOUBLE = "═".repeat(54);
    private static final String CARD_BAR_SINGLE = "─".repeat(54);
    private static final String TABLE_BAR_DOUBLE = "═".repeat(72);
    private static final String TABLE_BAR_SINGLE = "─".repeat(72);

    private final WeatherService weatherService;
    private final HomeCityService homeCityService;
    private final Scanner scanner;
    private final PrintStream out;
    private boolean isMetric = true; // true = Celsius (°C), false = Fahrenheit (°F)

    public ConsoleUI(WeatherService weatherService) {
        this(weatherService, null, new Scanner(System.in), System.out);
    }

    public ConsoleUI(WeatherService weatherService, HomeCityService homeCityService) {
        this(weatherService, homeCityService, new Scanner(System.in), System.out);
    }

    public ConsoleUI(WeatherService weatherService, Scanner scanner, PrintStream out) {
        this(weatherService, null, scanner, out);
    }

    public ConsoleUI(WeatherService weatherService, HomeCityService homeCityService, Scanner scanner, PrintStream out) {
        this.weatherService = Objects.requireNonNull(weatherService, "weatherService must not be null");
        this.homeCityService = homeCityService;
        this.scanner = Objects.requireNonNull(scanner, "scanner must not be null");
        this.out = Objects.requireNonNull(out, "out must not be null");
    }

    public void start() {
        printBanner();

        while (true) {
            String unitBadge = isMetric ? CYAN + "[°C]" + RESET : YELLOW + "[°F]" + RESET;
            out.print("\n" + BLUE + "[Weather " + unitBadge + BLUE + "]" + RESET
                    + " Enter city (or 'history', 'home', 'unit', 'exit') > ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("q")) {
                printExitMessage();
                break;
            }

            if (input.equalsIgnoreCase("history")) {
                displaySearchHistory();
                continue;
            }

            if (input.equalsIgnoreCase("home")) {
                handleHomeCityDashboard();
                continue;
            }

            if (input.equalsIgnoreCase("clear cache")) {
                weatherService.clearCache();
                out.println(GREEN + "✅ Weather cache cleared successfully." + RESET);
                continue;
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
            out.println(GRAY + "\n⏳ Fetching weather for \"" + cityName + "\"..." + RESET);
            Weather weather = weatherService.getWeather(cityName);
            displayWeatherCard(weather);

        } catch (CityNotFoundException e) {
            out.println(RED + "❌ " + e.getMessage() + RESET);
            out.println(
                    YELLOW + "💡 Tip: Check for typos or try specifying country code (e.g., \"Paris, FR\")." + RESET);

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

    private static int visualWidth(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        // Remove ANSI color and formatting escape sequences
        String plain = str.replaceAll("\u001B\\[[;\\d]*m", "");
        int width = 0;
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (c == '\uFE0F') {
                continue; // Variation selector takes 0 display width
            }
            if (Character.isHighSurrogate(c)) {
                width += 2; // Emojis like 📍, 🌡, 💧, 💨 take 2 columns
                i++;        // Skip low surrogate
            } else if ((c >= 0x2600 && c <= 0x27BF) || (c >= 0x2B50 && c <= 0x2B55)) {
                width += 2; // Miscellaneous weather symbols (☀️, ⛅, 🌧)
            } else {
                width += 1;
            }
        }
        return width;
    }

    private void printBoxRow(String content, int targetWidth, String borderPrefix, String borderSuffix) {
        int vWidth = visualWidth(content);
        int pad = Math.max(0, targetWidth - vWidth);
        out.println(borderPrefix + content + " ".repeat(pad) + borderSuffix);
    }

    private static String padRight(String str, int targetWidth) {
        if (str == null) return " ".repeat(targetWidth);
        int vWidth = visualWidth(str);
        return str + " ".repeat(Math.max(0, targetWidth - vWidth));
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 1) + "…";
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

        out.println(BLUE + "╔" + CARD_BAR_DOUBLE + "╗" + RESET);
        printBoxRow("📍  " + BOLD + truncate(loc.getCity() + countryStr + " " + coordsStr, 45) + RESET, 50, BLUE + "║  " + RESET, BLUE + "  ║" + RESET);
        out.println(BLUE + "╠" + CARD_BAR_DOUBLE + "╣" + RESET);
        printBoxRow("🌡️  Temperature : " + tempColor + BOLD + tempStr + RESET + " (Feels like: " + feelsStr + ")", 50, BLUE + "║  " + RESET, BLUE + "  ║" + RESET);
        printBoxRow(weatherIcon + "  Condition   : " + BOLD + capitalize(weather.getDescription()) + RESET, 50, BLUE + "║  " + RESET, BLUE + "  ║" + RESET);
        printBoxRow("💧  Humidity    : " + CYAN + weather.getHumidity() + "%" + RESET, 50, BLUE + "║  " + RESET, BLUE + "  ║" + RESET);
        printBoxRow("💨  Wind Speed  : " + windStr, 50, BLUE + "║  " + RESET, BLUE + "  ║" + RESET);
        out.println(BLUE + "╟" + CARD_BAR_SINGLE + "╢" + RESET);
        printBoxRow("🕒  Fetched at  : " + GRAY + timestamp + " | Smart Cache" + RESET, 50, BLUE + "║  " + RESET, BLUE + "  ║" + RESET);
        out.println(BLUE + "╚" + CARD_BAR_DOUBLE + "╝" + RESET);
    }

    private void displaySearchHistory() {
        List<SearchRecord> history = weatherService.getRecentSearches(30);
        if (history.isEmpty()) {
            out.println(YELLOW + "ℹ️ No recent searches found." + RESET);
            return;
        }

        out.println("\n" + BLUE + "╔" + TABLE_BAR_DOUBLE + "╗" + RESET);
        printBoxRow("📜  " + BOLD + "RECENT SEARCH HISTORY (Last " + history.size() + " searches)" + RESET, 70, BLUE + "║ " + RESET, BLUE + " ║" + RESET);
        out.println(BLUE + "╠" + TABLE_BAR_DOUBLE + "╣" + RESET);
        String header = String.format("%-19s | %-16s | %-8s | %-18s", "Searched At", "City", "Temp", "Condition");
        printBoxRow(GRAY + header + RESET, 70, BLUE + "║ " + RESET, BLUE + " ║" + RESET);
        out.println(BLUE + "╟" + TABLE_BAR_SINGLE + "╢" + RESET);

        for (SearchRecord rec : history) {
            String timeStr = rec.searchedAt().atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER);
            String tempStr = formatTemperature(rec.temperature());
            String row = String.format(Locale.US, "%-19s | %-16s | %-8s | %-18s",
                    timeStr, truncate(rec.cityName(), 16), tempStr, capitalize(truncate(rec.condition(), 18)));
            printBoxRow(row, 70, BLUE + "║ " + RESET, BLUE + " ║" + RESET);
        }
        out.println(BLUE + "╚" + TABLE_BAR_DOUBLE + "╝" + RESET);
    }

    private void handleHomeCityDashboard() {
        if (homeCityService == null) {
            out.println(YELLOW + "⚠️ Home City service is not available." + RESET);
            return;
        }

        Optional<HomeCityConfig> optConfig = homeCityService.getHomeCityConfig();
        if (optConfig.isEmpty()) {
            out.println("\n" + CYAN + "╔" + CARD_BAR_DOUBLE + "╗" + RESET);
            printBoxRow("       " + BOLD + "🏠 HOME CITY & AI FORECAST DASHBOARD" + RESET, 50, CYAN + "║  " + RESET, CYAN + "  ║" + RESET);
            out.println(CYAN + "╚" + CARD_BAR_DOUBLE + "╝" + RESET);
            out.println(YELLOW + "🏠 No Home City configured yet." + RESET);
            out.print("Enter your home city name (e.g. Pune, Tokyo, London) > ");
            String cityName = scanner.nextLine().trim();
            if (cityName.isEmpty() || cityName.equalsIgnoreCase("back") || cityName.equalsIgnoreCase("b")) {
                return;
            }
            setupHomeCityAndTrainAi(cityName);
        } else {
            // Already configured: display dashboard, live weather, and 7-day forecast
            displayPredictions();
        }

        while (true) {
            out.println("\n" + BOLD + "Home City Options:" + RESET);
            out.println("  1. " + CYAN + "Change Home City (Auto-syncs 10-year data & trains AI)" + RESET);
            out.println("  2. " + YELLOW + "Refresh / Retrain AI Predictions" + RESET);
            out.println("  3. " + GREEN + "View Live Weather & 7-Day Forecast" + RESET);
            out.println("  4. " + GRAY + "Back to Main Search" + RESET);
            out.print("Select an option (1-4) > ");

            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                out.print("Enter new home city name (e.g. London, Tokyo, Pune) > ");
                String newCity = scanner.nextLine().trim();
                if (!newCity.isEmpty() && !newCity.equalsIgnoreCase("back") && !newCity.equalsIgnoreCase("b")) {
                    setupHomeCityAndTrainAi(newCity);
                }
            } else if (choice.equals("2")) {
                executeAiTrainingWithProgress();
                displayPredictions();
            } else if (choice.equals("3")) {
                displayPredictions();
            } else if (choice.equals("4") || choice.equalsIgnoreCase("back") || choice.equalsIgnoreCase("b")) {
                break;
            } else {
                out.println(YELLOW + "⚠️ Invalid choice. Please choose 1, 2, 3, or 4." + RESET);
            }
        }
    }

    private void setupHomeCityAndTrainAi(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return;
        }

        try {
            // Step 1: Geocode
            out.println(GRAY + "⏳ [1/3] Geocoding \"" + cityName + "\" via Open-Meteo API..." + RESET);
            HomeCityConfig cfg = homeCityService.configureHomeCity(cityName);
            out.println(GREEN + "✅ Location resolved: " + BOLD + cfg.cityName()
                    + " (" + cfg.latitude() + "°, " + cfg.longitude() + "°)" + RESET);

            // Step 2: Download 10 years of hourly historical climate data
            out.println(GRAY + "📥 [2/3] Fetching 10 years of hourly climate data (~87,600 records) from Open-Meteo..." + RESET);
            out.println(GRAY + "   (Direct archive API • No API key required • Est: ~2-4s)..." + RESET);
            long syncStart = System.currentTimeMillis();
            int syncedCount = homeCityService.syncHistoricalData(10);
            long syncDuration = Math.max(1, (System.currentTimeMillis() - syncStart) / 1000);
            out.println(GREEN + "✅ Successfully saved " + BOLD + String.format(Locale.US, "%,d", syncedCount)
                    + " hourly records" + RESET + GREEN + " into SQLite cache! (" + syncDuration + "s)" + RESET);

            // Step 3: Run AI training and predictions
            executeAiTrainingWithProgress();

            // Step 4: Immediately display the newly trained predictions & live weather
            displayPredictions();

        } catch (WeatherAppException e) {
            out.println(RED + "❌ Failed during setup: " + e.getMessage() + RESET);
        } catch (Exception e) {
            out.println(RED + "❌ Unexpected error during setup: " + e.getMessage() + RESET);
        }
    }

    private void executeAiTrainingWithProgress() {
        out.println(CYAN + "🧠 [3/3] Training Facebook Prophet AI model & Scikit-Learn engines (Temp, Humidity & Rain)..." + RESET);
        out.println(GRAY + "   Estimated time: ~25-30 seconds • Analyzing 10-year hourly climate dataset..." + RESET);

        long start = System.currentTimeMillis();
        int lastMilestone = -1;

        try {
            Process process = homeCityService.startAiPipelineProcess();
            if (process == null) {
                out.println(YELLOW + "ℹ️ AI training process skipped or unavailable." + RESET);
                return;
            }

            // Consume process output in background to prevent OS buffer deadlock
            StringBuilder outputLog = new StringBuilder();
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputLog.append(line).append("\n");
                    }
                } catch (IOException ignored) {
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            // Print first milestone immediately
            out.println(GRAY + "   ⏳ Initializing Prophet & Scikit-Learn training pipelines..." + RESET);

            // Print clean milestone updates every ~8 seconds
            while (process.isAlive()) {
                long elapsedSec = (System.currentTimeMillis() - start) / 1000;
                int milestone = (int) (elapsedSec / 8);

                if (milestone > lastMilestone) {
                    lastMilestone = milestone;
                    if (elapsedSec >= 8 && elapsedSec < 16) {
                        out.println(GRAY + "   ⏳ Training Prophet temperature model on 10-year hourly cycles... (" + elapsedSec + "s elapsed)" + RESET);
                    } else if (elapsedSec >= 16 && elapsedSec < 24) {
                        out.println(GRAY + "   ⏳ Training Prophet relative humidity model on diurnal cycles... (" + elapsedSec + "s elapsed)" + RESET);
                    } else if (elapsedSec >= 24 && elapsedSec < 32) {
                        out.println(GRAY + "   ⏳ Training Scikit-Learn rain probability & precipitation engines... (" + elapsedSec + "s elapsed)" + RESET);
                    } else if (elapsedSec >= 32) {
                        out.println(GRAY + "   ⏳ Projecting 7-day multi-variate forecast & saving to SQLite... (" + elapsedSec + "s elapsed)" + RESET);
                    }
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            int exitCode = process.waitFor();
            long totalSec = (System.currentTimeMillis() - start) / 1000;

            if (exitCode == 0) {
                out.println(GREEN + "🎉 AI models trained: Temperature, Humidity & Rain predictions ready! (" + totalSec + "s)" + RESET);
            } else {
                out.println(RED + "❌ AI training failed with exit code " + exitCode + " (" + totalSec + "s)" + RESET);
                out.println(GRAY + "Details:\n" + outputLog + RESET);
            }

        } catch (IOException | InterruptedException e) {
            out.println(RED + "❌ Failed to run AI pipeline: " + e.getMessage() + RESET);
        }
    }

    private void displayPredictions() {
        Optional<HomeCityConfig> optConfig = homeCityService.getHomeCityConfig();
        if (optConfig.isEmpty()) {
            out.println(YELLOW + "⚠️ Please configure your home city first." + RESET);
            return;
        }

        HomeCityConfig cfg = optConfig.get();
        int recordCount = homeCityService.getRecordCount();
        String syncStr = cfg.lastSyncedAt() != null
                ? cfg.lastSyncedAt().atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER)
                : "Never";

        // Home City Summary Card
        out.println("\n" + CYAN + "╔" + CARD_BAR_DOUBLE + "╗" + RESET);
        printBoxRow("       " + BOLD + "🏠 HOME CITY & AI FORECAST DASHBOARD" + RESET, 50, CYAN + "║  " + RESET, CYAN + "  ║" + RESET);
        out.println(CYAN + "╠" + CARD_BAR_DOUBLE + "╣" + RESET);
        printBoxRow("City          : " + BOLD + cfg.cityName() + RESET, 50, CYAN + "║  " + RESET, CYAN + "  ║" + RESET);
        printBoxRow("Coordinates   : " + String.format(Locale.US, "%.2f°, %.2f°", cfg.latitude(), cfg.longitude()), 50, CYAN + "║  " + RESET, CYAN + "  ║" + RESET);
        printBoxRow("Hourly Records: " + GREEN + String.format(Locale.US, "%,d rows", recordCount) + RESET, 50, CYAN + "║  " + RESET, CYAN + "  ║" + RESET);
        printBoxRow("Last Synced   : " + GRAY + syncStr + RESET, 50, CYAN + "║  " + RESET, CYAN + "  ║" + RESET);
        out.println(CYAN + "╚" + CARD_BAR_DOUBLE + "╝" + RESET);

        // 1. Fetch & Display Live Current Weather for Home City
        try {
            Weather liveWeather = weatherService.getWeather(cfg.cityName());
            displayWeatherCard(liveWeather);
        } catch (WeatherAppException e) {
            out.println(GRAY + "ℹ️ Note: Live weather temporarily unavailable (" + e.getMessage() + ")" + RESET);
        }

        // 2. Fetch & Display Future AI Predictions
        List<WeatherPrediction> predictions = homeCityService.getPredictions(LocalDate.now());
        if (predictions.isEmpty()) {
            out.println(YELLOW + "\nℹ️ No future AI predictions found yet for " + cfg.cityName() + "." + RESET);
            out.println(GRAY + "💡 Tip: Select option 2 to train the AI model." + RESET);
            return;
        }

        out.println("\n" + YELLOW + "╔" + TABLE_BAR_DOUBLE + "╗" + RESET);
        printBoxRow("🤖  " + BOLD + "AI WEATHER PREDICTIONS (Temp, Humidity & Rain • 7 Days)" + RESET, 70, YELLOW + "║ " + RESET, YELLOW + " ║" + RESET);
        out.println(YELLOW + "╠" + TABLE_BAR_DOUBLE + "╣" + RESET);
        String predHeader = padRight("Date", 10) + " | "
                + padRight("Temp (Avg/Range)", 16) + " | "
                + padRight("Humidity", 8) + " | "
                + padRight("Rain %", 10) + " | "
                + padRight("Summary", 14);
        printBoxRow(GRAY + predHeader + RESET, 70, YELLOW + "║ " + RESET, YELLOW + " ║" + RESET);
        out.println(YELLOW + "╟" + TABLE_BAR_SINGLE + "╢" + RESET);

        for (WeatherPrediction p : predictions) {
            String tempAvg = formatTemperature(p.predictedTempAvg());
            String tempRange = String.format(Locale.US, "%s (%.0f-%.0f%s)",
                    tempAvg,
                    isMetric ? p.predictedTempMin() : (p.predictedTempMin() * 9.0 / 5.0 + 32.0),
                    isMetric ? p.predictedTempMax() : (p.predictedTempMax() * 9.0 / 5.0 + 32.0),
                    isMetric ? "°C" : "°F");

            String humStr = String.format(Locale.US, "💧 %.0f%%", p.predictedHumidity());

            String rainStr;
            if (p.predictedRainProb() >= 60.0) {
                rainStr = String.format(Locale.US, "🌧️ %.0f%%", p.predictedRainProb());
            } else if (p.predictedRainProb() >= 25.0) {
                rainStr = String.format(Locale.US, "🌦️ %.0f%%", p.predictedRainProb());
            } else {
                rainStr = String.format(Locale.US, "☀️ %.0f%%", p.predictedRainProb());
            }

            String summary = p.conditionSummary() != null ? p.conditionSummary() : "Normal";
            String row = padRight(p.forecastDate().toString(), 10) + " | "
                    + padRight(tempRange, 16) + " | "
                    + padRight(humStr, 8) + " | "
                    + padRight(rainStr, 10) + " | "
                    + padRight(truncate(summary, 14), 14);
            printBoxRow(row, 70, YELLOW + "║ " + RESET, YELLOW + " ║" + RESET);
        }
        out.println(YELLOW + "╚" + TABLE_BAR_DOUBLE + "╝" + RESET);
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
            return CYAN; // Cold
        } else if (celsius <= 25.0) {
            return GREEN; // Mild / Pleasant
        } else if (celsius <= 32.0) {
            return YELLOW; // Warm
        } else {
            return RED; // Hot
        }
    }

    private String getWeatherIcon(String description) {
        if (description == null)
            return "🌤️";
        String lower = description.toLowerCase();
        if (lower.contains("thunder") || lower.contains("storm"))
            return "⛈️";
        if (lower.contains("drizzle") || lower.contains("rain"))
            return "🌧️";
        if (lower.contains("snow") || lower.contains("ice") || lower.contains("sleet"))
            return "❄️";
        if (lower.contains("clear") || lower.contains("sun"))
            return "☀️";
        if (lower.contains("cloud"))
            return "⛅";
        if (lower.contains("mist") || lower.contains("fog") || lower.contains("haze") || lower.contains("smoke"))
            return "🌫️";
        return "🌤️";
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank())
            return "";
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private void printBanner() {
        out.println(BLUE + "╔" + CARD_BAR_DOUBLE + "╗" + RESET);
        printBoxRow("       " + YELLOW + "🌤️  JAVA CLI WEATHER APPLICATION" + RESET, 50, BLUE + "║  " + RESET, BLUE + "  ║" + RESET);
        printBoxRow("    " + GRAY + "Real-time weather + Smart Cache + AI Forecast" + RESET, 50, BLUE + "║  " + RESET, BLUE + "  ║" + RESET);
        out.println(BLUE + "╚" + CARD_BAR_DOUBLE + "╝" + RESET);
        out.println(BOLD + "Commands:" + RESET);
        out.println("  • " + CYAN + "<city name>" + RESET + "  : View weather (e.g., 'London', 'Tokyo', 'Pune')");
        out.println("  • " + YELLOW + "'history'" + RESET + "    : View recent 30 search history");
        out.println("  • " + GREEN + "'home'" + RESET + "       : Open Home City & AI Forecast dashboard");
        out.println("  • " + YELLOW + "'unit'" + RESET + " / " + YELLOW + "'f'" + RESET
                + "   : Toggle Celsius (°C) / Fahrenheit (°F)");
        out.println("  • " + GRAY + "'clear cache'" + RESET + ": Clear cached weather data");
        out.println("  • " + RED + "'exit'" + RESET + " / " + RED + "'q'" + RESET + "   : Exit the application");
    }

    private void printExitMessage() {
        out.println("\n" + GREEN + "👋 Goodbye! Thank you for using Java Weather CLI. Have a wonderful day!" + RESET);
    }
}
