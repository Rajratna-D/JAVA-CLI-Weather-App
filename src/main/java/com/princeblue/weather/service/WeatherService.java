package com.princeblue.weather.service;

import com.princeblue.weather.client.ApiClient;
import com.princeblue.weather.exception.WeatherAppException;
import com.princeblue.weather.model.SearchRecord;
import com.princeblue.weather.model.Weather;
import com.princeblue.weather.repository.WeatherRepository;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class WeatherService {
    private static final int DEFAULT_TTL_MINUTES = 15;

    private final ApiClient apiClient;
    private final WeatherMapper weatherMapper;
    private final WeatherRepository repository;
    private final int cacheTtlMinutes;

    public WeatherService(ApiClient apiClient, WeatherMapper weatherMapper) {
        this(apiClient, weatherMapper, null, DEFAULT_TTL_MINUTES);
    }

    public WeatherService(ApiClient apiClient, WeatherMapper weatherMapper, WeatherRepository repository) {
        this(apiClient, weatherMapper, repository, DEFAULT_TTL_MINUTES);
    }

    public WeatherService(ApiClient apiClient, WeatherMapper weatherMapper, WeatherRepository repository,
            int cacheTtlMinutes) {
        this.apiClient = Objects.requireNonNull(apiClient, "apiClient must not be null");
        this.weatherMapper = Objects.requireNonNull(weatherMapper, "weatherMapper must not be null");
        this.repository = repository;
        this.cacheTtlMinutes = cacheTtlMinutes;
    }

    public Weather getWeather(String cityName) throws WeatherAppException {
        if (cityName == null || cityName.isBlank()) {
            throw new IllegalArgumentException("City name cannot be empty");
        }
        String cleanCity = cityName.trim();

        // 1. Check TTL Cache if repository is available
        if (repository != null) {
            Optional<Weather> cached = repository.getCachedWeather(cleanCity, cacheTtlMinutes);
            if (cached.isPresent()) {
                // Record to search history so recent searches reflect the query
                repository.addSearchHistory(cached.get());
                return cached.get();
            }
        }

        // 2. Cache miss: Fetch from live API
        String rawJson = apiClient.fetchWeatherData(cleanCity);
        Weather freshWeather = weatherMapper.map(rawJson);

        // 3. Save to Cache & Search History
        if (repository != null) {
            repository.saveWeatherCache(freshWeather);
            repository.addSearchHistory(freshWeather);
        }

        return freshWeather;
    }

    public List<SearchRecord> getRecentSearches(int limit) {
        if (repository == null) {
            return Collections.emptyList();
        }
        return repository.getRecentSearches(limit);
    }

    public void clearCache() {
        if (repository != null) {
            repository.clearCache();
        }
    }
}
