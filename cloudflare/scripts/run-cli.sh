#!/usr/bin/env bash
# Runs an AdminCLI command against the Cloudflare Tunnel stack's database.
# Usage: run-cli.sh <cli-command> [args...]
#   e.g. run-cli.sh add-super-admin you@example.com
#        run-cli.sh list-users --all
#        run-cli.sh seed-volunteers 25
# See ../../fly/README.md "Database Admin CLI" for the command list.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ $# -lt 1 ]; then
  echo "Usage: $0 <cli-command> [args...]" >&2
  exit 1
fi

docker compose -f "$SCRIPT_DIR/../docker-compose.yml" --profile tools \
  run --rm cli "$@"
