# MandiSamiti Backlog Features (v2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Milestone 5 (Receipt Printing on A5 via expect/actual `ReceiptPrinter` and UI Print buttons on every receipt) and Input Audit Group B (Lend, Expense, Stock, Daily Cash Register, and Settings keyboard/input ergonomics and validation).

**Architecture:** 
1. **Printing (M5)**: Clean multiplatform abstraction `ReceiptPrinter` (`expect` in commonMain, `actual` on Android using `PrintManager` + `WebView.createPrintDocumentAdapter`, `actual` on iOS using `UIPrintInteractionController` + `UIMarkupTextPrintFormatter`, and `actual` on JVM desktop as a test stub). HTML slip renderer producing standard A5 formatted print layout (`@page { size: A5 portrait; margin: 10mm; }`) with high contrast, shop header, GSTIN/licence, exact equation, and itemized charges. Print buttons paired with Share on all receipt screens.
2. **Input Audit Group B**: Shared `MandiKeyboard` presets (`Decimal`, `Whole`, `Name`, `Search`, `Note`, `Identifier`, etc.) applied to all input fields across Lend, Expense, Stock, Daily Cash Register, and Settings screens. Eliminates all High-severity letter keyboards on numeric/rate fields, enforces `singleLine = true`, adds `autoCorrect = false` on proper nouns/codes, and connects `imeAction` and `KeyboardActions`.

**Tech Stack:** Kotlin Multiplatform (Compose Multiplatform 1.6.11), Android PrintManager & WebView, iOS UIPrintInteractionController, SQLDelight, JUnit / Kotlin Test.

## Global Constraints
- **Integer Math:** All currency in `Long` paisa (1 INR = 100 paisa) and weights in `Long` grams (1 Qtl = 100,000 g). Zero float arithmetic.
- **NGDL v1.2 Tokens:** Use design tokens from `ui/theme/Color.kt`. High contrast (≥ 4.5:1), vector icons (`ImageVector`), zero unicode emojis in UI.
- **Paper Size:** A5 paper (`PrintAttributes.MediaSize.ISO_A5` on Android, A5 CSS `@page { size: A5; margin: 10mm; }`).
- **Offline-First:** No network dependency for receipt generation or printing.
- **Silent Testing:** No audio/soundbox TTS during testing.
- **Preserve Trackers:** Update only `status` and `evidence` in `FEATURES.json`; never delete or reword `id` or `description`.

---

## Completed in v1 (Verified & Passing)
- **M1: Labour Math**: Extra weight $\le 20\text{ kg}$ is free, $> 20\text{ kg}$ adds $+1\text{ bori}$ (`BuySellMath.EXTRA_BAG_THRESHOLD_GRAMS = 20_000L`). Trader sell labour charged per quintal and total rounded to nearest rupee.
- **M2: Sell Form & Stock Warning**: Trader labour row in Settings; per-quintal labour on sales; stock shortfall warning dialog on Sell save with override.
- **M3: Extra Weight on Bills**: Slip equations on Buy and Sell receipts itemize extra weight (`10 × 50 + 21 = 5.21 क्विंटल × ₹2,000`).
- **M4: Shop Profile Edit**: Editable shop name, owner name, mandi name, address, GSTIN, licence no. SQLite schema v13 migration `12.sqm`, backend Alembic `0006`, and bill headers.
- **Emulator Verification**: All 5 target features (`labour-half-unit-rule`, `bill-shows-extra-weight`, `sell-validates-stock`, `trader-sell-labour-per-quintal`, `asal-rename-and-opening-cash-chip`) verified live on `emulator-5554` with screenshots in `scratch/test_evidence/`.

---

## File Structure

