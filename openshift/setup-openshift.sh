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

    echo "Applying PostgreSQL PVC..."
    oc apply -f "$SCRIPT_DIR/base/postgres-pvc.yaml"

    echo "Applying PostgreSQL Deployment and Service..."
    oc apply -f "$SCRIPT_DIR/base/postgres-deployment.yaml"
    oc apply -f "$SCRIPT_DIR/base/postgres-service.yaml"

    success "Core resources applied"
}

# Setup image builds
setup_builds() {
    step "Setting up image builds..."

    echo "Creating Backend ImageStream and BuildConfig..."
    oc apply -f "$SCRIPT_DIR/base/backend-buildconfig.yaml"

    echo "Creating Frontend ImageStream and BuildConfig..."
    oc apply -f "$SCRIPT_DIR/base/frontend-buildconfig.yaml"

    success "Build configurations created"
}

# Start builds
start_builds() {
    step "Starting image builds..."

    read -p "Do you want to start the builds now? (Y/n): " -n 1 -r
    echo

    if [[ ! $REPLY =~ ^[Nn]$ ]]; then
        echo "Starting backend build..."
        oc start-build backend --follow=false

        echo "Starting frontend build..."
        oc start-build frontend --follow=false

        echo ""
        echo "Builds started in background. Monitor with:"
        echo "  oc logs -f bc/backend"
        echo "  oc logs -f bc/frontend"
        echo ""
        echo "Or view in the OpenShift web console."
    else
        echo "Skipping builds. Start manually with:"
        echo "  oc start-build backend"
        echo "  oc start-build frontend"
    fi
}

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
        --build)
            check_prerequisites
            setup_builds
            start_builds
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
            echo "  (none)          Full setup: project, secrets, builds, deploy"
            echo "  --check         Check prerequisites only"
            echo "  --project-only  Create/switch to project only"
            echo "  --build         Setup and start builds only"
            echo "  --deploy        Deploy application only"
            echo "  --status        Show deployment status"
            echo "  --help          Show this help"
            echo ""
            echo "Environment variables:"
            echo "  PROJECT_NAME    OpenShift project name (default: tampavolunteers)"
            ;;
        *)
            check_prerequisites
            setup_project
            configure_secrets
            apply_resources
            setup_builds
            start_builds
            deploy_application
            show_status
            ;;
    esac
}

main "$@"
