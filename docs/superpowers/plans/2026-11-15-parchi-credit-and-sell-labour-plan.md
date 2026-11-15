# Buy Parchi Credit Jama, Multi-Day Payment History, and Sell Labour Whole-Rupee Rounding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Mandi accounting alignment: recognize unpaid Buy slip amounts as Credit (जमा) in the Daily Register, record subsequent cash payments as Cash Out (नामे) with date-stamped installments on the slip, format Print/Share receipts with combined "कुल खर्चा" matching the on-screen sequence, and add an editable per-quintal labour input on Sell Parchi that manipulates labour to ensure whole-rupee bills.

**Architecture:** 
1. **Parchi Credit (जमा)**: When a Buy slip is saved with an unpaid balance, insert a non-cash `BOOK_ENTRY` transaction (`TransactionType.JAMA_RECEIVED`, `sourceEntryType = "BUY_CREDIT"`). In `DailyRegisterViewModel`, display it on the Jama side with a `[क्रेडिट]` badge while keeping physical cash drawer calculations strictly filtered to `PaymentMode.CASH`.
2. **Multi-Day Payment Timeline**: Settle cash actions insert `TransactionType.UDHAR_GIVEN` with `paymentMode = CASH` on the current date and increment `buyEntry.paidPaisa`. `BuyTransactionReceiptScreen`, `ReceiptHtmlRenderer`, and WhatsApp text query these cash payments by `sourceEntryId` and render them in chronological order with date stamps.
3. **Receipt Sequence & Expense Consolidation**: In Print (A5) and WhatsApp share, follow the on-screen sequence exactly. Labour and commission are combined into a single "कुल खर्चा" line.
4. **Sell Labour Whole-Rupee Rounding**: Add an editable `labourRatePerQuintal` field on `SellEntryScreen` (`MandiKeyboard.Decimal`). `BuySellMath.calculateSell` rounds total receivable to the nearest whole rupee and assigns `adjustedLabour = nominalLabour + (roundedTotal - rawTotal)`.

**Tech Stack:** Kotlin Multiplatform (Compose Multiplatform 1.6.11), SQLDelight, Android PrintManager, JUnit / Kotlin Test.

## Global Constraints
- **Integer Math:** Currency strictly in `Long` paisa (1 INR = 100 paisa) and weights in `Long` grams (1 Qtl = 100,000 g). Zero float financial arithmetic.
- **NGDL v1.2 Design Tokens:** Colors from `ui/theme/Color.kt`. High contrast (≥ 4.5:1), vector icons (`ImageVector`), zero emojis in UI.
- **Physical Cash Isolation:** Physical cash in Galla is strictly based on `paymentMode == PaymentMode.CASH`. `BOOK_ENTRY` rows must never alter drawer note counts.
- **Whole Rupee Bills:** Sell bills with labour must have `totalReceivablePaisa % 100L == 0L`.
- **Silent Testing:** Zero TTS audio playback during tests.

---

## File Structure

```
core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/
├── math/
│   ├── BuySellMath.kt                              # [Modify] Ensure decimal per-quintal rates round to whole rupees via labour
│   └── SellSlipAmounts.kt                          # [Modify] Extract labour and verify consistency
├── model/
│   └── CashTransaction.kt                          # [Reference] TransactionType & PaymentMode.BOOK_ENTRY

composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/
├── ui/
│   ├── sell/
│   │   ├── SellEntryFormState.kt                   # [Modify] Add labourRatePerQuintal field and decimal handling
│   │   └── SellEntryScreen.kt                      # [Modify] Add editable labour rate input field
│   ├── buy/
│   │   ├── BuyTransactionReceiptScreen.kt          # [Modify] Render itemized payment installment history with dates
│   │   └── FarmerSlipCashSettlementSheet.kt        # [Reference] Cash settlement sheet
│   ├── register/
│   │   ├── DailyRegisterViewModel.kt               # [Modify] Support BOOK_ENTRY credit rows in Jama without inflating physical cash
│   │   └── DailyCashRegisterScreen.kt              # [Modify] Render [क्रेडिट] badge on non-cash Jama items
│   ├── slip/
│   │   └── ReceiptHtmlRenderer.kt                  # [Modify] Follow exact slip sequence, combine कुल खर्चा, itemize payments with dates
│   └── App.kt                                      # [Modify] Create BUY_CREDIT transaction on Buy save; wire payment history to BuyReceipt

core-domain/src/commonTest/kotlin/com/appwork/mandisamiti/domain/
├── BuySellMathTest.kt                              # [Modify] Add tests for decimal sell labour and whole-rupee rounding
└── SellSlipAmountsTest.kt                          # [Modify] Test breakdown consistency

composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/
├── ui/register/
│   └── DailyRegisterCreditJamaTest.kt              # [Create] Test BOOK_ENTRY credit Jama in Daily Register
├── ui/slip/
│   └── ReceiptHtmlRendererTest.kt                  # [Modify] Test A5 print receipt sequence, combined खर्चा, payment timeline
└── ui/buy/
    └── BuyReceiptPaymentHistoryTest.kt             # [Create] Test multi-day payment timeline rendering
```

