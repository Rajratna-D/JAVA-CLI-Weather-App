# Multi-variate Weather ML Training Script
# Trains:
# 1. Facebook Prophet Model for Temperature (min, avg, max)
# 2. Facebook Prophet Model for Relative Humidity
# 3. Scikit-Learn Classifier for Rain Probability (%)
# 4. Scikit-Learn Regressor for Expected Rainfall Amount (mm)

import time
import pickle
import joblib
import numpy as np
import pandas as pd
from prophet import Prophet
from config import TEMP_MODEL_PATH, HUMIDITY_MODEL_PATH, RAIN_MODEL_PATH, RAINFALL_MODEL_PATH, CONFIDENCE_INTERVAL
from db_utils import load_training_data
from sklearn.ensemble import HistGradientBoostingClassifier, HistGradientBoostingRegressor

def train_weather_models():
    print("📥 Loading historical climate data from SQLite...")
    df = load_training_data()
    print(f" Loaded {len(df):,} hourly records spanning from {df['ds'].min().date()} to {df['ds'].max().date()}.")

    # 1. Train Prophet Temperature Model
    print("\n🧠 [1/3] Training Prophet model on temperature cycles...")
    t0 = time.time()
    temp_df = pd.DataFrame({"ds": df["ds"], "y": df["temperature"]}).iloc[::2] # Sample 2-hourly for high accuracy & speed
    temp_model = Prophet(
        yearly_seasonality=True,
        weekly_seasonality=False,
        daily_seasonality=True,
        interval_width=CONFIDENCE_INTERVAL,
        changepoint_prior_scale=0.05
    )
    temp_model.fit(temp_df)
    with open(TEMP_MODEL_PATH, "wb") as f:
        pickle.dump(temp_model, f)
    print(f"✅ Temperature model trained in {time.time() - t0:.1f}s -> Saved to {TEMP_MODEL_PATH.name}")

    # 2. Train Prophet Humidity Model
    print("\n💧 [2/3] Training Prophet model on relative humidity diurnal cycles...")
    t0 = time.time()
    hum_df = pd.DataFrame({"ds": df["ds"], "y": df["humidity"]}).iloc[::3] # Sample 3-hourly for speed & stability
    hum_model = Prophet(
        yearly_seasonality=True,
        weekly_seasonality=False,
        daily_seasonality=True,
        interval_width=CONFIDENCE_INTERVAL,
        changepoint_prior_scale=0.05
    )
    hum_model.fit(hum_df)
    with open(HUMIDITY_MODEL_PATH, "wb") as f:
        pickle.dump(hum_model, f)
    print(f"✅ Humidity model trained in {time.time() - t0:.1f}s -> Saved to {HUMIDITY_MODEL_PATH.name}")

    # 3. Train Rain Probability Classifier & Rainfall Volume Regressor
    print("\n🌧️ [3/3] Training Scikit-Learn Rain Probability & Rainfall engines...")
    t0 = time.time()

    # Aggregate by day
    df["date"] = df["ds"].dt.date
    daily = df.groupby("date").agg({
        "temperature": "mean",
        "humidity": "mean",
        "precipitation": "sum"
    }).reset_index()

    doy = pd.to_datetime(daily["date"]).dt.dayofyear
    sin_doy = np.sin(2 * np.pi * doy / 365.25)
    cos_doy = np.cos(2 * np.pi * doy / 365.25)

    X = pd.DataFrame({
        "sin_doy": sin_doy,
        "cos_doy": cos_doy,
        "temperature": daily["temperature"],
        "humidity": daily["humidity"]
    })
    y_rain = (daily["precipitation"] >= 0.2).astype(int)

    # Classifier for rain probability
    rain_clf = HistGradientBoostingClassifier(random_state=42)
    rain_clf.fit(X, y_rain)
    joblib.dump(rain_clf, RAIN_MODEL_PATH)

    # Regressor for expected rainfall on wet days
    wet_mask = y_rain == 1
    if wet_mask.sum() > 20:
        rainfall_reg = HistGradientBoostingRegressor(random_state=42)
        rainfall_reg.fit(X[wet_mask], daily.loc[wet_mask, "precipitation"])
    else:
        rainfall_reg = None

    joblib.dump(rainfall_reg, RAINFALL_MODEL_PATH)
    print(f"✅ Rain engines trained in {time.time() - t0:.1f}s -> Saved to {RAIN_MODEL_PATH.name}")

    print("\n🎉 Multi-variate weather training pipeline completed successfully!")

train_weather_model = train_weather_models

if __name__ == "__main__":
    train_weather_models()
