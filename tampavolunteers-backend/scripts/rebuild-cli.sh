#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/../.."

docker compose -f "$PROJECT_ROOT/docker-compose.yml" --profile tools build cli
echo "CLI image rebuilt."
