# Khata & Cash Register Alignment with Physical Bahi Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align MandiSamiti's Cash Register and Ledger tabs with the authentic two-column paper rokad bahi page (`docs/reference/original-khata-page.jpg`) across 7 sequential commits on branch `feat/dashboard-review-fixes` with rigorous TDD and NGDL compliance.

**Architecture:** 
1. Auto-provision and synchronize `partyEntity` accounts from `BuyEntry`, `SellEntry`, and `LendEntry` transactions, computing real running balances.
2. Implement true two-column reconciliation in `DailyRegisterViewModel` comparing physical cash count against computed book closing.
3. Handle negative cash states with standard currency formatting (`-₹X`) and opening float alerts.
4. Redesign `DailyCashRegisterScreen` layout: 1-row compact hero, sticky bottom action bar, overflow menu, and `weight(1f)` bahi container showing >= 8 rows per column at 390dp.
5. Elevate row hierarchy: Party Name primary (16sp bold), category fallback (never "खाता"), 14sp floor, and `Jama (Cash In)` / `Naame (Cash Out)` labels.
6. Daily dated navigation with prev/next arrows, date picker dialog, and read-only past day view.
7. Outlier rate interceptor (> ₹20,000/qtl or > 5x moving avg) and village string digit cleaner.

**Tech Stack:** Kotlin Multiplatform (Compose Multiplatform), SQLDelight SQLite, Kotlinx Coroutines/Flow, NGDL design system tokens.

---

### Task 1: Commit 1: Auto-Provision Party Accounts for Buy, Sell, and Lend Entries

**Files:**
- Modify: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/repository/OfflineFirstBuyRepository.kt`
- Modify: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/repository/OfflineFirstSellRepository.kt`
- Modify: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/repository/OfflineFirstLendRepository.kt`
- Modify: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/Party.kt`
- Create/Modify: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/repository/PartyAccountProvisioningTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/MainActivity.kt` (startup backfill for historical unlinked records)

- [ ] **Step 1: Root-Cause Investigation**
  - Verify how `saveBuyEntry`, `saveSellEntry`, and `saveLendEntry` store party identifiers.
  - Root cause confirmed: Forms pass raw names as IDs without creating records in `partyEntity`. Ledger and Dashboard read strictly from `partyEntity`, yielding 0 accounts.
- [ ] **Step 2: Write Failing Integration Test**
  - In `PartyAccountProvisioningTest.kt`, test saving a Buy entry creates `Party(name = "राधे श्याम", partyType = FARMER)` and `getPartyBalanceStream` returns `-3758000L` (Net payable).
  - Test saving a Sell entry creates `Party(name = "सुनील ट्रेडर्स", partyType = BUYER)` with receivable balance.
  - Test saving a Lend entry creates `Party(name = "कैलाश किसान", partyType = FARMER)` with receivable balance.
- [ ] **Step 3: Implement Auto-Provisioning & Startup Backfill**
  - Add helper in repositories to upsert party into `partyEntity` on transaction save.
  - Add `backfillPartiesFromTransactions(database)` to ensure existing entries on device (e.g. RadheyShyam ₹37,580 due) populate `partyEntity` safely.
- [ ] **Step 4: Run Tests**
  - Verify all tests pass in `:core-data:jvmTest` and `:composeApp:jvmTest`.
- [ ] **Step 5: Commit 1**
  - Commit: `fix(party): auto-provision party ledger accounts on buy, sell, and lend entries`

---

### Task 2: Commit 2: Physical Cash Reconciliation ("Balanced" Badge)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/register/DailyRegisterViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/register/DailyCashRegisterScreen.kt`
- Modify: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/register/DailyRegisterViewModelTest.kt`
- Modify: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/register/GallaFlowTest.kt`

- [ ] **Step 1: Write Failing Tests for Reconciliation Badge**
  - Test: When `physicalCountPaisa == null`, badge state is `NOT_COUNTED` ("गिनती बाकी", Neutral Grey).
  - Test: When `physicalCountPaisa == bookClosingPaisa`, badge state is `BALANCED` ("जोड़ बराबर", Green).
  - Test: When `physicalCountPaisa != bookClosingPaisa`, badge state is `DISCREPANCY` ("अंतर: ₹X", Amber).
- [ ] **Step 2: Implement Reconciliation Logic in ViewModel & UI**
  - In `DailyRegisterViewModel`, decouple right column closing from theoretical formula when physical count is present.
  - Update `BalancedBadge` in `DailyCashRegisterScreen.kt` to reflect the three states with NGDL tokens (`MandiNeutralLight`, `MandiGreenLight`, `MandiAmberLight`).
- [ ] **Step 3: Run Tests**
  - Verify `GallaFlowTest` and `DailyRegisterViewModelTest` pass.
- [ ] **Step 4: Commit 2**
  - Commit: `feat(galla): implement authentic physical cash reconciliation and honest tally badge`

---

### Task 3: Commit 3: Negative Cash State Warning & Currency Formatting

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/domain/math/MandiCurrencyFormatter.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/domain/math/MandiMathEngine.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/register/DailyCashRegisterScreen.kt`
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/domain/math/MandiCurrencyFormatterTest.kt`

