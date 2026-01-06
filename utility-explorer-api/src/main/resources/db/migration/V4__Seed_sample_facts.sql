-- V4__Seed_sample_facts.sql

-- Insert sample electricity price facts for testing map endpoint
INSERT INTO fact_value (
    metric_id, source_id, geo_level, geo_id, period_start, period_end, 
    value_numeric, retrieved_at, source_published_at, is_aggregated, aggregation_method, payload_id
) VALUES
('ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH', 'EIA', 'STATE', '20', '2025-12-01', '2025-12-31', 14.6, '2026-01-06T14:15:00Z', '2026-01-02T00:00:00Z', false, null, null),
('ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH', 'EIA', 'STATE', '06', '2025-12-01', '2025-12-31', 28.1, '2026-01-06T14:15:00Z', '2026-01-02T00:00:00Z', false, null, null),
('ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH', 'EIA', 'STATE', '48', '2025-12-01', '2025-12-31', 12.3, '2026-01-06T14:15:00Z', '2026-01-02T00:00:00Z', false, null, null);