---

## Phase 1: Sell Parchi Labour Input & Whole-Rupee Rounding

### Task 1.1: Math Engine Decimal Sell Labour & Whole Rupee Rounding
**Files:**
- Modify: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/math/BuySellMath.kt`
- Test: `core-domain/src/commonTest/kotlin/com/appwork/mandisamiti/domain/BuySellMathTest.kt`

**Interfaces:**
- `BuySellMath.calculateSell(input: SellMathInput): SellMathResult`
- Input: `sellLabourPaisaPerQuintal: Long` (e.g. 550L for ₹5.50/Qtl).
- Output: `SellMathResult.totalReceivablePaisa % 100L == 0L`, `labourPaisa = nominalLabour + (totalReceivable - raw)`.

- [x] **Step 1: Write unit tests in `BuySellMathTest.kt`**
  ```kotlin
  @Test
  fun sellLabourDecimalRateRoundsToExactWholeRupee() {
      // 5.15 Qtl (515_000 g), ₹2,200/Qtl (220_000 paisa), arat 1.5% (150 bps), tulak ₹1/Qtl, labour ₹5.50/Qtl (550 paisa)
      val input = SellMathInput(
          bags = 10,
          weightPerBagGrams = 50_000,
          ratePaisaPerQuintal = 220_000,
          billMode = BillMode.BILL_OF_SUPPLY,
          aratBps = 150,
          tulakPaisaOverride = null,
          receivedPaisa = 0,
          extraWeightGrams = 15_000,
          sellLabourPaisaPerQuintal = 550
      )
      val result = BuySellMath.calculateSell(input)
      assertEquals(0L, result.totalReceivablePaisa % 100L, "Total receivable must be whole rupee")
      assertEquals(
          result.totalReceivablePaisa,
          result.grossPaisa + result.aratPaisa + result.tulakPaisa + result.mandiShulkPaisa + result.labourPaisa,
          "Component sum must equal total receivable"
      )
  }
  ```

- [x] **Step 2: Run test to verify behavior**
  `./gradlew :core-domain:jvmTest --tests "com.appwork.mandisamiti.domain.BuySellMathTest"`

- [x] **Step 3: Update `BuySellMath.calculateSell` if necessary to ensure rounding half-up matches exact whole rupee formula**

---

### Task 1.2: Sell Entry Form State & UI Input Field
**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/sell/SellEntryFormState.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/sell/SellEntryScreen.kt`

- [x] **Step 1: Add `labourPerQuintal: String` to `SellEntryFormState`**
  - Initialize with `settings?.sellLabourPaisaPerQuintal?.let { MandiMathEngine.paisaToRupeesString(it) } ?: ""`
  - Add `onLabourPerQuintalChange(value: String)`
  - In `aggregatePreview`, parse `labourRatePaisa = MandiMathEngine.parseRupeesToPaisa(labourPerQuintal)` and pass to calculation.

- [x] **Step 2: Add Labour Input Field in `SellEntryScreen.kt`**
  - Place in the trade charges section alongside Commission and Tulak.
  - Label: `if (isEnglish) "Labour (₹/Quintal)" else "हम्माली / मजदूरी (₹/क्विंटल)"`
  - Keyboard: `MandiKeyboard.Decimal`
  - Single line: `singleLine = true`

- [x] **Step 3: Run JVM tests**
  `./gradlew :composeApp:jvmTest --tests "com.appwork.mandisamiti.ui.sell.*"`

---

## Phase 2: Buy Parchi Credit Jama in Daily Register

