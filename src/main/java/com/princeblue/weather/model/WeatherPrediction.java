package com.princeblue.weather.model;

import java.time.LocalDate;
import java.time.Instant;

public record WeatherPrediction(
        LocalDate forecastDate,
        double predictedTempAvg,
        double predictedTempMin,
        double predictedTempMax,
        double predictedHumidity,
        double predictedRainProb,
        double predictedRainfallMm,
        String conditionSummary,
        Instant createdAt) {

    /**
     * Convenience constructor for backwards compatibility.
     */
    public WeatherPrediction(
            LocalDate forecastDate,
            double predictedTempAvg,
            double predictedTempMin,
            double predictedTempMax,
            String conditionSummary,
            Instant createdAt) {
        this(forecastDate, predictedTempAvg, predictedTempMin, predictedTempMax, 0.0, 0.0, 0.0, conditionSummary, createdAt);
    }
}

