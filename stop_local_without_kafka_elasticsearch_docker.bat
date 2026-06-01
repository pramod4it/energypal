@echo off
setlocal

set "ROOT=%~dp0"
cd /d "%ROOT%"

echo.
echo ============================================================
echo EnergyPal - Stop Local Core Mode
echo No Docker, no Kafka, no Elasticsearch
echo ============================================================
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$root=(Resolve-Path '.').Path; $found=$false; Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -and $_.CommandLine.Contains($root) -and $_.CommandLine.Contains('-0.1.0-SNAPSHOT.jar') } | ForEach-Object { $found=$true; Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue; Write-Host \"Stopped EnergyPal Java process $($_.ProcessId)\" }; Remove-Item -Path '.local-pids' -Recurse -Force -ErrorAction SilentlyContinue; if(-not $found){ Write-Host 'No EnergyPal local Java services were running.' }"

echo.
echo EnergyPal local core services stopped.
echo.

exit /b 0
