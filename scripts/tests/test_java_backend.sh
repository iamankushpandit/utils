#!/bin/bash
# Test Script for Java Backend Services

echo "🧪 TEST: Java Backend (Maven)"
echo "============================="

# Run Maven Test
# This runs unit tests in all modules (api, adapters, shared)
echo "Running mvn test..."
mvn test -Dnet.bytebuddy.experimental=true -Djacoco.skip=true

if [ $? -eq 0 ]; then
    echo "✅ Java Backend Tests Passed"
    exit 0
else
    echo "❌ Java Backend Tests Failed"
    exit 1
fi
