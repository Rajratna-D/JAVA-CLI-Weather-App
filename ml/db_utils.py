# SQLite database utility functions for multi-variate weather ML
import sqlite3
import pandas as pd
from datetime import datetime, timezone
from config import DB_PATH

def get_connection():
    if not DB_PATH.exists():
        raise FileNotFoundError(f"Database file not found at: {DB_PATH}. Please sync data in the Java app first.")
    return sqlite3.connect(DB_PATH)

def load_training_data():
    """
    Loads historical hourly weather data from SQLite.
    Returns DataFrame with columns: ['timestamp', 'temperature', 'humidity', 'precipitation', 'ds']
    """
    with get_connection() as conn:
        query = """
            SELECT timestamp, temperature, humidity, precipitation
            FROM home_city_hourly
            ORDER BY timestamp ASC
        """
        df = pd.read_sql_query(query, conn)

    if df.empty:
        raise ValueError("No historical data found in 'home_city_hourly'. Please sync home city data from the Java app first.")

    df["ds"] = pd.to_datetime(df["timestamp"])
    df["temperature"] = df["temperature"].astype(float)
    df["humidity"] = df["humidity"].astype(float)
    df["precipitation"] = df["precipitation"].astype(float)

    return df

def save_predictions_to_db(daily_predictions):
    """
    Saves daily aggregated multi-variate predictions into SQLite 'predictions' table.
    Expects a list of dicts with:
    [forecast_date, predicted_temp_avg, predicted_temp_min, predicted_temp_max,
     predicted_humidity, predicted_rain_prob, predicted_rainfall_mm, condition_summary]
    """
    now_epoch_ms = int(datetime.now(timezone.utc).timestamp() * 1000)

    with get_connection() as conn:
        cursor = conn.cursor()

        # Ensure columns exist in case table was created with older schema
        for col_def in [
            ("predicted_humidity", "REAL DEFAULT 0.0"),
            ("predicted_rain_prob", "REAL DEFAULT 0.0"),
            ("predicted_rainfall_mm", "REAL DEFAULT 0.0"),
        ]:
            try:
                cursor.execute(f"ALTER TABLE predictions ADD COLUMN {col_def[0]} {col_def[1]}")
            except sqlite3.OperationalError:
                pass  # column already exists

        for p in daily_predictions:
            cursor.execute("""
                INSERT OR REPLACE INTO predictions
                (forecast_date, predicted_temp_avg, predicted_temp_min, predicted_temp_max,
                 predicted_humidity, predicted_rain_prob, predicted_rainfall_mm,
                 condition_summary, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                str(p["forecast_date"]),
                round(float(p["predicted_temp_avg"]), 1),
                round(float(p["predicted_temp_min"]), 1),
                round(float(p["predicted_temp_max"]), 1),
                round(float(p.get("predicted_humidity", 0.0)), 1),
                round(float(p.get("predicted_rain_prob", 0.0)), 1),
                round(float(p.get("predicted_rainfall_mm", 0.0)), 1),
                p.get("condition_summary", "Partly Cloudy"),
                now_epoch_ms
            ))
        conn.commit()
    print(f"Successfully saved {len(daily_predictions)} days of multi-variate predictions into SQLite database!")
