-- V23__Enable_Weather_Source.sql
-- Enable the Weather source in source_config

INSERT INTO source_config (source_id, enabled, schedule_cron, timezone, check_strategy, max_lookback_periods, updated_at)
VALUES (
    'WEATHER_INSIGHTS_API', 
    true, 
    '0 0 6 * * *', -- Daily at 6am UTC
    'UTC', 
    'INGEST_ALWAYS', 
    1,
    NOW()
)
ON CONFLICT (source_id) DO UPDATE SET
    enabled = EXCLUDED.enabled,
    schedule_cron = EXCLUDED.schedule_cron,
    check_strategy = EXCLUDED.check_strategy,
    updated_at = NOW();
