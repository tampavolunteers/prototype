#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: $0 <email>"
  exit 1
fi

EMAIL="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/../.."

docker compose -f "$PROJECT_ROOT/docker-compose.yml" --profile tools run --rm cli remove-super-admin "$EMAIL"
