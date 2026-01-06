#!/bin/bash

echo "🛑 Stopping Utility Explorer..."

# Stop frontend if running
if [ -f frontend.pid ]; then
    FRONTEND_PID=$(cat frontend.pid)
    if kill -0 $FRONTEND_PID 2>/dev/null; then
        echo "Stopping frontend (PID: $FRONTEND_PID)..."
        kill $FRONTEND_PID
    fi
    rm -f frontend.pid
fi

# Stop Docker services
echo "Stopping Docker services..."
docker compose down

# Clean up log files
rm -f frontend.log

echo "✅ Utility Explorer stopped successfully"
