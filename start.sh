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
# For older docker-compose v1 environments, force legacy build path.
# This avoids passing unsupported flags like --iidfile to old docker clients.
if [[ "${COMPOSE_CMD[0]}" == "docker-compose" ]]; then
  COMPOSE_DOCKER_CLI_BUILD=0 DOCKER_BUILDKIT=0 "${COMPOSE_CMD[@]}" up -d --build
else
  "${COMPOSE_CMD[@]}" up -d --build
fi

echo "[start] Done."
echo "[start] Frontend: http://localhost:5173"
echo "[start] Backend : http://localhost:8080"
