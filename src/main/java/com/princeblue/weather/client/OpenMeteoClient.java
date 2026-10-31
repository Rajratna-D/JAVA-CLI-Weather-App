package com.princeblue.weather.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.princeblue.weather.exception.ApiUnavailableException;
import com.princeblue.weather.exception.CityNotFoundException;
import com.princeblue.weather.exception.WeatherAppException;
import com.princeblue.weather.model.HourlyWeatherRecord;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class OpenMeteoClient {

    public record GeocodingResult(String name, double latitude, double longitude) {
    }

    private final HttpClient httpClient;

    public OpenMeteoClient() {
        this(HttpClient.newHttpClient());
    }

    public OpenMeteoClient(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    /**
     * Resolves a city name to its latitude and longitude coordinates.
     */
    public GeocodingResult geocodeCity(String cityName) throws WeatherAppException {
        if (cityName == null || cityName.isBlank()) {
            throw new IllegalArgumentException("City name cannot be blank");
        }

        String encodedCity = URLEncoder.encode(cityName.trim(), StandardCharsets.UTF_8);
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedCity + "&count=1";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ApiUnavailableException("Open-Meteo Geocoding failed with status: " + response.statusCode());
            }

            JsonElement rootElement = JsonParser.parseString(response.body());
            if (!rootElement.isJsonObject()) {
                throw new ApiUnavailableException("Open-Meteo Geocoding returned unexpected payload.");
            }

            JsonObject json = rootElement.getAsJsonObject();
            if (!json.has("results") || !json.get("results").isJsonArray() || json.getAsJsonArray("results").isEmpty()) {
                throw new CityNotFoundException("City not found in geocoding: " + cityName);
            }

            JsonObject firstResult = json.getAsJsonArray("results").get(0).getAsJsonObject();
            String name = firstResult.has("name") && !firstResult.get("name").isJsonNull()
                    ? firstResult.get("name").getAsString()
                    : cityName.trim();
            double lat = firstResult.get("latitude").getAsDouble();
            double lon = firstResult.get("longitude").getAsDouble();

            return new GeocodingResult(name, lat, lon);

        } catch (CityNotFoundException | ApiUnavailableException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiUnavailableException("Geocoding request was interrupted", e);
        } catch (IOException | RuntimeException e) {
            throw new ApiUnavailableException("Unable to communicate with Open-Meteo Geocoding API", e);
        }
    }

    /**
     * Fetches historical hourly weather data for the specified date range.
     * Uses Locale.ROOT to guarantee dot-separated decimal coordinates worldwide.
     */
    public List<HourlyWeatherRecord> fetchHistoricalHourlyData(
            double latitude, double longitude, LocalDate startDate, LocalDate endDate) throws WeatherAppException {

        String url = String.format(Locale.ROOT,
                "https://archive-api.open-meteo.com/v1/archive?latitude=%.4f&longitude=%.4f&start_date=%s&end_date=%s&hourly=temperature_2m,relative_humidity_2m,precipitation",
                latitude, longitude, startDate, endDate);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60)) // Allow up to 60s for large multi-year payloads
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ApiUnavailableException(
                        "Open-Meteo Archive API failed with status: " + response.statusCode());
            }

            JsonElement rootElement = JsonParser.parseString(response.body());
            if (!rootElement.isJsonObject()) {
                throw new ApiUnavailableException("Open-Meteo response is not a valid JSON object.");
            }

            JsonObject json = rootElement.getAsJsonObject();
            if (!json.has("hourly") || !json.get("hourly").isJsonObject()) {
                throw new ApiUnavailableException("Open-Meteo response missing 'hourly' data field.");
            }

            JsonObject hourly = json.getAsJsonObject("hourly");
            if (!hourly.has("time") || !hourly.get("time").isJsonArray()) {
                throw new ApiUnavailableException("Open-Meteo response missing 'hourly.time' data field.");
            }

            JsonArray times = hourly.getAsJsonArray("time");
            JsonArray temps = hourly.has("temperature_2m") && hourly.get("temperature_2m").isJsonArray()
                    ? hourly.getAsJsonArray("temperature_2m") : null;
            JsonArray hums = hourly.has("relative_humidity_2m") && hourly.get("relative_humidity_2m").isJsonArray()
                    ? hourly.getAsJsonArray("relative_humidity_2m") : null;
            JsonArray precs = hourly.has("precipitation") && hourly.get("precipitation").isJsonArray()
                    ? hourly.getAsJsonArray("precipitation") : null;

            int size = times.size();
            if (temps != null) size = Math.min(size, temps.size());
            if (hums != null) size = Math.min(size, hums.size());
            if (precs != null) size = Math.min(size, precs.size());

            List<HourlyWeatherRecord> records = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                String timeStr = times.get(i).getAsString();
                LocalDateTime dt = LocalDateTime.parse(timeStr);

                double temp = getDoubleOrDefault(temps != null ? temps.get(i) : null, 0.0);
                double hum = getDoubleOrDefault(hums != null ? hums.get(i) : null, 0.0);
                double prec = getDoubleOrDefault(precs != null ? precs.get(i) : null, 0.0);

                records.add(new HourlyWeatherRecord(dt, temp, hum, prec));
            }

            return records;

        } catch (ApiUnavailableException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiUnavailableException("Historical data fetch was interrupted", e);
        } catch (IOException | RuntimeException e) {
            throw new ApiUnavailableException("Unable to fetch historical data from Open-Meteo", e);
        }
    }

    private double getDoubleOrDefault(JsonElement element, double defaultValue) {
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }
        return element.getAsDouble();
    }
}
