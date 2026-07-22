$ErrorActionPreference = "Stop"
$root = Resolve-Path "$PSScriptRoot\.."
Push-Location $root
if (-not (Test-Path ".env")) { Copy-Item ".env.example" ".env" }
docker compose -f docker-compose.local.yml up -d
docker compose -f docker-compose.local.yml ps
Pop-Location