```
composeApp/
├── src/commonMain/kotlin/com/appwork/mandisamiti/
│   ├── platform/
│   │   └── ReceiptPrinter.kt                        # [Create] expect class ReceiptPrinter
│   ├── ui/
│   │   ├── components/
│   │   │   ├── MandiKeyboard.kt                     # [Create] MandiKeyboard presets (Name, Note, Search, Decimal, etc.)
│   │   │   └── ReceiptParts.kt                      # [Modify] Add onPrint and Print button to ReceiptScaffold
│   │   ├── slip/
│   │   │   ├── ReceiptHtmlRenderer.kt               # [Create] A5 HTML receipt generator for Buy & Sell
│   │   │   └── ReceiptPreviewScreen.kt              # [Modify] Add Print button next to WhatsApp share
│   │   ├── buy/
│   │   │   └── BuyTransactionReceiptScreen.kt       # [Modify] Add Print button next to Share
│   │   ├── sell/
│   │   │   └── SellTransactionReceiptScreen.kt      # [Modify] Hook onPrint to ReceiptScaffold
│   │   ├── lend/
│   │   │   ├── LendEntryScreen.kt                   # [Modify] Input Audit Group B fixes
│   │   │   ├── LendTransactionsListScreen.kt        # [Modify] Search and return dialog input fixes
│   │   │   └── RepayLoanDialog.kt                   # [Modify] Decimal/Whole keyboard fixes
│   │   ├── expense/
│   │   │   └── ExpenseEntryScreen.kt                # [Modify] Input Audit Group B fixes
│   │   ├── stock/
│   │   │   └── StockRegisterScreen.kt               # [Modify] Search and adjustment dialog input fixes
│   │   ├── register/
│   │   │   └── DailyCashRegisterScreen.kt           # [Modify] Decimal keyboard on all cash in/out/closing dialogs
│   │   └── settings/
│   │       └── SettingsScreen.kt                    # [Modify] Decimal keyboard on commission & labour rates
│   └── App.kt                                       # [Modify] Pass ReceiptPrinter to screens
├── src/androidMain/kotlin/com/appwork/mandisamiti/
│   ├── platform/
│   │   └── ReceiptPrinter.android.kt                # [Create] actual class ReceiptPrinter using PrintManager + WebView
│   └── MainActivity.kt                              # [Modify] Instantiate and pass ReceiptPrinter
├── src/iosMain/kotlin/com/appwork/mandisamiti/
│   └── platform/
│       └── ReceiptPrinter.ios.kt                    # [Create] actual class ReceiptPrinter using UIPrintInteractionController
├── src/jvmMain/kotlin/com/appwork/mandisamiti/
│   ├── platform/
│   │   └── ReceiptPrinter.jvm.kt                    # [Create] actual class ReceiptPrinter stub
│   └── main.kt                                      # [Modify] Instantiate ReceiptPrinter for JVM
└── src/jvmTest/kotlin/com/appwork/mandisamiti/
    ├── ui/slip/
    │   └── ReceiptHtmlRendererTest.kt               # [Create] Verify A5 HTML generation and math formatting
    ├── ui/print/
    │   └── ReceiptPrintIntegrationTest.kt           # [Create] Test receipt print callbacks and HTML outputs
    └── audit/
        └── GroupBInputAuditTest.kt                  # [Create] Unit tests verifying keyboardOptions across Group B screens
```

---

## Phase 1 (P1): Print (M5)

