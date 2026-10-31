package com.princeblue.weather.client;

import com.princeblue.weather.exception.ApiUnavailableException;
import com.princeblue.weather.exception.CityNotFoundException;
import com.princeblue.weather.model.HourlyWeatherRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OpenMeteoClientTest {

    private HttpClient httpClient;
    private OpenMeteoClient client;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        client = new OpenMeteoClient(httpClient);
    }

    @Test
    void geocodeCity_withValidCity_returnsCoordinates() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("""
                {
                    "results": [
                        {
                            "name": "Pune",
                            "latitude": 18.5204,
                            "longitude": 73.8567
                        }
                    ]
                }
                """);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        OpenMeteoClient.GeocodingResult result = client.geocodeCity("Pune");

        assertNotNull(result);
        assertEquals("Pune", result.name());
        assertEquals(18.5204, result.latitude(), 0.0001);
        assertEquals(73.8567, result.longitude(), 0.0001);
    }

    @Test
    void geocodeCity_withEmptyResults_throwsCityNotFoundException() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("""
                {
                    "generationtime_ms": 0.12
                }
                """);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        assertThrows(CityNotFoundException.class, () -> client.geocodeCity("NonExistentCity"));
    }

    @Test
    void geocodeCity_withBlankInput_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> client.geocodeCity(""));
        assertThrows(IllegalArgumentException.class, () -> client.geocodeCity(null));
        assertThrows(IllegalArgumentException.class, () -> client.geocodeCity("   "));
    }

    @Test
    void fetchHistoricalHourlyData_formatsCoordinatesWithDotsEvenInNonUsLocales() throws Exception {
        // Temporarily set a comma-decimal locale (e.g. Germany)
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);

            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(200);
            when(response.body()).thenReturn("""
                    {
                        "hourly": {
                            "time": ["2026-01-01T00:00", "2026-01-01T01:00"],
                            "temperature_2m": [22.5, 21.8],
                            "relative_humidity_2m": [65.0, 70.0],
                            "precipitation": [0.0, 0.5]
                        }
                    }
                    """);

            ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
            when(httpClient.send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(response);

            List<HourlyWeatherRecord> records = client.fetchHistoricalHourlyData(
                    51.5074, -0.1278, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2));

            assertEquals(2, records.size());
            assertEquals(22.5, records.get(0).temperature());
            assertEquals(65.0, records.get(0).humidity());
            assertEquals(0.0, records.get(0).precipitation());

            HttpRequest capturedRequest = requestCaptor.getValue();
            String uri = capturedRequest.uri().toString();
            // Verify latitude and longitude use dots (not commas)
            assertTrue(uri.contains("latitude=51.5074"), "URI should contain dot-separated latitude: " + uri);
            assertTrue(uri.contains("longitude=-0.1278"), "URI should contain dot-separated longitude: " + uri);

        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void fetchHistoricalHourlyData_whenStatusNot200_throwsApiUnavailableException() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(503);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        assertThrows(ApiUnavailableException.class, () -> client.fetchHistoricalHourlyData(
                18.52, 73.85, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)));
    }
}

