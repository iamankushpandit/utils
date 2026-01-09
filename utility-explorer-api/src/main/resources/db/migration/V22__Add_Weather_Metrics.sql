-- V22__Add_Weather_Metrics.sql

-- 1. Insert Source
INSERT INTO source (source_id, name, type, attribution_text, notes)
VALUES ('WEATHER_INSIGHTS_API', 'Weather Insights API', 'PUBLIC', 'Mock Weather Data', 'Weather metrics for correlation analysis')
ON CONFLICT (source_id) DO NOTHING;

-- 2. Insert Metrics
-- Note: 'ingestion_config_json' is populated to align with what IngestionService might expect if it reloads from DB,
-- though metrics.yaml might override it.
INSERT INTO metric (metric_id, name, unit, description, default_granularity, supported_geo_levels, ingestion_config_json)
VALUES 
('WEATHER_STRESS_INDEX', 'Weather Stress Index', 'Index (0-1)', 'Calculated stress index based on extreme weather events', 'MONTH', 'STATE,NATIONAL', '{"adapter": "WEATHER_INSIGHTS"}'),
('TEMP_CURRENT_F', 'Current Temperature', 'F', 'Average Monthly Temperature in Fahrenheit', 'MONTH', 'STATE,NATIONAL', '{"adapter": "WEATHER_INSIGHTS"}'),
('TEMP_ANOMALY_F', 'Temperature Anomaly', 'F', 'Deviation from 10-year average', 'MONTH', 'STATE,NATIONAL', '{"adapter": "WEATHER_INSIGHTS"}')
ON CONFLICT (metric_id) DO NOTHING;
