-- V5__Seed_timeseries_facts.sql

-- Insert additional electricity price facts for timeseries testing (Kansas over multiple months)
INSERT INTO fact_value (
    metric_id, source_id, geo_level, geo_id, period_start, period_end, 
    value_numeric, retrieved_at, source_published_at, is_aggregated, aggregation_method, payload_id
) VALUES
('ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH', 'EIA', 'STATE', '20', '2025-10-01', '2025-10-31', 14.1, '2026-01-06T14:15:00Z', '2025-11-15T00:00:00Z', false, null, null),
('ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH', 'EIA', 'STATE', '20', '2025-11-01', '2025-11-30', 14.3, '2026-01-06T14:15:00Z', '2025-12-15T00:00:00Z', false, null, null),
('ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH', 'EIA', 'STATE', '06', '2025-10-01', '2025-10-31', 27.8, '2026-01-06T14:15:00Z', '2025-11-15T00:00:00Z', false, null, null),
('ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH', 'EIA', 'STATE', '06', '2025-11-01', '2025-11-30', 28.0, '2026-01-06T14:15:00Z', '2025-12-15T00:00:00Z', false, null, null);