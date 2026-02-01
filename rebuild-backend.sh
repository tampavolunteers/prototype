#!/bin/bash

# Rebuild Backend Only
# This script stops, rebuilds, and restarts only the backend container

echo "Stopping backend container..."
docker-compose stop backend

echo "Rebuilding backend container..."
docker-compose build backend

echo "Starting backend container..."
docker-compose up -d backend

echo "Waiting for backend to start..."
sleep 5

echo "Showing backend logs (Ctrl+C to exit)..."
docker-compose logs -f backend
