# Prophet model training script
import time
import pickle
from prophet import Prophet
from config import MODEL_PATH, CONFIDENCE_INTERVAL
from db_utils import load_training_data

def train_weather_model():
    print(" Loading historical weather data from SQLite...")
    df = load_training_data()
    print(f" Loaded {len(df):,} hourly records spanning from {df['ds'].min().date()} to {df['ds'].max().date()}.")

    print("\n Initializing Facebook Prophet model...")
    model = Prophet(
        yearly_seasonality=True,
        weekly_seasonality=False,  # Weather has no 7-day weekly work cycle
        daily_seasonality=True,    # Captures diurnal day/night temperature cycles
        interval_width=CONFIDENCE_INTERVAL,
        changepoint_prior_scale=0.05
    )

    print(" Training model on historical data (this takes ~20-30 seconds)...")
    start_time = time.time()
    model.fit(df[["ds", "y"]])
    duration = time.time() - start_time
    print(f" Training completed in {duration:.1f} seconds!")

    # Save trained model
    with open(MODEL_PATH, "wb") as f:
        pickle.dump(model, f)
    print(f" Saved trained model to: {MODEL_PATH}")

if __name__ == "__main__":
    train_weather_model()
