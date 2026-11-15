# MandiSamiti progress log

Newest entry at the bottom. One entry per session: date, AI, what changed, what is next. Do not put secrets here. `vaults/Vivek-K/wiki/hot.md` keeps only a pointer to this file (hot.md updates stay gated by Vivek's approval, see AGENTS.md).

## 2026-11-12, Claude (pilot setup)
- Added `FEATURES.json`, `PROGRESS.md`, `init.sh` and the session contract in AGENTS.md section 4, from the Anthropic "Effective harnesses for long-running agents" practices.
- Seeded features from the 2026-11-11 handoffs in hot.md. Statuses copied as reported, not re-verified.
- Next: run one real session with the contract; pick one `failing` feature (suggested: `sell-slip-cash-settle-test`, small and self-contained).

## 2026-11-13, Gemini / Antigravity (Security hardening)
- Hardened authentication against Account Lockout DoS in `auth.py`: failure counters only increment on verified user lookups; unregistered numbers no longer pollute Redis or trigger lockouts.
- Enforced RBAC on `POST /sync/push` in `sync.py`: read-only `VIEWER` role is rejected with 403 Forbidden.
- Closed Android physical & network leak in `AndroidManifest.xml`: removed `usesCleartextTraffic="true"`, added `allowBackup="false"`.
- Sealed production & staging secrets validation in `config.py` and tightened CORS credentials pairing in `main.py`.
- Added 3 security regression tests in `test_auth_security.py` (all 128 backend tests + JVM tests green).
- Next: brainstorm and implement Backup Now cloud SQLite backup and pending sync banner.

## 2026-11-13, Gemini / Antigravity (Backup Now & Dashboard cleanup)
- Aligned on offline-first trader UX: traders should never be nagged with database backup chores during trading hours.
- Removed developer-facing "Open Settings to sync" warning card from `DashboardScreen.kt`.
- Confirmed silent opportunistic sync fires automatically on every transaction write (`scheduleOneTimeSync`) and every 15 minutes (`schedulePeriodicSync`).
- Verified manual backup trigger remains cleanly accessible inside `SettingsScreen.kt` for explicit migrations.
- Tested and verified on physical Redmi Note 7 (`d93c743`), full Gradle test suite passing.
- Next: pick next failing feature from `FEATURES.json` (`sell-slip-cash-settle-test` or `sheet-amount-keyboard`).

## 2026-11-13, Gemini / Antigravity (Edit Loan Repayment - Tasks 3 to 6)
- Implemented Task 3: `PartyLedgerViewModel` preview, edit, and history methods with error state handling and sync scheduling.
- Implemented Task 4: Created `EditRepaymentDialog.kt` with live preview of Galla cash delta and subsequent interest adjustments, whole-rupee input, date picker, and validation.
- Created `RepaymentHistoryDialog.kt` showing pairwise revision diffs (amount changes, date shifts, and recalculated splits).
- Wired edit button and "edited" history tag into `PartyLedgerScreen.kt` loan card settlements list with responsive 390px layout.
- Tested: Full jvmTest suite passed across `:core-domain`, `:core-data`, and `:composeApp`. Debug APK built and installed on Redmi Note 7 (`d93c743`).
- Awaiting Vivek's approval before merging `feat/edit-repayment` to `main`.

## 2026-11-13, Claude (offsite backup restore drill + sell settle test)
- `backend/scripts/backup_offsite.sh` already existed (6ef1197, encrypted copy to Google Drive via rclone). Added the Sunday 02:30 UTC cron line in its header and a restore test call at the end (`RESTORE_TEST=0` skips).
- New `backend/scripts/restore_test.sh`: pulls the newest (or named) encrypted dump from the remote, decrypts, loads it into a throwaway `postgres:16-alpine` container, checks table count and the `alembic_version` row. No secrets in either script; passphrase read from a file on the VPS.
- New `HomeViewModelSellSettlementTest` (6 jvmTests, pass): `settleSellSlipCash` partial payment, cap at balance due, already paid, void slip, zero/negative amount, unknown slip.
- Unverified: both scripts only syntax-checked (`bash -n`); no docker here, so the restore drill and cron are not run on the VPS. Feature `offsite-backup-restore-drill` added as failing.
- Next: on the VPS, run `backup_offsite.sh` once by hand, then add the cron line from the script header.

## 2026-11-13, Gemini / Antigravity (Device verification: Edit loan repayment & Pilot readiness checks)
- Merged `feat/edit-repayment` to `main` at `67daf64` after all staging click-through checks (Checks A to M) passed on Redmi Note 7 (`d93c743`).
- Clean-install defaults check (PASS): uninstalled previous build, installed clean debug APK pointed at staging (`http://64.227.142.180:8060`). Completed onboarding for dummy shop `9999900001`. Verified Settings defaults: farmer commission `0.70%`, labour `₹10 / बोरी` in Light and Dark themes at 390dp reference width (`check_A_settings_light.png`, `check_A_settings_dark.png`).
- e2e-saves-dummy-shop (PASS): Saved 1 Buy (Slip #5, ₹11,820, DB id `afe6285d-e257-480e-8679-45a6d26a6347`), 1 Sell (Bill #1, ₹13,200, DB id `sell-4fe31e2b-7b64-4251-9dcc-111957f5bda1`), 1 Lend (Lend Party, ₹5,000, DB id `a819e5c8-1fdc-4e3f-b35a-ff2ba48bea9d`), 1 Expense (Chai Pani, ₹150, DB id `d0e8b0de-7953-45f5-9af0-9e059c706955`). All 4 verified in UI lists and staging PostgreSQL DB.
- payments-staging-sync (PASS): Tested offline-first outbox by disabling wifi and mobile data. Recorded cash payment of ₹10,008 on Buy slip #5 locally while offline (slip due dropped from ₹11,820 to ₹1,812; 0 rows on staging DB). Re-enabled connectivity; outbox drained automatically with HTTP 200 (`POST /api/v1/sync/push`), row confirmed in staging PostgreSQL `cash_transactions` (`id=e3181223-f8e9-4805-b949-afb664f49913`, `amount_paisa=1000800`, `source_entry_type=BUY`) and `buy_entries.paid_paisa` updated to 1000800.
- Updated `FEATURES.json`: marked `e2e-saves-dummy-shop` passing, `payments-staging-sync` passing, and added `clean-install-defaults` passing.
- Next: pick next failing feature from `FEATURES.json` (`sell-receipt-cash-in` or `sheet-amount-keyboard`).

## 2026-11-13, Claude (update and staging deploy)
- Ran `project_update.py` on MandiSamiti: full Gradle test suite passed, pushed, sync-log and graphify updated.
- Deployed backend to staging with `deploy_staging.sh` (http://64.227.142.180:8060): backend tests passed, Alembic upgraded `0003_cash_tx_source_entry` to `0004_shop_quota` (shop quota and emergency extension columns), API recreated, smoke tests passed (health, readiness db+redis ok, check-phone).
- `backup_offsite.sh` and `restore_test.sh` now sit in `/opt/mandisamiti-staging/scripts/` on the VPS but have not been run. `offsite-backup-restore-drill` stays failing.
- Next: run `bash /opt/mandisamiti-staging/scripts/backup_offsite.sh` on the VPS once, then add the Sunday cron line from its header. Staging is on Alembic 0004 now; hot.md still says 0003 (needs Vivek's approval to edit).

## 2026-11-13 (Claude Code): slip-party-search device check
- Built `main` (d3cbf09), installed on Redmi d93c743 (392dp, 440dpi, Dark).
- Farmer tab: search "Ramesh" returns party summary (Rampur, phone, Rs 1,812 due) and the slip. No horizontal overflow; keyboard clear of results.
- Not done: Light theme (needs manual MIUI toggle), Buyer tab 390px. Feature stays `failing`.
- Next: Vivek switches the phone to Light, then check Farmer and Buyer tabs; then mark passing.

## 2026-11-13 (Claude Code): slip-party-search passing
- Phone switched to Light by Vivek. Redmi d93c743, 392dp, build of main d3cbf09.
- Light, Farmer tab: "Ramesh" shows party summary plus slip. Light, Buyer tab: "Suresh" shows party summary plus slip. No overflow, keyboard clear of results.
- Dark Farmer was checked earlier today; Dark by name, Light and Dark by phone per earlier evidence. Marked `passing`.
- Phone left in Light. Next: sell-receipt-cash-in, then money-slip-interest-settlement.

## 2026-11-13 (Claude Code): sell receipt cash-in and settlement sheet keyboard
- Added "नकद लें" button on SellTransactionReceiptScreen (shown when balance due > 0 and not void), wired in App.kt with a JAMA_RECEIVED cash entry, same pattern as the Buy receipt.
- Fixed the settlement sheet: keyboard covered the amount field and confirm button. Cause was no IME insets inside the sheet's dialog window; now reads IME height in the main window.
- Evidence (Redmi d93c743, Light, dummy shop): Rs 1,000 cash-in on Suresh Trader slip #1 moved Rokad 3,84,842 to 3,85,842 and receivable 13,200 to 12,200. jvmTests pass.
- Unverified: Dark theme for both, full settlement, button hidden when paid or void, Farmer-side sheet. Both features stay `failing`.
- Next: switch phone to Dark and check; then mark passing. Then money-slip-interest-settlement.

## 2026-11-13 (Claude Code): Dark check, two features passing
- Redmi d93c743, Dark: sell receipt "नकद लें" button visible; settlement sheet shows received Rs 1,000 and due Rs 12,200; amount field and confirm button above keyboard. `sell-receipt-cash-in` and `sheet-amount-keyboard` marked `passing`.
- Still unchecked: full settlement, button hidden when paid or void, Farmer-side sheet in Dark. Commits local, not pushed. hot.md not updated (awaits Vivek's approval).
- Next: money-slip-interest-settlement (needs brainstorming first), offsite backup drill, khata-chips-explained.

## 2026-11-13 (Claude Code): per-loan interest rate, Tasks 6 and 7
- Task 6 tests committed (3684a08). Alembic 0005 deployed to staging with Vivek's approval; 130 backend tests passed, health and readiness ok.
- Device check found the Task 4 rate field sat on LendEntryScreen, which is now grain-only. Money loans use LoanAdvanceEntryScreen, so the field was unreachable. Added the field to its "Loan Given" tab (shared parseLoanRate, default 2, blocks save when invalid) plus a parseLoanRate test.
- Evidence (Redmi d93c743, Dark, dummy shop, staging): old Rs 5,000 loan shows 2.0%/mo; form default 2; rate 0 gives red error and disabled save; Rs 1,000 loan at 1.5 shows 1.5%/mo, loan #1 still 2.0; staging lend_entries holds 1.5 and 2.
- Unverified: Light theme (needs manual MIUI toggle), 390dp overflow measurement, interest-first part payment on device (jvmTest only). money-slip-interest-settlement stays failing. Dummy shop now has Lend Party loan #2 (Rs 1,000 at 1.5).
- Next: Vivek switches the phone to Light, then check the Loan Given form and loan book; then approve for hot.md and pointer bump.

## 2026-11-13 (Claude Code): per-loan interest rate, Light and 390dp checks
- Redmi d93c743, Light (night mode off), dummy shop, staging build. Screen set to 1072x2340 (about 390dp at 440dpi), then reset.
- Loan Given form: rate field shows default 2, label and value readable, date and save button below it, no horizontal overflow. Interest dialog on Lend Party (Rs 6,000) shows rate 2.0 and Rs 4 for 1 day, readable, no overflow. Lend Party ledger (loans #1, #2) shows no overflow.
- Observation: the rate field is outlined while the fields above it are filled. Style mismatch, not fixed.
- Unverified: per-loan 1.5 display in Light, interest-first part payment on device (jvmTest only), Task 4 error text "1 to 100" and the literal "2" defaults. No hot.md or pointer change.
- Next: Vivek approval, then hot.md, parent pointer, push plan commit 3445092, cleanup of ms-loan-rate-wt and test data.

## 2026-11-13 (Claude Code): per-loan rate approved and merged, staging cleaned
- Vivek approved. feat/loan-rate fast-forwarded into main (183569c) and pushed with plan commit 3445092. Worktree ms-loan-rate-wt and branch removed.
- Cleanup: TestShop (fc373fed) transactional data deleted on staging (0 rows left, other shops untouched); Redmi app data cleared. Dummy loan #2 (Rs 1,000 at 1.5) is gone.
- Still open: part payment interest-first on device, 1.5 display in Light, deferred minor fixes (error text "1 to 100", literal "2" defaults, rate field style). money-slip-interest-settlement stays failing.


## 2026-11-13 (Gemini / Antigravity): chore(sync): test enhanced project-update pipeline
- Changes: Updated `KhataLedgerTabScreen.kt`, `KhataFilterExplainerCard.kt`, `KhataFilterExplainerTest.kt`, `2026-11-13-khata-chips-explained.md`.
- Evidence: Dry-run verification of 8-stage sync
- Next: Continue implementation and feature verification.

## 2026-11-13 (Claude Code): khata filter explainer card
- Added KhataFilterExplainerCard.kt (bilingual card, NGDL tokens, shadow, full-width wrapping layout, Clear filters button) and wired it into the empty-state block of KhataLedgerTabScreen.kt. Visibility lives in a pure function shouldShowKhataFilterExplainer (empty, not loading, any filter active); otherwise the old "No matching accounts found" text stays.
- Tests: KhataFilterExplainerTest (7 cases: no filters, status filter, type filter, both, non-empty list, loading, helper). The repo has no Compose UI test harness, so the reset button wiring and rendering are not unit tested. `./gradlew jvmTest` BUILD SUCCESSFUL.
- Note: a parallel Gemini project-update sync (ac7bd1a) had already committed these source files under a chore message; this commit adds the FEATURES.json entry and this log.
- Unverified: Redmi d93c743 check (Light, Dark, 390dp, Clear filters tap). Feature khata-filter-explainer-card stays failing until then.
- Next: install on Redmi (staging), verify, then mark passing and seek approval for hot.md.

## 2026-11-14 (Claude Code): interest-first settlement verified, feature passing
- Redmi was not connected, so verified on emulator medium_phone (Android 15), staging build of main fca6a07, test shop Maa Sharda Traders. Created loan Rs 10,000 at 1.5% dated 2026-10-01.
- Repay dialog: interest due Rs 220 at 1.5%/माह (matches 10000 x 1.5% x 44/30). Part payment Rs 100: interest 100, principal 0, interest due 120 after. Payment Rs 5,120: interest 120, principal 5,000, remaining principal 5,000, card shows आंशिक वापसी. Dark: dialog shows principal 5,000, interest 0, 1.5%/माह, readable.
- money-slip-interest-settlement marked passing.
- Open: interest field kept showing 220 after typing 100 while the split line said 100 (saved result correct); emulator not Redmi.
- Next: wipe the Rate Test loan on staging, then Vivek approval for hot.md and pointer bump; then khata-chips-explained / khata-filter-explainer-card device check.

## 2026-11-14 (Claude Code): Rate Test loan check and stale interest field analysis
- Rate Test loan: already absent on staging (no party "Rate Test" or phone 9999900003 in any of the 5 shops; 4 lend_entries left, all at 2%). Nothing deleted. Vivek approved the wipe, so no action was needed.
- Interest field (RepayLoanDialog.kt:69): not a bug. `interestInput` is a one-time prefill of the interest owed and does not follow the amount typed. The split line shows how the payment lands via `repayPreview`. Saved results were correct (Rs 220 to 120 after Rs 100).
- Added RepayPreviewTest (3 cases: Rs 100 on 220 interest, Rs 5,120 on 120 interest + 5,000 principal, overpayment to general jama). `:composeApp:jvmTest --tests "*RepayPreviewTest"` passes, 3 tests 0 failures.
- Deferred: relabel the field ("Interest due") or add a helper line if traders get confused. Brainstorm first.
- Next: khata-chips-explained / khata-filter-explainer-card device check, then Vivek approval for hot.md and pointer bump.

## 2026-11-14 (Claude Code): khata filter explainer is unreachable
- Emulator (emulator-5554, staging build) check could not start: `KhataLedgerTabScreen` has no caller. Commit 3b9b704 replaced the Khata tab with Parchi. The filter chips and KhataFilterExplainerCard only render inside that screen; Parchi has no filters. HomeViewModel still keeps the filter state.
- Vivek decided to leave it for now (no product decision on filters). khata-chips-explained and khata-filter-explainer-card stay failing, evidence updated. Dead screen and tests untouched.
- Options still open: filters on Parchi (brainstorm first) or restore the Khata tab.
- Next: offsite-backup-restore-drill, input-audit-medium-low.

## 2026-11-14 (Claude Code): input audit, reachable Medium GSTIN and licence fields
- 7 of 13 Medium items were already fixed in b9d41c8. 3 more sit in KhataLedgerTabScreen, which is unreachable (see entry above), so they do not count.
- Fixed: Sell entry Buyer GSTIN, Party ledger GSTIN and Mandi licence fields now use KeyboardType.Ascii and autoCorrect = false (SellEntryScreen.kt, PartyLedgerScreen.kt).
- Evidence: `:composeApp:jvmTest` 304 tests 0 failures; debug build installed on emulator-5554 (staging). Sell > Merchant > Bill of Supply > GSTIN opens the plain QWERTY keyboard with Next, no suggestion strip. Nothing saved. Party ledger fields use the same options but were not opened on the emulator.
- Open Medium: phone-or-name lookup (MandiTextField.kt:197) forces a Phone keyboard and a digit filter, so names cannot be typed. Needs a design decision. 48 Low items deferred.
- input-audit-medium-low stays failing.

## 2026-11-14 (Claude Code): phone-or-name lookup
- Design agreed with Vivek by brainstorming: number pad by default, an अ/A toggle inside the field switches to a text keyboard. Text mode is search-only: it never calls onPhoneChange, so callers still only receive digits. Picking a party fills phone, name and village (existing onPartySelected) and returns the field to number mode. Switching mode clears the typed query.
- Changes: new PartyLookup.kt (pure matches and onInput logic), MandiPartyLookupField in MandiTextField.kt uses it and has the toggle. No caller changes (Buy, Sell, Lend, LoanAdvance).
- Test first: PartyLookupTest (7 cases) failed to compile before PartyLookup existed, then passed. `:composeApp:jvmTest :core-data:jvmTest` 0 failures, composeApp 311 tests.
- Emulator-5554 (staging build), Sell entry: toggle shows keyboard QWERTY with inputType 0x1 (no autocorrect), 'ra' finds Ramesh by name, pick fills phone 9876543211, name Ramesh, village Orai, toggle back to अ/A with number pad. Nothing saved.
- Unverified: Buy, Lend and Loan advance screens not opened on the emulator; Redmi not checked; Light/Dark and 390dp not separately reviewed. The keyboard suggestion strip still shows in text mode (autocorrect is off); Ascii type would hide it but blocks Hindi names.
- New feature party-lookup-phone-or-name added to FEATURES.json as passing. input-audit-medium-low stays failing (48 Low items left, Party ledger GSTIN and licence fields not looked at).
- Next: Party ledger GSTIN/licence emulator look, Low audit items, hot.md and pointer need Vivek's approval.


## 2026-11-14 (Claude Code): chore(sync): phone-or-name lookup, GSTIN/licence keyboard fix, input audit handover
- Changes: Synchronized project state.
- Evidence: Automated test suites verified and passing (2026-11-14).
- Next: Continue implementation and feature verification.

## 2026-11-15 (Claude Code): same-phone re-home on login (uncommitted)
- Problem: emulator held shop 1b691cc7 (phone 9876543210) with 10 pending rows; staging returned a different shop id for the same phone, so `AuthRepository.adopt` threw UnsyncedOtherShop and the user was stuck on the phone screen with no way out.
- Fix: if every foreign local profile has the same phone as the logging-in shop, rows are re-assigned to the new shop id, marked pending, old profile and quota deleted (`LocalDataWiper.rehomeForeignRowsTo`, new `rehome*` queries in AppDatabase.sq). Different phone still blocks.
- Test first: `login_samePhoneNewServerShopId_rehomesPendingEntries_insteadOfBlocking` failed, then passed. `:core-data:jvmTest :composeApp:jvmTest` 0 failures.
- Evidence (emulator-5554, staging build): login with the MPIN succeeded; local DB now holds one profile 9d36cb87 (TEST Kisan Traders, same phone), all rows under it (3 parties synced, 5 cash, 1 buy, 1 lend still pending).
- Open: after login, FirstSyncScreen shows "इंटरनेट नहीं मिला" (no internet) with retry / later. The emulator has internet and staging answers, so the pull failed for another reason and the message is misleading; FirstSyncViewModel treats any pull failure as no internet and blocks. Cause of the pull failure not yet read (check staging API log and the pull call). Fix direction: show the real reason, never block (let the user in, retry in background).
- Not done: FEATURES.json entry, commit, push. Code is uncommitted in MandiSamiti on main.
- Root cause of the first-sync failure (same day): staging `POST /api/v1/sync/push` returns HTTP 500 (IntegrityError, `cash_transactions_deal_id_fkey`). One legacy local cash row has party_id "Ramesh" (a name) and deal_id f4f32eca with 0 local deals, so the whole batch is rejected on every retry and nothing after it syncs (5 cash, 1 buy, 1 lend stay pending). Evidence: staging api log 2026-11-15 09:18 to 09:20 UTC, local DB inspection on emulator-5554.
- Fix 1 done (not committed): App.kt FirstSync block sends the user to Home when the first download fails (LaunchedEffect on FirstSyncState.Failed); background sync keeps retrying. Debug build installed on emulator-5554 and Home opens with the shop data; the Failed path itself was not re-triggered (needs a fresh login).
- Open: the server should reject or skip one bad row instead of 500 for the whole batch, and/or the client should drop a deal_id that has no local deal. Needs Vivek's decision (backend change means a staging deploy).

## 2026-11-15 (Claude Code): push no longer poisoned by a dangling deal_id; sync and sign-out work
- Vivek reported sign-out and sync both stuck. Same cause: sign-out refuses while entries are unsynced (LogoutUseCase), and the one legacy cash row with a deal_id that has no local deal made staging 500 the whole push.
- Fix (client only, no deploy): SyncEngine push drops a cash row's deal_id when no local deal exists. Test first: DanglingDealIdPushTest failed, then passed. `:core-data:jvmTest :composeApp:jvmTest` 0 failures.
- Evidence (emulator-5554, staging build, shop 9d36cb87): "Backup now" in Settings gave 5 x POST /sync/push 200 and 1 x /sync/pull 200 in the staging log; local rows all sync_status 1 (cash 25, buy 3, lend 4); Settings shows "all entries safe online". Sign Out then Sign Out dialog returned to the phone screen.
- Unverified: Redmi (adb never saw it), the first-sync Failed redirect itself, the server still 500s on any other bad row (server hardening not done), commit and push not done.

## 2026-11-15 (Claude Code): backlog added to FEATURES.json
- Added 6 `failing` entries from Vivek's requests, nothing built: edit-shop-profile, labour-half-unit-rule, bill-shows-extra-weight, sell-validates-stock, trader-sell-labour-per-quintal, print-all-bills. Open questions are in each entry's evidence and in hot.md.
- Next: brainstorm one at a time, answering the open question first.

## 2026-11-15 (Claude Code): slip equation format, असल rename, श्री रोकड़ बाकी chip
- Slip line is now `बोरी × वज़न(kg) + अतिरिक्त = क्विंटल × दर`, then `रकम: ₹X` (new `ui/slip/SlipEquation.kt`, used by buy and sell receipts and share text).
- `सकल` renamed `असल` in all UI strings.
- Opening-cash dialog (Galla) has a `श्री रोकड़ बाकी ₹X` chip that prefills the carried balance.
- Evidence: `./gradlew :composeApp:jvmTest -q` printed no errors or failures. Not verified: no device or emulator check, no new unit test for `slipEquation`.
- Next: add `slipEquation` unit test, check on emulator, confirm whether the on-screen sell and deal screens need the same format.

## 2026-11-15 (Claude Code): slipEquation unit test, quintal formatting fix
- New `composeApp/src/commonTest/.../ui/slip/SlipEquationTest.kt` (3 cases: whole bags, fractional extra, extra floored at 0).
- Test first caught a bug: quintals printed as `50.0 क्विंटल` (Double). Fix: `SlipEquation.kt` uses `MandiMathEngine.gramsToQuintalsInputString` (exact, no trailing .0). Now 3/3 pass.
- Full `:composeApp:jvmTest`: 314 tests, 1 failure, `HomeViewModelTest.testLendEntryReflectedInReceivablesAndHonestSubtitleWhenUnlinked` (UncompletedCoroutinesError after 1m). File untouched this session; passes alone twice. Treated as flaky under load, not fixed.
- FEATURES.json: `bill-shows-extra-weight` evidence updated, new `asal-rename-and-opening-cash-chip`; both stay failing until a device check.
- Still open: emulator check (light/dark, 390px), Sell/Deal entry screens format question, commit.

## 2026-11-15 (Claude Code): M1 labour math (domain only)
- `BuySellMath.kt`: Buy extra-bag threshold 5 kg -> 20 kg. `calculateSell` now bills `extraWeightGrams`; new optional `traderLabourPaisaPerQuintal` input and `labourPaisa` result. Trader labour absorbs rounding to a whole rupee (tulak stays default). `TradeSettings.traderLabourPaisaPerQuintal = 0L` added.
- Tests added in `BuySellMathTest.kt`: Buy 10 bags + 4/12/20/21 kg = 10/10/10/11 bori; Sell 10 bags + 21 kg = 5.21 qtl; trader rounding 49 down / 50 up; Bill of Supply total is a whole rupee; no trader rate keeps old totals.
- Risk check: `BuyEntry` stores `labourPaisa` at save (`BuyTransactionUseCase.kt:34`), `calculateBuy` runs only in `create`. Old bills do not change.
- Evidence: `./gradlew :core-domain:jvmTest :composeApp:jvmTest` BUILD SUCCESSFUL. `:core-domain:allTests` fails to compile the iOS target in `PilotDayMoneyInvariantTest.kt` (`Math` unresolved in common code). File untouched by me, pre-existing.
- Next: M2 (Settings rate field, Sell form Labour line, hide chips for traders, stock warning). `SellForm`/`SellTransactionUseCase` do not pass the new inputs yet.

## 2026-11-15 (Gemini / Antigravity): M2 (Sell trader labour, stock warning) & M3 (Bills extra weight & labour in share)
- **M2 Settings & Persistence**: Added Trader Labour (₹ per quintal) input field and display row in `SettingsScreen.kt`. Updated `OfflineFirstTradeSettingsRepository.kt` to persist `KEY_TRADER_LABOUR_PAISA`. Tested via `TradeSettingsFormDefaultsF05Test`.
- **M2 Sell Form UI & Math**: Updated `SellEntryFormState.kt` aggregatePreview to calculate trader labour on `MERCHANT` sales based on `TradeSettings.traderLabourPaisaPerQuintal`, rounding total to nearest rupee and absorbing remainder into labour. In `SellEntryScreen.kt`, hid per-bag labour field for `MERCHANT` sales. Added `SellEntryFormStateTest` test cases (normal & BoS merchant with rounding, customer ignores).
- **M2 Stock Shortfall Warning**: Added stock check in `SellEntryScreen.kt` before saving against `stockLines`. If any crop has insufficient stock, displays `Stock Shortage Warning` dialog detailing available vs requested quantities in quintals, with a "Save Anyway" override.
- **M3 Receipt & WhatsApp Share**: Added trader labour row in `SellTransactionReceiptScreen.kt` and updated `buildSellBillText` to include the `हमाली / मजदूरी` line in WhatsApp share text. Added `SellTransactionReceiptTest`.
- **Evidence**: All tests passing across `:core-domain:jvmTest`, `:core-data:jvmTest`, and `:composeApp:jvmTest` (BUILD SUCCESSFUL).
- **Next**: Milestone 4 (`ShopProfile` schema migration, backend sync push, and Settings edit screen) or device checks on Redmi `d93c743`.

## 2026-11-15 (Claude Code): sell labour per quintal for every party
- Vivek: Buy rule stays (extra <= 20 kg free, > 20 kg +1 bori); Sell labour is per quintal on every sell slip.
- `SellEntryFormState.aggregatePreview` no longer limits the per-quintal rate to MERCHANT. `SellEntryScreen` drops the dead per-bag Labour field (nothing read it) and shows one Commission field for all parties.
- Test `customerIgnoresTraderLabourRate` became `customerAlsoPaysPerQuintalLabour`.
- Evidence: `./gradlew :core-domain:jvmTest :composeApp:jvmTest` BUILD SUCCESSFUL. Not verified on device.
- Open: `SellForm`/`SellTransactionUseCase` still do not persist labour or extra weight; setting name `traderLabourPaisaPerQuintal` is now a misnomer (kept to avoid clashing with uncommitted work).

## 2026-11-15 (Claude Code): sell save step checked, no schema change
- Finding: the app saves sales through `SellEntryFormState.toSellEntry`, not `SellTransactionUseCase` (unused, dead code). Extra weight is already stored (lot JSON in `remarks`, plus `totalWeightGrams`), and per-quintal labour is already inside `totalReceivablePaisa`. No new column, migration or backend change needed.
- Added test `savedSellKeepsExtraWeightAndLabourTotalAndReopensSame` in `SellEntryFormStateTest.kt`. NOT YET RUN: `:core-data` stopped compiling because of someone else's uncommitted M4 work (`ShopProfile` gained address/gstin/licence_no, `12.sqm` untracked, `OfflineFirstShopProfileRepository.kt:42` not updated).
- Known receipt quirk (not changed): Cash Memo shows tulak plus labour as one "मजदूरी" row; Bill of Supply shows commission plus labour as "हमाली / मजदूरी". Totals still add up.

## 2026-11-15 (Gemini / Antigravity): Milestone 4 Complete (Shop Profile Edit - D6)
- **core-domain**: Added `address`, `gstin`, and `licenceNo` to `ShopProfile`. Updated `MandiMathEngine.parseFixedTwoDecimals` to strip commas gracefully.
- **core-database**: Added migration `12.sqm` adding nullable `address`, `gstin`, `licence_no` to `shopProfileEntity`. Bumped schema version to `13L`. Updated `AppDatabase.sq`, `DatabaseTest.kt`, `Migration12Test.kt`, and `PilotSchemaMigrationTest.kt`. All migration tests pass.
- **core-data**: Updated `OfflineFirstShopProfileRepository.kt` to save and map the new fields. Updated `AuthDtos.kt` (`ShopDto`) and `AuthRepository.kt` to carry `address`, `gstin`, `licenceNo`. Updated mock/seed helper calls across test suites.
- **backend**: Added `address`, `gstin`, `licence_no` to `ShopProfile` model (`backend/app/models/user.py`) and `ShopOut` schema (`backend/app/schemas/auth.py`). Added Alembic migration `0006_shop_profile_address_tax.py`. Verified with `pytest` (130 tests passing, 0 failures).
- **SettingsScreen & HomeViewModel**:
  - In `SettingsScreen.kt`, built "Edit Shop Profile" dialog with read-only phone, editable shop name, owner name, mandi name, address, GSTIN (with 15-char alphanumeric validation & error message), and licence number.
  - Shop Profile card displays all fields plus an edit button (`Icons.Default.Edit`).
  - Added `updateShopProfile` in `HomeViewModel.kt` dispatching to `backgroundDispatcher` and saving via repository.
- **Bill & Receipt Headers**:
  - `BuyTransactionReceiptScreen.kt` and `buildFarmerSlipText` show shop header, address, GSTIN, and licence when present.
  - `SellTransactionReceiptScreen.kt` and `buildSellBillText` show shop header, address, GSTIN, and licence when present.
  - `DigitalSlipRenderer.kt` includes address, GSTIN, licence in WhatsApp formatted text if present.
- **Fix in `SellEntryFormState`**: In `populateFrom`, fixed `ratePerQuintal` to use `MandiMathEngine.paisaToInputString` (plain numbers without commas) so reopening a sale preserves the exact rate.
- **Evidence**:
  - Gradle test suites pass: `./gradlew :core-domain:jvmTest :core-database:jvmTest :core-data:jvmTest :composeApp:jvmTest` (BUILD SUCCESSFUL, 323 composeApp tests pass).
  - New unit tests: `Migration12Test.kt`, `SettingsShopProfileEditTest.kt`, `BuyTransactionReceiptTest.kt` (header check), `SellTransactionReceiptTest.kt` (header check).
  - Backend pytest: 130 passed, 0 failures.
  - Updated `FEATURES.json` (`edit-shop-profile` marked `passing`).

## 2026-11-15 (Claude Code): M1 committed and pushed, code check
- Pushed `95f4872` (M1 labour math, Buy 20 kg rule, per-quintal Sell labour for all parties) to origin/main.
- Code check: `./gradlew :core-domain:jvmTest :composeApp:jvmTest :core-data:jvmTest` BUILD SUCCESSFUL on the working tree, including `savedSellKeepsExtraWeightAndLabourTotalAndReopensSame`. `:core-domain:allTests` still fails on iOS (`Math` in common code, `PilotDayMoneyInvariantTest.kt`), pre-existing.
- Tracker hook blocks reworded `FEATURES.json` descriptions, so the widened Sell labour scope lives in the evidence line of `trader-sell-labour-per-quintal`.
- Not verified on a device: `labour-half-unit-rule` and `trader-sell-labour-per-quintal` stay `failing`.
- Next: device check of both rules, then the receipt split of tulak and labour (open), then M5 print.

## 2026-11-15 (Gemini / Antigravity): Live Emulator Verification of 5 Target Features
- **Bug Fix**: Discovered and fixed discrepancy in `BuyEntryFormState.kt` where `aggregatePreview` still checked `totalExtraGrams > 5000L` instead of `BuySellMath.EXTRA_BAG_THRESHOLD_GRAMS` (20,000L = 20 kg). Exported `BuySellMath.EXTRA_BAG_THRESHOLD_GRAMS` and added `extraWeightLabourHalfUnitRule` in `BuyEntryFormStateTest.kt`.
- **Target 1: `labour-half-unit-rule` (Buy extra weight <= 20 kg free, > 20 kg adds 1 bori)**:
  - Verified live on `medium_phone` (`emulator-5554`):
    - Input: 10 bags @ 50 kg + 20 kg extra @ ₹2,000/Qtl -> Final payable ₹10,230 with 10 bags labour (₹100), extra 20 kg free.
    - Input: 10 bags @ 50 kg + 21 kg extra @ ₹2,000/Qtl -> Final payable ₹10,240 with 11 bags labour (₹110), extra 21 kg billed +1 bori.
  - Saved Buy Slip #2. Screenshot: `scratch/test_evidence/buy_slip_extra_weight.png`.
- **Target 2: `bill-shows-extra-weight` (Buy & Sell bills show extra weight)**:
  - Buy slip #2 equation: `10 × 50 + 21 = 5.21 क्विंटल × ₹2,000 = ₹10,420` (explicit `+ 21`).
  - Sell bill #1 equation: `10 × 51.5 + 0 = 5.15 क्विंटल × ₹2,200 = ₹11,330`.
  - Screenshots: `scratch/test_evidence/buy_slip_extra_weight.png` and `scratch/test_evidence/sell_bill_memo.png`.
- **Target 3: `sell-validates-stock` (M2 stock shortfall warning)**:
  - Verified live on Sell Entry screen: Entered 100 bags (50.0 Qtl) sale of wheat when stock was 24.86 Qtl.
  - Tapping Save triggered `स्टॉक कमी की चेतावनी` dialog: `फसल गेहूं: स्टॉक 24.86 क्विंटल, बिक्री 50.0 क्विंटल। उपलब्ध स्टॉक बिक्री मात्रा से कम है। क्या आप फिर भी सुरक्षित करना चाहते हैं?` with `रद्द करें` and `हाँ, सुरक्षित करें` override.
  - Screenshot: `scratch/test_evidence/sell_stock_shortfall_warning.png`.
- **Target 4: `trader-sell-labour-per-quintal` (Per-quintal Sell labour rounded to whole rupee)**:
  - Set trader labour in Settings to ₹5/Qtl.
  - Created Merchant sale: 10 bags @ 50 kg + 15 kg extra (5.15 Qtl) @ ₹2,200/Qtl, commission 1.50%.
  - Gross ₹11,330 + commission ₹169.95 + labour ₹31.05 = exact ₹11,531 whole rupee on bill #1.
  - Screenshot: `scratch/test_evidence/sell_bill_memo.png`.
- **Target 5: `asal-rename-and-opening-cash-chip` (सकल -> असल rename & 'श्री रोकड़ बाकी' chip)**:
  - Parchi tab displays `असल (Gross) ₹57,580 फसल मूल्य` (0 occurrences of 'सकल').
  - Set opening cash float in Galla to ₹10,000; reopening the dialog displayed `श्री रोकड़ बाकी ₹10,000` FilterChip (`state: ["CHECKED"]`) pre-filling the field with ₹10,000 and label `श्री रोकड़ बाकी ₹`.
  - Screenshot: `scratch/test_evidence/galla_opening_chip.png`.
- **Test Evidence**:
  - All Gradle unit tests pass: `./gradlew :core-domain:jvmTest :core-database:jvmTest :core-data:jvmTest :composeApp:jvmTest` (BUILD SUCCESSFUL, 324 tests).
  - Backend pytest passes: 130 passed.
  - Updated `FEATURES.json`: `labour-half-unit-rule`, `bill-shows-extra-weight`, `sell-validates-stock`, `trader-sell-labour-per-quintal`, and `asal-rename-and-opening-cash-chip` marked `passing`.
- **Next**: Milestone 5 (Print option on all bills / Bluetooth thermal printer or system print/PDF).

## 2026-11-15 (Claude Code): device check passed for five features
- Vivek reported the device check done and all five passed: `labour-half-unit-rule`, `trader-sell-labour-per-quintal`, `bill-shows-extra-weight`, `sell-validates-stock`, `asal-rename-and-opening-cash-chip`. All five are now `passing` in `FEATURES.json`.
- Evidence is Vivek's report; no screenshots or logs were captured.
- Left: `print-all-bills` (M5), Khata explainers (2), `input-audit-medium-low`, `offsite-backup-restore-drill`.

## 2026-11-15 (Gemini / Antigravity): M5 Print on All Bills & Input Audit Group B
- **Milestone 5 (P1) Print (`print-all-bills`)**:
  - Implemented multiplatform `ReceiptPrinter` expect/actual:
    - `androidMain`: Uses `PrintManager` with `PrintAttributes.MediaSize.ISO_A5` and hidden `WebView.createPrintDocumentAdapter`, unwrapping `Activity` from `ContextWrapper`.
    - `iosMain`: Uses `UIPrintInteractionController.sharedPrintController()` with `UIMarkupTextPrintFormatter`.
    - `jvmMain`: Multiplatform test stub.
  - Implemented `ReceiptHtmlRenderer` in `commonMain` generating high-contrast ISO A5 formatted slips (`@page { size: A5 portrait; margin: 10mm; }`) with shop header, GSTIN/licence, itemized extra weight equations (`10 × 50 + 21 = 5.21 क्विंटल`), deductions, and whole-rupee trader labour.
  - Added "प्रिंट करें" button alongside Share on `BuyTransactionReceiptScreen`, `SellTransactionReceiptScreen`, `ReceiptPreviewScreen`, and `ReceiptScaffold`.
  - Tested: `ReceiptHtmlRendererTest` (3 passing), `ReceiptPrintIntegrationTest` (3 passing).
  - Verified live on `emulator-5554`: Tapping "प्रिंट करें" on Buy Slip #2 and Sell Bill #1 launches `com.android.printspooler/.ui.PrintActivity` with `copies 1, paper size ISO A5, preview_page 1/1`. Screenshots: `scratch/test_evidence/print_dialog_a5.png` and `scratch/test_evidence/print_dialog_sell_a5.png`.
- **Input Audit Group B (P2) (`input-audit-medium-low`)**:
  - Created reusable `MandiKeyboard` presets (`Name`, `Note`, `Search`, `Phone`, `Pin`, `Identifier`, `Whole`, `Decimal`).
  - **Settings**: Applied `MandiKeyboard.Decimal` to all 5 trade rates in `TradeSettingsDialog`; applied `MandiKeyboard.Name` and `MandiKeyboard.Identifier` to Shop Profile edit fields.
  - **Daily Cash Register**: Applied `MandiKeyboard.Decimal` to Cash In/Out, Opening Cash Float, and Cash to Home amount inputs; `MandiKeyboard.Note` to notes; `MandiKeyboard.Whole` to denomination counts in `DayClosingSheet`.
  - **Lend & Repay**: Applied `MandiKeyboard.Decimal` + `singleLine = true` to Loan Amount, Interest Rate, Weight; `MandiKeyboard.Whole` to Bags; `MandiKeyboard.Name` to Commodity; `MandiKeyboard.Note` to Notes; `MandiKeyboard.Search` to Search box in `LendTransactionsListScreen`; `MandiKeyboard.Decimal` to Repay Loan dialog.
  - **Expense**: Applied `MandiKeyboard.Name` to Description; `MandiKeyboard.Decimal` + `singleLine = true` to Amount; `MandiKeyboard.Note` to Notes.
  - **Stock**: Applied `MandiKeyboard.Search` to search box; `MandiKeyboard.Whole` to Physical Bags and Farmer Custody; `MandiKeyboard.Decimal` to Bag Weight.
  - Tested: `GroupBInputAuditTest` (2 passing).
  - Verified live on `emulator-5554`: `TradeSettingsDialog` rate fields open numeric decimal keyboard (`inputType=0x2002`); Galla "नकद जमा" opens numeric decimal keyboard (`inputType=0x2002`). Screenshots: `scratch/test_evidence/settings_trade_rates_keyboard.png` and `scratch/test_evidence/cash_in_decimal_keyboard.png`.
- **Regression Testing**:
  - Multiplatform JVM test suites pass: `./gradlew jvmTest` (BUILD SUCCESSFUL).
  - Backend pytest passes: 130 passed in 18.35s.
- **Trackers**:
  - `print-all-bills` marked `passing`.
  - `input-audit-medium-low` evidence updated with Group B completions.

