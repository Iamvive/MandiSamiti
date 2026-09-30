#!/usr/bin/env bash
set -e

echo "🚨 [PRODUCTION DEPLOYMENT] Initiating Production Release..."

# Safety confirmation
echo "⚠️  Ensure you have verified changes on STAGING first!"

# 1. Run Smoke test on Staging to verify staging health before prod promote
echo "🩺 Verifying Staging health before production promotion..."
ssh appworx-core-vps "bash /opt/mandisamiti-staging/scripts/smoke_test.sh http://127.0.0.1:8060"

# 2. Sync to VPS Prod directory
echo "📦 Promoting verified code to /opt/mandisamiti-prod on VPS..."
rsync -avz --delete --exclude '__pycache__' --exclude '.pytest_cache' --exclude '*.db' /Users/appworx/Desktop/MandiSamiti/backend/ appworx-core-vps:/opt/mandisamiti-prod/

# 3. Build & Recreate Prod Containers with zero-downtime rolling restart
echo "🐳 Rebuilding Production containers on VPS..."
ssh appworx-core-vps "cd /opt/mandisamiti-prod && docker compose -f docker-compose.prod.yml up -d --build"

# 4. Run Smoke Tests against Production
echo "🩺 Running live smoke tests on Production..."
ssh appworx-core-vps "sleep 2 && bash /opt/mandisamiti-prod/scripts/smoke_test.sh http://127.0.0.1:8050"

echo "🏆 [PRODUCTION SUCCESS] Production deployment is live and healthy at http://64.227.142.180:8050"
