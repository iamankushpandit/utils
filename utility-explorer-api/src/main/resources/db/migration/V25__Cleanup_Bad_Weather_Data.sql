-- V25__Cleanup_Bad_Weather_Data.sql
-- Remove weather data that used postal codes instead of FIPS codes
DELETE FROM fact_value 
WHERE source_id = 'WEATHER_INSIGHTS_API' 
  AND geo_level = 'STATE' 
  AND length(geo_id) = 2 
  AND geo_id !~ '^[0-9]+$'; -- Delete if not numeric
