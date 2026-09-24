package com.princeblue.weather.service;

import com.princeblue.weather.client.OpenMeteoClient;
import com.princeblue.weather.exception.WeatherAppException;
import com.princeblue.weather.model.HomeCityConfig;
import com.princeblue.weather.model.HourlyWeatherRecord;
import com.princeblue.weather.model.WeatherPrediction;
import com.princeblue.weather.repository.WeatherRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class HomeCityService {

    private final WeatherRepository repository;
    private final OpenMeteoClient openMeteoClient;

    public HomeCityService(WeatherRepository repository, OpenMeteoClient openMeteoClient) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.openMeteoClient = Objects.requireNonNull(openMeteoClient, "openMeteoClient must not be null");
    }

    /**
     * Retrieves the currently configured home city, if one exists.
     */
    public Optional<HomeCityConfig> getHomeCityConfig() {
        return repository.getHomeCityConfig();
    }

    /**
     * Sets or updates the home city by resolving its coordinates through
     * Open-Meteo.
     */
    public HomeCityConfig configureHomeCity(String cityName) throws WeatherAppException {
        if (cityName == null || cityName.isBlank()) {
            throw new IllegalArgumentException("City name cannot be blank");
        }

        OpenMeteoClient.GeocodingResult geo = openMeteoClient.geocodeCity(cityName);
        HomeCityConfig config = new HomeCityConfig(
                geo.name(),
                geo.latitude(),
                geo.longitude(),
                null // Not yet synced
        );

        repository.saveHomeCityConfig(config);
        return config;
    }

    /**
     * Downloads hourly historical weather data for the home city from Open-Meteo
     * and stores it into SQLite.
     *
     * @param yearsBack Number of years of history to download (e.g. 5 or 10)
     * @return Number of hourly records downloaded and stored
     */
    public int syncHistoricalData(int yearsBack) throws WeatherAppException {
        HomeCityConfig config = repository.getHomeCityConfig()
                .orElseThrow(() -> new IllegalStateException("Home city is not configured yet."));

        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusYears(yearsBack);

        List<HourlyWeatherRecord> records = openMeteoClient.fetchHistoricalHourlyData(
                config.latitude(),
                config.longitude(),
                startDate,
                endDate);

        repository.saveHourlyWeatherBatch(records);

        HomeCityConfig updatedConfig = new HomeCityConfig(
                config.cityName(),
                config.latitude(),
                config.longitude(),
                Instant.now());
        repository.saveHomeCityConfig(updatedConfig);

        return records.size();
    }

    /**
     * Returns the total count of hourly records in the database for the home city.
     */
    public int getRecordCount() {
        return repository.getHourlyRecordCount();
    }

    /**
     * Retrieves weather predictions on or after the specified date.
     */
    public List<WeatherPrediction> getPredictions(LocalDate fromDate) {
        return repository.getPredictions(fromDate);
    }

    /**
     * Resolves the Python executable path, checking virtualenv first, then system PATH.
     */
    public String findPythonExecutable() {
        java.io.File winVenv = new java.io.File("ml/.venv/Scripts/python.exe");
        if (winVenv.exists()) {
            return winVenv.getAbsolutePath();
        }
        java.io.File unixVenv = new java.io.File("ml/.venv/bin/python");
        if (unixVenv.exists()) {
            return unixVenv.getAbsolutePath();
        }
        return "python";
    }

    /**
     * Starts the Facebook Prophet ML training & prediction pipeline as an external process.
     */
    public Process startAiPipelineProcess() throws java.io.IOException {
        String pythonExe = findPythonExecutable();
        ProcessBuilder pb = new ProcessBuilder(pythonExe, "ml/pipeline.py");
        pb.redirectErrorStream(true);
        return pb.start();
    }

    /**
     * Checks if the trained Prophet model file exists on disk.
     */
    public boolean isAiModelAvailable() {
        java.io.File modelFile = new java.io.File("ml/models/prophet_weather_model.pkl");
        return modelFile.exists() && modelFile.length() > 0;
    }
}
