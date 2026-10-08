#!/usr/bin/env bash
# Shared helper: runs an AdminCLI command inside the deployed backend machine.
# Usage: run-cli.sh <cli-command> [args...]
set -euo pipefail

BACKEND_APP="${BACKEND_APP:-tampavolunteers-backend}"

if ! command -v fly &>/dev/null && ! command -v flyctl &>/dev/null; then
  echo "ERROR: Fly CLI (fly) is not installed." >&2
  exit 1
fi

if ! fly auth whoami &>/dev/null; then
  echo "ERROR: Not logged in to Fly.io. Run: fly auth login" >&2
  exit 1
fi

fly ssh console --app "$BACKEND_APP" -C \
  "java -Dspring.profiles.include=cli -Dspring.main.web-application-type=none -jar /app/app.jar $*"
