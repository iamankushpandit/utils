# Architecture Overview

## System Architecture
- **Frontend**: Vue.js (map-first UI) - Day 10+
- **Backend**: Java Spring Boot (REST API + ingestion orchestration)
- **Database**: Postgres (facts + provenance + geo metadata)
- **Deployment**: Local-first (Docker Compose), cloud-ready

## Key Patterns
- Source-driven ingestion cadence
- Provenance tracking for all data
- Read-only Util Agent with QuerySpec validation
- 12-factor configuration
- Idempotent operations

## Database Design
Core tables: metric, source, source_config, source_run, fact_value, region, raw_payload

## API Design
- Versioned endpoints: `/api/v1/...`
- Provenance in all responses
- OpenAPI/contract-first approach

*Note: This is a placeholder created on Day 1. Detailed architecture is in memory bank files.*