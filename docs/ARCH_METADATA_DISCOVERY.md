# Architecture Proposal: Automated Metadata Discovery & Registry

## 1. Problem Statement
**The "Hardcoded Brain" Bottleneck**
Currently, the Python Intelligence Service acts as the "Brain" of the operation, interpreting user queries and mapping them to data. However, its knowledge of *what data exists* is hardcoded in a Python dictionary.

**Current State (`main.py`):**
```python
available_metrics = {
    "ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH": "electricity retail price...",
    "ELECTRICITY_MONTHLY_COST_USD_ACS": "average monthly electricity bill..."
}
```

**Consequence:**
If we add a new data source (e.g., "Wind Speed" via a new Java Adapter), the data will flow into Postgres, but the AI won't know it exists. We have to manually update `main.py` every time we touch the Java ingestion layer. This is brittle and scales poorly.

## 2. Proposed Solution: Self-Registering Architecture
We propose a **Event-Driven Metadata Discovery** system. The Ingestion Service (Java) will be the "Source of Truth" for what data is available, and it will broadcast this knowledge to the Intelligence Service (Python) via Kafka.

### Core Concept
**"Don't tell the Brain what to know. Let the Body tell the Brain what it can feel."**

## 3. Workflow Design

### Phase A: The Handshake (Service Startup)
1.  **Ingestion Service (Java) Starts Up**:
    *   It scans all registered Adapters (EIA, Weather, Census).
    *   It calls a new method: `getMetricDefinitions()`.
    *   It publishes a message to a new Kafka topic: `system.metadata.metrics`.

    *Payload Example:*
    ```json
    {
      "metric_id": "WIND_SPEED_MPH",
      "description": "Average wind velocity measured at turbine hub height",
      "unit_label": "mph",
      "display_name": "Wind Speed",
      "source_system": "NOAA_WEATHER"
    }
    ```

2.  **Intelligence Service (Python) Listens**:
    *   A background `KafkaConsumer` listens to `system.metadata.metrics`.
    *   Upon receiving a definition:
        1.  It generates a **Vector Embedding** for the `description`.
        2.  It stores the definition + embedding in a `metric_metadata` table in Postgres.
        3.  It updates its runtime cache.

### Phase B: The Query (Runtime)
1.  User asks: *"How windy is it in Texas?"*
2.  Python Service embeds request: `[0.12, 0.88, ...]`
3.  Search: Compares query vector against `metric_metadata` table.
4.  Match: Finds `WIND_SPEED_MPH` (Score: 0.95).
5.  Execution: Queries `fact_value` table for that ID.

## 4. Required Changes

### A. Infrastructure (Kafka)
*   Create new topic: `system.metadata.metrics`
    *   **Config:** `cleanup.policy=compact` (Ensures the latest definition for a metric is always retained, so new services can "replay" the state).

### B. Java Service (Ingestion)
1.  **Refactor Adapters:** Update `IngestionAdapter` interface to include `List<MetricDefinition> getMetricDefinitions()`.
2.  **Startup Runner:** Create `MetadataBroadcaster.java` that runs on `@EventListener(ApplicationReadyEvent.class)`.

### C. Python Service (Intelligence)
1.  **Database:** Create `MetricMetadata` SQLAlchemy model.
2.  **Consumer:** Add a background thread/process in `lifespan` to consume Kafka metadata events.
3.  **Refactor:** Remove `available_metrics` dict from `main.py`. Replace with DB lookup.

## 5. Benefits vs Costs

| Benefit | Cost / Risk |
| :--- | :--- |
| **Zero-Touch Scaling:** Add a stock market adapter in Java, and the AI immediately answers crypto questions without Python code changes. | **Complexity:** Adds a Kafka consumer to the Python service (currently HTTP only). |
| **Single Truth:** Documentation lives with the code that generates the data. | **Synchronization:** Slight delay (milliseconds) between Java startup and Python "learning" the new metric. |
| **Rich UX:** We can pass usage units (`mph`, `$`, `MWh`) dynamically to the UI. | |

## 6. Decision
Do we proceed with this decoupling? 
*   **Yes:** If we plan to add >3 more data sources.
*   **No:** If EIA and Census are the only datasets we will ever have.
