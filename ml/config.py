# Configuration settings for ML pipeline
import os
from pathlib import Path

# Paths
BASE_DIR = Path(__file__).resolve().parent
PROJECT_DIR = BASE_DIR.parent
DB_PATH = PROJECT_DIR / "weather.db"
MODEL_DIR = BASE_DIR / "models"
MODEL_PATH = MODEL_DIR / "prophet_weather_model.pkl"

# Ensure model directory exists
MODEL_DIR.mkdir(parents=True, exist_ok=True)

# Forecasting settings
FORECAST_DAYS = 7
CONFIDENCE_INTERVAL = 0.90  # 90% uncertainty interval for min/max temperature bounds
