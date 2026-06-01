@echo off
setlocal

set "ROOT=%~dp0"
cd /d "%ROOT%"

echo.
echo ============================================================
echo EnergyPal - Stop Full Docker Mode
echo Includes Kafka-compatible Redpanda and Elasticsearch
echo ============================================================
echo.

where docker >nul 2>nul
if errorlevel 1 (
  echo Docker was not found on PATH. Nothing to stop from Docker Compose.
  exit /b 1
)

docker compose down
if errorlevel 1 exit /b 1

echo.
echo EnergyPal Docker stack stopped.
echo.

exit /b 0
