#!/bin/bash
# Fly CLI (flyctl) Installation Script for macOS/Linux

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Fly CLI (flyctl) Installation Script${NC}"
echo "======================================"

if command -v fly &> /dev/null; then
    echo -e "${GREEN}Fly CLI is already installed:${NC}"
    fly version
    echo ""
    read -p "Do you want to reinstall/upgrade? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Exiting without changes."
        exit 0
    fi
fi

if command -v brew &> /dev/null; then
    echo -e "${YELLOW}Installing via Homebrew...${NC}"
    brew install flyctl
else
    echo -e "${YELLOW}Installing via official install script...${NC}"
    curl -L https://fly.io/install.sh | sh
    echo ""
    echo "Add flyctl to your PATH (add this to your shell profile):"
    echo '  export FLYCTL_INSTALL="$HOME/.fly"'
    echo '  export PATH="$FLYCTL_INSTALL/bin:$PATH"'
fi

echo ""
echo -e "${GREEN}Installation complete!${NC}"
echo ""
echo "Next steps:"
echo "  1. Log in: fly auth login"
echo "  2. Run the setup script: ./fly/setup-fly.sh"
