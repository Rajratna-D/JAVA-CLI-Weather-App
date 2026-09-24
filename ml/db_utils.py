# SQLite database utility functions
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
    Loads historical hourly weather data from SQLite and formats it for Prophet.
    Prophet expects columns 'ds' (datestamp) and 'y' (target variable).
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

    # Convert timestamp to datetime
    df["ds"] = pd.to_datetime(df["timestamp"])
    df["y"] = df["temperature"].astype(float)

    return df

def save_predictions_to_db(daily_predictions):
    """
    Saves daily aggregated predictions into the SQLite 'predictions' table.
    Expects a list of dicts or DataFrame with:
    [forecast_date, predicted_temp_avg, predicted_temp_min, predicted_temp_max, condition_summary]
    """
    now_epoch_ms = int(datetime.now(timezone.utc).timestamp() * 1000)

    with get_connection() as conn:
        cursor = conn.cursor()
        for p in daily_predictions:
            cursor.execute("""
                INSERT OR REPLACE INTO predictions
                (forecast_date, predicted_temp_avg, predicted_temp_min, predicted_temp_max, condition_summary, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
            """, (
                str(p["forecast_date"]),
                round(float(p["predicted_temp_avg"]), 1),
                round(float(p["predicted_temp_min"]), 1),
                round(float(p["predicted_temp_max"]), 1),
                p.get("condition_summary", "Partly Cloudy"),
                now_epoch_ms
            ))
        conn.commit()
    print(f"Successfully saved {len(daily_predictions)} days of predictions into SQLite database!")
