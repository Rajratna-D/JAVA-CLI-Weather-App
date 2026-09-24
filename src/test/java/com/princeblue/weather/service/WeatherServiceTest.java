package com.princeblue.weather.service;

import com.princeblue.weather.client.ApiClient;
import com.princeblue.weather.exception.CityNotFoundException;
import com.princeblue.weather.model.Location;
import com.princeblue.weather.model.Weather;
import com.princeblue.weather.repository.WeatherRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class WeatherServiceTest {

    private final ApiClient mockClient = Mockito.mock(ApiClient.class);
    private final WeatherRepository mockRepository = Mockito.mock(WeatherRepository.class);
    private final WeatherMapper mapper = new WeatherMapper();

    @Test
    void getWeather_withValidCity_returnsWeatherObject() throws Exception {
        WeatherService service = new WeatherService(mockClient, mapper);
        String sampleJson = """
                {
                  "name": "London",
                  "weather": [{"description": "clear sky"}],
                  "main": {"temp": 15.5, "humidity": 70}
                }
                """;

        when(mockClient.fetchWeatherData("London")).thenReturn(sampleJson);
        Weather weather = service.getWeather("London");
        assertEquals("London", weather.getLocation().getCity());
        assertEquals(15.5, weather.getTemperature());
        assertEquals("clear sky", weather.getDescription());
    }

    @Test
    void getWeather_whenCacheHit_doesNotCallApiClient() throws Exception {
        WeatherService serviceWithRepo = new WeatherService(mockClient, mapper, mockRepository);
        Location location = new Location("London", "GB", 51.5, -0.1);
        Weather cachedWeather = new Weather(location, 18.0, 18.0, 60, "cached clouds", 2.0);

        when(mockRepository.getCachedWeather("London", 15)).thenReturn(Optional.of(cachedWeather));

        Weather result = serviceWithRepo.getWeather("London");

        assertEquals("London", result.getLocation().getCity());
        assertEquals("cached clouds", result.getDescription());
        // Crucial: Client should NEVER be touched on cache hit!
        verifyNoInteractions(mockClient);
        // Search history should still be updated
        verify(mockRepository).addSearchHistory(cachedWeather);
    }

    @Test
    void getWeather_whenCacheMiss_callsApiAndSavesToCache() throws Exception {
        WeatherService serviceWithRepo = new WeatherService(mockClient, mapper, mockRepository);
        String sampleJson = """
                {
                  "name": "Paris",
                  "weather": [{"description": "sunny"}],
                  "main": {"temp": 22.0, "humidity": 55}
                }
                """;

        when(mockRepository.getCachedWeather("Paris", 15)).thenReturn(Optional.empty());
        when(mockClient.fetchWeatherData("Paris")).thenReturn(sampleJson);

        Weather result = serviceWithRepo.getWeather("Paris");

        assertEquals("Paris", result.getLocation().getCity());
        verify(mockClient).fetchWeatherData("Paris");
        verify(mockRepository).saveWeatherCache(any(Weather.class));
        verify(mockRepository).addSearchHistory(any(Weather.class));
    }

    @Test
    void getWeather_withBlankCity_throwsIllegalArgumentException() {
        WeatherService service = new WeatherService(mockClient, mapper);
        assertThrows(IllegalArgumentException.class, () -> service.getWeather("  "));
        verifyNoInteractions(mockClient);
    }

    @Test
    void getWeather_whenCityNotFound_throwsCityNotFoundException() throws Exception {
        WeatherService service = new WeatherService(mockClient, mapper);
        when(mockClient.fetchWeatherData("Atlantis"))
                .thenThrow(new CityNotFoundException("City not found: Atlantis"));
        assertThrows(CityNotFoundException.class, () -> service.getWeather("Atlantis"));
    }
}
