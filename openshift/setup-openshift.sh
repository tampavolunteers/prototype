#!/bin/bash
# OpenShift Project Setup and Deployment Script
# This script sets up the Tampa Volunteers platform on OpenShift/OKD

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_NAME="${PROJECT_NAME:-tampavolunteers}"

echo -e "${GREEN}Tampa Volunteers - OpenShift Setup Script${NC}"
echo "=========================================="

# Function to print step headers
step() {
    echo ""
    echo -e "${BLUE}>>> $1${NC}"
}

# Function to print success
success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Function to print warning
warn() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Function to print error
error() {
    echo -e "${RED}✗ $1${NC}"
}

# Check prerequisites
check_prerequisites() {
    step "Checking prerequisites..."

    # Check for oc CLI
    if ! command -v oc &> /dev/null; then
        error "OpenShift CLI (oc) is not installed."
        echo ""
        echo "Install it using one of these methods:"
        echo "  macOS:   ./scripts/install-oc-macos.sh"
        echo "  Windows: ./scripts/install-oc-windows.ps1"
        echo "  Or download from: https://mirror.openshift.com/pub/openshift-v4/clients/ocp/stable/"
        exit 1
    fi
    success "OpenShift CLI found: $(oc version --client 2>/dev/null | head -1)"

    # Check if logged in
    if ! oc whoami &> /dev/null; then
        error "Not logged in to OpenShift cluster."
        echo ""
        echo "Log in using:"
        echo "  oc login <cluster-url>"
        echo ""
        echo "Or get the login command from the OpenShift web console:"
        echo "  1. Click your username in the top right"
        echo "  2. Select 'Copy login command'"
        exit 1
    fi
    success "Logged in as: $(oc whoami)"
    success "Server: $(oc whoami --show-server)"
}

# Create or switch to project
setup_project() {
    step "Setting up project: $PROJECT_NAME"

    if oc get project "$PROJECT_NAME" &> /dev/null; then
        warn "Project '$PROJECT_NAME' already exists, switching to it..."
        oc project "$PROJECT_NAME"
    else
        echo "Creating new project: $PROJECT_NAME"
        oc new-project "$PROJECT_NAME" \
            --display-name="Tampa Volunteers" \
            --description="Tampa Volunteers Platform - Connecting volunteers with opportunities"
    fi
    success "Using project: $PROJECT_NAME"
}

# Update secrets with secure values
configure_secrets() {
    step "Configuring secrets..."

    echo ""
    warn "IMPORTANT: Update the secrets in openshift/base/secret.yaml before deploying to production!"
    echo ""
    read -p "Do you want to generate new secure secrets now? (y/N): " -n 1 -r
    echo

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        # Generate secure JWT secret
        JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')
        # Generate secure DB password
        DB_PASSWORD=$(openssl rand -base64 24 | tr -d '\n' | tr -dc 'a-zA-Z0-9')

        echo ""
        echo -e "${YELLOW}Generated secrets (save these securely):${NC}"
        echo "  JWT_SECRET: $JWT_SECRET"
        echo "  DB_PASSWORD: $DB_PASSWORD"
        echo ""

        # Update secret.yaml with generated values
        sed -i.bak \
            -e "s|JWT_SECRET:.*|JWT_SECRET: \"$JWT_SECRET\"|" \
            -e "s|POSTGRES_PASSWORD:.*|POSTGRES_PASSWORD: \"$DB_PASSWORD\"|" \
            -e "s|SPRING_DATASOURCE_PASSWORD:.*|SPRING_DATASOURCE_PASSWORD: \"$DB_PASSWORD\"|" \
            "$SCRIPT_DIR/base/secret.yaml"
        rm -f "$SCRIPT_DIR/base/secret.yaml.bak"

        success "Secrets updated in secret.yaml"
    else
        warn "Using default secrets - CHANGE BEFORE PRODUCTION!"
    fi
}

# Configure GHCR pull secret for pulling images from GitHub Container Registry
configure_ghcr_secret() {
    step "Configuring GitHub Container Registry pull secret..."

    echo ""
    echo "Images are pulled from ghcr.io/tampavolunteers/prototype"
    echo "You need a GitHub Personal Access Token (PAT) with 'read:packages' scope."
    echo ""
    echo "To create a PAT:"
    echo "  1. Go to https://github.com/settings/tokens"
    echo "  2. Generate new token (classic)"
    echo "  3. Select 'read:packages' scope"
    echo "  4. Copy the token"
    echo ""

    # Check if secret already exists
    if oc get secret ghcr-pull-secret -n "$PROJECT_NAME" &> /dev/null; then
        read -p "GHCR pull secret already exists. Do you want to update it? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            success "Using existing GHCR pull secret"
            return
        fi
        oc delete secret ghcr-pull-secret -n "$PROJECT_NAME"
    fi

    read -p "Enter your GitHub username: " GITHUB_USER
    read -sp "Enter your GitHub PAT: " GITHUB_PAT
    echo ""
    read -p "Enter your email (for registry): " GITHUB_EMAIL

    if [[ -z "$GITHUB_USER" || -z "$GITHUB_PAT" ]]; then
        error "GitHub username and PAT are required"
        echo "You can create the secret manually later with:"
        echo "  oc create secret docker-registry ghcr-pull-secret \\"
        echo "    --docker-server=ghcr.io \\"
        echo "    --docker-username=<username> \\"
        echo "    --docker-password=<pat> \\"
        echo "    --docker-email=<email>"
        return 1
    fi

    oc create secret docker-registry ghcr-pull-secret \
        --docker-server=ghcr.io \
        --docker-username="$GITHUB_USER" \
        --docker-password="$GITHUB_PAT" \
        --docker-email="${GITHUB_EMAIL:-$GITHUB_USER@users.noreply.github.com}" \
        -n "$PROJECT_NAME"

    success "GHCR pull secret created"
}

