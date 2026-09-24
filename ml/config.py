# Configuration settings for multi-variate ML pipeline
import os
from pathlib import Path

# Paths
BASE_DIR = Path(__file__).resolve().parent
PROJECT_DIR = BASE_DIR.parent
DB_PATH = PROJECT_DIR / "weather.db"
MODEL_DIR = BASE_DIR / "models"

TEMP_MODEL_PATH = MODEL_DIR / "prophet_temp_model.pkl"
HUMIDITY_MODEL_PATH = MODEL_DIR / "prophet_humidity_model.pkl"
RAIN_MODEL_PATH = MODEL_DIR / "rain_classifier.joblib"
RAINFALL_MODEL_PATH = MODEL_DIR / "rainfall_regressor.joblib"

# Backward compatibility alias
MODEL_PATH = TEMP_MODEL_PATH

# Ensure model directory exists
MODEL_DIR.mkdir(parents=True, exist_ok=True)

# Forecasting settings
FORECAST_DAYS = 7
CONFIDENCE_INTERVAL = 0.90  # 90% uncertainty interval for temperature/humidity bounds
