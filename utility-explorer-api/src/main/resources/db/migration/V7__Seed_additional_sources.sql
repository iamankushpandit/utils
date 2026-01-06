-- V7__Seed_additional_sources.sql

-- Insert placeholder sources for documentation (not yet implemented)
INSERT INTO source (source_id, name, type, terms_url, attribution_text, notes) VALUES
('FCC_BROADBAND', 'Federal Communications Commission - Broadband Data', 'PUBLIC', 'https://www.fcc.gov/general/fcc-data-policy', 'Source: Federal Communications Commission', 'Form 477 broadband deployment data (placeholder)'),
('EPA_WATER', 'Environmental Protection Agency - Water Quality', 'PUBLIC', 'https://www.epa.gov/data-policy', 'Source: U.S. Environmental Protection Agency', 'Safe Drinking Water Information System (placeholder)');

-- Insert source configurations (disabled until implemented)
INSERT INTO source_config (source_id, enabled, schedule_cron, timezone, check_strategy, max_lookback_periods) VALUES
('FCC_BROADBAND', false, '0 0 9 1 * *', 'UTC', 'CHECK_AND_INGEST_IF_NEW', 2),
('EPA_WATER', false, '0 0 9 15 * *', 'UTC', 'CHECK_AND_INGEST_IF_NEW', 4);