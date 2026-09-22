package com.princeblue.weather.service;

import java.util.Objects;

import com.princeblue.weather.client.ApiClient;
import com.princeblue.weather.exception.WeatherAppException;
import com.princeblue.weather.model.Weather;

public class WeatherService {
    private final ApiClient apiClient;
    private final WeatherMapper weatherMapper;

    public WeatherService(ApiClient apiClient, WeatherMapper weatherMapper) {
        this.apiClient = Objects.requireNonNull(apiClient, "apiClient must not be null");
        this.weatherMapper = Objects.requireNonNull(weatherMapper, "weatherMapper must not be null");
    }

    public Weather getWeather(String cityName) throws WeatherAppException {
        if (cityName == null || cityName.isBlank()) {
            throw new IllegalArgumentException("City name cannot be empty");
        }
        String cleanCity = cityName.trim();
        String rawJson = apiClient.fetchWeatherData(cleanCity);
        return weatherMapper.map(rawJson);
    }

}
