# Data Sources & Integration Memory Bank

## Source Integration Patterns
- **Plugin Architecture**: Each source implements SourcePlugin interface
- **Check Strategy**: CHECK_AND_INGEST_IF_NEW vs INGEST_ALWAYS
- **Change Detection**: Compare latest period/release ID vs stored data
- **Raw Payload Storage**: Hash + storage reference for audit trail

## Source Configuration Schema
```json
{
  "sourceId": "EIA",
  "enabled": true,
  "scheduleCron": "0 0 9 * * MON",
  "timezone": "UTC",
  "checkStrategy": "CHECK_AND_INGEST_IF_NEW",
  "maxLookbackPeriods": 3
}
```

## Supported Data Types
- **Electricity**: Retail prices, generation, consumption (state/county level)
- **Broadband**: Coverage percentages, speeds (state/county/place level)
- **Water**: Quality metrics, availability (varies by source)
- **Wastewater**: Treatment capacity, compliance (facility-based)

## Geographic Coverage Rules
- **STATE**: All sources should support (2-digit FIPS)
- **COUNTY**: Most sources support (5-digit FIPS)
- **PLACE**: Limited sources (Census Place GEOID)
- **Facility**: Point data aggregated to county/state level

## Temporal Patterns
- **Monthly**: Most utility data (electricity, broadband)
- **Quarterly**: Some regulatory reports
- **Annual**: Census-based datasets
- **Event-driven**: Compliance violations, outages

## Data Quality Checks
- **Range validation**: Reasonable min/max values per metric
- **Geographic validation**: Valid FIPS/GEOID codes
- **Temporal validation**: Reasonable date ranges
- **Unit consistency**: Standardized units per metric type

## Error Handling Patterns
- **Network failures**: Retry with exponential backoff
- **Parse errors**: Log raw payload + error, mark run as FAILED
- **Partial data**: Accept partial updates, log coverage gaps
- **Schema changes**: Version detection + graceful degradation

## Provenance Requirements
- **Source attribution**: Name + terms URL + attribution text
- **Retrieval timestamp**: When we fetched (retrieved_at)
- **Source timestamp**: When source published (source_published_at)
- **Payload hash**: Immutable reference to raw data