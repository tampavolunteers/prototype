#!/bin/bash

# Rebuild Backend and Database (Full Restart)
# This script stops, rebuilds backend, and restarts both backend and database containers

echo "Stopping backend and database containers..."
docker-compose stop backend postgres

echo "Rebuilding backend container..."
docker-compose build backend

echo "Starting database first..."
docker-compose up -d postgres

echo "Waiting for database to be ready..."
sleep 10

echo "Starting backend container..."
docker-compose up -d backend

echo "Waiting for backend to start..."
sleep 5

echo "Showing backend logs (Ctrl+C to exit)..."
docker-compose logs -f backend
