@echo off
REM Tampa Volunteers - Stop Development Environment Script (Windows)

echo 🛑 Stopping Tampa Volunteers Development Environment...
echo.

REM Stop Docker Compose services
echo 📦 Stopping PostgreSQL database...
docker-compose down

REM Kill Java processes (backend)
echo 🔧 Stopping backend...
for /f "tokens=5" %%a in ('netstat -aon ^| find ":8080" ^| find "LISTENING"') do (
    echo    Killing process %%a on port 8080
    taskkill /F /PID %%a >nul 2>&1
)

REM Kill Node processes (frontend)
echo ⚛️  Stopping frontend...
for /f "tokens=5" %%a in ('netstat -aon ^| find ":5173" ^| find "LISTENING"') do (
    echo    Killing process %%a on port 5173
    taskkill /F /PID %%a >nul 2>&1
)

echo.
echo ✅ All services stopped!
echo.
pause
