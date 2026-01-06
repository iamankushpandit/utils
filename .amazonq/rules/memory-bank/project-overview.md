# Utility Explorer - Project Memory Bank

## Core Purpose
Map-first, transparency-first dashboard that visualizes **free public utility-related datasets** (electricity, broadband, water, wastewater) on a US map with drilldowns and historical charts.

## Non-Negotiable Principles
1. **No ads, affiliate links, or sponsored content**
2. **No data invention**: No forecasting, smoothing, estimation, or imputation
3. **No silent blending**: Multiple sources for same metric shown separately
4. **Provenance everywhere**: Every value includes source + retrieved timestamp
5. **Free public data only** (no paid datasets in MVP)

## Architecture Stack
- **Frontend**: Vue.js (map-first UI)
- **Backend**: Java Spring Boot (REST API + ingestion orchestration)
- **Database**: Postgres (facts + provenance + geo metadata)
- **Deployment**: Local-first (Docker Compose), cloud-ready (single VM/VPS)

## Key Design Decisions
- **Geography IDs**: STATE (2-digit FIPS), COUNTY (5-digit FIPS), PLACE (Census Place GEOID)
- **Source-driven cadence**: Each data source has its own update schedule
- **Read-only Copilot**: QuerySpec JSON → validated → SQL (no LLM-generated SQL)
- **Transparent aggregation only**: Clearly labeled, reproducible, documented

## Data Integrity Model
Every displayed value must include:
- `source_id`, `source_name`
- `retrieved_at` (when we fetched)
- `source_published_at` (if source provides it)
- `geo_level_supported` for that metric/source
- Optional: `payload_hash` for immutability proof

## MVP Scope
- Map Explorer with 1-2 layers + at least 1 data source per layer
- Drilldown boundaries (values only where supported)
- History chart + CSV export
- Transparency page (static ideology + dynamic status)
- Ingestion dispatcher with per-source cadence
- Read-only Copilot for state-level cross-layer queries