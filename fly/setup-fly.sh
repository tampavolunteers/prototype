#!/bin/bash
# Fly.io Project Setup and Deployment Script
# This script sets up the Tampa Volunteers platform on Fly.io

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

BACKEND_APP="${BACKEND_APP:-tampavolunteers-backend}"
FRONTEND_APP="${FRONTEND_APP:-tampavolunteers-frontend}"
DB_APP="${DB_APP:-tampavolunteers-db}"
REGION="${REGION:-iad}"
ORG="${ORG:-personal}"

echo -e "${GREEN}Tampa Volunteers - Fly.io Setup Script${NC}"
echo "========================================"

step() { echo ""; echo -e "${BLUE}>>> $1${NC}"; }
success() { echo -e "${GREEN}✓ $1${NC}"; }
warn() { echo -e "${YELLOW}⚠ $1${NC}"; }
error() { echo -e "${RED}✗ $1${NC}"; }

check_prerequisites() {
    step "Checking prerequisites..."

    if ! command -v flyctl &> /dev/null && ! command -v fly &> /dev/null; then
        error "Fly CLI (flyctl) is not installed."
        echo ""
        echo "Install it using one of these methods:"
        echo "  macOS/Linux: ./scripts/install-flyctl-macos.sh"
        echo "  Windows:     .\\scripts\\install-flyctl-windows.ps1"
        echo "  Or see: https://fly.io/docs/flyctl/install/"
        exit 1
    fi
    success "Fly CLI found: $(fly version 2>/dev/null | head -1)"

    if ! fly auth whoami &> /dev/null; then
        error "Not logged in to Fly.io."
        echo ""
        echo "Log in using: fly auth login"
        exit 1
    fi
    success "Logged in as: $(fly auth whoami)"
}

create_apps() {
    step "Creating Fly apps (if they don't already exist)..."

    for app in "$BACKEND_APP" "$FRONTEND_APP"; do
        if fly apps list | awk '{print $1}' | grep -qx "$app"; then
            warn "App '$app' already exists, skipping creation"
        else
            fly apps create "$app" --org "$ORG"
            success "Created app: $app"
        fi
    done
}

create_database() {
    step "Setting up Postgres..."

    if fly apps list | awk '{print $1}' | grep -qx "$DB_APP"; then
        warn "Postgres app '$DB_APP' already exists, skipping creation"
    else
        fly postgres create \
            --name "$DB_APP" \
            --org "$ORG" \
            --region "$REGION" \
            --initial-cluster-size 1 \
            --vm-size shared-cpu-1x \
            --volume-size 3
        success "Postgres cluster created: $DB_APP"
    fi

    step "Attaching Postgres to the backend app..."

    ATTACH_OUTPUT=$(fly postgres attach --app "$BACKEND_APP" "$DB_APP" 2>&1) || {
        warn "Attach may have already been done, or failed. Output:"
        echo "$ATTACH_OUTPUT"
        return
    }
    echo "$ATTACH_OUTPUT"

    DATABASE_URL=$(echo "$ATTACH_OUTPUT" | grep -oE 'postgres://[^ ]+' | head -1)

    if [[ -z "$DATABASE_URL" ]]; then
        warn "Could not automatically parse the Postgres connection string."
        read -rp "Paste the DATABASE_URL shown above: " DATABASE_URL
    fi

    # postgres://user:pass@host:5432/dbname?sslmode=disable
    CREDS_AND_HOST="${DATABASE_URL#postgres://}"
    USER_PASS="${CREDS_AND_HOST%%@*}"
    HOST_AND_DB="${CREDS_AND_HOST#*@}"
    PG_USER="${USER_PASS%%:*}"
    PG_PASSWORD="${USER_PASS#*:}"
    HOST_PORT="${HOST_AND_DB%%/*}"
    DB_AND_QUERY="${HOST_AND_DB#*/}"
    DB_NAME="${DB_AND_QUERY%%\?*}"

    JDBC_URL="jdbc:postgresql://${HOST_PORT}/${DB_NAME}?sslmode=disable"

    fly secrets set --app "$BACKEND_APP" \
        SPRING_DATASOURCE_URL="$JDBC_URL" \
        SPRING_DATASOURCE_USERNAME="$PG_USER" \
        SPRING_DATASOURCE_PASSWORD="$PG_PASSWORD"

    success "Backend wired to Postgres ($JDBC_URL)"
}

