#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "🚀 [STAGING DEPLOYMENT] Starting Staging Pipeline..."

# 1. Run local test suite first
echo "🧪 Running unit tests..."
if [ -f "$BACKEND_DIR/.venv/bin/pytest" ]; then
  PYTHONPATH="$BACKEND_DIR" "$BACKEND_DIR/.venv/bin/pytest" "$BACKEND_DIR/tests"
elif [ -f "/Users/appworx/Desktop/automation/venv/bin/pytest" ]; then
  PYTHONPATH="$BACKEND_DIR" /Users/appworx/Desktop/automation/venv/bin/pytest "$BACKEND_DIR/tests"
else
  PYTHONPATH="$BACKEND_DIR" pytest "$BACKEND_DIR/tests"
fi

# 2. Sync to VPS Staging directory
echo "📦 Syncing code to /opt/mandisamiti-staging on VPS..."
rsync -avz --delete \
  --exclude '__pycache__' \
  --exclude '.pytest_cache' \
  --exclude '*.db' \
  --exclude '.venv' \
  "$BACKEND_DIR/" appworx-core-vps:/opt/mandisamiti-staging/

# 3. Build & Recreate Staging Containers
echo "🐳 Rebuilding Staging containers on VPS..."
ssh appworx-core-vps "cd /opt/mandisamiti-staging && docker compose -f docker-compose.staging.yml up -d --build"

# 4. Run Smoke Tests against Staging
echo "🩺 Running live smoke tests on Staging..."
ssh appworx-core-vps "sleep 2 && bash /opt/mandisamiti-staging/scripts/smoke_test.sh http://127.0.0.1:8060"

echo "🎉 [STAGING SUCCESS] Staging deployment is complete and verified at http://64.227.142.180:8060"
