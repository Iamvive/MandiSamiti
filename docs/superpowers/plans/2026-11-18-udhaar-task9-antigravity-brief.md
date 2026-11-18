# Task 9 brief for Antigravity: Kisaan khata with separate slip and udhaar balances

Plan source: `docs/superpowers/plans/2026-11-16-udhaar-rewrite-plan.md`, Task 9 (line 453). Read that task, then this brief. Where they differ, this brief wins.
Base: `main` at `bbfd0da` (Tasks 0 to 8b done, pushed). Paths are relative to `projects/MandiSamiti/`. `...` is `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti`.

## Decisions from Vivek (locked)
1. **No discount, anywhere.** Remove the discount button and the `DISCOUNT_GIVEN` write path from the Party Ledger. Do not replace it.
2. **Interest button goes too.** Remove "ब्याज जोड़ें" and the interest dialog (`openInterestDialog`, `recordCalculatedInterest`, the dialog near `PartyLedgerScreen.kt` line 1332). Interest now lives inside udhaar loans through the Task 7 switch. Do not delete the `DISCOUNT_GIVEN` or `INTEREST_ADDED` enum values: old rows may exist and the ledger must still render them.
3. Plain cash rule (Open decision 3 default, already built in Task 3): "रुपये दिए" repays an open उधार लिया oldest first, else creates a new उधार दिया. "रुपये मिले" repays an open उधार दिया, else creates a new उधार लिया. Overpayment against the opposite loans is blocked with a message.

## Verified facts (checked at `bbfd0da`)
- `PartyBalance` (`core-domain/.../model/Party.kt` line 34) has `slipBalancePaisa`, `udhaarGivenPaisa`, `udhaarTakenPaisa`, and `udhaarNetPaisa`. The two are never summed in the khata.
- `PartyLedgerUiState` (`.../ui/ledger/PartyLedgerViewModel.kt` line 86) already exposes `slipBalancePaisa`, `udhaarGivenPaisa`, `udhaarTakenPaisa`. It does NOT expose `udhaarNetPaisa` yet.
- `PartyLedgerScreen.kt` line 202 still has the interim `headerBalance = slipBalancePaisa + (given - taken)`. That sum is exactly what this task removes.
- `UdhaarUseCase.plainCash(shopId, partyId, side: CashSide, amountPaisa, date): PlainCashResult` exists (`core-domain/.../usecase/UdhaarUseCase.kt` line 153). Results: `Repaid(parts)` or `NewLoan(loan)`. `CashSide` is `GAVE` or `GOT`.
- `ledgerTagFor` already exists and `LedgerItem.CashItem.tag` is wired (`PartyLedgerViewModel.kt` line 74). Task 9 must render the tag on every row type, not only cash rows.
- Old paths still in the screen: `RepayLoanDialog` used at lines 1239 and 1282; `viewModel.repayLoan` at 1239 and 1289; `applyCashEntryToLoan` and `applyDepositSplitAcrossLoan` in the view model (lines 244 to 249); the cash dialog calls `recordCashEntry` at 750 and 908; the "हिसाब चुकता" modal starts at line 933; buttons at lines 326 (JAMA_RECEIVED) and 336 (UDHAR_GIVEN).
- Buy and Sell slip settle (`settleBuySlip`, `settleSellSlip`, lines 336 to 415) write `UDHAR_GIVEN` and `JAMA_RECEIVED` cash rows with slip references. Those are slip payments, not plain cash. Leave them alone. Plain cash routing applies only to the "रुपये दिए" and "रुपये मिले" buttons.
- The Task 8 pieces to reuse: `UdhaarPersonScreen` content, `UdhaarRepayDialog` and `UdhaarRepayFormState` (now with `normalisePickerDate`, local-date compare, `computeJob`), and `buildUdhaarRows`. Reuse them. Do not copy them.
- `EditRepaymentDialog.kt` and `RepaymentHistoryDialog.kt` stay. Repayment void is NOT possible today (`voidEntry` covers ledger items only). Do not invent a void. Note it in your report.

