# Task 7 brief: Short add form (for Antigravity)
Source plan: `2026-11-16-udhaar-rewrite-plan.md` lines 404-430. This brief adds verified facts and edge cases. If the two disagree, this brief wins and the reason is stated.
## Rules
- Feature first. `UdhaarAddFormState` tests are written in this task (plan Step 1). UI tests only after Vivek approves the screen.
- Money is `Long` paisa. No floats except `monthlyRatePercent: Double`.
- NGDL tokens only (`ui/theme/Color.kt`), vector icons, no emoji, contrast 4.5:1, no horizontal scroll at 390dp.
- No sound or TTS in any test. Do not edit `hot.md`. Work on `main`.
- End: append a `PROGRESS.md` entry and update `FEATURES.json` (status and evidence only), commit both with the feature.
## Step 0: fixes from the Task 6 review (do first, same commit family)
Task 6 (`f693594`) is accepted with two small fixes, because this task already edits `UdhaarLandingScreen.kt`:
- **Selected tab is lost on back.** `rememberPagerState(initialPage = 0)` inside `UdhaarLandingScreen` resets when the screen leaves composition (row tap to `PartyLedger`, then back). Edge case 17 required the tab to survive. Hold the selected tab where it outlives the screen: add `Screen.LendList` state in `App.kt` (a `rememberSaveable` `Int` in `App`, passed in as `initialPage` and written back via `snapshotFlow { pagerState.currentPage }`). Add a short note in `PROGRESS.md` on how.
- **"अज्ञात" is hard-coded** in `buildUdhaarRows` (`UdhaarRows.kt`). Edge case 6 asked for "Unknown" in English. Keep the row builder pure: store `name = null` fallback as an empty string or nullable and resolve the label in the screen using `isEnglish`. Update `edgeCase6_deletedOrMissingPartyStillShownWithFallback` to match.
- Not verified in Task 6, check once on the emulator while you are there: loading state (no flash of the empty text on first frame), 30-character name at 390dp, and a desktop-width screenshot.
## Verified facts
- `UdhaarLoanForm(shopId, partyId, direction, amountPaisa, date, kind = LOAN, monthlyRatePercent = 0.0, receiverName, receiverPhone)` at `core-domain/.../usecase/UdhaarUseCase.kt:21`.
- `UdhaarUseCase(repo, newId, nowMillis).createLoan(form)` requires amount > 0 and rate >= 0. It writes the loan and its cash row in one `saveLoanWithCash` call. It drops receiver fields unless direction is `GIVEN` (lines 62-63), and trims them. Cash type is `UDHAR_GIVEN` for GIVEN and `JAMA_RECEIVED` for TAKEN.
- The old add screen (`Screen.LendEntry` at `App.kt:946`, `LoanAdvanceEntryScreen`) saves with `lendRepo.saveLendEntry` plus a separate `cashRepo.recordTransaction`, and only ever records `UDHAR_GIVEN`. The new sheet must call `createLoan` only, never both paths.
- `UdhaarUseCase` is not necessarily built in `App.kt` yet. Run `grep -n "UdhaarUseCase(" composeApp/src` first. If it is missing, create it once next to `lendUseCase` with `IdGenerator.newId` and `Clock.System.now().toEpochMilliseconds()`.
- Party resolution: `LendPartyResolver` and `BuyPartyResolver` (`ui/lend`, `ui/buy`) have `matches(parties, phone, name)`, `existing(parties, selectedId, phone)` and `partyToSave(...)`. `LendPartyResolver.pick/needsPhonePrompt/willRename/partyToSave` take `LendEntryFormState`, not the new state. Add an overload that takes plain values (`name`, `phone`, `village`) and have the old function delegate to it. Do not write a second resolver.
- The old form also requires a **village** (`villageValidation`, `MandiValidators.sanitizeVillage`). The plan's field list has no village. Decision: the sheet has no village field. New parties are saved with `village = null`, and an existing party keeps its village. Vivek confirmed village will be captured by the new Khata flow, so the sheet never asks for it. Note it in `PROGRESS.md` as a decision, not an open question.
- Phone: `MandiValidators.validatePhone(phone, isRequired = true)` is the shared validator the old form uses. Use it. `normalizePhone` (`PhoneNumber.kt`) returns the last 10 digits or null; do not use it to validate.
- Rate: `parseLoanRate(text)` at `LendEntryFormState.kt:162` returns `Double?` (comma allowed, above 0, at most 100). Reuse it. Default text `"2"`.
- Amount: `MandiMathEngine.parseRupeesToPaisa(text)`. Check what it does with decimals and with huge input before relying on it (see edge case 4).
- Date picker: `ui/ledger/EditRepaymentDialog.kt` lines 66-97 (`DatePickerDialog` + `rememberDatePickerState`). Bottom sheet pattern: `ui/parchi/FarmerSlipCashSettlementSheet.kt`.
- Landing add button: `App.kt` `Screen.LendList` block, `onAddClick = { currentScreen = Screen.LendEntry // TODO(task7) }`. `LendEntryScreen` / `LoanAdvanceEntryScreen` stay in the repo, unreferenced from here. `Screen.LendEntry` is still reached from Home (`onNavigateToLend`, `App.kt:313`); leave that route alone and mention it in `PROGRESS.md` so Vivek can decide in Task 11.
## Edge cases and decisions
1. **Phone is mandatory.** Exactly 10 digits. `updatePhone("98-76 543210x")` gives `"9876543210"`. More than 10 digits are cut at 10.
2. **Saved match.** When the phone matches a saved party, fill the name and remember `selectedPartyId`. If the user then edits the name, show the rename notice (`willRename` rule) and do not rename silently.
3. **Name.** Trim. Blank or only spaces is invalid. Keep the 30-character wrap check at 390dp.
4. **Amount.** Whole rupees, above 0. Reject decimals and non-digits with a clear message. Cap at 9,99,99,999 so paisa conversion cannot overflow. Leading zeros are fine (`007` is 7).
5. **Date.** Default today. Past dates allowed. A future date is invalid. The date picker returns UTC midnight; compare by calendar day in the device zone, not by raw millis, so "today" is never rejected.
6. **Interest off.** `toForm().monthlyRatePercent == 0.0` even if `rateInput` holds junk.
7. **Interest on.** `parseLoanRate` rules. Zero, negative, over 100 or non-numeric is invalid and the sheet shows the reason in the current language.
8. **Give to other.** Only on the दिया tab. On लिया the toggle is hidden and `toForm` forces receiver fields to null, even if the toggle was on before the user switched direction.
9. **Receiver.** Name required when the toggle is on. Phone optional: blank becomes null, non-blank must pass the 10 digit rule.
10. **Double tap.** Save button disables while saving. One tap makes one loan.
11. **Failure.** If `createLoan` or the party save throws, keep the sheet open, keep the typed values, show an honest error. Save the party and the loan in an order that cannot leave a loan without a party. If the two writes cannot be one transaction, write the party first and say so in `PROGRESS.md`.
12. **Party type.** New parties are `PartyType.FARMER`, through `partyToSave`. An existing party is reused, never duplicated by phone.
13. **Deleted or shop-owner phone.** If the phone matches a party with `isDeleted == true`, do not silently revive it and do not create a duplicate. Decision: treat it as a match, show the name, and flag the case to Vivek in your report. Do not guess further.
14. **Sheet lifetime.** State survives keyboard open and rotation (`rememberSaveable` or a state holder). The form clears when the sheet closes after a save or an explicit dismiss.
15. **Direction.** The sheet opens with the direction of the tab the user is on (`pagerState.currentPage`). The direction is not editable inside the sheet.
16. **After save.** The sheet closes, no manual refresh (the stream updates the list), and the new person appears in the correct tab. Add a one-line confirmation (snackbar or toast, no sound).
17. **390dp.** The sheet scrolls. "सेव करें" stays reachable above the keyboard. No horizontal scroll. A 30-character name wraps.
18. **Language.** Labels and errors follow `tradeSettings?.isEnglish`. NGDL tokens, vector icons, no emoji.
19. **Old route.** Remove the `// TODO(task7)` line and the route to `Screen.LendEntry` from the landing add button.
20. **Safety.** No sound or TTS. Device writes only on staging test data. Delete every test loan and test party you create, and delete test-only screenshots.
## Steps (in order)
0. Step 0 fixes above, with the `edgeCase6` test updated. Run `UdhaarRowsTest`.
1. `UdhaarAddFormStateTest` (plan cases plus edge cases 1, 3, 4, 5, 6, 7, 8, 9). Run it, confirm FAIL.
2. `UdhaarAddFormState.kt`. Run it, confirm PASS.
3. Resolver overload, `UdhaarAddSheet.kt`, landing wiring, `App.kt` use-case wiring.
4. `./gradlew :core-domain:jvmTest :core-data:jvmTest :core-database:jvmTest :composeApp:jvmTest` (expected: all green, 652 plus the new tests, 0 failures).
5. Emulator-5554, staging URL only. Three cases: interest off, interest on, give-to-other. Light and dark at 390dp. Real taps. Screenshots to `docs/testing/screenshots/udhaar/t7-add-<off|on|receiver>-<light|dark>-390.png`. Also check back-navigation keeps the selected tab (Step 0).
6. Log, commit `feat(mandisamiti): simple udhaar add form`.
## Report back with
Test counts, screenshot paths, anything unverified (list it explicitly), the answer to edge case 13, and what you found for the loading state and the 30-character name.
