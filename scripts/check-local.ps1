$ErrorActionPreference = "Stop"

function Invoke-NativeChecked {
    param(
        [Parameter(Mandatory = $true)][string]$Command,
        [string[]]$CommandArgs = @()
    )

    & $Command @CommandArgs
    if ($LASTEXITCODE -ne 0) {
        throw "$Command failed with exit code $LASTEXITCODE"
    }
}

Write-Host "== Required environment ==" -ForegroundColor Cyan
Invoke-NativeChecked "java" @("-version")
Invoke-NativeChecked "$PSScriptRoot\..\backend\mvnw.cmd" @("-version")
Invoke-NativeChecked "node" @("-v")
Invoke-NativeChecked "npm" @("-v")

$dockerAvailable = $false
try {
    docker version --format '{{.Server.Version}}' | Out-Host
    $dockerAvailable = $LASTEXITCODE -eq 0
} catch {
    $dockerAvailable = $false
}

if ($dockerAvailable) {
    Write-Host "Docker available: Testcontainers integration tests can run." -ForegroundColor DarkGray
} else {
    Write-Host "Docker unavailable: local unit tests still run; Docker/Testcontainers suites may be skipped." -ForegroundColor Yellow
}

Write-Host "== Backend tests ==" -ForegroundColor Cyan
Push-Location "$PSScriptRoot\..\backend"
try {
    Invoke-NativeChecked ".\mvnw.cmd" @("clean", "test")
} finally {
    Pop-Location
}

Write-Host "== Frontend checks ==" -ForegroundColor Cyan
Push-Location "$PSScriptRoot\..\frontend"
try {
    Invoke-NativeChecked "npm" @("ci")
    Invoke-NativeChecked "npm" @("run", "lint")
    Invoke-NativeChecked "npm" @("run", "build")
} finally {
    Pop-Location
}

Write-Host "All executable local checks completed." -ForegroundColor Green
