#!/usr/bin/env bash
set -e

TARGET_URL=${1:-"http://127.0.0.1:8060"}
echo "🔍 Running smoke tests against $TARGET_URL..."

HEALTH_RES=""
for i in {1..8}; do
  HEALTH_RES=$(curl -s "$TARGET_URL/health" || true)
  if [[ "$HEALTH_RES" == *"healthy"* ]]; then
    echo "Health check succeeded on attempt $i: $HEALTH_RES"
    break
  fi
  echo "Waiting for API to be ready (attempt $i/8)..."
  sleep 2
done

if [[ "$HEALTH_RES" != *"healthy"* ]]; then
  echo "❌ Health check failed after retries!"
  exit 1
fi

OTP_RES=$(curl -s -X POST "$TARGET_URL/api/v1/auth/otp/send" -H "Content-Type: application/json" -d '{"phone": "9876543210"}')
echo "OTP Send Response: $OTP_RES"
if [[ "$OTP_RES" != *"sent"* ]]; then
  echo "❌ OTP send check failed!"
  exit 1
fi

echo "✅ All smoke tests passed successfully on $TARGET_URL!"
