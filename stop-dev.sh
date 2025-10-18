#!/bin/bash

# Tampa Volunteers - Stop Development Environment Script

echo "🛑 Stopping Tampa Volunteers Development Environment..."
echo ""

# Stop Docker Compose services
echo "📦 Stopping PostgreSQL database..."
docker-compose down

# Kill backend process if running
if [ -f "logs/backend.pid" ]; then
    BACKEND_PID=$(cat logs/backend.pid)
    echo "🔧 Stopping backend (PID: $BACKEND_PID)..."
    kill $BACKEND_PID 2>/dev/null || echo "   Backend already stopped"
    rm -f logs/backend.pid
fi

# Kill frontend process if running
if [ -f "logs/frontend.pid" ]; then
    FRONTEND_PID=$(cat logs/frontend.pid)
    echo "⚛️  Stopping frontend (PID: $FRONTEND_PID)..."
    kill $FRONTEND_PID 2>/dev/null || echo "   Frontend already stopped"
    rm -f logs/frontend.pid
fi

# Alternative: Kill by port (if PID file doesn't exist)
echo "🔍 Checking for processes on ports 8080 and 5173..."
lsof -ti:8080 | xargs kill -9 2>/dev/null || true
lsof -ti:5173 | xargs kill -9 2>/dev/null || true

echo ""
echo "✅ All services stopped!"
echo ""
