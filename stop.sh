#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  echo "[stop] WARN: docker compose/docker-compose not found. Skipping compose down."
  COMPOSE_CMD=()
fi

echo "[stop] Stopping docker compose services..."
if [[ ${#COMPOSE_CMD[@]} -gt 0 ]]; then
  "${COMPOSE_CMD[@]}" down
fi

# Fallback: if something still occupies common app ports, terminate it.
for port in 8080 5173; do
  pids="$(lsof -ti :"$port" 2>/dev/null || true)"
  if [[ -n "$pids" ]]; then
    echo "[stop] Port $port still in use. Killing PID(s): $pids"
    kill $pids || true
  fi
done

echo "[stop] Done."
