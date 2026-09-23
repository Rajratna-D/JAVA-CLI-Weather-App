package com.princeblue.weather.client;

import com.princeblue.weather.config.AppConfig;
import com.princeblue.weather.exception.ApiUnavailableException;
import com.princeblue.weather.exception.CityNotFoundException;
import com.princeblue.weather.exception.InvalidApiKeyException;
import com.princeblue.weather.exception.WeatherAppException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OpenWeatherClient implements ApiClient {

    private final HttpClient httpClient;
    private final AppConfig config;

    public OpenWeatherClient(HttpClient httpClient, AppConfig config) {
        this.httpClient = httpClient;
        this.config = config;
    }

    @Override
    public String fetchWeatherData(String cityName) throws WeatherAppException {

        String url = "https://api.openweathermap.org/data/2.5/weather"
                + "?q=" + cityName.trim()
                + "&appid=" + config.getApiKey();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            int statusCode = response.statusCode();

            if (statusCode == 200) {
                return response.body();
            }

            if (statusCode == 401) {
                throw new InvalidApiKeyException(
                        "Invalid OpenWeather API key."
                );
            }

            if (statusCode == 404) {
                throw new CityNotFoundException(
                        "City not found: " + cityName
                );
            }

            if (statusCode >= 500 && statusCode <= 599) {
                throw new ApiUnavailableException(
                        "OpenWeather API is unavailable. HTTP status: "
                                + statusCode
                );
            }

            throw new WeatherAppException(
                    "OpenWeather API request failed. HTTP status: "
                            + statusCode
            );

        } catch (InvalidApiKeyException e) {
            throw e;

        } catch (CityNotFoundException e) {
            throw e;

        } catch (ApiUnavailableException e) {
            throw e;

        } catch (WeatherAppException e) {
            throw e;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new ApiUnavailableException(
                    "OpenWeather API request was interrupted.",
                    e
            );

        } catch (IOException | RuntimeException e) {
            throw new ApiUnavailableException(
                    "Unable to communicate with OpenWeather API.",
                    e
            );
        }
    }
}
