#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "🔄 Running Alembic database migrations..."
cd "$BACKEND_DIR"

run_alembic() {
  if [ -f "$BACKEND_DIR/.venv/bin/alembic" ]; then
    "$BACKEND_DIR/.venv/bin/alembic" "$@"
  elif [ -f "$BACKEND_DIR/venv/bin/alembic" ]; then
    "$BACKEND_DIR/venv/bin/alembic" "$@"
  elif command -v alembic >/dev/null 2>&1; then
    alembic "$@"
  else
    python3 -m alembic "$@"
  fi
}

# Run with retry in case database container is still booting
MAX_ATTEMPTS=15
ATTEMPT=1
until run_alembic upgrade head; do
  if [ $ATTEMPT -ge $MAX_ATTEMPTS ]; then
    echo "❌ Migration failed after $MAX_ATTEMPTS attempts."
    exit 1
  fi
  echo "⏳ Waiting for database to be ready (attempt $ATTEMPT/$MAX_ATTEMPTS)..."
  sleep 2
  ATTEMPT=$((ATTEMPT + 1))
done

echo "✅ Migrations applied successfully."

