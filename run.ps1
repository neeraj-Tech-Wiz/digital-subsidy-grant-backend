# PowerShell Runner for Scheme Master Application
Write-Host "==============================================================================" -ForegroundColor Cyan
Write-Host "   Digital Subsidy & Grant Platform - Scheme Master Module                     " -ForegroundColor Green
Write-Host "==============================================================================" -ForegroundColor Cyan

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

Write-Host "[INFO] Compiling Java source files..." -ForegroundColor Yellow
$javaFiles = Get-ChildItem -Path "src/main/java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName

javac -d bin $javaFiles

if ($LASTEXITCODE -eq 0) {
    Write-Host "[SUCCESS] Compilation successful! Launching terminal interface..." -ForegroundColor Green
    java -cp bin com.digital.subsidy.scheme.SchemeMasterApplication
} else {
    Write-Host "[ERROR] Java compilation failed with exit code $LASTEXITCODE" -ForegroundColor Red
}
