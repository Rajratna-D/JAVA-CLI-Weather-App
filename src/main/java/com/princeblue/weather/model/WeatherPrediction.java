package com.princeblue.weather.model;

import java.time.LocalDate;
import java.time.Instant;

public record WeatherPrediction(
        LocalDate forecastDate,
        double predictedTempAvg,
        double predictedTempMin,
        double predictedTempMax,
        String conditionSummary,
        Instant createdAt) {
}
