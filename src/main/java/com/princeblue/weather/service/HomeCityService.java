package com.princeblue.weather.service;

import com.princeblue.weather.client.OpenMeteoClient;
import com.princeblue.weather.exception.WeatherAppException;
import com.princeblue.weather.model.HomeCityConfig;
import com.princeblue.weather.model.HourlyWeatherRecord;
import com.princeblue.weather.model.WeatherPrediction;
import com.princeblue.weather.repository.WeatherRepository;

import java.io.File;
import java.io.IOException;
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
     * @param yearsBack Number of years of history to download (e.g. 5 or 12)
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
     * Resolves the Python executable path, checking virtual environments (both ml/.venv and .venv),
     * then falling back to system PATH.
     */
    public String findPythonExecutable() {
        File[] candidatePaths = new File[] {
                new File("ml/.venv/Scripts/python.exe"),
                new File(".venv/Scripts/python.exe"),
                new File("ml/.venv/bin/python"),
                new File(".venv/bin/python")
        };

        for (File candidate : candidatePaths) {
            if (candidate.exists()) {
                return candidate.getAbsolutePath();
            }
        }

        return "python";
    }

    /**
     * Starts the multi-variate ML training & prediction pipeline as an external process.
     * Sets process working directory to project root and forces UTF-8 IO encoding.
     */
    public Process startAiPipelineProcess() throws IOException {
        String pythonExe = findPythonExecutable();
        File workingDir = new File(".").getAbsoluteFile();
        if (workingDir.getName().equals(".")) {
            workingDir = workingDir.getParentFile();
        }

        ProcessBuilder pb = new ProcessBuilder(pythonExe, "ml/pipeline.py");
        if (workingDir != null && workingDir.exists()) {
            pb.directory(workingDir);
        }
        pb.environment().put("PYTHONIOENCODING", "utf-8");
        pb.redirectErrorStream(true);
        return pb.start();
    }

    /**
     * Checks if trained model files exist on disk.
     */
    public boolean isAiModelAvailable() {
        File[] models = new File[] {
                new File("ml/models/prophet_temp_model.json"),
                new File("ml/models/prophet_temp_model.pkl"),
                new File("ml/models/prophet_weather_model.pkl")
        };

        for (File m : models) {
            if (m.exists() && m.length() > 0) {
                return true;
            }
        }
        return false;
    }
}
