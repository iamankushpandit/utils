#!/bin/bash
# Test Script for Python Intelligence Service

echo "🧪 TEST: Intelligence Service (Python)"
echo "======================================"

cd utility-explorer-intelligence

# Ensure venv exists
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
    source venv/bin/activate
    pip install -r requirements.txt
else
    source venv/bin/activate
fi

# Run pytest
# We only run unit tests, not integration tests that require DB
echo "Running pytest..."
# Add current directory to PYTHONPATH so tests can import 'main' and 'embeddings'
export PYTHONPATH=$PYTHONPATH:$(pwd)
pytest -v tests/

if [ $? -eq 0 ]; then
    echo "✅ Intelligence Service Tests Passed"
    exit 0
else
    echo "❌ Intelligence Service Tests Failed"
    exit 1
fi
