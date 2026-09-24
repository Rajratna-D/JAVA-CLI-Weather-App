# Prophet forecast and prediction script
import pickle
import pandas as pd
from datetime import datetime, timezone
from config import MODEL_PATH, FORECAST_DAYS
from db_utils import save_predictions_to_db

def generate_condition_summary(avg_temp, min_temp, max_temp):
    """Generates an intuitive condition summary based on predicted temperatures."""
    if avg_temp >= 33.0:
        return "Hot & Sunny"
    elif avg_temp >= 28.0:
        return "Warm & Clear"
    elif avg_temp >= 22.0:
        return "Pleasant & Mild"
    elif avg_temp >= 15.0:
        return "Cool & Breezy"
    else:
        return "Chilly"

def run_predictions():
    if not MODEL_PATH.exists():
        raise FileNotFoundError(f"Model file not found at: {MODEL_PATH}. Please run train.py first!")

    print(f" Loading trained Prophet model from {MODEL_PATH}...")
    with open(MODEL_PATH, "rb") as f:
        model = pickle.load(f)

    # Generate future hourly timestamps (7 days * 24 hours = 168 hours)
    future_hours = FORECAST_DAYS * 24
    print(f" Projecting weather forecast for the next {FORECAST_DAYS} days ({future_hours} hours)...")
    
    future = model.make_future_dataframe(periods=future_hours, freq="h", include_history=False)
    forecast = model.predict(future)

    # Group by calendar date to calculate daily metrics
    forecast["date"] = forecast["ds"].dt.date
    daily_records = []

    for date, group in forecast.groupby("date"):
        avg_temp = group["yhat"].mean()
        min_temp = group["yhat_lower"].min()
        max_temp = group["yhat_upper"].max()
        summary = generate_condition_summary(avg_temp, min_temp, max_temp)

        daily_records.append({
            "forecast_date": str(date),
            "predicted_temp_avg": avg_temp,
            "predicted_temp_min": min_temp,
            "predicted_temp_max": max_temp,
            "condition_summary": summary
        })

    # Save to SQLite database
    save_predictions_to_db(daily_records)

    # Print a summary table in terminal
    print("\n" + "=" * 65)
    print(f"{'Date':<12} | {'Avg Temp':<10} | {'Min - Max Temp':<18} | {'Summary':<15}")
    print("=" * 65)
    for r in daily_records:
        min_max = f"{r['predicted_temp_min']:.1f}°C - {r['predicted_temp_max']:.1f}°C"
        print(f"{r['forecast_date']:<12} | {r['predicted_temp_avg']:.1f}°C     | {min_max:<18} | {r['condition_summary']:<15}")
    print("=" * 65)

if __name__ == "__main__":
    run_predictions()
