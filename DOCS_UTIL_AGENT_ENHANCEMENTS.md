# Util Agent Enhancements & Architecture

This document captures the evolved architecture for the Util Agent feature and the Utility Explorer platform. It outlines the move to a microservices architecture, event-driven design, and the specific hybrid "Intelligence" workflow.

## 1. High-Level Goals
- **Separation of Concerns**: Decouple data ingestion, serving, and intelligence.
- **Util Agent as "Intelligence"**: A dedicated Python service to handle NLP, ML, and conversational implementation.
- **Event-Driven**: Use Kafka to coordinate ingestion completion, model retraining, and potentially async query patterns.
- **Transparency**: Clear provenance, "Model" vs "Fact" distinction, and logging for improvement.
- **Shared Domain**: Maintain a shared Java library for core Entities (DB Schema) to ensure consistency between Ingestion and Serving.

## 2. Architecture: Microservices

We are moving from a monolithic API to a 4-part structure within the monorepo:

### A. `utility-explorer-shared` (Java Library)
- **Role**: Shared Codebase.
- **Content**: JPA Entities (`FactValue`, `Metric`, `Source`), DTOs, Repository Interfaces, Common Utilities.
- **Consumers**: `utility-explorer-api`, `utility-explorer-ingestion`.

### B. `utility-explorer-api` (Java Service)
- **Role**: "System of Record" & UI Backend for Visualization.
- **Tech**: Spring Boot.
- **Responsibilities**:
  - Unauthenticated/Read-only API for Maps, TimeSeries (`/api/v1/map`, `/api/v1/timeseries`).
  - Auth/Admin API for system status.
  - **Gateway for Intelligence**: Validates API keys and proxies requests to `utility-explorer-intelligence`.
    - *Decision*: We proxy data to conceal the internal python service topology, distinct auth handling (API Key vs internal network headers), and to facilitate rate-limiting/auditing in a central place.
  - Listens to Kafka (`ingestion.done`) to invalidate caches.
  - **Does NOT** handle Util Agent NLP/ML anymore.

### C. `utility-explorer-ingestion` (Java Service)
- **Role**: Data Pipeline.
- **Tech**: Spring Boot / Java SE.
- **Responsibilities**:
  - Scheduled Jobs (Cron) to fetch from External Sources (EIA, Census).
  - Parsing & Normalization.
  - Writing to Postgres DB (using Shared Entities).
  - Publishing Events to Kafka (`ingestion.done`) upon successful cycle.

### D. `utility-explorer-intelligence` (Python Service)
- **Role**: "System of Intelligence" (Util Agent).
- **Tech**: Python (FastAPI/Flask), Scikit-Learn/PyTorch/Transformers.
- **Responsibilities**:
  - **Endpoint**: `/query` (Directly called by UI).
  - **NLP**: Intent classification (Metric/Geo/Time identification).
  - **Inference**: Uses trained models to answer natural language queries.
  - **Training**: Listens to Kafka (`ingestion.done`) -> Triggers retraining/re-indexing of vector store from Postgres.
  - **Validation**: Runs drift tests against known API values.

### E. `utility-explorer-ui` (Frontend)
- **Role**: User Interface.
- **Tech**: Vue 3 + Vite.
- **Behavior**:
  - Maps/Charts -> Calls `utility-explorer-api`.
  - Util Agent Panel -> Calls `utility-explorer-intelligence` directly.

## 3. Communication & Data Flow

### Event Bus (Kafka)
| Topic | Producer | Consumer | Payload Example |
|-------|----------|----------|-----------------|
| `ingestion.completed` | Ingestion Svc | Intelligence Svc, API Svc | `{ sourceId: "EIA", status: "SUCCESS", timestamp: "..." }` |
| `utilagent.feedback` | Intelligence Svc | Analytics (Future) | `{ queryId: 123, feedback: "positive" }` |

### Util Agent Query Flow
1. **User** types question in UI.
2. **UI** calls `POST http://intelligence-svc/api/v1/query`.
3. **Python Service**:
   - Parses Intent.
   - Runs Inference (or Semantic Search).
   - *Fallback/Validation*: May query DB read-replica if confidence is low.
   - Logs query (if enabled).
   - Returns structured JSON `UtilAgentResponse`.
4. **UI** renders response with:
   - "Model Generated" badge.
   - "Verify on Map" disclaimer.

