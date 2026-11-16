# Antigravity handoff: finish spare-crop (khaate) for MandiSamiti

## Context
Khaate lets the trader record day-end spare crop as a Buy or Sell with no party, no charges, no Galla cash effect. It has its own khaate line per crop in the Godown and counts as Mohari (Procured) in day-end close-out.

State as of 2026-11-16: Tasks 1 to 6 are built, tested and already merged into `main` (merge `e040116`; `main` HEAD is `37b1c29`). The `spare-crop-khaate` branch and the `../MandiSamiti-khaate` worktree are gone, so work directly in `projects/MandiSamiti` on a fresh branch off `main` (e.g. `khaate-finish`). Do not look for the old worktree.

Remaining: Task 6 device evidence, Tasks 7 to 10, device checks for Tasks 4 and 5, and the minors.

Source of truth (read first, do not re-derive):
- Plan: `projects/MandiSamiti/docs/superpowers/plans/2026-11-16-spare-crop-khaate.md` (Global Constraints at lines 13-26; Tasks 7-10 at lines 387-500 have the test code)
- Spec: `projects/MandiSamiti/docs/superpowers/specs/2026-11-16-spare-crop-khaate-design.md`
- Vault hot cache: `vaults/Vivek-K/wiki/hot.md`
- Last PROGRESS entry: `projects/MandiSamiti/PROGRESS.md` ("2026-11-16 (Claude Code): spare-crop (khaate) feature")

## Rules to keep (from plan and CLAUDE.md)
- Khaate entry: commission, labour, tulak and mandi shulk are 0. Amount (Asal) = weight x rate. `paidPaisa = receivedPaisa = 0`, no `cashTransactionEntity` row, no party khata effect.
- Close-out: khaate counts under "मोहरी" / "Mohari (Procured)", never Payable. Gross label "असल" / "Asal".
- Spare sell above the khaate line: warning only, save allowed. Normal Sell keeps blocking.
- Tests: `./gradlew :core-domain:jvmTest :composeApp:jvmTest`. Never `:core-domain:allTests` (known iOS compile failure). `HomeViewModelTest.testLendEntryReflectedInReceivablesAndHonestSubtitleWhenUnlinked` is flaky: rerun it alone before calling it a failure. Use `--rerun-tasks` for authoritative runs, because Gradle up-to-date results are not evidence.
- NGDL §5: tokens only from `docs/design-system/DESIGN-SYSTEM.md` §2.1, contrast 4.5:1 in both themes, vector icons, no emoji, no horizontal scroll at 390dp.
- Roast Room is Gemini-only; irrelevant here.
- Secrets: never print or log keys or `.env` values.
- One question at a time to Vivek.

## Step 0: Setup
1. `cd projects/MandiSamiti && git status && git checkout -b khaate-finish main`.
2. Confirm the build: `./gradlew :core-domain:jvmTest :composeApp:jvmTest`. Expect about 345 composeApp tests, 0 failures.
3. Emulator `emulator-5554` is the device target. Create `docs/testing/screenshots/khaate/`.

## Step 1: Task 6 device check (unblocks the review)
Screenshots in light, dark and 390dp for each case. Name files `t6-<case>-<light|dark>-390.png`.
- Godown commodity card with khaate activity (extra "खाते: N बोरी" row) and without (no row).
- Negative khaate balance shows the negative-warning token colour.
- Movement list shows the amber khaate chip on a khaate bill.
- Stock audit sheet shows two fields: normal count and "खाते बोरी गिनती" (prefilled).
Fix the Task 6 minors in the same pass:
- Card label "खाते" appears twice (`StockRegisterScreen.kt` ~775-784): keep one.
- Audit khaate field hidden when the crop has no khaate activity (`StockRegisterScreen.kt` ~1141): confirm intended. Spec says two inputs, so prefer always showing it for stock audits; ask Vivek only if unsure.
- Add a unit test for `StockBill.isKhaate` and the khaate-row condition.
- The XML khaate keys are unused (screens use inline `isEnglish` strings); either wire them or delete them, one pattern only.
Commit: `fix(mandisamiti): khaate Godown device polish and tests`.

## Step 2: Task 4 and 5 device checks
- Task 4 (audit): enter both counts, save, check the khaate diff = `khaateBags x bagWeightGrams - (khaateBought - khaateSold)`. Add the missing save-path formula test and record a red run first.
- Fix the deferred minor: a negative prefill in the audit field cannot be retyped.
- Task 5 (forms): Buy khaate 5 bags Gehu (no party, no phone, charges hidden), Sell khaate 8 bags (short-stock warning, saves, line shows -3), edit a khaate entry (toggle locked), confirm no Galla row and drawer balance unchanged. Light, dark and 390dp.
- Save path returns to the previous screen with no receipt: ask Vivek whether a khaate slip should show a receipt; default is leave as is.
- Populate or remove the unused `khaateAdjustmentGrams` (Task 3 minor), and add a multi-variety khaate stock test.

