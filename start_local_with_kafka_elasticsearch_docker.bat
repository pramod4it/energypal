@echo off
setlocal

set "ROOT=%~dp0"
cd /d "%ROOT%"

echo.
echo ============================================================
echo EnergyPal - Full Docker Mode
echo Includes Kafka-compatible Redpanda and Elasticsearch
echo ============================================================
echo.

where java >nul 2>nul
if errorlevel 1 (
  echo Java was not found on PATH. Install JDK 17 or later.
  exit /b 1
)

where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven was not found on PATH. Install Maven 3.9 or later.
  exit /b 1
)

where docker >nul 2>nul
if errorlevel 1 (
  echo Docker was not found on PATH. Install Docker Desktop.
  exit /b 1
)

docker compose version >nul 2>nul
if errorlevel 1 (
  echo Docker Compose V2 is not available. Install or update Docker Desktop.
  exit /b 1
)

echo Checking Java, Maven, Docker, and Docker Compose...
java -version
call mvn -version
docker --version
docker compose version

echo.
echo Building Java artifacts...
call mvn clean package
if errorlevel 1 exit /b 1

echo.
echo Starting Docker Compose stack...
docker compose up --build -d
if errorlevel 1 exit /b 1

echo.
echo Waiting for Auth Service and API Gateway...
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$targets=@(@('Auth Service','http://localhost:8081/actuator/health'),@('API Gateway','http://localhost:8080/actuator/health')); foreach($target in $targets){ $name=$target[0]; $url=$target[1]; $deadline=(Get-Date).AddSeconds(300); $ready=$false; while((Get-Date) -lt $deadline){ try{ $r=Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5; if($r.StatusCode -ge 200 -and $r.StatusCode -lt 300){ Write-Host \"$name is ready: $url\"; $ready=$true; break } } catch{} Start-Sleep -Seconds 5 }; if(-not $ready){ Write-Host \"$name did not become ready: $url\"; docker compose ps; exit 1 } }"
if errorlevel 1 exit /b 1

echo.
echo Running gateway registration smoke test...
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$body=@{email='reviewer@example.com';password='Passw0rd!';role='CUSTOMER'} | ConvertTo-Json -Compress; try{ $r=Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/register' -Method Post -ContentType 'application/json' -Body $body -UseBasicParsing -TimeoutSec 20; Write-Host \"Smoke test status: $($r.StatusCode)\" } catch { Write-Host 'Smoke test failed, but the stack is running. Check docker compose logs.' }"

echo.
echo ============================================================
echo EnergyPal full Docker stack is running.
echo API Gateway:    http://localhost:8080
echo Swagger UI:     http://localhost:8080/swagger-ui.html
echo Auth Service:   http://localhost:8081
echo Kafka/Redpanda: localhost:9092
echo Elasticsearch:  http://localhost:9200
echo.
echo To stop Docker services, run:
echo docker compose down
echo ============================================================
echo.

exit /b 0
