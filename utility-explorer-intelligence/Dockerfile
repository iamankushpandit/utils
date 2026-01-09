FROM python:3.11-slim

WORKDIR /app

# Install system dependencies if needed (e.g. for building python packages)
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Bake in Spacy Model
RUN python -m spacy download en_core_web_sm

COPY . .

# Bake in Intent Model (Train during build)
RUN python ml/train_model.py

# Run on port 8000 inside container (mapped via docker-compose)
EXPOSE 8000

CMD ["opentelemetry-instrument", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
