# Utility Explorer Platform

**Utility Explorer** is a distributed, intelligent platform for visualizing and analyzing U.S. utility data (Electricity Prices, Consumption, etc.). It combines interactive geospatial visualizations with an AI-powered "Intelligence Agent" capable of answering natural language queries and forecasting trends.

## 🚀 System Architecture

The platform has evolved from a monolithic application into a resilient **Microservices Architecture**:

```mermaid
graph TD
    User[User / Browser] -->|HTTP| UI[Frontend UI (Vue.js)]
    UI -->|HTTP| API[API Service (Java/Spring)]
    
    subgraph "Core Services"
        API -->|HTTP| Intel[Intelligence Service (Python/FastAPI)]
        API -.->|Kafka Events| Ingestion[Ingestion Service (Java/Spring)]
    end
    
    subgraph "Data Layer"
        Ingestion -->|Writes| DB[(PostgreSQL)]
        API -->|Reads| DB
        Intel -->|Reads| DB
    end
    
    subgraph "Observability (LGTM Stack)"
        Otel[OTEL Collector] --> Prometheus
        Otel --> Loki
        Otel --> Jaeger
        Prometheus --> Grafana
        Loki --> Grafana
        Jaeger --> Grafana
    end
```

### 🧩 Services

| Service | Technology | Port | Description |
| :--- | :--- | :--- | :--- |
| **`utility-explorer-ui`** | Vue 3, Vite | `5173` (Dev) | Interactive dashboard with Highcharts Maps and Chat Interface. |
| **`utility-explorer-api`** | Java 21, Spring Boot | `8090` | Gateway & Backend-for-Frontend. Handles user requests, proxies AI queries, and serves map data. |
| **`utility-explorer-ingestion`** | Java 21, Spring Boot | `8081` | Dedicated worker for fetching data from EIA & Census APIs. Triggered via Scheduler or Kafka events. |
| **`utility-explorer-intelligence`** | Python 3.11, FastAPI | `8092` | The "Brain". Handles NLP (Spacy), Semantic Search (Vector Embeddings), and Forecasting (Scikit-Learn). Consumes metadata from Kafka to learn about new data automatically. |
| **`utility-explorer-shared`** | Java Library | N/A | Common DTOs, Persistence Entities, and Utility classes shared between Java services. |

---

## 🎯 Scope: What It Is / Isn’t
- **Is:** A distributed web platform to browse metrics by geography, drill into states/counties, export CSV, consult an AI agent, and monitor system health via observability dashboards.
- **Isn’t:** A simple data warehouse or generic charting library. It is a specialized, intelligent analytics suite for public utility datasets.

### ✨ Key Features
- **Automated Metadata Discovery**: Data adapters "announce" their capabilities via Kafka. The AI Agent automatically consumes these announcements, enabling "Zero-Shot" understanding of new data sources without code changes.
- **Natural Language Querying**: Ask questions like "Where is electricity cheapest?" and get grounded responses with data provenance.
- **Geospatial Visualization**: Interactive chlorophyll maps layered with Census and Energy data.


---

## ✨ Key Features

### 1. 🌎 Geospatial Visualization & Analysis
- **Interactive Maps:** Drill down from State level to County level.
- **Time Series:** Visualize trends over time (last 6 years).
- **Transparency:** See exactly when data was last fetched and from where.
- **Export:** Download map and timeseries data as CSV.

### 2. 🤖 Utility Intelligence Agent
- **Natural Language Processing:** Ask *"What is the electricity rate in Texas?"* and get an instant, data-backed answer.
- **Intent Recognition:** Sophisticated ML pipeline distinguishes between Fact Retrieval, Comparisons, and Forecasting requests.
- **Guardrails:** Rejects out-of-scope queries (e.g., "How is the weather?") using Semantic Vector Search.
- **Forecasting:** Linear Regression models predict future costs based on historical trends.

### 3. 📡 Automated & Resilient Ingestion
- **Event-Driven:** Uses Kafka to decouple scheduling from execution.
- **Incremental Fetching:** Logic checks existing data to only fetch new gaps from upstream APIs.
- **Extensible Plugin Architecture:** Easily add new data sources (e.g., Water, Gas, Nuclear) by implementing a standard Java interface.
- **Sources:** Supports **EIA** (Energy Information Administration) and **Census ACS** out of the box.

### 4. 🔭 Full Observability (LGTM Stack)
- **Logs:** Centralized logging via **Loki**.
- **Metrics:** System and JVM metrics via **Prometheus**.
- **Traces:** Distributed tracing connects requests across UI, API, Intelligence, and Ingestion using **Jaeger**.
- **Dashboard:** Unified operational view in **Grafana**.

---

## 🛠️ Getting Started

### Prerequisites
- **Docker & Docker Compose** (Recommended for full stack)
- **Java 21+** (For local backend dev)
- **Python 3.11+** (For local intelligence dev)
- **Node.js 18+** (For local frontend dev)
- **API Keys:** You will need keys for EIA and Census APIs (free to obtain).

### ⚡ Quick Start (Docker)

1.  **Configure Environment**
    ```bash
    cp .env.template .env
    # Edit .env and add your EIA_API_KEY and CENSUS_API_KEY
    ```

2.  **Start the Stack**
    The `start.sh` script handles building and leveraging Docker Compose.
    ```bash
    ./start.sh
    ```
    *This will bring up Postgres, Kafka, Zookeeper, the Observability Stack, and all Application Services.*

