#!/bin/bash

# Tampa Volunteers - Development Environment Startup Script
# This script starts the entire development environment

set -e

echo "🚀 Starting Tampa Volunteers Development Environment..."
echo ""

# Check if Docker is available
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Start PostgreSQL with Docker Compose
echo "📦 Starting PostgreSQL database..."
docker-compose up -d

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for database to be ready..."
sleep 5

until docker-compose exec -T postgres pg_isready -U postgres &> /dev/null; do
    echo "   Database is still starting..."
    sleep 2
done

echo "✅ Database is ready!"
echo ""

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "⚠️  Java is not installed. Backend will not start automatically."
    echo "   Please install Java 17+ and run: cd tampavolunteers-backend && ./mvnw spring-boot:run"
else
    echo "🔧 Starting Spring Boot backend..."
    echo "   Backend will be available at http://localhost:8080/api"
    cd tampavolunteers-backend
    ./mvnw spring-boot:run > ../logs/backend.log 2>&1 &
    BACKEND_PID=$!
    echo "   Backend PID: $BACKEND_PID"
    cd ..
fi

# Check if Node.js is available
if ! command -v node &> /dev/null; then
    echo "⚠️  Node.js is not installed. Frontend will not start automatically."
    echo "   Please install Node.js 18+ and run: cd tampavolunteers-frontend && npm run dev"
else
    echo "⚛️  Starting Vite frontend..."
    echo "   Frontend will be available at http://localhost:5173"
    cd tampavolunteers-frontend

    # Install dependencies if node_modules doesn't exist
    if [ ! -d "node_modules" ]; then
        echo "   Installing dependencies..."
        npm install
    fi

    npm run dev > ../logs/frontend.log 2>&1 &
    FRONTEND_PID=$!
    echo "   Frontend PID: $FRONTEND_PID"
    cd ..
fi

echo ""
echo "✨ Development environment is starting up!"
echo ""
echo "📊 Services:"
echo "   - PostgreSQL: localhost:5432"
echo "   - Backend API: http://localhost:8080/api"
echo "   - Frontend: http://localhost:5173"
echo ""
echo "📝 Logs are being written to:"
echo "   - Backend: logs/backend.log"
echo "   - Frontend: logs/frontend.log"
echo ""
echo "🛑 To stop all services, run: ./stop-dev.sh"
echo ""
