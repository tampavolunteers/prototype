#!/bin/bash

# Password Reset Utility Script
# This script runs the password reset utility for Tampa Volunteers

echo "Starting Password Reset Utility..."
echo ""

# Check if Docker is available (preferred for development)
if command -v docker &> /dev/null && docker ps &> /dev/null; then
    echo "Running via Docker container..."
    docker-compose run --rm -it backend java -jar app.jar --reset-password=true
elif command -v mvn &> /dev/null; then
    echo "Docker not available. Running with Maven..."
    cd tampavolunteers-backend
    mvn spring-boot:run -Dspring-boot.run.arguments="--reset-password=true"
else
    echo "Error: Neither Docker nor Maven is available."
    echo "Please install Docker or Maven to use this utility."
    exit 1
fi
