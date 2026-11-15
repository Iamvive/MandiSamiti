# Design Spec: Buy Parchi Credit Jama, Multi-Day Payment History, and Sell Labour Whole-Rupee Adjustment

**Date:** 2026-11-15  
**Author:** Antigravity (Google DeepMind) & Vivek Kumar  
**Target Milestone:** Mandi Accounting Alignment  
**Status:** Approved by Vivek Kumar (2026-11-15)

---

## 1. Overview & Business Intent

In Indian Agricultural Mandi accounting (रोकड़ बही / बही-खाता), trading transactions follow strict operational conventions:

1. **Buy Parchi Unpaid Balance as Credit (जमा)**:
   - When a farmer sells crop (e.g. Monty sells wheat for ₹50,517 gross, less ₹697 expenses = ₹49,820, less ₹820 advance = ₹49,000 final payable), if the trader pays partial cash (e.g. ₹25,000) on Day 1, the remaining ₹24,000 is **Credit (उधार / जमा)**.
   - On Day 1 (15 Nov), this ₹24,000 is credited (**जमा**) to the farmer's account and recorded on the **जमा पक्ष (Credit)** of the Daily Register (रोकड़ बही) as a non-cash credit entry. It does **not** count as physical currency in the cash drawer.
2. **Subsequent Partial Payments Appended with Dates**:
   - If the farmer returns on Day 2 (16 Nov) and takes ₹12,000 cash:
     - Day 2's Daily Register records **₹12,000 नामे (Cash Out)** from the Galla.
     - The Buy Slip appends this payment as an itemized payment history row with its date:
       - *नकद भुगतान · 15 Nov: − ₹25,000*
       - *नकद भुगतान · 16 Nov: − ₹12,000*
       - **बाकी (Balance Left): ₹12,000**
3. **Print & WhatsApp Share Formatting**:
   - Both A5 Paper Print (`ReceiptHtmlRenderer`) and WhatsApp Share text must follow the exact sequence of the on-screen parchi:
     $$\text{Gross} \rightarrow \text{कुल खर्चा} \rightarrow \text{पर्ची राशि} \rightarrow \text{अग्रिम} \rightarrow \text{उधार कटौती} \rightarrow \text{अंतिम देय} \rightarrow \text{भुगतान विवरण (with dates)} \rightarrow \text{बाकी}$$
   - **Crucial Rule**: In Print and WhatsApp Share, **Labour and Commission must NOT be split** into separate rows. They must appear combined as a single **"कुल खर्चा"** row.
4. **Sell Parchi Labour Input & Whole-Rupee Rounding**:
   - The Sell Entry form must allow the trader to directly input the labour charge per quintal (editable, supporting decimals like ₹5.50 or ₹4.75, pre-filled from Settings).
   - If the resulting bill total has decimal paise, the bill total is rounded to the nearest whole rupee, and the **Labour charge absorbs the rounding delta** so the final bill has zero decimal paise.

---

## 2. Technical Architecture & Component Design

### Component A: Buy Slip Credit Jama & Payment History

#### 1. Transaction Types & Representation
- **Initial Slip Creation (Day 1)**:
  - If `netPayablePaisa - advancePaidPaisa - initialCashPaymentPaisa > 0`:
    - Insert `CashTransaction` for the unpaid credit:
      - `transactionType = TransactionType.JAMA_RECEIVED`
      - `paymentMode = PaymentMode.BOOK_ENTRY`
      - `amountPaisa = unpaidPaisa` (e.g. ₹24,000)
      - `sourceEntryType = "BUY_CREDIT"`
      - `sourceEntryId = buyEntry.id`
      - `partyId = buyEntry.farmerId`
      - `transactionDate = buyEntry.createdAt`
      - `remarks = "${farmerName} • खरीद पर्ची #${slipNo} बाकी जमा (क्रेडिट)"`
- **Cash Settlements (Day 1, Day 2, etc.)**:
  - Each payment from Galla creates:
    - `transactionType = TransactionType.UDHAR_GIVEN`
    - `paymentMode = PaymentMode.CASH`
    - `amountPaisa = paidAmountPaisa`
    - `sourceEntryType = "BUY"`
    - `sourceEntryId = buyEntry.id`
    - `transactionDate = currentTimeMs`
    - `remarks = "खरीद पर्ची #${slipNo} भुगतान"`
  - `BuyEntry.paidPaisa` is incremented by `paidAmountPaisa`.

