package com.princeblue.weather.service;

import com.princeblue.weather.client.OpenMeteoClient;
import com.princeblue.weather.model.HomeCityConfig;
import com.princeblue.weather.model.HourlyWeatherRecord;
import com.princeblue.weather.model.WeatherPrediction;
import com.princeblue.weather.repository.WeatherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class HomeCityServiceTest {

    private WeatherRepository repository;
    private OpenMeteoClient openMeteoClient;
    private HomeCityService service;

    @BeforeEach
    void setUp() {
        repository = mock(WeatherRepository.class);
        openMeteoClient = mock(OpenMeteoClient.class);
        service = new HomeCityService(repository, openMeteoClient);
    }

    @Test
    void configureHomeCity_resolvesAndSavesConfig() throws Exception {
        when(openMeteoClient.geocodeCity("Pune"))
                .thenReturn(new OpenMeteoClient.GeocodingResult("Pune", 18.52, 73.85));

        HomeCityConfig config = service.configureHomeCity("Pune");

        assertNotNull(config);
        assertEquals("Pune", config.cityName());
        assertEquals(18.52, config.latitude());
        assertEquals(73.85, config.longitude());
        assertNull(config.lastSyncedAt());

        verify(repository).saveHomeCityConfig(config);
    }

    @Test
    void configureHomeCity_withBlankCity_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> service.configureHomeCity(""));
        assertThrows(IllegalArgumentException.class, () -> service.configureHomeCity(null));
        assertThrows(IllegalArgumentException.class, () -> service.configureHomeCity("   "));
    }

    @Test
    void syncHistoricalData_fetchesAndSavesRecords() throws Exception {
        HomeCityConfig existingConfig = new HomeCityConfig("Pune", 18.52, 73.85, null);
        when(repository.getHomeCityConfig()).thenReturn(Optional.of(existingConfig));

        List<HourlyWeatherRecord> mockRecords = List.of(
                new HourlyWeatherRecord(LocalDateTime.of(2026, 1, 1, 0, 0), 22.0, 60.0, 0.0),
                new HourlyWeatherRecord(LocalDateTime.of(2026, 1, 1, 1, 0), 21.5, 62.0, 0.0)
        );
        when(openMeteoClient.fetchHistoricalHourlyData(eq(18.52), eq(73.85), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(mockRecords);

        int count = service.syncHistoricalData(5);

        assertEquals(2, count);
        verify(repository).saveHourlyWeatherBatch(mockRecords);
        verify(repository).saveHomeCityConfig(argThat(cfg -> cfg.lastSyncedAt() != null));
    }

    @Test
    void syncHistoricalData_whenUnconfigured_throwsIllegalStateException() {
        when(repository.getHomeCityConfig()).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.syncHistoricalData(5));
    }

    @Test
    void findPythonExecutable_returnsString() {
        String pythonExe = service.findPythonExecutable();
        assertNotNull(pythonExe);
        assertFalse(pythonExe.isBlank());
    }

    @Test
    void getPredictions_delegatesToRepository() {
        LocalDate now = LocalDate.now();
        List<WeatherPrediction> expected = List.of(
                new WeatherPrediction(now, 25.0, 20.0, 30.0, 70.0, 10.0, 0.0, "Sunny", Instant.now())
        );
        when(repository.getPredictions(now)).thenReturn(expected);

        List<WeatherPrediction> actual = service.getPredictions(now);
        assertEquals(expected, actual);
        verify(repository).getPredictions(now);
    }
}

