import sys
import time

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

def run_pipeline():
    print("=" * 60, flush=True)
    print("  AI MULTI-VARIATE FORECAST ENGINE (Prophet + Scikit-Learn)", flush=True)
    print("=" * 60, flush=True)
    
    start_all = time.time()
    
    print("\n[Step 1/2] Training AI models (Temperature, Humidity, Rain & Precipitation)...", flush=True)
    from train import train_weather_models
    train_weather_models()
    
    print("\n[Step 2/2] Generating 7-day multi-variate weather predictions...", flush=True)
    from predict import run_predictions
    run_predictions()
    
    elapsed = time.time() - start_all
    print(f"\nAI Pipeline finished in {elapsed:.1f} seconds!", flush=True)

if __name__ == "__main__":
    run_pipeline()
