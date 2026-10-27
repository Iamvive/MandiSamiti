# MandiSamiti Revamp — Flow 04: Expense Flow (दुकान एवं मंडी खर्चे)

## 1. Flow Overview & Scope
Tracks daily operational outlays (दुकान खर्च) including labour/hammali, tea/snacks, electricity, rent, transport, and stationeries.

### Screen Sequence
1. **S9 (ExpenseEntryScreen):** Category chip selector (हम्माली, किराया, बिजली, चाय-पानी, मजदूरी, अन्य), amount, payment mode (Cash / UPI / Bank), recipient name, notes.
2. **Integration:** Directly reduces Daily Cash Register (गल्ला) on cash payment.

---

## 2. Test Cases Matrix

| Case ID | Scenario | Input Data | Expected Output | Status |
| :--- | :--- | :--- | :--- | :--- |
| **TC-EXP-01** | Standard Cash Expense | Category: "चाय-पानी", Amount: ₹150, Mode: CASH | Saved in `expenses`, Cash register balance reduced by ₹150 | PENDING |
| **TC-EXP-02** | Hammali Labour Batch | Category: "हम्माली", Amount: ₹2,400, Payee: "मजदूर टोली" | Saved with payee details, recorded in daily closing | PENDING |
| **TC-EXP-03** | Zero Amount Validation | Amount: ₹0 | Save button disabled | PENDING |
