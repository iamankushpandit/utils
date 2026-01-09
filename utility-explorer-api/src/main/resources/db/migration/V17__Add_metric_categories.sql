ALTER TABLE metric ADD COLUMN category TEXT;
ALTER TABLE metric ADD COLUMN subcategory TEXT;
ALTER TABLE metric ADD COLUMN visualization_json TEXT;
ALTER TABLE metric ADD COLUMN ingestion_config_json TEXT;