### Task 1.1: Platform `ReceiptPrinter` expect/actual Abstraction
**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/platform/ReceiptPrinter.kt`
- Create: `composeApp/src/androidMain/kotlin/com/appwork/mandisamiti/platform/ReceiptPrinter.android.kt`
- Create: `composeApp/src/iosMain/kotlin/com/appwork/mandisamiti/platform/ReceiptPrinter.ios.kt`
- Create: `composeApp/src/jvmMain/kotlin/com/appwork/mandisamiti/platform/ReceiptPrinter.jvm.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/appwork/mandisamiti/MainActivity.kt`
- Modify: `composeApp/src/jvmMain/kotlin/com/appwork/mandisamiti/main.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/App.kt`

**Interfaces:**
- Produces: `ReceiptPrinter.printHtml(jobName: String, htmlContent: String)`
- Consumes: Platform context on Android (`Context.PRINT_SERVICE`), `UIPrintInteractionController` on iOS.

- [ ] **Step 1: Write common expect class `ReceiptPrinter`**
  ```kotlin
  package com.appwork.mandisamiti.platform

  expect class ReceiptPrinter {
      fun printHtml(jobName: String, htmlContent: String)
  }
  ```

- [ ] **Step 2: Implement Android `actual class ReceiptPrinter`**
  - Use `android.print.PrintManager` and a hidden `android.webkit.WebView`.
  - On `onPageFinished`, create print adapter via `webView.createPrintDocumentAdapter(jobName)`.
  - Configure `PrintAttributes` with `PrintAttributes.MediaSize.ISO_A5`.
  - Handle main-thread dispatching safely.

- [ ] **Step 3: Implement iOS `actual class ReceiptPrinter`**
  - Use `platform.UIKit.UIPrintInteractionController.sharedPrintController()`.
  - Set `UIMarkupTextPrintFormatter(htmlContent)`.
  - Present print sheet animated.

- [ ] **Step 4: Implement JVM `actual class ReceiptPrinter`**
  - Provide a safe stub for JVM/desktop and tests (`println` logging or capturing last printed HTML for test assertions).

- [ ] **Step 5: Instantiate and wire in `MainActivity.kt`, `main.kt`, and `App.kt`**
  - Pass `receiptPrinter: ReceiptPrinter` into `App(...)`.

- [ ] **Step 6: Verify compilation across platforms**
  - Run `./gradlew :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`.

---

### Task 1.2: A5 HTML Slip Renderer & Unit Tests
**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/slip/ReceiptHtmlRenderer.kt`
- Create: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/slip/ReceiptHtmlRendererTest.kt`

**Interfaces:**
- Produces: `ReceiptHtmlRenderer.renderBuyReceiptHtml(entry: BuyEntry, shop: ShopProfile, farmerName: String): String`
- Produces: `ReceiptHtmlRenderer.renderSellReceiptHtml(entry: SellEntry, shop: ShopProfile, buyerName: String): String`

- [ ] **Step 1: Write unit tests in `ReceiptHtmlRendererTest.kt`**
  - Test Buy receipt HTML includes shop name, GSTIN, licence, equation `10 × 50 + 21 = 5.21 क्विंटल × ₹2,000`, gross, deductions, and A5 CSS `@page { size: A5 portrait; }`.
  - Test Sell receipt HTML includes trader labour per quintal and whole-rupee total.
  - Run test to ensure it fails before implementation.

- [ ] **Step 2: Implement `ReceiptHtmlRenderer.kt`**
  - Clean HTML5 template with inline CSS for print styling:
    ```html
    <style>
      @page { size: A5 portrait; margin: 10mm; }
      body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; font-size: 13px; color: #111; }
      .header { text-align: center; border-bottom: 2px solid #000; padding-bottom: 6px; margin-bottom: 8px; }
      .shop-name { font-size: 18px; font-weight: bold; }
      .details-table, .items-table { width: 100%; border-collapse: collapse; margin-bottom: 8px; }
      .items-table th, .items-table td { border: 1px solid #ddd; padding: 4px 6px; text-align: left; }
      .total-row { font-size: 15px; font-weight: bold; background: #f0f0f0; }
    </style>
    ```
  - Map `BuyEntry` and `SellEntry` fields into the table template.

- [ ] **Step 3: Run unit tests and confirm pass**
  - Run `./gradlew :composeApp:jvmTest --tests "com.appwork.mandisamiti.ui.slip.ReceiptHtmlRendererTest"`.

---

### Task 1.3: Print UI Buttons on Receipt Screens
**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/components/ReceiptParts.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/buy/BuyTransactionReceiptScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/sell/SellTransactionReceiptScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/slip/ReceiptPreviewScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/App.kt`

**Interfaces:**
- `ReceiptScaffold`: Add `onPrint: (() -> Unit)? = null` parameter; render a secondary/primary Print button (`Icons.Default.Print`) beside Share.
- `BuyTransactionReceiptScreen`: Add Print button in bottom action bar.
- `SellTransactionReceiptScreen`: Wire `onPrint` callback to print bill HTML.

- [ ] **Step 1: Update `ReceiptParts.kt` (`ReceiptScaffold`)**
  - Add `onPrint: (() -> Unit)? = null` and `printLabel: String = "प्रिंट करें"`.
  - Place Print button next to Share in a two-column `Row` or stacked bar with vector icon `Icons.Default.Print`.

- [ ] **Step 2: Update `BuyTransactionReceiptScreen.kt`**
  - In bottom action bar, add Print button calling `onPrint(slipHtml)`.

- [ ] **Step 3: Update `SellTransactionReceiptScreen.kt`**
  - Pass `onPrint = { onPrint(billHtml) }` to `ReceiptScaffold`.

- [ ] **Step 4: Update `ReceiptPreviewScreen.kt`**
  - Add Print button in the action row.

- [ ] **Step 5: Wire print lambdas in `App.kt`**
  - Connect screen `onPrint` events to `receiptPrinter.printHtml(jobName, html)`.

- [ ] **Step 6: Write integration test in `ReceiptPrintIntegrationTest.kt`**
  - Verify that invoking the print callbacks passes non-empty HTML containing A5 CSS and correct amounts.
  - Run `./gradlew :composeApp:jvmTest --tests "com.appwork.mandisamiti.ui.print.*"`.

---

## Phase 2 (P2): Input Audit Group B

### Task 2.1: Shared Keyboard Presets (`MandiKeyboard.kt`)
**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/components/MandiKeyboard.kt`

- [ ] **Step 1: Define `MandiKeyboard` presets object**
  ```kotlin
  package com.appwork.mandisamiti.ui.components

  import androidx.compose.foundation.text.KeyboardOptions
  import androidx.compose.ui.text.input.ImeAction
  import androidx.compose.ui.text.input.KeyboardCapitalization
  import androidx.compose.ui.text.input.KeyboardType

  object MandiKeyboard {
      val Name = KeyboardOptions(
          capitalization = KeyboardCapitalization.Words,
          autoCorrect = false,
          keyboardType = KeyboardType.Text,
          imeAction = ImeAction.Next
      )
      val Note = KeyboardOptions(
          capitalization = KeyboardCapitalization.Sentences,
          autoCorrect = true,
          keyboardType = KeyboardType.Text,
          imeAction = ImeAction.Default
      )
      val Search = KeyboardOptions(
          capitalization = KeyboardCapitalization.None,
          autoCorrect = false,
          keyboardType = KeyboardType.Text,
          imeAction = ImeAction.Search
      )
      val Phone = KeyboardOptions(
          keyboardType = KeyboardType.Phone,
          imeAction = ImeAction.Next
      )
      val Pin = KeyboardOptions(
          keyboardType = KeyboardType.NumberPassword,
          imeAction = ImeAction.Done
      )
      val Identifier = KeyboardOptions(
          capitalization = KeyboardCapitalization.Characters,
          autoCorrect = false,
          keyboardType = KeyboardType.Ascii,
          imeAction = ImeAction.Next
      )
      val Whole = KeyboardOptions(
          keyboardType = KeyboardType.Number,
          imeAction = ImeAction.Next
      )
      val Decimal = KeyboardOptions(
          keyboardType = KeyboardType.Decimal,
          imeAction = ImeAction.Next
      )
  }
  ```

---

### Task 2.2: Settings Screen Input Audit
**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/settings/SettingsScreen.kt`

**Audit Target (Settings):**
- Fix High Severity: Commission and labour fields in `TradeSettingsDialog` currently have no `keyboardType` (letters keyboard opens for numeric input).
- Rates: Farmer Commission, Merchant Commission, Customer Commission, Labour Charge (₹ per bag), Trader Labour (₹ per quintal) $\rightarrow$ `MandiKeyboard.Decimal`.
- Edit Shop Profile dialog: Shop Name, Owner Name, Mandi Name, Address $\rightarrow$ `MandiKeyboard.Name`; GSTIN and Licence No $\rightarrow$ `MandiKeyboard.Identifier` (Ascii, autoCorrect=false, uppercase).
- All single-line fields have `singleLine = true`.

- [ ] **Step 1: Apply `MandiKeyboard.Decimal` to all 5 rate inputs in `SettingsScreen.kt`**
- [ ] **Step 2: Apply `MandiKeyboard.Identifier` to GSTIN & licence fields in Shop Profile edit dialog**
- [ ] **Step 3: Verify with unit test in `SettingsShopProfileEditTest.kt`**

---

### Task 2.3: Daily Cash Register Input Audit
**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/register/DailyCashRegisterScreen.kt`

**Audit Target (Daily Cash Register):**
- Fix High Severity:
  - Cash In (जमा) amount: opened letter keyboard $\rightarrow$ set `MandiKeyboard.Decimal`.
  - Cash Out (निकासी) amount: opened letter keyboard $\rightarrow$ set `MandiKeyboard.Decimal`.
  - Cash to Home (drawings) amount: opened letter keyboard $\rightarrow$ set `MandiKeyboard.Decimal`.
  - Opening cash float amount: set `MandiKeyboard.Decimal`.
- Denomination counts: `MandiKeyboard.Whole`.
- Physical cash count: `MandiKeyboard.Whole`.
- Reason / Note fields: `MandiKeyboard.Note`, `singleLine = true`.

- [ ] **Step 1: Update Cash In & Cash Out dialogs with `MandiKeyboard.Decimal` and `singleLine = true`**
- [ ] **Step 2: Update Opening Cash Float & Cash to Home dialogs with `MandiKeyboard.Decimal`**
- [ ] **Step 3: Update Day Close denomination counts with `MandiKeyboard.Whole`**
- [ ] **Step 4: Verify with unit tests in `DailyRegisterViewModelTest.kt`**

---

### Task 2.4: Lend & Repay Input Audit
**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/lend/LendEntryScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/lend/LendTransactionsListScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/ledger/RepayLoanDialog.kt`

**Audit Target (Lend & Repay):**
- `LendEntryScreen.kt`:
  - Loan Amount: `singleLine = true`, `MandiKeyboard.Decimal`.
  - Crop Name: `singleLine = true`, `MandiKeyboard.Name`.
  - Bags Count: `singleLine = true`, `MandiKeyboard.Whole`.
  - Weight: `singleLine = true`, `MandiKeyboard.Decimal`.
  - Notes: `minLines = 2`, `maxLines = 4`, `MandiKeyboard.Note`.
- `LendTransactionsListScreen.kt`:
  - Search box: `singleLine = true`, `MandiKeyboard.Search`, `KeyboardActions(onSearch = { keyboardController?.hide() })`.
  - Return amount: `singleLine = true`, `MandiKeyboard.Decimal`.
  - Return bags: `singleLine = true`, `MandiKeyboard.Whole`.
  - Return weight: `singleLine = true`, `MandiKeyboard.Decimal`.
- `RepayLoanDialog.kt`:
  - Amount received: `singleLine = true`, `MandiKeyboard.Decimal`, `imeAction = ImeAction.Next`.
  - Interest: `singleLine = true`, `MandiKeyboard.Decimal`, `imeAction = ImeAction.Done`.

- [ ] **Step 1: Update `LendEntryScreen.kt` fields with `singleLine = true` and `MandiKeyboard` presets**
- [ ] **Step 2: Update `LendTransactionsListScreen.kt` search box and return dialogs**
- [ ] **Step 3: Update `RepayLoanDialog.kt` amount and interest inputs**
- [ ] **Step 4: Verify with `LendFlowTest.kt` and `RepayPreviewTest.kt`**

---

### Task 2.5: Expense & Stock Input Audit
**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/expense/ExpenseEntryScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/stock/StockRegisterScreen.kt`

**Audit Target (Expense & Stock):**
- `ExpenseEntryScreen.kt`:
  - Expense Amount: `singleLine = true`, `MandiKeyboard.Decimal`, `imeAction = ImeAction.Next`.
  - Paid To: `singleLine = true`, `MandiKeyboard.Name`, `imeAction = ImeAction.Next`.
  - Remarks: `minLines = 2`, `maxLines = 4`, `MandiKeyboard.Note`, `imeAction = ImeAction.Done`.
- `StockRegisterScreen.kt`:
  - Godown search: `singleLine = true`, `MandiKeyboard.Search`, `KeyboardActions(onSearch = { keyboardController?.hide() })`.
  - Stock adjustment dialog (bags & weight): `singleLine = true`, `MandiKeyboard.Decimal`.

- [ ] **Step 1: Update `ExpenseEntryScreen.kt` with `singleLine = true` and `MandiKeyboard` presets**
- [ ] **Step 2: Update `StockRegisterScreen.kt` search and adjustment dialog inputs**
- [ ] **Step 3: Add `GroupBInputAuditTest.kt` verifying all audited fields reject multi-line Enter and specify numeric/decimal types**
- [ ] **Step 4: Run `./gradlew :composeApp:jvmTest`**

---

## Phase 3: Verification & Emulator Delivery

### Task 3.1: Full Automated Test Suite Verification
- [ ] Run `./gradlew :core-domain:jvmTest :core-database:jvmTest :core-data:jvmTest :composeApp:jvmTest`.
- [ ] Run backend pytest: `cd backend && PYTHONPATH=. .venv/bin/pytest`.
- [ ] Confirm all tests pass.

### Task 3.2: Live Emulator Verification (`emulator-5554`)
- [ ] Build & install debug APK: `./gradlew :composeApp:installDebug`.
- [ ] **Verify Print (M5)**:
  - Open Buy Slip #2. Verify **"प्रिंट करें"** button is present beside Share. Tap it; confirm Android print framework dialog appears configured for A5.
  - Open Sell Bill #1. Verify **"प्रिंट करें"** button is present beside Share. Tap it; confirm Android print framework dialog appears.
  - Capture screenshot of print dialog.
- [ ] **Verify Input Audit Group B**:
  - Open Settings $\rightarrow$ tap "दरें बदलें": verify numeric keypad opens for rates (not letters keyboard).
  - Open Galla $\rightarrow$ tap "नकद जमा": verify numeric decimal keypad opens.
  - Open Lend $\rightarrow$ tap New Loan: verify single-line numeric inputs.
  - Open Expense $\rightarrow$ verify amount opens Decimal keypad.
  - Open Godown $\rightarrow$ verify search box has Search IME action.
  - Capture layout dumps and screenshots.

### Task 3.3: Sync Trackers & Commit
- [ ] Update `FEATURES.json`:
  - `print-all-bills`: update status to `passing` with emulator evidence.
  - `input-audit-medium-low`: update evidence with Group B audited screens.
- [ ] Append entry to `PROGRESS.md` with test evidence, numbers, and screenshot references.
- [ ] Git commit cleanly following CLAUDE.md contract.
