# Claude fixes, 2026-10-29 PM review (branch `fix/pm-review-claude`)

Base: `fix/pm-review-2026-10-29` at `dde93a9`. One commit per item. Verified with
`./gradlew :core-domain:jvmTest :core-data:jvmTest :composeApp:jvmTest` (all green) and
`python3 -m unittest discover -s tests/ngdl-tokens` (5 tests, OK).

| ID | Status | Commit |
|----|--------|--------|
| R1 | fixed | 2eb66f4 |
| R2 | fixed | f4a5f89 |
| R3 | fixed | f9679a2 |
| R4 | fixed | f9679a2 |
| R5 | fixed | c159987 |
| R6 | fixed (needs device proof) | e8d668f |
| F18 | **not started, needs product call** | none |
| F21 | fixed | 9a7d954 |

## R1 over-repayment cap
- `LendRepository.settle(settlement, cashEntries)` now checks the outstanding balance, writes the settlement, the cash rows and the lend status in one DB transaction. It throws `LendException.OverRepayment` / `OverRepaymentWeight` and writes nothing.
- `App.kt` (Loan Book repay) and `PartyLedgerViewModel.applyCashEntryToLoan` both call `LendTransactionUseCase.repay`. The duplicated settle + cash code is gone.
- Repay sheet save is disabled while settlements are loading (`LendTransactionsListScreen.kt`, one line; Antigravity owns that file, expect a trivial conflict at most).
- Tests: `LendEntryRepositoryTest.overRepaymentIsRejectedAndWritesNothing`, `repaymentAfterFullRepaymentIsRejectedAndCashRowIsWrittenWithSettlement`, `LendTransactionUseCaseTest`.
- `PartyLedgerBalanceF01Test.reproF01...` reproduced the bug with a ₹7,000 repay on a ₹5,000 loan, which is now impossible. The scenario uses ₹5,000 and expects `-1,220,000` paisa. The old `-1,390,000` also assumed the expense row posts to the party, which F14 removed.
- Regression tests were not run red-then-green: the old signature did not accept the new arguments, so they could not compile before the fix.

## R2 pending count
- SQL fixed: `(remarks IS NULL OR (...))`. Test: `SyncEngineTest.testPendingEntryCountIncludesCashRowWithNullRemarks`.
- `LogoutUseCase` already blocks on the broad `getPendingCount()` (parties, rows, revisions) and only uses the user-facing count for the displayed number. `LogoutUseCaseTest.pendingEntryBlocksLogoutAndChangesNothing` already covers "only an unsynced party blocks". No change needed there.

## R3 / R4 cash void cascade
- `OfflineFirstLendRepository.voidLendInTransaction` and `OfflineFirstExpenseRepository.voidExpenseInTransaction` are the shared in-transaction cascades. The cash repo calls them for `[LEND:]` and `[EXPENSE:]` rows. No recursion: the cash row being voided is already void and the cascades skip void rows.
- `[SETTLE:]` cascade now writes revision rows for the settlement (VOID) and the lend (EDIT). Revision `snapshot_json` is empty, same as the existing cash cascades.
- Tests in `PartyLedgerBalanceF01Test`: `r4_voidingLoanCashRowVoidsLendSettlementsAndRepayCashRows`, `r3_voidingRepayCashRowWritesRevisionsForSettlementAndLend`.

## R5 voided expenses
- Reverted `is_void = 0` in `getExpensesByShop`. Only caller of the stream is `HomeViewModel` (KPIs via `computeKpis`, which filters voids). Test: `ExpenseFlowTest` voided-expense case.

## R6 apply-to-loan dialog
- Dismiss and the back button reopen the cash dialog with amount and note filled in. Button label changed from "रद्द करें" to "वापस जाएँ" so it does not suggest discarding.
- When the deposit is above the selected loan's outstanding, a new button "₹X उधार में, ₹Y सामान्य जमा" calls `LendTransactionUseCase.repayWithOverflow`: one settlement plus one general jama in one transaction. Split math: `splitDeposit()`.
- Tests: `LendTransactionUseCaseTest.splitDeposit_*`, `repayWithOverflow_writesSettlementAndGeneralJamaTogether`.
- Not verified: Compose rendering of the new button at 390dp, light and dark. Needs device proof.

## F18 slip numbers: needs a product call (not started)
Reasons I stopped:
1. `buyEntryEntity.slip_number` and `sellEntryEntity.invoice_number` already exist. Sell's `invoice_number` is the legal bill number for `BILL_OF_SUPPLY` (set in `SellEntryFormState.kt:310`). Reusing it for an internal slip number would mix a GST bill number with a counter. Decision needed: separate `slip_no` column on sell, or reuse.
2. Lend entries have no slip column; adding one needs a `.sqm` migration plus backend model and schema (`backend/app/models`, `schemas/entries.py` have `slip_number` and `invoice_number` only for buy and sell).
3. Prefixes ("ख-", "ब-") are unconfirmed, and lend and deal need one too.
4. Sync: one device per shop is the current assumption; two devices would allocate the same number offline. Needs a rule (device suffix, or server assigns).
5. `takeLast(5/6)` locations to remove once decided: `PartyLedgerScreen.kt` ~805, 1054, 1154, 1249; `SellTransactionReceiptScreen.kt` 135, 247; `SellTransactionsListScreen.kt` 183; `BuyTransactionReceiptScreen.kt` 122, 200; legacy `ReceiptPreviewScreen.kt` 75.

## F21 trailing colon
- Builders already skip the colon for blank notes. Display of legacy rows goes through `RemarksFormatter.cleanForDisplay`; the party-ledger lend note now does too. Tests: `RemarksFormatterTest`.

## Requests for other agents
- Codex: `DailyRegisterViewModel.kt:161` uses raw `tx.remarks` as a row title (shows tags and trailing colon). Wrap it in `RemarksFormatter.cleanForDisplay(...)`.
- Antigravity: `LendTransactionsListScreen.kt:282` prints `"नोट: ${entry.notes}"`; route through `RemarksFormatter.cleanForDisplay`. Rebase note: the repay sheet `canConfirm` line changed in R1.

## Needs device proof (Antigravity)
- R1: repay ₹7,000 on a ₹5,000 loan from both the Loan Book and the Party Ledger; both must refuse and leave balances unchanged.
- R6: deposit above outstanding shows the split button and posts one loan row and one general row; dismissing the apply dialog returns to the cash form with the amount filled.
- R4: void the lend's cash row in the Cash Drawer; lend, repayments and party balance all drop to zero.
- R5: no screen lists expenses from this stream today (only `HomeViewModel` reads it), so there is nothing to see on device except Total Expenses staying correct after a void.
