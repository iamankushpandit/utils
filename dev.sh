#!/bin/bash

# Utility Explorer - Development Mode
# For developers who want to run with live reload and detailed logging

set -e

echo "🔧 Utility Explorer - Development Mode"
echo "======================================"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() {
    echo -e "${BLUE}[DEV]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Check prerequisites
if ! command -v docker &> /dev/null || ! command -v node &> /dev/null; then
    echo "❌ Prerequisites missing. Please install Docker and Node.js first."
    exit 1
fi

# Setup environment
if [ ! -f .env ]; then
    cp .env.template .env
    print_success "Created .env from template"
fi

if [ ! -f utility-explorer-ui/.env ]; then
    cp utility-explorer-ui/.env.template utility-explorer-ui/.env
    print_success "Created UI .env from template"
fi

set -a
# shellcheck disable=SC1091
source .env
# shellcheck disable=SC1091
source utility-explorer-ui/.env
set +a

SERVER_PORT=${SERVER_PORT:-8080}
FRONTEND_PORT=${VITE_DEV_PORT:-5173}

# Start backend with live logs
print_status "Starting backend services..."
docker compose down 2>/dev/null || true
docker compose up -d --build

# Wait for backend
print_status "Waiting for backend..."
for i in {1..30}; do
    if curl -s "http://localhost:${SERVER_PORT}/actuator/health" > /dev/null 2>&1; then
        print_success "Backend ready!"
        break
    fi
    sleep 1
done

# Install frontend dependencies
print_status "Installing frontend dependencies..."
cd utility-explorer-ui
npm install
cd ..

print_success "🎉 Development environment ready!"
echo
echo "📊 Access URLs:"
echo "   Frontend: http://localhost:${FRONTEND_PORT}"
echo "   Backend:  http://localhost:${SERVER_PORT}"
echo
echo "🔧 Development Commands:"
echo "   Frontend dev server: cd utility-explorer-ui && npm run dev"
echo "   Backend logs:        docker compose logs -f api"
echo "   Database access:     docker compose exec postgres psql -U utility_explorer -d utility_explorer"
echo "   Stop services:       docker compose down"
echo
echo "📋 Quick Tests:"
echo "   curl http://localhost:${SERVER_PORT}/actuator/health"
echo "   curl http://localhost:${SERVER_PORT}/api/v1/metrics"
echo
echo "🚀 To start frontend development server:"
echo "   cd utility-explorer-ui && npm run dev"
