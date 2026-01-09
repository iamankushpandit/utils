-- V24__Fix_Weather_Metric_Geo_Levels.sql
-- Fix the supported_geo_levels for Weather metrics to include STATE

UPDATE metric
SET supported_geo_levels = 'STATE'
WHERE metric_id IN ('WEATHER_STRESS_INDEX', 'TEMP_CURRENT_F', 'TEMP_ANOMALY_F');
