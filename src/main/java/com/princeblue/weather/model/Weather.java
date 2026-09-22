package com.princeblue.weather.model;

import java.util.Objects;

public final class Weather {
	private final Location location;
	private final double temperature;
	private final double feelsLike;
	private final int humidity;
	private final String description;
	private final double windSpeed;

	public Weather(Location location, double temperature, double feelsLike, int humidity,
				   String description, double windSpeed) {
		this.location = Objects.requireNonNull(location, "location must not be null");
		this.description = Objects.requireNonNull(description, "description must not be null");
		this.temperature = temperature;
		this.feelsLike = feelsLike;
		this.humidity = humidity;
		this.windSpeed = windSpeed;
	}

	public Location getLocation() {
		return location;
	}

	public double getTemperature() {
		return temperature;
	}

	public double getFeelsLike() {
		return feelsLike;
	}

	public int getHumidity() {
		return humidity;
	}

	public String getDescription() {
		return description;
	}

	public double getWindSpeed() {
		return windSpeed;
	}
}
