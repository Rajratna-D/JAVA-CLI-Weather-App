package com.princeblue.weather.client;

import com.princeblue.weather.exception.WeatherAppException;

public interface ApiClient {

    String fetchWeatherData(String cityName) throws WeatherAppException;
}