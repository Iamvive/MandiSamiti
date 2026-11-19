# Task 15 brief for Antigravity: create a pesgi from the Udhaar add sheet (no deploy, no push)

Paths are relative to `projects/MandiSamiti/`. Run this after Task 14 is accepted, because both touch the Udhaar screens. Vivek's decision: **a loan / pesgi toggle on the Udhaar add sheet**, not a new entry on the buy slip.

## What a pesgi is
पेशगी = an advance: money given to a farmer before the farmer brings crop to sell. It counts in the udhaar balance from the day it is given, carries the "पेशगी" chip, and is expected to be cut from the farmer's next buy slip (spec `docs/superpowers/specs/2026-11-16-udhaar-rewrite-design.md` lines 30 and 37). It is `LendKind.PESGI`, direction `GIVEN`.

## Verified facts (checked in code, do not re-derive)
1. `UdhaarAddFormState.toForm()` (`ui/udhaar/UdhaarAddFormState.kt:127-137`) hardcodes `kind = LendKind.LOAN`. `UdhaarLoanForm` (`core-domain/.../usecase/UdhaarUseCase.kt:29`) already takes `kind: LendKind`, so `createLoan` needs no change.
2. `UdhaarAddSheet.kt` takes `direction: LendDirection` (line 126) and the form state is keyed on it (line 132) and saved through `UdhaarAddFormStateSaver` (lines 86-109, an index-based list). Any new field must be added to the saver at the end of the list so rotation and process death keep it.
3. The person screen already shows the "पेशगी" chip for `kind == PESGI` (`UdhaarPersonScreen.kt:439-456`). No change needed there; use it as the check that the new path works.
4. The interest switch is at `UdhaarAddSheet.kt:424`, and the receiver switch (GIVEN only) at ~line 465. Project rule: interest defaults off and the suggested rate when switched on is `RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT` (2.0).
5. The Task 13 device walk inserted the pesgi straight into SQLite because this path did not exist. Replace that step with the real UI path in this task.

## Do (in order)
1. Baseline: `./gradlew :core-domain:jvmTest :core-database:jvmTest :core-data:jvmTest :composeApp:jvmTest`. Never `:core-domain:allTests`. Record the count.
2. Add `kind: LendKind = LendKind.LOAN` to `UdhaarAddFormState`, and have `toForm()` use it. Add it to the end of `UdhaarAddFormStateSaver`, with a safe default when an older saved list is shorter.
3. UI: on the **उधार दिया (GIVEN)** sheet only, add a two-option segmented control "उधार / पेशगी" (English: "Loan / Advance") near the top, below the title. Default "उधार". Hide it on उधार लिया (TAKEN); the plan has no taken pesgi. NGDL tokens only, vector icons if any, no emoji, 48dp touch targets, no horizontal scroll at 390dp. Use an existing segmented or chip pattern from the repo before writing a new one; name the one you reused.
4. One-line helper text under the control when "पेशगी" is chosen: "फसल बेचने से पहले दिया गया पैसा, अगली पर्ची से कटेगा।" (English: "Money given before the crop is sold. It is cut from the next slip."). Both strings follow the inline `isEnglish` pattern.
5. Interest: leave the interest switch available and default off for both kinds. Do not auto-enable it for pesgi.
6. Tests (the feature is final once this works, so add them now):
   - `UdhaarAddFormStateTest`: `kind = PESGI` flows into `toForm()`; default is `LOAN`.
   - Saver round trip keeps `kind`; a short older list restores `LOAN`.
   - `UdhaarUseCaseTest` (or the existing create-loan test file): creating with `kind = PESGI` stores a `LendEntry` with `kind = PESGI`, direction `GIVEN`, and the same cash row as a loan.
   - A buy-slip cut against a PESGI still allocates oldest first (reuse `BuyLoanCutTest` setup; one PESGI older than one LOAN, assert the PESGI is settled first, and the settlement row carries the right loan id).
7. **Device walk, staging URL only, emulator, throwaway shop, no SQLite inserts.** Redo the Task 13 steps 5.1 to 5.7 through the app:
   1. Add a Rs 10,000 loan (interest off).
   2. Add a Rs 3,000 pesgi for the same farmer through the new toggle. Confirm the person screen shows the "पेशगी" chip and "बाकी ₹13,000".
   3. Buy slip of Rs 10,000, Rs 4,000 cash, Rs 5,000 loan cut. Write down which loan the cut settled first and why (oldest by date).
   4. Slip shows its own payment history and the deduction line.
   5. Udhaar shows Rs 8,000 left, ledger tags right.
   6. Overpay on the person screen and see the block message.
   7. Void the slip and confirm udhaar returns to Rs 13,000.
8. NGDL DoD on the new control: light and dark at 390dp, one desktop-width light shot, a contrast reading with the two colours and the ratio for the selected and unselected segment text, `tests/ngdl-tokens` output. Save as `docs/testing/screenshots/udhaar/t15-<screen>-<light|dark>-390.png`.
9. Logging: `PROGRESS.md` entry (runtime, changes, test counts from XML, device numbers, unverified list, next steps). Add a feature at the end of `FEATURES.json` ("Pesgi from add sheet") with `passing` only for what a test or the device walk proved. If the earlier Task 13 entry says pesgi was inserted via SQLite, add a note to it that Task 15 replaced that with a UI path; do not reword the entry. Append a durable note to `vaults/Vivek-K/wiki/log.md`, then `python3 scripts/vault_lint.py` from the repo root (0 errors). Do not touch `hot.md` and do not bump the parent submodule pointer.
10. Commit in the submodule: `feat(mandisamiti): create pesgi from udhaar add sheet` and `docs(mandisamiti): task 15 pesgi sign-off`. Do not push. Do not deploy.

## Out of scope
- Pesgi on the buy slip, a TAKEN pesgi, grain pesgi, and any change to the allocator or the loan cut rules.
- Older hardcoded `defaultElevation` cards (known gap).
- Backend and sync: `kind` already syncs (Task 12). Do not touch them.

## Edge cases to handle or report
1. Switching the toggle after typing amount and name keeps those fields.
2. Switching direction from दिया to लिया resets `kind` to LOAN (the control is hidden there).
3. Rotation keeps the chosen kind (saver test above).
4. Pesgi with the "पैसा किसी और को दिया?" receiver switch on: allowed, same as a loan.
5. A pesgi with interest switched on accrues like a loan; say whether the person screen shows "+ ब्याज" for it.
6. Hindi and English labels both checked.

## Report back (numbered)
1. Commit hashes.
2. Test counts per module from XML, before and after, and the new test names.
3. Which existing segmented or chip pattern you reused.
4. Device walk numbers for steps 7.1 to 7.7 and which loan the cut settled first.
5. Contrast numbers, screenshot names and the `tests/ngdl-tokens` output.
6. What is unverified: Redmi Note 7 (`d93c743`), VPS deploy, production migration, real-device sync.
7. Anything that conflicts with this brief. If so, stop and ask Vivek one question.
