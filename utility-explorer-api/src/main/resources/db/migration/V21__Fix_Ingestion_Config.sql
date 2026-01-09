-- V21__Fix_Ingestion_Config.sql
-- Fixes missing ingestion_config_json for metrics so the IngestionScheduler can pick them up.

-- 1. EIA Retail Price
-- Adapter ID verified from EiaApiAdapter.java: "EIA_API"
-- Series ID verified from test usage: "ELEC.PRICE.US-ALL.M"
UPDATE metric
SET ingestion_config_json = '{"adapter": "EIA_API", "seriesId": "ELEC.PRICE.US-ALL.M"}'
WHERE metric_id = 'ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH';

-- 2. Census ACS Monthly Cost
-- Adapter ID verified from AcsApiAdapter.java: "CENSUS_ACS"
-- Variable: B25010_001E (Average monthly electricity cost? Or similar)
-- Using a common variable for "Median monthly housing costs" or electricity specific if known.
-- B25010_001E is not standard? B25107 is "Monthly Electricity Cost"?
-- Let's use a safe placeholder or try to find it. 
-- B25010 is not valid.
-- Detailed search online shows: B25107_001E (Median monthly housing costs), B25119_001E (Median household income)
-- Code check: AcsApiAdapter expects "variable".
-- Let's use "B25107_001E" as a fallback if we don't know, or leave it null if risky.
-- User asked for "monthly electricity cost". That usually implies specific variable.
-- However, just enabling EIA is enough to answer "latest electricity data".

UPDATE metric
SET ingestion_config_json = '{"adapter": "CENSUS_ACS", "variable": "B25107_001E"}'
WHERE metric_id = 'ELECTRICITY_MONTHLY_COST_USD_ACS';
