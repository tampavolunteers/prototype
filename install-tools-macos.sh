#!/usr/bin/env bash
# Install local dev dependencies for Tampa Volunteers (macOS)
set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()    { printf "${GREEN}[INFO]${NC} %s\n" "$*"; }
warning() { printf "${YELLOW}[WARN]${NC} %s\n" "$*"; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/tampavolunteers-backend"

# ── Homebrew ─────────────────────────────────────────────────────────────────
if ! command -v brew &>/dev/null; then
  info "Installing Homebrew..."
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
  # Apple Silicon: add brew to current shell session
  if [[ -f /opt/homebrew/bin/brew ]]; then
    eval "$(/opt/homebrew/bin/brew shellenv)"
  fi
else
  info "Homebrew: $(brew --version | head -1)"
fi

# ── Java 17 ──────────────────────────────────────────────────────────────────
if brew list --cask temurin@17 &>/dev/null; then
  info "Java 17 (Temurin): already installed"
else
  info "Installing Java 17 (Temurin)..."
  brew install --cask temurin@17
fi

# ── Maven ─────────────────────────────────────────────────────────────────────
if command -v mvn &>/dev/null; then
  info "Maven: $(mvn --version 2>&1 | head -1)"
else
  info "Installing Maven..."
  brew install maven
fi

# ── Docker Desktop ────────────────────────────────────────────────────────────
if brew list --cask docker &>/dev/null; then
  info "Docker Desktop: already installed"
elif command -v docker &>/dev/null; then
  info "Docker: already installed (not via Homebrew)"
else
  info "Installing Docker Desktop..."
  brew install --cask docker
  warning "Open Docker Desktop from Applications to complete initial setup before running containers."
fi

# ── Node.js ───────────────────────────────────────────────────────────────────
if command -v node &>/dev/null; then
  info "Node.js: $(node --version)"
else
  info "Installing Node.js..."
  brew install node
fi

# ── Maven Wrapper ─────────────────────────────────────────────────────────────
if [[ -f "$BACKEND_DIR/mvnw" ]]; then
  info "Maven wrapper (mvnw): already present"
else
  info "Generating Maven wrapper in tampavolunteers-backend/..."
  (cd "$BACKEND_DIR" && mvn wrapper:wrapper)
  chmod +x "$BACKEND_DIR/mvnw"
fi

# ── Script permissions ────────────────────────────────────────────────────────
info "Ensuring backend scripts are executable..."
chmod +x "$BACKEND_DIR"/scripts/*.sh

info ""
info "All done! Restart your terminal (or run: source ~/.zshrc) if PATH changes aren't picked up."
