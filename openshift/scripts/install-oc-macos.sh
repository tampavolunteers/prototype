#!/bin/bash
# OpenShift CLI (oc) Installation Script for macOS
# This script installs the OpenShift CLI using Homebrew or direct download

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}OpenShift CLI (oc) Installation Script for macOS${NC}"
echo "=================================================="

# Check if oc is already installed
if command -v oc &> /dev/null; then
    echo -e "${GREEN}OpenShift CLI is already installed:${NC}"
    oc version --client
    echo ""
    read -p "Do you want to reinstall/upgrade? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Exiting without changes."
        exit 0
    fi
fi

# Detect architecture
ARCH=$(uname -m)
case $ARCH in
    x86_64)
        OC_ARCH="x86_64"
        ;;
    arm64)
        OC_ARCH="arm64"
        ;;
    *)
        echo -e "${RED}Unsupported architecture: $ARCH${NC}"
        exit 1
        ;;
esac

echo -e "${YELLOW}Detected architecture: $OC_ARCH${NC}"

# Installation method selection
echo ""
echo "Select installation method:"
echo "  1) Homebrew (recommended)"
echo "  2) Direct download from OKD/OpenShift"
echo ""
read -p "Enter choice [1]: " INSTALL_METHOD
INSTALL_METHOD=${INSTALL_METHOD:-1}

install_with_homebrew() {
    echo -e "${YELLOW}Installing via Homebrew...${NC}"

    # Check if Homebrew is installed
    if ! command -v brew &> /dev/null; then
        echo -e "${RED}Homebrew is not installed.${NC}"
        echo "Install Homebrew first: https://brew.sh"
        echo "Or run: /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
        exit 1
    fi

    # Install openshift-cli
    brew install openshift-cli

    echo -e "${GREEN}OpenShift CLI installed successfully via Homebrew${NC}"
}

install_direct_download() {
    echo -e "${YELLOW}Installing via direct download...${NC}"

    # OKD/OpenShift version - update as needed
    OC_VERSION="4.14.0"

    # Create temp directory
    TEMP_DIR=$(mktemp -d)
    cd "$TEMP_DIR"

    # Download URL for OKD (community distribution)
    if [[ "$OC_ARCH" == "arm64" ]]; then
        DOWNLOAD_URL="https://github.com/okd-project/okd/releases/download/${OC_VERSION}-0.okd-${OC_VERSION}/openshift-client-mac-arm64-${OC_VERSION}-0.okd-${OC_VERSION}.tar.gz"
    else
        DOWNLOAD_URL="https://github.com/okd-project/okd/releases/download/${OC_VERSION}-0.okd-${OC_VERSION}/openshift-client-mac-${OC_VERSION}-0.okd-${OC_VERSION}.tar.gz"
    fi

    echo "Downloading from: $DOWNLOAD_URL"

    # Try to download, fallback to mirror.openshift.com if github fails
    if ! curl -L -o oc.tar.gz "$DOWNLOAD_URL" 2>/dev/null; then
        echo -e "${YELLOW}GitHub download failed, trying mirror.openshift.com...${NC}"
        # Fallback to Red Hat's mirror for OpenShift (requires subscription for some versions)
        MIRROR_URL="https://mirror.openshift.com/pub/openshift-v4/clients/ocp/stable/openshift-client-mac.tar.gz"
        if [[ "$OC_ARCH" == "arm64" ]]; then
            MIRROR_URL="https://mirror.openshift.com/pub/openshift-v4/clients/ocp/stable/openshift-client-mac-arm64.tar.gz"
        fi
        curl -L -o oc.tar.gz "$MIRROR_URL"
    fi

    # Extract
    echo "Extracting..."
    tar -xzf oc.tar.gz

    # Install to /usr/local/bin
    INSTALL_DIR="/usr/local/bin"

    if [[ ! -w "$INSTALL_DIR" ]]; then
        echo -e "${YELLOW}Need sudo to install to $INSTALL_DIR${NC}"
        sudo mv oc "$INSTALL_DIR/"
        sudo chmod +x "$INSTALL_DIR/oc"
    else
        mv oc "$INSTALL_DIR/"
        chmod +x "$INSTALL_DIR/oc"
    fi

    # Also install kubectl if present
    if [[ -f "kubectl" ]]; then
        if [[ ! -w "$INSTALL_DIR" ]]; then
            sudo mv kubectl "$INSTALL_DIR/"
            sudo chmod +x "$INSTALL_DIR/kubectl"
        else
            mv kubectl "$INSTALL_DIR/"
            chmod +x "$INSTALL_DIR/kubectl"
        fi
        echo -e "${GREEN}kubectl also installed${NC}"
    fi

    # Cleanup
    cd -
    rm -rf "$TEMP_DIR"

    echo -e "${GREEN}OpenShift CLI installed successfully to $INSTALL_DIR${NC}"
}

case $INSTALL_METHOD in
    1)
        install_with_homebrew
        ;;
    2)
        install_direct_download
        ;;
    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac

# Verify installation
echo ""
echo -e "${GREEN}Verifying installation...${NC}"
if command -v oc &> /dev/null; then
    oc version --client
    echo ""
    echo -e "${GREEN}Installation complete!${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Log in to your OpenShift cluster:"
    echo "     oc login <cluster-url>"
    echo ""
    echo "  2. Or get the login command from the OKD/OpenShift web console:"
    echo "     - Click your username in the top right"
    echo "     - Select 'Copy login command'"
    echo ""
else
    echo -e "${RED}Installation verification failed. Please check your PATH.${NC}"
    exit 1
fi
