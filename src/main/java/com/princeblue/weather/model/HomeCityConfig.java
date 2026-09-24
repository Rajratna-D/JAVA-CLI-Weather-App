package com.princeblue.weather.model;

import java.time.Instant;

public record HomeCityConfig(
        String cityName,
        double latitude,
        double longitude,
        Instant lastSyncedAt) {
}
