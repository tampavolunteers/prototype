@echo off
REM Rebuild Backend and Database
REM This script stops, rebuilds, and restarts the backend and database containers

echo Stopping backend and database containers...
docker-compose stop backend postgres

echo Rebuilding backend container...
docker-compose build backend

echo Starting backend and database containers...
docker-compose up -d postgres backend

echo Waiting for services to be healthy...
timeout /t 5 /nobreak >nul

echo Showing backend logs (Ctrl+C to exit)...
docker-compose logs -f backend
