#!/bin/bash
echo "🚀 Starting Intelligence Service (Local Development)"
echo "   Port: 8092"
echo "   Swagger UI: http://localhost:8092/docs"

# Create virtualenv if not exists
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
fi

source venv/bin/activate
pip install -r requirements.txt

# Run
uvicorn main:app --reload --port 8092
