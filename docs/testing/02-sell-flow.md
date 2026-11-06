# MandiSamiti Revamp — Flow 02: Sell Flow (बिक्री सौदा — Direction B)

## 1. Flow Overview & Scope
The Sell Flow handles retail sales to direct consumers (Customer / ग्राहक) and wholesale distribution to grain merchants (Merchant / व्यापारी). Direction B unifies the Sell entry experience with the approved Buy entry design: phone-first party autocomplete, required name and village, 3-column lot rows (Bags | Weight/bag | Extra), tare removed from new slips, and an advance/balance section after the slip calculations.

---

## 2. Actor & Mode Rules Matrix

| Actor / Category | Allowed Mode | Phone & Village | Mandi Tax (1.5%) & Cess (0.5%) | Commission % & Labour (₹/bag) | Multiple Crop Lots | Advance Received (अग्रिम) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Customer (ग्राहक)** | **Normal Slip only** | **Mandatory** (10-digit phone, village) | Not Applied (0%) | Commission % + Labour (₹/bag) | Yes (multiple variants at diff rates) | Optional (reduces balance due) |
| **Merchant (व्यापारी)** | **Normal Slip** | **Mandatory** (10-digit phone, village) | Not Applied (0%) | Commission % + Labour (₹/bag) | Yes (multiple variants at diff rates) | Optional (reduces balance due) |
| **Merchant (व्यापारी)** | **Bill of Supply** | **Mandatory** (+ GSTIN & State Code) | **Mandatory** (Mandi 1.5% + Cess 0%) | Labour (₹/bag) + Tulak | Yes (multiple variants at diff rates) | Optional (reduces balance due) |

### Key Business & Direction B Guardrails
1. **Phone-First Party Block:**
   - Field 1: "मोबाइल नंबर या नाम" (Mobile number or name). Required 10-digit Indian phone.
   - Matching saved party autofills Name and Village with badge: "सहेजे गए नंबर से भरा गया। बदलने पर यह खाता हर जगह अपडेट होगा।"
   - New number creates the buyer party once.
   - Old party with no phone prompts for 10-digit mobile before Save.
   - Name and Village are editable and required. Edits update the party everywhere.
   - Save is disabled until phone (10 digits), name, and village are filled.
2. **Lot Grid (Direction B):**
   - Line 1 (3 columns): Bags (whole number, max 9,999) | Weight per bag (whole kg, max 999) | Extra weight (kg, decimals allowed, strictly `< bag weight`).
   - Line 2: Rate (whole rupees per quintal, max 1,00,000).
   - Multi-lot additions supported.
3. **Tare / Bardana Removed on New Slips:**
   - New Sell entries do not deduct tare. The entered weight is the billed weight.
   - Editing an old slip with stored tare displays tare read-only without modifying stored totals.
4. **Slip Summary (हिसाब पर्ची):**
   - मजदूरी कटौती sits above कमीशन कटौती.
   - Keeps Sell's trader bill arithmetic (Cash Memo whole rupee cut with tulak absorption vs Bill of Supply 1.5% Mandi Tax + Tulak).
5. **Separate Advance & Balance Section:**
   - Positioned below the slip calculations.
   - Shows party's existing loan debt and deposit balance as read-only cards. Sell slips never adjust loan or deposit.
   - Advance received now (अग्रिम / प्राप्त राशि) reduces `balanceDuePaisa` (`totalReceivable - advanceReceived`).
   - Dashboard "Vyapari receivable" tracks remaining balance due. Cash received is recorded to Galla via `JAMA_RECEIVED`.

---

## 3. Math Engine Specifications & Formulae

