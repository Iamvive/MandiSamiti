# MandiSamiti Task D1: Device Rehearsal Report

Date: 2026-10-30
Device: Redmi Note (adb `d93c743`)
Target Pilot Build: `exports/mandisamiti-pilot-debug.apk` / `composeApp/build/outputs/apk/debug/composeApp-debug.apk`
Evidence Folder: `docs/testing/evidence/pilot-rehearsal-2026-10-31/`

---

## 1. Executive Summary
- **Gate Verification**: `./gradlew :core-database:jvmTest :core-data:jvmTest jvmTest :composeApp:assembleDebug` and `backend pytest` (102 tests) all **PASS**.
- **Clean Launch on Device**: **PASS**. Launch directly displays Hindi onboarding/login view without flicker or crash (`02_clean_launch_screen.png`). Fresh database initializes at schema version 7 (`mandisamiti_clean.db`).
- **Pre-Pilot Upgrade Test**: **FAIL / KNOWN ISSUE RECORDED**. Upgrading over a pre-pilot build that already had `slip_no` added in-place crashes with `duplicate column name: slip_no` (SQLite error 1). Clean install (`pm clear`) is verified to resolve this.
- **Static OTP Constraint**: **BLOCKER IDENTIFIED & NOTIFIED**. Client UI (`RegisterViewModel.kt`) and backend Pydantic schema (`VerifyOtpRequest`) enforce a 6-digit OTP. The 8-digit OTP on VPS staging must be rotated to 6 digits before shop login.

---

## 2. Test Execution & Evidence

| Rehearsal Item | Result | Evidence / Notes |
|---|---|---|
| JVM Tests (core-database, core-data, composeApp) | PASS | 117 Gradle tasks executed/up-to-date, 0 failures |
| Backend Pytest (102 tests) | PASS | 102 passed, 4 warnings in 15.7s |
| Pilot APK Assembly | PASS | `composeApp-debug.apk` (21,065,839 bytes) |
| Device Clean Launch | PASS | `02_clean_launch_screen.png` - Hindi text renders cleanly |
| SQLite Schema at Head | PASS | `PRAGMA user_version = 7` verified on pulled device db |
| Over-the-Top Upgrade from Pre-Pilot Build | FAIL | Crash in `6.sqm`: `duplicate column name: slip_no` (`01_migration_crash_logcat.txt`). Mitigated by clean install for pilot |
| Staging OTP Compatibility Check | FAIL | Schema enforces 6 digits (`min_length=4, max_length=6`), client checks `length == 6`. VPS staging code must be 6 digits |

---

## 3. Required Pre-Install Remediation
1. **VPS Staging OTP**: Update `/opt/mandisamiti-staging/.env.staging` on the VPS to set `OTP_STATIC_CODE` to a random **6-digit** code.
2. **Shop Device Provisioning**: Ensure Vivek executes `adb shell pm clear com.appwork.mandisamiti` before beginning Day 1 shop entries, following `docs/pilot/INSTALL-CHECKLIST.md`.
