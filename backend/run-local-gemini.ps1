$ErrorActionPreference = "Stop"

$apiKey = $env:GEMINI_API_KEY
if (-not $apiKey) {
    $secureKey = Read-Host "Gemini API key" -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKey)
    try {
        $apiKey = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr).Trim()
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

if (-not $apiKey) {
    Write-Error "Gemini API key is required"
    exit 1
}

$env:AI_PROVIDER = "gemini"
$env:GEMINI_API_KEY = $apiKey
if (-not $env:GEMINI_MODEL) {
    $env:GEMINI_MODEL = "gemini-3.6-flash"
}

$apiKey = $null
mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
