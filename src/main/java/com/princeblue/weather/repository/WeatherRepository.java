package com.princeblue.weather.repository;

import com.princeblue.weather.model.HomeCityConfig;
import com.princeblue.weather.model.HourlyWeatherRecord;
import com.princeblue.weather.model.SearchRecord;
import com.princeblue.weather.model.Weather;
import com.princeblue.weather.model.WeatherPrediction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WeatherRepository {

    // ==========================================
    // Section A: Cache & History
    // ==========================================

    /**
     * Retrieves weather from cache if it exists and was cached within ttlMinutes.
     */
    Optional<Weather> getCachedWeather(String cityName, int ttlMinutes);

    /**
     * Saves or replaces weather data in the cache with the current timestamp.
     */
    void saveWeatherCache(Weather weather);

    /**
     * Inserts a search record and maintains a rolling window of recent searches
     * (e.g. max 30).
     */
    void addSearchHistory(Weather weather);

    /**
     * Returns up to 'limit' recent search records, newest first.
     */
    List<SearchRecord> getRecentSearches(int limit);

    /**
     * Clears all cached weather entries.
     */
    void clearCache();

    // ==========================================
    // Section B: Home City & ML Data
    // ==========================================

    /**
     * Gets the configured home city, if one has been set.
     */
    Optional<HomeCityConfig> getHomeCityConfig();

    /**
     * Saves or updates the home city configuration.
     */
    void saveHomeCityConfig(HomeCityConfig config);

    /**
     * Batch inserts historical hourly weather records (uses batching/transactions
     * for speed).
     */
    void saveHourlyWeatherBatch(List<HourlyWeatherRecord> records);

    /**
     * Retrieves historical hourly records within a date range.
     */
    List<HourlyWeatherRecord> getHourlyWeatherData(LocalDateTime from, LocalDateTime to);

    /**
     * Returns the total count of hourly records stored.
     */
    int getHourlyRecordCount();

    /**
     * Saves weather predictions produced by the Python ML model.
     */
    void savePredictions(List<WeatherPrediction> predictions);

    /**
     * Gets predictions on or after a given date.
     */
    List<WeatherPrediction> getPredictions(LocalDate fromDate);
}
