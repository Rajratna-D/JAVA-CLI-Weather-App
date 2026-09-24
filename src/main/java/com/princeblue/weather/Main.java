package com.princeblue.weather;

import com.princeblue.weather.client.OpenWeatherClient;
import com.princeblue.weather.config.AppConfig;
import com.princeblue.weather.service.WeatherMapper;
import com.princeblue.weather.service.WeatherService;
import com.princeblue.weather.ui.ConsoleUI;

public class Main {
    public static void main(String[] args) {
        try {
            AppConfig config = new AppConfig();
            OpenWeatherClient client = new OpenWeatherClient(config);
            WeatherMapper mapper = new WeatherMapper();
            WeatherService service = new WeatherService(client, mapper);

            ConsoleUI ui = new ConsoleUI(service);
            ui.start();

        } catch (IllegalStateException e) {
            System.err.println("\n❌ Configuration Error: " + e.getMessage());
            System.err.println("💡 Tip: Copy 'src/main/resources/config.properties.example' to 'config.properties' and set your OpenWeather API key.\n");
            System.exit(1);

        } catch (Exception e) {
            System.err.println("\n❌ Fatal Application Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
