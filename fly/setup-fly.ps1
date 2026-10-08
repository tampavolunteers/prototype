# Fly.io Project Setup and Deployment Script for Windows
# This script sets up the Tampa Volunteers platform on Fly.io

param(
    [switch]$CheckOnly,
    [switch]$AppsOnly,
    [switch]$DbOnly,
    [switch]$SecretsOnly,
    [switch]$DeployOnly,
    [switch]$Status,
    [switch]$Help,
    [string]$BackendApp = "tampavolunteers-backend",
    [string]$FrontendApp = "tampavolunteers-frontend",
    [string]$DbApp = "tampavolunteers-db",
    [string]$Region = "iad",
    [string]$Org = "personal"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir

function Write-Step { param([string]$Message) Write-Host ""; Write-Host ">>> $Message" -ForegroundColor Blue }
function Write-Success { param([string]$Message) Write-Host "[OK] $Message" -ForegroundColor Green }
function Write-Warning2 { param([string]$Message) Write-Host "[!] $Message" -ForegroundColor Yellow }
function Write-Error2 { param([string]$Message) Write-Host "[X] $Message" -ForegroundColor Red }

Write-Host "Tampa Volunteers - Fly.io Setup Script" -ForegroundColor Green
Write-Host "========================================"

if ($Help) {
    Write-Host ""
    Write-Host "Usage: .\setup-fly.ps1 [OPTIONS]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  (none)          Full setup: apps, database, secrets, deploy"
    Write-Host "  -CheckOnly      Check prerequisites only"
    Write-Host "  -AppsOnly       Create Fly apps only"
    Write-Host "  -DbOnly         Create/attach Postgres only"
    Write-Host "  -SecretsOnly    Configure application secrets only"
    Write-Host "  -DeployOnly     Deploy backend + frontend only"
    Write-Host "  -Status         Show deployment status"
    Write-Host "  -Help           Show this help"
    Write-Host ""
    Write-Host "Parameters:"
    Write-Host "  -BackendApp     Fly app name for backend (default: tampavolunteers-backend)"
    Write-Host "  -FrontendApp    Fly app name for frontend (default: tampavolunteers-frontend)"
    Write-Host "  -DbApp          Fly app name for Postgres (default: tampavolunteers-db)"
    Write-Host "  -Region         Fly region (default: iad)"
    Write-Host "  -Org            Fly org slug (default: personal)"
    exit 0
}

function Test-Prerequisites {
    Write-Step "Checking prerequisites..."

    $flyCmd = Get-Command fly -ErrorAction SilentlyContinue
    if (-not $flyCmd) { $flyCmd = Get-Command flyctl -ErrorAction SilentlyContinue }
    if (-not $flyCmd) {
        Write-Error2 "Fly CLI is not installed."
        Write-Host ""
        Write-Host "Install it using:"
        Write-Host "  .\scripts\install-flyctl-windows.ps1"
        Write-Host "Or see: https://fly.io/docs/flyctl/install/"
        exit 1
    }
    $flyVersion = & fly version 2>$null | Select-Object -First 1
    Write-Success "Fly CLI found: $flyVersion"

    try {
        $whoami = & fly auth whoami 2>$null
        if (-not $whoami) { throw "not logged in" }
        Write-Success "Logged in as: $whoami"
    }
    catch {
        Write-Error2 "Not logged in to Fly.io."
        Write-Host ""
        Write-Host "Log in using: fly auth login"
        exit 1
    }
}

function New-Apps {
    Write-Step "Creating Fly apps (if they don't already exist)..."

    $existing = (& fly apps list) -join "`n"

    foreach ($app in @($BackendApp, $FrontendApp)) {
        if ($existing -match "(?m)^$([regex]::Escape($app))\s") {
            Write-Warning2 "App '$app' already exists, skipping creation"
        }
        else {
            & fly apps create $app --org $Org
            Write-Success "Created app: $app"
        }
    }
}

function New-Database {
    Write-Step "Setting up Postgres..."

    $existing = (& fly apps list) -join "`n"
    if ($existing -match "(?m)^$([regex]::Escape($DbApp))\s") {
        Write-Warning2 "Postgres app '$DbApp' already exists, skipping creation"
    }
    else {
        & fly postgres create --name $DbApp --org $Org --region $Region `
            --initial-cluster-size 1 --vm-size shared-cpu-1x --volume-size 3
        Write-Success "Postgres cluster created: $DbApp"
    }

    Write-Step "Attaching Postgres to the backend app..."

    $attachOutput = & fly postgres attach --app $BackendApp $DbApp 2>&1 | Out-String
    Write-Host $attachOutput

    $match = [regex]::Match($attachOutput, 'postgres://\S+')
    $databaseUrl = $null
    if ($match.Success) { $databaseUrl = $match.Value }

    if (-not $databaseUrl) {
        Write-Warning2 "Could not automatically parse the Postgres connection string."
        $databaseUrl = Read-Host "Paste the DATABASE_URL shown above"
    }

    # postgres://user:pass@host:5432/dbname?sslmode=disable
    $uri = [regex]::Match($databaseUrl, 'postgres://(?<user>[^:]+):(?<pass>[^@]+)@(?<hostport>[^/]+)/(?<db>[^?]+)')
    if (-not $uri.Success) {
        Write-Error2 "Could not parse DATABASE_URL: $databaseUrl"
        return
    }

    $pgUser = $uri.Groups["user"].Value
    $pgPassword = $uri.Groups["pass"].Value
    $hostPort = $uri.Groups["hostport"].Value
    $dbName = $uri.Groups["db"].Value
    $jdbcUrl = "jdbc:postgresql://$hostPort/$dbName`?sslmode=disable"

    & fly secrets set --app $BackendApp `
        SPRING_DATASOURCE_URL="$jdbcUrl" `
        SPRING_DATASOURCE_USERNAME="$pgUser" `
        SPRING_DATASOURCE_PASSWORD="$pgPassword"

    Write-Success "Backend wired to Postgres ($jdbcUrl)"
}

function Set-Secrets {
    Write-Step "Configuring application secrets..."

    $response = Read-Host "Generate a new secure JWT_SECRET now? (y/N)"
    if ($response -eq "y" -or $response -eq "Y") {
        $bytes = New-Object byte[] 48
        [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
        $jwtSecret = [Convert]::ToBase64String($bytes)
        & fly secrets set --app $BackendApp JWT_SECRET="$jwtSecret"
        Write-Success "JWT_SECRET set"
    }
    else {
        Write-Warning2 "Skipping JWT_SECRET - set it manually with: fly secrets set --app $BackendApp JWT_SECRET=..."
    }

    Write-Host ""
    Write-Host "OAuth2 credentials (leave blank to configure later):"
    $googleId = Read-Host "  GOOGLE_CLIENT_ID"
    $googleSecret = Read-Host "  GOOGLE_CLIENT_SECRET"
    $githubId = Read-Host "  GITHUB_CLIENT_ID"
    $githubSecret = Read-Host "  GITHUB_CLIENT_SECRET"

    $pairs = @()
    if ($googleId) { $pairs += "GOOGLE_CLIENT_ID=$googleId" }
    if ($googleSecret) { $pairs += "GOOGLE_CLIENT_SECRET=$googleSecret" }
    if ($githubId) { $pairs += "GITHUB_CLIENT_ID=$githubId" }
    if ($githubSecret) { $pairs += "GITHUB_CLIENT_SECRET=$githubSecret" }

    if ($pairs.Count -gt 0) {
        & fly secrets set --app $BackendApp @pairs
        Write-Success "OAuth2 credentials set"
    }
    else {
        Write-Warning2 "Skipping OAuth2 credentials - set them later with: fly secrets set --app $BackendApp GOOGLE_CLIENT_ID=... GOOGLE_CLIENT_SECRET=... GITHUB_CLIENT_ID=... GITHUB_CLIENT_SECRET=..."
    }
}

function Deploy-Application {
    Write-Step "Deploying backend..."
    Push-Location (Join-Path $RootDir "tampavolunteers-backend")
    try { & fly deploy --app $BackendApp } finally { Pop-Location }
    Write-Success "Backend deployed"

    Write-Step "Deploying frontend..."
    Push-Location (Join-Path $RootDir "tampavolunteers-frontend")
    try { & fly deploy --app $FrontendApp } finally { Pop-Location }
    Write-Success "Frontend deployed"
}

function Show-Status {
    Write-Step "Deployment Status"

    Write-Host ""
    Write-Host "Backend:"
    & fly status --app $BackendApp

    Write-Host ""
    Write-Host "Frontend:"
    & fly status --app $FrontendApp

    Write-Host ""
    Write-Host "Application URLs:" -ForegroundColor Green
    Write-Host "  Frontend: https://$FrontendApp.fly.dev"
    Write-Host "  Backend:  https://$BackendApp.fly.dev/api"
}

if ($CheckOnly) {
    Test-Prerequisites
}
elseif ($AppsOnly) {
    Test-Prerequisites
    New-Apps
}
elseif ($DbOnly) {
    Test-Prerequisites
    New-Database
}
elseif ($SecretsOnly) {
    Test-Prerequisites
    Set-Secrets
}
elseif ($DeployOnly) {
    Test-Prerequisites
    Deploy-Application
    Show-Status
}
elseif ($Status) {
    Show-Status
}
else {
    Test-Prerequisites
    New-Apps
    New-Database
    Set-Secrets
    Deploy-Application
    Show-Status
}
