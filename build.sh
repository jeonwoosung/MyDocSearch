#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "[build] Building backend..."
(cd backend && mvn -q -DskipTests package)

echo "[build] Building frontend..."
(cd frontend && npm run build)

echo "[build] Done."
