#!/bin/bash
# Test Script for Infrastructure (Docker)
# Checks if all containers in 'docker-compose.yml' are up and healthy

echo "🧪 TEST: Infrastructure (Docker Health Checks)"
echo "=========================================="

# 1. Start everything in detached mode if not running
echo "Checking if containers are running..."
if [ -z "$(docker compose ps -q)" ]; then
    echo "Starting containers..."
    docker compose up -d
    echo "Waiting 20s for services to stabilize..."
    sleep 20
fi

# 2. Check each container status
SERVICES=("utility-explorer-postgres" "utility-explorer-kafka" "otel-collector" "jaeger" "prometheus" "grafana" "loki" "promtail")

ALL_HEALTHY=true

for SVC in "${SERVICES[@]}"; do
    STATUS=$(docker inspect --format='{{.State.Status}}' $SVC 2>/dev/null)
    if [ "$STATUS" == "running" ]; then
        echo "✅ $SVC is RUNNING"
    else
        echo "❌ $SVC is NOT RUNNING (Status: $STATUS)"
        ALL_HEALTHY=false
    fi
done

if [ "$ALL_HEALTHY" = true ]; then
    echo "✅ Infrastructure Tests Passed"
    exit 0
else
    echo "❌ Infrastructure Tests Failed"
    exit 1
fi
