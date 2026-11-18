# Task 8 brief: Udhaar person screen and one-button repayment (for Antigravity)

Source plan: `2026-11-16-udhaar-rewrite-plan.md` Task 8 (line 434). This brief adds verified facts and edge cases. If the two disagree, this brief wins and the reason is stated.

## Rules
- Feature first. `UdhaarRepayFormStateTest` is written first (pure state, plan Step 1). UI tests only after Vivek approves the screen.
- Money is `Long` paisa. No floats except the existing `monthlyRatePercent: Double`.
- NGDL tokens only (`ui/theme/Color.kt`), vector icons, no emoji, contrast 4.5:1, no horizontal scroll at 390dp.
- No sound or TTS. Do not edit `hot.md`. Work on `main`. Staging only on device, delete every test row (loans, settlements, cash rows, parties) and test-only screenshots.
- Do not rewrite `EditRepaymentDialog.kt`, `RepaymentHistoryDialog.kt` or the old `RepayLoanDialog.kt`. Do not touch `PartyLedgerScreen.kt` (Task 9).
- End: append a `PROGRESS.md` entry, update `FEATURES.json` (status and evidence only), commit both with the feature.
- Report test counts from `build/test-results/jvmTest/*.xml` per module, not from memory. Say what the diff does, not what you intended. Tasks 6 and 7 each shipped a wrong count or a claim the diff did not back.

## Verified facts
- `UdhaarUseCase.repay(shopId, partyId, direction, paymentPaisa, paidAt, mode = CASH, remarks = null)` returns `List<LoanPayment>`. It throws `UdhaarOverpaymentException(totalDuePaisa)` when the payment is above due, and `require(paymentPaisa > 0)`. Both live in `core-domain/.../usecase/UdhaarUseCase.kt`.
- `UdhaarAllocator`: oldest loan first by `entryDate`, interest before principal inside one loan. One payment can create several `LendSettlement` rows, one per loan touched.
- Interest is computed at `paidAt` (`openLoans(..., nowMs = paidAt)`), so the amount due depends on the payment date. Only `LendType.MONEY` and non-void loans count.
- `UdhaarPersonRow` (name nullable, village, `principalDuePaisa`, `interestDuePaisa`, `interestOn`) is built by `buildUdhaarRows`. It does not carry the phone or the loans.
- Streams: `LendRepository.getLendEntriesStream`, `getSettlementsStream`, `PartyRepository.getPartiesStream` (excludes deleted and `khaate-%` parties). `UdhaarLandingViewModel` is the pattern for combining them.
- Nav: `App.kt` `Screen` sealed interface (line ~81), back-navigation map (lines ~107 to 118), landing row tap currently goes to `Screen.PartyLedger(partyId, returnScreen = Screen.LendList)` (~line 1030). The selected landing tab lives in `lendListSelectedTab` (`rememberSaveable`) and must survive person screen and back.
- Edit repayment: `EditRepaymentDialog(settlement, isEnglish, preview, errorCode, onPreview, onConfirm, onDismiss)` plus `LendRepository.editSettlement(...)`. History: `RepaymentHistoryDialog(history, isEnglish, onDismiss)` plus `getSettlementHistory`. The void flow in `PartyLedgerScreen.kt` is keyed to `LedgerItem`, not to a settlement, so check how a repayment is voided before promising it (edge case 15).

## Decisions
- **Direction is part of the route.** `Screen.UdhaarPerson(partyId, direction)`. The landing passes the direction of the tab. A person who owes you and is owed by you appears on both tabs, and each screen shows and repays only its own direction.
- **Loans with repayments nested.** Each loan row shows date, amount, "पेशगी" chip for `LendKind.PESGI`, and "प्राप्तकर्ता: <name>" when `receiverName` is set (दिया side only). Its non-void and void settlements sit under it, because edit works on one settlement of one loan. `LendKind.CASH` and `LOAN` show no chip.
- **Due is computed at the picked date.** Add a small public function on `UdhaarUseCase` (for example `totalDue(shopId, partyId, direction, atMs)`) that reuses the private `openLoans`. Do not copy the interest logic into the UI.
- **Payment date lower bound.** The date may not be earlier than the newest open loan's `entryDate`, because `UdhaarAllocator` would otherwise pay a loan before it existed. Add `minDateMs` to `UdhaarRepayFormState` (brief wins over the plan's constructor). Message: "तारीख उधार की तारीख से पहले नहीं हो सकती" / "Date cannot be before the loan date".
- **Decimals.** Check `RuralInterestEngine` rounding. If interest due can be a non-multiple of 100 paisa, the amount field accepts up to 2 decimals through `MandiMathEngine.parseRupeesToPaisa` and a "पूरा चुकाएँ" / "Pay full" chip fills the exact due. If interest is always whole rupees, keep whole rupees only. Say which in the report.

