package com.princeblue.weather.exception;

public class ApiUnavailableException extends WeatherAppException {
	private static final long serialVersionUID = 1L;

	public ApiUnavailableException() {
		super();
	}

	public ApiUnavailableException(String message) {
		super(message);
	}

	public ApiUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
