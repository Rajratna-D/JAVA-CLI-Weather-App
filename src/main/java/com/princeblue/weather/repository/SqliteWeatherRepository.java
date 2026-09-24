package com.princeblue.weather.repository;

import com.princeblue.weather.model.HomeCityConfig;
import com.princeblue.weather.model.HourlyWeatherRecord;
import com.princeblue.weather.model.Location;
import com.princeblue.weather.model.SearchRecord;
import com.princeblue.weather.model.Weather;
import com.princeblue.weather.model.WeatherPrediction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteWeatherRepository implements WeatherRepository {

    private final String dbUrl;

    public SqliteWeatherRepository() {
        this("jdbc:sqlite:weather.db");
    }

    public SqliteWeatherRepository(String dbUrl) {
        this.dbUrl = dbUrl;
        initDatabase();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    private void initDatabase() {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {

            // Enable WAL mode for high concurrency & speed
            stmt.execute("PRAGMA journal_mode = WAL;");

            // 1. Weather Cache table (TTL caching)
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS weather_cache (
                            city_name TEXT PRIMARY KEY COLLATE NOCASE,
                            country TEXT,
                            latitude REAL,
                            longitude REAL,
                            temperature REAL,
                            feels_like REAL,
                            humidity INTEGER,
                            description TEXT,
                            wind_speed REAL,
                            cached_at INTEGER NOT NULL
                        );
                    """);

            // 2. Search History table (rolling window of 30)
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS search_history (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            city_name TEXT NOT NULL,
                            temperature REAL NOT NULL,
                            condition TEXT NOT NULL,
                            searched_at INTEGER NOT NULL
                        );
                    """);

            // 3. Home City Config table (single-row configuration)
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS home_city_config (
                            id INTEGER PRIMARY KEY CHECK (id = 1),
                            city_name TEXT NOT NULL,
                            latitude REAL NOT NULL,
                            longitude REAL NOT NULL,
                            last_synced_at INTEGER
                        );
                    """);

            // 4. Historical Hourly Weather Data table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS home_city_hourly (
                            timestamp TEXT PRIMARY KEY,
                            temperature REAL NOT NULL,
                            humidity REAL NOT NULL,
                            precipitation REAL NOT NULL
                        );
                    """);

            // 5. ML Predictions table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS predictions (
                            forecast_date TEXT PRIMARY KEY,
                            predicted_temp_avg REAL NOT NULL,
                            predicted_temp_min REAL NOT NULL,
                            predicted_temp_max REAL NOT NULL,
                            condition_summary TEXT,
                            created_at INTEGER NOT NULL
                        );
                    """);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite database: " + e.getMessage(), e);
        }
    }

    // Section A: Cache & History

    @Override
    public Optional<Weather> getCachedWeather(String cityName, int ttlMinutes) {
        String sql = """
                    SELECT city_name, country, latitude, longitude,
                           temperature, feels_like, humidity, description, wind_speed, cached_at
                    FROM weather_cache
                    WHERE city_name = ? AND cached_at >= ?
                """;

        long cutoffEpochMillis = Instant.now().minus(Duration.ofMinutes(ttlMinutes)).toEpochMilli();

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cityName);
            pstmt.setLong(2, cutoffEpochMillis);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Location location = new Location(
                            rs.getString("city_name"),
                            rs.getString("country"),
                            rs.getDouble("latitude"),
                            rs.getDouble("longitude"));
                    Weather weather = new Weather(
                            location,
                            rs.getDouble("temperature"),
                            rs.getDouble("feels_like"),
                            rs.getInt("humidity"),
                            rs.getString("description"),
                            rs.getDouble("wind_speed"));
                    return Optional.of(weather);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reading cache for: " + cityName, e);
        }
        return Optional.empty();
    }

    @Override
    public void saveWeatherCache(Weather weather) {
        String sql = """
                    INSERT OR REPLACE INTO weather_cache
                    (city_name, country, latitude, longitude, temperature, feels_like, humidity, description, wind_speed, cached_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            Location loc = weather.getLocation();
            pstmt.setString(1, loc.getCity());
            pstmt.setString(2, loc.getCountry());
            pstmt.setDouble(3, loc.getLatitude());
            pstmt.setDouble(4, loc.getLongitude());
            pstmt.setDouble(5, weather.getTemperature());
            pstmt.setDouble(6, weather.getFeelsLike());
            pstmt.setInt(7, weather.getHumidity());
            pstmt.setString(8, weather.getDescription());
            pstmt.setDouble(9, weather.getWindSpeed());
            pstmt.setLong(10, Instant.now().toEpochMilli());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving cache for: " + weather.getLocation().getCity(), e);
        }
    }

    @Override
    public void addSearchHistory(Weather weather) {
        String insertSql = """
                    INSERT INTO search_history (city_name, temperature, condition, searched_at)
                    VALUES (?, ?, ?, ?)
                """;

        // Rolling window: keeps only the 35 newest searches
        String pruneSql = """
                    DELETE FROM search_history WHERE id NOT IN (
                        SELECT id FROM search_history ORDER BY searched_at DESC LIMIT 35
                    )
                """;

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, weather.getLocation().getCity());
                    insertStmt.setDouble(2, weather.getTemperature());
                    insertStmt.setString(3, weather.getDescription());
                    insertStmt.setLong(4, Instant.now().toEpochMilli());
                    insertStmt.executeUpdate();
                }

                try (Statement pruneStmt = conn.createStatement()) {
                    pruneStmt.executeUpdate(pruneSql);
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving search history", e);
        }
    }

    @Override
    public List<SearchRecord> getRecentSearches(int limit) {
        String sql = """
                    SELECT id, city_name, temperature, condition, searched_at
                    FROM search_history
                    ORDER BY searched_at DESC
                    LIMIT ?
                """;

        List<SearchRecord> records = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(new SearchRecord(
                            rs.getLong("id"),
                            rs.getString("city_name"),
                            rs.getDouble("temperature"),
                            rs.getString("condition"),
                            Instant.ofEpochMilli(rs.getLong("searched_at"))));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reading search history", e);
        }
        return records;
    }

    @Override
    public void clearCache() {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM weather_cache");
        } catch (SQLException e) {
            throw new RuntimeException("Error clearing cache", e);
        }
    }

    // Section B: Home City & ML Data
    @Override
    public Optional<HomeCityConfig> getHomeCityConfig() {
        String sql = "SELECT city_name, latitude, longitude, last_synced_at FROM home_city_config WHERE id = 1";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                long lastSyncedMillis = rs.getLong("last_synced_at");
                Instant lastSynced = rs.wasNull() ? null : Instant.ofEpochMilli(lastSyncedMillis);

                return Optional.of(new HomeCityConfig(
                        rs.getString("city_name"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude"),
                        lastSynced));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reading home city config", e);
        }
        return Optional.empty();
    }

    @Override
    public void saveHomeCityConfig(HomeCityConfig config) {
        String sql = """
                    INSERT OR REPLACE INTO home_city_config (id, city_name, latitude, longitude, last_synced_at)
                    VALUES (1, ?, ?, ?, ?)
                """;

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, config.cityName());
            pstmt.setDouble(2, config.latitude());
            pstmt.setDouble(3, config.longitude());
            if (config.lastSyncedAt() != null) {
                pstmt.setLong(4, config.lastSyncedAt().toEpochMilli());
            } else {
                pstmt.setNull(4, Types.BIGINT);
            }

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving home city config", e);
        }
    }

    @Override
    public void saveHourlyWeatherBatch(List<HourlyWeatherRecord> records) {
        String sql = """
                    INSERT OR REPLACE INTO home_city_hourly (timestamp, temperature, humidity, precipitation)
                    VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int count = 0;
                for (HourlyWeatherRecord record : records) {
                    pstmt.setString(1, record.timestamp().toString());
                    pstmt.setDouble(2, record.temperature());
                    pstmt.setDouble(3, record.humidity());
                    pstmt.setDouble(4, record.precipitation());
                    pstmt.addBatch();

                    if (++count % 1000 == 0) {
                        pstmt.executeBatch();
                    }
                }
                pstmt.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving hourly weather batch", e);
        }
    }

    @Override
    public List<HourlyWeatherRecord> getHourlyWeatherData(LocalDateTime from, LocalDateTime to) {
        String sql = """
                    SELECT timestamp, temperature, humidity, precipitation
                    FROM home_city_hourly
                    WHERE timestamp >= ? AND timestamp <= ?
                    ORDER BY timestamp ASC
                """;

        List<HourlyWeatherRecord> result = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, from.toString());
            pstmt.setString(2, to.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new HourlyWeatherRecord(
                            LocalDateTime.parse(rs.getString("timestamp")),
                            rs.getDouble("temperature"),
                            rs.getDouble("humidity"),
                            rs.getDouble("precipitation")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying hourly weather data", e);
        }
        return result;
    }

    @Override
    public int getHourlyRecordCount() {
        String sql = "SELECT COUNT(*) FROM home_city_hourly";
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting hourly records", e);
        }
        return 0;
    }

    @Override
    public void savePredictions(List<WeatherPrediction> predictions) {
        String sql = """
                    INSERT OR REPLACE INTO predictions
                    (forecast_date, predicted_temp_avg, predicted_temp_min, predicted_temp_max, condition_summary, created_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (WeatherPrediction p : predictions) {
                    pstmt.setString(1, p.forecastDate().toString());
                    pstmt.setDouble(2, p.predictedTempAvg());
                    pstmt.setDouble(3, p.predictedTempMin());
                    pstmt.setDouble(4, p.predictedTempMax());
                    pstmt.setString(5, p.conditionSummary());
                    pstmt.setLong(6, p.createdAt().toEpochMilli());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving predictions", e);
        }
    }

    @Override
    public List<WeatherPrediction> getPredictions(LocalDate fromDate) {
        String sql = """
                    SELECT forecast_date, predicted_temp_avg, predicted_temp_min, predicted_temp_max,
                           condition_summary, created_at
                    FROM predictions
                    WHERE forecast_date >= ?
                    ORDER BY forecast_date ASC
                """;

        List<WeatherPrediction> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fromDate.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new WeatherPrediction(
                            LocalDate.parse(rs.getString("forecast_date")),
                            rs.getDouble("predicted_temp_avg"),
                            rs.getDouble("predicted_temp_min"),
                            rs.getDouble("predicted_temp_max"),
                            rs.getString("condition_summary"),
                            Instant.ofEpochMilli(rs.getLong("created_at"))));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reading predictions", e);
        }
        return list;
    }
}
