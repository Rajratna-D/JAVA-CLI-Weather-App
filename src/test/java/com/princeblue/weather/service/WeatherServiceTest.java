package com.princeblue.weather.service;

import com.princeblue.weather.client.ApiClient;
import com.princeblue.weather.exception.CityNotFoundException;
import com.princeblue.weather.model.Weather;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

public class WeatherServiceTest {

    private final ApiClient mockClient = Mockito.mock(ApiClient.class);
    private final WeatherMapper mapper = new WeatherMapper();
    private final WeatherService service = new WeatherService(mockClient, mapper);

    @Test
    void getWeather_withValidCity_returnsWeatherObject() throws Exception {
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
    void getWeather_withBlankCity_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> service.getWeather("  "));
        verifyNoInteractions(mockClient);
    }

    @Test
    void getWeather_whenCityNotFound_throwsCityNotFoundException() throws Exception {
        when(mockClient.fetchWeatherData("Atlantis"))
                .thenThrow(new CityNotFoundException("City not found: Atlantis"));
        assertThrows(CityNotFoundException.class, () -> service.getWeather("Atlantis"));
    }
}
