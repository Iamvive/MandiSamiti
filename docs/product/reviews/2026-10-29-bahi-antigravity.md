# MandiSamiti Bahi Model: Antigravity Handoff (2026-10-29)

**Branch:** `feat/bahi-antigravity`  
**Base:** `main` at `fe603f3`  
**Phone verification:** Redmi Note 7 (`d93c743`), Android 10 (MIUI).

---

## 1. Summary of Completed Work

### Part A: Device Proof of Merged Fixes (fe603f3)
- Tested R1, R4, R6, F19, F22, F23, F24 on the connected Redmi Note 7 device.
- Full verification matrix and screenshots committed in `docs/product/reviews/2026-10-29-device-proof.md` and `docs/testing/evidence/device-2026-10-29-merged/`.
- All tests passed.

### Part B: Feature Work

#### T1. Receipt Charges (D5)
- **Settings defaults:** Buy and Sell forms seed charges directly from saved shop `TradeSettings` (e.g. 1.50% commission, ₹150 labour, 0.35 qtl tare). Fixed legacy 2% / ₹5 defaults.
- **Per-receipt overrides:** Editing any charge on a receipt updates only that entry and never alters shop default settings.
- **Override hints:** Fields show subtle NGDL-compliant hint when modified: `डिफ़ॉल्ट से बदला गया (1.50%)` / `(₹150)` so accidental typing errors are instantly visible.
- **NGDL Iconography:** Removed raw unicode warning emoji (`⚠️`) at `BuyEntryScreen.kt:336`, replacing it with `Icons.Default.Warning` vector icon tinted with `MandiAmberDark`.
- **Unit test:** Added `ReceiptChargesAndDefaultsTest` in `composeApp/src/jvmTest` covering defaults seeding, override isolation, and tare weight deduction.

#### T2. Stock per Grain Type (D6)
- **Dashboard card:** Wired `GodownStockSummaryCard` directly into `DashboardScreen.kt`, showing daily commodity stock and oversold alerts. Tapping it opens `StockBreakdownSheet` with quintal and kg breakdowns.
- **Negative stock warning:** If a sale exceeds godown inventory, a warning chip is displayed on the sell entry screen without hard-blocking save, and the stock card flags it as `स्टॉक से ज्यादा बिक्री`.
- **Unit test:** Added `DailyStockCarryForwardAndNegativeWarningTest` in `core-domain/src/commonTest` verifying stock carry-forward over multiple days and negative stock flagging.

---

## 2. File Ownership & Files Touched
All changes strictly adhered to agent boundaries:
- `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/buy/BuyEntryScreen.kt` (Owned)
- `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/sell/SellEntryScreen.kt` (Owned)
- `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dashboard/DashboardScreen.kt` (Dashboard card placement)
- `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/receipts/ReceiptChargesAndDefaultsTest.kt` (New test)
- `core-domain/src/commonTest/kotlin/com/appwork/mandisamiti/domain/DailyStockCarryForwardAndNegativeWarningTest.kt` (New test)

*No edits made to `AppDatabase.sq`, migrations, `ui/register/*`, or loan calculation logic.*

---

## 3. Verification & Test Suite
- `./gradlew jvmTest`: **BUILD SUCCESSFUL** (all unit tests passing across all 4 modules).
- `python3 -m unittest discover -s tests/ngdl-tokens`: **OK** (5 tests passing).
- `./gradlew :composeApp:installDebug`: Installed and verified live on Redmi Note 7 in both light and dark modes. Screenshots captured in `docs/testing/evidence/bahi-antigravity/`.
