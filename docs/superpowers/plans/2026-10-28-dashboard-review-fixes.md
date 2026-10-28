# Dashboard Review Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the 7 prioritized dashboard review issues on single branch `feat/dashboard-review-fixes` for MandiSamiti (data bug root cause, bilingual toggle parity, NGDL token rendering, accounting color correction, readability floor, honest number formatting, and skeleton/empty states) with separate reviewable commits.

**Architecture:** Rectify financial data aggregation in `HomeViewModel` to account for active lends and unlinked parties; extend `RevampKpiGrid` and stock cards with full Hindi/English parity; replace hardcoded hex and alpha-blends with opaque NGDL tokens and elevated surfaces; invert semantic color tokens (`MandiGreenReceivable` for cash in / लेना है, `MandiRedPayable` for cash out / देना है); elevate typography to >= 14sp; format large numbers with compact Indian currency scales; and provide explicit empty and skeleton states.

**Tech Stack:** Kotlin Multiplatform (Compose Multiplatform), SQLDelight SQLite, Kotlinx Coroutines/Flow, NGDL design system tokens.

## Global Constraints
- Target device: Redmi Note 7 (1080x2340, Android 10, ADB device `d93c743`).
- Single git branch: `feat/dashboard-review-fixes`.
- Commit discipline: Exactly 7 commits in the exact order specified, each independently testable and green.
- NGDL §2.1 & §5: Tokens only from spec, contrast >= 4.5:1, elevation on all cards, vector icons only (no emojis), 0 horizontal scroll at 390px.
- TDD required: Failing test first for every testable unit.

---

### Task 1: Commit 1: Data Bug (Root Cause: Lends unlinked & ignored in receivables aggregation)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/home/HomeViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dashboard/DashboardScreen.kt`
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/home/HomeViewModelTest.kt`

**Interfaces:**
- `HomeUiState.totalMarketReceivablePaisa`: Net receivable including active unlinked lends.
- `HomeUiState.unlinkedLendAmountPaisa`: Sum of active lends not linked to any registered party in `partyEntity`.
- `HomeUiState.unlinkedLendCount`: Count of unlinked active lend entries.

- [ ] **Step 1: Write the failing test in HomeViewModelTest.kt**
  Write `testLendEntryReflectedInReceivablesAndHonestSubtitleWhenUnlinked()`:
  Setup `HomeViewModel` with 1 lend entry for ₹10,000 (`1_000_000L`) and 0 registered parties.
  Assert `state.kpis.totalLendPaisa == 1_000_000L`, `state.totalMarketReceivablePaisa == 1_000_000L`, and `state.unlinkedLendAmountPaisa == 1_000_000L`.

- [ ] **Step 2: Run test to confirm failure**
  Run `./gradlew :composeApp:testDebugUnitTest --tests com.appwork.mandisamiti.ui.home.HomeViewModelTest.testLendEntryReflectedInReceivablesAndHonestSubtitleWhenUnlinked`.

- [ ] **Step 3: Implement the root cause fix in HomeViewModel and DashboardScreen**
  In `HomeViewModel.observeDashboardData()`, combine `lendsFlow` with `observeParties` state to calculate total market receivables and track unlinked lends.
  In `DashboardScreen.kt`, update the metric subtitle to show `"₹10,000 दिया, कोई खाता लिंक नहीं"` / `"₹10,000 given, no party linked"` when `receivableCount == 0 && totalMarketReceivablePaisa > 0`.

- [ ] **Step 4: Run tests to confirm passing**
  Run `./gradlew test` in `projects/MandiSamiti`.

- [ ] **Step 5: Commit 1**
  Commit message: `fix(dashboard): incorporate lends into market receivables with honest unlinked state`

---

### Task 2: Commit 2: Language Toggle Gaps (Bilingual parity across all Dashboard components)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dashboard/DashboardRevampComponents.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dashboard/DashboardScreen.kt`
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/dashboard/DashboardBilingualTest.kt`

**Interfaces:**
- `RevampKpiGrid(kpis, isEnglish, ...)`
- `GodownStockSummaryCard(stockLines, isEnglish, ...)`
- Quick action sub-labels: "Buy / from farmer" and "Sell / to buyer" in English mode.

- [ ] **Step 1: Write failing test in DashboardBilingualTest.kt**
  Verify that when `isEnglish = true`, `RevampKpiGrid` and `GodownStockSummaryCard` labels contain 0 Devanagari characters (`\u0900-\u097F`).

- [ ] **Step 2: Run test to confirm failure**
  Run `./gradlew :composeApp:testDebugUnitTest --tests com.appwork.mandisamiti.ui.dashboard.DashboardBilingualTest`.

- [ ] **Step 3: Pass isEnglish through and update all hardcoded strings**
  Update `RevampKpiGrid` to render English labels ("Total Purchases", "Total Sales", "Total Outstanding (Lend)", "Total Expenses") and Hindi labels.
  Update `GodownStockSummaryCard` ("Godown Stock", "No stock recorded", "Oversold warning") and header sound toggle ("Mute" / "म्यूट").
  Update quick action sub-labels to "Buy / from farmer" and "Sell / to buyer".

- [ ] **Step 4: Run tests to confirm passing**
  Run `./gradlew test` in `projects/MandiSamiti`.

- [ ] **Step 5: Commit 2**
  Commit message: `feat(dashboard): full bilingual parity and conversational english sublabels`

---

### Task 3: Commit 3: Action Tile Rendering and Colours

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/theme/Color.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dashboard/DashboardScreen.kt`

