-- V6__Seed_sample_runs.sql

-- Insert sample source runs for testing status endpoint
INSERT INTO source_run (run_id, source_id, started_at, ended_at, status, rows_upserted, error_summary) VALUES
('550e8400-e29b-41d4-a716-446655440101', 'EIA', '2026-01-05T14:00:00Z', '2026-01-05T14:00:05Z', 'SUCCESS', 3, null),
('550e8400-e29b-41d4-a716-446655440102', 'EIA', '2026-01-06T14:00:00Z', '2026-01-06T14:00:03Z', 'NO_CHANGE', 0, null);