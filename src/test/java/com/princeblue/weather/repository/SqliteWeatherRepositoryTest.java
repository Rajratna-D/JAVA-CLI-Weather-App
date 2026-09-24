package com.princeblue.weather.repository;

import com.princeblue.weather.model.HomeCityConfig;
import com.princeblue.weather.model.HourlyWeatherRecord;
import com.princeblue.weather.model.Location;
import com.princeblue.weather.model.SearchRecord;
import com.princeblue.weather.model.Weather;
import com.princeblue.weather.model.WeatherPrediction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class SqliteWeatherRepositoryTest {

    @TempDir
    Path tempDir;

    private SqliteWeatherRepository repository;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test_weather.db");
        repository = new SqliteWeatherRepository("jdbc:sqlite:" + dbPath.toAbsolutePath());
    }

    @Test
    void saveAndGetCachedWeather_returnsCachedWeather() {
        Location location = new Location("London", "GB", 51.5, -0.1);
        Weather weather = new Weather(location, 18.5, 18.0, 65, "clear sky", 3.2);

        repository.saveWeatherCache(weather);

        Optional<Weather> cached = repository.getCachedWeather("London", 15);
        assertTrue(cached.isPresent());
        assertEquals("London", cached.get().getLocation().getCity());
        assertEquals(18.5, cached.get().getTemperature());
        assertEquals("clear sky", cached.get().getDescription());
    }

    @Test
    void getCachedWeather_isCaseInsensitive() {
        Location location = new Location("Pune", "IN", 18.52, 73.85);
        Weather weather = new Weather(location, 27.0, 28.0, 60, "scattered clouds", 2.1);

        repository.saveWeatherCache(weather);

        assertTrue(repository.getCachedWeather("pune", 15).isPresent());
        assertTrue(repository.getCachedWeather("PUNE", 15).isPresent());
        assertTrue(repository.getCachedWeather("Pune", 15).isPresent());
    }

    @Test
    void clearCache_removesAllCachedWeather() {
        Location location = new Location("Tokyo", "JP", 35.6, 139.6);
        Weather weather = new Weather(location, 22.0, 22.0, 50, "sunny", 1.5);

        repository.saveWeatherCache(weather);
        assertTrue(repository.getCachedWeather("Tokyo", 15).isPresent());

        repository.clearCache();
        assertTrue(repository.getCachedWeather("Tokyo", 15).isEmpty());
    }

    @Test
    void addSearchHistory_enforcesRollingWindow() {
        Location location = new Location("City", "IN", 0.0, 0.0);

        // Insert 40 searches
        for (int i = 1; i <= 40; i++) {
            Weather weather = new Weather(
                    new Location("City" + i, "IN", 0.0, 0.0),
                    20.0 + i, 20.0 + i, 50, "clear", 2.0);
            repository.addSearchHistory(weather);
        }

        List<SearchRecord> recent = repository.getRecentSearches(50);
        // The rolling window prunes to 35 newest entries
        assertTrue(recent.size() <= 35, "History should be capped to 35 entries");
        assertEquals("City40", recent.get(0).cityName(), "Newest search should be first");
    }

    @Test
    void saveAndGetHomeCityConfig_persistsConfig() {
        HomeCityConfig config = new HomeCityConfig("Pune", 18.52, 73.85, null);
        repository.saveHomeCityConfig(config);

        Optional<HomeCityConfig> retrieved = repository.getHomeCityConfig();
        assertTrue(retrieved.isPresent());
        assertEquals("Pune", retrieved.get().cityName());
        assertEquals(18.52, retrieved.get().latitude());
        assertEquals(73.85, retrieved.get().longitude());
        assertNull(retrieved.get().lastSyncedAt());

        // Update with sync timestamp
        Instant now = Instant.now();
        HomeCityConfig updated = new HomeCityConfig("Pune", 18.52, 73.85, now);
        repository.saveHomeCityConfig(updated);

        Optional<HomeCityConfig> updatedRetrieved = repository.getHomeCityConfig();
        assertTrue(updatedRetrieved.isPresent());
        assertNotNull(updatedRetrieved.get().lastSyncedAt());
    }

    @Test
    void saveHourlyWeatherBatch_persistsAndCountsRecords() {
        List<HourlyWeatherRecord> batch = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2024, 1, 1, 0, 0);

        for (int i = 0; i < 100; i++) {
            batch.add(new HourlyWeatherRecord(base.plusHours(i), 20.0 + (i % 10), 60.0, 0.0));
        }

        repository.saveHourlyWeatherBatch(batch);
        assertEquals(100, repository.getHourlyRecordCount());

        List<HourlyWeatherRecord> queried = repository.getHourlyWeatherData(
                base, base.plusHours(24));
        assertEquals(25, queried.size()); // 0 to 24 inclusive = 25 hours
    }

    @Test
    void saveAndGetPredictions_persistsPredictions() {
        LocalDate today = LocalDate.now();
        List<WeatherPrediction> predictions = List.of(
                new WeatherPrediction(today, 25.0, 20.0, 30.0, "Sunny", Instant.now()),
                new WeatherPrediction(today.plusDays(1), 26.0, 21.0, 31.0, "Partly Cloudy", Instant.now()));

        repository.savePredictions(predictions);

        List<WeatherPrediction> retrieved = repository.getPredictions(today);
        assertEquals(2, retrieved.size());
        assertEquals(25.0, retrieved.get(0).predictedTempAvg());
        assertEquals("Sunny", retrieved.get(0).conditionSummary());
    }
}
