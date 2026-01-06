# Technical Architecture Memory Bank

## API Design Patterns
- All endpoints versioned: `/api/v1/...`
- Responses always include provenance fields
- OpenAPI/contract-first approach
- 12-factor configuration (env vars only)

## Core Database Tables
- `metric`: Layer definitions (id, name, unit, supported geo levels)
- `source`: Data source metadata + attribution
- `source_config`: Per-source cadence + scheduling
- `source_run`: Ingestion run history (SUCCESS/NO_CHANGE/FAILED)
- `fact_value`: Normalized numeric facts with provenance
- `region`: Geography hierarchy (STATE/COUNTY/PLACE)
- `raw_payload`: Immutable payload references with hash

## Ingestion Architecture
- **Dispatcher Pattern**: Lightweight scheduler runs every 10 minutes
- **Source Plugins**: Encapsulate fetch/check + normalize + upsert
- **Idempotent Upserts**: Facts keyed by (metric_id, source_id, geo_level, geo_id, period_start, period_end)
- **Change Detection**: Compare latest period/release vs stored data
- **Locking**: DB advisory locks per source_id to prevent overlap

## Copilot Safety Model
- **QuerySpec JSON**: Structured queries, not LLM-generated SQL
- **Validation**: Server validates against strict schema + allow-list
- **Read-only**: Cannot modify database
- **Provenance Required**: Must cite sources + timestamps
- **No Guessing**: Refuse when data missing

## Frontend Patterns
- **Map-first**: Choropleth + drilldown boundaries
- **No Data Gaps**: Show "No data available" vs inventing values
- **Provenance UI**: Source + period + retrieved timestamp in legend
- **Static Boundaries**: Pre-built TopoJSON/GeoJSON assets

## Security Baseline
- API keys for Copilot endpoints
- Rate limiting (IP-based)
- Strict CORS in production
- Secrets in env vars only
- Dependency scanning + pinned versions