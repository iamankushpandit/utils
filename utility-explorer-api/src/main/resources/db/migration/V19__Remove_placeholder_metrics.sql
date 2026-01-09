-- V19__Remove_placeholder_metrics.sql
-- Remove placeholder metrics that are not yet implemented in code

-- Delete any facts associated with these metrics (should be none, but for safety)
DELETE FROM fact_value
WHERE metric_id IN ('ELEC_PRICE_RES', 'BB_SPEED_DOWN', 'ACS_MEDIAN_INCOME');

-- Delete the metrics themselves
DELETE FROM metric 
WHERE metric_id IN ('ELEC_PRICE_RES', 'BB_SPEED_DOWN', 'ACS_MEDIAN_INCOME');
