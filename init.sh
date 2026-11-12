#!/usr/bin/env bash
# Session smoke check. Exit non-zero if the base state is broken. Run at session start.
set -u
cd "$(dirname "$0")"
echo "== git =="; git branch --show-current; git status --short | head -10; git log --oneline -3
echo "== features =="
python3 - <<'PY'
import json
f=json.load(open("FEATURES.json"))["features"]
fail=[x["id"] for x in f if x["status"]=="failing"]
print(f"{len(f)-len(fail)} passing, {len(fail)} failing")
print("failing:", ", ".join(fail))
PY
echo "== device =="
if command -v adb >/dev/null 2>&1; then adb devices | sed -n '2,$p' | grep -v '^$' || echo "no device attached"; else echo "adb not installed (device checks must be done on Vivek's machine)"; fi
if [ "${SKIP_TESTS:-0}" = "1" ]; then echo "== tests skipped (SKIP_TESTS=1) =="; exit 0; fi
echo "== tests =="; ./gradlew jvmTest -q
