# Task 14 brief for Antigravity: open a farmer's khata from the app, then finish the Task 13 sign-off (no deploy, no push)

Paths are relative to `projects/MandiSamiti/`. Task 13 is accepted except the items below. Vivek's decision: **add a "खाता देखें" button on the Udhaar person screen, and make the party name on a Parchi slip card open the khata.** Do not add a tab and do not change Home.

## Verified facts (checked in code, do not re-derive)
1. `Screen.PartyLedger` is built only in `App.kt` (~line 317) inside `onNavigateToPartyKhata = { party -> ... }`. That callback is passed to `HomeScreen.kt:73,208` and `DashboardScreen.kt:103` (default `{}`), but `DashboardScreen` never calls it.
2. `UdhaarPersonScreen.kt` and `UdhaarLandingScreen.kt` open `Screen.UdhaarPerson`, never the khata. The bottom tab opens Parchi.
3. Earlier device checks (t9, t11b) reached the khata. Find out how, with `git log -S"Screen.PartyLedger" -- composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/App.kt` and the PROGRESS.md steps. Report whether a path existed that was later removed, or those runs used a code edit. This decides whether Task 14 fixes a regression.
4. Slip cards live in `ParchiListScreen.kt` (~lines 580-680 and 840-1040). Check what a tap on the slip body already does; do not break it. The party name needs its own tap target.

## Do (in order)
1. Baseline: `./gradlew :core-domain:jvmTest :core-database:jvmTest :core-data:jvmTest :composeApp:jvmTest`. Latest known total is 721 (109 + 12 + 186 + 414). Never `:core-domain:allTests`.
2. **Person screen button.** Add a "खाता देखें" (English: "View khata") button to `UdhaarPersonScreen`. NGDL tokens only, vector icon (Lucide style, `currentColor`), no emoji, min touch target 48dp, wraps at 390dp with no horizontal scroll. It calls a new `onOpenKhata: (Party) -> Unit` parameter that `App.kt` wires to the same `Screen.PartyLedger` the existing callback builds. Back from the khata returns to the person screen, not Home. Check the back stack.
3. **Parchi party name.** Make the party name on a slip card tap to the same khata. Keep the existing slip-body tap. If a long press on that card already exists (void flow), keep it working.
4. Reuse one navigation helper in `App.kt` for both entry points. Do not copy the `Screen.PartyLedger(...)` construction twice.
5. **Regression test** (the plan allows tests once the feature is final, and this one is): a routing test next to `ScreenRoutingTest.kt` that both callbacks resolve to `Screen.PartyLedger` for the party, and that back from the khata resolves to the screen it came from. If the back mapping lives in a `when` in `App.kt`, extract the smallest pure function that makes it testable.
6. **Device check, staging URL only, emulator, writes only on a throwaway shop.** With no code edit and a clean `git status` before installing: Home, then Udhaar, then a farmer, then "खाता देखें", then back. Parchi, then a slip's party name, then the khata, then back. Long-press a slip in the khata and confirm the void dialog still shows the loan-cut warning.
7. **Close the Task 13 sign-off gaps** using the same build:
   - Dark and light screenshots at 390dp for: Udhaar landing, add form, person screen (with the new button), khata, Home Udhaar card. Save new ones under `docs/testing/screenshots/udhaar/` named `t14-<screen>-<light|dark>-390.png`.
   - One desktop-width (above 600dp) light screenshot of the person screen and the khata.
   - A contrast reading for the new button text and icon in both themes, with the two colours and the ratio. Do not write "verified" without the numbers.
   - Run the `tests/ngdl-tokens` check from the repo root and paste the result.
8. Logging: `PROGRESS.md` entry (runtime, changes, test counts from XML, device observations, unverified list, next steps). Add a new feature at the end of `FEATURES.json` ("Khata entry points") with `passing` only for what a test or the device walk proved. Append a durable note to `vaults/Vivek-K/wiki/log.md` and run `python3 scripts/vault_lint.py` from the repo root (0 errors). Do not touch `hot.md` and do not bump the submodule pointer in the parent repo.
9. Commit in the submodule: `feat(mandisamiti): open khata from udhaar person screen and parchi party name`, and `docs(mandisamiti): task 14 khata entry sign-off`. Do not push. Do not deploy.

## Out of scope
- Pesgi creation. There is no UI path for it today and Vivek has not decided on one yet. Do not add a kind toggle. Mention in the report only if you touch code that sets `kind`.
- The older hardcoded `defaultElevation` cards (`DashboardScreen.kt`, `ExpenseListScreen.kt`, `SellTransactionsListScreen.kt`, `BuyTransactionsListScreen.kt`, `ParchiListScreen.kt`, `StockRegisterScreen.kt`). Known gap, no NGDL shadow token exists in the Compose app yet. For any new card or surface in this task, use the existing `MandiCard` or the same pattern as `UdhaarCard`, and say which.

## Edge cases to handle or report
1. A party with no udhaar and no slips: the button still opens an honest empty khata, not a crash.
2. A voided slip's party name still opens the khata.
3. Hindi and English labels both checked.
4. Rapid double tap on the button opens one khata, not two stacked.
5. A party deleted or missing from the list when the callback fires: no crash, stay on the current screen.

## Report back (numbered)
1. Commit hashes.
2. Answer to fact 3: how the khata was reached before, and whether a path was removed.
3. Test counts per module from XML, before and after, and the new test names.
4. Device walk observations for step 6, with the screenshot file names.
5. Contrast numbers and the `tests/ngdl-tokens` output.
6. Which existing card pattern the new button and any new surface use.
7. What is unverified: Redmi Note 7 (`d93c743`), VPS deploy, production migration, real-device sync.
8. Anything that conflicts with this brief. If so, stop and ask Vivek one question.
