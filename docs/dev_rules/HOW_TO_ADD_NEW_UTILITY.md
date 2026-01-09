# How to Add New Utilities and Insights

**Goal**: Extend the platform to support new utility types (e.g., Broadband, Water, Gas) without modifying the Core UI or Agent Logic.

The system is designed to be **Metadata-Driven**. The `metric` table in the database acts as the "Brain" of the application. If a metric is defined there with the correct metadata, the application will automatically:
1.  Ingest data for it (via a plugin).
2.  Display it in the Catalog.
3.  Visualize it on the Map (Choropleth/Points).
4.  Allow the AI Agent to answer questions about it.

---

## Step 1: Define the Metric (Database)

You must verify the new metric exists in the `metric` table. This is usually done via a Flyway migration.

**Required Fields:**
*   `metric_id`: Unique key (e.g., `BB_DOWNLOAD_SPEED`).
*   `category`: The utility vertical (e.g., `BROADBAND`).
*   `name`: Human-readable name (e.g., "Median Download Speed").
*   `unit`: Display unit (e.g., `Mbps`).
*   `visualization_json`: Instructions for the UI.
*   `ingestion_config_json`: (Optional) Instructions for the generic ingestion engine.

**Example Migration (`Vxx__Add_Broadband_Metric.sql`):**

```sql
INSERT INTO metric (
    metric_id, 
    category, 
    subcategory, 
    name, 
    description, 
    unit, 
    visualization_json,
    ingestion_config_json
) VALUES (
    'BB_DOWNLOAD_SPEED',
    'BROADBAND',
    'RESIDENTIAL',
    'Avg Download Speed',
    'Median download speed for residential connections via FCC NBM.',
    'Mbps',
    '{
        "type": "CHOROPLETH",
        "colorScale": "RED_TO_GREEN",
        "legendMin": 0,
        "legendMax": 1000,
        "formatting": "NUMBER_0_DECIMALS"
    }',
    '{
        "sourcePlugin": "FccBroadbandPlugin",
        "frequency": "QUARTERLY"
    }'
);
```

---

## Step 2: Implement Ingestion Logic (Java)

If the data source requires custom logic (most do), implement a new `SourcePlugin` in the `utility-explorer-api` module.

1.  Create a class implementing `SourcePlugin` (or extending `AbstractSourcePlugin`).
2.  Annotate with `@Component`.
3.  Implement `fetchData()`:
    *   Connect to external API/CSV.
    *   Map incoming data to `Measurement` objects.
    *   **CRITICAL**: Ensure `metricId` matches the DB entry (`BB_DOWNLOAD_SPEED`).
    *   **CRITICAL**: Ensure `geoId` matches a known Region (e.g., State FIPS code).

```java
@Component
public class FccBroadbandPlugin implements SourcePlugin {
    @Override
    public String  (){ return "FCC_NBM"; }
    
    @Override
    public void runIngestion(IngestionContext ctx) {
        // 1. Fetch JSON/CSV
        // 2. Parse
        // 3. Save Measurements
        measurementRepository.save(new Measurement(
            "BB_DOWNLOAD_SPEED", 
            "06", // California
            RegionLevel.STATE, 
            Instant.now(), 
            500.0, // Value
            sourceProvenance
        ));
    }
}
```

---

## Step 3: Verify & Deploy

Once the Migration and Plugin are ready:

1.  **Run Migrations**: Start the app to apply the SQL.
2.  **Trigger Ingestion**: Call the status endpoint or wait for the scheduler.
    *   `POST /api/ingestion/trigger/FCC_NBM`
3.  **Check UI**:
    *   The "Broadband" category should appear in the filter.
    *   Selecting "Avg Download Speed" should render the map using the `RED_TO_GREEN` scale defined in JSON.
    *   Clicking a region should show the time series.
4.  **Check Agent**:
    *   Ask: "What is the broadband speed in California?"
    *   The agent finds the `Metric` by name/description match and executes the `UserQuery`.

---

## Change Management
*   **Always** create a new Flyway migration file (e.g., `V19__...`) for new metrics.
*   **Never** modify existing migrations.
*   **Test** locally with `mvn clean install` before pushing.