**Interfaces:**
- Named tokens: `MandiBlueSell`, `MandiOrangeLend`, `MandiPinkExpense`, `MandiGreenBuy`.
- Function: `opaqueTint(accent: Color, background: Color, alpha: Float = 0.12f): Color`.
- Content description on action icons set to `null`.

- [ ] **Step 1: Add tokens and opaque tint helper in Color.kt**
  Define `MandiBlueSell = Color(0xFF2563EB)`, `MandiOrangeLend = Color(0xFFD97706)`, `MandiPinkExpense = Color(0xFFDB2777)`, `MandiGreenBuy = Color(0xFF16A34A)`.
  Add `opaqueTint` compositing function so elevation shadows don't bleed through transparent cards.

- [ ] **Step 2: Update QuickActionButton in DashboardScreen.kt**
  Replace hardcoded hex `Color(0xFFF57C00)` etc. with named tokens.
  Use `opaqueTint(...)` for `containerColor` instead of raw `.copy(alpha = 0.12f)` over elevation.
  Verify sub-label contrast >= 4.5:1 against the composited background.
  Set icon `contentDescription = null`.

- [ ] **Step 3: Verify NGDL token parity**
  Run `python3 tests/ngdl-tokens/test_token_parity.py`.

- [ ] **Step 4: Commit 3**
  Commit message: `fix(dashboard): use opaque NGDL surface tints, named tokens, and 4.5:1 contrast`

---

### Task 4: Commit 4: Colour Meaning Inversion (Green = Receivable, Red = Payable)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/theme/Color.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/theme/Theme.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dashboard/DashboardScreen.kt`
- Modify: Callers in `DailyCashRegisterScreen.kt`, `PartyCard.kt`, `ReceiptPreviewScreen.kt`

**Interfaces:**
- `MandiGreenReceivable`: Color(0xFF16A34A) (Money coming in / लेना है)
- `MandiRedPayable`: Color(0xFFDC2626) (Money going out / देना है)

- [ ] **Step 1: Invert color semantics in Color.kt and Theme.kt**
  Rename and align:
  Receivable (money owed to trader) = Green (`#16A34A`).
  Payable (money trader owes) = Red (`#DC2626`).

- [ ] **Step 2: Update DashboardScreen.kt metric cards**
  "Receivable (Market)" -> `MandiGreenReceivable` with `TrendingUp`.
  "Payable (Farmers)" -> `MandiRedPayable` with `TrendingDown`.

- [ ] **Step 3: Audit and update all callers across the codebase**
  Grep all occurrences of `MandiRedReceivable` and `MandiGreenPayable` to ensure no conflicting semantics remain in Cash Drawer or Ledger.

- [ ] **Step 4: Run tests to confirm passing**
  Run `./gradlew test` in `projects/MandiSamiti`.

- [ ] **Step 5: Commit 4**
  Commit message: `fix(theme): invert financial color semantics to green for receivable and red for payable`

---

