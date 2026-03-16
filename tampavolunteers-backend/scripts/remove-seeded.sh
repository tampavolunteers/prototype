#!/usr/bin/env bash
# Remove seeded data from the local dev environment.
# Usage: ./remove-seeded.sh [--volunteers] [--organizations] [--opportunities]
# Default (no flags): removes all three.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/../.."

docker compose -f "$PROJECT_ROOT/docker-compose.yml" --profile tools run --rm cli remove-seeded "$@"
