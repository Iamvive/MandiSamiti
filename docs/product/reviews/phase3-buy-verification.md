# Phase 3 Buy Flow Verification Report (A3/A5 — Direction B Final)

## 1. Scope & Verification Matrix
- **Design Target:** Direction "B final" from `docs/product/ux-directions/index.html` (the sole approved source).
- **Screens Rebuilt:**
  - `BuyEntryScreen.kt` (all 3 party states, 3-column lots, flat sections, हिसाब पर्ची, loan card, deposit card, after-slip summary, sticky dock).
  - `BuyPaymentScreen.kt` (post-save payment screen with "पर्ची #... सुरक्षित हुई", "आंशिक"/"पूरा" chips, cash deduction preview).
  - `BuyTransactionReceiptScreen.kt` (bill screen with Labour above Commission, "संशोधित"/"मूल पर्ची" badge, share and new buy actions).
- **Target Device:** Android Emulator `emulator-5554` (AVD `medium_phone`, Android 14 API 34), strictly with `-no-audio`.
- **Dimensions & Theme Checked:**
  - Narrow 390dp width (`adb -s emulator-5554 shell wm density 443`).
  - Both Light Mode (`cmd uimode night no`) and Dark Mode (`cmd uimode night yes`).
  - Contrast ≥ 4.5:1 on composited backgrounds.
  - Zero horizontal overflow at 390dp.

---

## 2. Side-by-Side Comparison: Direction B Final vs Live Emulator at 390dp

| Screen State | B Final Direction (`index.html`) | Live Emulator 390dp (`docs/testing/evidence/phase3/buy/b-final/`) | Verification Verdict |
| :--- | :--- | :--- | :--- |
| **State 1: Old party with no number** | Header "नई खरीद एंट्री" (no subtitle); flat fields 56dp/20sp; "10 अंकों का नंबर डालें"; warning banner "इस खाते में नंबर नहीं है। सेव से पहले नंबर डालें।"; disabled button "नंबर डालने पर सुरक्षित होगा". | `buy_state_old_party_light.png`<br>`buy_state_old_party_dark.png` | **MATCH**: Header, flat 56dp field, warning banner with alert icon, Save disabled with "नंबर डालने पर सुरक्षित होगा". |
| **State 2: Fresh number** | "मोबाइल नंबर या नाम" (`98765 00002`); "किसान का नाम" ('नाम लिखें'); "गांव" ('गांव लिखें'); hint "नया नंबर। एंट्री सेव होते ही खाता बन जाएगा।"; disabled button "नाम और गांव डालने पर सुरक्षित होगा". | `buy_state_new_party_light.png`<br>`buy_state_new_party_dark.png` | **MATCH**: Hint rendered, Save disabled with "नाम और गांव डालने पर सुरक्षित होगा" until name and village entered. |
| **State 3: Saved party (top)** | Saved lookup prefilled/selected (`9876543210`, `TEST Ramesh`, `TEST Nagla`); hint "सेव नंबर से नाम और गांव भरे..."; 3-col lots (बोरी संख्या, वजन/बोरी, अतिरिक्त); भाव on next line; सकल & वजन line. | `buy_state_saved_party_light.png`<br>`buy_state_saved_party_dark.png` | **MATCH**: 3-column lots row, rate on next line, gross calculation, hint line in place. |
| **State 3: Saved party (filled form)** | हिसाब पर्ची (Labour above Commission); Advance ₹3,000; Open Loan ₹3,200 with loan cut ₹1,000 and interest-first split hint; Deposit ₹500 (read-only per C6); final payable ₹19,630; enabled "सुरक्षित करें". | `buy_form_filled_light.png`<br>`buy_form_filled_dark.png` | **MATCH**: Labour above Commission, loan card with interest-first hint preview, read-only deposit per C6, sticky dock ₹19,630 with enabled "सुरक्षित करें". |
| **State 4: Payment Screen** | Header "किसान को भुगतान"; banner "पर्ची #... सुरक्षित हुई"; final payable tile; chips "आंशिक" / "पूरा"; flat field "अभी नकद दें (रु)"; Galla cash deduction notice; "भुगतान सुरक्षित करें" & "अभी नहीं". | `buy_payment_screen_light.png`<br>`buy_payment_screen_dark.png` | **MATCH**: Success banner, full/partial toggle, dynamic galla deduction text, "भुगतान सुरक्षित करें" and "अभी नहीं" buttons. |
| **State 5: Bill Screen** | Header "खरीद पर्ची #..."; badge "संशोधित · 1 Nov 2026, 17:10"; lines card with Labour above Commission; advance ₹3,000; loan cut ₹1,000; final payable ₹19,630; cash payment ₹19,630; left ₹0; "पर्ची भेजें" with share icon; secondary "नई खरीद". | `buy_bill_screen_light.png`<br>`buy_bill_screen_dark.png` | **MATCH**: Correct figures (Fix 1), B final date format (Fix 3), Labour above Commission, revision badge, paid and left amounts, "पर्ची भेजें" and "नई खरीद" buttons. |

---

## 3. Definition of Done Checklist (§5.7)

- [x] **Screenshots in Light & Dark at 390px Width:** All 12 screenshots captured at density 443 (390dp width) and stored in `docs/testing/evidence/phase3/buy/b-final/`.
- [x] **No Emoji:** Strictly vector icons (`MandiIcons`), zero unicode emoji in UI code (`phase3_ui_check.py: PASS`).
- [x] **Zero Raw Hex Colors:** Strictly uses NGDL tokens and themes (`brand-assets/theme.css`, `ui/theme/`).
- [x] **No UI Paisa/Gram Arithmetic:** All financial computations isolated into domain helper (`BuyPaymentCalculations.kt`, `phase3_ui_check.py: PASS`).
- [x] **No Forbidden Commands:** Explicit `adb -s emulator-5554 install -r -d` used. Redmi `d93c743` untouched.
- [x] **Labour Above Commission:** Placed strictly above Commission on all slips and receipts.
- [x] **Flat Grey-Filled Text Fields:** Flat 56dp height, 20sp typography (`MandiFlatTextField`).
- [x] **Sticky Bottom Dock:** Live final payable amount and full-width Save button always visible without scrolling.
- [x] **Tests Passing:** Full `:composeApp:jvmTest` suite passes (including `BaselineMoneyBehaviourAuditTest`).

---

## 4. Differences Kept
1. **Deposit Card Read-Only (C6 Decision):** Direction B final mock HTML originally had an editable "जमा से दें (रु)" field. Per Vivek's explicit decision 2026-11-01 (documented in plan C6i), deposit is not used or deducted on the Buy slip (to keep the slip scoped strictly to the current transaction). As specified, `depositPaisa` is displayed as read-only ("हमारे पास जमा: ₹500", "खाते में जमा सुरक्षित है...").
2. **Missing Phone Banner Shape:** Renders as an alert banner using theme surface elevated and `MandiIcons.Alert` vector icon with text "इस खाते में नंबर नहीं है। सेव से पहले नंबर डालें।", matching B final semantics while respecting NGDL token container elevation.

---

## 5. Unverified Items
- Physical execution on hardware Redmi `d93c743` (untested per contract).
- Existing old slips edit path (finding 1 loan cut on edit is being fixed separately in C7).
