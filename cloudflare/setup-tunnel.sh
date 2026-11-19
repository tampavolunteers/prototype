#!/bin/bash
# Cloudflare Tunnel setup and run script for the Tampa Volunteers dev site.
# Creates a locally-managed tunnel, routes DNS for the site + API hostnames,
# writes cloudflare/.env, and runs the docker compose stack.

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
CRED_DIR="$SCRIPT_DIR/.cloudflared"
CRED_FILE="$CRED_DIR/credentials.json"
COMPOSE=(docker compose -f "$SCRIPT_DIR/docker-compose.yml")

step() { echo ""; echo -e "${BLUE}>>> $1${NC}"; }
success() { echo -e "${GREEN}✓ $1${NC}"; }
warn() { echo -e "${YELLOW}⚠ $1${NC}"; }
error() { echo -e "${RED}✗ $1${NC}"; }

# Read KEY from .env (empty if unset).
get_env() {
    [[ -f "$ENV_FILE" ]] || return 0
    grep -E "^$1=" "$ENV_FILE" | tail -1 | cut -d= -f2- || true
}

# Set KEY=VALUE in .env, replacing an existing line or appending.
# Only used for generated values (hex/UUIDs), so no escaping needed.
set_env() {
    if grep -qE "^$1=" "$ENV_FILE"; then
        sed -i.bak "s|^$1=.*|$1=$2|" "$ENV_FILE" && rm -f "$ENV_FILE.bak"
    else
        echo "$1=$2" >> "$ENV_FILE"
    fi
}

load_settings() {
    TUNNEL_NAME="${TUNNEL_NAME:-$(get_env TUNNEL_NAME)}"
    TUNNEL_NAME="${TUNNEL_NAME:-tampavolunteers-dev}"
    SITE_HOSTNAME="${SITE_HOSTNAME:-$(get_env SITE_HOSTNAME)}"
    SITE_HOSTNAME="${SITE_HOSTNAME:-tampavolunteers.org}"
    API_HOSTNAME="${API_HOSTNAME:-$(get_env API_HOSTNAME)}"
    API_HOSTNAME="${API_HOSTNAME:-api.$SITE_HOSTNAME}"
}

check_prerequisites() {
    step "Checking prerequisites..."

    if ! command -v docker &> /dev/null || ! docker compose version &> /dev/null; then
        error "Docker with the compose plugin is required."
        echo "  See: https://docs.docker.com/engine/install/"
        exit 1
    fi
    success "Docker found: $(docker compose version | head -1)"

    if ! command -v cloudflared &> /dev/null; then
        error "cloudflared is not installed."
        echo ""
        echo "Install it using one of these methods:"
        echo "  macOS:         brew install cloudflared"
        echo "  Debian/Ubuntu: https://pkg.cloudflare.com/index.html"
        echo "  Windows:       winget install --id Cloudflare.cloudflared"
        echo "  Or see: https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/downloads/"
        exit 1
    fi
    success "cloudflared found: $(cloudflared --version 2>&1 | head -1)"

    if ! command -v openssl &> /dev/null; then
        error "openssl is required to generate secrets."
        exit 1
    fi
}

init_env() {
    step "Preparing $ENV_FILE..."

    if [[ ! -f "$ENV_FILE" ]]; then
        cp "$SCRIPT_DIR/.env.example" "$ENV_FILE"
        chmod 600 "$ENV_FILE"
        success "Created .env from .env.example"
    fi

    if [[ -z "$(get_env POSTGRES_PASSWORD)" ]]; then
        set_env POSTGRES_PASSWORD "$(openssl rand -hex 24)"
        success "Generated POSTGRES_PASSWORD"
    fi

    if [[ -z "$(get_env JWT_SECRET)" ]]; then
        set_env JWT_SECRET "$(openssl rand -hex 32)"
        success "Generated JWT_SECRET"
    fi

    if [[ -z "$(get_env GOOGLE_CLIENT_ID)" || -z "$(get_env GITHUB_CLIENT_ID)" ]]; then
        warn "OAuth2 credentials are blank - edit $ENV_FILE to enable Google/GitHub login (see README)"
    fi
}

login() {
    step "Checking Cloudflare login..."

    if [[ -f "${TUNNEL_ORIGIN_CERT:-$HOME/.cloudflared/cert.pem}" ]]; then
        success "Origin certificate found"
    else
        echo "A browser window will open - pick the $SITE_HOSTNAME zone to authorize."
        cloudflared tunnel login
        success "Logged in to Cloudflare"
    fi
}

lookup_tunnel_id() {
    cloudflared tunnel list --name "$TUNNEL_NAME" 2>/dev/null \
        | awk -v n="$TUNNEL_NAME" '$2 == n { print $1 }' \
        | grep -E '^[0-9a-f-]{36}$' | head -1 || true
}

