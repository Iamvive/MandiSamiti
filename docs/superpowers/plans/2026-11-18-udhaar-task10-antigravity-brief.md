# Task 10 brief for Antigravity: Buy slip loan cut and pesgi adjustment (plus the khata card number fix from Task 9)

Plan source: `docs/superpowers/plans/2026-11-16-udhaar-rewrite-plan.md`, Task 10 (line 471). Read that task, then this brief. Where they differ, this brief wins.
Base: `main` at `c72e00f` (Tasks 0 to 9 done, pushed). Paths are relative to `projects/MandiSamiti/`. `...` is `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti`.

## Two parts
- **Part A (the task):** a loan cut on a Buy slip repays the farmer's udhaar oldest first, in the same DB transaction as the slip.
- **Part B (carried over from the Task 9 review):** the Kisaan khata udhaar card shows ₹50,000 while the udhaar section under it shows "बाकी: ₹50,100 + ब्याज ₹100" for the same loan. Make them agree.

## Decisions from Vivek (locked)
- No discount anywhere (already done in Task 9).
- The single "उधार से काटें" field stays. Pesgi adjustment is the same field. No new field.
- Receipt formats and the slip's `उधार कटौती` line stay unchanged.

## Verified facts (checked at `c72e00f`)
- **The cut today is one loan only.** `App.kt` lines 515 to 535 pick the oldest open MONEY loan of the farmer (`lends.filter { !it.isVoid && it.partyId == loanPartyId && it.lendType == MONEY }`) and call `buyForm.setLoan(lendId, principal, interest)`. It does not filter on `direction`, so a TAKEN loan (we owe the farmer) can be offered as a loan to cut. That is a bug to fix here: only `LendDirection.GIVEN` loans count.
- **The form state is single-loan.** `.../ui/buy/BuyEntryFormState.kt` lines 143 to 175: `loanLendId`, `loanPrincipalPaisa`, `loanInterestDuePaisa`, `setLoan`, `loanCutSplit`, `loanCutError`, `buildLoanCut(): LendSettlement?`. The error text is "कटौती बकाया उधार से अधिक है" and "इस पार्टी का कोई उधार नहीं है". Khaate slips (`isKhaate`) cut nothing.
- **Checkout writes a settlement and no cash row.** `core-data/.../repository/OfflineFirstBuyRepository.kt` `saveBuyCheckout(entry, party, payments, loanCut: LendSettlement?)` (line 74). It calls `OfflineFirstLendRepository(...).settleInTransaction(loanCut, emptyList(), now)` (line 107) and requires `entry.paidPaisa == payments.sumOf { it.amountPaisa } + cut` (line 82). The slip number is assigned inside the transaction (`nextBuySlipNo`, line 97), so the "पर्ची #n" remark must be built after that.
- **Receipts read the cut from remarks.** `BuyEntryFormState.kt` line 389 to 395 writes `"उधार कटौती: <paisa> | "` into `remarks`; `extractLoanCut` (line 505) and `ReceiptHtmlRenderer.kt` line 55 (`LOAN_CUT:(\d+)`) read it. Keep that remark exactly as is.
- **Edit path:** line 389 reuses `extractLoanCut(existingEntry?.remarks)` when no new cut is entered. An edited slip must not write a second cut and must not lose the first.
- **Use case pieces you can reuse (core-domain):** `UdhaarAllocator.allocate(loans, paymentPaisa)` (pure, oldest first, interest before principal, returns `Ok(parts)` or `Over(totalDuePaisa)`), `RuralInterestEngine.interestDue(principal, ratePercent, lentAtMs, settlements, nowMs)`, `LendRepository.getLoansForParty(shopId, partyId, direction)`, `UdhaarUseCase.totalDue(shopId, partyId, direction, atMs)` (public), `UdhaarUseCase.repay(..., mode = BOOK_ENTRY, remarks)`. `repay` runs its own transaction (`settleAll`), so it cannot be called from inside `saveBuyCheckout`'s transaction. Allocate with the pure allocator and write through `settleInTransaction` instead (internal, same module).
- **Cash row shape for a loan cut:** `paymentMode = BOOK_ENTRY`, `lendId = loan.id`, `sourceEntryId = null`, type `JAMA_RECEIVED` for principal and `INTEREST_RECEIVED` for interest (same as Task 3 `cashRow`, including the `[SETTLE:<settlementId>:<lendId>]` suffix), remarks start with "पर्ची #<n> से कटौती".
- **Deposit in the loan card.** `App.kt` line 536 onward sums every non-void `JAMA_RECEIVED` row of the party into `buyForm.setDeposit(...)`. Since Task 3, udhaar rows are also `JAMA_RECEIVED`. Check where `depositPaisa` is used. If udhaar rows (`lendId != null`) leak into it, exclude them. The slip owns its own money story.
- **Part B source of the mismatch.** The khata card reads `PartyBalance.udhaarGivenPaisa` and `udhaarTakenPaisa` (principal only). The section header reads the person view model's total due (principal plus interest). They are different numbers by design, shown with the same label.

