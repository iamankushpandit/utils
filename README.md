# Utility Explorer

Utility Explorer is a full-stack app that visualizes U.S. utility data (initially electricity prices and ACS-based monthly costs) on interactive maps with drilldown, transparency into data freshness, and ingestion scheduling. It is not a general-purpose BI tool; it is focused on public/free utility datasets with configurable sources and periods.

## Scope: What It Is / Isn’t
- **Is:** A web UI + API to browse metrics by geography, drill into states/counties, export CSV, monitor source status, and trigger ingestion.
- **Isn’t:** A data warehouse, an auth system, or a generic charting platform. It assumes public/free datasets and does not manage paid/licensed data.

## Architecture Overview
The system is intentionally simple: a Vue 3/Vite frontend talks to a Spring Boot API that serves map/timeseries data from a relational database populated by scheduled ingestions. Highcharts Maps handles drilldown (state → county) with legend ranges coming from the API (and UI fallback when missing). Everything runs locally via Vite + Spring Boot and can be containerized for cloud deployment using the provided API Dockerfile and a trivial UI static image.

- **Frontend (utility-explorer-ui):** Vue 3 + Vite, Highcharts Maps for drilldown, Axios for API calls, environment-driven API base URL.  
- **Backend (utility-explorer-api):** Java (Spring Boot) with REST endpoints for catalog, map, timeseries, status, and ingestion control.  
- **Ingestion:** Spring-based dispatcher on cron-like schedules; writes to the DB (relational). Sources tagged live/mock; ACS and EIA are primary.  
- **Data Flow:** Ingestion → DB → API → UI maps/time series. Legend and value ranges are supplied by API, with client fallback.  
- **Deploy Targets:** Local dev servers; containerized for cloud (API Dockerfile provided).

### Component Inventory
- **UI:** MapExplorer (metric tabs, source cards, drilldown map), RegionDrawer (details + CSV export), Transparency (status/schedules + run now), Copilot panel (text queries).
- **API:** Controllers for metrics/sources/map/timeseries/status/ingestion; ingestion dispatcher + per-source jobs; DTOs for catalog and map/timeseries payloads.
- **Data:** Relational tables for metrics, sources, map values, timeseries points, and ingestion runs.

### API Surface (quick reference)
The API is small and predictable: catalog, map/timeseries data, ingestion controls, and status. Add SpringDoc/OpenAPI (e.g., `springdoc-openapi-starter-webmvc-ui`) to serve Swagger UI at `/swagger-ui.html` and JSON at `/v3/api-docs`, so clients can explore parameters and payloads without code-diving. Gate Swagger behind auth in production.

- `GET /metrics` — metric catalog.  
- `GET /sources` — source catalog (mock/live flags).  
- `GET /map` — params: `metricId`, `sourceId`, `geoLevel` (STATE|COUNTY), `parentGeoLevel`, `parentGeoId`, `period` (YYYY or YYYY-MM).  
- `GET /timeseries` — params: metricId, sourceId, geoLevel, geoId, from, to.  
- `GET /status/sources` — status, schedule, last/next run.  
- `POST /ingestion/run` and `/ingestion/run/{sourceId}` — trigger ingestion.  
- `GET /export/csv` — export map/timeseries to CSV.

### Environment Variables (minimum)
- **UI:** `VITE_API_BASE_URL` (default `http://localhost:8080/api/v1`).
- **API:** `EIA_API_KEY`, `CENSUS_API_KEY`, DB settings (`DB_URL`, `DB_USER`, `DB_PASS`), `INGESTION_DISPATCHER_ENABLED`, optional `SERVER_PORT`, CORS allowed origins, log level.
- Keep `.env` out of git; use secret managers in cloud.

### Data Model (conceptual)
- `metrics` (metricId, name, unit, description, defaultGranularity)
- `sources` (sourceId, name, mock flag, schedule, lastRun, nextRun, status)
- `map_values` (metricId, sourceId, geoLevel, geoId, periodStart, periodEnd, value, retrievedAt)
- `timeseries_values` (metricId, sourceId, geoLevel, geoId, periodStart, value, retrievedAt)
- `ingestion_runs` (sourceId, status, startedAt, finishedAt, message)

### Ingestion Flow
1) Dispatcher reads enabled sources and schedules.
2) Fetch upstream API (with key), validate, transform to internal metric/source/geo IDs.
3) Upsert map/timeseries rows; record run status.
4) Transparency page polls `/status/sources`; “Run now” posts to ingestion endpoints.

## Data Sources & Schedules
- **U.S. Energy Information Administration (EIA)** — Electricity retail price.
- **U.S. Census Bureau ACS** — Monthly electricity/utility cost (5-year distribution).
- Other sources (FCC, EPA) are currently out/disabled per prior requests.
- **Schedules:** Cron-like strings (e.g., `0 0 9 * * MON`); displayed in Transparency page. “Run now” triggers ingestion via API.

