#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ "${1:-}" = "--all" ]; then
  "$SCRIPT_DIR/run-cli.sh" list-users --all
else
  "$SCRIPT_DIR/run-cli.sh" list-users
fi
