package com.princeblue.weather.model;

import java.time.Instant;

public record SearchRecord(
        long id,
        String cityName,
        double temperature,
        String condition,
        Instant searchedAt) {
}
