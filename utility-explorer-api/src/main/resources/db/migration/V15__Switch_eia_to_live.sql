-- V15__Switch_eia_to_live.sql

UPDATE source
SET is_mock = FALSE
WHERE source_id = 'EIA';

DELETE FROM fact_value
WHERE source_id = 'EIA';