## 4. Implementation Plan

### Phase 1: Foundation (Java Refactor)
- [ ] Create `utility-explorer-shared` module.
- [ ] Refactor `utility-explorer-api`: move entities to shared.
- [ ] Create `utility-explorer-ingestion`: move ingestion logic from API to new service.
- [ ] Verify `docker-compose` runs both Java services + DB.

### Phase 2: Infrastructure & Events
- [ ] Add **Kafka** & **Zookeeper** to `docker-compose`.
- [ ] Implement Kafka Producer in Ingestion Service.
- [ ] Implement Kafka Consumer stub in API Service (log on receive).

### Phase 3: Intelligence Service (Python)
- [ ] Create `utility-explorer-intelligence` folder.
- [ ] Setup Python env (FastAPI, sqlalchemy, kafka-python, scikit-learn).
- [ ] Implement `/query` endpoint (Mock first, then logic).
- [ ] Implement Kafka Consumer -> Trigger "Train" (Pull data from DB -> Train -> Save Model).

### Phase 4: Integration
- [ ] Update UI `api.js` to point to Python service for Util Agent calls.
- [ ] Add UI badges/disclaimers.
- [ ] Implement Feedback loop.

## 3. Implementation Plan (Stories)

To achieve the architecture above, the work is broken down into 5 testable implementation stories using the "Strangler Fig" pattern to clear the path for the new service.

### Story 1: Scaffold & Infrastructure (The "Hello World")
*   **Goal**: Get a Python service running in the mesh.
*   **Tasks**:
    *   Create `utility-explorer-intelligence/` with `main.py` (FastAPI).
    *   Create `Dockerfile` and `requirements.txt`.
    *   Update `docker-compose.yml` to include the new service on port `8092`.
    *   Add a local `run_intelligence.sh` script for dev convenience.
*   **Success Criteria**: `curl localhost:8092/health` returns `{"status": "ok"}`.

### Story 2: The Gateway Link
*   **Goal**: Route traffic from the public API to the private Intelligence service.
*   **Tasks**:
    *   Refactor `utility-explorer-api`'s `UtilAgentController`.
    *   Replace the internal mock logic with a `RestTemplate` or `WebClient` call to `http://intelligence:8092/query`.
    *   Handle timeouts/errors gracefully (fallback to "Agent offline").
*   **Success Criteria**: Sending a POST to the Java API (`:8090`) returns a response generated by the Python API (`:8092`).

### Story 3: Shared Data Access
*   **Goal**: Give the Brain access to Memory (Database).
*   **Tasks**:
    *   Set up `SQLAlchemy` in the Python service.
    *   Define Python models reflecting the `Metric` and `FactValue` tables (or use introspection).
    *   Implement a "Stats" tool that can query "Max value for Metric X".
*   **Success Criteria**: The agent can correctly answer "How many data points do we have for EIA?" by querying the DB.

### Story 4: Structured Intent & Protocol
*   **Goal**: Formalize the conversation.
*   **Tasks**:
    *   Define Shared Request/Response schemas (JSON).
    *   Implement a basic "Router" in Python that detects if a user wants raw data vs. a summary.
*   **Success Criteria**: The Python service returns correctly formatted JSON that the Vue UI renders as a rich table/chart without client-side changes.

### Story 5: RAG Foundation (Vector Search)
*   **Goal**: "Smart" search.
*   **Tasks**:
    *   Add `langchain` or `llama-index` scaffolding (lightweight start).
    *   Implement a simple ingestion listener (Kafka) to index metric descriptions.
*   **Success Criteria**: Agent can answer "What metrics do you have about prices?" by semantically searching metric descriptions.

## 5. Transparency & Safety
- **Logging**: All queries logged to improve model. UI disclaimer: "Questions are logged to improve accuracy. Opt-out by not using the feature."
- **Badging**: Responses must indicate origin.
- **Drift Detection**: Python service should have a test suite that compares its answers against the deterministic Java API (via internal calls) to flag hallucinations.

## 6. Resources & Constraints
- **Hosting**: Local Docker Compose initially.
- **Compute**: Single Machine (Mac M1 Max). Model selection must be lightweight (Scikit-learn or distilled Transformers).
- **Data**: All historical data in DB is fair game for training.

---
*Maintained by Engineering. Last Updated: 2026-01-08*
