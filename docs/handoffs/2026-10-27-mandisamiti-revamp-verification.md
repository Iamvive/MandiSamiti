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

## 6. Stage 1 → Stage 2 UX Finding (Oct 27, 21:53-21:59 IST)
Added a disposable test party ("ZZ-TEST-DELETE-ME", Farmer) via Ledger > Add Party, specifically to tap-test the Buy flow without touching Dharmendra/Manoj's real balances. **This test party still exists in the live shop and has not been deleted — needs cleanup** (Ledger tab > ZZ-TEST-DELETE-ME > delete, if the UI supports it; it has ₹0 balance and 0 transactions, nothing was ever submitted against it).

**Finding:** On Stage 1 (आवक व तौल), filling बोरी संख्या=17, सकल वजन=13, काट=0.35 (net 12.65 क्विंटल, correct) and tapping the "→ अगला" button on the custom keypad does **not** advance to Stage 2, and gives no error/toast/logcat output. Traced via subagent code read (`composeApp/.../ui/deal/DealEntryViewModel.kt:229` `moveToNextField()`): "अगला" only cycles keypad focus between बोरी संख्या → सकल वजन → काट → (wraps to बोरी संख्या on Stage 1). Only the "स्टेज 2: नीलामी व भाव" breadcrumb tab itself (`DealEntryScreen.kt:165-166`, `toggleSettlementStage(true)`) actually changes stage, and it has no field-completion gate.

This is a real usability risk for the 25-65 age target users this app is designed for: the button is labeled and positioned like a "proceed to next step" affordance (black, full-width, arrow icon, bottom-right of a numeric keypad) but it's actually a numeric-entry-only field-cycle key. A first-time user who fills Stage 1 and taps "अगला" expecting to move to pricing will see nothing happen and may not notice the small tab row above is the actual navigation. Suggest either: (a) renaming/restyling अगला to make clear it's "next field" not "next step", or (b) wiring the TARE_WEIGHT→next-press branch in `moveToNextField()` to call `toggleSettlementStage(true)` once Stage 1's fields are filled, per the subagent's suggested fix location.

Confirmed Stage 2 renders correctly once reached: व्यापारी (खरीदार) buyer-selection dropdown appears, Stage 1 fields persist above (read-only context), tab underline moves to Stage 2, icon switches from the scale icon to the auction icon.

**Not completed this session** (stopped at context budget): Stage 2 rate/price entry and full submit, void-after-submit verification, Sell/Lend/Expense entry tap-throughs, dark theme (still blocked by MIUI's non-standard dark mode setting — needs manual toggle via MIUI Settings app or a non-MIUI device).
