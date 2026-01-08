#!/bin/bash

# Utility Explorer - Complete Setup Script
# This script sets up and launches the entire Utility Explorer project

set -e  # Exit on any error

echo "🚀 Utility Explorer - Complete Setup & Launch"
echo "=============================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

load_env() {
    if [ -f .env ]; then
        set -a
        # shellcheck disable=SC1091
        source .env
        set +a
    fi

    if [ -f utility-explorer-ui/.env ]; then
        set -a
        # shellcheck disable=SC1091
        source utility-explorer-ui/.env
        set +a
    fi

    SERVER_PORT=${SERVER_PORT:-8080}
    FRONTEND_PORT=${VITE_DEV_PORT:-5173}
}

# Check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    # Check Node.js
    if ! command -v node &> /dev/null; then
        print_error "Node.js is not installed. Please install Node.js (v16+) first."
        exit 1
    fi
    
    # Check npm
    if ! command -v npm &> /dev/null; then
        print_error "npm is not installed. Please install npm first."
        exit 1
    fi
    
    print_success "All prerequisites are installed"
}

# Setup environment
setup_environment() {
    print_status "Setting up environment..."
    
    # Copy environment file if it doesn't exist
    if [ ! -f .env ]; then
        if [ -f .env.template ]; then
            cp .env.template .env
            print_success "Created .env from template"
        else
            print_error ".env.template not found"
            exit 1
        fi
    else
        print_warning ".env already exists, skipping copy"
    fi
    
    # Setup UI environment
    if [ ! -f utility-explorer-ui/.env ]; then
        if [ -f utility-explorer-ui/.env.template ]; then
            cp utility-explorer-ui/.env.template utility-explorer-ui/.env
            print_success "Created UI .env from template"
        fi
    fi
}

# Build and start backend services
start_backend() {
    print_status "Starting backend services (Docker Compose)..."
    
    # Stop any existing containers
    docker compose down 2>/dev/null || true
    
    # Build and start services
    docker compose up -d --build
    
    print_status "Waiting for services to be ready..."
    
    # Wait for API to be ready (max 60 seconds)
    for i in {1..60}; do
        if curl -s "http://localhost:${SERVER_PORT}/api/v1/status/sources" > /dev/null 2>&1; then
            print_success "Backend services are ready!"
            break
        fi
        
        if [ $i -eq 60 ]; then
            print_error "Backend services failed to start within 60 seconds"
            print_status "Checking logs..."
            docker compose logs api
            exit 1
        fi
        
        echo -n "."
        sleep 1
    done
    echo
}

# Setup and start frontend
start_frontend() {
    print_status "Setting up frontend..."
    
    cd utility-explorer-ui
    
    # Install dependencies if node_modules doesn't exist
    if [ ! -d node_modules ]; then
        print_status "Installing npm dependencies..."
        npm install
        print_success "Dependencies installed"
    else
        print_warning "node_modules exists, skipping npm install"
    fi
    
    print_status "Starting frontend development server..."
    
    # Start in background
    npm run dev > ../frontend.log 2>&1 &
    FRONTEND_PID=$!
    
    # Save PID for cleanup
    echo $FRONTEND_PID > ../frontend.pid
    
    cd ..
    
    # Wait for frontend to be ready
    print_status "Waiting for frontend to be ready..."
    for i in {1..30}; do
        if curl -s "http://localhost:${FRONTEND_PORT}" > /dev/null 2>&1; then
            print_success "Frontend is ready!"
            break
        fi
        
        if [ $i -eq 30 ]; then
            print_error "Frontend failed to start within 30 seconds"
            if [ -f frontend.log ]; then
                print_status "Frontend logs:"
                tail -20 frontend.log
            fi
            exit 1
        fi
        
        echo -n "."
        sleep 1
    done
    echo
}

