-- V20__Cleanup_metrics_after_ingestion_fix.sql
-- Ingestion service was re-inserting these on startup. Now that configuration is fixed,
-- we remove them one last time.

DELETE FROM fact_value
WHERE metric_id IN ('ELEC_PRICE_RES', 'BB_SPEED_DOWN', 'ACS_MEDIAN_INCOME');

DELETE FROM metric 
WHERE metric_id IN ('ELEC_PRICE_RES', 'BB_SPEED_DOWN', 'ACS_MEDIAN_INCOME');
