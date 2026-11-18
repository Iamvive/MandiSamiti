# Task 8b brief: repay date bound and dialog state fixes (for Antigravity)

Follows Task 8 (`9884f51`, accepted). Two defects found in review. Nothing else changes.

## Rules
- Fix only the two items below. Do not touch `PartyLedgerScreen.kt`, `EditRepaymentDialog.kt`, `RepaymentHistoryDialog.kt` or `RepayLoanDialog.kt`. No new features.
- Money is `Long` paisa. NGDL tokens only. No sound or TTS. Do not edit `hot.md`. Work on `main`, do not push. Staging only on device, delete every test row and every test-only screenshot.
- Write the failing test first for each item, then the fix.
- End: append a `PROGRESS.md` entry, update `FEATURES.json` (status and evidence only), commit both with the fix. Also commit this brief file, it is untracked.
- Report test counts per module from `build/test-results/jvmTest/*.xml`, not from memory. Say what the diff does, not what you intended.

## Item 1: day comparison in `isSameOrAfterDay`
File: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/udhaar/UdhaarRepayFormState.kt`, lines 13 to 23.

Problem: the function returns true when the picked date passes in UTC **or** in the local zone (`pickedUtcDate >= minDate || pickedLocalDate >= minDate`). A date one day before the loan date can pass, so `UdhaarAllocator` can pay a loan before it existed. The two checks exist because two sources feed `dateMs`:
- the initial value `todayMs` is a real instant, so its day is the local day;
- the Material date picker (`datePickerState.selectedDateMillis`, `UdhaarPersonScreen.kt` line 643) returns midnight UTC of the chosen calendar day, so its day is the UTC day.

Fix: one source of truth per value, then one comparison.
- Normalise the picked value to the local calendar day when the picker confirms (read the picker value as a UTC date, then build the instant for that same calendar date in the local zone). Keep `dateMs` an instant of the chosen local day from then on.
- `isSameOrAfterDay` compares **local dates only**: local date of `dateMs` >= local date of `minDateMs`. No `||`.
- The picked day equal to the loan's day must be valid, even when the loan's `entryDate` has a later time of day than the picked instant.
- Check `isSameOrPastDay` in `UdhaarAddFormState.kt` line 16. If it uses the same either-zone pattern, fix it the same way with its own test and say so in the report. If it is already local-only, leave it.

Tests (pure state, `UdhaarRepayFormStateTest`), each with an explicit `TimeZone`, not the system default:
1. Asia/Kolkata: picked day one day before the loan day is invalid; same day is valid; next day is valid.
2. A negative-offset zone (America/Los_Angeles): same three cases give the same answers.
3. Loan `entryDate` at 18:00 local, picked day the same date: valid.
4. Picker round trip: picker returns midnight UTC of 12 Nov. After normalising, the local date is 12 Nov in both zones above.

## Item 2: typed amount lost when the due changes
File: `UdhaarPersonScreen.kt`, line 625.

Problem: `UdhaarRepayFormState` is built with `remember(uiState.totalDuePaisa, todayMs, uiState.minDateMs)`. When the person's streams emit a new due while the dialog is open (a sync lands, or another save changes a loan), the key changes, the state is rebuilt, and the typed amount and the picked date reset to empty and today.

Fix: build the state once for the dialog's lifetime and feed new dues into it.
- `remember(todayMs)` or no key for the state. `minDateMs` and the due come in through `LaunchedEffect(uiState.totalDuePaisa, uiState.minDateMs)` that calls `formState.updateTotalDue(...)` and updates `minDateMs` (make it a `var` with state if needed).
- Keep the typed amount and the picked date. After a date pick the due is computed at that date through `viewModel.computeDueAt`. A stream update must not overwrite that with the due at today: when the picked date is not today, recompute through `computeDueAt(formState.dateMs)` instead of using `uiState.totalDuePaisa`.
- If the new due is below the typed amount, the existing validation message shows. Do not clear the field.
- A stale `computeDueAt` result must not land after a newer date pick. If two picks happen quickly, the last pick wins.

Tests: the pure parts in `UdhaarRepayFormStateTest` (`updateTotalDue` keeps `amountRupees` and `dateMs`; typed amount above the new due shows the message; typed amount stays). The recompose behaviour is checked on device.

## Edge cases
1. Loan on 12 Nov, picked 11 Nov: save disabled, message "तारीख उधार की तारीख से पहले नहीं हो सकती" / "Date cannot be before the loan date".
2. Loan on 12 Nov, picked 12 Nov: valid, any time of day on the loan.
3. Picked date in the future: still blocked, message unchanged.
4. Two open loans, newest on 15 Nov: bound is 15 Nov (unchanged from Task 8).
5. Type 2000, then a stream emit changes the due: amount still 2000, date unchanged.
6. Pick 13 Nov (due differs from today), then a stream emit: the due shown is still the 13 Nov due.
7. Typed amount above the new due after an emit: message shows, field keeps its text, save disabled.
8. Two quick date picks: the due shown is for the last pick.
9. Dark and light, 390dp: the dialog layout is unchanged (no new widgets).
10. Double-tap save still writes one repayment (Task 8 guard unchanged).

## Device check (`emulator-5554`, staging, 390dp)
- Items 1, 2: open the repay dialog, type an amount, change the due while it is open (add a loan from a second path or edit one row in SQLite and wait for the stream), confirm the field keeps its text.
- Item 1: pick the day before the loan, confirm the block and message. Screenshot light and dark.
- Delete all test rows and screenshots not named in the report.

## Report back
Commit hash, per-module test counts from the XMLs, whether `isSameOrPastDay` had the same pattern, what the diff did for the stale `computeDueAt` case, and a list of anything not verified (Redmi `d93c743`, desktop width).
