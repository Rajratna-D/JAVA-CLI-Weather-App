package com.princeblue.weather.service;

import com.princeblue.weather.model.Location;
import com.princeblue.weather.model.Weather;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WeatherMapperTest {
	private final WeatherMapper mapper = new WeatherMapper();

	@Test
	void mapsOpenWeatherPayloadToImmutableDomainModel() {
		Weather weather = mapper.map("""
				{
				  "name": "London",
				  "coord": {"lat": 51.5072, "lon": -0.1276},
				  "weather": [{"description": "light rain"}],
				  "main": {"temp": 12.4, "feels_like": 11.8, "humidity": 81},
				  "wind": {"speed": 4.1},
				  "sys": {"country": "GB"}
				}
				""");

		assertEquals(new Location("London", "GB", 51.5072, -0.1276), weather.getLocation());
		assertEquals(12.4, weather.getTemperature());
		assertEquals(11.8, weather.getFeelsLike());
		assertEquals(81, weather.getHumidity());
		assertEquals("light rain", weather.getDescription());
		assertEquals(4.1, weather.getWindSpeed());
	}

	@Test
	void defaultsOptionalFieldsWhenTheApiOmitsThem() {
		Weather weather = mapper.map("""
				{
				  "name": "Oslo",
				  "weather": [{"description": "clear sky"}],
				  "main": {"temp": 8.0, "humidity": 55}
				}
				""");

		assertEquals("Oslo", weather.getLocation().getCity());
		assertEquals(8.0, weather.getFeelsLike());
		assertEquals(0.0, weather.getLocation().getLatitude());
		assertEquals(0.0, weather.getWindSpeed());
	}

	@Test
	void rejectsBlankPayloads() {
		assertThrows(IllegalArgumentException.class, () -> mapper.map("  \n"));
	}

	@Test
	void rejectsMalformedJson() {
		assertThrows(IllegalArgumentException.class, () -> mapper.map("{not-json"));
	}

	@Test
	void rejectsPayloadWithoutRequiredWeatherData() {
		assertThrows(IllegalArgumentException.class, () -> mapper.map("""
				{"name":"Paris","weather":[],"main":{"temp":20,"humidity":40}}
				"""));
		assertThrows(IllegalArgumentException.class, () -> mapper.map("""
				{"name":"Paris","weather":[{"description":"sunny"}],"main":{"humidity":40}}
				"""));
	}
}
