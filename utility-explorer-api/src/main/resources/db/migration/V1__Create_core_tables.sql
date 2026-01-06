-- V1__Create_core_tables.sql

-- 1) Metrics catalog
CREATE TABLE metric (
  metric_id           TEXT PRIMARY KEY,
  name                TEXT NOT NULL,
  unit                TEXT NOT NULL,
  description         TEXT,
  default_granularity TEXT NOT NULL,  -- MONTH|QUARTER|YEAR|EVENT
  supported_geo_levels TEXT NOT NULL  -- e.g. 'STATE,COUNTY' (simple MVP)
);

-- 2) Source registry
CREATE TABLE source (
  source_id           TEXT PRIMARY KEY,
  name                TEXT NOT NULL,
  type                TEXT NOT NULL,  -- PUBLIC (future: UPLOADED)
  terms_url           TEXT,
  attribution_text    TEXT,
  notes               TEXT
);

-- 3) Source scheduling config
CREATE TABLE source_config (
  source_id           TEXT PRIMARY KEY REFERENCES source(source_id),
  enabled             BOOLEAN NOT NULL DEFAULT TRUE,
  schedule_cron       TEXT NOT NULL,          -- cron string
  timezone            TEXT NOT NULL DEFAULT 'UTC',
  check_strategy      TEXT NOT NULL,          -- CHECK_AND_INGEST_IF_NEW | INGEST_ALWAYS
  max_lookback_periods INT NOT NULL DEFAULT 3,
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4) Ingestion runs
CREATE TABLE source_run (
  run_id              UUID PRIMARY KEY,
  source_id           TEXT NOT NULL REFERENCES source(source_id),
  started_at          TIMESTAMPTZ NOT NULL,
  ended_at            TIMESTAMPTZ,
  status              TEXT NOT NULL,  -- SUCCESS|NO_CHANGE|FAILED
  rows_upserted       INT NOT NULL DEFAULT 0,
  error_summary       TEXT
);

CREATE INDEX idx_source_run_source_started ON source_run(source_id, started_at DESC);

-- 5) Raw payload reference
CREATE TABLE raw_payload (
  payload_id          UUID PRIMARY KEY,
  source_id           TEXT NOT NULL REFERENCES source(source_id),
  run_id              UUID NOT NULL REFERENCES source_run(run_id),
  payload_hash        TEXT NOT NULL,
  storage_ref         TEXT NOT NULL,   -- local path or key (future S3)
  stored_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6) Regions (geo hierarchy)
CREATE TABLE region (
  region_pk           UUID PRIMARY KEY,
  geo_level           TEXT NOT NULL,  -- STATE|COUNTY|PLACE
  geo_id              TEXT NOT NULL,  -- FIPS or GEOID
  name                TEXT NOT NULL,
  parent_region_pk    UUID REFERENCES region(region_pk),
  centroid_lat        DOUBLE PRECISION,
  centroid_lon        DOUBLE PRECISION,
  UNIQUE (geo_level, geo_id)
);

-- 7) Facts (normalized numeric values)
CREATE TABLE fact_value (
  metric_id           TEXT NOT NULL REFERENCES metric(metric_id),
  source_id           TEXT NOT NULL REFERENCES source(source_id),

  geo_level           TEXT NOT NULL,  -- STATE|COUNTY|PLACE
  geo_id              TEXT NOT NULL,

  period_start        DATE NOT NULL,
  period_end          DATE NOT NULL,

  value_numeric        NUMERIC NOT NULL,

  retrieved_at        TIMESTAMPTZ NOT NULL,
  source_published_at TIMESTAMPTZ,

  is_aggregated       BOOLEAN NOT NULL DEFAULT FALSE,
  aggregation_method  TEXT,

  payload_id          UUID REFERENCES raw_payload(payload_id),

  PRIMARY KEY (metric_id, source_id, geo_level, geo_id, period_start, period_end),

  CHECK (
    (is_aggregated = FALSE AND aggregation_method IS NULL)
    OR
    (is_aggregated = TRUE AND aggregation_method IS NOT NULL)
  )
);

CREATE INDEX idx_fact_lookup ON fact_value(metric_id, source_id, geo_level, geo_id, period_start);