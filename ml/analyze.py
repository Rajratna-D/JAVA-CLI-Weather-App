import time
import pandas as pd
import numpy as np
from prophet import Prophet
from sklearn.metrics import mean_absolute_error, root_mean_squared_error
from db_utils import load_training_data

def evaluate_model_performance(df, test_days=30):
    test_hours = test_days * 24
    if len(df) <= test_hours * 2:
        print("Not enough data to perform a 30-day temporal split.")
        return

    print(f"\nPerforming Temporal Train/Test Split (Holding out last {test_days} days as test set)...")
    train_df = df.iloc[:-test_hours].copy()
    test_df = df.iloc[-test_hours:].copy()

    print(f"   • Training rows : {len(train_df):,} hours ({train_df['ds'].min().date()} to {train_df['ds'].max().date()})")
    print(f"   • Testing rows  : {len(test_df):,} hours ({test_df['ds'].min().date()} to {test_df['ds'].max().date()})")

    # Train on historical past
    model = Prophet(
        yearly_seasonality=True,
        weekly_seasonality=False,
        daily_seasonality=True,
        interval_width=0.90,
        changepoint_prior_scale=0.05
    )
    start_time = time.time()
    model.fit(train_df[["ds", "y"]])
    print(f"   • Trained validation model in {time.time() - start_time:.1f}s")

    # Predict the unseen 30-day test set
    future = model.make_future_dataframe(periods=test_hours, freq="h", include_history=False)
    forecast = model.predict(future)

    # Merge actual and predicted
    merged = pd.merge(test_df[["ds", "y"]], forecast[["ds", "yhat", "yhat_lower", "yhat_upper"]], on="ds")
    
    # Calculate metrics
    mae = mean_absolute_error(merged["y"], merged["yhat"])
    rmse = root_mean_squared_error(merged["y"], merged["yhat"])
    within_2c = (np.abs(merged["y"] - merged["yhat"]) <= 2.0).mean() * 100.0

    print("\n" + "=" * 60)
    print("           MODEL ACCURACY EVALUATION (TEST SET)")
    print("=" * 60)
    print(f"• Mean Absolute Error (MAE) : ±{mae:.2f}°C")
    print(f"• Root Mean Squared Error   : ±{rmse:.2f}°C")
    print(f"• Predictions within ±2.0°C : {within_2c:.1f}%")
    print("=" * 60)

def analyze_weather_data():
    print("Loading historical weather records from SQLite...")
    df = load_training_data()

    print("\n" + "=" * 60)
    print("             CLIMATE INSIGHTS FOR YOUR HOME CITY")
    print("=" * 60)

    # 1. Basic Stats
    total_hours = len(df)
    start_date = df["ds"].min().strftime("%Y-%m-%d")
    end_date = df["ds"].max().strftime("%Y-%m-%d")
    print(f"• Dataset Range       : {start_date} to {end_date}")
    print(f"• Total Hourly Rows   : {total_hours:,}")

    # 2. Temperature Extremes
    max_idx = df["y"].idxmax()
    min_idx = df["y"].idxmin()
    hottest = df.iloc[max_idx]
    coldest = df.iloc[min_idx]

    print(f"• All-Time Record High: {hottest['y']:.1f}°C (on {hottest['ds'].strftime('%Y-%m-%d %I:%M %p')})")
    print(f"• All-Time Record Low : {coldest['y']:.1f}°C (on {coldest['ds'].strftime('%Y-%m-%d %I:%M %p')})")
    print(f"• Historical Average  : {df['y'].mean():.1f}°C")

    # 3. Monthly Seasonal Breakdown
    df["month"] = df["ds"].dt.month_name()
    monthly = df.groupby("month", sort=False)["y"].agg(["mean", "min", "max"]).round(1)
    
    month_order = [
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ]
    monthly = monthly.reindex([m for m in month_order if m in monthly.index])

    print("\n" + "-" * 60)
    print(f"{'Month':<12} | {'Avg Temp':<10} | {'Record Low':<12} | {'Record High':<12}")
    print("-" * 60)
    for month, row in monthly.iterrows():
        print(f"{month:<12} | {row['mean']:.1f}°C     | {row['min']:.1f}°C      | {row['max']:.1f}°C")
    print("=" * 60)

    # 4. Out-of-sample Test Evaluation
    evaluate_model_performance(df, test_days=30)

if __name__ == "__main__":
    analyze_weather_data()