3.  **Access the Application**
    - **UI Dashboard:** [http://localhost:5173](http://localhost:5173)
    - **API Swagger:** [http://localhost:8090/swagger-ui.html](http://localhost:8090/swagger-ui.html)
    - **Grafana:** [http://localhost:3000](http://localhost:3000) (User: `admin` / Pass: `admin`)

---

## 🔍 Deep Dive: Architecture & Design

### Data Sources & Schedules
- **U.S. Energy Information Administration (EIA):** Electricity retail price.
- **U.S. Census Bureau ACS:** Monthly electricity/utility cost (5-year distribution).
- **Schedules:** Cron-like strings configured in `application.yml` (e.g., `0 0 9 * * MON`).

### Ingestion Flow
1. **Scheduler:** The `ingestion` service wakes up based on cron config.
2. **Fetch:** Adapters query upstream APIs (EIA/Census) using configured keys.
3. **Transform:** Raw JSON is normalized into `FactValue` entities.
4. **Load:** Data is upserted into the PostgreSQL database.
5. **Observe:** Metrics (rows ingested, duration) are pushed to Prometheus; Logs with trace IDs go to Loki.

## Data Model (Conceptual)
| Table | Description |
| :--- | :--- |
| `metrics` | Catalog of available data points (e.g., Retail Price) and units. |
| `sources` | Trusted providers (EIA, Census) with schedules and run status. |
| `fact_values` | The core dataset. Unified table for both Map (point-in-time) and Timeseries data. |
| `ingestion_runs` | Audit log of every data fetch operation, including status and row counts. |

## API Surface (Quick Reference)
The API Gateway (`8090`) routes requests to internal services.
- `GET /metrics` — Catalog of available metrics.
- `GET /sources` — List of data providers and their health.
- `GET /map` — Geospatial data for Highcharts. Params: `metricId`, `period`, `geoLevel` (STATE/COUNTY).
- `GET /timeseries` — Historical trends. Params: `metricId`, `geoId`, `from`, `to`.
- `POST /util-agent/query` — AI Agent endpoint. Body: `{ "question": "..." }`.
- `POST /ingestion/run/{sourceId}` — Manual trigger for data fetching.

### AI Usage Guidelines
- AI (e.g., Copilot, ChatGPT) can assist with code/tests/docs, but:
  - **Secrets:** Do not output secrets; keep keys in `env`/secret stores.
  - **Patterns:** Preserve established patterns (Vue + Highcharts, Spring controllers/services).
  - **Validation:** Document reasoning in PRs; avoid speculative data/logic changes without validation.

### Monetization Opportunities
If deploying commercially:
- **Premium Metrics:** Put high-value real-time ISO/RTO data behind auth.
- **Enriched Exports:** Offer PDF reports or raw SQL access.
- **API Access:** Metered access to the unified GraphQL/REST API.
- **Priority Ingestion:** Offer SLAs for specific data freshness.

---

## 🔒 Security & Compliance
- **Secrets:** Store in `.env` or secret managers (Vault/AWS Secrets Manager). Never commit keys.
- **Network:** Limit CORS to trusted UI origins.
- **Database:** Least-privilege users; TLS encryption in transit.
- **Auth:** (Future) Integrate Keycloak/OIDC for user management.

## ☁️ Cloud Deployment Pattern
- **Containers:** API (Java), Intelligence (Python), and Ingestion (Java) run on ECS/Kubernetes.
- **Frontend:** Static UI hosted on S3/CloudFront or Vercel.
- **Database:** Managed PostgreSQL (RDS/CloudSQL).
- **Scale:** API scales horizontally on CPU; Ingestion is a singleton worker or partitioned by Kafka partitions.

## 🔄 CI/CD & Operations
- **Pipeline:** `lint` → `test` → `build` → `docker push` → `deploy staging` → `smoke test`.
- **Glossary:**
  - **Metric:** A measurable value (e.g., electricity retail price).
  - **Source:** Upstream data provider (e.g., EIA, ACS), tagged live/mock.
  - **GeoLevel:** Spatial granularity (`STATE`, `COUNTY`).
  - **Period:** YYYY or YYYY-MM window for map/timeseries.

---

## 🧪 Testing & Verification

### Run Integration Tests
```bash
./test_all.sh
```
This runs a suite of scripts located in `scripts/tests/` that verify infrastructure health, Java backend logic, and Intelligence service accuracy.

### Manual Verification
You can manually test the NLP module using `curl` against the intelligence service:
```bash
curl -X POST "http://localhost:8092/query" \
     -H "Content-Type: application/json" \
     -d '{"question": "forecast electricity price for Texas"}'
```

---

## 📂 Documentation Index

- [Architecture Deep Dive](ARCHITECTURE.md) - Conceptual design and data flow.
- [Observability Guide](DOCS_OBSERVABILITY.md) - How to use the LGTM stack.
- [Implementation Rules](AI_IMPLEMENTATION_RULES.md) - Standards for adding AI features.
- [Adding New Sources](docs/dev_rules/HOW_TO_ADD_NEW_SOURCE.md) - Guide for writing new Data Adapters.

---

### Troubleshooting
- **Maps blank:** Verify `VITE_API_BASE_URL`, CORS, and that `./start.sh` ran successfully.
- **No data:** Check `ingestion` service logs in Docker or Grafana. Trigger "Run Now" in the UI Transparency tab.
- **Drilldown fails:** Confirm FIPS mapping exists for the county.
- **AI errors:** Check `intelligence` service logs; ensure model was trained during build (`python ml/train_model.py`).

### License
Proprietary / Private - All rights reserved.

## 📄 License

This software is **Proprietary** and confidential. Unauthorized copying, distribution, or use is strictly prohibited.

See the [LICENSE](LICENSE) file for the full legal text.
Copyright © 2026 GoodTime Micro®. All rights reserved.