### Task 5: Commit 5: Readability (Floor of 14sp, 16sp for Actionable Labels)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dashboard/DashboardScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dashboard/DashboardRevampComponents.kt`

**Typography Floor:**
- Tile label: 16sp
- Tile sub-label: 14sp (was 10sp)
- KPI label: 14sp (was 12sp)
- KPI amount: 22sp (was titleMedium ~16sp)
- Section title and "View All": 16sp (was 14sp / 12sp)
- Metric card subtitle: 14sp (was 11sp)

- [ ] **Step 1: Update typography scales in DashboardScreen.kt and DashboardRevampComponents.kt**
  Update all text styles to meet or exceed 14sp floor, and 16sp for actionable text.

- [ ] **Step 2: Test at 390dp width**
  Verify no clipped text, no horizontal scroll, and clean wrapping.

- [ ] **Step 3: Commit 5**
  Commit message: `style(dashboard): increase typography to 14sp floor and 16sp action labels for readability`

---

### Task 6: Commit 6: Honest Numbers and Amounts (Scope clarity & compact Indian currency formatting)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/domain/math/MandiCurrencyFormatter.kt`
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/domain/math/MandiCurrencyFormatterTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dashboard/DashboardRevampComponents.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dashboard/DashboardScreen.kt`

- [ ] **Step 1: Write failing unit test MandiCurrencyFormatterTest.kt**
  Test cases:
  - 99,999 -> "₹99,999" (en/hi)
  - 1,00,000 -> "₹1.00 L" / "₹1.00 लाख"
  - 99,99,999 -> "₹99.99 L" / "₹99.99 लाख"
  - 1,00,00,000 -> "₹1.00 Cr" / "₹1.00 करोड़"
  - 12,27,31,344 -> "₹12.27 Cr" / "₹12.27 करोड़"

- [ ] **Step 2: Run test to confirm failure**
  Run `./gradlew :composeApp:testDebugUnitTest --tests com.appwork.mandisamiti.domain.math.MandiCurrencyFormatterTest`.

- [ ] **Step 3: Implement MandiCurrencyFormatter**
  Implement `formatCompactRupees(paisa: Long, isEnglish: Boolean): String`.

- [ ] **Step 4: Integrate compact formatter in KpiCard and DashboardMetricCard**
  Show compact amount by default, and reveal exact full figure on click/toggle.
  Make all-time scope explicit in labels to match actual SQL query scope.
  Add small neutral indicator if purchases exceed 100x sales.

- [ ] **Step 5: Run tests to confirm passing**
  Run `./gradlew test` in `projects/MandiSamiti`.

- [ ] **Step 6: Commit 6**
  Commit message: `feat(dashboard): compact indian currency formatting and explicit all-time data scope`

---

### Task 7: Commit 7: Empty and Loading States, Duplicate Banner

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dashboard/DashboardScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/dashboard/DashboardRevampComponents.kt`

- [ ] **Step 1: Add actionable button to empty state**
  When `previewParties.isEmpty() && !uiState.isLoading`, render "पहला खाता जोड़ें" / "Add first account" button that calls `onNavigateToAllKhata()`.

- [ ] **Step 2: Implement Skeleton Shimmer / Placeholder while isLoading**
  Render placeholder animated / subtle gray cards for the 4 action tiles and 4 KPI cards when `uiState.isLoading` is true.

- [ ] **Step 3: Remove duplicate shop name greeting banner**
  Remove lines 88-123 in `DashboardScreen.kt` which duplicated the shop name from the header.

- [ ] **Step 4: Device verification on physical phone**
  Rebuild and install on Redmi Note 7 (`adb -s d93c743`).
  Capture screenshots in light and dark mode, Hindi and English.
  Save evidence into `docs/testing/evidence/dashboard/`.

- [ ] **Step 5: Run tests and token parity**
  Run `./gradlew test` in `projects/MandiSamiti` and `python3 tests/ngdl-tokens/test_token_parity.py`.

- [ ] **Step 6: Commit 7**
  Commit message: `feat(dashboard): add skeleton loader, actionable empty state, and remove duplicate greeting banner`

---

### Task 8: Sync & Project Update
- Run `python3 scripts/project_update.py`
- Document findings and decisions in `vaults/Vivek-K/wiki/entities/mandisamiti.md` and `hot.md`.

