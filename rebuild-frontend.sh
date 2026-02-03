#!/bin/bash

# Rebuild Frontend
# This script stops, rebuilds, and restarts the frontend container

echo "Stopping frontend container..."
docker-compose stop frontend

echo "Rebuilding frontend container..."
docker-compose build frontend

echo "Starting frontend container..."
docker-compose up -d frontend

echo "Waiting for service to start..."
sleep 3

echo "Showing frontend logs (Ctrl+C to exit)..."
docker-compose logs -f frontend
