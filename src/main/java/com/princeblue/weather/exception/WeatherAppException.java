package com.princeblue.weather.exception;

public class WeatherAppException extends Exception {
	private static final long serialVersionUID = 1L;

	public WeatherAppException() {
		super();
	}

	public WeatherAppException(String message) {
		super(message);
	}

	public WeatherAppException(String message, Throwable cause) {
		super(message, cause);
	}

	public WeatherAppException(Throwable cause) {
		super(cause);
	}
}
