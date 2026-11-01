#!/usr/bin/env bash
# ==============================================================================
# scripts/phase3_emulator.sh
# MandiSamiti Phase 3 Emulator & Scratch Backend Orchestrator
#
# Rules:
# - Target device MUST be an emulator (never hardware / Redmi d93c743).
# - Start AVD 'medium_phone' with -no-audio.
# - Scratch SQLite database with Base.metadata.create_all.
# - Random OTP_STATIC_CODE and SECRET_KEY, never printed to stdout/stderr.
# - Blank SENTRY_DSN and OPS_TELEGRAM_*.
# - Explicit adb install -r to emulator-5554 (never ./gradlew installDebug).
# - Fresh install reaches the Dashboard.
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$REPO_ROOT/backend"

TARGET_SERIAL="${TARGET_SERIAL:-emulator-5554}"
AVD_NAME="${AVD_NAME:-medium_phone}"
BACKEND_PORT="${BACKEND_PORT:-8000}"

# ------------------------------------------------------------------------------
# 1. Hardware Guardrail: Refuse to run on any non-emulator target
# ------------------------------------------------------------------------------
if [[ "$TARGET_SERIAL" =~ ^d93c743 ]] || [[ ! "$TARGET_SERIAL" =~ ^emulator- ]]; then
    echo "=================================================================" >&2
    echo "ERROR: Refusing to run on target serial '$TARGET_SERIAL'." >&2
    echo "This script strictly targets Android Virtual Devices (emulator-*)." >&2
    echo "Real device 'd93c743' holds production/pilot shop data." >&2
    echo "=================================================================" >&2
    exit 1
fi

# Locate adb
if command -v adb >/dev/null 2>&1; then
    ADB_BIN="$(command -v adb)"
elif [[ -x "$HOME/Library/Android/sdk/platform-tools/adb" ]]; then
    ADB_BIN="$HOME/Library/Android/sdk/platform-tools/adb"
elif [[ -n "${ANDROID_HOME:-}" && -x "$ANDROID_HOME/platform-tools/adb" ]]; then
    ADB_BIN="$ANDROID_HOME/platform-tools/adb"
else
    echo "ERROR: adb executable not found in PATH or Android SDK locations." >&2
    exit 1
fi

# Locate emulator
if command -v emulator >/dev/null 2>&1; then
    EMULATOR_BIN="$(command -v emulator)"
elif [[ -x "$HOME/Library/Android/sdk/emulator/emulator" ]]; then
    EMULATOR_BIN="$HOME/Library/Android/sdk/emulator/emulator"
elif [[ -n "${ANDROID_HOME:-}" && -x "$ANDROID_HOME/emulator/emulator" ]]; then
    EMULATOR_BIN="$ANDROID_HOME/emulator/emulator"
else
    echo "ERROR: emulator executable not found in PATH or Android SDK locations." >&2
    exit 1
fi

# Locate backend python venv
if [[ -x "$BACKEND_DIR/.venv/bin/python" ]]; then
    PYTHON_BIN="$BACKEND_DIR/.venv/bin/python"
    UVICORN_BIN="$BACKEND_DIR/.venv/bin/uvicorn"
elif [[ -x "/Users/appworx/Desktop/Vivek-K/projects/MandiSamiti/backend/.venv/bin/python" ]]; then
    PYTHON_BIN="/Users/appworx/Desktop/Vivek-K/projects/MandiSamiti/backend/.venv/bin/python"
    UVICORN_BIN="/Users/appworx/Desktop/Vivek-K/projects/MandiSamiti/backend/.venv/bin/uvicorn"
else
    PYTHON_BIN="$(command -v python3)"
    UVICORN_BIN="$(command -v uvicorn)"
fi

echo "================================================================="
echo " MandiSamiti Phase 3 Test Harness"
echo " Target Serial: $TARGET_SERIAL"
echo " AVD Name:      $AVD_NAME (with -no-audio)"
echo " Backend Port:  $BACKEND_PORT"
echo "================================================================="

# ------------------------------------------------------------------------------
# 2. Start AVD with -no-audio (if not already running & booted)
# ------------------------------------------------------------------------------
BOOTED="0"
if "$ADB_BIN" -s "$TARGET_SERIAL" get-state >/dev/null 2>&1; then
    BOOTED="$("$ADB_BIN" -s "$TARGET_SERIAL" shell getprop sys.boot_completed 2>/dev/null || echo "0")"
fi

if [[ "$BOOTED" != "1" ]]; then
    echo "[1/5] Starting AVD '$AVD_NAME' with -no-audio in background..."
    nohup "$EMULATOR_BIN" -avd "$AVD_NAME" -no-audio -gpu auto </dev/null >/dev/null 2>&1 &
    EMULATOR_PID=$!
    echo "$EMULATOR_PID" > /tmp/mandi_emulator.pid

    echo "      Waiting for $TARGET_SERIAL to connect..."
    "$ADB_BIN" -s "$TARGET_SERIAL" wait-for-device

    echo "      Waiting for boot completion..."
    while [[ "$("$ADB_BIN" -s "$TARGET_SERIAL" shell getprop sys.boot_completed 2>/dev/null || true)" != "1" ]]; do
        sleep 2
    done
    echo "  ✓ Emulator boot completed."