## What to build
1. **Two balance cards side by side** at the top: "पर्चियों का बाकी" (slip) and "उधार बाकी" (udhaar). Udhaar card shows दिया and लिया as two short lines when both are non-zero, never one netted figure. Slip card keeps the existing लेना है / देना है wording and colour rule. Remove the old single header total.
2. **Udhaar section** under the cards: the दिया / लिया `HorizontalPager` for this party only, with their loans and the Task 8 repay button. Hide the section when the party has no loans in either direction (honest empty state, no empty pager).
3. **One date-ordered ledger** below, every row tagged by `ledgerTagFor` ("पर्ची #n" or "उधार", never both).
4. **Route "रुपये दिए" and "रुपये मिले" through `UdhaarUseCase.plainCash`.** No direct `UDHAR_GIVEN` or `JAMA_RECEIVED` rows without a link.
5. **Remove:** the "apply to loan?" dialog, the loan option of the "हिसाब चुकता" dialog (it keeps buy and sell slips only), the discount button, the interest button and dialog. Then delete `RepayLoanDialog.kt` and any loan-only code left unused in `PartyLedgerScreen.kt` and `PartyLedgerViewModel.kt`, but only after a grep shows zero references. Check `LendTransactionsListScreen.kt` before deleting anything it uses.

## Test first (extend `PartyLedgerViewModelTest`)
Run them, see them fail, then implement.
- (a) State exposes `slipBalancePaisa` and `udhaarNetPaisa` separately. A buy slip payment moves only the slip number.
- (b) `onPlainCash(CashSide.GAVE, 5_000)` with no loans creates a GIVEN loan of kind CASH and raises `udhaarGivenPaisa`.
- (c) `onPlainCash(CashSide.GOT, ...)` above what the party owes us sets an error message and writes nothing.
- (d) Every row in the merged ledger list has the tag from `ledgerTagFor`.
- (e) `onPlainCash(CashSide.GAVE, ...)` when we owe them (TAKEN loan) repays that loan oldest first and does not create a new loan.
- (f) Plain cash that exactly clears the loan leaves `udhaar` at zero and the khata hides the udhaar section.
- (g) A party with only old `DISCOUNT_GIVEN` or `INTEREST_ADDED` rows still renders a ledger and does not crash.

## Edge cases (all must be handled or listed as unverified)
1. Party with a slip and a loan shows two cards, correctly tagged ledger.
2. Party with only slips: udhaar card shows "कोई उधार नहीं" or is hidden, one rule, applied consistently. Say which.
3. Party with only udhaar: slip card shows ₹0 honestly, not blank.
4. Party with loans in both directions: both lines show on the udhaar card, both pager tabs work.
5. Opposite-direction plain cash with an amount larger than what is owed is blocked with a message; the amount field keeps its value.
6. Interest-on loan: the "रुपये मिले" repay follows interest-before-principal (Task 3 allocator). The card shows principal plus "+ ब्याज ₹Y".
7. Double tap on save writes one row (same guard as Task 8).
8. Amounts take 2 decimals only where the repay dialog already does. Plain cash dialog stays whole rupees.
9. Old data: rows with `lendId = null` and an udhaar type still show a tag. Use whatever `ledgerTagFor` returns, do not add Hindi-remarks matching.
10. 390dp: both cards fit without horizontal scroll, long Hindi names wrap, `minmax(0, 1fr)` style weights.
11. Light and dark: contrast at least 4.5:1, tokens only, vector icons, no emoji.
12. Rotate or resize while a dialog is open keeps the typed amount (the Task 8b lesson). Do not key `remember` on balances.

## Do not touch
`UdhaarPersonScreen`, `UdhaarLandingScreen`, the Buy loan card (Task 10), Home (Task 11), `LendEntryScreen`, `GrainLendEntry`, backend and sync files.

## Device check
`emulator-5554`, staging URL only (never 10.0.2.2), 390dp, light and dark. Use a farmer with a slip and a loan. Screenshots `docs/testing/screenshots/udhaar/t9-khata-<light|dark>-390.png`. If you edit the staging DB for a test, restore it. Silent testing: no sound or voice.

## Report back
Commit hash. Per-module test counts parsed from `build/test-results/jvmTest/*.xml` for core-domain, core-data, core-database, composeApp (expect more than 687, 0 failures). Which edge case 2 rule you chose. What you deleted and the grep proof of zero references. What is unverified (Redmi `d93c743`, desktop width). Update `PROGRESS.md` and `FEATURES.json` (status and evidence only), commit them with the feature. Do not touch `hot.md` or the root submodule pointer.

Commit message: `feat(mandisamiti): kisaan khata with separate slip and udhaar balances`
