# OpenShift CLI (oc) Installation Script for Windows
# Run this script in PowerShell as Administrator
# Usage: .\install-oc-windows.ps1

param(
    [switch]$Force,
    [string]$InstallPath = "$env:LOCALAPPDATA\Programs\oc"
)

$ErrorActionPreference = "Stop"

Write-Host "OpenShift CLI (oc) Installation Script for Windows" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green

# Check if running as Administrator for PATH modification
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "WARNING: Not running as Administrator. PATH may not be updated system-wide." -ForegroundColor Yellow
    Write-Host "Consider running PowerShell as Administrator for full installation." -ForegroundColor Yellow
    Write-Host ""
}

# Check if oc is already installed
$existingOc = Get-Command oc -ErrorAction SilentlyContinue
if ($existingOc -and -not $Force) {
    Write-Host "OpenShift CLI is already installed:" -ForegroundColor Green
    & oc version --client
    Write-Host ""
    $response = Read-Host "Do you want to reinstall/upgrade? (y/N)"
    if ($response -ne "y" -and $response -ne "Y") {
        Write-Host "Exiting without changes."
        exit 0
    }
}

# Detect architecture
$arch = [System.Environment]::GetEnvironmentVariable("PROCESSOR_ARCHITECTURE")
switch ($arch) {
    "AMD64" { $ocArch = "amd64" }
    "ARM64" { $ocArch = "arm64" }
    default {
        Write-Host "Unsupported architecture: $arch" -ForegroundColor Red
        exit 1
    }
}

Write-Host "Detected architecture: $ocArch" -ForegroundColor Yellow

# Installation method selection
Write-Host ""
Write-Host "Select installation method:"
Write-Host "  1) winget (Windows Package Manager - recommended)"
Write-Host "  2) Chocolatey"
Write-Host "  3) Direct download from OKD/OpenShift"
Write-Host ""
$installMethod = Read-Host "Enter choice [1]"
if ([string]::IsNullOrEmpty($installMethod)) { $installMethod = "1" }

function Install-WithWinget {
    Write-Host "Installing via winget..." -ForegroundColor Yellow

    # Check if winget is available
    $winget = Get-Command winget -ErrorAction SilentlyContinue
    if (-not $winget) {
        Write-Host "winget is not installed." -ForegroundColor Red
        Write-Host "Install it from the Microsoft Store (App Installer) or use another method."
        exit 1
    }

    # Install OpenShift CLI
    winget install RedHat.OpenShift-Client --accept-source-agreements --accept-package-agreements

    Write-Host "OpenShift CLI installed successfully via winget" -ForegroundColor Green
}

function Install-WithChocolatey {
    Write-Host "Installing via Chocolatey..." -ForegroundColor Yellow

    # Check if Chocolatey is installed
    $choco = Get-Command choco -ErrorAction SilentlyContinue
    if (-not $choco) {
        Write-Host "Chocolatey is not installed." -ForegroundColor Red
        Write-Host "Install Chocolatey first: https://chocolatey.org/install"
        Write-Host "Or run (as Administrator):"
        Write-Host 'Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString("https://community.chocolatey.org/install.ps1"))'
        exit 1
    }

    # Install OpenShift CLI
    choco install openshift-cli -y

    Write-Host "OpenShift CLI installed successfully via Chocolatey" -ForegroundColor Green
}

function Install-DirectDownload {
    Write-Host "Installing via direct download..." -ForegroundColor Yellow

    # OKD version - update as needed
    $ocVersion = "4.14.0"

    # Create temp directory
    $tempDir = New-Item -ItemType Directory -Path "$env:TEMP\oc-install-$(Get-Random)" -Force

    # Download URL
    $downloadUrl = "https://github.com/okd-project/okd/releases/download/$ocVersion-0.okd-$ocVersion/openshift-client-windows-$ocVersion-0.okd-$ocVersion.zip"
    $zipPath = Join-Path $tempDir "oc.zip"

    Write-Host "Downloading from: $downloadUrl"

    try {
        # Try GitHub first
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $downloadUrl -OutFile $zipPath -UseBasicParsing
    }
    catch {
        Write-Host "GitHub download failed, trying mirror.openshift.com..." -ForegroundColor Yellow
        # Fallback to Red Hat's mirror
        $mirrorUrl = "https://mirror.openshift.com/pub/openshift-v4/clients/ocp/stable/openshift-client-windows.zip"
        Invoke-WebRequest -Uri $mirrorUrl -OutFile $zipPath -UseBasicParsing
    }

    # Extract
    Write-Host "Extracting..."
    Expand-Archive -Path $zipPath -DestinationPath $tempDir -Force

    # Create install directory
    if (-not (Test-Path $InstallPath)) {
        New-Item -ItemType Directory -Path $InstallPath -Force | Out-Null
    }

    # Move executables
    $ocExe = Get-ChildItem -Path $tempDir -Filter "oc.exe" -Recurse | Select-Object -First 1
    if ($ocExe) {
        Copy-Item -Path $ocExe.FullName -Destination $InstallPath -Force
        Write-Host "Installed oc.exe to $InstallPath" -ForegroundColor Green
    }

    $kubectlExe = Get-ChildItem -Path $tempDir -Filter "kubectl.exe" -Recurse | Select-Object -First 1
    if ($kubectlExe) {
        Copy-Item -Path $kubectlExe.FullName -Destination $InstallPath -Force
        Write-Host "Installed kubectl.exe to $InstallPath" -ForegroundColor Green
    }

    # Add to PATH
    $currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
    if ($currentPath -notlike "*$InstallPath*") {
        Write-Host "Adding $InstallPath to user PATH..." -ForegroundColor Yellow
        [Environment]::SetEnvironmentVariable("Path", "$currentPath;$InstallPath", "User")
        $env:Path = "$env:Path;$InstallPath"
        Write-Host "PATH updated. You may need to restart your terminal." -ForegroundColor Yellow
    }

    # Cleanup
    Remove-Item -Path $tempDir -Recurse -Force

    Write-Host "OpenShift CLI installed successfully to $InstallPath" -ForegroundColor Green
}

switch ($installMethod) {
    "1" { Install-WithWinget }
    "2" { Install-WithChocolatey }
    "3" { Install-DirectDownload }
    default {
        Write-Host "Invalid choice" -ForegroundColor Red
        exit 1
    }
}

# Verify installation (refresh PATH first)
Write-Host ""
Write-Host "Verifying installation..." -ForegroundColor Green

# Refresh environment
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

$ocCmd = Get-Command oc -ErrorAction SilentlyContinue
if ($ocCmd) {
    & oc version --client
    Write-Host ""
    Write-Host "Installation complete!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "  1. Log in to your OpenShift cluster:"
    Write-Host "     oc login <cluster-url>"
    Write-Host ""
    Write-Host "  2. Or get the login command from the OKD/OpenShift web console:"
    Write-Host "     - Click your username in the top right"
    Write-Host "     - Select 'Copy login command'"
    Write-Host ""
}
else {
    Write-Host "Installation verification failed. Please restart your terminal and try 'oc version'." -ForegroundColor Yellow
    Write-Host "If it still doesn't work, ensure $InstallPath is in your PATH." -ForegroundColor Yellow
}
