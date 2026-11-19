$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $projectRoot 'backend/.env'
if (-not (Test-Path -LiteralPath $envPath)) { throw 'Create backend/.env from backend/.env.example and fill it locally first.' }
foreach ($line in Get-Content -LiteralPath $envPath) {
    if ($line -match '^\s*([A-Z][A-Z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2].Trim().Trim('"').Trim("'"), 'Process')
    }
}
foreach ($key in @('DATABASE_URL','DATABASE_USERNAME','DATABASE_PASSWORD','SUPABASE_URL','SUPABASE_SERVICE_KEY','SUPABASE_PUBLISHABLE_KEY')) {
    if (-not [Environment]::GetEnvironmentVariable($key, 'Process')) { throw "Missing required setting: $key" }
}
Push-Location (Join-Path $projectRoot 'backend')
try {
    if (Test-Path -LiteralPath './mvnw.cmd') { & ./mvnw.cmd spring-boot:run }
    else { & mvn.cmd spring-boot:run }
    if ($LASTEXITCODE -ne 0) { throw 'Backend startup failed. See the server output.' }
} finally { Pop-Location }
