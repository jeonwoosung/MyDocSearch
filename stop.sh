#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
DB_DIR="$SCRIPT_DIR/db"
BACKUP_DIR="$DB_DIR/backups"

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

if [[ -d "$DB_DIR" ]] && find "$DB_DIR" -maxdepth 1 -type f | grep -q .; then
  mkdir -p "$BACKUP_DIR"
  BACKUP_FILE="$BACKUP_DIR/db-backup-$(date +%Y%m%d-%H%M%S).tar.gz"
  tar -czf "$BACKUP_FILE" --exclude './backups' -C "$DB_DIR" .
  echo "[stop] Database backup created: $BACKUP_FILE"

  mapfile -t old_backups < <(ls -1t "$BACKUP_DIR"/db-backup-*.tar.gz 2>/dev/null | tail -n +4)
  if [[ ${#old_backups[@]} -gt 0 ]]; then
    rm -f "${old_backups[@]}"
  fi
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
