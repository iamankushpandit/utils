#!/bin/bash

# Utility Explorer - Quick Start
# One-command setup and launch

echo "🚀 Utility Explorer - Quick Start"
echo "================================="

# Check if setup.sh exists
if [ ! -f setup.sh ]; then
    echo "❌ setup.sh not found. Please run this from the project root directory."
    exit 1
fi

# Run the full setup
./setup.sh

# Keep the script running to show logs
echo
echo "📊 Application is running. Press Ctrl+C to stop and view logs..."
echo

# Function to show logs when interrupted
show_logs() {
    echo
    echo "📋 Recent logs:"
    echo "==============="
    
    if [ -f frontend.log ]; then
        echo "Frontend logs (last 10 lines):"
        tail -10 frontend.log
        echo
    fi
    
    echo "Backend logs (last 10 lines):"
    docker compose logs --tail=10 api
    
    echo
    echo "🛑 To stop the application completely, run: ./stop.sh"
}

# Set up trap for Ctrl+C
trap show_logs INT

# Wait indefinitely (until Ctrl+C)
while true; do
    sleep 1
done