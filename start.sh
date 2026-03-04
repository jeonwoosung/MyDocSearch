#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  echo "[start] ERROR: docker compose/docker-compose not found."
  exit 1
fi

echo "[start] Building and starting docker compose services..."
"${COMPOSE_CMD[@]}" up -d --build

echo "[start] Done."
echo "[start] Frontend: http://localhost:5173"
echo "[start] Backend : http://localhost:8080"
