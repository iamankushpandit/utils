# Architecture Overview

## System Architecture
- **Frontend**: Vue.js (map-first UI)
- **Backend**: Java Spring Boot (REST API + ingestion orchestration)
- **Intelligence**: Python (Hybrid AI Service)
  - **Rule Engine**: Regex/Spacy for fast, deterministic queries.
  - **GenAI**: Local LLM (`qwen2.5-coder:1.5b`) via Ollama for complex Text-to-SQL.
  - **Forecasting**: Scikit-learn for trend prediction.
- **Database**: Postgres (facts + provenance + geo metadata)
- **Deployment**: Local-first (Docker Compose), cloud-ready

## Hybrid Intelligence Flow
1. **User Query** -> Intelligence Service (`identify_intent`)
2. **Path A (Fast)**: Regex checks for known patterns (e.g., "price in [State]").
   - If match: Execute SQL immediately.
3. **Path B (Smart)**: If no Regex match, fallback to `LLMClient`.
   - LLM generates SQL based on Schema.
   - Execute SQL and explain result.
4. **Forecasting**: If "predict/future" detected, trigger `execute_forecast`.

## Key Patterns
- Source-driven ingestion cadence
- Provenance tracking for all data
- Read-only Util Agent with QuerySpec validation
- 12-factor configuration
- Idempotent operations

## Database Design
Core tables: metric, source, source_config, source_run, fact_value, region, raw_payload

## API Design
- Versioned endpoints: `/api/v1/...`
- Provenance in all responses
- OpenAPI/contract-first approach