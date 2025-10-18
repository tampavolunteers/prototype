@echo off
REM Tampa Volunteers - Development Environment Startup Script (Windows)
REM This script starts the entire development environment

echo 🚀 Starting Tampa Volunteers Development Environment...
echo.

REM Check if Docker is available
docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker is not installed. Please install Docker Desktop first.
    exit /b 1
)

docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker Compose is not installed. Please install Docker Compose first.
    exit /b 1
)

REM Start PostgreSQL with Docker Compose
echo 📦 Starting PostgreSQL database...
docker-compose up -d

REM Wait for PostgreSQL to be ready
echo ⏳ Waiting for database to be ready...
timeout /t 5 /nobreak >nul

:waitloop
docker-compose exec -T postgres pg_isready -U postgres >nul 2>&1
if errorlevel 1 (
    echo    Database is still starting...
    timeout /t 2 /nobreak >nul
    goto waitloop
)

echo ✅ Database is ready!
echo.

REM Create logs directory if it doesn't exist
if not exist "logs" mkdir logs

REM Check if Java is available
java -version >nul 2>&1
if errorlevel 1 (
    echo ⚠️  Java is not installed. Backend will not start automatically.
    echo    Please install Java 17+ and run: cd tampavolunteers-backend ^&^& mvnw.cmd spring-boot:run
) else (
    echo 🔧 Starting Spring Boot backend...
    echo    Backend will be available at http://localhost:8080/api
    cd tampavolunteers-backend
    start /b cmd /c "mvnw.cmd spring-boot:run > ..\logs\backend.log 2>&1"
    cd ..
)

REM Check if Node.js is available
node --version >nul 2>&1
if errorlevel 1 (
    echo ⚠️  Node.js is not installed. Frontend will not start automatically.
    echo    Please install Node.js 18+ and run: cd tampavolunteers-frontend ^&^& npm run dev
) else (
    echo ⚛️  Starting Vite frontend...
    echo    Frontend will be available at http://localhost:5173
    cd tampavolunteers-frontend

    REM Install dependencies if node_modules doesn't exist
    if not exist "node_modules" (
        echo    Installing dependencies...
        call npm install
    )

    start /b cmd /c "npm run dev > ..\logs\frontend.log 2>&1"
    cd ..
)

echo.
echo ✨ Development environment is starting up!
echo.
echo 📊 Services:
echo    - PostgreSQL: localhost:5432
echo    - Backend API: http://localhost:8080/api
echo    - Frontend: http://localhost:5173
echo.
echo 📝 Logs are being written to:
echo    - Backend: logs\backend.log
echo    - Frontend: logs\frontend.log
echo.
echo 🛑 To stop all services, run: stop-dev.bat
echo.
echo Press Ctrl+C to exit this window (services will continue running)
pause