#### 2. Daily Register UI & Cash Drawer Isolation
- In `DailyRegisterViewModel` & `DailyCashRegisterScreen`:
  - **Physical Cash In-Hand**:
    $$\text{Cash Drawer} = \sum (\text{CASH } \text{In}) - \sum (\text{CASH } \text{Out})$$
    Strictly filters `paymentMode == PaymentMode.CASH`. The `BOOK_ENTRY` credit never touches physical drawer cash.
  - **जमा पक्ष (In/Credit List)**:
    - Lists physical cash deposits.
    - Also lists `BUY_CREDIT` entries with a distinctive `[क्रेडिट / गैर-नकद]` badge (MandiSlateBg/MandiTextSecondary).
  - **Daily Summary Totals**:
    - **कुल रोकड़ आवक (Cash In)**: actual cash received.
    - **कुल क्रेडिट जमा (Credit Jama)**: sum of `BOOK_ENTRY` rows for the day.
    - **कुल जमा योग (Total Jama)**: Cash In + Credit Jama.

#### 3. Buy Receipt Itemized Payment Breakdown
- `BuyTransactionReceiptScreen`, `ReceiptHtmlRenderer`, and `buildFarmerSlipText`:
  - Load all cash transactions where `sourceEntryType == "BUY" && sourceEntryId == buyEntry.id && !isDeleted && !isVoid`, ordered by `transactionDate ASC`.
  - Render each installment with its date:
    - *नकद भुगतान · 15 Nov: − ₹25,000*
    - *नकद भुगतान · 16 Nov: − ₹12,000*
  - Followed by:
    - **बाकी (Balance Left)**: ₹12,000 (if remaining balance > 0).
    - Or **`पूर्ण चुकता`** badge in green when balance is 0.

---

### Component B: Print (A5) & WhatsApp Share Formatting

The receipt sequence is synchronized across Screen, Print, and Share:

| Step | Item | Screen Display | Print (A5) & Share Display |
|---|---|---|---|
| 1 | Header | Shop Name, Mandi, GSTIN, Licence | Shop Name, Mandi, GSTIN, Licence |
| 2 | Meta | Parcha #, Date, Farmer Name, Village | Parcha #, Date, Farmer Name, Village |
| 3 | Crop Table | Crop, Bags, Weight (Equation), Rate, Amount | Crop, Bags, Weight (Equation), Rate, Amount |
| 4 | Gross | कुल असल राशि (Gross) | कुल असल राशि (Gross) |
| 5 | Deductions | मजदूरी (−₹340), कमीशन (−₹357), कुल खर्चा (−₹697) | **कुल खर्चा: − ₹697** *(No split!)* |
| 6 | Slip Value | पर्ची राशि: ₹49,820 | पर्ची राशि: ₹49,820 |
| 7 | Advance | अग्रिम / पेशगी: − ₹820 *(if any)* | अग्रिम / पेशगी: − ₹820 *(if any)* |
| 8 | Loan Cut | उधार कटौती: − ₹X *(if any)* | उधार कटौती: − ₹X *(if any)* |
| 9 | Final Payable | अंतिम देय राशि: ₹49,000 | अंतिम देय राशि: ₹49,000 |
| 10 | Payment History | Itemized by date: 15 Nov: −₹25,000, 16 Nov: −₹12,000 | Itemized by date: 15 Nov: −₹25,000, 16 Nov: −₹12,000 |
| 11 | Balance | बाकी: ₹12,000 *(or पूर्ण चुकता)* | बाकी: ₹12,000 *(or पूर्ण चुकता)* |

---

### Component C: Sell Parchi Labour Input & Whole-Rupee Manipulation

#### 1. Sell Entry Form (`SellEntryScreen.kt` & `SellEntryFormState.kt`)
- Add form state property: `labourRatePerQuintal: String` (defaults to `settings.sellLabourPaisaPerQuintal`, formatted in rupees e.g. `"5"` or `"5.00"`).
- Add input field in UI:
  - Label: **"हम्माली / मजदूरी (₹/क्विंटल)"**
  - Keyboard: `MandiKeyboard.Decimal`
  - Allows decimal values (e.g. `5.50`, `4.75`).

