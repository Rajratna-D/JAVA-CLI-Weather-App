import sys
import time

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

def run_pipeline():
    print("=" * 60, flush=True)
    print("  AI WEATHER FORECAST ENGINE (Facebook Prophet)", flush=True)
    print("=" * 60, flush=True)
    
    start_all = time.time()
    
    print("\n[Step 1/2] Training AI model on 10-year hourly dataset...", flush=True)
    from train import train_weather_model
    train_weather_model()
    
    print("\n[Step 2/2] Generating 7-day weather predictions...", flush=True)
    from predict import run_predictions
    run_predictions()
    
    elapsed = time.time() - start_all
    print(f"\nAI Pipeline finished in {elapsed:.1f} seconds!", flush=True)

if __name__ == "__main__":
    run_pipeline()
