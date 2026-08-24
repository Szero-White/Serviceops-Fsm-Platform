$ErrorActionPreference = "Stop"
$root = Resolve-Path "$PSScriptRoot\.."

Push-Location $root
try {
    if (-not (Test-Path ".env")) {
        Copy-Item ".env.example" ".env"
    }
    docker compose -f docker-compose.local.yml up -d --wait
    docker compose -f docker-compose.local.yml ps
} finally {
    Pop-Location
}