## Part A: what to build
1. **Party-level cut, not single loan.** The loan card's number is the farmer's GIVEN total due (principal plus interest, all open loans, PESGI included). The field is capped at that total. Over the cap: the existing error, nothing saved.
2. **Checkout allocates across loans.** Replace the `loanCut: LendSettlement?` parameter with a cut amount (and the party) and allocate inside `saveBuyCheckout`'s transaction: oldest loan first, interest before principal, one settlement plus its `BOOK_ENTRY` cash rows per loan part. If the allocator returns `Over`, throw and roll back the whole slip.
3. **Invariants stay:** `entry.paidPaisa == payments + cut`; the cut moves no drawer cash (CASH rows unchanged); the slip's own balance reflects only the slip; udhaar given drops by exactly the cut.
4. **Pesgi:** loans of kind `PESGI` are GIVEN loans and join the same allocation. No new field. If the farmer has any PESGI loan, the card label may say "पेशगी / उधार से काटें"; otherwise "उधार से काटें".
5. **Direction fix:** only GIVEN loans are offered. A farmer with only a TAKEN loan shows "इस पार्टी का कोई उधार नहीं है" for the cut.

## Part B: what to build
On the Kisaan khata udhaar card, show the same number as the section header: total due (principal plus interest) as the headline, with the same "+ ब्याज ₹Y" subline when interest is above zero, using `UdhaarUseCase.totalDue`. For a party with loans in both directions, each direction line uses its own total. Both numbers must come from one source so they cannot drift. Add a test that the card figure equals the section figure for a loan with interest.

## Test first
Run them, see them fail, then implement.
- `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/BuyLoanCutTest.kt`: farmer with a GIVEN loan of Rs 5,000 (day 1) and Rs 3,000 (day 2), interest off. Save a buy slip with a Rs 6,000 cut. Assert: slip deduction Rs 6,000; loan 1 fully settled; loan 2 has Rs 2,000 left; the new cash rows are `BOOK_ENTRY` with `lendId` set and `sourceEntryId == null`; the Galla drawer total (CASH rows) is unchanged; udhaar given drops by exactly Rs 6,000; the slip balance reflects only the slip.
- Cut above the loan total is rejected and nothing is saved (no slip, no settlement, no cash row).
- Interest on: loan Rs 10,000 at 2.0 percent, 30 days old, cut Rs 10,200 clears interest Rs 200 first.
- A PESGI loan plus an ordinary loan: the older one is cut first regardless of kind.
- A TAKEN loan is never touched by a cut.
- Slip number appears in the cash row remarks ("पर्ची #<n> से कटौती").
- Part B: card figure equals section figure for a loan with interest (view model test).

## Edge cases (handle or list as unverified)
1. Cut exactly equals the total due: loans fully settled, udhaar card shows zero and the udhaar section hides (Task 9 rule).
2. Cut of zero or blank: no settlement, no cash row, slip saves as before.
3. Khaate slip: no cut offered (current behavior).
4. Preview made on one day and saved after midnight: interest at save time can be slightly higher. The cut must never exceed the due at save time; if allocation says `Over`, show the error and keep the form.
5. Edit an existing slip that already carried a cut: no second cut, first cut kept, remark kept.
6. Voiding or deleting a slip that carried a cut: find out what happens to the settlements and `BOOK_ENTRY` rows today and after your change. Do not build a fix. Report it.
7. Party with the slip and a loan in both directions: the card number counts GIVEN only.
8. Farmer typed by phone only (no selected farmer): `BuyPartyResolver.existing` path still finds the party and its loans.
9. Receipt HTML and `BuyTransactionReceiptScreen` show the same `उधार कटौती` line as before, character for character.
10. Day close (`DayCloseCreditBuyTest`) totals do not move because of the cut.
11. 390dp, light and dark, no horizontal scroll, long Hindi names wrap, tokens only, no emoji.
12. Double tap on save writes one slip and one set of cut rows.

## Tests to update
`BuyEntryLoanSectionTest`, `DayCloseCreditBuyTest`, `BuyCheckoutTest`: update where they asserted the old row shape (settlement with no cash rows). Say in your report which assertions changed and why.

## Do not touch
Udhaar landing, person screen, add form, Home (Task 11), `LendEntryScreen`, `GrainLendEntry`, sync and backend files (Task 12), `RepayLoanDialog.kt` (Task 13).

## Device check
`emulator-5554`, staging URL only (never 10.0.2.2), 390dp, light and dark. Farmer with a loan with interest and one more loan: save a Buy slip with a cut that spans both loans. Check the khata afterwards: two cards, the udhaar card equals the section number, ledger rows tagged. Screenshots `docs/testing/screenshots/udhaar/t10-buy-cut-<light|dark>-390.png` and `t10-khata-after-cut-<light|dark>-390.png`. Restore any staging DB edit. Silent testing: no sound or voice. Delete test-only screenshots you do not need.

## Report back
Commit hash. Per-module test counts parsed from `build/test-results/jvmTest/*.xml` (core-domain, core-data, core-database, composeApp; expect more than 694, 0 failures). The answer to edge case 6. Whether the deposit sum in `App.kt` leaked udhaar rows. Which old assertions you changed. What is unverified (Redmi `d93c743`, desktop width). Update `PROGRESS.md` and `FEATURES.json` (status and evidence only) and commit them with the feature. Do not touch `hot.md` or the root submodule pointer.

Commit message: `feat(mandisamiti): loan cut on buy slip repays udhaar oldest first`
