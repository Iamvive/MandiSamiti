# MandiSamiti Revamp — Flow 03: Lend Flow (उधार खाता - पैसे एवं जिंस — Direction B)

> Screenshots cited here were deleted on 2026-11-13. Recover them from git history with `git show 05f8301^:<path>`.

## 1. Flow Overview & Scope
Mandi commission agents (आढ़ती) advance money (रोकड़ उधार / पेशगी) or lend grain seed/fertilizer (जिंस उधार) to farmers before harvesting season.
Direction B unifies the Lend entry experience with Buy and Sell:
- **Phone-First Party Block:** Mobile number is required (10-digit Indian phone). Entering a number looks up existing party or creates a new one. Old party without a number prompts for phone before Save. Name and Village are editable and required.
- **Money Loan (`MONEY`):** Principal amount in cash with expected return schedule.
- **Grain Loan (`GRAIN`):** Commodity loan in bags and kg, returnable as crop or settled in cash.

### Screen Sequence & Components
1. **S7 (LendEntryScreen - Direction B):**
   - Mode selector toggle: **[रुपये / नकद (Money)]** vs **[अनाज (Grain)]**.
   - **Party Block (Phone-First):**
     - Field 1: "मोबाइल नंबर या नाम" (Mobile number or name). Required 10 digits (`6-9` prefix).
     - Field 2: "किसान / व्यक्ति का नाम" (Farmer / Person Name). Required.
     - Field 3: "गांव" (Village). Required.
     - Source hint badge:
       - Saved party: *"सहेजे गए नंबर से भरा गया। बदलने पर यह खाता हर जगह अपडेट होगा।"*
       - New party: *"नया नंबर। एंट्री सेव होते ही खाता बन जाएगा।"*
       - Old party without phone: *"सुरक्षित करने से पहले 10 अंकों का नंबर दर्ज करें"*.
   - For Money: Principal amount in rupees (parsed to exact integer paisa via `MandiMathEngine.parseRupeesToPaisa`).
   - For Grain: Commodity name, bag count, and weight in kg (parsed to exact grams via `MandiMathEngine.parseKgToGrams`).
   - Expected return date (days e.g. 15, 30, 60 days chip or custom date picker).
   - Optional remarks / notes.
   - Validation & Save button:
     - Disabled if phone is blank (*"10 अंकों का नंबर डालें"*).
     - Disabled if phone invalid (*"मान्य 10 अंकों का मोबाइल नंबर दर्ज करें"*).
     - Disabled if name or village is blank (*"नाम और गांव डालने पर सुरक्षित होगा"*).
     - Disabled if money amount is 0 (*"उधार राशि (रु) दर्ज करें"*).
     - Disabled if grain crop/weight is 0 (*"जिंस और कुल वजन दर्ज करें"*).

2. **S8 (LendTransactionsListScreen):**
   - Search query field (search by party name or commodity name).
   - Real-time sections: Active/Open outstandings vs Settled loans with expandable accordion.
   - Dynamic chips: **[बकाया (Open)]**, **[आंशिक वापसी (Partial)]**, **[पूर्ण चुकता (Settled)]**, and **[अतिदेय]** alert badge for overdue loans.
   - Floating Action Button (+) to open `LendEntryScreen`.
   - **Repayment Bottom Sheet:**
     - For Money: Enter repayment amount in Rupees.
     - For Grain: Enter returned weight in Kg and bag count.
     - Automatically updates loan status to `PARTIAL` or `SETTLED` based on threshold.

3. **Buy S1 Cross-Flow Integration (`BuyEntryScreen`):**
   - Dynamic `activeLoanChecker` continuously monitors selected farmer against active un-voided, non-settled loans.
   - Displays warning badge **[सक्रिय उधार मौजूद है (हिसाब में कटौती संभव)]** to alert the trader during crop purchase settlement.

4. **Dashboard KPI Reflection (`DashboardRevampComponents`):**
   - Tile **[कुल उधार (बाकी)]** aggregates live sum of active non-void money loans.

---

## 2. Test Cases Matrix & Verification

| Case ID | Scenario | Input Data | Expected Output | Status |
| :--- | :--- | :--- | :--- | :--- |
| **TC-LEND-01** | Phone Required Validation | Blank phone / invalid phone | Save button disabled with explanation | **PASS** |
| **TC-LEND-02** | Name & Village Required | Valid phone, blank name or village | Save button disabled ("नाम और गांव डालने पर सुरक्षित होगा") | **PASS** |
| **TC-LEND-03** | Phone Autocomplete & Source Hint | Enter `6398207680` (Pawan) | Name and village autofilled with saved source badge | **PASS** |
| **TC-LEND-04** | Old Party Without Phone Prompt | Select old party without phone | Warning prompt shown, Save disabled until phone entered | **PASS** |
| **TC-LEND-05** | Money Loan Save | Phone: `9876543210`, Party: "रामू किसान", Village: "खेडा", Amount: ₹5,000 | Saved in `lend_entries` with `amountPaisa = 500,000L`, status `OPEN`, party created | **PASS** |
| **TC-LEND-06** | Grain Loan Save | Phone: `9826012345`, Party: "सुरेश", Village: "देवास", Commodity: "गेहूं", Bags: 4, Weight: 200kg | Saved with kind = `GRAIN`, `bagsCount = 4`, `weightGrams = 200,000L` | **PASS** |
| **TC-LEND-07** | Partial Repayment & Settlement | ₹5,000 loan repaid in parts | Transitions from `OPEN` to `PARTIAL` to `SETTLED` | **PASS** |

---

## 3. Physical Device Verification (Redmi Note 7, Serial: `d93c743`)
Screenshots stored in `docs/testing/evidence/lend-direction-b/`:
- `01_lend_entry_empty.png`: Empty Lend screen with Direction B phone-first block and disabled Save button.
- `02_lend_entry_phone_autocomplete.png`: Autocomplete saved party by phone with source hint badge.
- `03_lend_entry_filled_money.png`: Filled money loan with return date chips.
- `04_lend_entry_grain_mode.png`: Grain mode with commodity and weight.
- `05_lend_saved_list.png`: Saved loan in Lend transactions list.
