@echo off
REM Tampa Volunteers - Cleanup Script (Windows)
REM This script stops services that were started outside of Docker Compose

echo Cleaning up services started outside Docker Compose...
echo.

echo Stopping Spring Boot / Maven processes...
taskkill /F /FI "WINDOWTITLE eq mvn*" >nul 2>&1
if %errorlevel%==0 (
    echo    Stopped Maven
) else (
    echo    No Maven processes found
)

taskkill /F /FI "IMAGENAME eq java.exe" /FI "WINDOWTITLE eq *spring-boot*" >nul 2>&1
if %errorlevel%==0 (
    echo    Stopped Spring Boot
) else (
    echo    No Spring Boot processes found
)

echo.
echo Stopping Node / npm processes...
taskkill /F /FI "IMAGENAME eq node.exe" /FI "WINDOWTITLE eq *vite*" >nul 2>&1
if %errorlevel%==0 (
    echo    Stopped Vite
) else (
    echo    No Vite processes found
)

echo.
echo Checking for services on ports 8080 and 5173...
for /f "tokens=5" %%a in ('netstat -ano ^| find ":8080" ^| find "LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
    if %errorlevel%==0 echo    Killed process on port 8080
)

for /f "tokens=5" %%a in ('netstat -ano ^| find ":5173" ^| find "LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
    if %errorlevel%==0 echo    Killed process on port 5173
)

echo.
echo Cleanup complete!
echo.
echo To start services with Docker Compose, run: start-dev.sh
echo.
pause