else
    echo "[1/5] Emulator $TARGET_SERIAL is already online and ready."
fi

# Wake up screen and dismiss keyguard
"$ADB_BIN" -s "$TARGET_SERIAL" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
"$ADB_BIN" -s "$TARGET_SERIAL" shell wm dismiss-keyguard >/dev/null 2>&1 || true

# ------------------------------------------------------------------------------
# 3. Setup and start Backend with Scratch Database and Random Secrets
# ------------------------------------------------------------------------------
echo "[2/5] Initializing scratch backend on port $BACKEND_PORT..."

SCRATCH_DB="$BACKEND_DIR/scratch_phase3.db"
rm -f "$SCRATCH_DB"

# Generate random secrets without printing them
OTP_STATIC_CODE="$("$PYTHON_BIN" -c "import secrets; print(f'{secrets.randbelow(900000) + 100000}')")"
SECRET_KEY="$("$PYTHON_BIN" -c "import secrets; print(secrets.token_hex(32))")"

export APP_ENV="development"
export DATABASE_URL="sqlite+aiosqlite:///${SCRATCH_DB}"
export OTP_STATIC_ENABLED="true"
export OTP_STATIC_CODE
export SECRET_KEY
export SENTRY_DSN=""
export SENTRY_ENVIRONMENT=""
export OPS_TELEGRAM_BOT_TOKEN=""
export OPS_TELEGRAM_CHAT_ID=""
export PYTHONPATH="$BACKEND_DIR"

# Initialize SQLite tables via Base.metadata.create_all
cd "$BACKEND_DIR"
"$PYTHON_BIN" -c "
import asyncio
from app.database import Base, engine
import app.models

async def init():
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    await engine.dispose()

asyncio.run(init())
"
echo "  ✓ Base.metadata.create_all completed on scratch database."

# Stop any previous process holding the port
lsof -ti :"$BACKEND_PORT" | xargs kill -9 2>/dev/null || true

# Launch uvicorn
nohup "$UVICORN_BIN" app.main:app --host 0.0.0.0 --port "$BACKEND_PORT" </dev/null > /tmp/mandi_backend_phase3.log 2>&1 &
BACKEND_PID=$!
echo "$BACKEND_PID" > /tmp/mandi_backend_phase3.pid

# Wait for backend health check
READY=0
for _ in $(seq 1 30); do
    if curl -s "http://127.0.0.1:$BACKEND_PORT/docs" >/dev/null 2>&1; then
        READY=1
        break
    fi
    sleep 0.5
done

if [[ "$READY" -ne 1 ]]; then
    echo "ERROR: Backend failed to start on port $BACKEND_PORT within timeout." >&2
    cat /tmp/mandi_backend_phase3.log >&2
    exit 1
fi
echo "  ✓ Local backend running with scratch DB (PID: $BACKEND_PID)."

# ------------------------------------------------------------------------------
# 4. Build and Install Debug APK
# ------------------------------------------------------------------------------
echo "[3/5] Checking debug APK build..."
cd "$REPO_ROOT"
APK_PATH="${1:-$REPO_ROOT/composeApp/build/outputs/apk/debug/composeApp-debug.apk}"

if [[ ! -f "$APK_PATH" ]]; then
    echo "      Building debug APK via ./gradlew :composeApp:assembleDebug..."
    ./gradlew :composeApp:assembleDebug --max-workers=2 --no-daemon -q
fi

echo "      Installing APK explicitly to $TARGET_SERIAL..."
"$ADB_BIN" -s "$TARGET_SERIAL" install -r -d "$APK_PATH" >/dev/null
echo "  ✓ Debug APK installed."

# ------------------------------------------------------------------------------
# 5. Clear Data & Launch Fresh Install to Dashboard
# ------------------------------------------------------------------------------
echo "[4/5] Launching fresh install on $TARGET_SERIAL..."
"$ADB_BIN" -s "$TARGET_SERIAL" shell pm clear com.appwork.mandisamiti >/dev/null
"$ADB_BIN" -s "$TARGET_SERIAL" shell am start -S -n com.appwork.mandisamiti/.MainActivity >/dev/null

echo "[5/5] Driving onboarding flow to Dashboard..."
export ADB_BIN
export TARGET_SERIAL
export OTP_STATIC_CODE

"$PYTHON_BIN" "$SCRIPT_DIR/drive_onboarding.py"

echo "================================================================="
echo " Phase 3 Test Harness Ready!"
echo " - Emulator:  $TARGET_SERIAL ($AVD_NAME, -no-audio)"
echo " - Backend:   http://127.0.0.1:$BACKEND_PORT (Scratch SQLite)"
echo " - Dashboard: Live on device"
echo " - Secrets:   Kept internal (never logged/printed)"
echo "================================================================="
