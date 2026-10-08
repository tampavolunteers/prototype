#!/usr/bin/env bash
# Remove seeded data from the deployed Fly.io environment.
# Usage: ./remove-seeded.sh [--volunteers] [--organizations] [--opportunities]
# Default (no flags): removes all three.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
"$SCRIPT_DIR/run-cli.sh" remove-seeded "$@"
