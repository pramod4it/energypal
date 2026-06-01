@echo off
setlocal

set "ROOT=%~dp0"
cd /d "%ROOT%"

echo.
echo ============================================================
echo EnergyPal - Local Core Mode
echo No Docker, no Kafka, no Elasticsearch
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

echo Checking Java and Maven...
java -version
call mvn -version

echo.
echo Stopping any existing EnergyPal local Java services...
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$root=(Resolve-Path '.').Path; $ports=8080..8090; $listeners=Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $ports -contains $_.LocalPort } | Select-Object -ExpandProperty OwningProcess -Unique; foreach($id in $listeners){ $p=Get-CimInstance Win32_Process -Filter \"ProcessId=$id\"; if($p.CommandLine -and $p.CommandLine.Contains($root) -and $p.CommandLine.Contains('-0.1.0-SNAPSHOT.jar')){ Stop-Process -Id $id -Force -ErrorAction SilentlyContinue; Write-Host \"Stopped stale service process $id\" } }; Remove-Item -Path '.local-pids' -Recurse -Force -ErrorAction SilentlyContinue"

echo.
echo Building Java artifacts...
call mvn clean package
if errorlevel 1 exit /b 1

if not exist "%ROOT%logs\local" mkdir "%ROOT%logs\local"
if not exist "%ROOT%.local-pids" mkdir "%ROOT%.local-pids"

echo.
echo Starting core services...
call :start_service auth-service 8081
timeout /t 5 /nobreak >nul
call :start_service customer-service 8082
call :start_service supplier-service 8083
call :start_service tariff-service 8084
call :start_service usage-service 8085
call :start_service payment-service 8087
timeout /t 5 /nobreak >nul
call :start_service api-gateway 8080

echo.
echo Waiting for Auth Service and API Gateway...
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$targets=@(@('Auth Service','http://localhost:8081/actuator/health'),@('API Gateway','http://localhost:8080/actuator/health')); foreach($target in $targets){ $name=$target[0]; $url=$target[1]; $deadline=(Get-Date).AddSeconds(240); $ready=$false; while((Get-Date) -lt $deadline){ try{ $r=Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5; if($r.StatusCode -ge 200 -and $r.StatusCode -lt 300){ Write-Host \"$name is ready: $url\"; $ready=$true; break } } catch{} Start-Sleep -Seconds 3 }; if(-not $ready){ Write-Host \"$name did not become ready: $url\"; exit 1 } }"
if errorlevel 1 exit /b 1

echo.
echo ============================================================
echo EnergyPal local core services are running.
echo API Gateway:  http://localhost:8080
echo Swagger UI:   http://localhost:8080/swagger-ui.html
echo Auth Service: http://localhost:8081
echo Logs:         %ROOT%logs\local
echo.
echo Skipped services: billing-service, notification-service,
echo search-indexer-service, audit-service.
echo Those require Kafka and/or Elasticsearch.
echo.
echo To stop local services manually, run:
echo powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$root=(Resolve-Path '.').Path; Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -and $_.CommandLine.Contains($root) -and $_.CommandLine.Contains('-0.1.0-SNAPSHOT.jar') } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }"
echo ============================================================
echo.

exit /b 0

:start_service
set "MODULE=%~1"
set "PORT=%~2"
set "JAR=%ROOT%%MODULE%\target\%MODULE%-0.1.0-SNAPSHOT.jar"
set "OUT=%ROOT%logs\local\%MODULE%.out.log"
set "ERR=%ROOT%logs\local\%MODULE%.err.log"
set "PID=%ROOT%.local-pids\%MODULE%.pid"

if not exist "%JAR%" (
  echo Missing jar: %JAR%
  exit /b 1
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$p=Start-Process -FilePath 'java.exe' -ArgumentList @('-jar','%JAR%') -WorkingDirectory '%ROOT%' -RedirectStandardOutput '%OUT%' -RedirectStandardError '%ERR%' -WindowStyle Hidden -PassThru; Set-Content -Path '%PID%' -Value $p.Id -Encoding ASCII; Write-Host 'Started %MODULE% on port %PORT%'"
exit /b 0
