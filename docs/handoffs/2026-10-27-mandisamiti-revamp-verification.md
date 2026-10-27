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

## 5. ADB Tap-Through (Oct 27, 21:48-21:51 IST)
Driven via `adb shell input tap` + `screencap` (no scrcpy session available in this agent environment). This touched the **live shop** ("Maa Sharda Traders", Orai Mandi, real farmer accounts Dharmendra ₹7,050 / Manoj ₹1,000) — no form was submitted, to keep the real ledger untouched.

- [x] App launch clean, no crash in logcat, `MainActivity` focused.
- [x] Dashboard: KPI cards (Receivable/Payable), Active Ledger Accounts list, speed-dial FAB — vector icons, no emoji, no overflow.
- [x] Bottom nav: Dashboard / Ledger / Cash Drawer / Settings all route correctly.
- [x] Ledger tab: search bar, All/Receivable/Payable filter chips, Add Party FAB.
- [x] Cash Drawer: Cash In Hand, Opening Balance, Total In/Out, Cash In/Out buttons, Today's Cash Entries list — correct red/green coding.
- [x] Settings: shop profile, Hindi/English toggle, Trade & Commission Rates, Voice Confirmation switch, Local Offline Storage status, Sign Out — no theme toggle in-app.
- [x] New Trade Entry (स्टेज 1: आवक व तौल) for Dharmendra/Wheat: typed gross weight 123 Qtl with taar 0.35 Qtl prefilled → net weight computed live and correctly as 122.65 क्विंटल. Custom numeric keypad (0-9, 00, ., C, काटें, अगला) all tappable. Backed out via ← without saving.
- [ ] **Dark theme not verifiable**: `adb shell settings put secure ui_night_mode 2` has no effect on this MIUI/Redmi Note 7 build (MIUI uses its own dark-mode setting, not AOSP `ui_night_mode`). Confirms the existing note in feature-status.md under Authentication & Sign Out. Needs either a manual toggle via MIUI Settings > Display > Dark mode, or a non-MIUI test device.
- [ ] Stage 2 (नीलामी व भाव) of Buy entry, and the full Sell/Lend/Expense entry flows, not tap-tested this pass — same caution applies: real farmer accounts, so any full run should either use a disposable test party or be void-reverted immediately after.
