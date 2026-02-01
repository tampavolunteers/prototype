@echo off
REM Password Reset Utility Script for Windows
REM This script runs the password reset utility for Tampa Volunteers

echo Starting Password Reset Utility...
echo.

REM Check if Docker is available (preferred for development)
where docker >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    docker ps >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
        echo Running via Docker container...
        docker-compose run --rm -it backend java -jar app.jar --reset-password=true
        goto :end
    )
)

REM Fall back to Maven if Docker is not available
where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Docker not available. Running with Maven...
    cd tampavolunteers-backend
    mvn spring-boot:run -Dspring-boot.run.arguments="--reset-password=true"
) else (
    echo Error: Neither Docker nor Maven is available.
    echo Please install Docker or Maven to use this utility.
    pause
    exit /b 1
)

:end
pause
