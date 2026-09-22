package com.princeblue.weather.client;

import com.princeblue.weather.config.AppConfig;

import java.net.http.HttpClient;

public class OpenWeatherClient implements ApiClient {

    private final HttpClient httpClient;
    private final AppConfig config;

    public OpenWeatherClient(HttpClient httpClient, AppConfig config) {
        this.httpClient = httpClient;
        this.config = config;
    }

    @Override
    public String get(String url) {
        return "";
    }
}