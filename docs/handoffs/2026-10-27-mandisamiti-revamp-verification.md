# MandiSamiti Revamp Hardware Verification Report

- **Date:** 2026-10-27
- **Device:** Redmi Note 7 (Serial: `d93c743`, Android 10)
- **Branch:** `feat/mandisamiti-revamp` (at `revamp-integrated`)
- **Status:** PASS

## 1. Automated Gates Verification
- `:core-database:verifyCommonMainAppDatabaseMigration`: PASS
- `:core-domain:jvmTest`: PASS
- `:core-database:jvmTest`: PASS
- `:core-data:jvmTest`: PASS (including math, repository, and sync v2 tests)
- `:composeApp:jvmTest`: PASS (including Screen routing, Buy, Sell, Lend/Expense, and Dashboard KPIs)
- `backend/pytest`: PASS (97 passed, 0 failures)

## 2. Hardware Installation & Launch
- Command: `./gradlew :composeApp:installDebug` -> SUCCESS (`Installed on 1 device.`)
- Launch: `adb -s d93c743 shell monkey -p com.appwork.mandisamiti -c android.intent.category.LAUNCHER 1`
- Focused Activity: `com.appwork.mandisamiti/.MainActivity` (active in foreground, 0 crashes reported in logcat)

## 3. UI & Business Rules Checklist
- [x] Multi-AI implementation branches `feat/mandisamiti-revamp-tests` and `feat/mandisamiti-revamp-ui` successfully merged into `feat/mandisamiti-revamp`.
- [x] S1–S3 Buy screens: Pure calculations match worked example (Net payable Rs 11,322.50).
- [x] S4–S6 Sell screens: Normal vs Bill of Supply match worked example (Total Rs 26,660.00, Balance Rs 16,660.00).
- [x] S7–S9 Lend & Expense: Money vs Grain toggle, Hindi category labels, loan book with repayment sheet.
- [x] S10–S11 Dashboard: 4 KPI cards, godown stock breakdown, P&L card, SpeedDialFab with vector icons.
- [x] Hardware App Launch: Running live on Redmi Note 7 (`d93c743`).

## 4. Unverified Items
- Interactive manual tap-through of every individual form screen on device display (requires physical interactive user session via `scrcpy -s d93c743`).
