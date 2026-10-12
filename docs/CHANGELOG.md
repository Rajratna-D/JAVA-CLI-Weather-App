# 📋 Changelog

All notable changes to the **Java CLI Weather & AI Climate Forecasting Workstation** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned
- Docker multi-stage containerization with Java 25 JRE + Python 3.12 ML environment.
- GraalVM Native Image compilation for sub-10ms instantaneous startup.
- Automated retraining triggers via scheduled cron background tasks.
- Air Quality Index (AQI) forecasting via Open-Meteo European Air Quality API.

---

## [1.5.0] - 2026-09-27

### Changed
- **Extended Historical Climate Window from 10 → 12 Years**:
  - `ConsoleUI.java` now passes `syncHistoricalData(12)` to fetch ~105,120 hourly records (up from ~87,600).
  - UI progress strings updated across all Home City sync stages to reflect 12-year data window.
  - Updated all documentation (`README.md`, `ARCHITECTURE.md`, `ML_CONTRACT.md`, `CONCEPTS.md`, `TROUBLESHOOTING.md`) for consistency.
  - Unit tests (`ConsoleUITest.java`) updated with new mock expectations for 12-year sync.
- Train/Test split window shifted from 2016–2024 to 2014–2024 to accommodate the expanded 12-year historical dataset.

---

## [1.4.0] - 2026-09-25

### Added
- **Enterprise Documentation Suite**:
  - `docs/ARCHITECTURE.md`: Complete system topology, N-tier layered specification, SQLite WAL concurrency model, and ProcessBuilder IPC mechanics.
  - `docs/CONCEPTS.md`: Comprehensive engineering reference covering Java OOP (Inheritance, Polymorphism, Abstraction, Encapsulation, Records), GoF Design Patterns, DBMS internals, Computer Networking (HTTP/1.1, DNS, TLS), OS IPC, and ML Mathematics.
  - `docs/ML_CONTRACT.md`: Authoritative specification of the Java ↔ Python ProcessBuilder interface, shared SQLite database schema, and error codes.
  - `docs/TROUBLESHOOTING.md`: 19-scenario diagnosis guide with 30-second automated health-check scripts, OpenWeather 401/404/429 runbooks, Windows Prophet C++ build fixes, SQLite lock resolutions, and Unicode console troubleshooting.
  - `docs/CHANGELOG.md`: Version history and migration notes.
- Restructured all project documentation, specifications, and planning artifacts into a centralized `docs/` directory.

### Changed
- Reorganized `README.md` to feature a streamlined **Documentation Suite** matrix with direct links to technical references.
- Enhanced navigation bar in `README.md` to link directly into `docs/`.

---

## [1.3.0] - 2026-09-24

### Added
- **Multi-Variate Machine Learning Ensemble**:
  - Facebook Prophet model for **Relative Humidity** (`prophet_humidity_model.pkl`) modeling diurnal evaporation and seasonal moisture cycles.
  - Scikit-Learn `HistGradientBoostingClassifier` (`rain_classifier.joblib`) for daily **Rain Probability (%)** prediction.
  - Scikit-Learn `HistGradientBoostingRegressor` (`rainfall_model.joblib` / `rainfall_regressor.joblib`) for expected **Precipitation Volume (mm)** on wet days.
- **Cyclical Feature Engineering**:
  - Implemented sinusoidal solar/orbital transformations ($\sin(2\pi \cdot \text{DOY}/365.25)$ and $\cos(2\pi \cdot \text{DOY}/365.25)$) preserving continuous seasonal periodicity across year boundaries.
- **Intelligent Condition Synthesis Engine**:
  - Rule-based meteorological classifier evaluating temperature, humidity, rain probability, and rainfall volume to synthesize human-readable summaries (`⛈️ Heavy Rain`, `🌧️ Rain Likely`, `🌦️ Showers Likely`, `⛅ Chance of Rain`, `🌫️ Humid / Misty`, `☀️ Hot & Sunny`, etc.).
- **Terminal UI Alignment Engine**:
  - Implemented custom `visualWidth()` algorithm in `ConsoleUI.java` to handle Unicode surrogate pairs (emojis), miscellaneous weather symbols (`0x2600`-`0x27BF`), and zero-width variation selectors (`\uFE0F`).
  - Added JUnit test suite `ConsoleUITest.java` validating table border alignment across all weather conditions and emoji lengths.
- **Automatic Database Schema Migration**:
  - Added non-destructive `ALTER TABLE predictions ADD COLUMN ...` auto-migration in both Java (`SqliteWeatherRepository`) and Python (`db_utils.py`).

### Changed
- Enhanced 7-day terminal dashboard table to display Date, Temperature (Avg/Range), Humidity %, Rain %, and Condition Summary.
- Subsampled Prophet training datasets (2-hourly for temperature, 3-hourly for humidity) to accelerate model convergence while maintaining high fidelity.

---

## [1.2.0] - 2026-09-23

### Added
- **Home City & 12-Year Climate Synchronization**:
  - `HomeCityService.java` orchestrating persistent home location setup and 12-year hourly historical climate data sync.
  - `OpenMeteoClient.java` integrating with Open-Meteo Geocoding and Historical Archive APIs with zero API key requirement.
  - Single-transaction JDBC batch insertion storing ~105,120 hourly climate records in `home_city_hourly` in under 2.5 seconds.
- **Python Machine Learning Pipeline Foundation**:
  - Initial Facebook Prophet integration (`prophet_weather_model.pkl` / `prophet_temp_model.pkl`) for temperature trend and seasonality forecasting.
  - Non-blocking `ProcessBuilder` execution bridge in Java with asynchronous background daemon stream draining to prevent OS pipe deadlocks.
  - Milestone progress indicators in CLI during model training.

### Changed
- Database upgraded with `home_city_config`, `home_city_hourly`, and `predictions` tables.

---

## [1.1.0] - 2026-09-22

### Added
- **SQLite Persistence & Smart Caching**:
  - Integrated `sqlite-jdbc` driver with Write-Ahead Logging (`PRAGMA journal_mode = WAL;`) for concurrent read/write transactions.
  - `weather_cache` table implementing 15-minute TTL caching to minimize external API consumption and protect against rate limits.
  - `search_history` table implementing a capped rolling window of the last 30 search queries.
- **CLI Enhancements**:
  - `history` command rendering an ANSI-formatted recent search audit table.
  - `unit` / `f` command toggling between Celsius (°C) and Fahrenheit (°F).
  - `clear cache` command to manually flush cached weather readings.

---

## [1.0.0] - 2026-09-21

### Added
- **Core CLI Weather Workstation**:
  - Java 25 foundation utilizing strongly typed, immutable `record` types (`Weather`, `Location`).
  - `OpenWeatherClient.java` integrating with OpenWeatherMap 2.5 Current Weather Data API using Java 11 `HttpClient` and Jackson JSON parser.
  - Handcrafted terminal user interface (`ConsoleUI.java`) featuring ANSI color palettes, dynamic temperature color scaling, and ASCII/Unicode box-drawing cards.
  - Domain-specific exception hierarchy (`CityNotFoundException`, `InvalidApiKeyException`, `ApiUnavailableException`, `WeatherAppException`).
  - Secure credential loader reading from `src/main/resources/config.properties`.
  - 31 automated unit and integration tests using JUnit 5 and Mockito.
  - Windows one-click launcher scripts (`run.bat`, `test.bat`).
