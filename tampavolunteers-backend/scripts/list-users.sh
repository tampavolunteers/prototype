#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/../.."

ARGS="list-users"
if [ "${1:-}" = "--all" ]; then
  ARGS="list-users --all"
fi

docker compose -f "$PROJECT_ROOT/docker-compose.yml" --profile tools run --rm cli $ARGS
