package com.princeblue.weather;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.princeblue.weather.config.AppConfig;

public class Main {
    public static void main(String[] args) throws Exception {
        AppConfig config = new AppConfig();
        String apiKey = config.getApiKey();

        String url = "https://api.openweathermap.org/data/2.5/weather?q=London&appid=" + apiKey + "&units=metric";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status Code: " + response.statusCode());
        System.out.println("Response Body: " + response.body());
    }
}