### Task 2.1: Record `BUY_CREDIT` Transaction on Buy Slip Save
**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/App.kt`

- [x] **Step 1: In `App.kt` Screen.BuyEntry checkout handler**
  - Calculate `unpaidCreditPaisa = entry.netPayablePaisa - entry.advancePaidPaisa - initialPaidPaisa`.
  - When `unpaidCreditPaisa > 0L`:
    - Insert a `CashTransaction`:
      ```kotlin
      val creditTx = CashTransaction(
          id = IdGenerator.newId(),
          shopId = entry.shopId,
          partyId = entry.farmerId,
          transactionType = TransactionType.JAMA_RECEIVED,
          amountPaisa = unpaidCreditPaisa,
          paymentMode = PaymentMode.BOOK_ENTRY,
          transactionDate = now,
          remarks = "${party?.name ?: "किसान"} • खरीद पर्ची #${entry.slipNo ?: ""} बाकी जमा (क्रेडिट)",
          createdAt = now,
          updatedAt = now,
          sourceEntryType = "BUY_CREDIT",
          sourceEntryId = entry.id
      )
      cashRepo.recordTransaction(creditTx)
      ```

---

### Task 2.2: Daily Register ViewModel & Screen Credit Support
**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/register/DailyRegisterViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/register/DailyCashRegisterScreen.kt`
- Create: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/register/DailyRegisterCreditJamaTest.kt`

- [x] **Step 1: Write unit test in `DailyRegisterCreditJamaTest.kt`**
  - Verify that a `BOOK_ENTRY` transaction with `TransactionType.JAMA_RECEIVED`:
    - Appears in `todayTransactions` under Jama.
    - Does **not** increase `inHandCashDrawerPaisa` (physical cash remains isolated).
    - Increases `totalJamaSidePaisa` (Credit In).

- [x] **Step 2: Update `DailyRegisterViewModel.kt`**
  - Ensure `CashDrawer.summarize` calculates `openingPaisa`, `cashInPaisa`, `cashOutPaisa`, and `closingPaisa` strictly on `paymentMode == PaymentMode.CASH`.
  - Add `todayCreditInPaisa: Long` to `DailyRegisterUiState` summing `BOOK_ENTRY` rows on the selected date.

- [x] **Step 3: Update `DailyCashRegisterScreen.kt`**
  - In `jamaList`, show items with `paymentMode == PaymentMode.BOOK_ENTRY`.
  - Render a badge: `[क्रेडिट / गैर-नकद]` with `MandiSlateBg` and `MandiTextSecondary`.
  - In the summary cards, display:
    - `रोकड़ आवक: ₹...`
    - `क्रेडिट जमा: ₹...`

- [x] **Step 4: Run unit tests**
  `./gradlew :composeApp:jvmTest --tests "com.appwork.mandisamiti.ui.register.*"`

---

## Phase 3: Buy Slip Multi-Day Payment History

### Task 3.1: Wire Itemized Payment History to Buy Slip Screen
**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/buy/BuyTransactionReceiptScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/App.kt`
- Create: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/buy/BuyReceiptPaymentHistoryTest.kt`

- [x] **Step 1: Add `payments: List<CashTransaction> = emptyList()` parameter to `BuyTransactionReceiptScreen`**
  - In `App.kt` for `Screen.BuyReceipt`:
    - Filter `allCashTransactions.filter { it.sourceEntryType == "BUY" && it.sourceEntryId == screen.id && !it.isDeleted && !it.isVoid && it.transactionType == TransactionType.UDHAR_GIVEN && it.paymentMode == PaymentMode.CASH }.sortedBy { it.transactionDate }`.
    - Pass this list to `BuyTransactionReceiptScreen`.

- [x] **Step 2: Render payment history rows in `BuyTransactionReceiptScreen.kt`**
  - Replace the single consolidated cash payment line with an iteration over `payments`:
    ```kotlin
    if (payments.isNotEmpty()) {
        ReceiptDivider()
        payments.forEach { p ->
            ReceiptAmountRow(
                label = "• नकद भुगतान · ${receiptDateShort(p.transactionDate)}",
                amountText = "− ₹${MandiMathEngine.paisaToRupeesString(p.amountPaisa)}"
            )
        }
        ReceiptDivider()
        if (leftPaisa > 0L) {
            ReceiptAmountRow(
                label = if (isEnglish) "Left" else "बाकी (शेष देय)",
                amountText = MandiMathEngine.formatRupees(leftPaisa),
                isBold = true
            )
        } else {
            ReceiptAmountRow(
                label = "स्थिति",
                amountText = "पूर्ण चुकता",
                isBold = true,
                textColor = MandiGreen
            )
        }
    }
    ```
  - When `leftPaisa == 0L`, hide the `गल्ला से नकद भुगतान करें →` button.

- [x] **Step 3: Run unit tests**
  `./gradlew :composeApp:jvmTest --tests "com.appwork.mandisamiti.ui.buy.*"`

---

## Phase 4: A5 Print & WhatsApp Share Alignment

### Task 4.1: Synchronize Print (A5) & WhatsApp Receipts
**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/slip/ReceiptHtmlRenderer.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/buy/BuyTransactionReceiptScreen.kt`
- Modify: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/slip/ReceiptHtmlRendererTest.kt`

- [x] **Step 1: Write unit tests in `ReceiptHtmlRendererTest.kt`**
  - Verify that Buy A5 receipt HTML:
    - Follows sequence: Gross $\rightarrow$ कुल खर्चा $\rightarrow$ पर्ची राशि $\rightarrow$ अग्रिम $\rightarrow$ उधार कटौती $\rightarrow$ अंतिम देय राशि $\rightarrow$ Payment timeline with dates $\rightarrow$ बाकी.
    - Contains combined "कुल खर्चा" row (`entry.labourPaisa + entry.commissionPaisa`).
    - Does **not** contain separate "मजदूरी" or "कमीशन" rows.
    - Contains separate date-stamped rows for each payment installment.

- [x] **Step 2: Update `ReceiptHtmlRenderer.renderBuyReceiptHtml`**
  - Accept `payments: List<CashTransaction> = emptyList()`.
  - Format combined deduction:
    ```html
    <tr>
      <td>कुल खर्चा:</td>
      <td class="text-right">- ₹${MandiMathEngine.paisaToRupeesString(totalDeductions)}</td>
    </tr>
    ```
  - Format each payment installment:
    ```html
    <tr>
      <td>• नकद भुगतान · ${formatDateShort(p.transactionDate)}:</td>
      <td class="text-right">- ₹${MandiMathEngine.paisaToRupeesString(p.amountPaisa)}</td>
    </tr>
    ```

- [x] **Step 3: Update `buildFarmerSlipText` in `BuyTransactionReceiptScreen.kt`**
  - Follow the exact same sequence in WhatsApp text:
    - Combined "कुल खर्चा: -₹..."
    - Payment timeline with dates: `• नकद भुगतान · 15 Nov: - ₹...`
    - Balance remaining or "पूर्ण चुकता".

- [x] **Step 4: Run unit tests**
  `./gradlew :composeApp:jvmTest --tests "com.appwork.mandisamiti.ui.slip.ReceiptHtmlRendererTest"`

---

## Phase 5: Verification & Delivery

### Task 5.1: Automated Test Suite Run
- [x] Run `./gradlew :core-domain:jvmTest :core-database:jvmTest :core-data:jvmTest :composeApp:jvmTest`.
- [x] Run backend tests: `cd backend && PYTHONPATH=. .venv/bin/pytest`.
- [x] Verify 100% test pass.

### Task 5.2: Live Verification on `emulator-5554`
- [x] Install debug APK: `./gradlew :composeApp:installDebug`.
- [x] **Verify Buy Slip #1 (Monty)**:
  - Open Slip #1: verify initial payment (15 Nov: ₹25,000) and remaining balance ₹24,000.
  - Tap "गल्ला से नकद भुगतान करें →", enter ₹12,000, save.
  - Verify Slip #1 appends:
    - `• नकद भुगतान · 15 Nov: − ₹25,000`
    - `• नकद भुगतान · 16 Nov: − ₹12,000`
    - `बाकी: ₹12,000`
  - Tap "प्रिंट करें": verify A5 print preview shows single "कुल खर्चा" row and both payment rows with dates.
  - Capture screenshot to `scratch/test_evidence/buy_slip_payment_timeline.png`.
- [x] **Verify Daily Register**:
  - Open Galla: verify ₹12,000 appears in **नामे (Cash Out)** for today.
  - Switch to 15 Nov: verify ₹24,000 appears in **जमा** with `[क्रेडिट]` badge.
  - Verify physical cash drawer is not inflated by credit.
  - Capture screenshot to `scratch/test_evidence/daily_register_credit_jama.png`.
- [x] **Verify Sell Parchi Labour Input**:
  - Open New Sell Parchi.
  - Verify "हम्माली / मजदूरी (₹/क्विंटल)" input field is present.
  - Enter decimal labour rate (e.g. `5.50`).
  - Save: verify bill total receivable is an exact whole rupee (zero paise).
  - Capture screenshot to `scratch/test_evidence/sell_decimal_labour_whole_rupee.png`.

### Task 5.3: Update Trackers & Git Commit
- [x] Update `PROGRESS.md` with implementation and verification details.
- [ ] Commit all changes cleanly following the repository guidelines.
