-- V9__Seed_additional_metrics.sql

INSERT INTO metric (metric_id, name, unit, description, default_granularity, supported_geo_levels) VALUES
('BROADBAND_COVERAGE_PERCENT', 'Broadband Coverage', 'percent', 'Households with broadband access', 'MONTH', 'STATE'),
('WATER_QUALITY_COMPLIANCE_PERCENT', 'Water Quality Compliance', 'percent', 'Systems in compliance with water quality standards', 'MONTH', 'STATE')
ON CONFLICT (metric_id) DO NOTHING;
