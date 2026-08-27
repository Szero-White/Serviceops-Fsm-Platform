param(
    [string]$AdminUser,
    [string]$PostgresBin,
    [switch]$SkipBackup
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$envFile = Join-Path $repoRoot ".env"
$envExample = Join-Path $repoRoot ".env.example"
$backupDir = Join-Path $repoRoot "db-backups"

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
    Write-Host "Created .env from .env.example. Review the local PostgreSQL credentials before resetting." -ForegroundColor Yellow
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
if (-not $AdminUser) { $AdminUser = $postgresUser }

if ($postgresHost -notin @("localhost", "127.0.0.1", "::1")) {
    throw "Refusing reset: POSTGRES_HOST='$postgresHost' is not local."
}
if ($postgresDb -notmatch '^[A-Za-z0-9_]+$' -or $postgresUser -notmatch '^[A-Za-z0-9_]+$' -or $AdminUser -notmatch '^[A-Za-z0-9_]+$') {
    throw "Database and role names may contain only letters, numbers and underscore."
}
if ($postgresDb -in @("postgres", "template0", "template1")) {
    throw "Refusing reset of PostgreSQL system database '$postgresDb'."
}

function Resolve-PostgresTool([string]$Name) {
    if ($PostgresBin) {
        $candidate = Join-Path $PostgresBin "$Name.exe"
        if (Test-Path $candidate) { return $candidate }
        throw "Could not find $Name.exe under -PostgresBin '$PostgresBin'."
    }

    $command = Get-Command "$Name.exe" -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }

    $service = Get-CimInstance Win32_Service -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match "postgres" -or $_.DisplayName -match "postgres" } |
        Select-Object -First 1
    if ($service -and $service.PathName -match '"([^\"]+\\pg_ctl\.exe)"') {
        $candidate = Join-Path (Split-Path $matches[1]) "$Name.exe"
        if (Test-Path $candidate) { return $candidate }
    }

    throw "Could not find $Name.exe. Add PostgreSQL bin to PATH or pass -PostgresBin."
}

$psql = Resolve-PostgresTool "psql"
$pgDump = Resolve-PostgresTool "pg_dump"
$pgRestore = Resolve-PostgresTool "pg_restore"

$adminPassword = [Environment]::GetEnvironmentVariable("POSTGRES_ADMIN_PASSWORD", "Process")
if (-not $adminPassword -and $AdminUser -eq $postgresUser) {
    $adminPassword = $postgresPassword
}
if (-not $adminPassword) {
    $secure = Read-Host "PostgreSQL password for admin role '$AdminUser'" -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $adminPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

try {
    $env:PGPASSWORD = $adminPassword

    Write-Host "`n=== LOCAL DATABASE RESET PRECHECK ===" -ForegroundColor Cyan
    Write-Host "Target: $postgresHost`:$postgresPort / $postgresDb / owner $postgresUser" -ForegroundColor DarkGray

    & $psql -h $postgresHost -p $postgresPort -U $AdminUser -d postgres -v ON_ERROR_STOP=1 -c "SELECT current_user;"
    if ($LASTEXITCODE -ne 0) { throw "Admin authentication failed. Database was not reset." }

    $roleExists = (& $psql -h $postgresHost -p $postgresPort -U $AdminUser -d postgres -tA -v ON_ERROR_STOP=1 -c "SELECT 1 FROM pg_roles WHERE rolname='$postgresUser';").Trim()
    if ($LASTEXITCODE -ne 0 -or $roleExists -ne "1") {
        throw "Configured POSTGRES_USER '$postgresUser' does not exist. Database was not reset."
    }

    $databaseExists = (& $psql -h $postgresHost -p $postgresPort -U $AdminUser -d postgres -tA -v ON_ERROR_STOP=1 -c "SELECT 1 FROM pg_database WHERE datname='$postgresDb';").Trim() -eq "1"

    if ($databaseExists -and -not $SkipBackup) {
        New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
        $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
        $backup = Join-Path $backupDir "$postgresDb-before-reset-$stamp.dump"

        Write-Host "`n=== BACKUP CURRENT DATABASE ===" -ForegroundColor Cyan
        & $pgDump -h $postgresHost -p $postgresPort -U $AdminUser -d $postgresDb -F c -f $backup
        if ($LASTEXITCODE -ne 0) { throw "Backup failed. Database was not reset." }

        & $pgRestore -l $backup *> $null
        if ($LASTEXITCODE -ne 0) { throw "Backup verification failed. Database was not reset." }

        $backupFile = Get-Item $backup
        Write-Host "Backup verified: $($backupFile.FullName) ($([math]::Round($backupFile.Length / 1KB, 1)) KB)" -ForegroundColor Green
    }
    elseif ($databaseExists) {
        Write-Host "Backup skipped explicitly with -SkipBackup." -ForegroundColor Yellow
    }
    else {
        Write-Host "Database '$postgresDb' does not exist yet; there is nothing to back up." -ForegroundColor Yellow
    }

    $confirmation = Read-Host "Type '$postgresDb' to DROP and recreate this LOCAL database"
    if ($confirmation -ne $postgresDb) {
        throw "Confirmation did not match. Database was not reset."
    }

    Write-Host "`n=== RESET LOCAL DATABASE ===" -ForegroundColor Cyan
    & $psql -h $postgresHost -p $postgresPort -U $AdminUser -d postgres -v ON_ERROR_STOP=1 -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='$postgresDb' AND pid <> pg_backend_pid();"
    if ($LASTEXITCODE -ne 0) { throw "Could not terminate database connections." }

    & $psql -h $postgresHost -p $postgresPort -U $AdminUser -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS $postgresDb;"
    if ($LASTEXITCODE -ne 0) { throw "DROP DATABASE failed." }

    & $psql -h $postgresHost -p $postgresPort -U $AdminUser -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE $postgresDb OWNER $postgresUser;"
    if ($LASTEXITCODE -ne 0) { throw "CREATE DATABASE failed." }

    Write-Host "`nLocal database recreated successfully." -ForegroundColor Green
    Write-Host "Next: .\scripts\dev-start.ps1" -ForegroundColor Green
    Write-Host "Flyway will migrate V1 -> latest and the local seeder will recreate demo data on first boot." -ForegroundColor DarkGray
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
