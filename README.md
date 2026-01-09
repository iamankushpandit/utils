# Utility Explorer Platform# Utility Explorer



**Utility Explorer** is a distributed, intelligent platform for visualizing and analyzing U.S. utility data (Electricity Prices, Consumption, etc.). It combines interactive geospatial visualizations with an AI-powered "Intelligence Agent" capable of answering natural language queries and forecasting trends.Utility Explorer is a full-stack app that visualizes U.S. utility data (initially electricity prices and ACS-based monthly costs) on interactive maps with drilldown, transparency into data freshness, and ingestion scheduling. It is not a general-purpose BI tool; it is focused on public/free utility datasets with configurable sources and periods.



## 🚀 System Architecture## Scope: What It Is / Isn’t

- **Is:** A web UI + API to browse metrics by geography, drill into states/counties, export CSV, monitor source status, and trigger ingestion.

The platform has evolved from a monolithic application into a resilient **Microservices Architecture**:- **Isn’t:** A data warehouse, an auth system, or a generic charting platform. It assumes public/free datasets and does not manage paid/licensed data.



```mermaid## Architecture Overview

graph TDThe system is intentionally simple: a Vue 3/Vite frontend talks to a Spring Boot API that serves map/timeseries data from a relational database populated by scheduled ingestions. Highcharts Maps handles drilldown (state → county) with legend ranges coming from the API (and UI fallback when missing). Everything runs locally via Vite + Spring Boot and can be containerized for cloud deployment using the provided API Dockerfile and a trivial UI static image.

    User[User / Browser] -->|HTTP| UI[Frontend UI (Vue.js)]

    UI -->|HTTP| API[API Service (Java/Spring)]- **Frontend (utility-explorer-ui):** Vue 3 + Vite, Highcharts Maps for drilldown, Axios for API calls, environment-driven API base URL.  

    - **Backend (utility-explorer-api):** Java (Spring Boot) with REST endpoints for catalog, map, timeseries, status, and ingestion control.  

    subgraph "Core Services"- **Ingestion:** Spring-based dispatcher on cron-like schedules; writes to the DB (relational). Sources tagged live/mock; ACS and EIA are primary.  

        API -->|HTTP| Intel[Intelligence Service (Python/FastAPI)]- **Data Flow:** Ingestion → DB → API → UI maps/time series. Legend and value ranges are supplied by API, with client fallback.  

        API -.->|Kafka Events| Ingestion[Ingestion Service (Java/Spring)]- **Deploy Targets:** Local dev servers; containerized for cloud (API Dockerfile provided).

    end

    ### Documentation

    subgraph "Data Layer"*   [How to Add a New Source](docs/dev_rules/HOW_TO_ADD_NEW_SOURCE.md): Guide for contributors to add new data adapters.

        Ingestion -->|Writes| DB[(PostgreSQL)]*   [Architecture](ARCHITECTURE.md): High-level system design.

        API -->|Reads| DB*   [Demo](DEMO.md): Walkthrough of features.

        Intel -->|Reads| DB

    end### Component Inventory

    - **UI:** MapExplorer (metric tabs, source cards, drilldown map), RegionDrawer (details + CSV export), Transparency (status/schedules + run now), Util Agent panel (text queries).

    subgraph "Observability (LGTM Stack)"- **API:** Controllers for metrics/sources/map/timeseries/status/ingestion; ingestion dispatcher + per-source jobs; DTOs for catalog and map/timeseries payloads.

        Otel[OTEL Collector] --> Prometheus- **Data:** Relational tables for metrics, sources, map values, timeseries points, and ingestion runs.

        Otel --> Loki

        Otel --> Jaeger### API Surface (quick reference)

        Prometheus --> GrafanaThe API is small and predictable: catalog, map/timeseries data, ingestion controls, and status. Add SpringDoc/OpenAPI (e.g., `springdoc-openapi-starter-webmvc-ui`) to serve Swagger UI at `/swagger-ui.html` and JSON at `/v3/api-docs`, so clients can explore parameters and payloads without code-diving. Gate Swagger behind auth in production.

        Loki --> Grafana

        Jaeger --> Grafana- `GET /metrics` — metric catalog.  

    end- `GET /sources` — source catalog (mock/live flags).  

```- `GET /map` — params: `metricId`, `sourceId`, `geoLevel` (STATE|COUNTY), `parentGeoLevel`, `parentGeoId`, `period` (YYYY or YYYY-MM).

