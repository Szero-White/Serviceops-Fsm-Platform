param(
    [switch]$StartPostgres
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$backendDir = Join-Path $repoRoot "backend"
$frontendDir = Join-Path $repoRoot "frontend"
$envFile = Join-Path $repoRoot ".env"
$envExample = Join-Path $repoRoot ".env.example"

function Read-DotEnv([string]$Path) {
    $values = @{}
    if (-not (Test-Path $Path)) { return $values }

    foreach ($line in Get-Content $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#")) { continue }
        $separator = $trimmed.IndexOf("=")
        if ($separator -lt 1) { continue }
        $name = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1).Trim()
        $values[$name] = $value
    }
    return $values
}

function Start-InheritedEnvironmentProcess(
    [string]$WorkingDirectory,
    [string]$Command,
    [hashtable]$Environment
) {
    $previous = @{}
    foreach ($name in $Environment.Keys) {
        $previous[$name] = [Environment]::GetEnvironmentVariable($name, "Process")
        [Environment]::SetEnvironmentVariable($name, [string]$Environment[$name], "Process")
    }

    try {
        Start-Process cmd.exe -WorkingDirectory $WorkingDirectory -ArgumentList "/k", $Command
    }
    finally {
        foreach ($name in $Environment.Keys) {
            [Environment]::SetEnvironmentVariable($name, $previous[$name], "Process")
        }
    }
}

if (-not (Test-Path $envFile)) {
    Copy-Item $envExample $envFile
    Write-Host "Created .env from .env.example. This file is ignored by Git." -ForegroundColor Yellow
}

$settings = Read-DotEnv $envFile

function Get-Setting([string]$Name, [string]$DefaultValue) {
    $processValue = [Environment]::GetEnvironmentVariable($Name, "Process")
    if ($processValue) { return $processValue }
    if ($settings.ContainsKey($Name) -and $settings[$Name]) { return $settings[$Name] }
    return $DefaultValue
}

$postgresHost = Get-Setting "POSTGRES_HOST" "localhost"
$postgresPort = Get-Setting "POSTGRES_PORT" "5432"
$postgresDb = Get-Setting "POSTGRES_DB" "serviceops"
$postgresUser = Get-Setting "POSTGRES_USER" "serviceops"
$postgresPassword = Get-Setting "POSTGRES_PASSWORD" "serviceops"
$demoPassword = Get-Setting "DEMO_PASSWORD" "Demo@2026"
$aiEnabled = Get-Setting "AI_ENABLED" "true"
$aiProvider = Get-Setting "AI_PROVIDER" "gemini"
$geminiApiKey = Get-Setting "GEMINI_API_KEY" ""
$geminiBaseUrl = Get-Setting "GEMINI_BASE_URL" "https://generativelanguage.googleapis.com/v1beta"
$geminiModel = Get-Setting "GEMINI_MODEL" "gemini-3.6-flash"
$aiConnectTimeout = Get-Setting "AI_CONNECT_TIMEOUT" "4s"
$aiSuggestionTimeout = Get-Setting "AI_SUGGESTION_TIMEOUT" "12s"
$aiHelpTimeout = Get-Setting "AI_HELP_TIMEOUT" "18s"

if ($StartPostgres) {
    & (Join-Path $PSScriptRoot "start-postgres.ps1")
}

$backendEnvironment = @{
    POSTGRES_HOST = $postgresHost
    POSTGRES_PORT = $postgresPort
    POSTGRES_DB = $postgresDb
    POSTGRES_USER = $postgresUser
    POSTGRES_PASSWORD = $postgresPassword
    DEMO_PASSWORD = $demoPassword
    AI_ENABLED = $aiEnabled
    AI_PROVIDER = $aiProvider
    GEMINI_API_KEY = $geminiApiKey
    GEMINI_BASE_URL = $geminiBaseUrl
    GEMINI_MODEL = $geminiModel
    AI_CONNECT_TIMEOUT = $aiConnectTimeout
    AI_SUGGESTION_TIMEOUT = $aiSuggestionTimeout
    AI_HELP_TIMEOUT = $aiHelpTimeout
}

$frontendEnvironment = @{
    VITE_DEMO_PASSWORD = $demoPassword
}

Write-Host "Starting ServiceOps backend and frontend..." -ForegroundColor Cyan
Write-Host "PostgreSQL: $postgresHost`:$postgresPort / $postgresDb / $postgresUser" -ForegroundColor DarkGray
Write-Host "Frontend:   http://localhost:3000" -ForegroundColor DarkGray
Write-Host "Swagger:    http://localhost:8080/swagger-ui.html" -ForegroundColor DarkGray

if ($aiEnabled -eq "true" -and $aiProvider -eq "gemini") {
    if ($geminiApiKey) {
        Write-Host "AI:         Gemini configured for local development" -ForegroundColor DarkGray
    }
    else {
        Write-Warning "AI is enabled but GEMINI_API_KEY is missing. ServiceOps will use the built-in fallback. Run scripts/configure-gemini-local.ps1 to configure local Gemini access."
    }
}
elseif ($aiEnabled -eq "false") {
    Write-Host "AI:         disabled" -ForegroundColor DarkGray
}
else {
    Write-Host "AI:         $aiProvider" -ForegroundColor DarkGray
}

# Secrets are inherited through the child process environment. They are not
# embedded in the cmd.exe command line where process-inspection tools could expose them.
Start-InheritedEnvironmentProcess $backendDir 'mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"' $backendEnvironment

# Always start Vite. Install dependencies only when node_modules is missing.
$frontendCommand = 'if exist node_modules\.bin\vite.cmd (call npm run dev) else (call npm ci && call npm run dev)'
Start-InheritedEnvironmentProcess $frontendDir $frontendCommand $frontendEnvironment
