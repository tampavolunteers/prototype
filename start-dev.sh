#!/bin/bash

# Tampa Volunteers - Development Environment Startup Script
# This script starts all services via Docker Compose

set -e

# Get the script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo "🚀 Starting Tampa Volunteers Development Environment..."
echo ""

# Check if Docker is available
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null 2>&1; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Create logs directory if it doesn't exist
mkdir -p logs

echo "📦 Building and starting all services..."
echo ""

# Start all services with Docker Compose
docker compose up --build -d

echo ""
echo "⏳ Waiting for services to be ready..."
echo ""

# Wait for backend to be healthy (retry for up to 60 seconds)
MAX_RETRIES=30
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -f http://localhost:8080/api/actuator/health &> /dev/null; then
        echo "✅ Backend is ready!"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "   Waiting for backend... ($RETRY_COUNT/$MAX_RETRIES)"
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "⚠️  Backend health check timed out. Check logs with: docker compose logs backend"
fi

echo ""
echo "✨ Development environment is ready!"
echo ""
echo "📊 Services:"
echo "   - PostgreSQL:  localhost:5432"
echo "   - Backend API: http://localhost:8080/api"
echo "   - Frontend:    http://localhost:5173"
echo ""
echo "📝 View logs:"
echo "   - All services:    docker compose logs -f"
echo "   - Backend only:    docker compose logs -f backend"
echo "   - Frontend only:   docker compose logs -f frontend"
echo "   - Database only:   docker compose logs -f postgres"
echo ""
echo "🛑 To stop all services, run: ./stop-dev.sh"
echo ""
