# PROJECT_STATE.md — Utility Explorer

## Current status (as of 2026-01-06)
- Phase/Day: Day 5 (COMPLETED)
- Repo layout: Monorepo structure
- Local run: Docker Compose + Spring Boot + Flyway + All core APIs ready

## What is built
- Memory Bank documentation structure
- Spring Boot API with health endpoint
- Docker Compose setup with Postgres
- Flyway migrations with core schema
- Seed data: 1 metric + 1 source + 5 US states + sample facts
- Catalog endpoints: /metrics, /sources, /coverage
- Region endpoints: /regions/search, /regions/{geoLevel}/{geoId}, /regions/{geoLevel}/{geoId}/children
- Map endpoint: /map with legend stats and provenance

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
- Copilot: read-only, must return grounded results, can answer cross-layer queries
- Repo structure: Monorepo with utility-explorer-api and utility-explorer-ui
- DB choice: Postgres

## Open decisions (must ask before proceeding)
- Map boundary format: GeoJSON vs TopoJSON (default: TopoJSON)
- Cron parsing dependency for nextRunAt (return null initially)

## Next planned step
- Day 6: Implement GET /timeseries and GET /export/csv endpoints

## Validation commands (target)
- docker compose up -d
- curl http://localhost:8080/actuator/health
- npm run dev (UI) [Day 10+]