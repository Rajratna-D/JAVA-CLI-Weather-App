package com.princeblue.weather.model;

import java.time.LocalDateTime;

public record HourlyWeatherRecord(
        LocalDateTime timestamp,
        double temperature,
        double humidity,
        double precipitation) {
}