create_tunnel() {
    step "Creating tunnel '$TUNNEL_NAME' (if it doesn't already exist)..."

    TUNNEL_ID="$(lookup_tunnel_id)"
    if [[ -n "$TUNNEL_ID" ]]; then
        warn "Tunnel '$TUNNEL_NAME' already exists ($TUNNEL_ID), reusing it"
    else
        cloudflared tunnel create "$TUNNEL_NAME"
        TUNNEL_ID="$(lookup_tunnel_id)"
        if [[ -z "$TUNNEL_ID" ]]; then
            error "Created tunnel but couldn't look up its ID. Check: cloudflared tunnel list"
            exit 1
        fi
        success "Created tunnel $TUNNEL_ID"
    fi
    set_env CLOUDFLARE_TUNNEL_ID "$TUNNEL_ID"

    # Write the tunnel credentials for the cloudflared container. Works for
    # tunnels created on another machine too.
    mkdir -p "$CRED_DIR"
    chmod 700 "$CRED_DIR"
    cloudflared tunnel token --cred-file "$CRED_FILE" "$TUNNEL_ID" > /dev/null
    # The cloudflared image runs as a non-root uid, so the bind-mounted file
    # must be world-readable. The 700 parent dir keeps other host users out.
    chmod 644 "$CRED_FILE"
    success "Wrote tunnel credentials to $CRED_FILE"
}

route_dns() {
    step "Routing DNS to the tunnel..."

    local tunnel_id
    tunnel_id="$(get_env CLOUDFLARE_TUNNEL_ID)"
    if [[ -z "$tunnel_id" ]]; then
        error "CLOUDFLARE_TUNNEL_ID is not set - run ./setup-tunnel.sh --tunnel first"
        exit 1
    fi

    for host in "$SITE_HOSTNAME" "$API_HOSTNAME"; do
        if cloudflared tunnel route dns "$tunnel_id" "$host"; then
            success "$host -> $tunnel_id.cfargotunnel.com"
        else
            warn "Could not route $host. If an A/AAAA/CNAME record already exists"
            warn "(e.g. imported from your registrar), delete it in the Cloudflare"
            warn "dashboard, or re-run with: cloudflared tunnel route dns --overwrite-dns $tunnel_id $host"
        fi
    done

    if ! grep -q "hostname: $SITE_HOSTNAME\$" "$SCRIPT_DIR/config.yml" \
        || ! grep -q "hostname: $API_HOSTNAME\$" "$SCRIPT_DIR/config.yml"; then
        warn "config.yml ingress hostnames don't match $SITE_HOSTNAME / $API_HOSTNAME - update it"
    fi
}

start_stack() {
    step "Building and starting the stack..."
    if [[ ! -f "$CRED_FILE" ]]; then
        error "Missing $CRED_FILE - run ./setup-tunnel.sh first"
        exit 1
    fi
    "${COMPOSE[@]}" up -d --build
    success "Stack started"
}

stop_stack() {
    step "Stopping the stack..."
    "${COMPOSE[@]}" down
    success "Stack stopped (database volume kept)"
}

show_status() {
    step "Status"
    "${COMPOSE[@]}" ps

    local tunnel_id
    tunnel_id="$(get_env CLOUDFLARE_TUNNEL_ID)"
    if [[ -n "$tunnel_id" ]] && command -v cloudflared &> /dev/null; then
        echo ""
        cloudflared tunnel info "$tunnel_id" || true
    fi

    echo ""
    echo -e "${GREEN}Application URLs:${NC}"
    echo "  Frontend: https://$SITE_HOSTNAME"
    echo "  Backend:  https://$API_HOSTNAME/api"
    local port
    port="$(get_env FRONTEND_PORT)"
    echo "  Local:    http://127.0.0.1:${port:-8081}"
}

main() {
    load_settings

    case "${1:-}" in
        --check)
            check_prerequisites
            ;;
        --env)
            init_env
            ;;
        --tunnel)
            check_prerequisites
            init_env
            login
            create_tunnel
            ;;
        --dns)
            check_prerequisites
            route_dns
            ;;
        --up)
            start_stack
            show_status
            ;;
        --down)
            stop_stack
            ;;
        --status)
            show_status
            ;;
        --logs)
            "${COMPOSE[@]}" logs -f --tail=100 "${@:2}"
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  (none)       Full setup: .env, login, tunnel, DNS, start stack"
            echo "  --check      Check prerequisites only"
            echo "  --env        Create .env and generate secrets only"
            echo "  --tunnel     Log in, create tunnel, write credentials only"
            echo "  --dns        Route DNS for site + API hostnames only"
            echo "  --up         Build and (re)start the stack (redeploy)"
            echo "  --down       Stop the stack (keeps the database volume)"
            echo "  --status     Show container + tunnel status"
            echo "  --logs [svc] Tail logs (all services, or e.g. backend)"
            echo "  --help       Show this help"
            echo ""
            echo "Settings (env vars, or in cloudflare/.env):"
            echo "  TUNNEL_NAME    Tunnel name (default: tampavolunteers-dev)"
            echo "  SITE_HOSTNAME  Public site hostname (default: tampavolunteers.org)"
            echo "  API_HOSTNAME   Public API hostname (default: api.\$SITE_HOSTNAME)"
            ;;
        "")
            check_prerequisites
            init_env
            login
            create_tunnel
            route_dns
            start_stack
            show_status
            ;;
        *)
            error "Unknown option: $1 (see --help)"
            exit 1
            ;;
    esac
}

main "$@"
