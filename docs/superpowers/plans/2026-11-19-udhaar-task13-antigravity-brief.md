# Task 13 brief for Antigravity: udhaar cleanup, docs, device sign-off, logging (last task, no deploy)

Plan: `docs/superpowers/plans/2026-11-16-udhaar-rewrite-plan.md`, Task 13 (lines 519-532). Read it first. Tasks 10, 11, 11b and 12 are accepted (latest commit 1df00bf). Paths are relative to `projects/MandiSamiti/`.

## Verified facts (checked in code, do not re-derive)
1. `LoanAdvanceEntryScreen.kt` and `LendTransactionsListScreen.kt` still exist in `composeApp/.../ui/lend/`. `App.kt` imports both (lines 10-11) and calls `LoanAdvanceEntryScreen(` at ~line 989. `LendTransactionsListScreen` has no call site in `App.kt` beyond the import. Check the nav graph before deleting: if the new Udhaar add form already replaced the call at line 989, the call is dead; if it is still the live route for something, stop and ask Vivek one question.
2. Keep `LendEntryScreen.kt`, `GrainLendEntry`, `LendEntryFormState.kt`, `LendPartyResolver.kt` (grain scope and shared helpers). Before deleting `LendEntryFormState.kt` or `LendPartyResolver.kt` nothing needs to change; leave them unless grep shows zero references.
3. Dead tests: `LendEntryDirectionBTest.kt` and `LendFlowTest.kt` in `composeApp/src/jvmTest/.../ui/lend/`. Delete only the tests that exercise the two deleted screens. Keep any test that checks domain behaviour (allocator, use case, balances). Say which tests you removed and which you kept.
4. Carry-over notes from Task 12 PROGRESS.md (line 739) are named only as "void warning, `snapshot_json`, helper visibility, hardcoded elevation token". The repo does not say more. Find each yourself with grep and fix only what is clearly a defect in code this rewrite touched:
   - **void warning**: the void confirmation text for a buy slip with a loan cut (`इस पर्ची की उधार कटौती ... भी वापस हो जाएगी।`). Check that the same warning shows for English (`isEnglish`) and that a slip with no cut shows no warning.
   - **`snapshot_json`**: lives in `backend/app/models/sync_conflict.py` and `sync.py` (~lines 287-300) for revisions. Report what you found. Change nothing unless a udhaar row can reach it with a null or wrong value; if unclear, list it under "unverified" and move on.
   - **helper visibility**: new helpers made `internal` or `public` only for tests (for example `sellDepositPaisa` in `App.kt`, line 107). Tighten to the narrowest visibility that still compiles with the tests.
   - **hardcoded elevation token**: the new Udhaar screens (landing, add form, person screen, khata cards, Home Udhaar card) must use `--ngdl-card-shadow` via the NGDL compose token, not `CardDefaults.cardElevation(defaultElevation = N.dp)`. Fix those in files the rewrite created or changed. Do not sweep the older screens (`DashboardScreen.kt`, `ExpenseListScreen.kt`, `SellTransactionsListScreen.kt`); list them in the report as out of scope.

## Do (in order)
1. Run the full suite first for a baseline: `./gradlew :core-domain:jvmTest :core-database:jvmTest :core-data:jvmTest :composeApp:jvmTest`. Never `:core-domain:allTests`. Latest known total is 720 tests. Rerun `HomeViewModelTest.testLendEntryReflectedInReceivablesAndHonestSubtitleWhenUnlinked` once if it fails.
2. Delete the two dead screens, their `App.kt` imports and the dead call site, and the dead tests (facts 1 and 3). Fix the four carry-over items (fact 4). Run the suite again. Expected: 0 failures, count drops only by the tests you deleted.
3. Docs: update `docs/product/feature-status.md` (Udhaar row), add "Superseded by 2026-11-16 udhaar rewrite" at the top of `docs/decisions/2026-11-11-record-loan-advance.md`, create `docs/decisions/2026-11-16-udhaar-model.md` (the core rule: slip balance and udhaar balance never add; oldest loan first, interest before principal; overpayment blocked; plus the three open-decision outcomes below).
   - Open decision 1: DISCOUNT_GIVEN and INTEREST_ADDED buttons removed (Task 9, accepted).
   - Open decision 2: Home shows slip balances only, plus the Udhaar card with दिया and लिया totals (Task 11, accepted).
   - Open decision 3: plain "रुपये दिए" repays an outstanding उधार लिया oldest first, else creates a new उधार दिया; "रुपये मिले" the mirror; overpayment against opposite loans blocked (Task 3, accepted).
