-- V13__Mark_additional_sources_mock.sql

UPDATE source
SET is_mock = TRUE
WHERE source_id IN ('FCC_BROADBAND', 'EPA_WATER');
