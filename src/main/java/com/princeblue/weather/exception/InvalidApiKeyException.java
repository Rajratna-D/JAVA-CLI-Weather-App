package com.princeblue.weather.exception;

public class InvalidApiKeyException extends WeatherAppException {
	private static final long serialVersionUID = 1L;

	public InvalidApiKeyException() {
		super();
	}

	public InvalidApiKeyException(String message) {
		super(message);
	}

	public InvalidApiKeyException(String message, Throwable cause) {
		super(message, cause);
	}
}
