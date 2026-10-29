# Bahi day page handoff — Codex

Date: 2026-10-29  
Branch: `feat/bahi-codex`  
Base: `fe603f3`

## Changed

- Added the pure `BahiDayPageBuilder` in `core-domain`. It maps D1 entry types to Left/Right, duplicates non-cash entries on the opposite side, keeps voided rows struck through but out of totals, strikes same-day advances, carries earlier advances on Left, reports likely causes, and supports a `DAY_CLOSE_DIFF` balancing line.
- Added all eight requested JVM builder scenarios.
- Added `BahiDayQueries.sq` without changing `AppDatabase.sq`: per-day reads, earlier-advance lookup, `[BUY:<id>]` linking through existing remarks, close-difference lookup, and revisions after close.
- Added `RTGS`, `NEFT`, `CHEQUE`, and legacy `ONLINE` support to cash payment modes; kept `UPI`, `BANK`, and `BOOK_ENTRY` readable. Added matching expense modes and `DAY_CLOSE_DIFF`.
- Updated direct day entries, expense entries, and loan repayment to select Cash/RTGS/NEFT/Cheque/UPI. Loan repayment now persists the selected mode.
- Wired purchases, sales, expenses, loan settlements, and cash rows into the day-page projection. Loan settlements remain principal-only with `TODO(feat/bahi-claude)` for the incoming principal/interest fields.
- Day closing now shows compact Left and Right columns side by side, totals, difference, and likely causes. A cash shortage can be closed and is persisted as a `DAY_CLOSE_DIFF` expense plus its linked cash row, so later opening cash recalculates.
- Historical days are editable; the existing repository revisions continue to preserve edits/voids.

## Files outside primary ownership

- `composeApp/.../App.kt`: injected existing buy/sell/lend/expense repositories into the register view model and passed loan repayment payment mode.
- `composeApp/.../ui/expense/ExpenseEntryScreen.kt`: detailed payment-mode picker and Hindi label for day-close difference.
- `composeApp/.../ui/lend/LendTransactionsListScreen.kt`: detailed repayment payment-mode picker.
- `core-domain/.../model/CashTransaction.kt`, `ExpenseEntry.kt`: free-text enum values only; no schema change.
- `core-domain/.../usecase/LendTransactionUseCase.kt`: persists repayment payment mode.

## Claude/schema follow-up

- After `feat/bahi-claude` merges, map `lendSettlementEntity.principal_paisa` and `interest_paisa` to separate Left lines at the named TODO.
- Buy and Sell entities have no payment-mode field. Their entry forms cannot persist Cash/RTGS/NEFT/Cheque/UPI without either a linked cash transaction for each settlement or new free-text columns. No column was added here.
- A durable closed-at timestamp is absent. `getChangesAfterDayClose` is ready, but the “changed after closing” marker needs Claude’s durable close record (recommended: `dayClosingEntity.closed_at`) to distinguish a close from an ordinary `DAY_CLOSE_DIFF` row, including balanced closes.

## Evidence and device proof

- Chose side-by-side columns at 390px; each gets equal width, single-line ellipsized labels, and compact amounts. No horizontal container is used.
- No emulator AVD is installed. The only attached target is Antigravity-owned Redmi `d93c743`; it was not installed to or launched. Therefore light/dark 390px screenshots and physical control click-through remain for Antigravity.
- Contrast uses existing NGDL `Color.kt` aliases only; no hex values or emoji were added.

## Verification

- `./gradlew :core-domain:jvmTest --tests '*BahiDayPageBuilderTest'`
- `./gradlew jvmTest`
- `./gradlew :composeApp:assembleDebug`
- `python3 tests/ngdl-tokens/test_token_parity.py`
- `python3 scripts/vault_lint.py`

Results: all Gradle tests and debug assembly passed (`BUILD SUCCESSFUL`, 117 tasks); NGDL parity passed 5/5; vault lint passed 111 pages with 0 errors and 0 warnings.