# Verify system health
verify_system() {
    print_status "Verifying system health..."
    
    # Test API health
    HEALTH_RESPONSE=$(curl -s "http://localhost:${SERVER_PORT}/actuator/health")
    if echo "$HEALTH_RESPONSE" | grep -q '"status":"UP"'; then
        print_success "✓ API health check passed"
    else
        print_warning "Actuator health check not available, probing status endpoint..."
        STATUS_RESPONSE=$(curl -s "http://localhost:${SERVER_PORT}/api/v1/status/sources")
        if [ -n "$STATUS_RESPONSE" ]; then
            print_success "✓ API status endpoint responding"
        else
            print_error "✗ API health check failed"
            echo "Response: $HEALTH_RESPONSE"
            exit 1
        fi
    fi
    
    # Test metrics endpoint
    METRICS_RESPONSE=$(curl -s "http://localhost:${SERVER_PORT}/api/v1/metrics")
    if echo "$METRICS_RESPONSE" | grep -q "ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH"; then
        print_success "✓ Metrics endpoint working"
    else
        print_error "✗ Metrics endpoint failed"
        exit 1
    fi
    
    # Test sources endpoint
    SOURCES_RESPONSE=$(curl -s "http://localhost:${SERVER_PORT}/api/v1/sources")
    if echo "$SOURCES_RESPONSE" | grep -q "EIA"; then
        print_success "✓ Sources endpoint working"
    else
        print_error "✗ Sources endpoint failed"
        exit 1
    fi
    
    # Test map endpoint
    MAP_RESPONSE=$(curl -s "http://localhost:${SERVER_PORT}/api/v1/map?metricId=ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH&sourceId=EIA&geoLevel=STATE&period=2025-12")
    if echo "$MAP_RESPONSE" | grep -q "retrievedAt"; then
        print_success "✓ Map endpoint working"
    else
        print_error "✗ Map endpoint failed"
        exit 1
    fi
    
    # Test frontend
    if curl -s "http://localhost:${FRONTEND_PORT}" | grep -q "Utility Explorer"; then
        print_success "✓ Frontend is serving content"
    else
        print_error "✗ Frontend is not serving content properly"
        exit 1
    fi
}

# Display access information
show_access_info() {
    echo
    echo "🎉 Utility Explorer is now running!"
    echo "=================================="
    echo
    echo "📊 Frontend (Main Application):"
    echo "   http://localhost:${FRONTEND_PORT}"
    echo
    echo "🔧 Backend API:"
    echo "   http://localhost:${SERVER_PORT}"
    echo "   Health: http://localhost:${SERVER_PORT}/actuator/health"
    echo
    echo "📋 Quick API Tests:"
    echo "   Metrics: curl http://localhost:${SERVER_PORT}/api/v1/metrics"
    echo "   Sources: curl http://localhost:${SERVER_PORT}/api/v1/sources"
    echo "   Status:  curl http://localhost:${SERVER_PORT}/api/v1/status/sources"
    echo
    echo "🤖 Util Agent API (requires API key):"
    echo "   curl -X POST http://localhost:${SERVER_PORT}/api/v1/util-agent/query \\"
    echo "     -H 'Content-Type: application/json' \\"
    echo "     -H 'X-API-Key: dev_key_change_me' \\"
    echo "     -d '{\"question\": \"high electricity and low broadband\"}'"
    echo
    echo "📖 Demo Walkthrough:"
    echo "   Follow the steps in DEMO.md for a complete tour"
    echo
    echo "🛑 To stop the application:"
    echo "   ./stop.sh (or Ctrl+C, then docker compose down)"
    echo
}

# Create stop script
create_stop_script() {
    cat > stop.sh << 'EOF'
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
EOF

    chmod +x stop.sh
}

# Cleanup function for script interruption
cleanup() {
    print_warning "Setup interrupted. Cleaning up..."
    
    # Stop frontend if we started it
    if [ -f frontend.pid ]; then
        FRONTEND_PID=$(cat frontend.pid)
        if kill -0 $FRONTEND_PID 2>/dev/null; then
            kill $FRONTEND_PID
        fi
        rm -f frontend.pid
    fi
    
    # Stop Docker services
    docker compose down 2>/dev/null || true
    
    exit 1
}

# Set up cleanup trap
trap cleanup INT TERM

# Main execution
main() {
    echo "Starting setup process..."
    echo
    
    load_env
    check_prerequisites
    setup_environment
    start_backend
    start_frontend
    verify_system
    create_stop_script
    show_access_info
    
    print_success "Setup completed successfully! 🎉"
    echo
    print_status "The application will continue running in the background."
    print_status "Use './stop.sh' to stop all services when you're done."
}

# Run main function
main
