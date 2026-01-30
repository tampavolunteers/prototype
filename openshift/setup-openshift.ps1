# OpenShift Project Setup and Deployment Script for Windows
# This script sets up the Tampa Volunteers platform on OpenShift/OKD

param(
    [switch]$CheckOnly,
    [switch]$ProjectOnly,
    [switch]$BuildOnly,
    [switch]$DeployOnly,
    [switch]$Status,
    [switch]$Help,
    [string]$ProjectName = "tampavolunteers"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host ">>> $Message" -ForegroundColor Blue
}

function Write-Success {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[!] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[X] $Message" -ForegroundColor Red
}

Write-Host "Tampa Volunteers - OpenShift Setup Script" -ForegroundColor Green
Write-Host "=========================================="

if ($Help) {
    Write-Host ""
    Write-Host "Usage: .\setup-openshift.ps1 [OPTIONS]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  (none)         Full setup: project, secrets, builds, deploy"
    Write-Host "  -CheckOnly     Check prerequisites only"
    Write-Host "  -ProjectOnly   Create/switch to project only"
    Write-Host "  -BuildOnly     Setup and start builds only"
    Write-Host "  -DeployOnly    Deploy application only"
    Write-Host "  -Status        Show deployment status"
    Write-Host "  -Help          Show this help"
    Write-Host ""
    Write-Host "Parameters:"
    Write-Host "  -ProjectName   OpenShift project name (default: tampavolunteers)"
    exit 0
}

function Test-Prerequisites {
    Write-Step "Checking prerequisites..."

    # Check for oc CLI
    $oc = Get-Command oc -ErrorAction SilentlyContinue
    if (-not $oc) {
        Write-Error "OpenShift CLI (oc) is not installed."
        Write-Host ""
        Write-Host "Install it using:"
        Write-Host "  .\scripts\install-oc-windows.ps1"
        Write-Host ""
        exit 1
    }
    $ocVersion = & oc version --client 2>$null | Select-Object -First 1
    Write-Success "OpenShift CLI found: $ocVersion"

    # Check if logged in
    try {
        $user = & oc whoami 2>$null
        $server = & oc whoami --show-server 2>$null
        Write-Success "Logged in as: $user"
        Write-Success "Server: $server"
    }
    catch {
        Write-Error "Not logged in to OpenShift cluster."
        Write-Host ""
        Write-Host "Log in using:"
        Write-Host "  oc login <cluster-url>"
        Write-Host ""
        exit 1
    }
}

function Set-Project {
    Write-Step "Setting up project: $ProjectName"

    $existing = & oc get project $ProjectName 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Warning "Project '$ProjectName' already exists, switching to it..."
        & oc project $ProjectName
    }
    else {
        Write-Host "Creating new project: $ProjectName"
        & oc new-project $ProjectName `
            --display-name="Tampa Volunteers" `
            --description="Tampa Volunteers Platform - Connecting volunteers with opportunities"
    }
    Write-Success "Using project: $ProjectName"
}

