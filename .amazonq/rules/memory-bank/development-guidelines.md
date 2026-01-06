# Development Guidelines Memory Bank

## Code Organization
- **Two Repos**: `utility-explorer-ui` (Vue) + `utility-explorer-api` (Spring Boot)
- **Package Structure**: api, service, geo, metrics, sources, ingestion, persistence, security, observability
- **Config**: Environment variables only (12-factor compliance)

## Data Handling Rules
- **Never invent data**: Show "No data available" instead of estimates
- **Transparent aggregation**: Must be labeled + reproducible + documented
- **Idempotent operations**: All writes must be repeatable without side effects
- **Provenance tracking**: Store retrieved_at + optional source_published_at

## API Response Standards
```json
{
  "metric": {"metricId": "...", "unit": "..."},
  "source": {"sourceId": "...", "name": "...", "termsUrl": "..."},
  "retrievedAt": "2026-01-06T14:15:00Z",
  "sourcePublishedAt": "2026-01-02T00:00:00Z",
  "values": [...]
}
```

## Testing Strategy
- **Unit Tests**: Source parsers, QuerySpec validation, provenance handling
- **Integration Tests**: Testcontainers Postgres, ingestion idempotency
- **UI Tests**: Route rendering, map loading, "No data" cases

## Deployment Patterns
- **Local**: Docker Compose (postgres + api + optional ui container)
- **Production**: Static UI + single VPS (Spring Boot + Postgres)
- **Cloud-ready**: Architecture avoids vendor lock-in

## Error Handling
- **Ingestion failures**: Record in source_run table with error_summary
- **Missing data**: Return structured "No data available" responses
- **Copilot safety**: Validate QuerySpec, refuse unsafe/incomplete queries

## Performance Considerations
- **Caching**: Map responses by (metric, source, geoLevel, parent, period)
- **Indexing**: fact_value lookup index, source_run by source+date
- **Boundaries**: Static TopoJSON assets for fast UI rendering

## Observability Requirements
- **Health checks**: /actuator/health, /actuator/readiness
- **Metrics**: Last successful run per source, failure counts, ingestion duration
- **Logging**: Structured JSON to stdout, never log secrets