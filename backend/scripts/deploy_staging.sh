#!/usr/bin/env bash
set -e

echo "🚀 [STAGING DEPLOYMENT] Starting Staging Pipeline..."

# 1. Run local test suite first
echo "🧪 Running unit tests..."
PYTHONPATH=/Users/appworx/Desktop/MandiSamiti/backend /Users/appworx/Desktop/automation/venv/bin/pytest /Users/appworx/Desktop/MandiSamiti/backend/tests

# 2. Sync to VPS Staging directory
echo "📦 Syncing code to /opt/mandisamiti-staging on VPS..."
rsync -avz --delete --exclude '__pycache__' --exclude '.pytest_cache' --exclude '*.db' /Users/appworx/Desktop/MandiSamiti/backend/ appworx-core-vps:/opt/mandisamiti-staging/

# 3. Build & Recreate Staging Containers
echo "🐳 Rebuilding Staging containers on VPS..."
ssh appworx-core-vps "cd /opt/mandisamiti-staging && docker compose -f docker-compose.staging.yml up -d --build"

# 4. Run Smoke Tests against Staging
echo "🩺 Running live smoke tests on Staging..."
ssh appworx-core-vps "sleep 2 && bash /opt/mandisamiti-staging/scripts/smoke_test.sh http://127.0.0.1:8060"

echo "🎉 [STAGING SUCCESS] Staging deployment is complete and verified at http://64.227.142.180:8060"
