package com.princeblue.weather.model;

import java.util.Objects;

public final class Location {
	private final String city;
	private final String country;
	private final double latitude;
	private final double longitude;

	public Location(String city, String country, double latitude, double longitude) {
		this.city = Objects.requireNonNull(city, "city must not be null");
		this.country = country;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public String getCity() {
		return city;
	}

	public String getCountry() {
		return country;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Location location)) {
			return false;
		}
		return Double.compare(latitude, location.latitude) == 0
				&& Double.compare(longitude, location.longitude) == 0
				&& city.equals(location.city)
				&& Objects.equals(country, location.country);
	}

	@Override
	public int hashCode() {
		return Objects.hash(city, country, latitude, longitude);
	}
}
