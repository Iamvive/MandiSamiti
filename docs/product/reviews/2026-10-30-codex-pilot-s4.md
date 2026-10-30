# Codex pilot S4 handoff

Date: 2026-10-30

Branch: `chore/pilot-s4`

Base commit: `971f49d`

## Completed

- Reconciled the pilot debug APK already exported at the Vivek-K workspace path `exports/mandisamiti-pilot-debug.apk`.
- Recorded the artifact identity in `docs/pilot/INSTALL-CHECKLIST.md`:
  - package `com.appwork.mandisamiti`
  - version `2.0.0` (`versionCode 1`)
  - size 20,890,987 bytes
  - SHA-256 `17536d7e39457d9a903fd5b3c20a08713fb983e4b91d3a31a7d172bc9a82e8a4`
- Added the shop install, TEST flow, sync check, wipe, and first-real-entry checklist.
- Added `docs/pilot/KNOWN-ISSUES.md` with pilot stop conditions, open infrastructure work, security limits, migration risks, and debug-signing limits.
- Removed the tracked fallback OTP value and the instruction to ask an AI to reveal the private OTP. The checklist now directs Vivek to the approved local or VPS secret store.

## Verification

```text
ANDROID_HOME=/Users/appworx/Library/Android/sdk ANDROID_SDK_ROOT=/Users/appworx/Library/Android/sdk ./gradlew :core-database:jvmTest :core-data:jvmTest jvmTest :composeApp:assembleDebug
```

Result: `BUILD SUCCESSFUL in 50s`, 117 tasks executed.

```text
cd backend
PYTHONPATH=. venv/bin/pytest -q
```

Result: `102 passed, 4 warnings in 24.32s`.

## Next action

Antigravity runs D1 with this exact APK and records PASS or blockers in `docs/product/reviews/2026-10-31-pilot-rehearsal.md`. The shop install must wait for D1 PASS.

The weekly encrypted Google Drive copy remains open. Nightly VPS backups and the restore test are already working.
