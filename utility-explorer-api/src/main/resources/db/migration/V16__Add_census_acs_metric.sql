-- V16__Add_census_acs_metric.sql

INSERT INTO metric (metric_id, name, unit, description, default_granularity, supported_geo_levels) VALUES
('ELECTRICITY_MONTHLY_COST_USD_ACS',
 'Monthly Electricity Cost (ACS)',
 'USD/month',
 'Estimated average monthly electricity cost from ACS 5-year distribution bins (>= $250 bucket assumed $275).',
 'YEAR',
 'STATE,COUNTY,PLACE')
ON CONFLICT (metric_id) DO NOTHING;

INSERT INTO source (source_id, name, type, terms_url, attribution_text, notes, is_mock) VALUES
('CENSUS_ACS',
 'U.S. Census Bureau ACS',
 'PUBLIC',
 'https://www.census.gov/data/developers/about/terms-of-service.html',
 'Source: U.S. Census Bureau ACS (5-year)',
 'ACS 5-year estimates for monthly electricity cost.',
 false)
ON CONFLICT (source_id) DO NOTHING;

INSERT INTO source_config (source_id, enabled, schedule_cron, timezone, check_strategy, max_lookback_periods)
VALUES ('CENSUS_ACS', true, '0 0 10 15 12 *', 'UTC', 'INGEST_ALWAYS', 5)
ON CONFLICT (source_id) DO NOTHING;
