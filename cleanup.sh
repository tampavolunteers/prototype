#!/bin/bash

# Tampa Volunteers - Cleanup Script
# This script stops services that were started outside of Docker Compose
# (e.g., manually started npm or mvn processes)

echo "🧹 Cleaning up services started outside Docker Compose..."
echo ""

# Kill Spring Boot / Maven processes
echo "🔧 Stopping Spring Boot / Maven processes..."
pkill -f "spring-boot:run" && echo "   ✅ Stopped Spring Boot" || echo "   ℹ️  No Spring Boot processes found"
pkill -f "mvn" && echo "   ✅ Stopped Maven" || echo "   ℹ️  No Maven processes found"

# Kill Node / npm processes (be careful with this)
echo ""
echo "⚛️  Stopping Vite / npm dev server..."
pkill -f "vite" && echo "   ✅ Stopped Vite" || echo "   ℹ️  No Vite processes found"

# Kill processes on specific ports
echo ""
echo "📡 Checking for services on ports 8080 and 5173..."
if lsof -ti:8080 > /dev/null 2>&1; then
    lsof -ti:8080 | xargs kill -9 2>/dev/null && echo "   ✅ Killed process on port 8080"
else
    echo "   ℹ️  No process found on port 8080"
fi

if lsof -ti:5173 > /dev/null 2>&1; then
    lsof -ti:5173 | xargs kill -9 2>/dev/null && echo "   ✅ Killed process on port 5173"
else
    echo "   ℹ️  No process found on port 5173"
fi

echo ""
echo "✅ Cleanup complete!"
echo ""
echo "💡 To start services with Docker Compose, run: ./start-dev.sh"
echo ""