# Apply Kubernetes resources
apply_resources() {
    step "Applying Kubernetes resources..."

    # Apply in order: namespace, configmap, secrets, pvc, then services
    echo "Applying namespace..."
    oc apply -f "$SCRIPT_DIR/base/namespace.yaml" 2>/dev/null || true

    echo "Applying ConfigMap..."
    oc apply -f "$SCRIPT_DIR/base/configmap.yaml"

    echo "Applying Secrets..."
    oc apply -f "$SCRIPT_DIR/base/secret.yaml"

    # Note: GHCR pull secret is created via configure_ghcr_secret() function
    # which uses 'oc create secret docker-registry' for proper encoding

    echo "Applying PostgreSQL PVC..."
    oc apply -f "$SCRIPT_DIR/base/postgres-pvc.yaml"

    echo "Applying PostgreSQL Deployment and Service..."
    oc apply -f "$SCRIPT_DIR/base/postgres-deployment.yaml"
    oc apply -f "$SCRIPT_DIR/base/postgres-service.yaml"

    success "Core resources applied"
}

# Note: Image builds now happen via GitHub Actions and are pushed to ghcr.io
# The setup_builds and start_builds functions have been removed.
# Images are pulled from ghcr.io/tampavolunteers/prototype/backend and /frontend

# Deploy application
deploy_application() {
    step "Deploying application components..."

    echo "Deploying Backend..."
    oc apply -f "$SCRIPT_DIR/base/backend-deployment.yaml"
    oc apply -f "$SCRIPT_DIR/base/backend-service.yaml"
    oc apply -f "$SCRIPT_DIR/base/backend-route.yaml"

    echo "Deploying Frontend..."
    oc apply -f "$SCRIPT_DIR/base/frontend-deployment.yaml"
    oc apply -f "$SCRIPT_DIR/base/frontend-service.yaml"
    oc apply -f "$SCRIPT_DIR/base/frontend-route.yaml"

    success "Application deployed"
}

# Show status and URLs
show_status() {
    step "Deployment Status"

    echo ""
    echo "Pods:"
    oc get pods -n "$PROJECT_NAME"

    echo ""
    echo "Services:"
    oc get services -n "$PROJECT_NAME"

    echo ""
    echo "Routes:"
    oc get routes -n "$PROJECT_NAME"

    echo ""
    echo -e "${GREEN}Application URLs:${NC}"
    FRONTEND_URL=$(oc get route frontend -n "$PROJECT_NAME" -o jsonpath='{.spec.host}' 2>/dev/null || echo "pending")
    BACKEND_URL=$(oc get route backend -n "$PROJECT_NAME" -o jsonpath='{.spec.host}' 2>/dev/null || echo "pending")

    echo "  Frontend: https://$FRONTEND_URL"
    echo "  Backend:  https://$BACKEND_URL/api"

    echo ""
    echo -e "${YELLOW}Note: It may take a few minutes for pods to become ready.${NC}"
    echo "Monitor with: oc get pods -w"
}

# Main execution
main() {
    case "${1:-}" in
        --check)
            check_prerequisites
            ;;
        --project-only)
            check_prerequisites
            setup_project
            ;;
        --ghcr-secret)
            check_prerequisites
            oc project "$PROJECT_NAME" 2>/dev/null || setup_project
            configure_ghcr_secret
            ;;
        --deploy)
            check_prerequisites
            deploy_application
            show_status
            ;;
        --status)
            oc project "$PROJECT_NAME" 2>/dev/null || true
            show_status
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  (none)          Full setup: project, secrets, GHCR auth, deploy"
            echo "  --check         Check prerequisites only"
            echo "  --project-only  Create/switch to project only"
            echo "  --ghcr-secret   Configure GHCR pull secret only"
            echo "  --deploy        Deploy application only"
            echo "  --status        Show deployment status"
            echo "  --help          Show this help"
            echo ""
            echo "Environment variables:"
            echo "  PROJECT_NAME    OpenShift project name (default: tampavolunteers)"
            echo ""
            echo "Note: Images are built via GitHub Actions and stored in ghcr.io"
            echo "      You need a GitHub PAT with 'read:packages' scope for GHCR access"
            ;;
        *)
            check_prerequisites
            setup_project
            configure_secrets
            configure_ghcr_secret
            apply_resources
            deploy_application
            show_status
            ;;
    esac
}

main "$@"
