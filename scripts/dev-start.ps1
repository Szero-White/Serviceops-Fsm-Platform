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

if (-not (Test-Path $envFile)) {
    Copy-Item $envExample $envFile
    Write-Host "Created .env from .env.example. Edit it once if your native PostgreSQL credentials differ." -ForegroundColor Yellow
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

if ($StartPostgres) {
    & (Join-Path $PSScriptRoot "start-postgres.ps1")
}

function CmdSet([string]$Name, [string]$Value) {
    return ('set "{0}={1}"' -f $Name, $Value)
}

$backendCommand = @(
    (CmdSet "POSTGRES_HOST" $postgresHost),
    (CmdSet "POSTGRES_PORT" $postgresPort),
    (CmdSet "POSTGRES_DB" $postgresDb),
    (CmdSet "POSTGRES_USER" $postgresUser),
    (CmdSet "POSTGRES_PASSWORD" $postgresPassword),
    (CmdSet "DEMO_PASSWORD" $demoPassword),
    'mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"'
) -join '&& '

# Always start Vite. Install dependencies only when node_modules is missing.
$frontendCommand = @(
    (CmdSet "VITE_DEMO_PASSWORD" $demoPassword),
    'if exist node_modules\.bin\vite.cmd (call npm run dev) else (call npm ci && call npm run dev)'
) -join '&& '

Write-Host "Starting ServiceOps backend and frontend..." -ForegroundColor Cyan
Write-Host "PostgreSQL: $postgresHost`:$postgresPort / $postgresDb / $postgresUser" -ForegroundColor DarkGray
Write-Host "Frontend:   http://localhost:3000" -ForegroundColor DarkGray
Write-Host "Swagger:    http://localhost:8080/swagger-ui.html" -ForegroundColor DarkGray

Start-Process cmd.exe -WorkingDirectory $backendDir -ArgumentList "/k", $backendCommand
Start-Process cmd.exe -WorkingDirectory $frontendDir -ArgumentList "/k", $frontendCommand