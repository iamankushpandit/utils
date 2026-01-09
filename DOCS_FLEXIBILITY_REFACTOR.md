# Docs: Flexibility & Extensibility Architecture

**Status**: Draft
**Goal**: Transition Utility Explorer from an "Electricity App" to a "Generic Utility Platform".
**Core Philosophy**: Configuration-as-Code driven extensibility. Adding a new Utility vertical (e.g., Broadband, Water) should require **Zero Code Changes** in the frontend or agent logic, only configuration and (optionally) specific ingestion adapters.

---

## 1. The Challenge
Currently, the system is "Metric-Hardcoded".
- **Agent**: Recognizes "price" because we wrote `if (text.contains("price"))`.
- **UI**: Renders "Electricity" tab because we hardcoded `<Tab label="Electricity"/>`.
- **Ingestion**: Fetches data because `EiaClient` specifically looks for price series IDs.

**Problem**: Adding "Broadband" (which has Speed, Coverage, Cost insights) requires touching:
1.  Java Entities (`Metric` needs to group them).
2.  UI Code (New components for Broadband).
3.  Agent Code (New keyword dictionaries).
4.  Ingestion Code (Hardwired fetchers).

---

## 2. The Solution: Metadata-Driven Architecture

We treat the database `Metric` table as the "Brain" of the system. If it's in the DB, the system knows how to ingest, display, and talk about it.

### A. Enhanced Data Model (`utility-explorer-shared`)

We will expand the `Metric` entity to carry all necessary context.

```java
@Entity
public class Metric {
    @Id
    private String metricId;        // e.g., "BB_DOWNLOAD_SPEED"

    // 1. Organization
    private String category;        // "BROADBAND", "ELECTRICITY", "WATER"
    private String subCategory;     // "RESIDENTIAL", "COMMERCIAL" (Optional)
    private String name;            // "Avg Download Speed"
    private String description;     // "Median download throughput in Mbps via FCC..."

    // 2. Visualization Hints (Frontend is Dumb, DB is Smart)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> visualization;
    /*
      {
        "type": "CHOROPLETH",
        "colorScale": "BLUE_TO_GREEN", // High = Good
        "valueFormat": "NUMBER",       // vs CURRENCY vs PERCENT
        "unitLabel": "Mbps"
      }
    */

    // 3. Ingestion Config (Ingestion is Generic)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> ingestionConfig;
    /*
      {
        "adapter": "FCC_API",
        "parameters": {
           "variable": "speed_down_median",
           "api_endpoint": "/broadband/data"
        }
      }
    */
}
```

### B. Configuration as Code (`metrics.yaml`)
We stop inserting rows via SQL scripts. We define the "Truth" in a YAML file that validatesthe system on startup.

```yaml
version: 1.0
categories:
  - id: "ELECTRICITY"
    name: "Electricity"
    icon: "bolt"
  - id: "BROADBAND"
    name: "Internet Access"
    icon: "wifi"

metrics:
  - id: "ELEC_PRICE_RES"
    category: "ELECTRICITY"
    name: "Residential Price"
    visualization:
      colorScale: "GREEN_TO_RED" # High is Bad
    ingestion:
      adapter: "EIA_API"
      seriesId: "ELEC.PRICE.US-RES.M"

  - id: "BB_SPEED_DOWN"
    category: "BROADBAND"
    name: "Download Speed"
    visualization:
      colorScale: "BLUE_TO_GREEN" # High is Good
```

---

## 3. Impact on Subsystems

### A. Ingestion Service (The Adapter Pattern)
It no longer "Runs the EIA Job". It "Runs the Scheduler".
1.  **Scheduler**: Reads all active Metrics from DB.
2.  **Dispatcher**: Sees `ingestion.adapter = "EIA_API"`.
3.  **Execution**: Instantiates `EiaAdapter` (Generic Class) and passes `seriesId` from config.
*   *Future Extensibility*: To add Water, you implement 1 class (`EpaAdapter`) and write YAML. The scheduling/retries/writing logic remains untouched.

### B. Intelligence Service (Metadata-Aware Agent)
The Python Agent must not have hardcoded keywords.
1.  **Startup**: Python loads all Metrics + Descriptions from Postgres.
2.  **Prompt Construction**:
    *   *System Prompt*: "You have access to the following Utility Categories: {List}. For 'Broadband', you have metrics: {Names}."
    *   *User*: "How is the internet in Texas?"
    *   *LLM Reasoning*: "User said 'internet' partial match to category 'Broadband'. Associated metrics are Speed and Cost. I will query those."
3.  **Result**: Adding a new metric in YAML automatically teaches the Agent about it.

### C. UI (Dynamic Rendering)
The UI stops being "The Electricity App".
1.  **Nav**: Loop through `/api/metrics/categories` to render Sidebar/Tabs.
2.  **Controls**: Inside a category, loop through valid `Metric` objects to fill the "Insight" dropdown.
3.  **Map**: When `metricId` changes, read `metric.visualization.colorScale` to determine if Red is Good or Bad.

---

## 4. Implementation Stages

### Stage 1: The Generic Library (`utility-explorer-shared`)
- Create module.
- Define `Metric` with JSON columns (`visualization`, `ingestionConfig`).
- Define `Category` enum/entity.
- Create `DataSourceProvider` interface for ingestion adapters.

### Stage 2: Database Migration
- Flyway script to alter `metric` table (add `category`, JSON columns).
- Migration script to convert existing `ELECTRICITY_*` rows into this new format (e.g., set `category='ELECTRICITY'`).

### Stage 3: Ingestion Refactor
- Split `utility-explorer-ingestion`.
- Refactor `EiaClient` into `DataSourceAdapter`.
- Implement `YamlConfigLoader` to populate DB on startup.

### Stage 4: Frontend Updates
- Update `MapExplorer.vue` to fetch Categories and render controls dynamically.
- Remove hardcoded "Electricity" labels.

---

## 5. Design Considerations / Risks

1.  **Performance**: Loading standard metadata is fast, but JSON parsing in Java Entities requires simpler jackson setup (`hibernate-types` library).
2.  **Versioning**: If `metrics.yaml` changes (renames a metric), we need a strategy.
    *   *Decision*: Metric IDs are immutable. Only display names/configs change.
3.  **Complex Inestions**: Some sources (like ACS) are complex files, not simple API hits.
    *   *Solution**: The `Adapter` can be as complex as needed. The Config just passes parameters to it.

---

*This document serves as the blueprint for the "Flexibility Refactor" prior to building the Util Agent.*
