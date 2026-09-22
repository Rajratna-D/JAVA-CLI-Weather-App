package com.princeblue.weather.exception;

public class CityNotFoundException extends WeatherAppException {
	private static final long serialVersionUID = 1L;

	public CityNotFoundException() {
		super();
	}

	public CityNotFoundException(String message) {
		super(message);
	}

	public CityNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
