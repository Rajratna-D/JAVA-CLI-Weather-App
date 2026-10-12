# 🎓 Concepts & Engineering Reference

> A comprehensive breakdown of every Computer Science, Software Engineering, and Machine Learning concept applied in this project — mapped to actual implementation files.

---

## Table of Contents

- [Java OOP](#-java-oop)
- [Database Management (DBMS)](#-database-management-dbms)
- [Networking](#-networking)
- [Systems Design](#-systems-design)
- [Cybersecurity](#-cybersecurity)
- [Quality Assurance & Testing](#-quality-assurance--testing)
- [Machine Learning & Statistics](#-machine-learning--statistics)
- [Software Engineering Practices](#-software-engineering-practices)
- [Data Structures & Algorithms](#-data-structures--algorithms)
- [Operating Systems Concepts](#-operating-systems-concepts)

---

## ☕ Java OOP

| Concept | How It's Applied | File Reference |
| :--- | :--- | :--- |
| **Immutable Records (Value Objects)** | Java 25 `record` constructs enforce immutability — no setters, auto-generated `equals()`, `hashCode()`, and `toString()`. All domain models are value objects. | `Weather.java`, `WeatherPrediction.java`, `HourlyWeatherRecord.java`, `Location.java`, `SearchRecord.java`, `HomeCityConfig.java` |
| **Interface Abstraction** | `WeatherRepository` defines a storage contract (interface) that decouples business logic from any specific SQL implementation. Allows swapping SQLite for PostgreSQL or an in-memory mock without touching service code. | `WeatherRepository.java` |
| **Concrete Implementation** | `SqliteWeatherRepository` implements the `WeatherRepository` interface with SQLite-specific SQL, prepared statements, and WAL configuration. | `SqliteWeatherRepository.java` |
| **Dependency Injection (DI)** | `Main.java` acts as a manual DI container — constructing all dependencies (clients, mapper, repository, services, UI) and wiring them via constructor injection. No framework magic; pure constructor DI. | `Main.java` |
| **Single Responsibility Principle** | Each class has one job: `WeatherMapper` only maps JSON → domain model; `ConsoleUI` only handles rendering; `WeatherService` only orchestrates caching. | All service/client/repository classes |
| **Exception Hierarchy** | Custom domain exception tree rooted in `WeatherAppException`, with specialized subtypes (`CityNotFoundException`, `ApiKeyException`, `ApiException`). Enables targeted `catch` blocks and user-friendly error messages. | `exception/` package |
| **Encapsulation** | Internal state (DB connections, cache maps, API keys) is `private` with controlled access through public methods. | All classes |
| **Polymorphism** | Methods accept `WeatherRepository` (interface type), but at runtime the `SqliteWeatherRepository` implementation handles the actual database calls. | `WeatherService.java`, `HomeCityService.java` |

---

## 🗄️ Database Management (DBMS)

| Concept | How It's Applied | File Reference |
| :--- | :--- | :--- |
| **Relational Schema Design** | 5-table normalized schema: `weather_cache`, `search_history`, `home_city_config`, `home_city_hourly`, `predictions`. Each table has a defined primary key and serves a distinct purpose. | `SqliteWeatherRepository.java` |
| **Primary Keys & Constraints** | `weather_cache` uses `city_name COLLATE NOCASE` as PK (natural key); `home_city_config` uses `CHECK(id = 1)` to enforce singleton pattern; `search_history` uses `AUTOINCREMENT` surrogate key. | `SqliteWeatherRepository.java` |
| **Prepared Statements** | 100% parameterized queries using `?` placeholders. Prevents SQL injection by separating code from data at the database engine level. | `SqliteWeatherRepository.java` |
| **Transaction Batching** | Bulk insert of ~105,120 hourly climate records uses manual `setAutoCommit(false)` → batch `addBatch()` → `executeBatch()` → `commit()` with `rollback()` on failure. Reduces I/O overhead by orders of magnitude vs. individual inserts. | `SqliteWeatherRepository.java` |
| **Write-Ahead Logging (WAL)** | `PRAGMA journal_mode = WAL;` enables concurrent readers while writers commit. Critical for allowing Java reads while Python ML pipeline writes model predictions. | `SqliteWeatherRepository.java` |
| **TTL Cache Invalidation** | 15-minute soft TTL: SQL query computes `(current_epoch - cached_at) < 900` to determine if cached weather is still fresh. Avoids unnecessary API calls. | `WeatherService.java`, `SqliteWeatherRepository.java` |
| **Schema Migration** | Runtime table introspection using `PRAGMA table_info(table_name)` to detect missing columns. Automatically runs `ALTER TABLE ADD COLUMN` for non-destructive schema evolution without a migration framework. | `SqliteWeatherRepository.java` |
| **UPSERT (INSERT OR REPLACE)** | `INSERT OR REPLACE INTO` handles both new inserts and updates to existing rows atomically, keyed on the primary key. | `SqliteWeatherRepository.java` |
| **Rolling Window Queries** | Search history is capped at 35 entries using `DELETE FROM search_history WHERE id NOT IN (SELECT id FROM search_history ORDER BY searched_at DESC LIMIT 35)`. | `SqliteWeatherRepository.java` |
| **Case-Insensitive Collation** | `COLLATE NOCASE` on the `city_name` primary key ensures "Pune", "pune", and "PUNE" all resolve to the same cache entry. | `SqliteWeatherRepository.java` |

---

## 🌐 Networking

| Concept | How It's Applied | File Reference |
| :--- | :--- | :--- |
| **HTTP/2 Client** | Uses `java.net.http.HttpClient` (introduced in Java 11) for non-blocking, connection-pooled HTTP/2 requests to OpenWeatherMap and Open-Meteo APIs. | `OpenWeatherClient.java`, `OpenMeteoClient.java` |
| **RESTful API Consumption** | Constructs proper REST URIs with query parameters (`?q=city&appid=key&units=metric`), sends GET requests, and parses JSON responses. | `OpenWeatherClient.java`, `OpenMeteoClient.java` |
| **HTTP Status Code Handling** | Maps response codes to typed exceptions: `401` → `ApiKeyException`, `404` → `CityNotFoundException`, `5xx` → `ApiException` with retry guidance. | `OpenWeatherClient.java` |
| **URL Encoding** | City names with spaces and special characters are encoded using `URLEncoder.encode(city, UTF_8)` to produce valid URI components. | `OpenWeatherClient.java` |
| **Request Timeouts** | Configurable connection and read timeouts via `HttpRequest.Builder.timeout()` to prevent indefinite hangs on network failures. | `OpenWeatherClient.java` |
| **JSON Parsing** | Uses `org.json` library to navigate nested JSON objects and arrays (`getJSONObject`, `getJSONArray`, `getDouble`) from API responses. | `WeatherMapper.java`, `OpenMeteoClient.java` |

---

## 🏗️ Systems Design

| Concept | How It's Applied | File Reference |
| :--- | :--- | :--- |
| **N-Tier Layered Architecture** | Strict separation into Presentation (UI) → Service (Business Logic) → Repository (Data Access) → External Clients. Each layer only talks to its immediate neighbor. | Entire project structure |
| **Polyglot Inter-Process Communication (IPC)** | Java invokes Python ML scripts via `ProcessBuilder`, passing arguments through command-line parameters and reading results from a shared SQLite database. Cross-language bridge without REST or gRPC overhead. | `HomeCityService.java` |
| **OS Pipe Buffer Deadlock Prevention** | A dedicated daemon thread drains the Python subprocess's `stdout`/`stderr` streams. Without this, the OS pipe buffer (typically 64KB) fills up, blocking the child process and causing a deadlock. | `ConsoleUI.java` |
| **Shared-Database Integration Pattern** | SQLite acts as the contract between Java and Python. Both runtimes read/write to the same `weather.db` — no serialization protocol needed. | `SqliteWeatherRepository.java`, `db_utils.py` |
| **Offline-First / Cache-First Design** | The app checks the local SQLite cache first. API is only called when cache is stale (TTL expired). User gets instant results from cache for repeated queries. | `WeatherService.java` |
| **Singleton Configuration** | `home_city_config` table uses `CHECK(id = 1)` to guarantee at most one home city configuration row exists — a database-enforced singleton. | `SqliteWeatherRepository.java` |
| **ANSI Terminal Rendering Engine** | Custom Unicode-aware visual width calculator handles multi-byte characters, emoji variation selectors (`\uFE0F`), surrogate pairs, and zero-width joiners to produce perfectly aligned box-drawing UI. | `ConsoleUI.java` |
| **Script Launcher Pattern** | `run.bat` and `test.bat` abstract away Maven commands for Windows users, providing one-click launch and test execution. | `run.bat`, `test.bat` |

---

## 🔒 Cybersecurity

| Concept | How It's Applied | File Reference |
| :--- | :--- | :--- |
| **SQL Injection Prevention** | Every database query uses parameterized prepared statements (`?`). Zero string concatenation in SQL construction. | `SqliteWeatherRepository.java` |
| **Secret Management** | API keys stored in `config.properties` (git-ignored). A safe `.example` template is committed instead. Defense-in-depth `.gitignore` rules prevent accidental key exposure. | `config.properties.example`, `.gitignore` |
| **Model Binary Exclusion** | Trained `.pkl` and `.joblib` model files are git-ignored. Only a `.gitkeep` placeholder is tracked, preventing large binary blobs from entering version history. | `.gitignore`, `ml/models/.gitkeep` |
| **Input Validation** | City names and user commands are validated before being passed to API endpoints or database queries. | `WeatherService.java`, `ConsoleUI.java` |
| **Dependency Isolation** | Python ML environment runs inside an isolated virtual environment (`.venv`), preventing system-level package conflicts and supply chain contamination. | `ml/.venv/`, `requirements.txt` |

---

## 🧪 Quality Assurance & Testing

| Concept | How It's Applied | File Reference |
| :--- | :--- | :--- |
| **Unit Testing (JUnit 5)** | 31 tests covering weather mapping, cache logic, repository operations, and UI rendering. Tests run via `mvn test` or `test.bat`. | `src/test/` directory |
| **Mocking (Mockito)** | External dependencies (API clients, repositories) are mocked to test service logic in isolation without network or database access. | `WeatherServiceTest.java`, `ConsoleUITest.java` |
| **Visual Layout Testing** | Dedicated tests verify that ANSI table borders align to exact column widths (74 columns for the 7-day forecast table), catching rendering regressions. | `ConsoleUITest.java` |
| **Integration Testing** | Repository tests run against real SQLite databases (in-memory or temporary files) to verify SQL correctness, schema migration, and transaction behavior. | `SqliteWeatherRepositoryTest.java` |
| **Test Automation** | `test.bat` and Maven Surefire plugin enable CI-ready test execution. All tests pass in under 5 seconds. | `pom.xml`, `test.bat` |

---

## 🧠 Machine Learning & Statistics

| Concept | How It's Applied | File Reference |
| :--- | :--- | :--- |
| **Time-Series Decomposition** | Facebook Prophet decomposes 12-year hourly temperature and humidity data into trend $g(t)$, seasonal $s(t)$, holiday $h(t)$, and residual $\epsilon_t$ components using an additive model: $y(t) = g(t) + s(t) + h(t) + \epsilon_t$. | `train.py` |
| **Fourier Series Seasonality** | Periodic patterns are captured using Fourier series: $s(t) = \sum_{n=1}^{N}(a_n \cos(\frac{2\pi nt}{P}) + b_n \sin(\frac{2\pi nt}{P}))$ with yearly ($P = 365.25$ days) and daily ($P = 24$ hours) periods. | `train.py` |
| **Bayesian Uncertainty Intervals** | Prophet generates 90% credible intervals for temperature and humidity forecasts using Stan's posterior sampling, giving upper/lower bounds on each prediction. | `predict.py`, `train.py` |
| **L-BFGS Optimization** | Prophet model parameters are solved using the Limited-memory Broyden–Fletcher–Goldfarb–Shanno (L-BFGS) algorithm — a quasi-Newton numerical optimization method. | `train.py` |
| **Gradient Boosting Classification** | `HistGradientBoostingClassifier` from Scikit-Learn predicts rain occurrence probability $P(\text{Rain} \mid \text{features})$ using histogram-based gradient boosting, which handles large datasets efficiently via binning. | `train.py` |
| **Gradient Boosting Regression** | `HistGradientBoostingRegressor` predicts rainfall volume (mm) for wet days. Uses the same binned gradient boosting approach for continuous target prediction. | `train.py` |
| **Harmonic Feature Engineering** | Day-of-year is transformed into continuous circular features: $\sin(\frac{2\pi \cdot \text{doy}}{365.25})$ and $\cos(\frac{2\pi \cdot \text{doy}}{365.25})$ to eliminate artificial boundary discontinuities between Dec 31 and Jan 1. | `train.py`, `predict.py` |
| **Cascaded Multi-Stage Pipeline** | Stage 1 (Prophet) forecasts continuous variables (temp, humidity) → Stage 2 feeds those forecasts as features into gradient boosting models for discrete rain classification and intensity regression. | `predict.py` |
| **Train/Test Split Validation** | 80/20 chronological split (2014–2024 train, 2024–2026 test) ensures the model is validated on future unseen data, preventing temporal leakage. | `train.py` |
| **Model Serialization** | Trained Prophet models serialized via `pickle` (`.pkl`); Scikit-Learn models via `joblib` (`.joblib`). Enables instant inference without retraining. | `train.py`, `predict.py` |
| **Calibrated Probability Estimation** | Rain classifier outputs `predict_proba()` scores (0–100%) rather than binary labels, enabling nuanced forecast summaries ("Chance of Rain" vs "Rain Likely"). | `predict.py` |
| **Data Sub-Sampling** | ~105,120 hourly records are sub-sampled to 2–3 hourly intervals during training to reduce model fitting time from ~60s to ~29s while preserving seasonal patterns. | `train.py` |
| **ROC-AUC Evaluation** | Rain classifier performance measured by Area Under the ROC Curve (AUC = 0.971), measuring the model's ability to discriminate between rainy and dry days across all decision thresholds. | `train.py` |

---

## ⚙️ Software Engineering Practices

| Concept | How It's Applied | File Reference |
| :--- | :--- | :--- |
| **Separation of Concerns** | Each class/module has a single, well-defined responsibility. UI doesn't know about SQL; SQL doesn't know about APIs. | Entire codebase |
| **Configuration Externalization** | API keys, base URLs, and settings live in external `config.properties` files — not hardcoded. Supports per-environment overrides. | `AppConfig.java`, `config.properties` |
| **Graceful Degradation** | If the ML pipeline fails (Python not installed, model not trained), the app gracefully falls back to live weather only — no crash. | `HomeCityService.java`, `ConsoleUI.java` |
| **Resource Management (try-with-resources)** | All JDBC `Connection`, `PreparedStatement`, and `ResultSet` objects are managed with try-with-resources blocks to guarantee cleanup even on exceptions. | `SqliteWeatherRepository.java` |
| **Builder Pattern (HttpRequest)** | HTTP requests constructed using `HttpRequest.newBuilder().uri(...).timeout(...).build()` — a fluent builder API. | `OpenWeatherClient.java` |
| **Version Control Hygiene** | Comprehensive `.gitignore` covering build artifacts (`target/`), IDE files, credentials, virtual environments, model binaries, and database files. | `.gitignore` |
| **Cross-Platform Compatibility** | Application tested on Windows and Linux. Path separators, Python executable names (`python` vs `python3`), and line endings are handled. | `HomeCityService.java`, `run.bat` |

---

## 📊 Data Structures & Algorithms

| Concept | How It's Applied | File Reference |
| :--- | :--- | :--- |
| **HashMap (Cache Lookups)** | In-memory structures for fast O(1) city-to-weather lookups before falling through to SQLite. | `WeatherService.java` |
| **List/ArrayList** | Collections of `SearchRecord`, `WeatherPrediction`, and `HourlyWeatherRecord` for ordered display and batch processing. | Throughout service and repository classes |
| **String Processing** | Unicode-aware string width calculation iterating codepoints, handling surrogate pairs, variation selectors, and zero-width joiners for table alignment. | `ConsoleUI.java` |
| **Date/Time Arithmetic** | Epoch timestamp math for TTL calculations; ISO 8601 parsing for forecast dates; `LocalDateTime` for human-readable display formatting. | `SqliteWeatherRepository.java`, `ConsoleUI.java` |
| **Batch Processing** | JDBC batch operations accumulate thousands of SQL statements and execute them in a single I/O round-trip. | `SqliteWeatherRepository.java` |

---

## 🖥️ Operating Systems Concepts

| Concept | How It's Applied | File Reference |
| :--- | :--- | :--- |
| **Process Management** | Java spawns Python as a child process via `ProcessBuilder`. Parent process manages the child's lifecycle — start, monitor, and clean up. | `HomeCityService.java` |
| **Inter-Process Communication (IPC)** | Parent and child processes communicate through a shared SQLite database file (filesystem-level IPC). Standard output pipes are used for progress logging. | `HomeCityService.java`, `db_utils.py` |
| **Pipe Buffer Management** | OS provides fixed-size pipe buffers (~64KB). A daemon thread continuously drains these buffers to prevent the child process from blocking on write. | `ConsoleUI.java` |
| **Daemon Threads** | The pipe-drain thread is marked as daemon (`setDaemon(true)`) so it doesn't prevent JVM shutdown when the main thread exits. | `ConsoleUI.java` |
| **Virtual Environment Isolation** | Python `.venv` creates an isolated filesystem namespace for packages, preventing conflicts with system-wide installations — analogous to containerization at the package level. | `ml/.venv/` |
| **File Locking (WAL)** | SQLite's WAL mode uses shared memory (`-shm`) and WAL index (`-wal`) files for fine-grained concurrent access control, replacing coarse database-level locks. | `weather.db`, `SqliteWeatherRepository.java` |

---

> **📖 See also**: [README.md](../README.md) for full project overview | [ARCHITECTURE.md](ARCHITECTURE.md) for system diagrams | [ML_CONTRACT.md](ML_CONTRACT.md) for data specifications | [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for error guides.
