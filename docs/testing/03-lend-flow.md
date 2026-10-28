# MandiSamiti Revamp — Flow 04: Lend Flow (उधार खाता - पैसे एवं जिंस)

## 1. Flow Overview & Scope
Mandi commission agents (आढ़ती) advance money (रोकड़ उधार / पेशगी) or lend grain seed/fertilizer (जिंस उधार) to farmers before harvesting season. Flow 04 manages the complete lifecycle of these loans:
- **Money Loan (`MONEY`):** Principal amount in cash or bank transfer with expected return schedule.
- **Grain Loan (`GRAIN`):** Commodity loan in bags and quintals, returnable as crop or settled in cash at market value.

### Screen Sequence & Components
1. **S7 (LendEntryScreen):**
   - Mode selector toggle: **[रुपये / नकद (Money)]** vs **[अनाज (Grain)]**.
   - Party name / mobile input.
   - For Money: Principal amount in rupees (parsed to exact integer paisa via `MandiMathEngine.parseRupeesToPaisa`).
   - For Grain: Commodity name, bag count, and weight in kg (parsed to exact grams via `MandiMathEngine.parseKgToGrams`).
   - Expected return date (days e.g. 30 days or specific date).
   - Optional remarks / notes.
   - Validation: Save button strictly disabled if amount or weight is 0 or party is blank.

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
| **TC-LEND-01** | Money Loan | Party: "रामू किसान", Amount: ₹5,000, Return: 30 days | Saved in `lend_entries` with `amountPaisa = 500,000L`, status `OPEN` | **PASS (Unit Verified)** |
| **TC-LEND-02** | Grain Loan | Party: "सुरेश", Commodity: "गेहूं", Bags: 4, Weight: 200kg | Saved with kind = `GRAIN`, `bagsCount = 4`, `weightGrams = 200,000L` (2.00 Qtl) | **PASS (Unit Verified)** |
| **TC-LEND-03** | Zero Amount Validation | Party: "मोहन", Amount: ₹0 / blank | Save button disabled; non-positive inputs rejected | **PASS (Unit Verified)** |
| **TC-LEND-04** | Active Loan Prompt in Buy S1 | Farmer "रामू किसान" selected in S1 Buy Entry | `activeLoanChecker` triggers `सक्रिय उधार मौजूद है` warning chip | **PASS (Unit Verified)** |
| **TC-LEND-05** | Partial Repayment | ₹5,000 loan, repaid ₹2,000 via Repayment Sheet | Loan status transitions to `PARTIAL` | **PASS (Unit Verified)** |
| **TC-LEND-06** | Full Settlement | Remaining ₹3,000 repaid | Loan status transitions to `SETTLED`, active loan prompt turns off | **PASS (Unit Verified)** |
| **TC-LEND-07** | Dashboard KPI Update | Active ₹10,000 loan recorded | `DashboardKpis.totalLendPaisa` updates to ₹10,000 (1,000,000 paisa) | **PASS (Unit Verified)** |

---

## 3. Automated Test Coverage
- Verified via automated test suite in `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/lend/LendFlowTest.kt`:
  - `tcLend01_saveMoneyLoan_createsActiveOpenLend`
  - `tcLend02_saveGrainLoan_recordsBagsAndWeightGrams`
  - `tcLend03_zeroAmountValidation_disabled`
  - `tcLend04_activeLoanPromptInBuyEntry`
  - `repaymentLifecycle_partialToSettled_reflectedInDashboardKpis`
- Build status: **BUILD SUCCESSFUL (87/87 tests passed)**.
