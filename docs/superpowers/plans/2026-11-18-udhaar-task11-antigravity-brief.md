# Task 11 brief for Antigravity: Home entry point and Udhaar card

Plan: `docs/superpowers/plans/2026-11-16-udhaar-rewrite-plan.md`, Task 11 (lines 489-499). Read that first. Task 10 is accepted (commit efa7483).

## Locked decision (Vivek, 2026-11-18)
Home "market receivable" counts **slip balances only**. A separate **Udhaar card** shows total दिया and total लिया and opens the Udhaar landing page. Slips and udhaar are never summed into one number.

## Verified facts (checked in code, do not re-derive)
1. `HomeViewModel.kt:307` builds each party's `balancePaisa` as `slipBalancePaisa + udhaarNetPaisa`, with the comment "until the Udhaar card (Task 11)". Lines 314-319 then split that into `rec` and `pay`, so today both Home totals (`totalMarketReceivablePaisa`, `totalFarmerPayablePaisa`) mix slips and udhaar. This is the main bug to fix: use `slipBalancePaisa` alone there.
2. `HomeViewModel.kt:249-269` adds `unlinkedLendAmountPaisa` (money lends whose `partyId` is not a known party) into `totalMarketReceivablePaisa` at lines 269 and 325. Check whether any such lend can still exist after Task 4. If not, delete `unlinkedLendAmountPaisa`, `unlinkedLendCount` and their UI uses. If one can exist (old data), report it and keep it out of the slip total.
3. `PartyBalance.udhaarGivenPaisa` and `udhaarTakenPaisa` (Party.kt:41 area) are principal only. Task 9 and 10 made Khata and the landing page show principal plus interest via `UdhaarUseCase` total due. Home must show the same figure, otherwise Home says one number and the landing page another.
4. `UdhaarLandingViewModel` already aggregates given and taken totals from the lend and settlement streams (`UdhaarRows.buildUdhaarRows`). Reuse that logic or share one helper. Do not write a third interest calculation.
5. Entry points: `App.kt:331` passes `onNavigateToLendList = { currentScreen = Screen.LendList }`. `HomeScreen.kt:86` and `:205` already thread the callback down to `DashboardScreen.kt:98`.
6. `DashboardScreen.kt:177` computes `allTimeLendsTotal` as the sum of every non-void lend amount, including settled ones, and shows it on the "कुल उधार" card (line 493) which also opens `onNavigateToLendList`. That number is stale and conflicts with the new card. Replace that card with the new Udhaar card (do not keep both).
7. Existing tests to extend or fix: `ui/home/HomeViewModelTest.kt`, `HomeViewModelSettlementTest.kt`, `HomeViewModelSellSettlementTest.kt`. Any that assert a receivable or payable that included udhaar must change; list each one in your report.

## Steps (tests first, as in Task 10)
1. Failing test in `HomeViewModelTest`: one party with a slip balance of Rs 6,000, a GIVEN loan of Rs 10,000 and a TAKEN loan of Rs 2,000 (0% interest). Expect `totalMarketReceivablePaisa = 600_000`, `udhaarGivenTotalPaisa = 1_000_000`, `udhaarTakenTotalPaisa = 200_000`, `totalFarmerPayablePaisa` unaffected by the udhaar. Run, see it fail.
2. Second test: a GIVEN loan at 2% with interest due. The Home given total equals principal plus interest, identical to the figure the landing page shows for the same data.
3. Third test: a repayment (settlement) lowers the given total, a voided loan is excluded, a SETTLED loan contributes 0.
4. Implement: the two new Home state fields, the slip-only totals, the Udhaar card (given and taken, opens the landing page), remove dead unlinked-lend code.
5. Run `./gradlew :core-data:jvmTest :composeApp:jvmTest` (and the other two modules). Report counts parsed from the result XMLs.
6. Device: staging only, never 10.0.2.2. Emulator at 390dp, light and dark. Screenshots `docs/testing/screenshots/udhaar/t11-home-light-390.png` and `t11-home-dark-390.png`. Tap the card and confirm it opens the landing page. Restore any test data after.
7. Log: PROGRESS.md entry, append a feature to the end of FEATURES.json, commit `feat(mandisamiti): home udhaar card opens landing page`. Do not touch hot.md.

## UI rules (NGDL, `docs/design-system/DESIGN-SYSTEM.md` §2.1 and §5)
Tokens only, card uses `--ngdl-card-shadow` equivalent, vector icon only, no emoji, contrast 4.5:1 in both themes, no horizontal scroll at 390dp (`weight` plus `minWidth 0`, wrapping text). Hindi first with English labels under `isEnglish`. Honest empty state: when both totals are 0 show Rs 0 and the card still opens the landing page. Large numbers (Rs 10,00,000 and above) must not truncate.

## Edge cases to handle or report
1. Party with both a given and a taken loan: both totals count, never netted.
2. Farmer with a slip payable and a given loan: payable stays slips only.
3. Party deleted (soft) but with open loans: decide from how the landing page treats it, and match it.
4. Interest changes with time: the card must refresh when the date rolls, same as the landing page.
5. Loading state: show a placeholder, not Rs 0, until streams emit.
6. Hindi number grouping via `MandiMathEngine.formatRupees`.
7. English mode label for the new card.

## Report back (answer each, numbered)
1. Commit hash.
2. Test counts per module from XML.
3. Which existing assertions changed and why.
4. Was `unlinkedLendAmountPaisa` dead or not.
5. What was verified on device and what was not (Redmi `d93c743` and desktop width stay unverified unless you test them).
6. Carry-over checks from Task 10, report only, do not fix: (a) `voidBuyEntry` leaves lend settlements and BOOK_ENTRY cash rows in place, so a voided slip keeps udhaar reduced; confirm there is still no other path that reverts them. (b) grep every place that sums `JAMA_RECEIVED` or `UDHAR_GIVEN` cash rows and list any that do not exclude rows with `lendId != null` (the Task 10 deposit leak fix was local to `App.kt`).
