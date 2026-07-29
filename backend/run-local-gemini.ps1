param(
    [string]$ApiKey
)

if (-not $ApiKey) {
    $ApiKey = Read-Host "Gemini API key"
}

if (-not $ApiKey.Trim()) {
    Write-Error "Gemini API key is required"
    exit 1
}

$env:AI_PROVIDER = "gemini"
$env:GEMINI_API_KEY = $ApiKey.Trim()

if (-not $env:GEMINI_MODEL) {
    $env:GEMINI_MODEL = "gemini-3.6-flash"
}

mvn.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
