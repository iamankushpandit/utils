#!/bin/bash
# Test Script for UI (Vue.js)

echo "🧪 TEST: Frontend (Vue/Vitest)"
echo "=============================="

cd utility-explorer-ui

# Ensure dependencies are installed
if [ ! -d "node_modules" ]; then
    echo "Installing node modules..."
    npm install
fi

# Run Vitest in CI mode (run once and exit)
echo "Running npm run test..."
npm run test -- --run

if [ $? -eq 0 ]; then
    echo "✅ Frontend Tests Passed"
    exit 0
else
    echo "❌ Frontend Tests Failed"
    exit 1
fi
