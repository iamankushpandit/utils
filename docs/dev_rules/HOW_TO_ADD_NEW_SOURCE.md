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
    public List<MetricDefinition> getMetricDefinitions() {
        // Announce what this adapter provides (for AI discovery)
        MetricDefinition def = new MetricDefinition();
        def.setMetricId("NEW_DATA_METRIC_ID"); // matches config
        def.setDescription("Description for the AI to understand this metric.");
        def.setUnit("unit_label");
        // ... optional metadata
        return List.of(def);
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

### 3.1 Metadata Broadcasting (AI Discovery)

New adapters **must** implement `getMetricDefinitions()`. This method is called on startup by the `MetadataBroadcaster`. The definitions are sent to a Kafka topic (`system.metadata.metrics`) which the Python Intelligence Service consumes. This allows the AI agent to "learn" about the new data source automatically without code changes in the Python service.

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

### 6. Configure Database & Schedule (Important)

You must register your source in the database to control its execution schedule and display transparency metadata (e.g. "Daily at 6 AM").
Create a Flyway migration script in `utility-explorer-api/src/main/resources/db/migration/` (e.g., `V26__Enable_My_Source.sql`):

```sql
INSERT INTO source_config (source_id, enabled, schedule_cron, timezone, check_strategy, max_lookback_periods, updated_at)
VALUES (
    'NEW_SOURCE_API', 
    true, 
    '0 0 6 * * *', -- Daily at 6am UTC (Enables the "Daily" badge in UI)
    'UTC', 
    'INGEST_ALWAYS', 
    1,
    NOW()
)
ON CONFLICT (source_id) DO UPDATE SET
    schedule_cron = EXCLUDED.schedule_cron;
```

### 7. Geography & Data Standards

*   **GeoID Format**: The system strictly uses **FIPS Codes** for state and county identification.
    *   **Do NOT** use Postal Codes (e.g., "AL").
    *   **DO** use FIPS Codes (e.g., "01" for Alabama).
    *   If your external source provides postal codes, map them to FIPS in your adapter (see `WeatherAdapter.java` for a `STATE_FIPS` map example).
*   **Values**: Ensure numeric values are valid and units match the definition.

### 8. Verify

1.  Rebuild the project: `mvn clean install`.
2.  Restart the stack: `./setup.sh`.
3.  Trigger ingestion manually via API (or wait for schedule):
    ```bash
    curl -X POST http://localhost:8090/api/v1/ingestion/run/NEW_SOURCE_API
    ```
4.  Check logs: `docker compose logs -f ingestion`.

## Common Pitfalls & Solutions

### 1. Data Not Showing on Map
*   **Symptom**: Ingestion logs show 200 OK, but map is empty.
*   **Cause**: GeoID Mismatch. The Source is likely sending Postal Codes (e.g., "TX") instead of FIPS Codes (e.g., "48").
*   **Solution**:
    1. Check the `fact_value` table: `SELECT * FROM fact_value WHERE source_id = 'YOUR_SOURCE';`
    2. If `geo_id` contains letters, you must add a mapping step in your Adapter.
    3. Run a cleanup migration to delete the bad data.

### 2. "Metric Not Found" or Foreign Key Errors
*   **Symptom**: Ingestion service logs error or database constraint violation.
*   **Cause**: Typo in `metric_id` between `metrics.yaml`, Java Adapter, or Database.
*   **Solution**: Ensure the string `MY_METRIC_ID` is identical in:
    *   `metrics.yaml`
    *   `MyAdapter.java` (in `getMetricDefinitions`)
    *   `Vxx__Seed_Metrics.sql`

### 3. Intelligence Service Crashing
*   **Symptom**: `utility-explorer-intelligence` container exits during testing.
*   **Cause**: Running heavy verification scripts (like `verify_nlp.py` with 50+ items) can OOM the container locally.
*   **Solution**:
    *   `docker compose restart intelligence` to recover.
    *   Run tests in smaller batches.

### 4. Schedule Badge Missing in UI
*   **Symptom**: UI shows "Unknown Schedule" for your source.
*   **Cause**: Missing entry in `source_config` table.
*   **Solution**: Ensure you added the migration script (Step 6) and that valid CRON syntax is used.

## Examples

Refer to these existing implementations for guidance:
*   **EIA Adapter**: `utility-explorer-adapter-eia` (Fetches data from Energy Information Administration)
*   **ACS Adapter**: `utility-explorer-adapter-acs` (Fetches data from Census Bureau)