#### 2. Whole-Rupee Rounding Engine (`BuySellMath.kt`)
```kotlin
fun calculateSell(input: SellMathInput): SellMathResult {
    val grams = totalWeightGrams(input.bags, input.weightPerBagGrams, input.extraWeightGrams)
    val gross = MandiMathEngine.grossAmountPaisa(grams, input.ratePaisaPerQuintal)
    val bos = input.billMode == BillMode.BILL_OF_SUPPLY
    val arat = MandiMathEngine.percentageOf(gross, input.aratBps)
    val shulk = if (bos) MandiMathEngine.percentageOf(gross, MANDI_SHULK_BPS) else 0L
    val tulak = if (bos) input.tulakPaisaOverride ?: defaultTulakPaisa(grams) else defaultTulakPaisa(grams)

    if (input.sellLabourPaisaPerQuintal > 0L) {
        val nominalLabour = MandiMathEngine.grossAmountPaisa(grams, input.sellLabourPaisaPerQuintal)
        val rawTotal = gross + arat + tulak + shulk + nominalLabour
        // Round to nearest whole rupee (0 paise)
        val roundedTotal = (rawTotal + 50L).floorDiv(100L) * 100L
        val adjustedLabour = nominalLabour + (roundedTotal - rawTotal)
        return SellMathResult(
            totalWeightGrams = grams,
            grossPaisa = gross,
            aratPaisa = arat,
            tulakPaisa = tulak,
            mandiShulkPaisa = shulk,
            totalReceivablePaisa = roundedTotal,
            balanceDuePaisa = roundedTotal - input.receivedPaisa,
            labourPaisa = adjustedLabour
        )
    }
    // ...
}
```

---

## 3. Data Integrity & Invariants

1. **Physical Drawer Invariant**:
   $$\text{Physical Cash In Hand} == \sum (\text{TransactionType.JAMA\_RECEIVED with CASH}) - \sum (\text{TransactionType.UDHAR\_GIVEN with CASH})$$
   `BOOK_ENTRY` rows are excluded from physical note counts.
2. **Whole Rupee Bill Invariant**:
   For any Sell bill with labour set:
   $$\text{totalReceivablePaisa} \pmod{100} == 0L$$
   $$\text{grossPaisa} + \text{aratPaisa} + \text{tulakPaisa} + \text{mandiShulkPaisa} + \text{labourPaisa} == \text{totalReceivablePaisa}$$
3. **Receipt Equality Invariant**:
   On all Buy receipts (Screen, A5 Print, WhatsApp Share):
   $$\text{Final Payable} == \text{Gross} - \text{Total Deductions} - \text{Advance} - \text{Loan Cut}$$
   $$\text{Remaining Balance} == \text{Final Payable} - \sum (\text{Cash Installments})$$

---

## 4. Verification Plan

1. **Automated Unit Tests**:
   - `BuySellMathTest`: Verify Sell labour whole-rupee rounding across fractional weights and rates (e.g. 5.15 Qtl @ ₹5.50 labour).
   - `SellSlipAmountsTest`: Verify labour extraction matches rounded total.
   - `ReceiptHtmlRendererTest`: Verify A5 print contains combined "कुल खर्चा" (no separate labour/commission split), date-stamped installments, and exact sequence.
   - `BuyTransactionReceiptTest`: Verify itemized payment history rows rendered in order.
   - `DailyRegisterViewModelTest`: Verify `BOOK_ENTRY` credit rows appear on Jama side but do not affect physical cash drawer balance.
2. **Live Emulator Verification**:
   - Create Buy Slip with partial cash payment $\rightarrow$ verify Parchi Baaki appears on Jama side as Credit.
   - Pay second installment $\rightarrow$ verify Day 2 Galla gets Cash Out (नामे), and Buy slip shows two date-stamped payment lines.
   - Verify Print and WhatsApp share output.
   - Create Sell Bill with decimal labour (e.g. ₹5.50/Qtl) $\rightarrow$ verify bill total is an exact whole rupee with zero paise.
