# MandiSamiti Revamp — Flow 06: Galla Management (दैनिक गल्ला रोकड़ बही एवं मिलान)

## 1. Flow Overview & Scope
Galla Management handles the physical cash drawer reconciliation (दैनिक गल्ला रोकड़ बही / रोज़नामा) for the mandi shop. It provides real-time auditability of in-hand physical cash, tracks opening balances carried forward from previous trading days, records direct cash inflows and outflows, incorporates operational expenses and farmer loans, supports evening cash count verification with discrepancy detection, and produces WhatsApp-shareable day-closing summaries alongside Soundbox Hindi voice announcements.

### Traditional Indian Baniya Rokad Bahi Architecture (दो पल्ला रोकड़ बही)
Rooted in traditional Indian merchant bahi-khata accounting (देशी नामा बही-खाता पद्धति), the ledger uses an authentic two-column Left & Right folded ledger sheet (दो पल्ला):
- **बायां पल्ला (Left Wing) — जमा पक्ष (Jama / आवक / Cash In):**
  - **Top Row (बाकी निकाला / Siri):** Opening cash balance carried forward from previous trading days.
  - **Entries:** All daily cash receipts, direct collections, and loan repayments in bold green (`+₹`).
  - **Bottom Total (कुल जमा जोड़):** $\text{Opening Balance} + \text{Today's Cash In}$.
- **दायां पल्ला (Right Wing) — नामे पक्ष (Name / निकासी / Cash Out):**
  - **Entries:** All daily operational disbursements, purchase payments, expenses (hammali, tea), and loan advances in bold red (`-₹`).
  - **Bottom Anchor (सिल्क बाकी / Silak):** Physical cash remaining in the drawer ($\text{In-Hand Closing Cash}$).
  - **Bottom Total (कुल नामे जोड़):** $\text{Today's Cash Out} + \text{Closing Cash in Hand}$.
- **Tally Bar (बही मिलान / जोड़ बराबर):**
  - Since $\text{Closing Cash} = \text{Opening} + \text{In} - \text{Out}$, mathematically:
    $$\text{Left Total (कुल जमा)} \equiv \text{Right Total (कुल नामे)}$$
  - When balanced, the bahi displays **जोड़ बराबर (Tally Matched)** with a verified badge.
- **Dynamic View Modes (पल्ला चयन):**
  - `दो पल्ला (Split L/R)`: Side-by-side split bahi with central fold line (default).
  - `जमा पक्ष (Cash In)`: Full-width left side for itemized receipts.
  - `नामे पक्ष (Cash Out)`: Full-width right side for itemized expenses and advances.

---

## 2. Test Cases Matrix & Verification

| Case ID | Scenario | Input / Precondition | Expected Output | Status |
| :--- | :--- | :--- | :--- | :--- |
| **TC-GAL-01** | Opening Balance Carry-Forward | Previous day net cash +₹12,000; today +₹5,000 In, -₹2,000 Out | Opening = ₹12,000, Closing = ₹15,000; previous day entry excluded from today's list | **PASS (Unit Verified)** |
| **TC-GAL-02** | Direct Cash Inflow (Jama) | Record cash in ₹15,000 from buyer | `todayCashInPaisa` = 1,500,000, `inHandCashDrawerPaisa` = 1,500,000 | **PASS (Unit Verified)** |
| **TC-GAL-03** | Direct Cash Outflow (Udhar) | Drawer has ₹10,000; record cash out ₹3,500 advance | `todayCashOutPaisa` = 350,000, `inHandCashDrawerPaisa` = 650,000 (₹6,500) | **PASS (Unit Verified)** |
| **TC-GAL-04** | Cash Expense Integration | Drawer has ₹5,000; Hammali cash expense ₹800 recorded | Cash out = ₹800, Closing in drawer = ₹4,200 | **PASS (Unit Verified)** |
| **TC-GAL-05** | Loan Advance & Repayment | Initial ₹20,000; loan ₹4,000 cash out, loan repayment ₹1,500 cash in | Cash In = ₹21,500, Cash Out = ₹4,000, Closing = ₹17,500 | **PASS (Unit Verified)** |
| **TC-GAL-06** | Physical Cash Reconciliation | Expected in hand ₹10,000 | Count ₹10,000 -> MATCH (₹0); Count ₹10,500 -> SURPLUS (+₹500); Count ₹9,800 -> SHORTAGE (-₹200) | **PASS (Unit Verified)** |
| **TC-GAL-07** | Day Closing WhatsApp Report | Shop: "बालाजी ट्रेडर्स", Cash In: ₹2,500 | Formatted text with shop, mandi, date, opening, in, out, closing, and optional reconciliation | **PASS (Unit Verified)** |
| **TC-GAL-08** | Soundbox Hindi Voice Announcement | In hand cash ₹1,800; trigger announcement | Speaks: *"आज का दिन समाप्त। गल्ले में कुल नकदी ₹1,800 है।"* | **PASS (Unit Verified)** |
| **TC-GAL-09** | Voided Transaction Exclusion | Valid ₹1,000 JAMA, Voided ₹500 JAMA | Closing = ₹1,000; voided ₹500 strictly excluded from drawer totals | **PASS (Unit Verified)** |
| **TC-GAL-10** | Opening Balance Flexibility (Setup/Adjust) | Set initial float ₹5,000; adjust to ₹4,500 | Opening = ₹5,000 -> ₹4,500; drawer closing balance updates instantly | **PASS (Unit Verified)** |
| **TC-GAL-11** | Discrepancy Auto-Adjustment | Expected ₹10,000, Counted ₹9,800 (-₹200 shortage); adjust | Records shortage adjustment; drawer re-balances to exact ₹9,800 match | **PASS (Unit Verified)** |
| **TC-GAL-12** | Owner Cash Withdrawal Home (Drawings) | Drawer has ₹25,000; owner withdraws ₹20,000 to take home | Cash out = ₹20,000; ₹5,000 left in drawer for tomorrow's opening float | **PASS (Unit Verified)** |

---

## 3. Automated Test Coverage
- Verified via automated test suite in `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/register/GallaFlowTest.kt`:
  - `tcGal01_openingBalanceCarriedForwardFromPreviousDays`
  - `tcGal02_todayDirectCashInflow_incrementsCashInAndClosing`
  - `tcGal03_todayDirectCashOutflow_incrementsCashOutAndReducesClosing`
  - `tcGal04_expenseOutflow_reducesCashDrawer`
  - `tcGal05_lendOutflowAndRepayInflow_reconcilesCashDrawer`
  - `tcGal06_physicalCashReconciliation_computesExactDifference`
  - `tcGal07_dayClosingReportFormatting_includesAllMetricsAndReconciliation`
  - `tcGal08_voiceAnnouncement_speaksClosingBalanceInHindi`
  - `tcGal09_voidedTransactions_excludedFromDrawerTotals`
  - `tcGal10_setOrAdjustOpeningBalance_updatesOpeningAndBalances`
  - `tcGal11_reconcileAndAdjustDiscrepancy_closesDifference`
  - `tcGal12_cashWithdrawalHome_deductsFromClosing`
- Unit tests in `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/register/DailyRegisterViewModelTest.kt`:
  - `testDailyRegisterCashDrawerReconciliation`
  - `gallaShowsOnlyTodayWithOpeningCarriedForward`
  - `transactionRecordedBeforePartiesEmitStillShowsThePartyName`
  - `testDayClosingReportGeneration`
  - `recordingAnEntryAsksForASync`
- Full test suite execution: **100% GREEN (12 Galla test cases passed, all modules green)**.
