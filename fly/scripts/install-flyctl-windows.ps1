# Fly CLI (flyctl) Installation Script for Windows

$flyCmd = Get-Command fly -ErrorAction SilentlyContinue
if (-not $flyCmd) { $flyCmd = Get-Command flyctl -ErrorAction SilentlyContinue }

if ($flyCmd) {
    Write-Host "Fly CLI is already installed:" -ForegroundColor Green
    & fly version
    $response = Read-Host "Do you want to reinstall/upgrade? (y/N)"
    if ($response -ne "y" -and $response -ne "Y") {
        Write-Host "Exiting without changes."
        exit 0
    }
}

Write-Host "Installing Fly CLI via official install script..." -ForegroundColor Yellow
iwr https://fly.io/install.ps1 -useb | iex

Write-Host ""
Write-Host "Installation complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. Log in: fly auth login"
Write-Host "  2. Run the setup script: .\fly\setup-fly.ps1"
