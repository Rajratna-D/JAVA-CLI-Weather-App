# Multi-variate Prophet & Scikit-Learn Prediction Script
import pickle
import joblib
import numpy as np
import pandas as pd
from datetime import datetime
from config import TEMP_MODEL_PATH, HUMIDITY_MODEL_PATH, RAIN_MODEL_PATH, RAINFALL_MODEL_PATH, FORECAST_DAYS
from db_utils import save_predictions_to_db

def generate_condition_summary(avg_temp, min_temp, max_temp, humidity, rain_prob, rainfall_mm):
    """
    Intelligently generates weather summary taking into account temperature,
    relative humidity, rain probability, and expected precipitation volume.
    """
    if rain_prob >= 70.0:
        if rainfall_mm >= 12.0:
            return "⛈️ Heavy Rain"
        else:
            return "🌧️ Rain Likely"
    elif rain_prob >= 40.0:
        return "🌦️ Showers Likely"
    elif rain_prob >= 20.0:
        return "⛅ Chance of Rain"
    else:
        # Dry conditions
        if humidity >= 85.0:
            return "🌫️ Humid / Misty"
        elif avg_temp >= 33.0:
            return "☀️ Hot & Sunny"
        elif avg_temp >= 26.0:
            return "🌤️ Warm & Clear"
        elif avg_temp >= 20.0:
            return "⛅ Mild & Pleasant"
        elif avg_temp >= 14.0:
            return "🌥️ Cool & Breezy"
        else:
            return "❄️ Chilly"

def run_predictions():
    if not TEMP_MODEL_PATH.exists():
        raise FileNotFoundError(f"Model file not found at: {TEMP_MODEL_PATH}. Please run train.py first!")

    print(f" Loading trained models from disk...")
    with open(TEMP_MODEL_PATH, "rb") as f:
        temp_model = pickle.load(f)

    hum_model = None
    if HUMIDITY_MODEL_PATH.exists():
        with open(HUMIDITY_MODEL_PATH, "rb") as f:
            hum_model = pickle.load(f)

    rain_clf = None
    if RAIN_MODEL_PATH.exists():
        rain_clf = joblib.load(RAIN_MODEL_PATH)

    rainfall_reg = None
    if RAINFALL_MODEL_PATH.exists():
        try:
            rainfall_reg = joblib.load(RAINFALL_MODEL_PATH)
        except Exception:
            rainfall_reg = None

    # Generate future hourly timestamps (7 days * 24 hours = 168 hours)
    future_hours = FORECAST_DAYS * 24
    print(f" Projecting multi-variate forecast for the next {FORECAST_DAYS} days ({future_hours} hours)...")

    future = temp_model.make_future_dataframe(periods=future_hours, freq="h", include_history=False)
    temp_forecast = temp_model.predict(future)

    hum_forecast = None
    if hum_model is not None:
        hum_forecast = hum_model.predict(future)

    temp_forecast["date"] = temp_forecast["ds"].dt.date
    if hum_forecast is not None:
        hum_forecast["date"] = hum_forecast["ds"].dt.date

    daily_records = []

    for date, temp_group in temp_forecast.groupby("date"):
        avg_temp = float(temp_group["yhat"].mean())
        min_temp = float(temp_group["yhat_lower"].min())
        max_temp = float(temp_group["yhat_upper"].max())

        # Humidity calculation
        if hum_forecast is not None:
            hum_group = hum_forecast[hum_forecast["date"] == date]
            avg_hum = float(np.clip(hum_group["yhat"].mean(), 10.0, 100.0))
        else:
            avg_hum = 55.0

        # Rain Probability & Expected Rainfall calculation
        doy = pd.to_datetime(date).dayofyear
        sin_doy = np.sin(2 * np.pi * doy / 365.25)
        cos_doy = np.cos(2 * np.pi * doy / 365.25)
        feature_df = pd.DataFrame([{
            "sin_doy": sin_doy,
            "cos_doy": cos_doy,
            "temperature": avg_temp,
            "humidity": avg_hum
        }])

        if rain_clf is not None:
            rain_prob = float(rain_clf.predict_proba(feature_df)[0][1] * 100.0)
            rain_prob = float(np.clip(rain_prob, 0.0, 100.0))
        else:
            rain_prob = 0.0

        if rain_prob >= 20.0 and rainfall_reg is not None:
            expected_rainfall = float(np.clip(rainfall_reg.predict(feature_df)[0], 0.0, 200.0))
        else:
            expected_rainfall = 0.0

        summary = generate_condition_summary(avg_temp, min_temp, max_temp, avg_hum, rain_prob, expected_rainfall)

        daily_records.append({
            "forecast_date": str(date),
            "predicted_temp_avg": avg_temp,
            "predicted_temp_min": min_temp,
            "predicted_temp_max": max_temp,
            "predicted_humidity": avg_hum,
            "predicted_rain_prob": rain_prob,
            "predicted_rainfall_mm": expected_rainfall,
            "condition_summary": summary
        })

    # Save to SQLite database
    save_predictions_to_db(daily_records)

    # Print summary table in terminal
    print("\n" + "=" * 80)
    print(f"{'Date':<11} | {'Avg Temp':<9} | {'Min - Max':<15} | {'Humidity':<9} | {'Rain Prob':<10} | {'Rain (mm)':<10} | {'Summary'}")
    print("=" * 80)
    for r in daily_records:
        min_max = f"{r['predicted_temp_min']:.1f}° - {r['predicted_temp_max']:.1f}°C"
        print(f"{r['forecast_date']:<11} | {r['predicted_temp_avg']:.1f}°C    | {min_max:<15} | {r['predicted_humidity']:.0f}%     | {r['predicted_rain_prob']:.0f}%       | {r['predicted_rainfall_mm']:.1f} mm    | {r['condition_summary']}")
    print("=" * 80)

if __name__ == "__main__":
    run_predictions()
