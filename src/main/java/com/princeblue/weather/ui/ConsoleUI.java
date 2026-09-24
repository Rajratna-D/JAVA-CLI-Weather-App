package com.princeblue.weather.ui;

public class ConsoleUI {
    private final WeatherService weatherService;
    private final Scanner scanner;

    public ConsoleUI(WeatherService weatherService) {
        this(weatherService, new Scanner(System.in));
    }

    public ConsoleUI(WeatherService weatherService, Scanner scanner) {
        this.weatherService = Objects.requireNonNull(weatherService);
        this.scanner = Objects.requireNonNull(scanner);
    }

    public void start() {
    System.out.println("==========================================");
    System.out.println("       🌤️  JAVA CLI WEATHER APP           ");
    System.out.println("==========================================");

    while (true) {
        System.out.print("\nEnter city name (or 'exit' to quit): ");
        String input = scanner.nextLine().trim();

        // 1. Check if user wants to quit
        if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
            System.out.println("Goodbye! Have a great day! 👋");
            break; // Exits the while loop and closes the app
        }

        // 2. Ignore empty Enter presses
        if (input.isEmpty()) {
            continue;
        }

        // 3. Fetch and display weather
        fetchAndDisplay(input);
    }

    private void fetchAndDisplay(String city) {
    try {
        Weather weather = weatherService.getWeather(city);
        displayWeatherCard(weather);

    } catch (CityNotFoundException e) {
        System.out.println("❌ " + e.getMessage());

    } catch (InvalidApiKeyException e) {
        System.out.println("⚠️ " + e.getMessage() + " Please verify config.properties.");

    } catch (ApiUnavailableException e) {
        System.out.println("📡 Network error: " + e.getMessage());

    } catch (WeatherAppException e) {
        System.out.println("❗ Unexpected error: " + e.getMessage());
    }
    
    private void displayWeatherCard(Weather weather) {
    System.out.println("------------------------------------------");
    System.out.println(" 📍 " + weather.getLocation().getCity() + ", " + weather.getLocation().getCountry());
    System.out.println(" 🌡️  Temperature: " + weather.getTemperature() + "°C (Feels like: " + weather.getFeelsLike() + "°C)");
    System.out.println(" ☁️  Condition:   " + weather.getDescription());
    System.out.println(" 💧 Humidity:    " + weather.getHumidity() + "%");
    System.out.println(" 💨 Wind Speed:  " + weather.getWindSpeed() + " m/s");
    System.out.println("------------------------------------------");
}
}
}
}

