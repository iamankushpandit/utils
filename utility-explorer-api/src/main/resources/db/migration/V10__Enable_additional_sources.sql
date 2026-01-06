-- V10__Enable_additional_sources.sql

UPDATE source_config
SET enabled = true
WHERE source_id IN ('FCC_BROADBAND', 'EPA_WATER');