- [ ] **Step 1: Write Failing Unit Tests for Negative Currency Formatting**
  - Assert negative amounts format as `"-₹11,500"` (never `"₹-11,500"`).
- [ ] **Step 2: Update Formatters and Add Amber Negative Cash Banner**
  - Fix any prefixing bugs in `MandiCurrencyFormatter` and `MandiMathEngine`.
  - In `DailyCashRegisterScreen.kt`, when `inHandCashDrawerPaisa < 0`, render the amber alert banner with CTA to open Opening Float dialog.
- [ ] **Step 3: Run Tests & Verify on Device**
  - Run `:composeApp:jvmTest`.
- [ ] **Step 4: Commit 3**
  - Commit: `fix(galla): format negative currency as -₹X and add missing opening float alert`

---

### Task 4: Commit 4: Screen Space Optimization for High Bahi Density (390dp Target)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/register/DailyCashRegisterScreen.kt`

- [ ] **Step 1: Redesign Hero to 1 Compact Row**
  - Single row displaying Closing Cash prominently, flanked by compact Opening, In, and Out figures.
- [ ] **Step 2: Realine Action Bar & Overflow Menu**
  - Move "Cash In" and "Cash Out" to sticky bottom bar.
  - Relocate "Set Opening Float" and "Cash to Home" into a dropdown/overflow menu.
- [ ] **Step 3: Ensure >= 8 Visible Rows in Bahi Container**
  - Set bahi container `Modifier.weight(1f)` with zero horizontal scroll at 390dp.
- [ ] **Step 4: Device Screencap Verification**
  - Capture 1080x2340 screen on Redmi Note 7 to confirm 8+ rows visible.
- [ ] **Step 5: Commit 4**
  - Commit: `style(galla): compact 1-row hero, bottom action bar, and 8+ row visible bahi density`

---

### Task 5: Commit 5: Row Typography, Information Hierarchy, and Localization

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/register/DailyCashRegisterScreen.kt`

- [ ] **Step 1: Implement Row Hierarchy**
  - Line 1: Party Name (Bold, 16sp). Fallback to transaction category (e.g. "खर्च: हम्माली"), never generic "खाता".
  - Line 2: Village / Remarks (Muted, 14sp).
  - Trailing: Amount (Bold, 16sp).
- [ ] **Step 2: Authentic Bahi Terminology**
  - Hindi: `जमा` (Cash In) and `नामे` (Cash Out).
  - English: `Jama (Cash In)` and `Naame (Cash Out)`.
- [ ] **Step 3: Full Bilingual & Contrast Audit**
  - Header sound toggle: "Mute" / "म्यूट". Contrast >= 4.5:1.
- [ ] **Step 4: Commit 5**
  - Commit: `style(galla): party name priority, authentic bahi terminology, and 14sp typography floor`

---

### Task 6: Commit 6: Date Navigation and Historical Page Archival

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/register/DailyRegisterViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/register/DailyCashRegisterScreen.kt`
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/register/DailyRegisterViewModelTest.kt`

- [ ] **Step 1: Write Failing Tests for Date Navigation**
  - Test selecting past date loads transactions and opening balance for that day.
  - Test past date state is flagged as read-only.
- [ ] **Step 2: Implement Date Bar & Read-Only Banner**
  - Sticky date bar with `[Prev]` Date `[Next]` (disabled on today).
  - Date picker dialog trigger.
  - Read-only banner on past dates: `"पिछला दिन, सिर्फ देखने के लिए"` / `"Past date, view-only"`.
- [ ] **Step 3: Run Tests**
  - Verify `:composeApp:jvmTest`.
- [ ] **Step 4: Commit 6**
  - Commit: `feat(galla): daily dated bahi navigation and historical view-only archive`

---

### Task 7: Commit 7: Validation Guards for Commodity Rates and Address Fields

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/buy/BuyEntryScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/buy/BuyEntryViewModel.kt`
- Create/Modify: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/buy/BuyValidationTest.kt`

- [ ] **Step 1: Write Failing Validation Tests**
  - Test rate > ₹20,000/qtl triggers confirmation prompt requirement.
  - Test village string sanitization strips stray numeric noise (e.g. `"Khedae10n2800"` -> `"Khedae"`).
- [ ] **Step 2: Implement Rate Safeguard & Village Sanitizer**
  - Add `MAX_STANDARD_RATE_PER_QUINTAL = 20_000_00L`.
  - Add confirmation dialog in `BuyEntryScreen.kt`.
  - Sanitize village inputs while preserving valid postal address formatting.
- [ ] **Step 3: Run Tests & Verification**
  - Verify all test suites pass.
- [ ] **Step 4: Commit 7**
  - Commit: `feat(buy): rate outlier confirmation guard and village input sanitization`

---

### Task 8: Verification, Device Screencaps & Project Update
- Capture screenshots for each commit in `docs/testing/evidence/galla/`.
- Run `python3 tests/ngdl-tokens/test_token_parity.py` and `./gradlew test`.
- Run `python3 scripts/project_update.py`.