function Set-Secrets {
    Write-Step "Configuring secrets..."

    Write-Host ""
    Write-Warning "IMPORTANT: Update the secrets in openshift/base/secret.yaml before deploying to production!"
    Write-Host ""

    $response = Read-Host "Do you want to generate new secure secrets now? (y/N)"
    if ($response -eq "y" -or $response -eq "Y") {
        # Generate secure secrets using .NET
        $bytes = New-Object byte[] 48
        $rng = [System.Security.Cryptography.RNGCryptoServiceProvider]::Create()
        $rng.GetBytes($bytes)
        $jwtSecret = [Convert]::ToBase64String($bytes)

        $bytes = New-Object byte[] 24
        $rng.GetBytes($bytes)
        $dbPassword = [Convert]::ToBase64String($bytes) -replace '[^a-zA-Z0-9]', ''
        $dbPassword = $dbPassword.Substring(0, [Math]::Min(24, $dbPassword.Length))

        Write-Host ""
        Write-Host "Generated secrets (save these securely):" -ForegroundColor Yellow
        Write-Host "  JWT_SECRET: $jwtSecret"
        Write-Host "  DB_PASSWORD: $dbPassword"
        Write-Host ""

        # Update secret.yaml
        $secretFile = Join-Path $ScriptDir "base\secret.yaml"
        $content = Get-Content $secretFile -Raw
        $content = $content -replace 'JWT_SECRET:.*', "JWT_SECRET: `"$jwtSecret`""
        $content = $content -replace 'POSTGRES_PASSWORD:.*', "POSTGRES_PASSWORD: `"$dbPassword`""
        $content = $content -replace 'SPRING_DATASOURCE_PASSWORD:.*', "SPRING_DATASOURCE_PASSWORD: `"$dbPassword`""
        Set-Content -Path $secretFile -Value $content

        Write-Success "Secrets updated in secret.yaml"
    }
    else {
        Write-Warning "Using default secrets - CHANGE BEFORE PRODUCTION!"
    }
}

function Apply-Resources {
    Write-Step "Applying Kubernetes resources..."

    Write-Host "Applying namespace..."
    & oc apply -f "$ScriptDir\base\namespace.yaml" 2>$null

    Write-Host "Applying ConfigMap..."
    & oc apply -f "$ScriptDir\base\configmap.yaml"

    Write-Host "Applying Secrets..."
    & oc apply -f "$ScriptDir\base\secret.yaml"

    Write-Host "Applying PostgreSQL PVC..."
    & oc apply -f "$ScriptDir\base\postgres-pvc.yaml"

    Write-Host "Applying PostgreSQL Deployment and Service..."
    & oc apply -f "$ScriptDir\base\postgres-deployment.yaml"
    & oc apply -f "$ScriptDir\base\postgres-service.yaml"

    Write-Success "Core resources applied"
}

function Set-Builds {
    Write-Step "Setting up image builds..."

    Write-Host "Creating Backend ImageStream and BuildConfig..."
    & oc apply -f "$ScriptDir\base\backend-buildconfig.yaml"

    Write-Host "Creating Frontend ImageStream and BuildConfig..."
    & oc apply -f "$ScriptDir\base\frontend-buildconfig.yaml"

    Write-Success "Build configurations created"
}

function Start-Builds {
    Write-Step "Starting image builds..."

    $response = Read-Host "Do you want to start the builds now? (Y/n)"
    if ($response -ne "n" -and $response -ne "N") {
        Write-Host "Starting backend build..."
        & oc start-build backend

        Write-Host "Starting frontend build..."
        & oc start-build frontend

        Write-Host ""
        Write-Host "Builds started. Monitor with:"
        Write-Host "  oc logs -f bc/backend"
        Write-Host "  oc logs -f bc/frontend"
    }
    else {
        Write-Host "Skipping builds. Start manually with:"
        Write-Host "  oc start-build backend"
        Write-Host "  oc start-build frontend"
    }
}

function Deploy-Application {
    Write-Step "Deploying application components..."

    Write-Host "Deploying Backend..."
    & oc apply -f "$ScriptDir\base\backend-deployment.yaml"
    & oc apply -f "$ScriptDir\base\backend-service.yaml"
    & oc apply -f "$ScriptDir\base\backend-route.yaml"

    Write-Host "Deploying Frontend..."
    & oc apply -f "$ScriptDir\base\frontend-deployment.yaml"
    & oc apply -f "$ScriptDir\base\frontend-service.yaml"
    & oc apply -f "$ScriptDir\base\frontend-route.yaml"

    Write-Success "Application deployed"
}

function Show-Status {
    Write-Step "Deployment Status"

    Write-Host ""
    Write-Host "Pods:"
    & oc get pods -n $ProjectName

    Write-Host ""
    Write-Host "Services:"
    & oc get services -n $ProjectName

    Write-Host ""
    Write-Host "Routes:"
    & oc get routes -n $ProjectName

    Write-Host ""
    Write-Host "Application URLs:" -ForegroundColor Green
    try {
        $frontendUrl = & oc get route frontend -n $ProjectName -o jsonpath='{.spec.host}' 2>$null
        $backendUrl = & oc get route backend -n $ProjectName -o jsonpath='{.spec.host}' 2>$null
        Write-Host "  Frontend: https://$frontendUrl"
        Write-Host "  Backend:  https://$backendUrl/api"
    }
    catch {
        Write-Host "  (pending - routes not yet created)"
    }

    Write-Host ""
    Write-Warning "Note: It may take a few minutes for pods to become ready."
    Write-Host "Monitor with: oc get pods -w"
}

# Main execution
if ($CheckOnly) {
    Test-Prerequisites
}
elseif ($ProjectOnly) {
    Test-Prerequisites
    Set-Project
}
elseif ($BuildOnly) {
    Test-Prerequisites
    Set-Builds
    Start-Builds
}
elseif ($DeployOnly) {
    Test-Prerequisites
    Deploy-Application
    Show-Status
}
elseif ($Status) {
    & oc project $ProjectName 2>$null
    Show-Status
}
else {
    # Full setup
    Test-Prerequisites
    Set-Project
    Set-Secrets
    Apply-Resources
    Set-Builds
    Start-Builds
    Deploy-Application
    Show-Status
}
