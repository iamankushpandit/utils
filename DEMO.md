# DEMO.md — Utility Explorer (Local)

## Goal
Demonstrate the project locally with provenance, transparency, and "no invented data."

## Prerequisites
- Docker installed
- Node.js installed (for UI)
- Java 17+ installed (for API)

---

## Step 1 — Start backend + database
```bash
docker compose up -d --build
```

Validate:
```bash
curl http://localhost:8080/actuator/health
```

Expected: `{"status":"UP"}`

---

## Step 2 — Start UI
```bash
cd utility-explorer-ui
npm install
npm run dev
```

Open: [http://localhost:5173](http://localhost:5173)

---

## Step 3 — Workflow: Transparency page

* Navigate to `/transparency`
* Confirm it displays:
  * Principles (No Data Invention, No Silent Blending, Provenance Everywhere, Free Public Data Only)
  * Data Status table from `/api/v1/status/sources`
  * Live status showing EIA source with enabled=true, last runs, schedule

---

## Step 4 — Workflow: Map + legend + provenance

* Navigate to `/`
* Select metric: "Electricity Retail Price (cents/kWh)"
* Select source: "U.S. Energy Information Administration"
* Select period: "December 2025"
* Confirm:
  * Map renders state boundaries for Kansas, California, Texas, New York, Florida
  * States with data are colored (green to red scale)
  * Legend shows min/max values, unit, retrieved timestamp
  * Hover shows state name + value + unit
  * Missing coverage shows boundaries but no color fill

---

## Step 5 — Workflow: Drill into a region

* Click Kansas on the map
* Drawer opens showing:
  * Current value: 14.6 cents/kWh
  * Source: U.S. Energy Information Administration
  * Retrieved timestamp
  * Historical chart with 3 data points (Oct, Nov, Dec 2025)
  * Chart shows trend line with hover tooltips

---

## Step 6 — Export CSV

* In the region drawer, click "Export CSV"
* Confirm CSV downloads as "Kansas_ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH.csv"
* Open CSV and verify columns:
  * periodStart, periodEnd, value, retrievedAt, sourcePublishedAt
  * All 3 time periods included with full provenance

---

## Step 7 — Data Status

* Navigate to `/transparency`
* Scroll to "Live Data Status" section
* Confirm it shows:
  * EIA source: Enabled, cron schedule, last success timestamp
  * Additional placeholder sources (FCC_BROADBAND, EPA_WATER) marked as disabled
  * Last run status and timing information

---

## Step 8 — Copilot (Read-only)

* On map page, scroll to "Ask Questions About the Data" section
* Try example query: "high electricity and low broadband"
* Confirm response shows:
  * Status: OK or INSUFFICIENT_DATA
  * Summary explaining available data
  * Table with electricity pricing data
  * Sources used with retrieved timestamps
  * Notes about data limitations

---

## Step 9 — API Endpoints (Optional)

Test key endpoints directly:

```bash
# Metrics catalog
curl http://localhost:8080/api/v1/metrics

# Sources catalog  
curl http://localhost:8080/api/v1/sources

# Map data
curl "http://localhost:8080/api/v1/map?metricId=ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH&sourceId=EIA&geoLevel=STATE&period=2025-12"

# Time series
curl "http://localhost:8080/api/v1/timeseries?metricId=ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH&sourceId=EIA&geoLevel=STATE&geoId=20&from=2025-10-01&to=2025-12-31"

# Source status
curl http://localhost:8080/api/v1/status/sources

# Copilot (requires API key)
curl -X POST http://localhost:8080/api/v1/copilot/query \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev_key_change_me" \
  -d '{"question": "high electricity and low broadband"}'
```

---

## Verification Checklist

✅ **No Data Invention**: Missing regions show "No data available", never estimated values  
✅ **Provenance Everywhere**: Every displayed value includes source + retrieved timestamp  
✅ **Transparency**: Source status, methodology, and limitations clearly documented  
✅ **Interactive Map**: Choropleth coloring, hover tooltips, click-to-drill functionality  
✅ **Time Series**: Historical charts with provenance in tooltips  
✅ **Export**: CSV includes full provenance columns  
✅ **Read-only Copilot**: Structured queries with citations and safety constraints  

---

## Architecture Highlights

* **Local-first**: Runs entirely with Docker Compose
* **12-factor**: All configuration via environment variables
* **Provenance-first**: Every fact includes retrieved_at + source attribution
* **Plugin architecture**: Extensible ingestion framework with mock plugin
* **Safety-first Copilot**: QuerySpec validation prevents unsafe queries
* **No vendor lock-in**: Portable across cloud providers