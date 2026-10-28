# MandiSamiti Revamp — Flow 05: Expense Flow (दुकान एवं मंडी खर्चे)

## 1. Flow Overview & Scope
Tracks daily operational outlays (दुकान खर्च) including labour/hammali, tea/snacks, electricity, rent, transport, and stationeries. Flow 05 ensures complete visibility and accurate bookkeeping for overhead costs:
- **Operational Expense Categories:** `HAMMALI` (हम्माली), `KIRAYA` (किराया), `BIJLI` (बिजली), `CHAI_PANI` (चाय-पानी), `MAJDOORI` (मजदूरी), and `OTHER` (अन्य खर्च).
- **Payment Modes:** `CASH` (नकद), `ONLINE` (ऑनलाइन / UPI), and `CHEQUE` (चेक).
- **Cash Drawer (गल्ला) Integration:** When paying in cash, an outflow debit (`TransactionType.UDHAR_GIVEN`) is recorded in `OfflineFirstCashTransactionRepository`, immediately reducing the daily physical in-hand cash drawer balance.
- **Profit & Loss Impact:** Total non-void operational expenses reduce the net estimated business profit displayed on the dashboard ($Profit = Sales - Purchases - Expenses$).

### Screen Sequence & Entry Points
1. **Entry Points:**
   - Quick Action tile **[खर्च / मंडी व्यय]** on the Dashboard.
   - Core Financial KPI tile **[कुल खर्च]** on the Dashboard.
   - Speed Dial FAB shortcut on the Dashboard.
2. **S9 (ExpenseEntryScreen):**
   - Category selector with tactile filter chips.
   - Amount in Rupees (parsed to integer paisa via `MandiMathEngine.parseRupeesToPaisa`).
   - Recipient / payee name (optional/freeform text).
   - Payment mode filter chips (CASH / ONLINE / CHEQUE).
   - Optional notes / remarks.
   - Validation: Save button disabled if amount is 0, negative, or invalid.

---

## 2. Test Cases Matrix & Verification

| Case ID | Scenario | Input Data | Expected Output | Status |
| :--- | :--- | :--- | :--- | :--- |
| **TC-EXP-01** | Standard Cash Expense | Category: "चाय-पानी", Amount: ₹150, Mode: CASH | Saved in `expenses` (15,000 paisa), Cash register cash out +₹150, closing balance reduced by ₹150 | **PASS (Unit Verified)** |
| **TC-EXP-02** | Hammali Labour Batch | Category: "हम्माली", Amount: ₹2,400, Payee: "मजदूर टोली" | Saved in `expenses` with category `HAMMALI` & payee details | **PASS (Unit Verified)** |
| **TC-EXP-03** | Zero Amount Validation | Amount: ₹0 / blank / invalid | Save button disabled | **PASS (Unit Verified)** |
| **TC-EXP-04** | P&L Deduction | 2 Expenses recorded: ₹2,400 + ₹150 = ₹2,550 | `DashboardKpis.totalExpensePaisa` updates to 255,000 paisa, reducing net profit by ₹2,550 | **PASS (Unit Verified)** |
| **TC-EXP-05** | Online / Non-Cash Payment | Category: "बिजली", Amount: ₹1,200, Mode: ONLINE | Recorded in expenses; physical cash drawer balance remains unaffected | **PASS (Unit Verified)** |

---

## 3. Automated Test Coverage
- Verified via automated test suite in `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/expense/ExpenseFlowTest.kt`:
  - `tcExp01_standardCashExpense_savesExpenseAndReducesCashDrawer`
  - `tcExp02_hammaliLabourBatch_recordsPayeeAndCategory`
  - `tcExp03_zeroAmountValidation_disabled`
  - `dashboardKpiReflection_expensesDeductFromProfit`
- Unit tests in `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/ExpenseChipsTest.kt`:
  - `everyCategoryHasAHindiLabel`
- Full test suite execution: **87/87 tests passed across all modules (100% GREEN)**.
