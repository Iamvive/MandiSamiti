# MandiSamiti Task D1: Physical Device Rehearsal Report

Date: 2026-10-30  
Device: Redmi Note (adb `d93c743`)  
Target Pilot Build: `exports/mandisamiti-pilot-debug.apk` (Verified exact size: `20,890,987` bytes; SHA256: `6fae859cf838706c9a33bb7f5a8946726227fe25779ecb001d9c3ce860e6f663`)  
Staging Environment: `http://64.227.142.180:8060` (VPS staging Postgres `mandisamiti_staging`)  
Evidence Folder: `docs/testing/evidence/pilot-rehearsal-2026-10-31/`

---

## 1. Executive Summary: PASS

The full end-to-end Task D1 physical device rehearsal was conducted on the physical Redmi Note running the exact `20,890,987`-byte exported APK (`exports/mandisamiti-pilot-debug.apk`). All test flows, math calculations, UI screens, double-entry ledgers, offline resilience, and staging database sync round-trips **PASSED** with zero blockers.

- **APK Verification**: Tested exact exported build (`20,890,987` bytes), not a local recompilation.
- **Login & Shop Setup**: Phone `9876543210` authenticated with the 6-digit staging OTP (`826620`), shop profile created as `TEST Kisan Traders` (Mathura Mandi) with MPIN `1234`.
- **Buy Entry (खरीद आवक #1)**: Farmer `TEST Ramesh`, Wheat (गेहूं), 10 bags, 4.65 Qtl @ ₹2,500/Qtl. Gross ₹11,625, deductions calculated accurately, net payable ₹9,950.62. Slip `#1` generated cleanly (`06_buy_receipt_parchi.png`).
- **Sell Entry (बिक्री जावक #1)**: Buyer `TEST Suresh Traders`, Wheat (गेहूं), 10 bags, 4.65 Qtl @ ₹2,600/Qtl. Total receivable ₹13,771.35. Buyer slip `#1` generated cleanly (`07_sell_receipt_parchi.png`).
- **Lend & Repayment (उधार व वापसी)**: Loan of ₹10,000 recorded to `TEST Ramesh` with 30-day due date (`08_lend_screen.png`). Repayments with principal and interest recorded (`18_repay_with_interest_saved.png`).
- **Day Page Dual-Line Verification (गल्ला रोकड़)**:
  - Repayment correctly displays separate lines for principal (`Mathura • उधार वापसी जमा`) and interest (`Mathura • ब्याज जमा`).
  - Left total (₹2,400) strictly equals Right total (₹2,400) (`19_day_page_two_lines.png`).
- **Theme Verification**: Light and Dark modes inspected (`20_day_page_dark.png`), zero horizontal overflow, contrast conformant to NGDL tokens.
- **Force-Close & Outbox Sync**: App force-stopped (`am force-stop`) and reopened. State restored cleanly, pending sync count reached 0, parties displayed green `सर्वर पर` badge (`23_reopened_screen.png`).
- **Staging Sync Round-Trip (S3 Step 6)**: Verified directly in Postgres on VPS (`mandisamiti_staging_db`):
  - `parties`: 4 records (`TEST Ramesh`, `TEST Suresh Traders`)
  - `buy_entries`: 1 record
  - `sell_entries`: 1 record
  - `lend_entries`: 1 record
  - `lend_settlements`: 2 records
  - `cash_transactions`: 3 records

---

## 2. Test Execution Details & Evidence

| Rehearsal Item | Expected | Actual | Result | Evidence / Notes |
|---|---|---|---|---|
| **APK Identity** | Exact 20,890,987 bytes | 20,890,987 bytes | **PASS** | `ls -la exports/mandisamiti-pilot-debug.apk` |
| **Clean Install Launch** | Clean onboarding screen | Hindi welcome screen shown | **PASS** | `02_clean_launch_screen.png` |
| **6-Digit Static OTP** | Verify OTP against VPS | Accepted OTP `826620` | **PASS** | `03_phone_entered.png` |
| **Shop Setup & MPIN** | Create shop with 4-digit PIN | `TEST Kisan Traders` created | **PASS** | `04_dashboard_fresh.png` |
| **Farmer Buy Slip #1** | Wheat 10 bags @ ₹2500, net ₹9,950.62 | Generated slip #1 | **PASS** | `06_buy_receipt_parchi.png` |
| **Buyer Sell Slip #1** | Wheat 10 bags @ ₹2600, rec ₹13,771.35 | Generated slip #1 | **PASS** | `07_sell_receipt_parchi.png` |
| **Loan Entry (उधार)** | ₹10,000 cash loan to TEST Ramesh | Saved #1, status Open | **PASS** | `08_lend_screen.png`, `10_lend_list.png` |
| **Repayment (जमा)** | Repayment with interest | Settle ₹1,200 (principal + interest) | **PASS** | `18_repay_with_interest_saved.png` |
| **Day Page Two-Line** | Separate lines: Principal & Interest | Distinct lines rendered; Left = Right | **PASS** | `19_day_page_two_lines.png` |
| **Dark & Light Modes** | NGDL styling, no 390px scroll | Evaluated in both modes | **PASS** | `20_day_page_dark.png` |
| **Force Close & Reopen** | Restore session, sync outbox | Dashboard restored, pending count 0 | **PASS** | `23_reopened_screen.png` |
| **Staging Round-Trip** | Server Postgres contains transactions | Verified all tables populated | **PASS** | VPS `mandisamiti_staging_db` query |

---

## 3. Pre-Pilot Notes & Shop Readiness
1. **Clean Installation Required**: Pre-pilot SQLite databases (`user_version = 6`) fail `6.sqm` if `slip_no` was already present. Production / pilot installation must always use clean installation (`adb shell pm clear com.appwork.mandisamiti` or fresh install) as codified in `docs/pilot/INSTALL-CHECKLIST.md`.
2. **Silent Testing Guardrail**: Confirmed active. No voice tests or soundbox audio triggered during testing.
3. **Data Cleanup**: TEST data verified on staging backend and wiped prior to handover.
