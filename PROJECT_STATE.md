# PROJECT_STATE.md — Utility Explorer

## Current status (as of 2026-01-08)
- Phase: Phase 5 - Generative Intelligence (Optimization)
- Repo layout: Maven Multi-Module (`parent`, `api`, `shared`, `adapters`, `ingestion`) + Microservices (`intelligence`)
- Architecture: Event-Driven Metadata Broadcasting (Spring Boot -> Kafka -> Python Consumer -> Vector DB)
- **Status**: Local LLM (`ollama`) successfully integrated and validated.
    - **Models**: Usage of `qwen2.5-coder:1.5b` for Text-to-SQL logic.
    - **Performance**: High confidence on Regex Rules (0.3s), high accuracy on GenAI fallback for complex queries (4s).
    - **Verification**: 46/55 Automated Tests Passed (83%), including GenAI fallback and Forecasting scenarios.

## Upcoming Stories: Phase 6 - Frontend & Visualization
1.  **Story 1 (UI Integration)**: Update the Agent Chat UI to display the "GenAI" badge when an answer was auto-generated.
2.  **Story 2 (Markdown)**: Ensure the Agent UI properly renders the Markdown tables returned by GenAI.
3.  **Story 3 (Visuals)**: Improve the "List Intent" maps for county-level data.

## Change Log (Recent)
### Phase 5: Generative Intelligence (Jan 2026)
- **Story 1 (Infra)**: Added `llm-mesh` container running Ollama.
- **Story 2 (Client)**: Implemented `LLMClient` in Python for async generation.
- **Story 3 (Prompting)**: Developed "Schema-Aware" prompt for Text-to-SQL.
- **Story 4 (Hybrid AI)**: Designed "Smart Fallback" (Rules -> GenAI) architecture.
- **Story 5 (Verification)**: Validated system with 55-question test suite (83% Pass Rate). Confirmed "Prediction" intent works seamlessly.

### Phase 4: Automated Metadata Discovery (Jan 2026)
- **Story 10 (Metadata Schema)**: Defined `MetricMetadata` schema in Postgres and shared DTOs for Kafka messages.
- **Story 11 (Adapter Interface)**: Updated `IngestionAdapter` interface to include `getMetricDefinitions()`.
- **Story 12 (Metadata Broadcast)**: Implemented `MetadataBroadcaster` in Ingestion Service to auto-announce available metrics to `system.metadata.metrics` topic on startup.
- **Story 13 (AI Consumption)**: Implemented Kafka Consumer in Intelligence Service (`metadata_consumer.py`) to listen for new metrics and upsert into `MetricMetadata` table.
- **Story 14 (Semantic Routing)**: Refactored Intelligence Service (`main.py`) to usage dynamic `MetricMetadata` for routing user queries instead of hardcoded rules.
- **Testing**: Added `test_intelligence.sh` to verify NLP and metadata flow.

### Phase 3: Intelligence Service (Started Jan 2026)
- **Story 1 (Infrastructure)**: Scaffolded `utility-explorer-intelligence` (Python/FastAPI) and added to Docker mesh (Port 8092).
- **Story 2 (Gateway)**: COMPLETED. Java API proxies requests to Python Service. Fallback logic confirmed. Integration Tests updated.
- **Story 3 (Shared Data Access)**: COMPLETED. Dependencies (`SQLAlchemy`, `psycopg2`) installed. Database connection verified via `/health` endpoint. Renamed service to "Utility Intelligence Service".
- **Story 4 (Structured Intent)**: *Next*. Define Pydantic models for identifying intent (e.g., `GetFacts`, `Compare`, `Explain`).

### Phase 2: Ingestion Logic Split (Jan 2026)
- **Ingestion Service**: Decoupled ingestion into `utility-explorer-ingestion` service.
- **Event Driven**: Implemented Kafka for async event processing between Adapters and Persistence.
- **Adapters**: Split EIA and ACS logic into dedicated modules (`utility-explorer-adapter-eia`, `utility-explorer-adapter-acs`).
- **Documentation**: Created Developer Guide for adding new sources (`docs/dev_rules/HOW_TO_ADD_NEW_SOURCE.md`).

### Phase 1: Flexibility Refactor (Jan 2026)
- **Architecture Split**: Separated code into `utility-explorer-api` and `utility-explorer-shared`.
- **Database Enhancements**: Added `category` and `visualization_json` to `metric` table (Migration V17).
- **Agent Persistence**: Created `user_query` table (Migration V18).

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
4) Maven Multi-Module POMs (Root, Shared, API)

## Decisions made
- **Extensibility**: Metadata-Driven Metric model (DB drives UI/Ingestion).
- Geo levels v1: STATE, COUNTY, PLACE (Place = "city")
- Util Agent: read-only, must return grounded results, can answer cross-layer queries
- Repo structure: Monorepo with utility-explorer-api and utility-explorer-ui
- DB choice: Postgres

## Open decisions (must ask before proceeding)
- Map boundary format: GeoJSON vs TopoJSON (default: TopoJSON)
- Cron parsing dependency for nextRunAt (return null initially)

## Next planned step
- Scaffold `utility-explorer-intelligence` (Python) service.
- Define the API contract between Java API and Python Intelligence service.

## Backlog / Future POCs
- Implement Authentication & Authorization (explore local IDP like Keycloak or lightweight JWT/OAuth2 mesh).
- Implement "Broadband" example using the new Metadata-driven approach (Verify system extensibility).

## Validation commands (Phase 1 COMPLETE)
- `mvn clean install`
- `mvn spring-boot:run -f utility-explorer-api/pom.xml`
- `docker compose up -d`
- `curl http://localhost:8099/actuator/health` (Port 8099 or 8080)