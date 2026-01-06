-- V2__Seed_initial_data.sql

-- Insert 1 metric (electricity pricing)
INSERT INTO metric (metric_id, name, unit, description, default_granularity, supported_geo_levels) VALUES
('ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH', 'Electricity Retail Price', 'cents/kWh', 'Average retail electricity price', 'MONTH', 'STATE');

-- Insert 1 source (EIA placeholder)
INSERT INTO source (source_id, name, type, terms_url, attribution_text, notes) VALUES
('EIA', 'U.S. Energy Information Administration', 'PUBLIC', 'https://www.eia.gov/about/copyrights_reuse.php', 'Source: U.S. Energy Information Administration', 'Official federal energy statistics');

-- Insert source configuration
INSERT INTO source_config (source_id, enabled, schedule_cron, timezone, check_strategy, max_lookback_periods) VALUES
('EIA', true, '0 0 9 * * MON', 'UTC', 'CHECK_AND_INGEST_IF_NEW', 3);