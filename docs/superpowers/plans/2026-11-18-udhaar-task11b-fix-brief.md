# Task 11b brief for Antigravity: void buy slip reverses its loan cut, plus two deposit and advance leaks

Context: Task 10 (efa7483) and Task 11 (79c510d) are accepted. Your own Task 11 report found these three problems. This task fixes them. It sits between Task 11 and Task 12 of `docs/superpowers/plans/2026-11-16-udhaar-rewrite-plan.md`.

## Locked decision (Vivek, 2026-11-18)
Voiding a buy slip that cut udhaar **reverses the cut automatically**, in the same DB transaction as the void. The farmer owes again exactly what they owed before the slip.

## Verified facts (checked in code)
1. `OfflineFirstBuyRepository.voidBuyEntry` (core-data, ~line 238) only flips `is_void` on the slip and records a revision. It does nothing to udhaar.
2. The slip has no structural link to its repayment. The cut writes `LendSettlement` rows and `BOOK_ENTRY` cash rows with `sourceEntryId = null`, `lendId` set, and remarks `"पर्ची #<slipNo> से कटौती [SETTLE:<settlementId>:<lendId>]"`. The only link is that remark text.
3. A precedent exists: `OfflineFirstLendRepository.voidLendInTransaction` (~line 348) voids a loan, its settlements (`queries.voidLendSettlement`, plus a revision row each) and its linked cash rows found by tag (`queries.getCashTransactionsByTag`). Copy that pattern, do not invent a new one.
4. `settleInTransaction` (~line 134) sets the loan status to `SETTLED` or `PARTIAL` from live (non-void) settlements. There is no code that sets it back to `OPEN` when settlements are voided. You must add the recompute: no live settlement means `OPEN`, principal repaid below the amount means `PARTIAL`, else `SETTLED`.
5. Leak 1, certain: `App.kt:827` (Sell entry flow) sums `JAMA_RECEIVED` for the buyer's deposit without `&& it.lendId == null`. The same fix was applied at `App.kt:551` for Buy in Task 10.
6. Leak 2, unconfirmed: `BahiDayQueries.sq` `getUnlinkedFarmerAdvancesBefore` (~line 28) selects `UDHAR_GIVEN` rows with no `lend_id IS NULL` check. If new loans write a `UDHAR_GIVEN` row with `lendId` set, they can appear as legacy advances on the Buy screen. Part of your job is to find out whether that happens.

## Steps (tests first, then implement)
1. **Tests** in `core-data/src/jvmTest/.../BuyLoanCutTest.kt` (extend it), each run and seen failing first:
   a. Farmer with Loan 1 Rs 5,000 and Loan 2 Rs 3,000, slip with Rs 6,000 cut, then `voidBuyEntry`: both settlements `is_void = 1`, the 2 or 3 cash rows `is_void = 1`, Loan 1 and Loan 2 both `OPEN`, total due back to the pre-slip amount, slip `is_void = 1`.
   b. Void a slip with no loan cut: behaves as before, no udhaar rows touched.
   c. Void twice: second call changes nothing, no exception.
   d. A later normal repayment on Loan 2 (from the Udhaar person screen) exists, then the slip is voided: only the cut's settlements are voided, the later repayment stays, Loan 2 ends `PARTIAL` with the right principal outstanding.
   e. A loan the cut touched was voided before the slip void: skip it without error.
   f. Two slips for the same farmer, void only slip #2: slip #1's settlements untouched.
   g. Atomicity: force a failure after the first settlement is voided (test hook or a bad id) and assert nothing changed.
2. **Link the repayment to the slip.** For new slips, add a tag to the cash row remarks, for example `[BUYCUT:<buyEntryId>]`, written in the same place as the `[SETTLE:...]` tag, and look rows up with `getCashTransactionsByTag`. For slips saved before this change (staging has slip #4 for farmer Monty), fall back to matching `BOOK_ENTRY` rows for that party whose remarks contain `"पर्ची #<slipNo> से कटौती"`. Use one lookup helper for both. Do not change `sourceEntryId = null`, the Galla cash drawer depends on it.
3. **Implement** the reversal inside the existing `database.transaction` in `voidBuyEntry`: find the cut's cash rows, collect their settlement ids from the `[SETTLE:...]` tag, void each settlement (with revision row), void the cash rows (same insert-with-void pattern as `voidLendInTransaction`), recompute each affected loan's status and write a lend revision. Put the shared "void one settlement and recompute status" logic in `OfflineFirstLendRepository` as an `internal fun` next to `settleInTransaction`, and call it from both places if `voidLendInTransaction` can reuse it without behaviour change.
4. **Fix leak 1** at `App.kt:827`. Add a regression test in `composeApp` (a Sell entry deposit with a lend-linked `JAMA_RECEIVED` row is not counted).
5. **Check leak 2.** Write a test that saves a new loan and an old-style advance for the same farmer and asks the query which rows it returns. If loan rows leak, add `AND lend_id IS NULL` and keep the old advance behaviour. If they do not leak, add no code change and say why with the evidence.
6. **UI:** find the screen that offers "void slip" and show the reversal in its confirm text, for example "इस पर्ची की उधार कटौती (₹52,000) भी वापस हो जाएगी" with the amount, only when the slip had a cut. Hindi first, English under `isEnglish`. NGDL tokens only. If the void flow has no confirm dialog today, report that and do not add a new screen.
7. **Device** (staging URL only, never 10.0.2.2, emulator 390dp, light and dark, silent): reproduce Task 10's setup for farmer Monty (two loans, slip with a cut), void the slip, then confirm in SQLite and on the Udhaar landing page and Monty's Khata that both loans are OPEN and the due amount is back. Screenshots `t11b-void-confirm-<light|dark>-390.png` and `t11b-khata-after-void-light-390.png`. Restore all test data and delete temp files and screenshots you do not keep.
8. **Log and commit:** PROGRESS.md entry, new feature at the end of FEATURES.json, commit `fix(mandisamiti): void buy slip reverses loan cut, exclude loan rows from deposit and advances`. Do not touch hot.md. Do not touch backend or sync (Task 12 owns that).

## Edge cases to handle or report
1. Slip edit: if a buy slip can be edited after saving, does the edit re-run the cut or leave it? Report only.
2. Slip void by reason `VoidReason` types: reversal applies to every reason.
3. Interest: after the reversal, interest due is recomputed from the loan start with the cut gone. Confirm the due amount equals the pre-slip value on the same day.
4. Synced rows: the void writes `syncStatus = 0` on every touched row so the outbox sends them. Do not touch the sync code.
5. Cash drawer: the Galla cash total must not change at void time (the cut never moved cash).
6. A cut that touched a TAKEN loan cannot exist (cuts use GIVEN only). Do not add code for it.

## Report back (numbered)
1. Commit hash.
2. Test counts per module from the result XMLs.
3. Which existing assertions changed and why.
4. Leak 2: confirmed or not, with the evidence.
5. How you linked the repayment to the slip, and how the fallback treats slip #4 on staging.
6. Device: verified and not verified (Redmi `d93c743` and desktop width stay unverified unless tested).
7. Anything in the code that conflicts with this brief. If so, stop and ask Vivek one question.
