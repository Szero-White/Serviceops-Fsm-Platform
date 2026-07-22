$ErrorActionPreference = "Stop"
Write-Host "== Environment ==" -ForegroundColor Cyan
java -version
& "$PSScriptRoot\..\backend\mvnw.cmd" -version
node -v
npm -v
docker version --format '{{.Server.Version}}'

Write-Host "== Backend tests ==" -ForegroundColor Cyan
Push-Location "$PSScriptRoot\..\backend"
.\mvnw.cmd clean test
Pop-Location

Write-Host "== Frontend checks ==" -ForegroundColor Cyan
Push-Location "$PSScriptRoot\..\frontend"
npm ci
npm run lint
npm run build
Pop-Location

Write-Host "All local checks passed." -ForegroundColor Green