4. NGDL Definition of Done (`docs/design-system/DESIGN-SYSTEM.md` §5.7) on emulator, **staging URL only** (`http://64.227.142.180:8060`, never `10.0.2.2`). Screens: Udhaar landing, add form, person screen, Kisaan khata, Home Udhaar card. Check light and dark at 390dp and desktop width: no horizontal scroll, contrast at least 4.5:1, no emoji, tokens only, controls clicked. Writes only on a throwaway shop, never on real data. Run the `tests/ngdl-tokens` check from §5.7. Add one regression test for any visual bug you fix.
5. End-to-end walk on device, in this order, with observed numbers written down:
   1. Add a Rs 10,000 loan, interest off.
   2. Add a pesgi of Rs 3,000 for the same farmer.
   3. Make a buy slip of Rs 10,000 with Rs 4,000 cash and a Rs 5,000 loan cut.
   4. Check the slip shows its own payment history and the deduction line.
   5. Check udhaar shows Rs 8,000 left (10,000 + 3,000 - 5,000) and the ledger tags are right.
   6. Overpay on the person screen and see the blocking message.
   7. Extra, because Tasks 11b and 12 changed it: void that buy slip and check udhaar returns to Rs 13,000 and the slip balance returns.
6. Logging (mandatory, nothing in chat without proof on disk): append a dated session entry to `PROGRESS.md` (runtime, changes, test counts from `build/test-results/jvmTest/*.xml`, observed device numbers, unverified list, next steps). Update `FEATURES.json`: add or update an "Udhaar rewrite" entry with `status` and evidence; `passing` only for what a test or the device walk proved. Append any new verified check at the end. Write a durable note to `vaults/Vivek-K/wiki/log.md`, then run `python3 scripts/vault_lint.py` from the repo root (must report 0 errors). Do not touch `hot.md`; Claude refreshes it after Vivek approves.
7. Cleanup: delete test-only screenshots you did not need, temp files, and `ms-repay-edit-wt` if its staging test is done. Keep the screenshots that PROGRESS.md cites. Do not commit `__pycache__`.
8. Commit `docs(mandisamiti): udhaar rewrite sign-off and progress log`. Code cleanup may be a separate commit `refactor(mandisamiti): remove old lend screens and tighten udhaar helpers`. Do not push and do not deploy.

## Edge cases to handle or report
1. Deleting the old add screen must not strand a nav route or a deep link: grep `Screen.` route names and the back stack handling, and open the app after the change.
2. Void then re-cut on the same farmer: a second loan cut after a voided one allocates against restored loans, not stale settlements.
3. A pesgi (KIND = PESGI) cut from a slip appears under the pesgi tag, not the loan tag.
4. Hindi and English strings both checked on the person screen and the overpayment message.
5. Physical Redmi Note 7 (`d93c743`) was never tested. If it is connected, run steps 5.1 to 5.6 there too; if not, list it as unverified.

## Report back (numbered)
1. Commit hashes.
2. Test counts per client module from XML, before and after, and which tests you deleted.
3. What each of the four carry-over items turned out to be, and what you changed.
4. Observed numbers from the device walk (steps 5.1 to 5.7).
5. §5.7 checklist result, with the `tests/ngdl-tokens` output.
6. Older screens left with hardcoded elevation (out of scope list).
7. What is unverified: VPS deploy, production migration, real-device sync, Redmi Note 7.
8. Anything that conflicts with this brief. If so, stop and ask Vivek one question.
