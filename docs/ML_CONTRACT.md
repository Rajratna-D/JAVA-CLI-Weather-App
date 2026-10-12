# 🔗 ML Contract — Java ↔ Python Interface Specification

> Defines the exact data contract between the **Java CLI** (consumer) and the **Python ML pipeline** (producer). This document is the authoritative reference for any developer modifying either side of the bridge.

---

## Table of Contents

- [Overview](#overview)
- [Invocation Contract](#-invocation-contract)
- [Shared Database Schema](#-shared-database-schema)
- [Data Flow: Write Path (Java → SQLite)](#-data-flow-write-path-java--sqlite)
- [Data Flow: Read Path (Python ← SQLite)](#-data-flow-read-path-python--sqlite)
- [Data Flow: Write Path (Python → SQLite)](#-data-flow-write-path-python--sqlite-1)
- [Data Flow: Read Path (Java ← SQLite)](#-data-flow-read-path-java--sqlite)
- [Model Artifacts](#-model-artifacts)
- [Stdout Protocol](#-stdout-protocol)
- [Error Contract](#-error-contract)
- [Extending the Contract](#-extending-the-contract)

---

## Overview

The Java and Python runtimes communicate through two mechanisms:

```
┌──────────────┐                         ┌──────────────┐
│   Java CLI   │──── ProcessBuilder ────►│  Python ML   │
│  (Consumer)  │                         │  (Producer)  │
│              │                         │              │
│  Writes:     │     ┌─────────────┐     │  Reads:      │
│  home_city_  │────►│ weather.db  │◄────│  home_city_  │
│  hourly      │     │  (SQLite)   │     │  hourly      │
│              │     │             │     │              │
│  Reads:      │◄────│             │────►│  Writes:     │
│  predictions │     └─────────────┘     │  predictions │
└──────────────┘                         └──────────────┘
```

| Mechanism | Purpose | Direction |
| :--- | :--- | :--- |
| **ProcessBuilder** | Launches `ml/pipeline.py` as a child process | Java → Python |
| **SQLite (Shared DB)** | Data exchange medium — no serialization protocol needed | Bidirectional |
| **Stdout Pipe** | Progress logging streamed back to Java for real-time UI display | Python → Java |

---

## 🚀 Invocation Contract

### How Java Calls Python

```java
// HomeCityService.java — startAiPipelineProcess()
String pythonExe = findPythonExecutable();  // Resolves .venv first, then system PATH
ProcessBuilder pb = new ProcessBuilder(pythonExe, "ml/pipeline.py");
pb.redirectErrorStream(true);              // Merge stderr into stdout
Process process = pb.start();
```

### Python Executable Resolution Order

| Priority | Path | Platform |
| :--- | :--- | :--- |
| 1st | `ml/.venv/Scripts/python.exe` | Windows |
| 2nd | `ml/.venv/bin/python` | Linux / macOS |
| 3rd | `python` (system PATH) | Fallback |

### Command-Line Arguments

| Argument | Value | Notes |
| :--- | :--- | :--- |
| `argv[0]` | `ml/pipeline.py` | Entry point script — must not be changed |

**No additional CLI arguments are passed.** All configuration (DB path, model paths, forecast days) is managed via `ml/config.py`.

### Working Directory

The `ProcessBuilder` inherits the JVM's working directory, which must be the project root (`JAVA-CLI-Weather-App/`). All relative paths in `config.py` depend on this.

---

## 🗄️ Shared Database Schema

File: `weather.db` (project root)  
Mode: `PRAGMA journal_mode = WAL;`

### Table: `home_city_hourly` (Java writes, Python reads)

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `timestamp` | `TEXT` | `PRIMARY KEY` | ISO 8601 datetime string (e.g., `2024-01-15T14:00`) |
| `temperature` | `REAL` | `NOT NULL` | Hourly temperature in °C |
| `humidity` | `REAL` | `NOT NULL` | Hourly relative humidity (%) |
| `precipitation` | `REAL` | `NOT NULL` | Hourly precipitation (mm) |

**Source**: Open-Meteo Historical Weather API (12-year archive)  
**Writer**: `SqliteWeatherRepository.saveHourlyWeatherBatch()` — batch insert with manual commit  
**Volume**: ~105,120 rows (12 years × 365.25 days × 24 hours)

### Table: `predictions` (Python writes, Java reads)

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `forecast_date` | `TEXT` | `PRIMARY KEY` | Date string `YYYY-MM-DD` |
| `predicted_temp_avg` | `REAL` | `NOT NULL` | Mean predicted temperature (°C) |
| `predicted_temp_min` | `REAL` | `NOT NULL` | Lower bound from 90% credible interval |
| `predicted_temp_max` | `REAL` | `NOT NULL` | Upper bound from 90% credible interval |
| `predicted_humidity` | `REAL` | `DEFAULT 0.0` | Mean predicted relative humidity (%) |
| `predicted_rain_prob` | `REAL` | `DEFAULT 0.0` | Rain occurrence probability (0.0–100.0) |
| `predicted_rainfall_mm` | `REAL` | `DEFAULT 0.0` | Expected rainfall volume (mm), 0.0 on dry days |
| `condition_summary` | `TEXT` | *nullable* | Human-readable weather label (e.g., `"🌧️ Rain Likely"`) |
| `created_at` | `INTEGER` | `NOT NULL` | Epoch milliseconds (UTC) when prediction was generated |

**Writer**: `db_utils.save_predictions_to_db()` — Python side  
**Reader**: `SqliteWeatherRepository.getPredictions(LocalDate)` — Java side

---

## ⬇️ Data Flow: Write Path (Java → SQLite)

```
User selects "Sync Historical Data" in Home City menu
    │
    ▼
HomeCityService.syncHistoricalData(yearsBack=12)
    │
    ├── OpenMeteoClient.fetchHistoricalHourlyData(lat, lon, startDate, endDate)
    │   └── HTTP GET → https://archive-api.open-meteo.com/v1/archive?...
    │       Returns: List<HourlyWeatherRecord> (~105,120 records)
    │
    ├── SqliteWeatherRepository.saveHourlyWeatherBatch(records)
    │   └── INSERT OR REPLACE INTO home_city_hourly (timestamp, temperature, humidity, precipitation)
    │       Batch size: 1000 rows per executeBatch()
    │       Transaction: Manual commit/rollback
    │
    └── Updates home_city_config.last_synced_at = Instant.now()
```

### HourlyWeatherRecord (Java Record)

```java
public record HourlyWeatherRecord(
    LocalDateTime timestamp,    // ISO 8601 stored as TEXT
    double temperature,         // °C
    double humidity,            // Relative humidity %
    double precipitation        // mm
) {}
```

---

## ⬆️ Data Flow: Read Path (Python ← SQLite)

```python
# db_utils.py — load_training_data()
query = """
    SELECT timestamp, temperature, humidity, precipitation
    FROM home_city_hourly
    ORDER BY timestamp ASC
"""
```

Returns a Pandas DataFrame with columns: `['timestamp', 'temperature', 'humidity', 'precipitation', 'ds']`

**Critical**: The `ds` column is a `pd.to_datetime()` conversion of `timestamp` — required by Prophet.

---

## ⬆️ Data Flow: Write Path (Python → SQLite)

```python
# db_utils.py — save_predictions_to_db()
cursor.execute("""
    INSERT OR REPLACE INTO predictions
    (forecast_date, predicted_temp_avg, predicted_temp_min, predicted_temp_max,
     predicted_humidity, predicted_rain_prob, predicted_rainfall_mm,
     condition_summary, created_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
""", (
    str(forecast_date),              # "2026-09-25"
    round(predicted_temp_avg, 1),    # 24.5
    round(predicted_temp_min, 1),    # 16.2
    round(predicted_temp_max, 1),    # 33.8
    round(predicted_humidity, 1),    # 83.0
    round(predicted_rain_prob, 1),   # 97.2
    round(predicted_rainfall_mm, 1), # 12.4
    condition_summary,               # "🌧️ Rain Likely"
    epoch_millis_utc                 # 1727251200000
))
```

### Schema Auto-Migration (Python Side)

Python runs `ALTER TABLE ADD COLUMN` for each new multi-variate column before writing, gracefully handling the case where an older Java version created the `predictions` table without humidity/rain columns:

```python
for col_def in [
    ("predicted_humidity", "REAL DEFAULT 0.0"),
    ("predicted_rain_prob", "REAL DEFAULT 0.0"),
    ("predicted_rainfall_mm", "REAL DEFAULT 0.0"),
]:
    try:
        cursor.execute(f"ALTER TABLE predictions ADD COLUMN {col_def[0]} {col_def[1]}")
    except sqlite3.OperationalError:
        pass  # Column already exists
```

---

## ⬇️ Data Flow: Read Path (Java ← SQLite)

```java
// SqliteWeatherRepository.getPredictions(LocalDate fromDate)
String sql = """
    SELECT forecast_date, predicted_temp_avg, predicted_temp_min, predicted_temp_max,
           predicted_humidity, predicted_rain_prob, predicted_rainfall_mm,
           condition_summary, created_at
    FROM predictions
    WHERE forecast_date >= ?
    ORDER BY forecast_date ASC
""";
```

### WeatherPrediction (Java Record)

```java
public record WeatherPrediction(
    LocalDate forecastDate,       // Parsed from TEXT "YYYY-MM-DD"
    double predictedTempAvg,      // °C
    double predictedTempMin,      // °C (lower bound)
    double predictedTempMax,      // °C (upper bound)
    double predictedHumidity,     // %
    double predictedRainProb,     // 0.0–100.0
    double predictedRainfallMm,   // mm
    String conditionSummary,      // Human-readable label with emoji
    Instant createdAt             // From epoch millis
) {}
```

---

## 📦 Model Artifacts

| File | Format | Producer | Size (approx.) | Description |
| :--- | :--- | :--- | :--- | :--- |
| `ml/models/prophet_temp_model.pkl` | Python pickle | `train.py` | ~15 MB | Prophet temperature model (12-year fit) |
| `ml/models/prophet_humidity_model.pkl` | Python pickle | `train.py` | ~10 MB | Prophet humidity model |
| `ml/models/rain_classifier.joblib` | Joblib | `train.py` | ~1 MB | HistGradientBoosting rain classifier |
| `ml/models/rainfall_regressor.joblib` | Joblib | `train.py` | ~1 MB | HistGradientBoosting rainfall regressor |

**All model files are `.gitignore`d.** Only `ml/models/.gitkeep` is tracked.

### Model Availability Check (Java Side)

```java
// HomeCityService.isAiModelAvailable()
File tempModel = new File("ml/models/prophet_temp_model.pkl");
File legacyModel = new File("ml/models/prophet_weather_model.pkl");
return (tempModel.exists() && tempModel.length() > 0) || (legacyModel.exists() && legacyModel.length() > 0);
```

---

## 📡 Stdout Protocol

Python's stdout is merged with stderr (`redirectErrorStream(true)`) and consumed by a **daemon thread** on the Java side to prevent OS pipe buffer deadlocks.

### Expected Output Format

```text
============================================================
  AI MULTI-VARIATE FORECAST ENGINE (Prophet + Scikit-Learn)
============================================================

[Step 1/2] Training AI models (Temperature, Humidity, Rain & Precipitation)...
📥 Loading historical climate data from SQLite...
 Loaded 87,648 hourly records spanning from 2016-09-24 to 2026-09-24.

🧠 [1/3] Training Prophet model on temperature cycles...
✅ Temperature model trained in 12.3s -> Saved to prophet_temp_model.pkl

💧 [2/3] Training Prophet model on relative humidity diurnal cycles...
✅ Humidity model trained in 8.1s -> Saved to prophet_humidity_model.pkl

🌧️ [3/3] Training Scikit-Learn Rain Probability & Rainfall engines...
✅ Rain engines trained in 0.4s -> Saved to rain_classifier.joblib

[Step 2/2] Generating 7-day multi-variate weather predictions...
 Loading trained models from disk...
 Projecting multi-variate forecast for the next 7 days (168 hours)...
Successfully saved 7 days of multi-variate predictions into SQLite database!

AI Pipeline finished in 28.9 seconds!
```

**Java does NOT parse stdout content.** It only displays it as progress feedback. All actual data exchange happens through SQLite.

---

## ❌ Error Contract

| Error Source | Behavior | Java Handling |
| :--- | :--- | :--- |
| Python not installed | `ProcessBuilder.start()` throws `IOException` | Caught in `ConsoleUI`; shows "Python not found" message |
| `weather.db` missing | Python raises `FileNotFoundError` | Process exits with non-zero code; Java detects via `process.waitFor()` |
| `home_city_hourly` empty | Python raises `ValueError("No historical data found")` | Process exits; Java shows "sync data first" message |
| Model file missing | Python raises `FileNotFoundError` on pickle load | Process exits; Java shows "train model first" message |
| Pip dependencies missing | Python raises `ModuleNotFoundError` (e.g., `No module named 'prophet'`) | Process exits; Java shows install instructions |

---

## 🔧 Extending the Contract

### Adding a New Prediction Column

1. **Python side** (`db_utils.py`): Add `ALTER TABLE ADD COLUMN` migration for the new column.
2. **Python side** (`predict.py`): Compute the new value and include it in the `daily_records` dict.
3. **Python side** (`db_utils.py`): Add the column to the `INSERT OR REPLACE` statement.
4. **Java side** (`SqliteWeatherRepository.java`): Add `ensureColumnExists()` call in `initDatabase()`.
5. **Java side** (`WeatherPrediction.java`): Add the field to the record definition.
6. **Java side** (`SqliteWeatherRepository.java`): Read the column in `getPredictions()`.
7. **Java side** (`ConsoleUI.java`): Display the new data in the forecast table.

### Adding a New Model

1. Add path constant in `ml/config.py`.
2. Train and serialize in `ml/train.py`.
3. Load and predict in `ml/predict.py`.
4. Add to `.gitignore` if binary (`.pkl`, `.joblib`).

---

> **📖 See also**: [README.md](../README.md) for setup instructions | [CONCEPTS.md](CONCEPTS.md) for engineering concepts | [ARCHITECTURE.md](ARCHITECTURE.md) for sequence diagrams
