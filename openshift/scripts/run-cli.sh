#!/usr/bin/env bash
# Shared helper: runs an AdminCLI command inside the deployed backend pod.
# Usage: run-cli.sh <cli-command> [args...]
set -euo pipefail

PROJECT_NAME="${PROJECT_NAME:-tampavolunteers}"

if ! command -v oc &>/dev/null; then
  echo "ERROR: OpenShift CLI (oc) is not installed." >&2
  exit 1
fi

if ! oc whoami &>/dev/null; then
  echo "ERROR: Not logged in to OpenShift. Run: oc login <cluster-url>" >&2
  exit 1
fi

oc project "$PROJECT_NAME" &>/dev/null

oc exec deployment/backend -n "$PROJECT_NAME" -- \
  java -Dspring.profiles.include=cli \
       -Dspring.main.web-application-type=none \
       -jar /app/app.jar "$@"
