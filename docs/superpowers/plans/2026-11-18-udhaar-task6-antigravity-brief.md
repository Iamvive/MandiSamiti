# Task 6 brief: Udhaar landing page (for Antigravity)

Source plan: `2026-11-16-udhaar-rewrite-plan.md` lines 376-403. This brief adds verified facts and edge cases. If the two disagree, this brief wins and the reason is stated.

## Rules
- Feature first. Tests for `buildUdhaarRows` are written in this task (plan Step 1), UI tests only after Vivek approves the screen.
- Money is `Long` paisa. No floats except the existing `monthlyRatePercent: Double`.
- NGDL tokens only (`ui/theme/Color.kt`), vector icons, no emoji, contrast 4.5:1, no horizontal scroll at 390dp.
- No sound or TTS in any test. Do not edit `hot.md`. Work on `main`.
- End: append a `PROGRESS.md` entry and update `FEATURES.json` (status and evidence only), commit both with the feature.

## Verified facts
- Models: `core-domain/.../domain/model/LendEntry.kt`. `LendEntry.kind` is `LOAN | PESGI | CASH`. `UdhaarUseCase.kt:152` creates `CASH` loans, and the spec counts pesgi as udhaar.
- Streams exist: `LendRepository.getLendEntriesStream(shopId)`, `getSettlementsStream(shopId)`, and `PartyRepository.getPartiesStream(shopId)`.
- Interest: `RuralInterestEngine.interestDue(principal, monthlyRatePercent, lentAtMs, settlements, nowMs)`. It filters void settlements itself.
- Outstanding and interest per loan: copy the logic in `PartyLedgerViewModel.kt` lines 172-186. It zeroes interest when outstanding is 0 and only computes it for `LendType.MONEY`.
- Current wiring: `App.kt` line 1009 `Screen.LendList` renders `LendTransactionsListScreen`. Back target of `LendList` is `Home` (line 109). `LendEntry` backs to `LendList` (line 107).

## Edge cases and decisions
1. **Kind filter.** Do not filter on `kind`. LOAN, PESGI and CASH all count. The plan only says direction, not void, MONEY. Add a test: a PESGI loan appears in the list.
2. **Interest-only leftover.** A loan whose principal is fully repaid but interest is unpaid is dropped, because the ledger zeroes interest at 0 outstanding. This matches the person ledger. Add a test and note it in `PROGRESS.md` as a known behaviour for Vivek.
3. **Status field is not trusted.** Compute from settlements. Never use `LendStatus.SETTLED` to hide a row, since a stale status would hide real dues.
4. **Legacy settlements.** Rows with `principalPaisa == 0 && interestPaisa == 0` count as all principal. Reuse the same rule the ledger uses for reduced principal. Add a test.
5. **Overpayment.** Settlements above the loan amount: clamp outstanding at 0 with `coerceAtLeast(0L)`, never negative.
6. **Deleted or missing party.** `Party.isDeleted` may be true, or the party may be missing from the list. Still show the row, with name fallback "अज्ञात" / "Unknown", so money is never hidden. Add a test.
7. **Same party in both tabs.** Allowed. Rows are built per direction, and each tab has its own total.
8. **Sorting.** Total due descending, ties broken by name ascending, so the order is stable between recompositions.
9. **`interestOn`.** True when any open (outstanding > 0) loan of that party in that direction has `monthlyRatePercent > 0.0`. A repaid loan with a rate does not turn it on.
10. **Time.** `nowMs` is passed in, never read inside `buildUdhaarRows`. The ViewModel reads the clock when the streams emit. No ticking timer.
11. **Loading vs empty.** Start the state as `null` (loading). Show the empty state only after the first emission, so there is no flash of "no udhaar" while data loads. Empty state text is honest per tab ("अभी कोई उधार दिया नहीं है").
12. **`shopId` null.** Use an empty-string stream like the current code, and render the empty state.
13. **Total line.** Sum of principal and interest across rows of that tab. Show principal total as the headline and interest as a smaller secondary line only when above 0.
14. **Dropped callbacks.** The old list had repay, void and `getSettlements`. These move to the person screen in Task 8. Until then, tapping a row opens `Screen.PartyLedger(partyId, returnScreen = Screen.LendList)`, which already exists. Do not delete `LendTransactionsListScreen`; leave it unreferenced.
15. **Grain loans.** `GRAIN` loans are no longer listed anywhere after this task. Check whether the Home totals or any other screen still reach them. If real grain loans exist on staging, flag it to Vivek rather than silently hiding them.
16. **Add button before Task 7.** The "जोड़ें" button routes to `Screen.LendEntry` for now. Task 7 swaps it for the sheet. Mark the line with `// TODO(task7)`.
17. **Pager.** Tabs switch on tap and swipe. Keep the selected tab across back-navigation (`rememberSaveable`). Pager swipe must not fight the row list scroll.
18. **Language.** Use `tradeSettings?.isEnglish` for labels, same as the other screens.
19. **Text overflow at 390dp.** Long names and villages wrap or ellipsize with `minWidth = 0`. Amounts never wrap. Check with a 30-character name.
20. **Back handler.** Keep `MandiBackHandler` so Android back goes Home.

## Steps (in order)
1. `UdhaarRowsTest` (plan cases a to g, plus edge cases 1, 2, 4, 5, 6, 8, 9). Run it, confirm FAIL.
2. `UdhaarRows.kt`: `UdhaarPersonRow` and `buildUdhaarRows`. Run it, confirm PASS.
3. `UdhaarLandingScreen.kt` with `UdhaarLandingViewModel`. Pager, rows, total line, empty and loading states.
4. `App.kt`: `Screen.LendList` renders the new screen, `onBack = Screen.Home`.
5. `./gradlew :core-domain:jvmTest :core-data:jvmTest :core-database:jvmTest :composeApp:jvmTest` (expected: all green, 0 failures).
6. Emulator-5554, staging URL only. Check both tabs in light and dark at desktop width and 390dp. Real taps: tab switch, swipe, row tap, add button. Do not write to real data. Screenshots to `docs/testing/screenshots/udhaar/t6-landing-<diya|liya>-<light|dark>-390.png`. Delete any test-only screenshots afterwards.
7. Log, commit `feat(mandisamiti): udhaar landing page with diya and liya pager`.

## Report back with
Test counts, screenshot paths, anything unverified (list it explicitly), and the answer to edge case 15.