## Setup
1) **Prereqs:** Node 18+, npm; Java 17+; Docker optional for DB/API.  
2) **Env:** Copy `.env.template` to `.env` in both UI and API roots. Key vars:  
   - UI: `VITE_API_BASE_URL` (default `http://localhost:8080/api/v1`)  
   - API: source keys (e.g., `EIA_API_KEY`, `CENSUS_API_KEY`), DB URL/user/pass, ingestion flags.  
3) **API Keys:**  
   - EIA: get a free key at https://www.eia.gov/opendata/register.php → set `EIA_API_KEY`.  
   - Census: request at https://api.census.gov/data/key_signup.html → set `CENSUS_API_KEY`.  
4) **Install:**  
   - UI: `npm install` in `utility-explorer-ui`.  
   - API: build with Maven/Gradle per project config.  
5) **Run locally:**  
   - API: `./mvnw spring-boot:run` (or Gradle equivalent).  
   - UI: `npm run dev -- --host` in `utility-explorer-ui` (uses `VITE_API_BASE_URL`).  
6) **Build:** `npm run build` (UI) and `./mvnw package` (API).

## Cloud Deployment (example pattern)
- **Containerize:** Use API Dockerfile; create a simple Dockerfile for UI (build → nginx serve).  
- **Infra:**  
  - API: container on ECS/Kubernetes/Cloud Run; expose 8080; attach RDS/CloudSQL.  
  - UI: static hosting + CDN (S3+CloudFront, GCS+Cloud CDN, or Vercel/Netlify).  
  - Secrets: use Secret Manager/SSM/Key Vault for API keys; never bake into images.  
  - Networking: enable CORS for UI origin; optional WAF; HTTPS via ALB/Ingress/Cloud Load Balancer.  
- **DB:** Managed Postgres/MySQL with backups and automated minor upgrades.  
- **Scaling:** API horizontal autoscaling on CPU/latency; DB with read replicas if needed; UI is static.  
- **Observability:** Ship logs to CloudWatch/Stackdriver/ELK; metrics via Prometheus/OpenTelemetry; alerts on ingestion failures and API 5xx.

## CI/CD Considerations
Aim for fast feedback and low-risk releases: lint/test, build artifacts, run automated checks, deploy to staging, smoke test, and promote. Keep secrets in vault/runner; gate DB migrations on backups; prefer blue/green or canary for API and immutable static deploy for UI. Document rollbacks and keep them one-click.

- **Pipeline:** lint/test → build UI/API → package containers → deploy to staging → smoke tests → promote to prod.  
- **Checks:** unit/integration tests, lint/format, dependency scanning (Snyk/OWASP), container vulnerability scan.  
- **Secrets:** injected at deploy time (vault/runner secrets).  
- **Rollout:** blue/green or canary for API; immutable static deploy for UI; automated DB migrations (Flyway/Liquibase) gated on backup.  
- **Stages:** `ci-ui` (npm ci, lint, unit, build), `ci-api` (mvn/gradle verify, integration with testcontainers), `image-build` (API + UI static), `deploy-staging` (smoke: health + sample map), `promote` (manual/canary), `rollback` plan.

## Testing
- **UI:** Component/unit tests (Vue Test Utils), smoke E2E (Cypress/Playwright) against a seeded API.  
- **API:** Unit + integration tests (REST controllers, ingestion services); contract tests for `/map`/`/timeseries`.  
- **Data:** Ingestion pipeline tests with sample payloads; backfill scripts tested in staging before prod runs.

## Operating / Transparency
- **Transparency page:** shows source status, schedules, last/next run, “Run now” per source.  
- **Logs/Alerts:** Alert on failed ingestion runs, API 5xx spikes, DB errors.  
- **Exports:** CSV export via UI triggers API `/export/csv`.

## AI Usage Guidelines
- AI can assist with code/tests/docs, but:  
  - Do not output secrets; keep keys in env/secret stores.  
  - Preserve established patterns (Vue + Highcharts, Spring controllers/services).  
  - Document reasoning in PRs; avoid speculative data/logic changes without validation.  
  - Keep generated content concise and reviewable.

## Design Considerations (with Pros/Cons)
- **12-Factor Alignment:**  
  - Pros: Clear separation of config, stateless processes, easy cloud deploys.  
  - Cons: Background ingest jobs still need scheduling/state (not purely stateless).  
- **Highcharts Maps:**  
  - Pros: Built-in drilldown, topojson library, good labeling.  
  - Cons: Size and licensing considerations; custom drilldown is harder to control.  
- **API-driven Legend/Map Data:**  
  - Pros: UI stays thin; server controls ranges/periods.  
  - Cons: UI needs fallbacks if legend missing; tight coupling to API contracts.  
- **Ingestion Scheduling (cron):**  
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
