# Phase 3 Buy Flow Verification Report (A5)

## 1. Scope & Verification Matrix
- **Screens Verified:** `BuyEntryScreen.kt`, `BuyPaymentScreen.kt`, `BuyTransactionReceiptScreen.kt`
- **Device Target:** Android Emulator `emulator-5554` (AVD `medium_phone`, Android 14 API 34), no audio
- **Display Checkpoints:**
  - Standard 411dp width (1080x2400 @ 420dpi)
  - Narrow 390dp width (`wm density 443`)
  - Hindi language localization
  - Dark & Light themes (tested on composited background tokens)
  - Legacy Tare line readback preservation (`extractTare`)

---

## 2. Evidence Artifacts

| Screen / State | Evidence Screenshot Path | Observations |
| :--- | :--- | :--- |
| **Buy Entry Form** | `docs/testing/evidence/phase3/buy/01-buy-entry-screen.png` | Sticky bottom dock always visible; final payable ₹23,630 calculated dynamically via `preview.payoutPaisa`; Save button full width; zero horizontal scroll at 390dp; zero unicode emoji. |
| **Post-Save Payment** | `docs/testing/evidence/phase3/buy/02-buy-payment-screen.png` | Banner `✓ पर्ची #1 सुरक्षित हुई`; large ₹23,630 final payable; payment mode chips ("पूरा भुगतान", "आंशिक नकद"); drawer cash deduction note `गल्ले से ₹23,630 नकद निकलेगा`; action buttons "भुगतान सुरक्षित करें" and "अभी नहीं". |
| **Buy Receipt (Revised)** | `docs/testing/evidence/phase3/buy/03-buy-receipt-screen.png` | Labour row ("मजदूरी") placed strictly above Commission ("कमीशन"); Loan cut row ("उधार कटौती"); Paid ("भुगतान किया") and Left ("बाकी राशि") rows; revision indicator badge `संशोधित • 01/11/2026 13:58 • #1`. |

---

## 3. Definition of Done Checklist

- [x] **No Emoji:** Verified zero unicode emojis in all UI code (`phase3_ui_check.py`).
- [x] **Zero Raw Colors:** Strictly uses NGDL tokens and themes (`phase3_ui_check.py`).
- [x] **No UI Paisa/Gram Arithmetic:** All financial computations isolated into domain engine/helper (`BuyPaymentCalculations.kt`, `phase3_ui_check.py: PASS`).
- [x] **No `install-debug-task`:** Explicit `adb install -r -d` used throughout.
- [x] **Monotonic Slips & Single Series:** Monotonic slip numbering preserved across app resets and sync.
- [x] **Legacy Tare Preservation:** `extractTare` extracts and displays tare line on receipts for historical slips with remarks.
- [x] **Unit & Regression Tests:** Full JVM test suite passed (`:composeApp:jvmTest`, `:core-data:jvmTest`, `:core-database:jvmTest`, `:core-domain:jvmTest` — 23 actionable tasks green).

---

## 4. Unverified Items
- Loan repayment integration from C6 (waiting on C6 merge).
- Physical execution on hardware Redmi `d93c743` (untested per project instructions).
