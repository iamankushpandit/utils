# How to Contribute a New Source and Insight

This guide explains how to add a new data source (e.g., Water Quality, Broadband usage) and corresponding insights to the Utility Explorer platform. The system is designed to be extensible through a plugin-based architecture using **Adapters**.

## System Architecture

The ingestion pipeline follows this flow:
1.  **Ingestion Service**: Dispatches jobs based on a schedule.
2.  **Adapters**: Fetch data from external APIs (EIA, Census, etc.) and convert it to a standard `IngestionEvent`.
3.  **Kafka**: Acts as a buffer between data collection and persistence.
4.  **Persistence**: The `IngestionListener` consumes events and saves them to the PostgreSQL database.
5.  **API/UI**: Reads the authenticated data and displays it on the map.

## Step-by-Step Guide

### 1. Define the Metric

Before writing code, define what you are measuring in `utility-explorer-ingestion/src/main/resources/config/metrics.yaml`.

```yaml
metrics:
  - id: "NEW_DATA_METRIC_ID"
    name: "New Data Metric Name"
    description: "Description of what this metric represents."
    unit: "unit_label"
    category: "NEW_CATEGORY"
    sourceId: "NEW_SOURCE_API"
```

*   **id**: Unique identifier used in the code and database.
*   **sourceId**: Matches the adapter ID you will create.

### 2. Create a New Adapter Module

Create a new Maven module for your adapter (e.g., `utility-explorer-adapter-newsource`).

**`pom.xml` dependencies:**
*   `utility-explorer-shared` (for `IngestionAdapter` interface and DTOs)
*   `spring-kafka` (for publishing events)
*   External client libraries (if needed)

### 3. Implement the Adapter Interface

Implement `com.utilityexplorer.shared.adapter.IngestionAdapter`.

```java
@Component
public class NewSourceAdapter implements IngestionAdapter {

    private final KafkaTemplate<String, IngestionEvent> kafkaTemplate;
    
    // Inject KafkaTemplate
    public NewSourceAdapter(KafkaTemplate<String, IngestionEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public String getSourceId() {
        return "NEW_SOURCE_API"; // Must match config
    }

    @Override
    public void fetchAndPublish(String metricId, Map<String, String> params) {
        // 1. CALL EXTERNAL API
        // 2. TRANSFORM RESPONSE
        // 3. PUBLISH TO KAFKA
        
        IngestionEvent event = new IngestionEvent();
        event.setMetricId(metricId);
        event.setSourceId(getSourceId());
        event.setValue(parsedValue);
        // ... set other fields
        
        kafkaTemplate.send("raw-utility-data", event);
    }
}
```

### 4. Register the Adapter

Ensure your new module is included in the `utility-explorer-ingestion` service.

1.  Add the dependency in `utility-explorer-ingestion/pom.xml`:
    ```xml
    <dependency>
        <groupId>com.utilityexplorer</groupId>
        <artifactId>utility-explorer-adapter-newsource</artifactId>
        <version>${project.parent.version}</version>
    </dependency>
    ```

2.  The `AdapterRegistry` automatically discovers beans implementing `IngestionAdapter` thanks to Spring's component scanning. Ensure your adapter has the `@Component` annotation.

### 5. Configure APIs and Secrets

If your source requires an API Key:
1.  Add it to `.env` (e.g., `NEW_SOURCE_API_KEY=xyz`).
2.  Pass it to the ingestion container in `docker-compose.yml`:
    ```yaml
    ingestion:
      environment:
        NEW_SOURCE_API_KEY: ${NEW_SOURCE_API_KEY}
    ```
3.  Inject it into your adapter class using `@Value("${NEW_SOURCE_API_KEY}")`.

### 6. Verify

1.  Rebuild the project: `mvn clean install`.
2.  Restart the stack: `./setup.sh`.
3.  Trigger ingestion manually via API (or wait for schedule):
    ```bash
    curl -X POST http://localhost:8090/api/v1/ingestion/run/NEW_SOURCE_API
    ```
4.  Check logs: `docker compose logs -f ingestion`.

## Examples

Refer to these existing implementations for guidance:
*   **EIA Adapter**: `utility-explorer-adapter-eia` (Fetches data from Energy Information Administration)
*   **ACS Adapter**: `utility-explorer-adapter-acs` (Fetches data from Census Bureau)