- `GET /map/range` — same params plus `startPeriod`/`endPeriod` (YYYY or YYYY-MM); returns multiple map snapshots for each period in the inclusive range.

### 🧩 Services- `GET /timeseries` — params: metricId, sourceId, geoLevel, geoId, from, to.  

- `GET /status/sources` — status, schedule, last/next run.  

| Service | Technology | Port | Description |- `POST /ingestion/run` and `/ingestion/run/{sourceId}` — trigger ingestion.  

| :--- | :--- | :--- | :--- |- `GET /export/csv` — export map/timeseries to CSV.

| **`utility-explorer-ui`** | Vue 3, Vite | `5173` (Dev) | Interactive dashboard with Highcharts Maps and Chat Interface. |- `POST /util-agent/query` — natural-language question (JSON body `{ "question": "<text>" }`) with `X-API-Key`; returns a Util Agent response with tables, highlights, and sources.

| **`utility-explorer-api`** | Java 21, Spring Boot | `8090` | Gateway & Backend-for-Frontend. Handles user requests, proxies AI queries, and serves map data. |

| **`utility-explorer-ingestion`** | Java 21, Spring Boot | `8081` | Dedicated worker for fetching data from EIA & Census APIs. Triggered via Scheduler or Kafka events. |### API Docs (Swagger/OpenAPI)

| **`utility-explorer-intelligence`** | Python 3.11, FastAPI | `8092` | The "Brain". Handles NLP (Spacy), Semantic Search (Vector Embeddings), and Forecasting (Scikit-Learn). |Swagger UI is available when the API runs with the SpringDoc starter: open `/swagger-ui.html` (or `/swagger-ui/index.html`) to browse and try endpoints; machine-readable JSON lives at `/v3/api-docs`. In production, restrict this to authenticated/internal users. Include examples for common flows (state map, county drilldown, timeseries export) and clarify required headers (e.g., `X-API-Key` for Util Agent).

| **`utility-explorer-shared`** | Java Library | N/A | Common DTOs, Persistence Entities, and Utility classes shared between Java services. |

### Environment Variables (minimum)

---- **UI:** `VITE_API_BASE_URL` (default `http://localhost:8080/api/v1`).

- **API:** `EIA_API_KEY`, `CENSUS_API_KEY`, DB settings (`DB_URL`, `DB_USER`, `DB_PASS`), `INGESTION_DISPATCHER_ENABLED`, optional `SERVER_PORT`, CORS allowed origins, log level, `UTIL_AGENT_ENABLED`, `UTIL_AGENT_API_KEY`.  

## ✨ Key Features- **History windows:** `EIA_MONTHS_BACK` (default 72 months), `CENSUS_ACS_MIN_YEAR`/`CENSUS_ACS_MAX_YEAR` (defaults now-5 .. now) to pull 6 years of ACS.

- Keep `.env` out of git; use secret managers in cloud.

### 1. 🌎 Geospatial Visualization

- **Interactive Maps:** Drill down from State level to County level.### Data Model (conceptual)

- **Time Series:** Visualize trends over time (last 6 years).- `metrics` (metricId, name, unit, description, defaultGranularity)

- **Transparency:** See exactly when data was last fetched and from where.- `sources` (sourceId, name, mock flag, schedule, lastRun, nextRun, status)

- `map_values` (metricId, sourceId, geoLevel, geoId, periodStart, periodEnd, value, retrievedAt)

### 2. 🤖 Utility Intelligence Agent- `timeseries_values` (metricId, sourceId, geoLevel, geoId, periodStart, value, retrievedAt)

- **Natural Language Processing:** Ask *"What is the electricity rate in Texas?"* and get an instant, data-backed answer.- `ingestion_runs` (sourceId, status, startedAt, finishedAt, message)

- **Intent Recognition:** Sophisticated ML pipeline distinguishes between Fact Retrieval, Comparisons, and Forecasting requests.

