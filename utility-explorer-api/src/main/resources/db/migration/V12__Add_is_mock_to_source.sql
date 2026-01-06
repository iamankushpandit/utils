-- V12__Add_is_mock_to_source.sql

ALTER TABLE source
ADD COLUMN IF NOT EXISTS is_mock BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE source
SET is_mock = TRUE
WHERE source_id = 'EIA';
