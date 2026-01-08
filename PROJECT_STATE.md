# PROJECT_STATE.md — Utility Explorer

## Current status (as of 2026-01-06)
- Phase/Day: Day 14 (COMPLETED - MVP READY)
- Repo layout: Monorepo structure
- Local run: Docker Compose + Spring Boot + Vue UI fully functional

## What is built
- Memory Bank documentation structure
- Spring Boot API with health endpoint
- Docker Compose setup with Postgres
- Flyway migrations with core schema
- Seed data: 1 metric + 1 source + 5 US states + timeseries facts + sample runs
- Catalog endpoints: /metrics, /sources, /coverage
- Region endpoints: /regions/search, /regions/{geoLevel}/{geoId}, /regions/{geoLevel}/{geoId}/children
- Map endpoint: /map with legend stats and provenance
- Timeseries endpoint: /timeseries with sorted points and provenance
- Export endpoint: /export/csv with provenance columns
- Status endpoint: /status/sources with run history and configuration
- Ingestion framework: dispatcher + locking + mock plugin
- Source documentation: SOURCES.md with EIA + placeholders
- Vue UI shell: routes, transparency page with status widget
- Interactive map: Leaflet with state boundaries, choropleth coloring, click handlers
- Region drawer: current value + provenance + time series chart + CSV export
- Read-only Util Agent: QuerySpec validation + API key auth + structured responses
- Integration tests: Full endpoint coverage + Util Agent safety verification
- Complete documentation: DEMO.md with step-by-step walkthrough

## What is agreed (non-negotiable)
- No invented data (no forecasting/interpolation/imputation)
- No hidden blending across sources
- Every value must include provenance: retrieved_at (+ source_published_at when available)
- Show "No data available" when data is missing
- Local-first, cloud-ready, 12-factor config

## Key artifacts (authoritative)
1) Memory Bank files in .amazonq/rules/memory-bank/
2) DB migrations in utility-explorer-api/src/main/resources/db/migration/
3) Docker Compose setup
4) Spring Boot API with health endpoint

## Decisions made
- Geo levels v1: STATE, COUNTY, PLACE (Place = "city")
- Util Agent: read-only, must return grounded results, can answer cross-layer queries
- Repo structure: Monorepo with utility-explorer-api and utility-explorer-ui
- DB choice: Postgres

## Open decisions (must ask before proceeding)
- Map boundary format: GeoJSON vs TopoJSON (default: TopoJSON)
- Cron parsing dependency for nextRunAt (return null initially)

## Next planned step
- MVP COMPLETE: Ready for deployment and real data source integration

## Validation commands (MVP COMPLETE)
- docker compose up -d --build
- curl http://localhost:8080/actuator/health
- cd utility-explorer-ui && npm install && npm run dev
- Open http://localhost:5173 and follow DEMO.md walkthrough