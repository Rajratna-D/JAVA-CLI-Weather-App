package com.princeblue.weather;

import com.princeblue.weather.client.OpenMeteoClient;
import com.princeblue.weather.client.OpenWeatherClient;
import com.princeblue.weather.config.AppConfig;
import com.princeblue.weather.repository.SqliteWeatherRepository;
import com.princeblue.weather.service.HomeCityService;
import com.princeblue.weather.service.WeatherMapper;
import com.princeblue.weather.service.WeatherService;
import com.princeblue.weather.ui.ConsoleUI;

public class Main {
    public static void main(String[] args) {
        try {
            // 1. Configuration & Clients
            AppConfig config = new AppConfig();
            OpenWeatherClient openWeatherClient = new OpenWeatherClient(config);
            OpenMeteoClient openMeteoClient = new OpenMeteoClient();
            WeatherMapper mapper = new WeatherMapper();

            // 2. Database Repository
            SqliteWeatherRepository repository = new SqliteWeatherRepository();

            // 3. Service Layer
            WeatherService weatherService = new WeatherService(openWeatherClient, mapper, repository);
            HomeCityService homeCityService = new HomeCityService(repository, openMeteoClient);

            // 4. UI Layer
            ConsoleUI ui = new ConsoleUI(weatherService, homeCityService);
            ui.start();

        } catch (IllegalStateException e) {
            System.err.println("\n❌ Configuration Error: " + e.getMessage());
            System.err.println(
                    "💡 Tip: Copy 'src/main/resources/config.properties.example' to 'config.properties' and set your OpenWeather API key.\n");
            System.exit(1);

        } catch (Exception e) {
            System.err.println("\n❌ Fatal Application Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
