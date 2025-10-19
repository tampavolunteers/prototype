#!/bin/bash

# Tampa Volunteers - Stop Development Environment Script
# This script stops all services via Docker Compose

# Get the script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo "🛑 Stopping Tampa Volunteers Development Environment..."
echo ""

# Stop all Docker Compose services
docker compose down

echo ""
echo "✅ All services stopped!"
echo ""
echo "💡 To start again, run: ./start-dev.sh"
echo "🗑️  To remove all data (volumes), run: docker compose down -v"
echo ""
