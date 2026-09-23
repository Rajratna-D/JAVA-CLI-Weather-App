package com.princeblue.weather.client;

import com.princeblue.weather.config.AppConfig;
import com.princeblue.weather.exception.ApiUnavailableException;
import com.princeblue.weather.exception.CityNotFoundException;
import com.princeblue.weather.exception.InvalidApiKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenWeatherClientTest {

    private HttpClient httpClient;
    private AppConfig config;
    private OpenWeatherClient client;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        config = mock(AppConfig.class);

        when(config.getApiKey()).thenReturn("test-api-key");

        client = new OpenWeatherClient(httpClient, config);
    }

    @Test
    void shouldReturnResponseBodyWhenStatusIs200() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"name\":\"Pune\"}");

        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        String result = client.fetchWeatherData("Pune");

        assertEquals("{\"name\":\"Pune\"}", result);
    }

    @Test
    void shouldThrowInvalidApiKeyExceptionWhenStatusIs401() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(401);

        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        assertThrows(
                InvalidApiKeyException.class,
                () -> client.fetchWeatherData("Pune"));
    }

    @Test
    void shouldThrowCityNotFoundExceptionWhenStatusIs404() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(404);

        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        assertThrows(
                CityNotFoundException.class,
                () -> client.fetchWeatherData("InvalidCity"));
    }

    @Test
    void shouldThrowApiUnavailableExceptionWhenStatusIs500() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(500);

        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        assertThrows(
                ApiUnavailableException.class,
                () -> client.fetchWeatherData("Pune"));
    }

    @Test
    void shouldThrowApiUnavailableExceptionWhenNetworkFails() throws Exception {
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.io.IOException("Network error"));

        assertThrows(
                ApiUnavailableException.class,
                () -> client.fetchWeatherData("Pune"));
    }

    @Test
    void shouldHandleCityNamesWithSpaces() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"name\":\"New York\"}");

        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        String result = client.fetchWeatherData("New York");

        assertEquals("{\"name\":\"New York\"}", result);
    }

    @Test
    void shouldInitializeWithConvenienceConstructor() {
        OpenWeatherClient defaultClient = new OpenWeatherClient(config);
        assertNotNull(defaultClient);
    }
}