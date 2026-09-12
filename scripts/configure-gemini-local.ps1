$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$envFile = Join-Path $repoRoot ".env"
$envExample = Join-Path $repoRoot ".env.example"

if (-not (Test-Path $envFile)) {
    Copy-Item $envExample $envFile
}

$secureKey = Read-Host "Gemini API key" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKey)
try {
    $apiKey = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr).Trim()
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
}

if (-not $apiKey) {
    Write-Error "Gemini API key is required"
    exit 1
}

$lines = @(Get-Content $envFile)
$updated = $false
for ($index = 0; $index -lt $lines.Count; $index++) {
    if ($lines[$index] -match '^\s*GEMINI_API_KEY=') {
        $lines[$index] = "GEMINI_API_KEY=$apiKey"
        $updated = $true
        break
    }
}
if (-not $updated) {
    $lines += "GEMINI_API_KEY=$apiKey"
}

[IO.File]::WriteAllLines($envFile, $lines, [Text.UTF8Encoding]::new($false))
$apiKey = $null
Write-Host "Gemini key saved to local .env (ignored by Git)." -ForegroundColor Green
Write-Host "Restart ServiceOps with: .\scripts\dev-start.ps1" -ForegroundColor DarkGray
