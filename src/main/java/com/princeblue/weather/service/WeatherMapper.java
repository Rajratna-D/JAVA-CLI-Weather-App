package com.princeblue.weather.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.princeblue.weather.model.Location;
import com.princeblue.weather.model.Weather;

public class WeatherMapper {
	private final Gson gson;

	public WeatherMapper() {
		this(new Gson());
	}

	public WeatherMapper(Gson gson) {
		this.gson = gson;
	}

	public Weather map(String payload) {
		if (payload == null || payload.isBlank()) {
			throw new IllegalArgumentException("weather payload must not be blank");
		}

		try {
			JsonElement rootElement = JsonParser.parseString(payload);
			if (!rootElement.isJsonObject()) {
				throw new IllegalArgumentException("weather payload must be a JSON object");
			}

			JsonObject root = gson.fromJson(rootElement, JsonObject.class);
			JsonObject main = requiredObject(root, "main");
			JsonArray weatherEntries = requiredArray(root, "weather");
			if (weatherEntries.isEmpty() || !weatherEntries.get(0).isJsonObject()) {
				throw new IllegalArgumentException("weather payload must contain a weather entry");
			}

			JsonObject weatherEntry = weatherEntries.get(0).getAsJsonObject();
			String city = requiredString(root, "name");
			String description = requiredString(weatherEntry, "description");
			double temperature = requiredNumber(main, "temp");
			int humidity = requiredInteger(main, "humidity");

			JsonObject coordinates = optionalObject(root, "coord");
			JsonObject system = optionalObject(root, "sys");
			JsonObject wind = optionalObject(root, "wind");
			Location location = new Location(
					city,
					optionalString(system, "country"),
					optionalNumber(coordinates, "lat"),
					optionalNumber(coordinates, "lon"));

			return new Weather(
					location,
					temperature,
					optionalNumber(main, "feels_like", temperature),
					humidity,
					description,
					optionalNumber(wind, "speed"));
		} catch (JsonParseException | ClassCastException | NumberFormatException exception) {
			throw new IllegalArgumentException("invalid weather payload", exception);
		}
	}

	private static JsonObject requiredObject(JsonObject object, String member) {
		JsonObject value = optionalObject(object, member);
		if (value == null) {
			throw new IllegalArgumentException("missing JSON object: " + member);
		}
		return value;
	}

	private static JsonObject optionalObject(JsonObject object, String member) {
		JsonElement value = object.get(member);
		return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
	}

	private static JsonArray requiredArray(JsonObject object, String member) {
		JsonElement value = object.get(member);
		if (value == null || !value.isJsonArray()) {
			throw new IllegalArgumentException("missing JSON array: " + member);
		}
		return value.getAsJsonArray();
	}

	private static String requiredString(JsonObject object, String member) {
		String value = optionalString(object, member);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("missing JSON string: " + member);
		}
		return value;
	}

	private static String optionalString(JsonObject object, String member) {
		if (object == null || !object.has(member) || object.get(member).isJsonNull()) {
			return null;
		}
		return object.get(member).getAsString();
	}

	private static double requiredNumber(JsonObject object, String member) {
		if (!object.has(member) || object.get(member).isJsonNull()) {
			throw new IllegalArgumentException("missing JSON number: " + member);
		}
		return object.get(member).getAsDouble();
	}

	private static int requiredInteger(JsonObject object, String member) {
		if (!object.has(member) || object.get(member).isJsonNull()) {
			throw new IllegalArgumentException("missing JSON integer: " + member);
		}
		return object.get(member).getAsInt();
	}

	private static double optionalNumber(JsonObject object, String member) {
		return optionalNumber(object, member, 0.0);
	}

	private static double optionalNumber(JsonObject object, String member, double defaultValue) {
		if (object == null || !object.has(member) || object.get(member).isJsonNull()) {
			return defaultValue;
		}
		return object.get(member).getAsDouble();
	}
}