### Multi-Lot Aggregation
Each sale can contain $N \ge 1$ crop lots:
- **Lot Weight (grams):** $W_i = (\text{bags}_i \times \text{weightPerBag}_i) + \text{extraWeight}_i$
- **Lot Gross (paisa):** $G_i = \text{round}\left(\frac{W_i \times \text{ratePaisaPerQuintal}_i}{100,000}\right)$
- **Total Weight (grams):** $W_{\text{total}} = \sum_{i=1}^N W_i$
- **Total Gross (paisa):** $G_{\text{total}} = \sum_{i=1}^N G_i$
- **Total Bags:** $B_{\text{total}} = \sum_{i=1}^N \text{bags}_i$

### Mode Calculations
1. **Normal Slip (Customer or Merchant Cash Memo):**
   - Commission Paisa: $\text{round}\left(\frac{G_{\text{total}} \times \text{commissionBps}}{10,000}\right)$
   - Nominal Tulak: $W_{\text{total}} \times 1\text{ Rs/Qtl}$
   - Total Receivable cut to whole rupee: $\lfloor (G_{\text{total}} + \text{commissionPaisa} + \text{nominalTulak}) / 100 \rfloor \times 100$
   - Tulak absorbs rounding: $\text{totalReceivable} - G_{\text{total}} - \text{commissionPaisa}$
   - Balance Due: $\text{totalReceivable} - \text{advanceReceived}$

2. **Bill of Supply (Merchant only):**
   - Mandi Shulk (1.5%): $\text{round}\left(\frac{G_{\text{total}} \times 150}{10,000}\right)$
   - Tulak: $W_{\text{total}} \times \text{tulakRate}$
   - Arat / Commission: $\text{round}\left(\frac{G_{\text{total}} \times \text{commissionBps}}{10,000}\right)$
   - Total Receivable: $G_{\text{total}} + \text{mandiShulk} + \text{tulakPaisa} + \text{commissionPaisa}$
   - Balance Due: $\text{totalReceivable} - \text{advanceReceived}$

---

## 4. Test Cases Matrix

| Case ID | Scenario | Input Data | Expected Output | Status |
| :--- | :--- | :--- | :--- | :--- |
| **TC-SELL-01** | Phone & Village Gating | Missing phone or village | Save disabled; explanatory text on button | **PASS (JVM & Device)** |
| **TC-SELL-02** | Party Autocomplete | Type 10-digit saved phone | Autocompletes name + village, shows saved party badge | **PASS on Device** |
| **TC-SELL-03** | Extra Weight Boundary | Extra weight >= bag weight (e.g. 50kg on 50kg bag) | Red error under field; Save blocked | **PASS (JVM & Device)** |
| **TC-SELL-04** | Tare Removed on New Slips | New slip entry | Tare is blank and not deducted from billed weight | **PASS (JVM & Device)** |
| **TC-SELL-05** | Old Slip Tare Preserved | Edit slip with stored tare | Tare shows read-only; totals intact | **PASS (JVM)** |
| **TC-SELL-06** | Advance & Balance Due | Receivable ₹10,155, advance ₹3,000 | Balance due ₹7,155; cash recorded to Galla | **PASS (JVM & Device)** |
| **TC-SELL-07** | Read-Only Loan & Deposit | Party with ₹5,250 loan and ₹1,500 deposit | Cards display balances; slip total unchanged | **PASS (JVM & Device)** |
| **TC-SELL-08** | BOS Merchant Gating | Merchant + BOS mode | Requires 15-char GSTIN and 2-digit State Code | **PASS (JVM & Device)** |

---

## 5. Physical Device Test Evidence (Redmi Note 7, Serial: `d93c743`)
Screenshots captured and stored in `docs/testing/evidence/sell-direction-b/`:
- `01_sell_entry_empty.png`: Phone-first layout, disabled Save button with explanation.
- `02_sell_entry_phone_party_autocomplete.png`: Autocomplete party by phone, source badge, name & village filled.
- `03_sell_entry_lot_validation.png`: 3-column lot layout, limits and extra weight validation.
- `04_sell_entry_advance_and_slip.png`: हिसाब पर्ची + पर्ची के बाद का हिसाब (Advance + read-only loan balance).
- `05_sell_saved_receipt_s6.png`: Saved Sell slip receipt.
