# MandiSamiti Revamp — Flow 03: Lend Flow (उधार खाता - पैसे एवं जिंस)

## 1. Flow Overview & Scope
Mandi traders frequently advance money (रोकड़ उधार) or lend commodity seed/grain (जिंस उधार) to farmers before harvesting. Flow 03 manages:
- **Money Loan (`MONEY`):** Principal amount in cash or bank transfer with expected return date.
- **Grain Loan (`GRAIN`):** Commodity loan in bags and quintals, returnable as grain or settled in cash at prevailing market price.

### Screen Sequence
1. **S7 (LendEntryScreen):** Mode toggle (पैसे vs जिंस), Party search/input, Principal amount or commodity bags/kg, Return date, and remarks.
2. **S8 (LendTransactionsListScreen):** Open loans overview with filter chips (सभी, सक्रिय उधार, चुकता).

---

## 2. Calculation & Ledger Integration
- Money loans post a debit to the farmer's khata ledger and credit cash/bank register.
- When recording a subsequent Buy entry (S1), if an active loan exists for that farmer, a warning badge is shown with one-tap loan recovery deduction.

---

## 3. Test Cases Matrix

| Case ID | Scenario | Input Data | Expected Output | Status |
| :--- | :--- | :--- | :--- | :--- |
| **TC-LEND-01** | Money Loan | Party: "रामू किसान", Amount: ₹5,000, Return: 30 days | Saved in `lend_entries`, Khata balance +₹5,000 debit | PENDING |
| **TC-LEND-02** | Grain Loan | Party: "सुरेश", Commodity: "गेहूं", Bags: 4, Weight: 200kg | Saved with kind = `GRAIN`, bags & kg recorded | PENDING |
| **TC-LEND-03** | Zero Amount Validation | Party: "मोहन", Amount: ₹0 | Save button disabled | PENDING |
| **TC-LEND-04** | Active Loan Prompt in Buy S1 | Farmer with active loan selected in S1 | "सक्रिय उधार मौजूद है" chip appears on S1 | PENDING |