configure_secrets() {
    step "Configuring application secrets..."

    read -p "Generate a new secure JWT_SECRET now? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')
        fly secrets set --app "$BACKEND_APP" JWT_SECRET="$JWT_SECRET"
        success "JWT_SECRET set"
    else
        warn "Skipping JWT_SECRET - set it manually with: fly secrets set --app $BACKEND_APP JWT_SECRET=..."
    fi

    echo ""
    echo "OAuth2 credentials (leave blank to configure later):"
    read -p "  GOOGLE_CLIENT_ID: " GOOGLE_CLIENT_ID
    read -p "  GOOGLE_CLIENT_SECRET: " GOOGLE_CLIENT_SECRET
    read -p "  GITHUB_CLIENT_ID: " GITHUB_CLIENT_ID
    read -p "  GITHUB_CLIENT_SECRET: " GITHUB_CLIENT_SECRET

    if [[ -n "$GOOGLE_CLIENT_ID" || -n "$GITHUB_CLIENT_ID" ]]; then
        fly secrets set --app "$BACKEND_APP" \
            ${GOOGLE_CLIENT_ID:+GOOGLE_CLIENT_ID="$GOOGLE_CLIENT_ID"} \
            ${GOOGLE_CLIENT_SECRET:+GOOGLE_CLIENT_SECRET="$GOOGLE_CLIENT_SECRET"} \
            ${GITHUB_CLIENT_ID:+GITHUB_CLIENT_ID="$GITHUB_CLIENT_ID"} \
            ${GITHUB_CLIENT_SECRET:+GITHUB_CLIENT_SECRET="$GITHUB_CLIENT_SECRET"}
        success "OAuth2 credentials set"
    else
        warn "Skipping OAuth2 credentials - set them later with: fly secrets set --app $BACKEND_APP GOOGLE_CLIENT_ID=... GOOGLE_CLIENT_SECRET=... GITHUB_CLIENT_ID=... GITHUB_CLIENT_SECRET=..."
    fi
}

deploy_application() {
    step "Deploying backend..."
    (cd "$ROOT_DIR/tampavolunteers-backend" && fly deploy --app "$BACKEND_APP")
    success "Backend deployed"

    step "Deploying frontend..."
    (cd "$ROOT_DIR/tampavolunteers-frontend" && fly deploy --app "$FRONTEND_APP")
    success "Frontend deployed"
}

show_status() {
    step "Deployment Status"

    echo ""
    echo "Backend:"
    fly status --app "$BACKEND_APP"

    echo ""
    echo "Frontend:"
    fly status --app "$FRONTEND_APP"

    echo ""
    echo -e "${GREEN}Application URLs:${NC}"
    echo "  Frontend: https://${FRONTEND_APP}.fly.dev"
    echo "  Backend:  https://${BACKEND_APP}.fly.dev/api"
}

main() {
    case "${1:-}" in
        --check)
            check_prerequisites
            ;;
        --apps-only)
            check_prerequisites
            create_apps
            ;;
        --db-only)
            check_prerequisites
            create_database
            ;;
        --secrets-only)
            check_prerequisites
            configure_secrets
            ;;
        --deploy)
            check_prerequisites
            deploy_application
            show_status
            ;;
        --status)
            show_status
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  (none)         Full setup: apps, database, secrets, deploy"
            echo "  --check        Check prerequisites only"
            echo "  --apps-only    Create Fly apps only"
            echo "  --db-only      Create/attach Postgres only"
            echo "  --secrets-only Configure application secrets only"
            echo "  --deploy       Deploy backend + frontend only"
            echo "  --status       Show deployment status"
            echo "  --help         Show this help"
            echo ""
            echo "Environment variables:"
            echo "  BACKEND_APP    Fly app name for backend (default: tampavolunteers-backend)"
            echo "  FRONTEND_APP   Fly app name for frontend (default: tampavolunteers-frontend)"
            echo "  DB_APP         Fly app name for Postgres (default: tampavolunteers-db)"
            echo "  REGION         Fly region (default: iad)"
            echo "  ORG            Fly org slug (default: personal)"
            ;;
        *)
            check_prerequisites
            create_apps
            create_database
            configure_secrets
            deploy_application
            show_status
            ;;
    esac
}

main "$@"