## Edge cases
1. **Amount.** Blank, zero, non-numeric: invalid with a clear message. Above due at the picked date: invalid, message "बाकी सिर्फ ₹X है" / "Only ₹X is due" with X from the due at that date. Exactly the due: valid.
2. **Date.** Default today. Future date invalid (reuse `isSameOrPastDay` from `UdhaarAddFormState.kt`, calendar day in the device zone, so "today" is never rejected). Earlier than `minDateMs` invalid.
3. **Date change updates due.** Changing the date re-computes the due and the over-payment check. A previously valid amount can become invalid.
4. **Overpayment from the use case.** If `repay` still throws `UdhaarOverpaymentException` (a race with another write), `onOverpayment(e)` sets the same message, the dialog stays open, nothing is saved.
5. **Oldest first.** Two loans, pay part of the older: only the older gets a settlement. Pay across both: older fully, newer partly. Interest goes before principal inside a loan. Cover in a unit test on the allocator path (the use case, not only the form).
6. **Exact rest.** Paying exactly the total due leaves principal 0 and interest 0 and the person leaves the landing list.
7. **Settled state.** If both dues are 0 (opened from history or a stale route), the button is hidden, the balance line says "चुकता" / "Settled", and the history stays visible. No crash.
8. **Interest line.** "+ ब्याज ₹Y" shows only when `interestOn` and `Y > 0`. Principal and interest are never merged into one number.
9. **Header.** Name, village, phone. Missing name shows "अज्ञात" / "Unknown" resolved in the screen by `isEnglish`. Missing village or phone: no empty row. A deleted or unknown party id still opens the screen with the fallback name.
10. **Button label.** "पैसे मिले" on दिया, "पैसे दिए" on लिया, in English "Received" / "Paid". The dialog has amount and date only. No mode or remarks field.
11. **Double tap.** The save button disables while saving. One tap makes one repayment.
12. **Failure.** If `repay` throws anything else, keep the dialog and typed values, show a fixed message in the current language, log the real error. Never show `e.message` raw.
13. **Dialog lifetime.** Typed values survive keyboard and rotation (`rememberSaveable` or a state holder). The dialog clears after save or dismiss.
14. **After save.** Dialog closes, the list and balance update from the streams with no manual refresh, a one-line snackbar confirms (no sound), and the landing screen shows the new totals after back.
15. **Edit and void on repayment rows.** Reuse the existing dialogs for edit and history. For void, find how a settlement is voided today. If it cannot be done without `LedgerItem` or `PartyLedgerScreen.kt`, stop, do not rewrite it, and report what is needed. Voided settlements show muted with "रद्द" and are not counted.
16. **Grain loans.** Only `LendType.MONEY` loans appear. Staging has none today, but filter anyway.
17. **Navigation.** Tapping a landing row opens the person screen for the tab's direction. Back returns to the landing page on the same tab. Add the new screen to the back map in `App.kt`. The old `Screen.LendEntry` route stays (Task 11 decides).
18. **Loading.** First frame shows the spinner while state is null, never an empty-state message.
19. **390dp.** The screen scrolls, no horizontal scroll, the action button stays reachable, a 30-character name wraps, long receiver names wrap.
20. **Language and tokens.** Labels and errors follow `tradeSettings?.isEnglish`. NGDL tokens, vector icons, no emoji.

## Steps
0. Read `PROGRESS.md`, `git log -5`. Tree must be clean before you start.
1. `UdhaarRepayFormStateTest` (plan cases plus edge cases 1 to 4). Run it, confirm FAIL.
2. `UdhaarRepayFormState.kt`. Run it, confirm PASS.
3. Use case `totalDue` plus a test for it. Test for edge cases 5 and 6 through `UdhaarUseCase` with the existing in-memory repo pattern (see `UdhaarSaveLoanTest.kt` and `UdhaarUseCaseTest.kt`).
4. `UdhaarPersonViewModel.kt` (modelled on `UdhaarLandingViewModel`), `UdhaarPersonScreen.kt`, the repay dialog, `App.kt` route, landing tap wiring.
5. `./gradlew :core-domain:jvmTest :core-data:jvmTest :core-database:jvmTest :composeApp:jvmTest`. Expected: 669 plus the new tests, 0 failures.
6. Emulator-5554, staging URL only. Cases, light and dark at 390dp, real taps:
   - `part`: two loans on different dates, pay part of the older, check in SQLite that only the older has a settlement.
   - `rest`: pay the exact remaining total, person leaves the list.
   - `over`: try to overpay, message shows, nothing saved.
   - `interest`: one interest-on loan, balance line shows "+ ब्याज".
   Screenshots to `docs/testing/screenshots/udhaar/t8-person-<part|rest|over|interest>-<light|dark>-390.png`. Also check back keeps the tab. Delete all test rows afterwards.
7. Log, commit `feat(mandisamiti): udhaar person screen and oldest-first repayment`.

## Report back
Commit hash, real test counts per module, the decimals decision (edge case from Decisions), what you found for edge case 15, the loading-state and 30-character name findings, and anything unverified (list it explicitly).
