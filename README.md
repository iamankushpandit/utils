# Utility Explorer

A map-first, transparency-first dashboard that visualizes free public utility-related datasets (electricity, broadband, water, wastewater) with provenance tracking and no data invention.

## Core Principles

- **No ads, affiliate links, or sponsored content**
- **No data invention**: No forecasting, smoothing, estimation, or imputation
- **No silent blending**: Multiple sources shown separately
- **Provenance everywhere**: Every value includes source + retrieved timestamp
- **Free public data only**

## Architecture

- **Frontend**: Vue.js (map-first UI)
- **Backend**: Java Spring Boot (REST API + ingestion orchestration)
- **Database**: Postgres (facts + provenance + geo metadata)
- **Deployment**: Local-first (Docker Compose), cloud-ready

## 🚀 Quick Start

**One-command setup and launch:**
```bash
./start.sh
```

**Or step-by-step:**
```bash
# Setup and launch everything
./setup.sh

# Access the application
open http://localhost:5173

# Stop when done
./stop.sh
```

**For developers:**
```bash
# Development mode with live reload
./dev.sh
```

## Manual Setup (Alternative)

1. Copy environment template:
   ```bash
   cp .env.template .env
   ```

2. Start the application:
   ```bash
   docker compose up -d --build
   ```

3. Start frontend:
   ```bash
   cd utility-explorer-ui && npm install && npm run dev
   ```

4. Verify health:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

## Project Structure

- `utility-explorer-api/` - Spring Boot API
- `utility-explorer-ui/` - Vue.js frontend
- `docs/` - Documentation
- `.amazonq/rules/memory-bank/` - AI context and guidelines
- `setup.sh` - Complete setup and launch script
- `start.sh` - Quick start script
- `dev.sh` - Development mode script
- `DEMO.md` - Complete walkthrough guide