## Step 3: Task 7 Mohari in close-out
Follow plan lines 387-415 exactly (TDD):
1. Read `core-domain/.../domain/bahi/BahiDayPageBuilder.kt` first; map the real builder and field names onto the plan's test (`khaateGoesToMohariNotPayableAndNotCash`). Test file: `core-domain/src/commonTest/.../BahiDayMohariTest.kt`.
2. Run, expect FAIL.
3. Add `khaateBuyAsalPaisa` and `khaateSellAsalPaisa` (sum of `grossPaisa` of the day's non-void khaate buys and sells). Exclude `isKhaate` entries from payable and receivable sums. Expected cash is unchanged.
4. `DailyRegisterViewModel.kt` passes the values through; `DayClosingSheet.kt` Details (~213) gets two rows: "मोहरी खरीद (असल)" / "Mohari buy (Asal)" and "मोहरी बिक्री (असल)" / "Mohari sell (Asal)". Add both to `values/strings.xml` and `values-en/strings.xml`.
5. Also check the Daily Register and Bahi print/share paths do not list khaate as a party entry.
6. Run, expect PASS. Device: light, dark, 390dp screenshots of the close-out sheet.
Commit: `feat(mandisamiti): mohari rows in day-end close-out`.

## Step 4: Task 8 edit and void
Follow plan lines 419-455 (TDD). Tests in `composeApp/src/jvmTest/.../KhaateEditVoidTest.kt`: edit writes a revision and moves the khaate line; void restores the line; edit keeps `isKhaate` and zero charges.
- Copy the setup from existing edit and void tests; find paths with `grep -rn "entryRevisionEntity\|insertRevision" composeApp/src core-database/src`.
- Implement: edit form reopens with `isKhaate = true`; the revision snapshot includes `is_khaate`; list screens (`BuyTransactionsListScreen.kt`, `SellTransactionsListScreen.kt`, `ParchiListScreen.kt`) show the khaate chip and "खाते" in place of a party name.
- Straggler hunt: grep every party balance, Khata and party search query for a missing `is_khaate` filter. The system party `khaate-<shop>` must never appear in party lists, Khata or search.
Commit: `feat(mandisamiti): edit and void for khaate entries`.

## Step 5: Task 9 backend sync
Follow plan lines 459-484 (TDD).
1. Failing pytest first: `cd backend && pytest -k is_khaate`. Use the real endpoints and fixtures in `backend/tests/`.
2. Alembic migration `<rev>_add_is_khaate.py`: `is_khaate BOOLEAN NOT NULL DEFAULT false` on both buy and sell entry tables. Schemas default `False` so old clients still sync. Find models with `grep -rn "is_void" backend/app`.
3. Client mapper: `grep -rn "getPendingSyncBuyEntries" composeApp/src --include="*.kt"`; send and read the field.
4. Acceptance check: the hidden `khaate-<shop>` party (pushed with sync_status 0) is accepted by the server as a normal party row. Write a test for it.
5. Run the full backend suite (previously 130 of 130).
Commit: `feat(backend): sync is_khaate on buy and sell entries`.
**Do not deploy.** The backend deploy goes through `/project-deploy` and needs Vivek's explicit confirmation.

## Step 6: Task 10 tracker and logging (mandatory, CLAUDE.md §4)
1. Append to `FEATURES.json`: `{"id":"spare-crop-khaate", ..., "status":"failing", "evidence":"Domain and UI built; device check by Vivek pending"}` (exact JSON in plan line 495).
2. `python3 scripts/check_tracker.py` must print `tracker ok (30 features)`. It expected 29 before this.
3. Append a `PROGRESS.md` session entry: timestamp, "Antigravity", commits, fresh test counts with `--rerun-tasks`, screenshot paths, what stays unverified. Set the spec status line to "Built, device check pending".
4. Log the lessons in `vaults/Vivek-K/wiki/log.md` plus a concept note, refresh `wiki/hot.md`, then `python3 scripts/vault_lint.py` (0 errors).
5. Hand Vivek the device checklist (plan line 499): khaate buy 5 bags Gehu; khaate sell 8 bags (warning, saves, line -3); Godown two lines; audit two counts; close-out Mohari rows; Galla cash unchanged; edit then void. Flip to `passing` only after Vivek reports.
6. Commit: `docs(mandisamiti): track spare-crop-khaate`. Run `/project-update`. Ask Vivek before merging to `main`.

## Verification (definition of done)
- Fresh `./gradlew :core-domain:jvmTest :composeApp:jvmTest --rerun-tasks`: 0 failures (flaky test rerun alone if it fails).
- Backend pytest all green, including the `is_khaate` round trip.
- `python3 scripts/check_tracker.py` ok at 30 features; `python3 scripts/vault_lint.py` 0 errors.
- Screenshots exist in `docs/testing/screenshots/khaate/` for Godown, audit, forms and close-out in light, dark and 390dp.
- Anything not verified is listed in the PROGRESS entry, not claimed.

## Open decisions for Vivek (ask one at a time, only if blocked)
- Should a khaate save show a receipt, or return to the previous screen as now?
- Is hiding the audit khaate field when the crop has no khaate activity intended?
- Backend deploy of the `is_khaate` migration: confirm via `/project-deploy`.
- Task 2 Step 7 on-device upgrade check needs a `13.db` snapshot; Migration13Test stands in until one exists.
