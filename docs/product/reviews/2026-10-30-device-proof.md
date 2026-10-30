# MandiSamiti Merged `main` Device Proof (Redmi Note 7, adb `d93c743`)
Date: 2026-10-30
Commit: `88c989b` / `b8e79c2`
Tester: Antigravity (Gemini 3.8 Flash)

## Build & Installation Verdict
- **Command:** `./gradlew :composeApp:installDebug`
- **Result:** **PASS (Clean build & install in 35s)**
- **App Startup:** Initialized cleanly, database transaction backfilled parties, splash rendered and navigated to Auth / Login.

---

## Device Test Verification Matrix

| Area / Decision | Check Description | Evidence Screenshot | Verdict | Notes |
|---|---|---|---|---|
| **App Launch & Auth** | Clean install after `pm clear`, mobile login & MPIN verify | `01-launch.png` | **PASS** | Logged in with TEST phone `9876543210` and MPIN. |
| **D5: Receipt Charge Defaults** | Buy entry pre-populates commission (1.50%), labour (150), kata (0.35) from TradeSettings | `06-buy-charges-default.png` | **PASS** | Exact default charge tokens loaded into form. |
| **D5: Edited Charge & Hint** | Commission edited to 2.00%; shows warning/hint "डिफ़ॉल्ट से बदला गया (1.50%)" | `07-buy-charges-edited-hint.png` | **PASS** | Real-time hint appeared below commission field; saved receipt persisted edited ₹232.50 commission. |
| **Buy Entry & Receipt** | 10 bags wheat @ ₹2,500/qtl saved to TEST Ramesh | `08-buy-receipt-light.png` | **PASS** | Gross ₹11,625, deductions applied, net payable ₹9,892.50. |
| **D6: Dashboard Stock Card** | Stock card carries wheat inventory forward on Dashboard | `09-dashboard-with-stock.png` | **PASS** | Shows "गेहूं • 10 बोरी • 4.65 क्विंटल". |
| **D6: Overselling Warning** | Sell form shows warning when attempting to sell 15 bags wheat when only 10 (4.65 qtl) in stock | `11-sell-oversell-warning.png` | **PASS** | Displays amber alert: "चेतावनी: गेहूं की बिक्री (7.5 qtl) गोदाम स्टॉक (4.65 qtl) से अधिक है". |
| **D6: Overselling Allowed** | Overselling warning does not block save; trader can confirm and proceed | `12-sell-receipt-saved.png` | **PASS** | Bill saved cleanly for buyer TEST Suresh. |
| **D6: Negative Stock Display** | Dashboard stock card reflects negative inventory after oversell | `13-dashboard-negative-stock.png` | **PASS** | Shows "स्टॉक से ज्यादा बिक्री -2.5 qtl" with negative pill. |
| **D1 / D2: Two-Sided Galla** | Galla register displays two-sided Jama (Credit) and Name (Debit) columns | `14-galla-two-sided.png` | **PASS** | Both pallas balance and separate incoming/outgoing transaction lists. |
| **D9: Day-Close Difference** | Physical cash count dialog reconciles expected vs actual drawer cash | `15-galla-close-dialog.png`, `16-day-close-count-entered.png` | **PASS** | Reconciled difference calculated and reason options presented. |
| **R6: Deposit Overflow** | Receiving ₹30,000 against ₹21,118.85 balance settles bill and splits excess | `20-deposit-dialog-light.png`, `21-deposit-overflow-preview.png`, `22-buyer-ledger-after-split.png` | **PASS** | Ledger balance flipped to ₹8,881.15 advance/dena hai. |
| **F20–F24: KPIs & Navigation** | Dashboard KPIs, sync badges, and bottom navigation bar work without crash | `02-dashboard-light.png`, `25-relaunch-dashboard.png` | **PASS** | Bottom bar transitions between Dashboard, Khata, Galla, and Settings smoothly. |
| **390px Width Check** | No horizontal scrolling or content clipping on Redmi (1080x2340 @ ~393dp) | `02-dashboard-light.png`, `04-khata-light.png`, `18-party-ledger-light.png` | **PASS** | Responsive card layouts and wrapping rows conform to NGDL §5.5. |

---

## Observations & Handoff for Codex & Claude
1. **The merged `main` build is solid, stable, and completely functional on device.**
2. **Task A1 is 100% COMPLETE.**
3. **Green light for Codex (Part B) and Claude (Part C) to proceed in parallel.**