- **Guardrails:** Rejects out-of-scope queries (e.g., "How is the weather?") using Semantic Vector Search.### Ingestion Flow

- **Forecasting:** Linear Regression models predict future costs based on historical trends.1) Dispatcher reads enabled sources and schedules.

2) Fetch upstream API (with key), validate, transform to internal metric/source/geo IDs.

### 3. 📡 Automated Ingestion3) Upsert map/timeseries rows; record run status.

- **Resilient Fetching:** Incremental logic only fetches new data gaps.4) Transparency page polls `/status/sources`; “Run now” posts to ingestion endpoints.

- **Sources:** Supports **EIA** (Energy Information Administration) and **Census ACS** out of the box.

- **Event Driven:** Uses Kafka for decoupling ingestion triggers from execution.## Data Sources & Schedules

- **U.S. Energy Information Administration (EIA)** — Electricity retail price.

### 4. 🔭 Full Observability (LGTM)- **U.S. Census Bureau ACS** — Monthly electricity/utility cost (5-year distribution).

- **Logs:** Centralized logging via **Loki**.- Other sources (FCC, EPA) are currently out/disabled per prior requests.

- **Metrics:** System and JVM metrics via **Prometheus**.- **Schedules:** Cron-like strings (e.g., `0 0 9 * * MON`); displayed in Transparency page. “Run now” triggers ingestion via API.

- **Traces:** Distributed tracing connects requests across all microservices using **Jaeger**.

- **Dashboard:** Unified view in **Grafana**.## Setup

1) **Prereqs:** Node 18+, npm; Java 17+; Docker optional for DB/API.  

---2) **Env:** Copy `.env.template` to `.env` in both UI and API roots. Key vars:  

   - UI: `VITE_API_BASE_URL` (default `http://localhost:8080/api/v1`)  

## 🛠️ Getting Started   - API: source keys (e.g., `EIA_API_KEY`, `CENSUS_API_KEY`), DB URL/user/pass, ingestion flags.  

3) **API Keys:**  

### Prerequisites   - EIA: get a free key at https://www.eia.gov/opendata/register.php → set `EIA_API_KEY`.  

- **Docker & Docker Compose** (Recommended for full stack)   - Census: request at https://api.census.gov/data/key_signup.html → set `CENSUS_API_KEY`.  

- **Java 21+** (For local backend dev)4) **Install:**  

- **Python 3.11+** (For local intelligence dev)   - UI: `npm install` in `utility-explorer-ui`.  

- **Node.js 18+** (For local frontend dev)   - API: build with Maven/Gradle per project config.  

- **API Keys:** You will need keys for EIA and Census APIs (free to obtain).5) **Run locally:**  

   - API: `./mvnw spring-boot:run` (or Gradle equivalent).  

### ⚡ Quick Start (Docker)   - UI: `npm run dev -- --host` in `utility-explorer-ui` (uses `VITE_API_BASE_URL`).  

6) **Build:** `npm run build` (UI) and `./mvnw package` (API).

1.  **Configure Environment**

    ```bash## Cloud Deployment (example pattern)

    cp .env.template .env- **Containerize:** Use API Dockerfile; create a simple Dockerfile for UI (build → nginx serve).  

    # Edit .env and add your EIA_API_KEY and CENSUS_API_KEY- **Infra:**  

    ```  - API: container on ECS/Kubernetes/Cloud Run; expose 8080; attach RDS/CloudSQL.  

  - UI: static hosting + CDN (S3+CloudFront, GCS+Cloud CDN, or Vercel/Netlify).  

2.  **Start the Stack**  - Secrets: use Secret Manager/SSM/Key Vault for API keys; never bake into images.  

    The `start.sh` script handles building and leveraging Docker Compose.  - Networking: enable CORS for UI origin; optional WAF; HTTPS via ALB/Ingress/Cloud Load Balancer.  

    ```bash- **DB:** Managed Postgres/MySQL with backups and automated minor upgrades.  

    ./start.sh- **Scaling:** API horizontal autoscaling on CPU/latency; DB with read replicas if needed; UI is static.  

    ```- **Observability:** Ship logs to CloudWatch/Stackdriver/ELK; metrics via Prometheus/OpenTelemetry; alerts on ingestion failures and API 5xx.

    *This will bring up Postgres, Kafka, Zookeeper, the Observability Stack, and all Application Services.*

## CI/CD Considerations

3.  **Access the Application**Aim for fast feedback and low-risk releases: lint/test, build artifacts, run automated checks, deploy to staging, smoke test, and promote. Keep secrets in vault/runner; gate DB migrations on backups; prefer blue/green or canary for API and immutable static deploy for UI. Document rollbacks and keep them one-click.

    - **UI Dashboard:** [http://localhost:5173](http://localhost:5173)

    - **API Swagger:** [http://localhost:8090/swagger-ui.html](http://localhost:8090/swagger-ui.html) (or `http://localhost:8080/swagger-ui.html` if running default)- **Pipeline:** lint/test → build UI/API → package containers → deploy to staging → smoke tests → promote to prod.  

    - **Grafana:** [http://localhost:3000](http://localhost:3000) (User: `admin` / Pass: `admin`)- **Checks:** unit/integration tests, lint/format, dependency scanning (Snyk/OWASP), container vulnerability scan.  

- **Secrets:** injected at deploy time (vault/runner secrets).  

---- **Rollout:** blue/green or canary for API; immutable static deploy for UI; automated DB migrations (Flyway/Liquibase) gated on backup.  

- **Stages:** `ci-ui` (npm ci, lint, unit, build), `ci-api` (mvn/gradle verify, integration with testcontainers), `image-build` (API + UI static), `deploy-staging` (smoke: health + sample map), `promote` (manual/canary), `rollback` plan.

## 🧪 Testing & Verification

## Testing

### Run Integration Tests- **UI:** Component/unit tests (Vue Test Utils), smoke E2E (Cypress/Playwright) against a seeded API.  

```bash- **API:** Unit + integration tests (REST controllers, ingestion services); contract tests for `/map`/`/timeseries`.  

./test_all.sh- **Data:** Ingestion pipeline tests with sample payloads; backfill scripts tested in staging before prod runs.

```

This runs a suite of scripts located in `scripts/tests/` that verify infrastructure health, Java backend logic, and Intelligence service accuracy.## Operating / Transparency

- **Transparency page:** shows source status, schedules, last/next run, “Run now” per source.  

### Manual Verification- **Logs/Alerts:** Alert on failed ingestion runs, API 5xx spikes, DB errors.  

You can manually test the NLP module using `curl` against the intelligence service:- **Exports:** CSV export via UI triggers API `/export/csv`.

```bash

curl -X POST "http://localhost:8092/query" \## AI Usage Guidelines

     -H "Content-Type: application/json" \- AI can assist with code/tests/docs, but:  

     -d '{"question": "forecast electricity price for Texas"}'  - Do not output secrets; keep keys in env/secret stores.  

```  - Preserve established patterns (Vue + Highcharts, Spring controllers/services).  

  - Document reasoning in PRs; avoid speculative data/logic changes without validation.  

---  - Keep generated content concise and reviewable.



## 📂 Documentation Index## Design Considerations (with Pros/Cons)

- **12-Factor Alignment:**  

- [Architecture Deep Dive](ARCHITECTURE.md) - Conceptual design and data flow.  - Pros: Clear separation of config, stateless processes, easy cloud deploys.  

- [Observability Guide](DOCS_OBSERVABILITY.md) - How to use the LGTM stack.  - Cons: Background ingest jobs still need scheduling/state (not purely stateless).  

- [Implementation Rules](AI_IMPLEMENTATION_RULES.md) - Standards for adding AI features.- **Highcharts Maps:**  

- [Adding New Sources](docs/dev_rules/HOW_TO_ADD_NEW_SOURCE.md) - Guide for writing new Data Adapters.  - Pros: Built-in drilldown, topojson library, good labeling.  

  - Cons: Size and licensing considerations; custom drilldown is harder to control.  

---- **API-driven Legend/Map Data:**  

  - Pros: UI stays thin; server controls ranges/periods.  

### License  - Cons: UI needs fallbacks if legend missing; tight coupling to API contracts.  

Proprietary / Private - All rights reserved.- **Ingestion Scheduling (cron):**  

  - Pros: Simple, predictable; easy “run now.”  
  - Cons: Not timezone-aware by default; failure retries need explicit handling.  
- **Secrets Management:**  
  - Pros: Keys in env/secret managers; safer rotation.  
  - Cons: More ops setup; local env onboarding requires care.  
- **Scalability:**  
  - Pros: UI is static; API horizontal scale is straightforward.  
  - Cons: DB and ingestion backfills can bottleneck; need batching/backpressure.  
- **Resilience (timeouts/retries/circuit breakers):**  
  - Pros: Protects against flaky upstreams; keeps UI responsive.  
  - Cons: More complexity; tuning required per source.  
- **Observability:**  
  - Pros: Faster incident response; visibility into ingestion failures.  
  - Cons: Requires log/metric pipelines and dashboards.  
- **Data Quality/Backfill:**  
  - Pros: Validations prevent bad data; backfill improves coverage.  
  - Cons: Backfills can be expensive; schema evolution must be coordinated.

## Security & Compliance
- Store secrets in env/secret managers; never commit keys. Prefer signed releases and integrity checks if distributing artifacts.
- Limit CORS to trusted UI origins; enforce HTTPS and consistent TLS settings across services.
- DB: least-privilege user, TLS to DB, backups + tested restores; audit access where required.
- Dependency and container scanning in CI; keep base images patched; document SBOMs if needed.
- Add auth/RBAC if exposed beyond trusted users; gate Swagger/OpenAPI to authenticated/internal users in production.

## Performance, Scaling, Resilience
- UI is static; serve via CDN; tree-shake and lazy-load map assets where possible.
- API: horizontal autoscale; tune DB pool; cache map/timeseries responses with reasonable TTL/ETag.
- Ingestion: per-source timeouts, retries with jitter, circuit breakers; batch DB writes.
- Backpressure: limit concurrent ingestions; paginate upstream fetches; avoid long transactions.
- Preload/cdn-cache topojson to reduce drilldown latency.

## Observability & Operations
- Logs: structured JSON; include source, metric, period, counts, latency. Capture drilldown errors (topojson fetch failures) to catch map regressions.
- Metrics: ingestion duration/success rate, API latency/5xx, DB pool usage.
- Alerts: failed ingestions, empty map for current period, API 5xx spikes, DB saturation.
- Dashboards: source freshness, ingestion throughput, user/API errors; optional tracing (OpenTelemetry) for slow endpoints.

## Accessibility & UX
- Enable Highcharts accessibility module when licensing permits; otherwise document warnings.
- Keyboard navigation for tabs/buttons; ensure color-scale contrast; tooltip fallback text for nulls.

## Known Limitations
- No built-in auth; assumes trusted users.
- Highcharts bundle size; topojson fetched from public CDN.
- County drilldown depends on FIPS/hc-key mapping; missing counties log warnings.
- Only public/free data sources supported; paid/licensed not handled.
- Backfills and large ingestions can stress DB; plan capacity accordingly.

## Glossary
- **Metric:** A measurable value (e.g., electricity retail price).
- **Source:** Upstream data provider (e.g., EIA, ACS), tagged live/mock.
- **GeoLevel:** Spatial granularity (`STATE`, `COUNTY`).
- **Period:** YYYY or YYYY-MM window for map/timeseries.
- **Ingestion:** Process that pulls upstream data and writes to DB.

## Troubleshooting
- Maps blank: verify `VITE_API_BASE_URL`, CORS, `/metrics` and `/sources` responses.
- Drilldown fails: check console network for topojson fetch; ensure metric/source is ACS; confirm FIPS mapping exists.
- “No data” messages: inspect ingestion status and DB contents for the requested period; trigger “Run now.”
- CORS/network errors: align UI origin with API CORS config; use HTTPS in production.

## Improvements / Roadmap
- Add auth/role-based access if needed (currently assumes open use).  
- Improve map UX: cached topojson, reduced bundle size, and accessibility module for Highcharts.  
- Ingestion robustness: retries with jitter, DLQ for failed pulls, per-source timeouts.  
- Schema/versioning: Flyway/Liquibase migrations and automated backups/retention.  
- Caching: server-side cache for map/timeseries responses with ETag/If-None-Match.  
- CI/CD: add canary deploys and automated smoke checks post-deploy.  
- Documentation: expand API docs (OpenAPI), sample curl/HTTPie scripts.
- Monetization experiments: premium metrics/sources behind auth, higher-frequency/expanded geography tiers, enriched exports (PDF/Excel/embeds), API usage tiers with rate limits and billing, white-label embeds for partners, and “priority ingestion” SLA offerings.  

## Monetization Opportunities
If you choose to monetize, keep the base experience free and layer value-adds: gated premium metrics/sources, higher-frequency updates, richer geography (block/group), branded exports (PDF/Excel/embeds), white-label map widgets, and partner APIs with rate limits and billing. Consider “priority ingestion” SLAs for paid tiers. Implement auth/RBAC, usage metering, and billing before exposing paid endpoints.

## Working With the Code
- UI dev: `npm run dev` in `utility-explorer-ui`, point `VITE_API_BASE_URL` at your API.  
- API dev: run Spring Boot locally with DB; use `.env.template` to fill keys; keep `.env` out of git.  
- Tests: run unit/integration before PRs; seed minimal data for E2E map smoke.  
- Contributions: follow existing patterns; avoid committing secrets; keep PRs small and reviewed.

## Documentation & Code Comments
- **Frontend:** key components (`MapExplorer.vue`, `RegionDrawer.vue`, `MapComponent.vue`) include inline comments near fetch logic, drilldown handling, and UI windows. They explain period selection, aggregation rules, and drilldown fallbacks so new devs can tune display windows via `VITE_DISPLAY_YEARS`, `VITE_MAP_SEARCH_YEARS`, etc. Keep comments up to date when changing map behavior or time ranges.
- **Backend:** ingestion plugins (`CensusAcsElectricityCostSourcePlugin`, `EiaRetailPriceSourcePlugin`) include explanatory comments about weighting buckets, API probing, and guardrails. When adding new sources or metrics, document which `FactValue` columns are populated and how `SourceContext` timestamps propagate.

## Swagger & OpenAPI Delivery
Swagger UI is published by Springdoc at `http://localhost:8081/swagger-ui/index.html` (also `/swagger-ui.html`); the JSON spec lives at `/v3/api-docs`. Use this for live contract validation, sample payloads, and ingestion triggers. In production, toggle exposure or protect it behind auth to avoid public discovery.

## Onboarding Checklist (doc-focused)
1. Copy `.env.template` to `.env` in both API and UI directories.  
2. Populate secrets: `EIA_API_KEY`, `CENSUS_API_KEY`, database credentials, `VITE_API_BASE_URL`.  
3. Validate data ranges: EIA returns 72 months by default; ACS defaults to the latest available year minus 6 lookback (configurable via `CENSUS_ACS_YEARS_BACK`). Document any gaps (e.g., ACS 2017-2019 might be unpublished) and adjust `CENSUS_ACS_MIN_YEAR` accordingly.  
4. Start Postgres container (`docker-compose up utils-postgres-1`); ensure `DB_HOST` points to it (`localhost` for local runs).  
5. Start API (`mvn -Dmaven.test.skip=true spring-boot:run`) and UI (`npm run dev -- --host 0.0.0.0 --port 5173`).  
6. Verify Swagger page, `/metrics`, `/sources`, `/status/sources`, and sample map responses before sharing with stakeholders.

## Architecture & Lifecycle Notes (for Prod readiness)
- **Ingress**: Dispatcher reads enabled plugins and triggers them on schedule; they fetch upstream, validate status codes, compute `FactValue` entries, and log metrics.  
- **Persistence**: facts store `retrievedAt`, `sourcePublishedAt`, `isAggregated`, and `aggregationMethod`. Regions have FIPS parents for drilldown.  
- **UI Flow**: metric tabs → per-source cards → `MapComponent` fetches best year/month, renders Highcharts map, and emits region clicks to `RegionDrawer`. Drawer aggregates time series into yearly buckets and exports CSV.  
- **Telemetry**: logs include ingestion events, missing legend warnings, and map fetch fallbacks; add structured logging / metrics in prod to monitor latency/errors.  
- **Swagger**: used for onboarding new consumers—document ingestion control endpoints and map queries for partners wanting embed or API access.
