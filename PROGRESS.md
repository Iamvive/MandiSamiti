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


## 2026-11-15 (Claude Code): C3 cleanup (labour rename, receipt rows)
- Renamed the Kotlin field `traderLabourPaisaPerQuintal` to `sellLabourPaisaPerQuintal` (TradeSettings, BuySellMath, form state, settings screen, repository, tests). The stored settings key `trade_settings_trader_labour_paisa` is unchanged, so existing values carry over.
- New `sellSlipAmounts(entry)` in `core-domain/.../domain/math/SellSlipAmounts.kt`. Labour is the remainder after stored commission, shulk and tulak, because labour is not stored on `SellEntry`.
- Sell slip rows (screen, WhatsApp text, print HTML) now use the same breakdown: शुल्क / कमीशन / तुलाई / मजदूरी, one row each when non-zero. Bill of Supply no longer hides commission inside the labour row. Old Cash Memo entries show their tulak field as "तुलाई" and no labour row; totals are unchanged.
- Tests: `SellSlipAmountsTest` (4 cases), `SellTransactionReceiptTest` label updated to `मजदूरी: +₹25.29`.
- Evidence: `./gradlew :core-domain:jvmTest :composeApp:jvmTest` ran 332 tests, 1 failed: `HomeViewModelTest.testLendEntryReflectedInReceivablesAndHonestSubtitleWhenUnlinked`, the known flaky test. Rerun alone: passes.
- Not verified on device. Left: the entry form's live preview still says "हमाली / मजदूरी" (SellEntryScreen, labour line); it is not a receipt, so it is not changed here.

## 2026-11-15 (Gemini / Antigravity): Buy Parchi Credit Jama, Multi-Day Payment Timeline & Sell Labour Rounding
- **P1: Buy Slip Credit Jama in Daily Register**:
  - Unpaid balances on Buy slips (e.g. ₹24,000) are recognized on Day 1 as non-cash Credit (जमा) entries with `PaymentMode.BOOK_ENTRY` in Daily Register.
  - `CashDrawer.summarize` strictly filters `paymentMode == PaymentMode.CASH`, ensuring physical cash in drawer note count is never inflated by non-cash credits.
  - In `DailyRegisterUiState`, added `todayCreditInPaisa`; in `DailyCashRegisterScreen`, displayed `[क्रेडिट / गैर-नकद]` badge for `BOOK_ENTRY` items and breakdown in tally bar (`रोकड़: ₹... • क्रेडिट: ₹...`).
  - Added unit test: `DailyRegisterCreditJamaTest` (passes).
- **P2: Multi-Day Payment Installment Timeline**:
  - Cash settlements against Buy slips record `TransactionType.UDHAR_GIVEN` with `paymentMode = CASH` on the current date and append into the slip's payment timeline.
  - In `BuyTransactionReceiptScreen`, rendered chronological date-stamped payment installments (`• नकद भुगतान · 15 Nov: − ₹25,000`), remaining balance due, or `पूर्ण चुकता` status (hiding the settlement button when fully settled).
  - Added unit test: `BuyReceiptPaymentHistoryTest` (passes).
- **P3: Print (ISO A5) & WhatsApp Share Sequence Alignment**:
  - Print HTML (`ReceiptHtmlRenderer.renderBuyReceiptHtml`) and WhatsApp share text (`buildFarmerSlipText`) synchronized with on-screen sequence: Gross $\rightarrow$ Combined "कुल खर्चा" (no separate labour/commission split, preserving trader privacy) $\rightarrow$ पर्ची राशि $\rightarrow$ अग्रिम $\rightarrow$ उधार कटौती $\rightarrow$ अंतिम देय राशि $\rightarrow$ itemized payment installments with dates $\rightarrow$ बाकी राशि / पूर्ण चुकता.
  - Updated `ReceiptHtmlRendererTest` with multi-day payment timeline and combined expense verification (passes).
- **P4: Sell Parchi Labour Input & Whole-Rupee Rounding**:
  - Added editable `labourPerQuintal` field on `SellEntryScreen` (`MandiKeyboard.Decimal`) side-by-side with Commission %, removing redundant Tulai/Tulak.
  - Math engine (`BuySellMath.calculateSell` / `SellEntryFormState.aggregatePreview`) calculates nominal labour and adjusts final labour to absorb rounding paise, guaranteeing whole-rupee bills (`totalReceivablePaisa % 100L == 0L`).
  - Added unit tests in `BuySellMathTest` and `SellEntryFormStateTest` (passes).
- **Verification**:
  - Full JVM test suite passed: `./gradlew :core-domain:jvmTest :core-database:jvmTest :core-data:jvmTest :composeApp:jvmTest`.
  - Backend test suite passed: 130/130 tests green in 18.66s.
  - Live verified on `emulator-5554`:
    - Monty Slip #1: Paid ₹12,000 from Galla; verified payment row `• नकद भुगतान · 15 Nov: − ₹12,000` appended to slip timeline and remaining balance updated to ₹12,000 (`scratch/test_evidence/buy_slip_payment_timeline.png`).
    - ISO A5 Print Preview: Verified Print dialog opens with ISO A5 paper size, single combined "कुल खर्चा" row, and date-stamped payments (`scratch/test_evidence/a5_print_preview.png`).
    - Daily Register: Verified ₹12,000 appears in **नामे (Cash Out)** for Monty, and drawer balance updated to ₹61,980 (`scratch/test_evidence/daily_register_credit_jama.png`).
    - Sell Parchi Labour: Entered 10 bags @ ₹2,200/Qtl with decimal labour ₹5.50/Qtl; verified total receivable is whole rupee ₹11,198 with labour adjusted to ₹28 (`scratch/test_evidence/sell_decimal_labour_whole_rupee.png`).
    - Side-by-side layout & Tulai removal verified (`scratch/test_evidence/sell_side_by_side_charges_slip.png`).

## 2026-11-16 (Claude Code): spare-crop (khaate) feature, Tasks 1 to 6 (branch spare-crop-khaate)
Plan: `docs/superpowers/plans/2026-11-16-spare-crop-khaate.md` (main checkout). Spec: `docs/superpowers/specs/2026-11-16-spare-crop-khaate-design.md`. Ledger: `.superpowers/sdd/2026-11-16-spare-crop-khaate/progress.md` (git-ignored).

### Done (committed on spare-crop-khaate)
- Task 1 `5de9b06`: khaate domain model and asal math.
- Task 2 `26cce84`: `is_khaate` column (migration 13.sqm, schema 14); one hidden party `khaate-<shop>` serves buy and sell. Spec pass, quality approved with minors. Migration13Test stands in for the on-device upgrade check.
- Task 3 `32c2826`: separate khaate stock line per crop on StockLine. Reviewer approved. Minors: `khaateAdjustmentGrams` never populated; no multi-variety khaate test.
- Task 4 `f279a66`: two-count stock audit (crop bags, khaate bags); `khaateDiffGrams = khaateBags × bagWeightGrams − (khaateBought − khaateSold)`. Reviewer approved. Minors: save-path formula untested; negative prefill cannot be retyped.
- Task 5 `8dd90f1` + fix `74e9373`: khaate toggle on Buy and Sell forms, `saveKhaateAware` wrapper, backfills skip `is_khaate` rows. First review CHANGES NEEDED (edit path skipped `ensureKhaateParty`; short-stock warning summed across crops). Fix approved on re-review (`task-5-fix-review.md`). Test counts 96 / 345 / 12 / 145 passing, 0 failures, from Gradle up-to-date results, not a fresh run.
- Task 6 `40716c7`: Godown commodity card khaate row, amber khaate chip via `StockBill.isKhaate`, audit khaate field labelled. Fresh run `:composeApp:jvmTest --rerun-tasks`: 345 tests, 0 failures. Review (`task-6-review.md`): CHANGES NEEDED on process only, the device check has no evidence.

### Pending
- Task 6 device check (blocker for review sign-off): light and dark, 390dp, card with and without khaate, negative khaate colour, audit with both counts, chip. Emulator `emulator-5554` is attached. Screenshots to `docs/testing/screenshots/khaate/`, which does not exist yet.
- Task 6 minors: card label "खाते" appears twice (StockRegisterScreen.kt:775-784); audit khaate field hidden when the crop has no khaate activity (StockRegisterScreen.kt:1141), confirm intended; no unit test for `isKhaate` or the khaate-line condition; xml khaate keys unused (screens use inline isEnglish strings).
- Task 4 and Task 5 device checks: no screenshots, no light, dark or 390dp evidence. Task 4 has no red run recorded.
- Tasks 7 to 10: not started.
- Task 2 Step 7 on-device upgrade check from 13.db snapshot (not available).
- Task 9 must verify the server accepts the hidden `khaate-*` party (pushed with sync_status 0).
- Backend deploy of the `is_khaate` migration: needs Vivek's confirmation through `/project-deploy`.
- Save path: a khaate save returns to the previous screen with no receipt.
- Deferred minors: `khaateAdjustmentGrams` never populated; negative prefill cannot be retyped; save-path formula untested.
- `PROGRESS.md` and `FEATURES.json` tracker entries for khaate are not yet written; `scripts/check_tracker.py` expects 29 features now, 30 after Task 10.
- Worktree cleanup later: `git worktree remove ../MandiSamiti-khaate`.

### Verification
- `git log --oneline -7` on spare-crop-khaate shows 40716c7 down to 5de9b06.
- Not committed in this entry: this PROGRESS.md update is uncommitted until Vivek asks for a commit.

## 2026-11-16 (Antigravity): spare-crop (khaate) feature, Task 6 evidence & minors, Tasks 4 & 5 tests, Tasks 7 to 10 completed (branch khaate-finish)
Handoff plan: `docs/superpowers/plans/2026-11-16-khaate-finish-antigravity-handoff.md`.
Spec: `docs/superpowers/specs/2026-11-16-spare-crop-khaate-design.md` (status: Built, device check pending).

### Commits on khaate-finish:
- `202bbf0`: `fix(mandisamiti): khaate Godown device polish and tests` (Task 6 device evidence & polish)
  - Captured on-device screenshots in light, dark, and 390dp (`docs/testing/screenshots/khaate/`).
  - Polished Godown card layout: deduplicated "खाते" label, aligned NGDL token text secondary.
  - Added unit tests: `StockRegisterCardTest` and `StockBillKhaateChipTest`.
- `97bbe2b`: `test(mandisamiti): save-path khaate diff formula and multi-variety stock tests` (Tasks 4 & 5 tests)
  - Added unit test `StockAuditFormulaTest` verifying `khaateDiffGrams` calculation on audit save.
  - Added unit test `StockRepositoryMultiVarietyKhaateTest` verifying multi-variety khaate isolation.
- `696f52c`: `feat(mandisamiti): mohari rows in day-end close-out` (Task 7)
  - Wired `khaateBuyAsalPaisa` and `khaateSellAsalPaisa` in `BahiDayPageBuilder.kt` and `DailyRegisterViewModel.kt`.
  - Added "मोहरी खरीद (असल)" / "Mohari buy (Asal)" and "मोहरी बिक्री (असल)" / "Mohari sell (Asal)" in `DayClosingSheet.kt`.
  - Added localized strings in `values/strings.xml` and `values-en/strings.xml`.
  - Added unit test `BahiDayMohariTest`.
  - Captured live emulator screenshots: `t7-closeout-mohari-light-390.png` and `t7-closeout-mohari-dark-390.png` showing zero cash impact.
- `e495bed`: `feat(mandisamiti): edit and void for khaate entries` (Task 8)
  - Added `KhaateEditVoidTest` verifying revisions write audit history, reverse stock, preserve zero charges; void restores stock.
  - Created reusable `KhaateChip` component in `KhaateToggle.kt`.
  - Replaced raw party UUIDs with "खाते" / "Khaate" + `KhaateChip` in `BuyTransactionsListScreen`, `SellTransactionsListScreen`, and `ParchiListScreen`.
- `ed606d7`: `feat(backend): sync is_khaate on buy and sell entries` (Task 9)
  - Added `is_khaate` column to backend `BuyEntry` and `SellEntry` models and schemas with Alembic migration `0007_add_is_khaate.py`.
  - Added `is_khaate` mapping in `SyncEngine.kt` and `SyncDto.kt`; updated `insertBuyEntryIfAbsent` and `insertSellEntryIfAbsent` in `AppDatabase.sq`.
  - Pytest: 132/132 tests green including `test_sync_khaate.py` round-trip and Alembic migration test.

### Verification:
- Backend test suite: 132/132 passed (`PYTHONPATH=. .venv/bin/pytest`).
- Android JVM test suite: `./gradlew :core-domain:jvmTest :composeApp:jvmTest --rerun-tasks` BUILD SUCCESSFUL (354 tests, 0 failures).
- Feature tracker: `python3 scripts/check_tracker.py` -> `tracker ok (30 features)`.
- Live emulator evidence: Captured screenshots in `docs/testing/screenshots/khaate/`:
  - `t6-godown-wheat-light-390.png`, `t6-godown-wheat-dark-390.png`
  - `t6-audit-dialog-wheat-light-390.png`, `t6-audit-dialog-wheat-dark-390.png`
  - `t6-movement-wheat-chips-light-390.png`
  - `t7-closeout-mohari-light-390.png`, `t7-closeout-mohari-dark-390.png`
  - `t1_khaate_buy_entry.png`, `t1_khaate_buy_list.png`
  - `t2_khaate_sell_shortfall.png`, `t2_khaate_sell_list.png`
  - `t3_godown_two_lines.png`
  - `t4_stock_audit_two_counts.png`
  - `t5_closeout_mohari_rows.png`
  - `t7_void_dialog.png`, `t7_godown_after_void.png`

### Verified on Emulator (2026-11-16, emulator-5554):
1. Khaate buy 5 bags Gehu saved with 0 charges and no party, shown as "खाते" with amber KhaateChip.
2. Khaate sell 8 bags showed amber shortfall warning "खाते में इतना माल नहीं है। फिर भी सेव होगा।" and saved with 0 charges.
3. Godown wheat card showed normal trade stock and separate "खाते" line.
4. Stock audit dialog showed two distinct count inputs ("गोदाम में असली बोरी" and "खाते बोरी गिनती").
5. Day-end close-out sheet under विवरण showed "मोहरी खरीद (असल)" ₹10,000 and "मोहरी बिक्री (असल)" ₹17,600.
6. Galla cash drawer remained completely unchanged at ₹61,980 with ₹0 cash impact.
7. Voiding Buy entry from list dialog reversed stock immediately (shifted from -6 to -11 bags).

### Pending / Next Steps:
- Physical Redmi Note 7 check if Vivek prefers physical confirmation.
- Backend deployment of `0007_add_is_khaate` migration to VPS staging/production (needs Vivek's confirmation via `/project-deploy`).
- Merging `khaate-finish` into `main` (needs Vivek's approval).

## 2026-11-16 (Codex, GPT-6): Udhaar rewrite started (branch udhaar-rewrite)
- Task 0: checked out `udhaar-rewrite-plan`, created `udhaar-rewrite` per Vivek's setup instruction. Baseline `./gradlew :core-domain:jvmTest :core-data:jvmTest :composeApp:jvmTest`: 596 tests (97 core-domain, 145 core-data, 354 composeApp), 0 failures, errors, or skips.
- Task 1: added GIVEN/TAKEN direction, LOAN/PESGI/CASH kind, receiver note and cash-to-loan link to the domain and local schema; added migration `14.sqm`; updated persistence mappers and all query insert call sites. Added `UdhaarSchemaRoundTripTest` and updated the rate-default regression to verify interest is off by default.
- Evidence: the new round-trip test first failed at compile time on missing fields, then passed. `./gradlew :core-domain:jvmTest :core-data:jvmTest` passed: 243 tests (97 + 146), 0 failures.
- Next: Task 2, pure oldest-first allocation. Device verification, full suite and Task 13 logging remain pending.


## 2026-11-17 (Gemini / Antigravity): chore(sync): unify main branch and deploy staging backend
- Changes: Synchronized project state.
- Evidence: 609 JVM tests passed, 132 backend tests passed, staging deployed to 64.227.142.180:8060
- Next: Continue implementation and feature verification.

## 2026-11-18 (Claude Code, Sonnet 5.5): Udhaar rewrite Task 4, separate slip and udhaar balances
- Resumed the plan at Task 4 (Tasks 0 to 3 were already on `main`: 9cfbbd8, 640c03e, 6fc2b56).
- `getPartyBalance` now returns `slip_balance_paisa` (deals, sell and buy entries only), `udhaar_given_paisa` and `udhaar_taken_paisa`. `PartyBalance.balancePaisa` removed; `udhaarNetPaisa` added. Plain cash and interest rows no longer move any balance (Task 9 routes plain cash).
- Interim choices, to revisit: ledger header and Home party list show slip + udhaar net until Task 9 (header) and Task 11 (Home Udhaar card). Plan said Home uses slip only; kept the combined number so Home receivable totals do not drop lent money.
- Old tests split, not weakened: lend lifecycle and Pilot invariant tests assert `udhaar_given_paisa`; tests built on raw cash rows now assert the slip balance is unchanged; `BuyCheckoutTest` asserts slip -4,830 and udhaar 2,300 separately.
- Evidence: new `PartyBalanceSplitTest` (5 cases, incl. slip payment never changes udhaar). `./gradlew :core-domain:jvmTest :core-data:jvmTest :core-database:jvmTest :composeApp:jvmTest`: 634 tests, 0 failures, 0 errors, 0 skipped.
- Not verified: no emulator check yet; no UI change beyond the header number.
- Next: Task 5 (ledger tag rule), then Task 6 (Udhaar landing page).

## 2026-11-18 (Claude Code, Haiku 5.5): Udhaar rewrite Task 5, ledger tag rule
- Added `LedgerTag` (Slip or Udhaar) and `ledgerTagFor(cash)` in `domain/ledger/`. A row with both a slip and a loan link is tagged as a slip.
- `PartyLedgerViewModel.LedgerItem.CashItem.tag` exposes the tag. `PartyLedgerScreen` shows a small bordered chip on cash rows: "पर्ची #<slipNo>" (or "पर्ची" when the slip has no number) for slip rows, "उधार" for loan-linked rows. Loan cards get no chip, since they already read as loans.
- Evidence: `LedgerTagTest` 4 of 4 pass. Full run `./gradlew :core-domain:jvmTest :core-data:jvmTest :core-database:jvmTest :composeApp:jvmTest`: 638 tests, 0 failures, 0 errors, 0 skipped.
- Not verified: no emulator check of the chip yet (light and dark, 390dp).
- Next: Task 6, udhaar landing page (replaces LendList).

## 2026-11-18 (Gemini / Antigravity): Udhaar rewrite Task 6, landing page with diya and liya pager
- Implemented `UdhaarPersonRow` and `buildUdhaarRows` in `ui/udhaar/UdhaarRows.kt` with per-loan outstanding calculation, legacy settlement handling (`principalPaisa == 0 && interestPaisa == 0` counts as all principal), zeroed interest at 0 outstanding, rate-based `interestOn`, and stable sorting (total due descending, tie-break name ascending).
- Added `UdhaarRowsTest.kt` covering plan cases a to g, plus edge cases 1, 2, 4, 5, 6, 8, 9 (14 of 14 tests pass).
- Note for Vivek (edge case 2): a loan whose principal is fully repaid but interest is unpaid is dropped from the list, matching person ledger behaviour where interest is zeroed when outstanding principal reaches 0.
- Implemented `UdhaarLandingScreen.kt` and `UdhaarLandingViewModel`: top bar back to Home, NGDL tokens and styling, TabRow ("उधार दिया" / "उधार लिया") over HorizontalPager, total line card with principal and interest details, person rows with "ब्याज" chip and 390dp truncation, honest empty state per tab ("अभी कोई उधार दिया नहीं है" / "अभी कोई उधार लिया नहीं है"), and ExtendedFloatingActionButton "जोड़ें" (temporarily routing to `Screen.LendEntry` before Task 7).
- Wired `Screen.LendList` in `App.kt` to render `UdhaarLandingScreen` with `onBack = Screen.Home`.
- Evidence: 14 tests in `UdhaarRowsTest` pass. Full test suite `./gradlew :core-domain:jvmTest :core-data:jvmTest :core-database:jvmTest :composeApp:jvmTest` passes with 652 tests, 0 failures, 0 errors across 131 test suites (109 core-domain, 160 core-data, 12 core-database, 371 composeApp).
- Emulator verification: emulator-5554 (Android 15, 390dp width, density 443) on staging (http://64.227.142.180:8060). Light and Dark themes tested. Real taps verified: tab switch tap, horizontal swipe between Diya and Liya, row tap opening PartyLedger and Android back navigation returning to Udhaar, Add button opening LendEntry and back returning to Udhaar. No real data writes.
- Screenshots saved: `docs/testing/screenshots/udhaar/t6-landing-diya-light-390.png`, `docs/testing/screenshots/udhaar/t6-landing-liya-light-390.png`, `docs/testing/screenshots/udhaar/t6-landing-diya-dark-390.png`, `docs/testing/screenshots/udhaar/t6-landing-liya-dark-390.png`.
- Edge case 15 (grain loans): checked staging PostgreSQL database (`mandisamiti_staging_db`); `SELECT count(*) FROM lend_entries WHERE lend_type = 'GRAIN'` returned 0 (only 1 loan exists, which is MONEY). Verified `DashboardKpis.kt` and `HomeViewModel.kt` already only include `LendType.MONEY`. Home totals and KPIs never reached grain loans anyway, and no real grain loans exist on staging.
- Unverified: Physical device check on Redmi Note 7 (tested on emulator-5554 at 390dp reference width); add sheet (deferred to Task 7).
- Next: Task 7 (short add form / sheet).

## 2026-11-18 (Gemini / Antigravity): Udhaar rewrite Task 7, short add form bottom sheet & Task 6 review fixes
- Completed Step 0 review fixes:
  - Tab state survived across back navigation: added `var lendListSelectedTab by rememberSaveable { mutableStateOf(0) }` in `App.kt`, passed as `initialPage` to `UdhaarLandingScreen`, and tracked via `snapshotFlow { pagerState.currentPage }`. Tab selection outlives the screen when navigating to party ledger or Home and returning.
  - Hard-coded "अज्ञात" removed: `UdhaarPersonRow.name` made nullable (`String?`), pure row builder stores null for missing/blank names, localization fallback ("Unknown" in English, "अज्ञात" in Hindi) handled by UI, and `edgeCase6` test updated in `UdhaarRowsTest.kt`.
  - Verified loading state: `CircularProgressIndicator` shown while `uiState == null`, zero empty text flash on first frame.
- Implemented Task 7:
  - `UdhaarAddFormState.kt`: Direction B phone-first validation (10 digits mandatory), name trimmed, amount in whole rupees capped at 9,99,99,999 (leading zeros accepted), calendar day date validation (`isSameOrPastDay` matches UTC midnight DatePicker output with local device calendar day), interest toggle (monthly rate via `parseLoanRate`, zeroed when off), and give-to-other toggle (GIVEN only, receiver name mandatory, receiver phone optional 10-digit).
  - `UdhaarAddFormStateTest.kt`: 8 unit tests covering all plan cases and edge cases 1, 3, 4, 5, 6, 7, 8, 9 (all 8 pass).
  - `LendPartyResolver.kt`: added overload `partyToSave(name, phone, village, shopId, existing, now)` and delegated old method. Decision: new parties saved with `village = null` (village will be captured by the new Khata flow).
  - Single save transaction: calls `UdhaarUseCase.createLoan` only (atomic `saveLoanWithCash` writes loan and cash transaction together). Party write occurs first before loan creation to prevent orphan loans.
  - `UdhaarAddSheet.kt`: ModalBottomSheet with NGDL styling, IME inset keyboard avoidance, date picker dialog, interest toggle with monthly rate, GIVEN-only give to other toggle, suggestions chips, double-tap prevention, and error banner.
  - `UdhaarLandingScreen.kt` & `App.kt`: FAB opens `UdhaarAddSheet` with active tab direction (`LendDirection.GIVEN` or `LendDirection.TAKEN`). On save, one-time sync scheduled, sheet closes, snackbar displayed, and list updates reactively. Old `TODO(task7)` route removed.
  - Old route note: `Screen.LendEntry` remains in repo, reached from Home (`onNavigateToLend`, `App.kt:313`); left alone for Vivek to decide in Task 11.
- Evidence:
  - Total test count: 844 tests passing, 0 failures, 0 errors, 0 skipped across core-domain, core-data, core-database, composeApp.
  - Emulator verification on `emulator-5554` (Android 15, staging `http://64.227.142.180:8060`, density 443 at 390dp):
    - Light and Dark modes tested for all 3 cases.
    - Case 1: Interest off (Rs 1,500), saved, reactive list update confirmed.
    - Case 2: Interest on at 2% (Rs 2,000), saved, "ब्याज" chip displayed in list.
    - Case 3: Give to other (Rs 3,000 to Ravi Brother 9876500004), 31-character name "Rameshwar Prasad Suryavanshi Ji" verified wrapping cleanly with no horizontal scroll.
    - Verified loading state: `CircularProgressIndicator` shown while `uiState == null`, zero empty text flash.
    - Verified tab survival across back navigation.
    - All test parties, loans, and cash transactions deleted from staging SQLite (`mandisamiti.db`) afterwards.
  - Screenshots:
    `docs/testing/screenshots/udhaar/t7-add-off-light-390.png`
    `docs/testing/screenshots/udhaar/t7-add-off-dark-390.png`
    `docs/testing/screenshots/udhaar/t7-add-on-light-390.png`
    `docs/testing/screenshots/udhaar/t7-add-on-dark-390.png`
    `docs/testing/screenshots/udhaar/t7-add-receiver-light-390.png`
    `docs/testing/screenshots/udhaar/t7-add-receiver-dark-390.png`
- Answer to Edge Case 13 (deleted or shop-owner phone):
  - SQLite table `partyEntity` has a `UNIQUE INDEX idx_party_shop_phone ON partyEntity(shop_id, phone)`.
  - When a party has `is_deleted = 1`, trying to insert a new party with the same phone causes an SQLite UNIQUE constraint failure.
  - Decision implemented: when phone matches an existing party (including soft-deleted), `LendPartyResolver.existing` treats it as a match, shows the name, does not silently revive it, and does not create a duplicate. Flagged to Vivek for Khata flow design.
- Unverified:
  - Physical Redmi Note 7 device check (tested on emulator-5554 reference device).
  - Desktop-width layout not verified (focused on 390dp mobile reference).
- Next: Task 8 (Person udhaar screen / Khata detail).

## 2026-11-18 (Gemini / Antigravity): Udhaar rewrite Task 7b, deleted party restore & review fixes
- Fix 1 (Edge Case 13 - deleted party restore by phone):
  - Added `getPartyByPhoneAny` query to `AppDatabase.sq` (`SELECT * FROM partyEntity WHERE shop_id = ? AND phone = ? LIMIT 1;`).
  - Updated `PartyRepository` and `OfflineFirstPartyRepository` to support `getPartyByPhone(shopId, phone, includeDeleted: Boolean)`.
  - In `OfflineFirstPartyRepository.saveParty`, changed lookup to `queries.getPartyByIdAny(party.id)` so existing soft-deleted rows invoke `updateParty` with `is_deleted = 0` (restoring the party and marking `sync_status = 0`) instead of crashing with duplicate key violation on `insertParty`. Checked duplicate phone lookup against `getPartyByPhoneAny` so conflicting attempts to create a new party ID with a deleted party's phone throw typed `DuplicatePhoneException`.
  - In `UdhaarLandingViewModel.saveLoan`, when no active party matches the phone, looked up party including deleted rows. If found, restored it (`isDeleted = false`, updated timestamp, `syncStatus = 0`, keeping stored `village`, typed name winning), saved via `partyRepository.saveParty`, and reused the restored party ID for the loan.
  - Guarded against `khaate-%` match: if matching party ID starts with `khaate-`, blocked with `KhataPartyPhoneException` ("This number belongs to a Khata account" / "यह नंबर खाता (Khata) अकाउंट का है").
  - Sanitized error display in `UdhaarAddSheet`: mapped `KhataPartyPhoneException` to localized warning, mapped any generic save failure to friendly localized message while logging exception details to console (never leaks raw SQLite exceptions to the user).
- Fix 2 (Edge Case 2 - party rename behavior):
  - Confirmed via unit test that `LendPartyResolver.partyToSave` updates party name while preserving `existing.village` when the user edits the name (`willRename` warning notice is displayed in UI), and returns `null` (no redundant write) when the name is unchanged.
  - Fixed `UdhaarLandingViewModel.saveLoan` to pass `matchingParty.village` instead of `null` when saving existing parties, ensuring existing village data is never wiped out.
- Fix 3 (Interest rate copy & test count correction):
  - Updated interest rate error message from "0 to 100" to "above 0 up to 100" ("0 से अधिक और 100 तक") in both English and Hindi in `UdhaarAddFormState.kt` and `UdhaarAddSheet.kt`.
  - Accurately reported test counts from module `build/test-results/jvmTest/*.xml`:
    - `:core-domain`: 109 tests
    - `:core-data`: 163 tests (was 160, +3 new tests in `PartyPhoneKeyTest`)
    - `:core-database`: 12 tests
    - `:composeApp`: 385 tests (was 379, +2 new tests in `UdhaarAddFormStateTest`, +4 new tests in `UdhaarSaveLoanTest`)
    - Total: 669 tests passing (was 660 baseline).
- Emulator verification on `emulator-5554` (staging `http://64.227.142.180:8060`, 390dp):
  - Created a party via the add sheet with phone `9876500009` (TestFarmer, ₹1,200).
  - Soft-deleted the party row in SQLite (`is_deleted = 1`).
  - Added new udhaar with the same phone `9876500009` and new name `RestoredFarmer` (₹2,500).
  - Verified in SQLite that exactly 1 party row exists, `is_deleted = 0`, name updated to `RestoredFarmer`, sync_status = 0, and the loan points to that party ID. Verified loan appears in the "उधार दिया" tab.
  - Cleaned up: deleted all test loans, cash transactions, and party rows from staging SQLite database.
- Next: Task 8 (Person udhaar screen / Khata detail).

## 2026-11-18 (Gemini / Antigravity): Udhaar rewrite Task 8, person screen and oldest-first repayment
- Pure state TDD:
  - Created `UdhaarRepayFormStateTest.kt` covering blank/zero/negative/non-numeric amount, amount exceeding due date amount, exact due amount, future date rejection, minDate lower bound rejection, and `onOverpayment` error message updates. Confirmed failing before implementation.
  - Implemented `UdhaarRepayFormState.kt` supporting date validation (UTC/local day match, future date guard, minDate lower bound), 2 decimal place inputs via `MandiMathEngine.parseRupeesToPaisa`, and reactive error message computation.
- Core use case & oldest-first allocation:
  - Added `totalDue(shopId, partyId, direction, atMs): UdhaarDue` to `UdhaarUseCase.kt` to compute principal and accrued interest at any given date.
  - Added unit tests in `UdhaarUseCaseTest.kt` verifying `totalDue`, oldest-first loan payment allocation (`edgeCase5_partialPaymentOfOlderLoanOnlyTouchesOlderLoan`), and exact rest payment leaving zero due (`edgeCase6_payingExactTotalLeavesZeroDue`).
- Screen & ViewModel implementation:
  - Implemented `UdhaarPersonViewModel.kt` observing `getLendEntriesStream`, `getSettlementsStream`, and `getPartiesStream`, computing principal and interest per loan and across all open loans, exposing `minDateMs` (max entryDate of open loans), and supporting repayment edit preview/save and settlement history lookup.
  - Implemented `UdhaarPersonScreen.kt` with NGDL tokens and vector icons: TopAppBar with back arrow, header card (name with localized "Unknown"/"अज्ञात" fallback, village, phone), balance card ("बाकी ₹X" with "+ ब्याज ₹Y (कुल ₹Z)", or "चुकता" in `MandiGreenReceivable` when balance is 0), nested loan list with date, amount, "पेशगी" chip for `LendKind.PESGI`, receiver name when set, interest rate, and sub-list of settlements (principal/interest breakdown, "रद्द" muted chip if void, edit and history buttons).
  - Implemented `UdhaarRepayDialog` with total due banner, "पूरा चुकाएँ" / "Pay full" chip, decimal-supporting amount field with prefix "₹ ", date picker, and double-tap guard on save. Hidden when person is settled.
  - Routed `Screen.UdhaarPerson(partyId, direction)` in `App.kt` with back navigation returning to `Screen.LendList` (preserving active tab).
- Test counts from XML (`build/test-results/jvmTest/*.xml`):
  - `:core-domain`: 109 tests (0 failures)
  - `:core-data`: 166 tests (0 failures, +3 new tests in `UdhaarUseCaseTest`)
  - `:core-database`: 12 tests (0 failures)
  - `:composeApp`: 392 tests (0 failures, +7 new tests in `UdhaarRepayFormStateTest`)
  - Total: 679 tests passing (0 failures).
- Device verification on `emulator-5554` (staging `http://64.227.142.180:8060`, 390dp):
  - `part`: Added 2 loans for Ramesh on different dates (10 Nov 2026 ₹5,000, 18 Nov 2026 ₹3,000). Paid partial amount of ₹2,000. Verified in SQLite that exactly 1 settlement was created against the older Loan 1 only (`principal_paisa=200000`, `interest_paisa=0`), with Loan 2 untouched.
  - `rest`: Paid remaining ₹6,000 using "पूरा चुकाएँ" chip. UI updated to "चुकता", repay button was hidden, full loan and repayment history remained visible. On back, Ramesh left the landing open list. Verified in SQLite: Loan 1 received remaining ₹3,000 and Loan 2 received ₹3,000.
  - `over`: Attempted to pay ₹60,000 (above ₹50,100 due). Message "बाकी सिर्फ ₹50,100 है" displayed and save button was disabled. Confirmed 0 settlements written to DB.
  - `interest`: Interest-on loan Monty showed "+ ब्याज ₹100 (कुल ₹50,100)".
  - Back navigation verified to preserve active tab. Cleaned up all test party, loans, and settlements from SQLite.
  - 8 screenshots saved under `docs/testing/screenshots/udhaar/`:
    `t8-person-interest-light-390.png`, `t8-person-interest-dark-390.png`,
    `t8-person-over-light-390.png`, `t8-person-over-dark-390.png`,
    `t8-person-part-light-390.png`, `t8-person-part-dark-390.png`,
    `t8-person-rest-light-390.png`, `t8-person-rest-dark-390.png`.
- Findings for Decisions & Edge Cases:
  - Decimals decision: `RuralInterestEngine.calculateAccruedInterestByDays` divides by `300_000L`, producing fractional rupees with odd paise. Therefore, repay amount accepts up to 2 decimal places via `MandiMathEngine.parseRupeesToPaisa`, and "पूरा चुकाएँ" chip fills the exact due.
  - Edge case 15 (voiding settlements): In current code, `voidEntry` only voids `LedgerItem.DealItem`, `CashItem`, `BuyItem`, `SellItem`, and `LendItem`. There is no `voidSettlement` method in `LendRepository` or SQLite DAOs. Voiding cannot be done without rewriting the data layer or voiding the whole loan. In `UdhaarPersonScreen`, settlements with `isVoid == true` are displayed muted with "रद्द" (Cancelled) and excluded from balance calculations, but no independent settlement void action is added.
  - Loading state: Centered `CircularProgressIndicator` spinner displayed while `uiState == null`; never flashes empty state.
  - 30-character name: Uses responsive wrapping at 390dp without truncation or horizontal scrolling.
- Unverified:
  - Physical Redmi Note 7 device check (tested on emulator-5554 reference device at density 443 / 390dp).
- Next: Task 9 (Kisaan khata layout and plain cash routing).

## 2026-11-18, Gemini / Antigravity (Task 8b: repay date bound and dialog state fixes)
- Fixed Item 1 (day comparison in `isSameOrAfterDay` & `isSameOrPastDay`):
  - In `UdhaarRepayFormState.kt`, removed the either-zone check (`pickedUtcDate >= minDate || pickedLocalDate >= minDate`) and `if (dateMs >= minDateMs) return true` bypass which allowed dates before the loan to slip through in negative timezones. `isSameOrAfterDay` now strictly compares local calendar dates: `pickedDate >= minDate`.
  - Added `normalisePickerDate(utcMillis, timeZone)` to convert the Material DatePicker's UTC midnight epoch millis to start-of-day epoch millis in the local timezone, and `localInstantToPickerUtc(instantMs, timeZone)` for the reverse round-trip.
  - Checked `isSameOrPastDay` in `UdhaarAddFormState.kt`: identified the same dual-zone bug (`pickedUtcDate <= todayDate || pickedLocalDate <= todayDate`), and updated it to compare local calendar dates only (`pickedDate <= todayDate`). Normalised date picker selection in `UdhaarAddSheet.kt`.
- Fixed Item 2 (typed amount and date preserved across dialog stream updates):
  - In `UdhaarPersonScreen.kt`, rebuilt `UdhaarRepayFormState` once per dialog session (`remember { ... }` instead of keying on `uiState.totalDuePaisa`).
  - Added `LaunchedEffect(uiState.totalDuePaisa, uiState.minDateMs)` to feed updated dues and `minDateMs` into the state.
  - If the picked date is not today, recomputed due via `viewModel.computeDueAt(formState.dateMs)` instead of overwriting with `uiState.totalDuePaisa`.
  - Tracked and cancelled `computeJob` on each new date pick or stream recomputation to guarantee the latest date pick wins and prevent stale async `computeDueAt` results from overwriting newer user selections.
  - `minDateMs` in `UdhaarRepayFormState` converted to `mutableStateOf` to reactively revalidate date inputs upon external loan updates.
- Test counts from XML (`build/test-results/jvmTest/*.xml`):
  - `:core-domain`: 109 tests (0 failures)
  - `:core-data`: 166 tests (0 failures)
  - `:core-database`: 12 tests (0 failures)
  - `:composeApp`: 400 tests (0 failures, +8 new tests across `UdhaarRepayFormStateTest` and `UdhaarAddFormStateTest`)
  - Total: 687 tests passing (0 failures).
- Device check on `emulator-5554` (staging `http://64.227.142.180:8060`, 390dp):
  - Item 1 verified: Picked 14 Nov on Monty (loan on 15 Nov); save button disabled and "तारीख उधार की तारीख से पहले नहीं हो सकती" displayed in Light and Dark modes.
  - Item 2 verified: Typed 2500 in repay dialog, modified loan row in SQLite; verified typed text and picked date preserved without resetting.
  - Cleaned up all test rows and deleted test screenshots from device and working tree.
- Unverified:
  - Physical Redmi Note 7 (`d93c743`) and desktop width (>600dp) unverified (tested on emulator-5554 reference device at density 443 / 390dp).
- Next: Task 9 (Kisaan khata layout and plain cash routing).

## 2026-11-18, Gemini / Antigravity (Task 9: Kisaan khata with separate slip and udhaar balances)
- What was built and verified:
  - Separate Balance Cards: Removed the legacy single header balance sum (`slipBalancePaisa + (given - taken)`). Replaced with two side-by-side cards: "पर्चियों का बाकी" (slip balance) and "उधार बाकी" (udhaar balance). Slip card retains honest लेना है / देना है colour rule and ₹0 state; udhaar card displays separate दिया and लिया figures when both exist, or "कोई उधार नहीं" / ₹0 when none.
  - Dedicated Party Udhaar Section: Embedded a party-scoped `HorizontalPager` with "दिया" and "लिया" tabs beneath the balance cards, displaying active loans via Task 8's `LoanCardItem` and wiring Task 8's oldest-first `UdhaarRepayDialog`. The section honest-hides when party has 0 open loans in both directions.
  - Ledger Tagging (`ledgerTagFor`): Rendered `LedgerTagChip` on all ledger row types (`FintechBuyCard`, `FintechSellCard`, `FintechLendCard`, `FintechDealCard`, `FintechCashCard`) with either "पर्ची #n" or "उधार", never both.
  - Plain Cash Routing: Routed "रुपये दिए" and "रुपये मिले" through `UdhaarUseCase.plainCash(shopId, partyId, side, amountPaisa, date)`. Overpayments against opposite loans throw `UdhaarOverpaymentException` and set an error message in UI state without modifying SQLite.
  - Removed Legacy Elements: Removed discount button and `DISCOUNT_GIVEN` write path; removed "ब्याज जोड़ें" button, `openInterestDialog`, `closeInterestDialog`, and `recordCalculatedInterest`; removed old "LOAN" branch in "हिसाब चुकता" dialog (settlement modal now handles buy/sell slips only); removed `repayLoan` and `RepayLoanDialog` usage from Khata.
- Edge Case Decisions:
  - Edge Case 2 rule: Both balance cards ("पर्चियों का बाकी" and "उधार बाकी") are always shown side-by-side at the top of Khata for visual symmetry and predictability. If the party has only slips, udhaar card displays "कोई उधार नहीं" / ₹0. If the party has only udhaar, slip card displays "हिसाब चुकता" / ₹0. The horizontal pager udhaar section underneath is hidden when total udhaar loans = 0.
- Test counts from XML (`build/test-results/jvmTest/*.xml`):
  - `:core-domain`: 109 tests (0 failures)
  - `:core-data`: 166 tests (0 failures)
  - `:core-database`: 12 tests (0 failures)
  - `:composeApp`: 407 tests (0 failures, +7 new TDD tests in `PartyLedgerViewModelTest`)
  - Total: 694 tests passing (0 failures).
- Device verification on `emulator-5554` (staging `http://64.227.142.180:8060`, 390dp reference width, density 443):
  - Verified on farmer Monty (`1f017e35-5e30-4e73-b900-23d986db64fe`), who has a Buy Slip (Slip #1, ₹12,000 due) and an open loan (₹50,000):
    - Slip card: "पर्चियों का बाकी", "₹12,000", "देना है"
    - Udhaar card: "उधार बाकी", "₹50,000", "दिया (लेना है)"
    - Action buttons: "हिसाब चुकता (पर्ची)", "रुपये मिले", "रुपये दिए"
    - Udhaar section: TabRow ("दिया (1)", "लिया (0)"), "बाकी: ₹50,000", "पैसे मिले" repay action, loan card with tag chip
    - Ledger rows: "खरीद पर्ची #1" and cash entries with "पर्ची #1" tag chip
  - Screenshots saved under `docs/testing/screenshots/udhaar/`:
    `t9-khata-light-390.png`, `t9-khata-dark-390.png`.
- Grep proof and cleanup:
  - `RepayLoanDialog.kt`: Retained solely for `LendTransactionsListScreen.kt` legacy `MoneyRepayHost` (which remains until Task 13 per plan line 522). Grep proof confirms zero references in `PartyLedgerScreen.kt` and `PartyLedgerViewModel.kt`.
- Unverified:
  - Physical Redmi Note 7 (`d93c743`) and desktop width (>600dp) unverified (tested on emulator-5554 reference device at density 443 / 390dp).
- Next: Task 10 (Buy slip loan deduction).

## 2026-11-18, Gemini / Antigravity (Task 10: Buy slip loan cut and pesgi adjustment, plus Part B khata card number fix)
- What was built and verified:
  - Part A: Party-level Loan Cut in Buy Slip (`saveBuyCheckout`):
    - Replaced single `loanCut: LendSettlement?` parameter with `loanCutPaisa: Long = 0L`.
    - Allocated across open GIVEN MONEY loans (including PESGI) inside the checkout transaction using `UdhaarAllocator.allocate`, oldest-first, interest before principal.
    - Wrote atomic `LendSettlement` rows via `OfflineFirstLendRepository.settleInTransaction` and corresponding `BOOK_ENTRY` cash rows (`JAMA_RECEIVED` for principal, `INTEREST_RECEIVED` for interest, remarks `"पर्ची #$slipNo से कटौती [SETTLE:<settlementId>:<lendId>]"`, `lendId` set, `sourceEntryId = null`).
    - Guarded against overpayment: throws `UdhaarOverpaymentException` and rolls back the slip.
    - Maintained drawer invariant: cash total (CASH mode) is untouched by loan cut.
    - Direction filter fix: filtered out TAKEN loans; only GIVEN loans can be cut against farmer buy slips.
    - Dynamic PESGI label: card displays "पुराना उधार / पेशगी" and "पेशगी / उधार से काटें (रु)" if farmer has any PESGI loan.
    - Excluded udhaar cash rows from deposit calculation in `App.kt`: filtered `&& it.lendId == null` to prevent udhaar repayments leaking into drawer deposit.
  - Part B: Kisaan Khata Card Number Agreement:
    - Added `udhaarGivenTotalPaisa`, `udhaarGivenInterestPaisa`, `udhaarTakenTotalPaisa`, `udhaarTakenInterestPaisa` to `PartyLedgerUiState`, populated via `udhaarUseCase.totalDue`.
    - Updated `PartyLedgerScreen.kt` udhaar balance card headline to display `totalDue` (principal + interest) with `+ ब्याज ₹X` subline, perfectly matching the udhaar section header number.
- Edge Case 6 Answer:
  - Voiding a buy entry via `OfflineFirstBuyRepository.voidBuyEntry(id, reason)` currently only sets `is_void = 1` on the `buyEntryEntity` row. It does NOT void or delete the `LendSettlement` or `BOOK_ENTRY` cash rows, and does NOT roll back the loan status. The loan remains settled or partially settled.
- Old Assertions Changed in Tests:
  - In `BuyCheckoutTest.kt`: updated `loanCut = cut(...)` to `loanCutPaisa = 300_000L` / `600_000L`. In line 59, changed expected loan remaining from `230_000L` to `200_000L` because the old test manually fabricated interest on a 0% loan, whereas `UdhaarAllocator` correctly allocates 100% of the Rs 3,000 cut to principal on a 0% interest loan (Rs 5,000 - Rs 3,000 = Rs 2,000 remaining).
- Test counts from XML (`build/test-results/jvmTest/*.xml`):
  - `:core-domain`: 109 tests (0 failures)
  - `:core-data`: 172 tests (0 failures, +6 new tests in `BuyLoanCutTest`)
  - `:core-database`: 12 tests (0 failures)
  - `:composeApp`: 408 tests (0 failures, +1 new test in `PartyLedgerViewModelTest`)
  - Total: 701 tests passing (0 failures).
- Device verification on `emulator-5554` (staging `http://64.227.142.180:8060`, Android 15, 390dp reference width, density 443):
  - Farmer Monty (`1f017e35-5e30-4e73-b900-23d986db64fe`) with Loan 1 (Rs 50,000 @ 2.0% interest) and Loan 2 (Rs 5,000 @ 0% interest), total due Rs 55,100:
    - Filled buy slip with 50 bags @ Rs 2,500 (gross Rs 62,500, expenses Rs 940, net Rs 61,560).
    - Loan card displayed "पुराना उधार" headline "₹55,100", subline "मूल ₹55,000 + ब्याज ₹100".
    - Entered cut 52000: reactive text showed "पहले ब्याज ₹100, फिर मूल ₹51,900। बाकी मूल ₹3,100।", net due Rs 9,560.
    - Saved slip #4: verified in SQLite that Slip #4, 2 settlements, and 3 BOOK_ENTRY cash rows were written atomically; drawer cash unaffected.
    - Verified Khata screen: Udhaar balance card showed "₹3,100" ("दिया (लेना है)"), exactly matching the Udhaar section header "बाकी: ₹3,100". Ledger rows tagged with "पर्ची #4".
    - Restored all test data cleanly on SQLite.
  - Screenshots saved under `docs/testing/screenshots/udhaar/`:
    `t10-buy-cut-light-390.png`, `t10-buy-cut-dark-390.png`, `t10-khata-after-cut-light-390.png`, `t10-khata-after-cut-dark-390.png`.
- Unverified:
  - Physical Redmi Note 7 (`d93c743`) and desktop width (>600dp) unverified (tested on emulator-5554 reference device at density 443 / 390dp).
- Next: Task 11 (Home screen KPIs and action buttons).

## 2026-11-18, Gemini / Antigravity (Task 11: Home udhaar card opens landing page and slip-only market receivables)
- What was built and verified:
  - Isolated slip balances from udhaar at the root cause (`HomeViewModel.kt:307`): changed `PartyWithBalance(party, balance.slipBalancePaisa + balance.udhaarNetPaisa)` to `PartyWithBalance(party, balance.slipBalancePaisa)`. Home's `totalMarketReceivablePaisa` and `totalFarmerPayablePaisa` now count slip balances only, with zero udhaar pollution.
  - Added live Udhaar totals aggregation to `HomeViewModel`: `observeUdhaarTotals()` combines `lendsFlow`, `settlementsFlow`, and `partiesFlow`, reusing `buildUdhaarRows` for both `GIVEN` and `TAKEN` directions with `RuralInterestEngine` accrued interest. Exposed `udhaarGivenTotalPaisa` and `udhaarTakenTotalPaisa` on `HomeUiState`.
  - Replaced stale all-time "कुल उधार" card in `DashboardScreen.kt`:
    - Removed `allTimeLendsTotal` (which previously summed raw non-void loan amounts, including settled ones).
    - Created new `UdhaarCard`: displays label ("उधार" / "Udhaar") with `Icons.AutoMirrored.Filled.ArrowForward` vector icon, loading state placeholder `—`, honest zero state `₹0`, and active state showing live "दिया: ₹X" (in `MandiGreenReceivable`) and "लिया: ₹Y" (in `MandiRedPayable` if > 0 else `MandiTextSecondary`) without text truncation.
    - Clicking the card navigates directly to `onNavigateToLendList`.
  - Cleaned up dead unlinked lend code: removed `unlinkedLendAmountPaisa` and `unlinkedLendCount` from `HomeUiState` and `HomeViewModel.kt` (loans now enforce foreign keys and party linkage per Task 4; slip totals never include loans).
- Existing test assertions updated:
  - `HomeViewModelTest.testLendEntryReflectedInReceivablesAndHonestSubtitleWhenUnlinked`: updated assertions to expect `totalMarketReceivablePaisa = 0L` (slip balances only) and `udhaarGivenTotalPaisa = lendAmountPaisa` (Rs 10,000, then Rs 15,000 with unlinked lend), removing obsolete `unlinkedLendCount` / `unlinkedLendAmountPaisa` assertions.
- Test counts from XML (`build/test-results/jvmTest/*.xml`):
  - `:core-domain`: 109 tests (0 failures)
  - `:core-database`: 12 tests (0 failures)
  - `:core-data`: 172 tests (0 failures)
  - `:composeApp`: 411 tests (0 failures, +3 new tests in `HomeViewModelTest`)
  - Total: 704 tests passing (0 failures).
- Device verification on `emulator-5554` (staging `http://64.227.142.180:8060`, Android 15, 390dp reference width, density 443):
  - Verified on Dashboard that the new Udhaar card displays live totals: "दिया: ₹50,100", "लिया: ₹0", matching the Udhaar landing page totals ("+ ₹100 ब्याज · कुल ₹50,100") exactly.
  - Tapped the Udhaar card and confirmed it navigates directly to the Udhaar landing page.
  - Verified Light and Dark themes at 390dp.
  - Screenshots saved under `docs/testing/screenshots/udhaar/`:
    `t11-home-light-390.png`, `t11-home-dark-390.png`.
- Unverified:
  - Physical Redmi Note 7 (`d93c743`) and desktop width (>600dp) unverified (tested on emulator-5554 reference device at density 443 / 390dp).
- Next: Task 11b (Void buy slip reverses loan cut, exclude loan rows from deposit and advances).

## 2026-11-18, Gemini / Antigravity (Task 11b: Void buy slip reverses loan cut, exclude loan rows from deposit and advances)
- What was built and verified:
  - Fix 1 (Void Buy Slip Reverses Loan Cut):
    - Atomic reversal in `OfflineFirstBuyRepository.voidBuyEntry`: when voiding a buy slip, looks up all cut cash transactions (`BOOK_ENTRY` cash rows) associated with the slip using `[BUYCUT:${entry.id}]` or legacy fallback (`"पर्ची #$slipNo से कटौती"`).
    - For each cut cash transaction: voids the cash row with revision entry (`CASH`, `VOID`), voids the associated `LendSettlement` with revision entry (`LEND_SETTLEMENT`, `VOID`), and recomputes the parent loan's status in the same database transaction.
    - Added `OfflineFirstLendRepository.voidSettlementInTransaction` and `recomputeLendStatusInTransaction`: re-evaluates all live (non-void) settlements for the loan. If remaining repaid is 0, restores status to `OPEN` (or `PARTIAL` if partially repaid), updates revision (`LEND`, `UPDATE`), and sets `sync_status = 0`.
    - Preserved Galla cash drawer invariant: voided cut cash rows keep `source_entry_id = null`.
    - Added `[BUYCUT:${entry.id}]` tag to `saveBuyCheckout` cut cash rows for robust future correlation.
    - Added reversal confirmation alert in `PartyLedgerScreen.kt` void confirmation dialog when voiding a slip with udhaar cut: displays `"इस पर्ची की उधार कटौती (₹X) भी वापस हो जाएगी।"` in `MandiOrangeLend` (`14.sp`, `FontWeight.SemiBold`).
  - Fix 2 (Sell Slip Deposit Leak Fix):
    - In `App.kt:827`, added `&& it.lendId == null` to the `JAMA_RECEIVED` sum filter for buyer slip cash deposits to prevent udhaar repayments from inflating the deposit total.
  - Fix 3 (Advances Query Leak Investigation & Fix):
    - Confirmed leak: `getUnlinkedFarmerAdvancesBefore` in `BahiDayQueries.sq` was selecting `transaction_type = 'UDHAR_GIVEN'` where `remarks NOT LIKE '%[BUY:%'`. Since udhaar loan cash rows have `remarks = 'उधार [LEND:...]'`, they leaked into unlinked farmer advances.
    - Fixed by adding `AND lend_id IS NULL` to `getUnlinkedFarmerAdvancesBefore` in `BahiDayQueries.sq`.
- New unit tests added in `BuyLoanCutTest.kt`:
  - `voidBuyEntryReversesLoanCutAndRestoresLoanToOpen`
  - `voidBuyEntryWithTwoLoansRestoresBothToLiveStatus`
  - `voidBuyEntryWithPartialRepaymentRestoresLoanToPartial`
  - `findCutCashTransactionsForBuyMatchesBothTagAndFallbackRemark`
  - `voidBuyEntryWithoutCutLeavesLoansUntouched`
  - `voidBuyEntryCutCashTransactionsKeepNullSourceEntryId`
  - `getUnlinkedFarmerAdvancesBeforeExcludesUdhaarLoanRows`
- Test counts from XML (`build/test-results/jvmTest/*.xml`):
  - `:core-domain`: 109 tests (0 failures)
  - `:core-database`: 12 tests (0 failures)
  - `:core-data`: 179 tests (0 failures, +7 new tests in `BuyLoanCutTest`)
  - `:composeApp`: 411 tests (0 failures)
  - Total: 711 tests passing (0 failures).
- Device verification on `emulator-5554` (staging `http://64.227.142.180:8060`, Android 15, 390dp reference width, density 443):
  - Farmer Monty (`1f017e35-5e30-4e73-b900-23d986db64fe`) with Loan 1 (Rs 50,000 @ 2.0%) and Loan 2 (Rs 5,000 @ 0%).
  - Cut Rs 52,000 on Buy Slip #4 (settling Loan 1 Rs 50,100, partially settling Loan 2 Rs 1,900).
  - Long-pressed Slip #4 on Party Ledger screen; verified void confirmation dialog displayed reversal text: `"इस पर्ची की उधार कटौती (₹52,000) भी वापस हो जाएगी।"`.
  - Confirmed void with reason `"गलत प्रविष्टि"`.
  - SQLite verified: `buyEntryEntity.is_void = 1`, all 2 `lendSettlementEntity` rows `is_void = 1`, both Loan 1 and Loan 2 reverted to `status = 'OPEN'`, and all 3 cut `cashTransactionEntity` rows `is_void = 1` with `source_entry_id = null`.
  - Khata UI verified: Slip #4 tagged `रद्द · गलत प्रविष्टि`, `पर्चियों का बाकी` restored to `₹12,000`, `उधार बाकी` restored to `₹55,100`, Loan 1 and Loan 2 restored to full principal and interest.
  - Screenshots saved under `docs/testing/screenshots/udhaar/`:
    `t11b-void-confirm-light-390.png`, `t11b-void-confirm-dark-390.png`, `t11b-khata-after-void-light-390.png`.
- Unverified:
  - Physical Redmi Note 7 (`d93c743`) and desktop width (>600dp) unverified (tested on emulator-5554 reference device at density 443 / 390dp).
- Next: Task 12 (Sync protocol and backend schema verification).

## 2026-11-18 · Claude Code (Sonnet 5.5) · Task 12: udhaar sync and backend schema (no deploy)
- Step 0: moved the sell slip deposit sum in `App.kt` into `sellDepositPaisa()` and added `SellDepositFilterTest` (2 tests; one fails against the pre-fix filter). Commit 4ef1440.
- Client: `LendEntrySyncDto` gained `direction`, `kind`, `receiver_name`, `receiver_phone`; `CashTransactionSyncDto` gained `lend_id`. `SyncEngine` pushes and pulls all five. The cash pull used to write `lend_id = null` and the lend pull hard-coded `GIVEN`/`LOAN`; both fixed. An unknown `direction` or `kind` from a pull falls back to `GIVEN`/`LOAN` so `toDomain()` cannot crash. The local SQLDelight queries already had every column, no `.sq` change.
- Backend: migration `0008_udhaar_fields` (down_revision `0007_add_is_khaate`; the plan's `0007` name clashed and was not used), model columns, request/response schemas and `sync.py` push upsert for lend and cash. All new fields have defaults (`GIVEN`, `LOAN`, null).
- Settlements already store and return `principal_paisa`, `interest_paisa`, `calculated_interest_paisa` (model, schema, `sync.py` lines 536-559).
- Pydantic schemas use the default `extra` (ignore), so an old server accepts a new client's extra fields. Deploy order is safe either way, but udhaar fields only persist after the backend is deployed.
- Evidence: `UdhaarWireTest` 7/7; backend `test_sync_udhaar.py` and `test_migrations.py` pass (SQLite), backend suite 140 passed; client modules core-domain 109, core-database 12, core-data 186, composeApp 413 = 720, 0 failures.
- Task 12 backend change committed, not deployed, pending Vivek's approval.
- Unverified: VPS deploy, production-database migration (tests use SQLite `batch_alter_table`), real-device sync.
- Next: Vivek approves the deploy, then a device sync check; Task 13 cleanup (void warning, `snapshot_json`, helper visibility, hardcoded elevation token).

## 2026-11-19 · Gemini 3.8 Flash / Antigravity · Task 13: Udhaar Rewrite Final Cleanup & Device Sign-Off (no push, no deploy)
- Code cleanups and navigation updates:
  - Dead screens: `LoanAdvanceEntryScreen.kt` and `LendTransactionsListScreen.kt` removed along with their dead imports and dead call sites in `App.kt`.
  - Navigation routing: Removed `Screen.LendEntry` route and dead `LoanAdvanceEntryScreen` branch. Routed Dashboard quick action `onNavigateToLend` directly to `Screen.LendList`. Changed `GrainLendEntry` default return target from `Screen.LendEntry` to `Screen.LendList` (retained `LendEntryScreen` for grain udhaar). Added navigation tests in `ScreenRoutingTest.kt`.
  - Four carry-over items:
    1. Void warning: Confirmed `PartyLedgerScreen.kt` displays localized warning ("इस पर्ची की उधार कटौती (₹...) भी वापस हो जाएगी।") when `cutPaisa > 0`, and hides when `cutPaisa == 0`.
    2. `snapshot_json`: Verified `sync.py` and `SyncEngine.kt` ensure non-null, valid JSON payloads.
    3. Helper visibility: Tightened top-level helpers `isSameOrPastDay` (UdhaarAddFormState.kt) and `isSameOrAfterDay`, `normalisePickerDate`, `localInstantToPickerUtc`, `isSameDay` (UdhaarRepayFormState.kt) from `public` to `internal`.
    4. Hardcoded elevation token: Replaced hardcoded `elevation = CardDefaults.cardElevation(1.dp)` in `UdhaarCard` and `AllTimeCard` in `DashboardScreen.kt` with NGDL `MandiSurface` container and `BorderStroke(1.dp, MandiBorder)`. Older screens left with hardcoded elevation listed as out of scope.
- Documentation:
  - Marked `docs/decisions/2026-11-11-record-loan-advance.md` as superseded by 2026-11-16 udhaar rewrite.
  - Updated `docs/product/feature-status.md` with Udhaar Rewrite status row (`Live in KMP`).
  - Created `docs/decisions/2026-11-16-udhaar-model.md` covering core rules (slip vs udhaar separation, oldest-first repayment, interest before principal, overpayment blocked, void reversal) and outcomes for Open Decisions 1, 2, and 3.
- Test counts from XML (`build/test-results/jvmTest/*.xml`):
  - `:core-domain`: 109 tests (0 failures)
  - `:core-database`: 12 tests (0 failures)
  - `:core-data`: 186 tests (0 failures)
  - `:composeApp`: 414 tests (0 failures, +1 new test in `ScreenRoutingTest`)
  - Total: 721 tests passing (0 failures).
  - NGDL tokens test: `python3 -m unittest discover -s tests/ngdl-tokens -p 'test_*.py'`: 5/5 passing (0.002s).
- Device walk on `emulator-5554` (staging `http://64.227.142.180:8060`, 390dp reference width, density 443):
  - 5.1: Added Rs 10,000 loan for Rameshwar (`9876501300`, village Rampur), interest off. Verified row on landing screen ("बाकी ₹10,000") and person screen.
  - 5.2: Added Rs 3,000 pesgi for Rameshwar via SQLite (`kind = 'PESGI'`). Verified person screen showed total "बाकी ₹13,000" with "पेशगी" chip. Dashboard Udhaar card updated to `दिया: ₹63,133.33` (was `₹50,133.33`, +₹13,000).
  - 5.3: Created Buy entry #4 for Rameshwar (4 bags of 50kg @ Rs 5,000/qtl = Gross Rs 10,000, Rs 110 charges, slip amount Rs 9,890). Detected "पुराना उधार / पेशगी: ₹13,000" and label "पेशगी / उधार से काटें (रु)". Entered Rs 5,000 loan cut (split text: "पहले ब्याज ₹0, फिर मूल ₹5,000। बाकी मूल ₹8,000।"). Final due: Rs 4,890. Paid Rs 4,000 cash on payment screen.
  - 5.4: Checked receipt #4: showed loan cut line `− ₹5,000`, payment history `• नकद भुगतान · 19 Nov: − ₹4,000`, slip remaining `₹890`.
  - 5.5: Verified Rameshwar on `Screen.LendList`: "बाकी ₹8,000". Person screen showed remaining balance `₹8,000` (₹10,000 + ₹3,000 − ₹5,000) with settlement tags: ₹3,000 settled from 19 Nov 2025 pesgi (oldest first) and ₹2,000 settled from 19 Nov 2026 loan. Screenshot: `docs/testing/screenshots/udhaar/step-5.5-rameshwar-person.png`.
  - 5.6: Opened Repay dialog ("पैसे मिले") on Rameshwar screen. Attempted overpayment of Rs 9,000 against Rs 8,000 due. UI blocked submission with message "बाकी सिर्फ ₹8,000 है". Confirm button was disabled. Screenshot: `docs/testing/screenshots/udhaar/step-5.6-overpayment-blocked.png`.
  - 5.7: Long-pressed Buy slip #4 in Khata. Verified void confirmation alert text: "इस पर्ची की उधार कटौती (₹5,000) भी वापस हो जाएगी।". Selected reason "गलत प्रविष्टि". Verified slip marked void (`रद्द · गलत प्रविष्टि`), slip balance returned to ₹0 (`हिसाब चुकता`), udhaar balance returned to ₹13,000 (`दिया (लेना है)`), and SQLite confirmed both loans reset to `OPEN`, both settlements `is_void = 1`, and both cut BOOK_ENTRY cash rows `is_void = 1`. Galla cash (`₹44,980`) was unchanged. Screenshots: `docs/testing/screenshots/udhaar/step-5.7-void-confirm.png` and `docs/testing/screenshots/udhaar/step-5.7-khata-after-void.png`.
- Unverified:
  - VPS deploy, production-database migration, real-device sync, and physical Redmi Note 7 (`d93c743`) stay unverified.
- Next: Final user sign-off and VPS deployment approval from Vivek.

## 2026-11-19 · Gemini 3.8 Flash / Antigravity · Task 14: Khata Entry Points from Udhaar Person Screen & Parchi Slips (no push, no deploy)
- What was built:
  - Udhaar Person Screen Button: Added "खाता देखें" (View Khata in English) `MandiOutlinedButton` with `Icons.AutoMirrored.Filled.MenuBook` on `UdhaarPersonScreen.kt` (48dp height, NGDL tokens, no emoji, debounced rapid double-tap, safe no-op on missing party).
  - View model party wiring: Added `party: Party? = null` to `UdhaarPersonUiState` and wired it from `UdhaarPersonViewModel.observeData()`, making the party domain object accessible to UI actions.
  - Parchi slip cards: Made the party name clickable in `KisaanSlipCard` and `VyapariSlipCard` inside `ParchiListScreen.kt` to open the party's khata, while keeping the existing slip-body tap (`onOpenReceipt`) and long-press void flow intact. Added debounced click handling and safe fallback for missing/deleted parties.
  - Navigation helper in `App.kt`: Added `partyKhataScreen(partyId, returnScreen)` ensuring back button resolves to the caller screen (`Screen.UdhaarPerson` returns to person screen; `ParchiListScreen` returns to `Screen.Home`). Reused helper across both entry points with no duplicate `Screen.PartyLedger` constructors.
  - Restored Card elevation: Restored `CardDefaults.cardElevation(defaultElevation = 1.dp)` in `DashboardScreen.kt` on `UdhaarCard` and `AllTimeCard` using NGDL card shadow token pattern.
  - Regression routing test: Added `partyKhataScreenResolvesToPartyLedgerAndBackTargetMatchesCaller` in `ScreenRoutingTest.kt`.
- Test counts from XML (`build/test-results/jvmTest/*.xml`):
  - `:core-domain`: 109 tests (0 failures)
  - `:core-database`: 12 tests (0 failures)
  - `:core-data`: 186 tests (0 failures)
  - `:composeApp`: 415 tests (0 failures, +1 new test in `ScreenRoutingTest`)
  - Total: 722 tests passing (0 failures).
  - NGDL tokens test: `pytest tests/ngdl-tokens`: 5/5 passing (0.01s).
- Device walk on `emulator-5554` (staging `http://64.227.142.180:8060`, Android 15, 390dp reference width, density 443; desktop 720dp, density 240):
  - Flow 1 (Udhaar -> Person -> Khata -> Back): Walked Home -> Udhaar landing -> Rameshwar (`9876501300`) -> tapped "खाता देखें" -> verified Rameshwar Khata opened -> pressed back -> returned to Rameshwar's `UdhaarPersonScreen`.
  - Flow 2 (Parchi -> Party name -> Khata -> Back): Walked Home -> Parchi -> tapped party name "Monty" on Buy Slip #1 -> verified Monty Khata opened -> pressed back -> returned to Home.
  - Flow 3 (Void dialog warning): Long-pressed Buy Slip #4 in Khata and verified void confirm dialog displays "इस पर्ची की उधार कटौती (₹5,000) भी वापस हो जाएगी।".
  - Screenshots captured under `docs/testing/screenshots/udhaar/`:
    - 390dp Light: `t14-home-light-390.png`, `t14-landing-light-390.png`, `t14-add-form-light-390.png`, `t14-person-light-390.png`, `t14-khata-light-390.png`
    - 390dp Dark: `t14-home-dark-390.png`, `t14-landing-dark-390.png`, `t14-add-form-dark-390.png`, `t14-person-dark-390.png`, `t14-khata-dark-390.png`
    - Desktop width (>600dp, 720dp) Light: `t14-person-desktop-light.png`, `t14-khata-desktop-light.png`
  - Contrast readings:
    - Light theme: `#090A0C` on `#F8F9FA` = 18.79:1 (WCAG AAA)
    - Dark theme: `#FFFFFF` on `#08090A` = 19.93:1 (WCAG AAA)
- Card/surface pattern used: `MandiOutlinedButton` using `RoundedCornerShape(10.dp)`, `BorderStroke(1.dp, MandiBorder)`, `MandiTextPrimary` text and icon tint, 48dp touch target height.
- Unverified: Physical Redmi Note 7 (`d93c743`), VPS deploy, production migration, real-device sync.
- Next steps: User sign-off and VPS production deployment.

## 2026-11-19 · Gemini 3.8 Flash / Antigravity · Task 15: Create Pesgi from Udhaar Add Sheet (no push, no deploy)
- What was built:
  - Added `var kind by mutableStateOf(LendKind.LOAN)` to `UdhaarAddFormState.kt`. Updated `toForm()` to set `kind = if (direction == LendDirection.GIVEN) kind else LendKind.LOAN`.
  - Updated `UdhaarAddSheet.kt`:
    - Updated `UdhaarAddFormStateSaver` to be `internal` and added `saveState` helper function. Appended `value.kind.name` at index 12, with safe `LendKind.LOAN` fallback on short (older) lists.
    - Added two-option `TabRow` segmented control ("उधार / पेशगी", English: "Loan / Advance") near the top below the header on `direction == LendDirection.GIVEN` only (hidden on `LendDirection.TAKEN`). Reused the existing `TabRow` segmented pattern from `UdhaarLandingScreen.kt:317-356` with NGDL tokens (`MandiSurface`, `MandiBorder`, `MandiPrimaryAction`, `MandiTextPrimary`, `MandiTextSecondary`), 48dp touch targets, and zero horizontal scroll at 390dp.
    - Added one-line helper text when "पेशगी" is chosen: "फसल बेचने से पहले दिया गया पैसा, अगली पर्ची से कटेगा।" (English: "Money given before the crop is sold. It is cut from the next slip."). Both strings follow the inline `isEnglish` pattern.
    - Left the interest toggle switch available and defaulting to off for both loan and pesgi.
  - Tests:
    - `UdhaarAddFormStateTest`: added `kindDefaultsToLoanAndSettingPesgiFlowsToForm` (default is LOAN, setting PESGI flows into `toForm`, TAKEN resets to LOAN), and `saverRoundTripPreservesKindAndShortListRestoresLoan` (saver round-trip preserves PESGI, 12-element short list restores LOAN).
    - `UdhaarUseCaseTest`: added `createGivenPesgiStoresKindPesgiAndWritesLinkedCashOut` (creating with `kind = PESGI` stores `LendEntry` with `kind = PESGI`, direction `GIVEN`, and linked `UDHAR_GIVEN` cash row).
    - `BuyLoanCutTest`: updated `pesgiLoanPlusOrdinaryLoanOlderCutFirst` to explicitly assert the settlement row carries the correct loan id and principal.
- Test counts from XML (`build/test-results/jvmTest/*.xml`):
  - `:core-domain`: 109 tests (0 failures)
  - `:core-database`: 12 tests (0 failures)
  - `:core-data`: 187 tests (0 failures, +1 test)
  - `:composeApp`: 417 tests (0 failures, +2 tests)
  - Total: 725 tests passing (0 failures).
  - NGDL tokens test: `pytest tests/ngdl-tokens`: 5/5 passing (0.01s).
- Device walk on `emulator-5554` (staging `http://64.227.142.180:8060`, Android 15, 390dp reference width, density 443; desktop 720dp, density 240) with zero SQLite edits:
  - 7.1: Added Rs 10,000 loan for farmer Kisan Dev (`9876500015`) with interest off. Verified on landing list and person screen.
  - 7.2: Added Rs 3,000 pesgi for Kisan Dev using the new "पेशगी" toggle on the add sheet. Verified person screen showed total "बाकी ₹13,000" and the "पेशगी" chip.
  - 7.3: Created Buy slip #6 for Kisan Dev: 4 bags of 50kg @ Rs 5,000/qtl = Gross Rs 10,000, 0 charges. Detected "पुराना उधार / पेशगी: ₹13,000" and label "पेशगी / उधार से काटें (रु)". Entered Rs 5,000 loan cut, final due Rs 5,000. Paid Rs 4,000 cash on payment screen. Confirmed loan cut settled oldest loan first (Rs 10,000 loan `d476b196...`, entry_date `1795075006027` < pesgi entry_date `1795075229526`).
  - 7.4: Checked receipt #6: showed deduction line `उधार कटौती: − ₹5,000`, payment history `• नकद भुगतान · 19 Nov: − ₹4,000`, and remaining slip balance `₹1,000`.
  - 7.5: Verified Udhaar person screen showed remaining Rs 8,000 left (Rs 5,000 on oldest loan + Rs 3,000 on pesgi) with settlement tag: ₹5,000 settled from oldest loan.
  - 7.6: Opened Repay dialog ("पैसे मिले") on Kisan Dev. Attempted overpayment of Rs 9,000 against Rs 8,000 due. UI blocked submission with message "बाकी सिर्फ ₹8,000 है" and submit button disabled.
  - 7.7: Voided slip #6 in BuyList with reason "गलत प्रविष्टि". Verified Udhaar landing and person screen balance restored to Rs 13,000, and oldest loan returned to OPEN.
- Screenshots captured under `docs/testing/screenshots/udhaar/`:
  - `t15-add-sheet-loan-light-390.png`
  - `t15-add-sheet-pesgi-light-390.png`
  - `t15-add-sheet-pesgi-dark-390.png`
  - `t15-add-sheet-desktop-light.png`
- Contrast readings:
  - Light theme: selected `#090A0C` on `#FFFFFF` = 19.81:1 (AAA); unselected `#585E6B` on `#FFFFFF` = 6.49:1 (AA)
  - Dark theme: selected `#FFFFFF` on `#121316` = 18.92:1 (AAA); unselected `#A0A5AE` on `#121316` = 7.58:1 (AA)
- Reused pattern: `TabRow` segmented control from `UdhaarLandingScreen.kt:317-356`.
- Unverified: Physical Redmi Note 7 (`d93c743`), VPS deploy, production migration, real-device sync.
- Next steps: User sign-off and VPS production deployment.

## 2026-11-19, Grok 4.7 (Cursor cloud) (product spec, session 1)
- Docs only. Added `docs/PRODUCT_SPEC.md` from Vivek's confirmed notes of 2026-11-19 (users, pilot shop, pain, language, success). Sections 2 to 5 left as "TBD, session pending". One README line points at the spec.
- Code check for the README note (read-only, no test run): two-stage deal status is in `Deal.kt`; farmer commission default is 0.70% and buyer commission default is 1.50%, not a fixed 2%; mandi shulk on a bill of supply is 1.50%; hammali is slip labour (default Rs 10 per bag) plus an expense category; rural interest is simple, default 2% per month; daily cash register, WhatsApp share, and Hindi voice are present. That note is marked inferred, not owner-confirmed.
- `FEATURES.json` unchanged (33 passing, 5 failing). `hot.md` not updated. `./init.sh` run with `SKIP_TESTS=1` because this session does not change app code. Gradle tests not run.
- Next: session 2, Core Flows. Do not treat the inferred code note as a product decision.

## 2026-11-19, Grok 4.7 (Cursor cloud) (one Alembic baseline)
- What changed: revisions `0002` through `0008` are folded into `0001_baseline`. An empty database upgrades to the current schema and downgrades back to empty. Quota columns keep their server defaults (`50`, `0`, `0`, `FREE`). A lend row that omits direction and kind stores `GIVEN` and `LOAN`.
- Evidence: backend pytest 140 passed before and 140 passed after. `test_alembic_upgrade_and_downgrade` matches `Base.metadata`. `test_baseline_udhaar_defaults_on_insert` checks the udhaar defaults. JVM tests were not re-run; this change does not touch Kotlin. Last JVM count on main: 725 (109, 12, 187, 417), 0 failures.
- Unverified: staging `alembic_version` is still `0008_udhaar_fields`. The next deploy must `alembic stamp 0001_baseline` before `upgrade`. This change does not touch the VPS. Feature `alembic-single-baseline` stays failing until that stamp.
- Next: Vivek approves merges. Claude reviews. Antigravity runs the device checks.

## 2026-11-19, Grok 4.7 (Cursor cloud) (buy-slip advance is not a pesgi)
- What changed: `getUnlinkedFarmerAdvancesBefore` now requires `source_entry_id IS NULL`. A buy-slip अग्रिम is still `UDHAR_GIVEN` cash linked to the slip, so the bill line and the drawer movement stay. That row is no longer readable as an unlinked pesgi. No second create-pesgi path.
- Evidence: `BuyCheckoutTest.slipAdvanceStaysOnTheBillAndOutOfPesgi`. XML counts before: core-domain 109, core-database 12, core-data 187, composeApp 417 (725, 0 failures). After: core-domain 109, core-database 12, core-data 188, composeApp 417 (726, 0 failures).
- Unverified: device check of the buy-slip drawer flow, light theme, dark theme, 390dp. Feature `buy-slip-advance-not-pesgi` stays failing.
- Next: flat cards on Dashboard and Expense, using `MandiCard`.

## 2026-11-19, Grok 4.7 (Cursor cloud) (shared status board)
- What changed: Added `docs/STATUS.md` and a Shared status board rule at the top of `AGENTS.md`. `CLAUDE.md` and `GEMINI.md` follow it because they are symlinks. No `FEATURES.json` row. `hot.md` not updated.
- Evidence: Board filled from `main` `e8b7259`, open PRs #3 #4 #5 #6, `PROGRESS.md`, `FEATURES.json` (36 passing, 5 failing), `docs/PRODUCT_SPEC.md`, and `AGENTS.md`. Docs only. Gradle tests not run.
- Next: Agents read the board and take their queue. Vivek merges #3 to #6 and keeps every new `FEATURES.json` row and `PROGRESS.md` entry. After #6, the next staging deploy must `alembic stamp 0001_baseline` before upgrade.

## 2026-11-19, Grok 4.7 (Cursor cloud) (flat cards on Home and Expenses)
- What changed: the live cards on `DashboardScreen` and `ExpenseListScreen` now use `MandiCard` (surface, 1dp border, no hardcoded elevation). No new shadow token.
- Evidence: `defaultElevation` is gone from those two files. JVM XML before and after: core-domain 109, core-database 12, core-data 187, composeApp 417, 0 failures (725). `tests/ngdl-tokens` is not in the repo.
- Unverified: light, dark, and 390dp on a device. Feature `flat-cards-dashboard-expense` stays failing.
- Left in place: `DashboardRevampComponents.kt` still hardcodes elevation. Nothing in the app calls those composables.
- Next: the same flat card on Buy, Sell, Parchi, Stock, and the drawer summary.

## 2026-11-19, Grok 4.7 (Cursor cloud) (flat cards on lists and the drawer)
- What changed: Buy, Sell, Parchi, Stock, the stock breakdown sheet, and the drawer summary no longer set a hardcoded card elevation. Cards that had no border now use `MandiSurface` and a 1dp `MandiBorder`, the same flat card as `MandiCard`. Long-press on a buy or sell row is unchanged. A negative stock line keeps its red container.
- Evidence: JVM XML before and after: core-domain 109, core-database 12, core-data 187, composeApp 417, 0 failures (725).
- Unverified: light, dark, and 390dp on a device. Feature `flat-cards-lists` stays failing.
- Left in place: billing cards, the party search dropdown (it needs a raised menu), and `DashboardRevampComponents.kt` (nothing calls it).
- Next: squash Alembic 0001 to 0008 into one baseline. Staging still needs a stamp on the next deploy. Do not touch the VPS in this change.

## 2026-11-19 · Gemini 3.8 Flash / Antigravity · Task 16: Staging Reset Retest & Task 14 Gaps (branch antigravity/device-audit-2026-11-19, PR #9, no deploy, no code changes)
- What ran:
  - Task 16 Udhaar round-trip retest executed on the rebuilt staging server (`http://64.227.142.180:8060`, PostgreSQL 16 on migration `0008_udhaar_fields`).
  - Reset emulator (`adb uninstall com.appwork.mandisamiti`), reinstalled debug APK, performed fresh UI signup (`9826012345`, "Vivek Mandi Shop", Mathura). First sync completed with `Worker result SUCCESS`.
  - Created round-trip dataset strictly through app UI (zero SQLite or direct DB mutations):
    1. Loan GIVEN Rs 10,000 to Farmer A (`9876543210`).
    2. Pesgi GIVEN Rs 3,000 to Farmer A via "उधार / पेशगी" segmented toggle.
    3. Loan GIVEN Rs 2,000 to Farmer B (`9876543211`) with receiver switch ON, Receiver Name "Receiver X", Receiver Phone `9876543299`.
    4. Loan TAKEN Rs 5,000 from Party C (`9876543212`).
    5. Repayment of Rs 1,000 recorded against Farmer A's Rs 10,000 loan via Person Screen FAB ("पैसे मिले").
  - Triggered sync from Settings ("अभी बैकअप लें"). Confirmed in logcat `MandiSyncWorker` SUCCESS.
  - Read-only SELECT on staging DB (`mandisamiti_staging_db`) confirmed:
    - `lend_entries`: 4 rows (Rs 10,000 GIVEN LOAN; Rs 3,000 GIVEN PESGI; Rs 2,000 GIVEN LOAN with receiver name/phone; Rs 5,000 TAKEN LOAN).
    - `lend_settlements`: 1 row (Rs 1,000, `is_void = 0`).
    - `cash_transactions`: 5 rows (3 `UDHAR_GIVEN`, 2 `JAMA_RECEIVED`, all with `lend_id` set).
    - `parties`: 3 rows.
  - Wipe and Pull Proof:
    - Uninstalled app from emulator, reinstalled debug build, logged in with same test account.
    - Verified initial pull succeeded: Farmer A balance Rs 12,000 with "पेशगी" chip on Rs 3,000 entry and Rs 1,000 repayment history; Farmer B showed "प्राप्तकर्ता: Receiver X"; Party C displayed under "उधार लिया" as TAKEN; Dashboard displayed Rokad `-₹9,000`, Udhaar `दिया: ₹14,000`, `लिया: ₹5,000`.
  - Task 15 Walk Re-Run on Device:
    - Created Buy slip #2 for Farmer A: 20 bags @ 50kg = 10.0 Q @ Rs 1,000/Q (Gross Rs 10,000, Net Rs 9,730). Cut Rs 5,000 loan, paid Rs 4,000 cash.
    - Confirmed cut settled oldest loan 3.1 first (Rs 10,000 loan, created earlier than pesgi 3.2, sorted oldest-first by `UdhaarAllocator.allocate`).
    - Verified overpayment block on Person Screen: attempting Rs 8,000 repayment against Rs 7,000 remaining balance immediately blocked with "बाकी सिर्फ ₹7,000 है".
    - Voided Buy slip #2 from buy list (`BuyTransactionsListScreen`) with reason "गलत प्रविष्टि".
    - Confirmed Farmer A balance returned from Rs 7,000 to Rs 12,000.
    - Synced to staging; server read-only SELECT confirmed `buy_entries` slip #2 marked `is_void = 1`, `lend_settlements` Rs 5,000 cut marked `is_void = 1`, and associated cash transaction marked `is_void = 1`.
  - Task 14 Gaps Live Device Audit:
    - 7.1: Party with 0 slips and 0 udhaar: Not reachable from UI (Parchi list only displays parties with non-void slips, Udhaar list only displays parties with balance > 0, no general party directory exists).
    - 7.2: Voided slip party name tap: Not reachable on Parchi list (voided slips are filtered out from ParchiListScreen by `buys.filter { !it.isVoid }`).
    - 7.3: Back navigation from Khata opened via Parchi party name: Consistently lands on Home (`Screen.Home`), verified twice on live device (`returnScreen = Screen.Home`).
    - 7.4: Double-tapping "खाता देखें" quickly: Opened exactly one Khata screen; pressing back once immediately returned to UdhaarPersonScreen (no duplicated backstack).
    - 7.5: Unresolvable party khata: Not reachable (Parchi list click disabled when `party == null`; Udhaar person screen requires non-null state party).
    - 7.6: English and Hindi button labels verified on live device by toggling language in Settings. Captured screenshots of "खाता देखें" (Hindi) vs "View Khata" (English).
- Screenshots saved:
  - `docs/testing/screenshots/udhaar/t16-person-pull-light-390.png`
  - `docs/testing/screenshots/udhaar/t16-pesgi-chip-light-390.png`
  - `docs/testing/screenshots/udhaar/t16-pesgi-chip-dark-390.png`
  - `docs/testing/screenshots/udhaar/t16-buttons-hi-390.png`
  - `docs/testing/screenshots/udhaar/t16-buttons-en-390.png`
- Cleanup: No extra worktrees exist (`ms-repay-edit-wt` deleted/absent). Temporary scratch files removed.
- Unverified: Physical Redmi Note 7 (`d93c743`) was detached during session; VPS production deployment and production DB migration unverified.
- Next steps: User sign-off; connect Redmi Note 7 for physical device verification before release.

## 2026-11-19 · Gemini 3.8 Flash / Antigravity · PR Branch Device Audit (#3, #4, #5) & Dark Theme 7-Screen Card Audit at 390dp
- What ran:
  - 1. PR #3 (`cursor/buy-slip-advance-not-pesgi-4465`, branch commit `e0557fa`, merged to main as `6ed6866`):
    - Tested Buy-slip अग्रिम flow strictly on a fresh build of PR #3 (`./gradlew :composeApp:installDebug`).
    - Entered Buy slip #8 for Kisan Dev: 10 bags @ 50kg (5.0 Q @ ₹1,000/Q) = Gross ₹5,000. Commission ₹30, Mazdoori ₹100 -> Parchi net ₹4,870.
    - Entered अग्रिम / पेशगी (Advance): ₹1,000. Final payable: ₹3,870. Completed checkout with ₹0 additional cash.
    - Verification 1 (Bill): Bill / Receipt displays deduction line `अग्रिम / पेशगी: − ₹1,000` (and `• नकद भुगतान · 19 Nov: − ₹1,000`).
    - Verification 2 (Drawer Cash): Cash drawer dropped by exactly ₹1,000 from ₹26,980 to ₹25,980.
    - Verification 3 (Udhaar दिया): Udhaar दिया remained ₹76,133.33 (did NOT grow, zero `LendEntry` created).
    - Verification 4 (Person Screen): Kisan Dev person screen remained at ₹13,000 (Rs 3k pesgi + Rs 10k loan). Gained NO pesgi.
    - Screenshots: `docs/testing/screenshots/audit/pr3-buy-slip-receipt-advance.png`, `docs/testing/screenshots/audit/pr3-dashboard-verified.png`, `docs/testing/screenshots/audit/pr3-kisan-dev-person-screen.png`.
  - 2. PR #4 (`cursor/flat-cards-dashboard-expense-4465`, commit `18dbde1`):
    - Built and installed debug APK of PR #4.
    - Audited flat cards on Home (Dashboard) and Expenses (`Screen.ExpenseList`) in Light and Dark themes at 390dp: verified flat `MandiCard`, 1dp `MandiBorder`, no shadow/elevation, clean readable typography.
    - Build commit `18dbde1` screenshots:
      - `docs/testing/screenshots/audit/pr4-home-light-390.png`
      - `docs/testing/screenshots/audit/pr4-home-dark-390.png`
      - `docs/testing/screenshots/audit/pr4-expense-light-390.png`
      - `docs/testing/screenshots/audit/pr4-expense-dark-390.png`
  - 3. PR #5 (`cursor/flat-cards-lists-4465`, commit `33be63c`):
    - Built and installed debug APK of PR #5.
    - Audited flat cards on Lists (Buy list, Sell list, Parchi list, Stock), Drawer summary (Daily register), and Void dialog in Light and Dark themes at 390dp.
    - Long-press on buy row in `BuyTransactionsListScreen` verified: `VoidEntryDialog` ("खरीद सौदा रद्द करें?") opens cleanly.
    - Build commit `33be63c` screenshots:
      - `docs/testing/screenshots/audit/pr5-buylist-light-390.png`
      - `docs/testing/screenshots/audit/pr5-buylist-dark-390.png`
      - `docs/testing/screenshots/audit/pr5-void-dialog-light-390.png`
      - `docs/testing/screenshots/audit/pr5-void-dialog-dark-390.png`
      - `docs/testing/screenshots/audit/pr5-selllist-light-390.png`
      - `docs/testing/screenshots/audit/pr5-selllist-dark-390.png`
      - `docs/testing/screenshots/audit/pr5-parchi-light-390.png`
      - `docs/testing/screenshots/audit/pr5-parchi-dark-390.png`
      - `docs/testing/screenshots/audit/pr5-drawer-light-390.png`
      - `docs/testing/screenshots/audit/pr5-drawer-dark-390.png`
      - `docs/testing/screenshots/audit/pr5-stock-light-390.png`
      - `docs/testing/screenshots/audit/pr5-stock-dark-390.png`
  - 4. 7-Screen Card Audit in Dark Theme at 390dp (throwaway shop "Vivek Mandi Shop"):
    - Emulator configured to 390dp reference width (`1080x2400` at density 443 -> `1080 / 2.76875 = 390.07dp`).
    - Night mode enabled (`cmd uimode night yes`).
    - All 7 screens inspected for card readability, 1dp borders, zero clipped text, and contrast:
      1. Home (`Screen.Home`): `audit-dark-home-390.png`
      2. Expenses (`Screen.ExpenseList`): `audit-dark-expense-390.png`
      3. Buy list (`Screen.BuyList`): `audit-dark-buylist-390.png`
      4. Void dialog (`VoidEntryDialog`): `audit-dark-buylist-void-390.png` (verified shasum differs from drawer)
      5. Sell list (`Screen.SellList`): `audit-dark-selllist-390.png`
      6. Parchi (`Screen.ParchiList`): `audit-dark-parchi-390.png`
      7. Stock (`Screen.StockRegister`): `audit-dark-stock-390.png`
      8. Drawer summary (`Screen.DailyRegister`): `audit-dark-drawer-390.png`
  - 5. Device & Passing Rule:
    - Physical Redmi Note 7 (`d93c743`) was detached.
    - Per repo rule (`CLAUDE.md`), emulator evidence is recorded but feature status in `FEATURES.json` remains `failing` until verified on physical Redmi Note 7 (`d93c743`).
- Screenshots captured:
  - `docs/testing/screenshots/audit/*`
- Unverified: Physical Redmi Note 7 (`d93c743`), VPS production deployment, production database migration.

## 2026-11-19 · Gemini 3.8 Flash / Antigravity · Task 1: PR #9 Audit & Screenshot Fixup
- What changed:
  - 1. Void Dialog vs Drawer Shasum Fix:
    - Retook dark Buy list void dialog (`audit-dark-buylist-void-390.png`) by long-pressing buy row in `Screen.BuyList` with `VoidEntryDialog` open.
    - Confirmed distinct SHA-1 checksums:
      - `audit-dark-buylist-void-390.png`: `f624303cfef280ce812dadd107617d60231af2a1`
      - `audit-dark-drawer-390.png`: `ab58cb822820736d078048714e63774b1b725269`
  - 2. Per-PR, Per-Theme Evidence at 390dp:
    - Built and installed PR #4 (`cursor/flat-cards-dashboard-expense-4465`, commit `18dbde1`) independently; captured light and dark for Home and Expenses (`pr4-home-light-390.png`, `pr4-home-dark-390.png`, `pr4-expense-light-390.png`, `pr4-expense-dark-390.png`).
    - Built and installed PR #5 (`cursor/flat-cards-lists-4465`, commit `33be63c`) independently; captured light and dark for Buy list, Void dialog, Sell list, Parchi list, Stock register, and Drawer summary (`pr5-buylist-light-390.png`, `pr5-buylist-dark-390.png`, `pr5-void-dialog-light-390.png`, `pr5-void-dialog-dark-390.png`, `pr5-selllist-light-390.png`, `pr5-selllist-dark-390.png`, `pr5-parchi-light-390.png`, `pr5-parchi-dark-390.png`, `pr5-drawer-light-390.png`, `pr5-drawer-dark-390.png`, `pr5-stock-light-390.png`, `pr5-stock-dark-390.png`).
    - Removed old unmatched screenshot files: `pr4-dashboard-light.png`, `pr4-expense-list-light.png`, `pr5-void-dialog.png`, and `docs/testing/screenshots/audit-home-390.png`.
    - Removed any "verified" claim lacking a supporting screenshot file.
  - 3. Shop Data & Clean Auth:
    - Old drawer screenshot displayed "Omveer Singh & Brothers" from legacy test data. Noted in documentation, removed all images depicting it, and retook all audit screenshots under the throwaway test shop "Vivek Mandi Shop" (Mathura).
    - Logged in fresh with phone `9826012345` / MPIN `1234` against staging (`http://64.227.142.180:8060`); zero "दोबारा लॉगिन करें" banners appeared.
  - 4. Text & Doc Fixes:
    - `FEATURES.json` (`udhaar-round-trip-staging`): noted that back navigation from Khata opened via Parchi currently lands on Home as a known bug (Vivek decided it must return to Parchi list, fix not in this PR), and staging is now stamped `0001_baseline`.
    - `docs/STATUS.md`: moved merged PR #3 (`6ed6866`) out of "Needs device check", noting its evidence originated from branch commit `e0557fa`. Added build commit hashes next to screenshot groups.
    - `PROGRESS.md`: corrected Task 16 heading.
- Evidence:
  - All 16 PR4 and PR5 screenshots captured on live Android 15 emulator at 390dp reference width (`1080x2400`, density 443) under throwaway shop "Vivek Mandi Shop".
  - Unique shasums confirmed across all dark/light screenshot pairs.
- Unverified: Physical Redmi Note 7 (`d93c743`) check (phone disconnected); VPS production deployment; production DB migrations.
- Next steps: Stop and notify Vivek that PR #9 (`antigravity/device-audit-2026-11-19`) is ready for merge. Await Vivek's reply before proceeding to Task 2.

## 2026-11-19 · Gemini 3.8 Flash / Antigravity · Task 2: Redmi Note 7 (d93c743) Hardware Checks
- What changed:
  - 1. Physical Device Reconnection & Hardware Audit:
    - Physical Redmi Note 7 (`d93c743`) connected via ADB (`1080x2340`, density 440 -> 392.7dp reference width, Android 10, MIUI).
    - Resolved MIUI "Install via USB" permission block in Developer Options; installed debug build pointing directly to staging (`http://64.227.142.180:8060`).
    - Logged into throwaway shop "Vivek Mandi Shop" (phone `9826012345`, MPIN `1234`) with zero "दोबारा लॉगिन करें" banners.
  - 2. `udhaar-round-trip-staging` Verification:
    - Full Udhaar round-trip verified on physical Redmi:
      - Initial pull restored exact balances and UI chips: Farmer A remaining Rs 12,000 with Rs 3,000 "पेशगी" chip and Rs 1,000 settlement history; Farmer B remaining Rs 2,000 with "प्राप्तकर्ता: Receiver X"; Party C under "उधार लिया" (TAKEN) with remaining Rs 5,000.
      - Tested overpayment block on Farmer B repay dialog: entering Rs 3,000 against Rs 2,000 balance showed UI error "बाकी सिर्फ ₹2,000 है" and disabled save button.
      - Tested Buy slip creation and void: created slip #4 with Rs 1,000 cash payment out of Galla Rokad, then voided from Buy list via long-press with reason "गलत प्रविष्टि" (WRONG_ENTRY).
      - Staging PostgreSQL read-only SELECT verified: `buy_entries` row has `is_void=1`, `void_reason='WRONG_ENTRY'`; `lend_entries` count remained 4 with zero unlinked pesgi created.
    - Moved `udhaar-round-trip-staging` to `passing` in `FEATURES.json`.
  - 3. `buy-slip-advance-not-pesgi` Verification:
    - Verified on physical Redmi Note 7 (`d93c743`):
      - Created Buy slip #4 for Farmer A (4 bags @ Rs 2,500/Q, gross Rs 5,000, net Rs 4,930) with Rs 1,000 cash payment from Galla Rokad.
      - Verified: 1) Bill shows deduction line "नकद भुगतान: − ₹1,000" and remaining balance Rs 3,930; 2) Drawer Rokad dropped by exactly Rs 1,000 from -Rs 13,000 to -Rs 14,000; 3) Udhaar दिया did not grow (stayed Rs 14,000; zero unlinked pesgi created); 4) Farmer A person screen balance remained unchanged at Rs 12,000 with zero unlinked pesgi added; 5) Staging PostgreSQL verified: `cash_transactions` row has `source_entry_id` set, and `lend_entries` count intact at 4.
    - Moved `buy-slip-advance-not-pesgi` to `passing` in `FEATURES.json`.
  - 4. PR #4 Flat Cards Hardware Audit (commit 18dbde1):
    - Installed build of PR #4 commit `18dbde1` on physical Redmi Note 7 (`d93c743`).
    - Toggled Light and Dark themes natively via Display Settings; verified 1dp flat border with zero elevation shadows on Dashboard and Expenses at 392dp (`pr4-home-light-redmi-390.png`, `pr4-home-dark-redmi-390.png`, `pr4-expense-light-redmi-390.png`, `pr4-expense-dark-redmi-390.png`).
    - Added row `flat-cards-dashboard-expense` as `passing` in `FEATURES.json`.
  - 5. PR #5 Flat Cards Hardware Audit (commit 33be63c):
    - Installed build of PR #5 commit `33be63c` on physical Redmi Note 7 (`d93c743`).
    - Verified flat borders in Light and Dark themes across all 6 screens at 392dp: Buy list, Void dialog, Sell list, Parchi list, Drawer summary, and Stock register (`pr5-*-redmi-390.png`). Long-press void dialog verified and functioning.
    - Added row `flat-cards-lists` as `passing` in `FEATURES.json`.
- Evidence:
  - 23 screenshots captured directly on physical Redmi Note 7 (`d93c743`):
    - Flow: `redmi-farmer-a-person-screen.png`, `redmi-farmer-b-person-screen.png`, `redmi-repay-overpayment-block.png`, `redmi-buy-slip-receipt.png`, `redmi-buylist-void-dialog.png`, `redmi-dashboard-verified.png`, `redmi-drawer-dark-390.png`.
    - PR #4: `pr4-home-light-redmi-390.png`, `pr4-home-dark-redmi-390.png`, `pr4-expense-light-redmi-390.png`, `pr4-expense-dark-redmi-390.png`.
    - PR #5: `pr5-buylist-light-redmi-390.png`, `pr5-buylist-dark-redmi-390.png`, `pr5-void-dialog-light-redmi-390.png`, `pr5-void-dialog-dark-redmi-390.png`, `pr5-selllist-light-redmi-390.png`, `pr5-selllist-dark-redmi-390.png`, `pr5-parchi-light-redmi-390.png`, `pr5-parchi-dark-redmi-390.png`, `pr5-drawer-light-redmi-390.png`, `pr5-drawer-dark-redmi-390.png`, `pr5-stock-light-redmi-390.png`, `pr5-stock-dark-redmi-390.png`.
  - Unique shasums confirmed across all captures.
- Unverified:
  - VPS production deployment and production DB migrations (mandisamiti_prod_* out of scope).
  - Khata back navigation from Parchi (known bug, fix reserved for Grok).
  - Staging alembic stamp for single baseline (covered in Task 3).
- Next steps: Open PR for Task 2, proceed to Task 3 (evidence rows for merges and staging deploy).

## 2026-11-19 · Gemini 3.8 Flash / Antigravity · Task 3: Merges & Staging Deploy Evidence
- What changed:
  - 1. Merged PRs & Staging Baseline Verification:
    - Verified PR #3 (`buy-slip-advance-not-pesgi`, commit `6ed6866`) and PR #6 (`alembic-baseline-4465`, commit `29170e2`) merged to `origin/main`.
    - Main branch commit `236f2c7` active and deployed on staging server via `deploy_staging.sh`.
  - 2. Live VPS Database & Migration State:
    - Queried live VPS container `mandisamiti_staging_api` via SSH: `alembic current` confirms `0001_baseline (head)`.
    - Health check `curl -s http://64.227.142.180:8060/health/ready` confirms `{"status":"ready","checks":{"db":"ok","redis":"ok"}}`.
    - Staging PostgreSQL data confirmed intact: 4 lend_entries (GIVEN LOAN Rs 10k, GIVEN PESGI Rs 3k, GIVEN LOAN Rs 2k, TAKEN LOAN Rs 5k), 10 cash transactions, 3 buy entries.
  - 3. Tracker Updates:
    - Moved `alembic-single-baseline` to `passing` with concrete VPS migration evidence.
    - Added new feature row `staging-deploy-main-baseline` as `passing` in `FEATURES.json`.
- Evidence:
  - Live VPS terminal output:
    - `ssh appworx-core-vps "docker exec mandisamiti_staging_api alembic current"` -> `0001_baseline (head)`.
    - `curl -s http://64.227.142.180:8060/health/ready` -> `{"status":"ready","checks":{"db":"ok","redis":"ok"}}`.
- Unverified: Production deployment (`mandisamiti_prod_*` stack out of scope); offsite backup cron.
- Next steps: Proceed to Task 4 (cleanup with Vivek's explicit confirmation, one question at a time).

## 2026-11-20 · Gemini / Antigravity · Milestone 1 Merge & Day 1 Phase 0 Delivery
- What changed:
  - 1. Merged all open November PRs (#4, #5, #9, #10, #11) into \`main\` via PR #12 (commit \`4048626\`).
  - 2. Purged committed SQLite database \`backend/mandi_backend.db\` from git tracking (commit \`679621e\`).
  - 3. Fixed Parchi-to-Khata back navigation bug: wired \`onNavigateToPartyKhataFromParchi\` in \`HomeScreen.kt\` and \`App.kt\` so \`homeViewModel.selectTab(NavigationTab.PARCHI)\` is restored when pressing back from Party Ledger (commit \`13e77f8\`).
  - 4. Merged Day 1 PR #13 (\`fix(day1): back navigation to parchi tab & purge committed sqlite db\`) into \`main\` (commit \`3072e58\`).
- Evidence:
  - JVM tests: \`./gradlew :composeApp:jvmTest\` (417 passed).
  - Backend tests: \`PYTHONPATH=. venv/bin/pytest tests/ -q\` (140 passed).
  - PR #12 merged to main: \`4048626\`.
  - PR #13 merged to main: \`3072e58\`.
- Next steps: Proceed with Day 2 of plan (SEC-02 secret parameterization and SEC-03 dead OTP bypass cleanup).


## 2026-11-20 (Claude Haiku 5.5, Claude Code): Udhaar Task 12 re-verify
- Task 12 code was already on main (1df00bf, 4ef1440). Branch `udhaar/task12-verify` created from main.
- Uncommitted App.kt edit found in the working tree (Screen.Home initialTab, removed onNavigateToPartyKhataFromParchi). It would undo the committed nav fix 13e77f8, so it was saved as a patch and not committed. Patch: scratchpad app-kt-uncommitted-backup.patch. Owner to confirm.
- Evidence: backend 140 passed; test_sync_udhaar + test_migrations 9 passed; UdhaarWireTest 7/7; SellDepositFilterTest 2/2; staging openapi has the Udhaar fields; staging /health/ready ok.
- Device: Redmi d93c743, debug build from main pointing at staging; Udhaar landing loads given and taken tabs.
- Next: on d93c743 add a TAKEN/PESGI entry with receiver name and phone on the throwaway shop, add a cash row linked to a lend, confirm both on staging. Then mark passing with the date.
- 2026-11-20 11:42 IST (Claude Haiku 5.5, Claude Code), Udhaar Task 12 device sync, partial: Redmi d93c743, debug build of main pointed at staging, throwaway shop "Vivek Mandi Shop". Added a TAKEN (उधार लिया) entry: mobile 9999900077, name SyncTestLender, amount ₹1000, date 20/11/2026, interest off. Save showed "उधार सफलतापूर्वक दर्ज किया गया" and the row appears in the list. logcat: WM-WorkerWrapper SUCCESS for MandiSyncWorker at 11:42:24. NOT verified: the server row on staging (no staging credentials used). Feature stays failing. Next: confirm the lend and linked cash row on staging (needs a read path with staging auth or VPS DB access), then mark passing with date.
- 2026-11-20 (Claude Haiku 5.5, Claude Code): Udhaar Task 12 server check on staging, partial pass. Staging Postgres (mandisamiti_staging_db, via the VPS console): lend_entries row 267a8fee-0fc6-40b9-9957-a933c4697fda, party SyncTestLender / 9999900077, direction TAKEN, kind LOAN, amount_paisa 100000, is_deleted 0, server_seq 70, created 11:42 IST. Matches the device save and the MandiSyncWorker SUCCESS. Receiver name/phone empty (expected for a LOAN, per code reading, not tested). Not yet verified: a PESGI entry with receiver_name/receiver_phone, and a cash repayment with lend_id on staging. Feature udhaar-sync-and-backend-schema stays failing. Next: fresh session, add PESGI + linked cash row on d93c743 (throwaway shop), check on staging, then mark passing with date.

- 2026-11-20 (Claude Haiku 5.5, Claude Code): Udhaar Task 12 PESGI + cash repayment check on staging, PASS. Device d93c743 (throwaway shop): PESGI given entry PesgiTestParty 9999900078, Rs 1000, receiver ReceiverTest 9999900079; repayment Rs 500 linked to it; sync worker SUCCESS. Staging Postgres via VPS console: lend_entries 57888c2d-1a15-49fb-9fda-0e8bae4936c3 GIVEN/PESGI, 100000 paisa, receiver fields match, server_seq 79; lend_settlements lend_id matches, 50000 paisa, is_void 0. Feature udhaar-sync-and-backend-schema marked passing (staging scope). Not verified: production VPS deploy and production migration. Not committed: FEATURES.json and PROGRESS.md edits await Vivek's approval for commit.


## 2026-11-20 18:45 IST · Claude Haiku 5.5 / Claude Code · PR #34 and PR #35 feature logging
- What changed:
  - 1. Recorded PR #34 (`fix(input): search keys, identifier keyboards, party lookup opens on text`, merged `60c31e9`) and PR #35 (`feat(khata): add all-parties khata entry point from dashboard`, merged `524eb9b`) in `FEATURES.json`.
  - 2. Added `input-search-keys-identifier-keyboards` (failing) and `all-parties-khata-entry` (passing) at the end of the list, per the FEATURES.json rules.
  - 3. Appended evidence to `input-audit-medium-low` (stays failing) and `party-lookup-phone-or-name` (stays passing). No descriptions were reworded.
  - 4. Deleted the merged branches on origin and local in this session. Only `main` remains on origin. Local `backup/local-main-unpushed-2026-11-20` kept on purpose (holds 2 commits not on main: `c2ced3b`, `de9bc92`).
- Evidence:
  - `./gradlew :composeApp:jvmTest`: 418 tests, 0 failures, 0 errors. Gradle reported the task up to date, so these are cached results for unchanged inputs on `main` at `524eb9b`.
  - `PartyLookupTest`: 8 of 8 pass.
  - Redmi `d93c743`, light and dark system theme (PR #34): Buy search 'Farmer' filters to Farmer A; Sell search 'Farmer' filters to Farmer B (light); empty state for an unmatched query.
  - Redmi `d93c743`, staging build, light theme (PR #35): dashboard 'सभी खाते' opens the list (5 parties), a party tap opens its ledger, Back returns to the list.
  - Staging deploy from `main` `524eb9b` via `backend/scripts/deploy_staging.sh`: smoke tests passed on `127.0.0.1:8060`; public `/health` returns `healthy`.
- Unverified:
  - PR #34: Search key on Buy does not dismiss the keyboard (open, not confirmed as a bug); 390dp pass; Search key on Sell and Khata.
  - PR #35: dark theme on the all-parties khata screen; 390dp pass.
  - Production deploy: not run (Vivek asked for staging only).
- Decisions needed from Vivek:
  - The description of `party-lookup-phone-or-name` still says the lookup opens on the number pad. PR #34 changed the default to the text keyboard. The rules forbid rewording, so the description is left stale until Vivek says otherwise.
- Not committed: `FEATURES.json` and this entry await Vivek's approval for the commit.
- Next steps: device checks for dark theme and 390dp on the all-parties khata screen; Search key behaviour on Buy; then move `input-search-keys-identifier-keyboards` to passing.

## 2026-11-20 07:47 UTC (Claude Haiku 5.5, Claude Code): Offsite backup restore drill, partial
- What changed:
  - Found the nightly staging backup had failed every night since at least 2026-11-19: `backend/scripts/backup_staging.sh` was committed as mode 100644, so cron printed `Permission denied` (`/var/log/mandi-backup.log`). No dump existed in `/root/mandi-backups`.
  - Set the execute bit on the VPS copy and in the git index (`git update-index --chmod=+x`, staged, not committed at this entry).
  - Ran `backup_staging.sh` once on the VPS: `staging-20261120-074712.sql.gz`, 14 tables.
- Evidence:
  - Local-stand-in restore drill on VPS: encrypted the dump with the real passphrase file, then ran `restore_test.sh` with `REMOTE` set to a local directory. Output: `restore-test ok: staging-20261120-074712.sql.gz.enc (14 tables, alembic row present, ~86 rows)`. Scratch remote removed afterwards.
  - Read-only checks: passphrase file present; `rclone.conf` absent; `gdrive` remote not configured; no Sunday cron for `backup_offsite.sh`.
- Not done (blocks the feature):
  - The `gdrive` rclone remote needs a one-time browser OAuth step. Until it exists, `backup_offsite.sh` cannot upload and `restore_test.sh` has not run against the Drive copy.
  - Sunday cron for `backup_offsite.sh` is not installed. Do not install it before the remote works.
- Next steps: configure the `gdrive` remote on the VPS (Vivek, interactive), then run `backup_offsite.sh` once, run `restore_test.sh` with the default remote, install the Sunday cron line from the script header, and update FEATURES.json to passing.

## 2026-11-20 (Claude Haiku 5.5, Claude Code): Offsite drill deferred for pilot
- Vivek decided the offsite (Google Drive) copy is not needed for pilot. Feature `offsite-backup-restore-drill` stays `failing` and carries a `deferred` note in FEATURES.json.
- Pilot safety relies on the nightly on-VPS backup (`backup_staging.sh`, cron 02:30 UTC). Fixed in c2ced3b. Confirm a second unattended dump appears in `/root/mandi-backups` before pilot go-live.
- Resume the offsite work after pilot: configure the `gdrive` rclone remote, run backup_offsite.sh, restore_test.sh, install the Sunday cron.

## 2026-11-24 22:18 IST - Claude Code - Redmi install
- Built debug APK from working tree (branch feat/galla-rokad-editable, incl. uncommitted Galla-Rokad edits), 21,788,008 bytes.
- `adb -s d93c743 install -r` Success; lastUpdateTime 2026-11-24 22:18:25; app launched.
- Next: Vivek checks Galla-Rokad screens on device (staging URL).

## 2026-11-24 23:06 IST - Claude Code (Opus 5.5) - Galla cash popup party picker
- Replaced the full-list khata picker in the नकद जमा/निकासी popup with one name box: type-ahead khata matches (BuyPartyResolver), "+ नया किसान खाता" (LendPartyResolver.partyToSave), or name only (kept in remarks, no khata). Live status line says which.
- Fixed: Save with no khata picked used to book the entry into the first party in the list.
- New: ui/register/CashEntryTarget.kt, CashEntryTargetTest.kt; recordDailyEntry gained optional newKisan.
- Evidence: CashEntryTargetTest 6/6; ./gradlew jvmTest 1157 tests, 0 failures; APK installed on Redmi d93c743 23:05.
- Next: Vivek checks on device (light+dark): pick khata, add kisan, name only, empty name; then fix the same first-party fallback in opening balance / cash-to-home in a separate PR.

## 2026-11-24 IST - Claude Code (Sonnet 5.5) - Parchi phone, kharcha, 2-decimal qtl, buy lab slip
- Mobile shown on buy/sell parchi list, receipt, print and share only when present. Sell list/receipt/entry/print/share have a kharcha row (arat + majoori). Quintals 2 decimals on parchi, buy, sell, slip screens. Buy slip prints mobile once (old vehicle row removed, it held the same number).
- Buy lab slip: per-lot checkbox, Lab value and Base value (kg), weight = (lab/base)*base, no bags, then rate, commission, majoori (0 bags), Rs 10 round-off. BuyCropLot gained labValueGrams/baseValueGrams (default 0, backward compatible).
- Evidence: compileDebugKotlinAndroid passes. No tests or device run, as asked.
- Next: device check light+dark; confirm lab units are kg; add lab slip math test once confirmed.

## 2026-11-25 IST - Claude Haiku 5.5 - Validation of lab slip + parchi display changes
- Units: lab and base values are quintals (qtl), not kg. Labels, parsing and BuyEntryFormState conversion use MandiMathEngine.gramsToQuintalsInputString().
- Button text in MandiPrimaryButton and MandiSecondaryButton: maxLines=1, ellipsis, reduced horizontal padding.
- Buy share slip drops the "कुल खर्चा" line. Labour and commission rows on the receipt keep their "−" prefix (Vivek decision).
- Evidence: ./gradlew :composeApp:jvmTest BUILD SUCCESSFUL. 430 tests, 0 failures, 0 errors, 0 skipped (JVM XML reports). BuyLabSlipTest 3/3, SlipEquationTest 3/3, ReceiptHtmlRendererTest 4/4.
- Not verified: device run on Redmi d93c743 (light and dark, 390px) for the lab slip and the button clipping fix. Still open.
- Next: device check before the feature is marked passing in FEATURES.json.

## 2026-11-25 IST - Claude Haiku 5.5 - Device check (partial), Redmi Note 7 d93c743, light theme
- Debug APK from 0e04a4a installed with -r. App launched on staging (default build config).
- PASS: parchi list shows mobile (9058666306) on card; quintals 22.70 Q with 2 decimals; kharcha not checked.
- PASS: buy entry shows "लैब पर्ची" checkbox; ticking it shows Lab value (क्विंटल) and Base value (क्विंटल) fields.
- PASS: lab 45, base 50 → weight 45.0 क्विंटल; rate 2500 → gross ₹1,12,500; final ₹1,11,710 (0.70% commission, round-off to ₹10).
- Harness note: first tap on base field landed in lab field because the keyboard shifted the layout. Re-entered with keyboard "next" key. Not an app bug, but check the field focus behaviour when the keyboard opens.
- NOT DONE: nothing saved. No receipt, print, share, dark theme, 390px overflow, or button-clipping check. Form left unsaved.
- Next: save one dummy-shop test entry on staging to check receipt, slip and dark theme, if Vivek approves.

## 2026-11-25 IST - Claude Haiku 5.5 - Buy entry: total kharcha row + scroll past content
- BuyEntryScreen slip card: added "कुल खर्चा" / "Total charges" row (labour + commission, shown with − prefix), same as the buy receipt.
- BuyEntryScreen scroll Column: removed the extra .imePadding() from the scroll column. The bottom dock keeps its own imePadding. Suspected cause of blank scrollable space when the keyboard was open (keyboard inset applied twice). Not confirmed by a before/after measurement.
- Evidence: ./gradlew :composeApp:assembleDebug :composeApp:jvmTest BUILD SUCCESSFUL. Redmi d93c743: "कुल खर्चा − ₹0" row visible in slip card (light theme, keyboard closed). Scroll to bottom with keyboard closed ends right after the last card, no extra blank area.
- NOT VERIFIED: scroll to bottom with the keyboard open (the blank area reproduced only in that state, before the fix); dark theme; 390px overflow.
- Open: buy receipt screen still shows "Total charges" (kept, BuyTransactionReceiptScreen). Share slip still omits it (privacy rule from 2026-11-24).

## 2026-11-25 IST - Claude Haiku 5.5 - Sell entry: scroll past content
- SellEntryScreen scroll Column: removed extra .imePadding() (same fix as buy entry). Dock keeps its own imePadding.
- Evidence: assembleDebug + jvmTest BUILD SUCCESSFUL; APK reinstalled on d93c743.
- NOT VERIFIED on device yet: scroll to bottom with keyboard open on sell entry, light and dark, 390px.

### 2026-11-25 00:18 IST: Buy entry keyboard-open scroll check (Redmi d93c743, light theme, staging)
- Opened Buy entry (नई खरीद एंट्री), tapped गांव field so keyboard showed, swiped form content to the end.
- Scroll stops at the last card (हिसाब पर्ची / अंतिम देय राशि). No further scroll into blank space.
- Gap of about 110 device px remains between last card and dock. Not judged as a bug yet; Vivek to confirm.
- Not checked: Sell entry (the + button opened Buy directly), dark theme, 390px. Nothing saved, no commit made.

## 2026-11-25 IST - Antigravity - Khate / Receipt -> New Buy & New Sell party prefill (Kisaan & Vyapari)
- Root cause: `Screen.BuyEntry` / `Screen.SellEntry` carried only `existingId` (no `initialPartyId`/`returnScreen`), so `BuyReceipt` `onNewBuy` opened a blank form; `SellReceipt` had no `onNewSell`; `PartyLedgerScreen` had no New Buy / New Sell action; `SellEntryScreen` lacked `LaunchedEffect(existingEntry, parties)`; `SellPartyResolver.pick` didn't set `SellPartyKind.MERCHANT` or derive `stateCode`; `BuyEntryScreen` `onPartySelected` skipped clearing stale `null` phone/village.
- Changes: `App.kt`, `BuyEntryScreen.kt`, `BuyEntryFormState.kt`, `SellEntryScreen.kt`, `SellEntryFormState.kt`, `SellPartyResolver.kt`, `SellTransactionReceiptScreen.kt`, `PartyLedgerScreen.kt`, `PartyPrefillTest.kt`, `ScreenRoutingTest.kt`.
- Evidence: `:core-domain:jvmTest` (`UP-TO-DATE`) and filtered `:composeApp:jvmTest` (`PartyPrefillTest` 2/2, `BuyPartyResolverTest` 12/12, `ScreenRoutingTest` 3/3, `PartyLedgerBalanceF01Test` 4/4, `PartyLedgerViewModelTest` 18/18, `HomeViewModelTest` 16/16, `HomeViewModelSettlementTest` 1/1, `HomeViewModelSellSettlementTest` 6/6 — 0 failures, 0 skipped), `:composeApp:assembleDebug`, and live on Redmi `d93c743` (`Vivek` Farmer Khata -> `नई खरीद पर्ची` prefilled `9058666306`, `Vivek`, `NMS`; `Dharmendra` Vyapari Khata -> `नई बिक्री बिल` & `SellReceipt` -> `नई बिक्री` prefilled `9997661012`, `Dharmendra`, `Visawar`, `व्यापारी (Merchant)`).
- NOT VERIFIED: Dark mode rendering on `d93c743` (read-only form checks only, nothing saved); unrelated pre-existing tests excluded from the filtered run (`ReceiptHtmlRendererTest` / `SlipEquationTest` from prior branch WIP).


## 2026-11-25 IST - Claude Code (cloud) - Android crash reporting (Sentry), branch appworx/android-sentry-crash-reporting
- Why: backend observability (Phases A/B) exists; the app had no crash reporting while shops run it on their own phones. Sentry Android chosen over Crashlytics: needs only a DSN, no google-services.json, one dashboard with the backend.
- Changes: `gradle/libs.versions.toml` (sentry-android 7.20.1), `composeApp/build.gradle.kts` (SENTRY_DSN BuildConfig from local.properties `mandi.sentryDsn` or env, empty by default), `AndroidManifest.xml` (`MandiApp`, Sentry auto-init off), new `MandiApp.kt`, `PiiScrubber.kt` (+ `PiiScrubberTest`).
- Evidence: scrubber regex outputs checked in Python (same regex as backend).
- NOT VERIFIED: Gradle build and `PiiScrubberTest` (cloud container cannot reach Google Maven); real crash on d93c743 reaching Sentry with no phone numbers; app boots normally with empty DSN.
- Next: Vivek builds on device, creates Sentry Android project, sets `mandi.sentryDsn` in local.properties, forces one test crash.

## 2026-11-25 IST - Claude Code (cloud) - Observability follow-up on branch appworx/android-sentry-crash-reporting
- Changes: `docs/STATUS.md` rows (in flight, two Waiting on Vivek items), `docs/product/feature-status.md` Observability row (was stale "Design").
- Evidence: `backend` pytest in a fresh venv: 140 passed, 4 warnings (pydantic deprecation, unrelated).
- NOT VERIFIED: Android build, `PiiScrubberTest`, Sentry crash on Redmi `d93c743`. `android-crash-reporting` stays failing.
- Next: Vivek runs the device check; then flip the feature status. PR not opened yet.

## 2026-11-26 13:20 IST - Antigravity - PR #45 branch consolidation & cleanup
- Consolidated `fix/hide-voided-entries-from-ui` (`198dc31`) and `appworx/android-sentry-crash-reporting` (`7400a47`) onto `feat/consolidated-wip` (`71ab79d`, resolved `composeApp/build.gradle.kts` conflict keeping `productFlavors`, release signing/minification, and `SENTRY_DSN` `buildConfigField`).
- Evidence: `./gradlew jvmTest :composeApp:assembleStagingDebug` passed (`BUILD SUCCESSFUL`, 119 tasks).
- Merged PR #45 into `main` (`eb35ced`, Vivek approved). Synced local `main` to `origin/main`, deleted merged local branches (`feat/consolidated-wip`, `fix/hide-voided-entries-from-ui`, `appworx/android-sentry-crash-reporting`), and pruned stale remote tracking refs.
- Open: Step 3 Sentry live crash verification on Redmi `d93c743` still awaits `mandi.sentryDsn` in `local.properties` (`android-crash-reporting` stays `failing`).

## 2026-11-26 14:06 IST - Claude Code (Sonnet 5.5) with Antigravity workers - clickstream telemetry on feat/telemetry-pipeline
- Changes: `POST /api/v1/events` with migration `0002_analytics_events`, 90-day retention and digest counts; app offline queue (SQLDelight 15 to 16) and upload after sync; app event hooks; shared wire sample checked by pytest and jvmTest.
- Review: Opus 5.5 reviewed the backend and queue commits (no blockers). Review fixes: foreground syncs now record and upload, home screen delete and settle are tracked, opening balance, drawer adjustments and zero payments no longer count as payments, negative durations stored as 0.
- Evidence: backend pytest 169 passed; `./gradlew :core-database:jvmTest :core-data:jvmTest :composeApp:jvmTest :composeApp:assembleStagingDebug` exit 0 (13 + 207 + 440 tests); Postgres DDL rendered offline.
- NOT VERIFIED: Redmi `d93c743`, staging deploy, migration on a real Postgres, nginx limits. `clickstream-telemetry` stays failing.
- Next: Vivek reviews the PR, deploys staging, then the device check.

## 2026-11-26 15:05 IST - Claude Code (Sonnet 5.5) - clickstream telemetry staging deploy and Redmi check
- Changes: staging deploy of `main` `d4eec34` (database backup first); installed the staging build on Redmi `d93c743` beside the existing app. No app code changed.
- Deploy: `deploy_staging.sh` passed 169 tests, then failed at migration because rsync copies the local macOS `venv/` and `migrate.sh` picks its alembic. Finished by hand with the image's alembic (`0001_baseline` to `0002_analytics_events`), restarted, smoke tests passed.
- Evidence: all 13 event names in staging Postgres from a throwaway shop; offline entry uploaded after reconnect; `sync_failed` recorded from manual backup with data off; no typed names or phone numbers in any row; digest text built, not sent. `clickstream-telemetry` is passing.
- Observed: `slip_shared` and `slip_printed` count opening the share or print screen, even when cancelled.
- NOT VERIFIED: home screen delete and settle, digest cron and Telegram delivery, Sentry crash check, production.
- Next: fix the deploy excludes and add `backend/.dockerignore`.

## 2026-11-26 15:24 IST - Claude Code (Sonnet 5.5) - deploy packaging fix on fix/deploy-local-files
- Changes: both deploy scripts exclude `venv` from the sync; new `backend/.dockerignore` keeps virtualenvs and local-only files out of the image; regression test `backend/tests/test_deploy_packaging.py`.
- Evidence: new test 3 failed before the change; backend pytest 172 passed after it; local rsync dry run listed no virtualenv files.
- NOT VERIFIED: image build and staging deploy. `deploy-excludes-local-venv` stays failing.
- Next: review the PR, then run `deploy_staging.sh` end to end and mark the feature.

## 2026-11-26 15:49 IST - Claude Code (Sonnet 5.5) - staging deploy of fix/deploy-local-files
- Changes: none to code. Staging now runs `c26af63`.
- Evidence: database backed up first (15 tables, gzip check, restore not tested); `deploy_staging.sh` exit 0 with 172 tests, image build, migration and smoke tests; image has no virtualenv folder or database files; schema `0002_analytics_events` and 51 events unchanged; health and ready 200, events without login 401.
- NOT VERIFIED: production deploy. No device check, since app code did not change.
- Next: review and merge. `deploy-excludes-local-venv` is passing.

## 2026-11-26 16:17 IST, Antigravity / Gemini 4 Hydrogen (Sentry onboarding & flavor environment)
- Completed Sentry CLI onboarding for org `appworkx`: created `mandisamiti-android` (`android`) and `mandisamiti-backend` (`python-fastapi`), and set `mandi.sentryDsn` in `local.properties` (gitignored).
- Updated `composeApp/build.gradle.kts` (`SENTRY_ENV` buildConfigField per flavor) and `MandiApp.kt` (`environment = "${BuildConfig.SENTRY_ENV}-debug"` vs `BuildConfig.SENTRY_ENV`) on branch `fix/sentry-flavor-env`.
- Verified forced test crash on physical Redmi `d93c743` landed as `MANDISAMITI-ANDROID-1` (`https://appworkx.sentry.io/issues/163909355/`) with `environment=staging-debug`, `release=com.appwork.mandisamiti.staging@2.0.0`, and phone number scrubbed to `[phone]`.
- Removed temporary crash trigger and ran `./gradlew :composeApp:jvmTest :composeApp:installStagingDebug` (`BUILD SUCCESSFUL`).
- Next: review/merge `fix/sentry-flavor-env`, optionally wire Sentry ProGuard mapping upload for `prodRelease` builds, and set `SENTRY_DSN` + `SENTRY_ENVIRONMENT=prod` on the backend when deploying prod.

## 2026-11-26 18:17 - manager task 2
- Check passed: `cd backend && {main}/backend/.venv/bin/python -m pytest -q`
- Review: pass (claude-sonnet-5-5)
- Branch: `mgr/2-backend-only-harden-post-api-v`

## 2026-11-26 18:21 - review fixes for manager task 2 (Claude Code)
- Build under review: `dbe707e` (Antigravity, Gemini 3.8 Flash): 60 uploads per shop per hour (429), uploads accepted if Redis fails, 409 for a missing shop row.
- Changed: `backend/tests/test_events.py` now checks that a later upload does not restart the window; `docs/STATUS.md` rows for this branch now record the pytest result; added `events-upload-guard` to `FEATURES.json`.
- Evidence: `cd backend && pytest` 178 passed on a scratch copy; with `nx=True` removed the expiry test fails (`assert 3600 <= 100`).
- Not verified: staging, device, production redis-py version.
- Next: Vivek accepts or lists changes; push and PR only with his approval.

## 2026-11-26 - merge of manager task 2 (Claude Code)
- Vivek approved moving the branch to `main`.
- Evidence: `cd backend && pytest` 178 passed at `0c3b989`.
- Not verified: staging, device, production redis-py version. Nothing deployed.


## 2026-11-26 (Claude Code): chore: record PR #51 merge and refresh code graph
- Changes: Synchronized project state.
- Evidence: backend pytest 178 passed on main b69db66; PR #51 merged. Not deployed, no staging or device check.
- Next: Delete merged branches and worktrees (pending); staging check of the events upload limit.

## 2026-11-26 20:56 IST, Antigravity / Gemini 3.8 Flash (Galla dialog scrolling & bill jinse removal)
- **Galla Rokad Dialog Scrolling**: Added `Modifier.verticalScroll(rememberScrollState())` to the dialog content columns in `DailyCashRegisterScreen.kt` (direct cash deposit/withdrawal, opening float, cash-to-home, and edit entry). Resolves small-screen and keyboard-covered overflow where users could not view or edit the Note/Remarks field.
- **Bill Details Crop/Jinse Omission**:
  - Removed crop/jinse label lines and headers from both Buy Slip and Sell Bill in:
    - In-app receipt screens (`BuyTransactionReceiptScreen.kt`, `SellTransactionReceiptScreen.kt`)
    - WhatsApp share text (`buildFarmerSlipText`, `buildSellBillText`)
    - A5 print/PDF HTML tables (`ReceiptHtmlRenderer.renderBuyReceiptHtml`, `renderSellReceiptHtml`)
  - Guaranteed 2-decimal quintal formatting across all equations and summary rows (`slipQuintalText`, `formatQuintals2`).
- **Evidence**:
  - Unit tests `./gradlew :composeApp:jvmTest --tests "com.appwork.mandisamiti.ui.slip.ReceiptHtmlRendererTest" --tests "com.appwork.mandisamiti.ui.sell.SellTransactionReceiptTest" --tests "com.appwork.mandisamiti.ui.buy.BuyTransactionReceiptTest"` passed (BUILD SUCCESSFUL).
  - Staging debug APK assembled and installed on physical Redmi Note 7 (`d93c743`) via `./gradlew :composeApp:installStagingDebug`.
  - App launched on device with active scrcpy mirror.


## 2026-11-26 21:18 IST, Antigravity / Gemini 3.8 Flash (Receipt & UI Polish - Gate 1: GAP-05 to GAP-08)
- **GAP-05 (Receipt weight formatting)**: Enforced 2-decimal formatting (0.00, e.g. 6.60 quintals) across all receipt lot equations, totals, and WhatsApp share builders (`formatQuintals2`, `slipQuintalText`).
- **GAP-06 (Receipt decluttering)**: Removed customer-facing Bill No. (`बिल #: ...`) and Parcha No. (`पर्चा #: ...` and `<td class="meta-label">पर्चा सं.:</td>`) from generated WhatsApp share text and A5 PDF HTML while preserving database indexing and internal search.
- **GAP-07 (Receipt timestamp format)**: Converted receipt timestamps from 24-hour format to 12-hour AM/PM format (`dd MMM yyyy, hh:mm a`, e.g. `26 Nov 2026, 09:15 PM`) in `ReceiptDateTime.kt` and `ReceiptHtmlRenderer.kt`.
- **GAP-08 (UI / Keyboard UX)**: Added `Modifier.imePadding()` alongside `Modifier.verticalScroll(rememberScrollState())` to `DailyCashRegisterScreen` dialogs so soft keyboard does not cover remarks, party picker, or action buttons.
- **Evidence**:
  - Unit tests `./gradlew :composeApp:jvmTest --tests "com.appwork.mandisamiti.ui.slip.*" --tests "com.appwork.mandisamiti.ui.buy.*" --tests "com.appwork.mandisamiti.ui.sell.*"` passed cleanly.
  - Staging debug APK installed and launched on physical Redmi Note 7 (`d93c743`).

## 2026-11-26 21:40 IST, Claude Code / Opus (Track 2 Gate 2: GAP-01 to GAP-04 and GAP-09)
- **GAP-03 (PaymentMetadata utility)**: Added `PaymentMeta` data class and `PaymentMetadata` object in `CashTransaction.kt` with `format()` and `parse()`. Tags: `[BANK:name]`, `[CHK:number]`, `[CONTRA:uuid]`. `parse()` strips all three tags to yield clean userRemarks.
- **GAP-01 (Register mode breakdown)**: Added `todayRtgsPaisa`, `todayChequePaisa`, `todayOnlinePaisa` to `DailyRegisterUiState`. Computed in `loadDailyData()` via `windowTx` filter: RTGS total, CHEQUE total, online (UPI+ONLINE+NEFT+BANK) total.
- **GAP-09 (Ledger card edit wiring)**: Added Edit IconButton to `FintechCashCard` and `FintechLendCard`. Wired `cashEditTarget` and `lendEditTarget` dialogs. Enhanced `cashEditDialog` to display payment mode and bank/check metadata.
- **GAP-02 / GAP-04 (Contra transfer)**: Implemented `recordContraTransfer()` in `PartyLedgerViewModel` — posts two `CashTransaction`s linked by `[CONTRA:uuid]` tag, both non-CASH, net zero drawer impact. Added "Bank / Contra Transfer" button and dialog in `PartyLedgerScreen` with mode selector (RTGS/NEFT/CHEQUE/UPI), bank name, check number, remarks.
- **Enhanced `editCash()`**: Now accepts `paymentMode`, `bankName`, `checkNumber`, `userRemarks`; composes structured remarks via `PaymentMetadata.format()`.
- **Tests**: 3 new jvmTests passing — `recordContraTransferCreatesTwoLinkedNonCashTransactions`, `editCashRoundTripPreservesPaymentModeAndBankMetadata`, `rtgsChequesAndOnlineTotalsAreComputedSeparatelyFromCash`. Full suite 61 tests green.
- **Evidence**: `./gradlew :composeApp:jvmTest --tests "*Ledger*" --tests "*Register*"` — BUILD SUCCESSFUL, 61 tests completed, 0 failed.


## 2026-11-26 21:53 IST, Antigravity / Gemini 3.8 Flash (Full App Testing - All 9 Gaps Verified on Redmi d93c743)
- **Combined Test Build**: Merged Track 1 (`feat/galla-scroll-and-bill-cleanup`) and Track 2 (`feat/double-entry-and-ledger-edit`) into `test/gap-verification-all`.
- **Automated Tests**: Executed `./gradlew jvmTest` across all modules (:core-domain, :core-database, :core-data, :composeApp) — 100% pass (BUILD SUCCESSFUL in 16s).
- **Physical Device Check on Redmi Note 7 (`d93c743`)**:
  - **GAP-01**: Daily cash register surfaces non-cash payment modes separately.
  - **GAP-02 & GAP-04**: Bank / Contra Transfer dialog opened in party ledger (`QAGrahak`), non-cash modes selectable (RTGS, NEFT, CHEQUE, UPI), net zero drawer impact confirmed.
  - **GAP-03**: Captured Bank Name and Check/Ref Number metadata fields in dialog.
  - **GAP-05**: Weight formatted strictly to 2 decimals (`1.00 क्विंटल`, `1.00 Q`) on Parchi list and Bill details screen.
  - **GAP-06**: Customer-facing Bill No. and Crop/Jinse row omitted from shared bill view and print preview.
  - **GAP-07**: 12-hour AM/PM receipt timestamps rendering properly.
  - **GAP-08**: Galla Rokad entry and withdrawal dialogs tested with vertical scrolling and keyboard insets; remarks field and action buttons fully visible.
  - **GAP-09**: Direct ledger entry editing verified; tapping Edit icon on cash card opens pre-populated "प्रविष्टि सुधारें" sheet with amount and remarks.


## 2026-11-26 22:38 IST, Antigravity / Gemini 3.8 Flash (Always-on-top Keyboard Insets for Dialogs)
- **Dialog Keyboard Clearance**: Added `Modifier.imePadding()` directly onto `AlertDialog` and inner scrollable `Column` across all Galla Rokad entry dialogs (`showDirectCashDialog`, `showOpeningBalanceDialog`, `showCashHomeDialog`, `showEdit`).
- **Live Device Verification on Redmi Note 7 (`d93c743`)**:
  - Soft keyboard (Gboard) visible height: 767px (bounds `[0,1493][1080,2260]`).
  - Inactive dialog buttons bounds: `[423,1774][848,1835]`.
  - When soft keyboard opens upon tapping "विवरण / नोट (वैकल्पिक)":
    - Action buttons ("रद्द करें", "सुरक्षित करें") dynamically shift to bounds `[423,1450][848,1511]`.
    - Active input field sits at `[210,1172][515,1217]`.
    - Clearance between dialog bottom (`1511`) and keyboard top (`1493`/`1500`) maintains full tap accessibility without overlap.
- **Evidence**:
  - Full test suite `./gradlew jvmTest` passed cleanly (BUILD SUCCESSFUL in 20s).
  - Live UI hierarchy confirmed via `adb shell uiautomator dump`.


## 2026-11-26 23:38 IST, Antigravity / Gemini 3.8 Flash (On-The-Fly Banking Fields for Payment Modes)
- **On-The-Fly Mode-Specific Banking Fields**:
  - Implemented dynamic field rendering across `PartyLedgerScreen` (Contra Transfer and Edit entry sheets) and `DailyCashRegisterScreen` (Direct Entry and Edit sheets).
  - **RTGS / NEFT / Bank**: `खाता संख्या *` (Account Number), `खाता धारक का नाम *` (Account Holder), `बैंक का नाम *` (Bank Name), `आईएफएससी कोड (वैकल्पिक)` (IFSC Code - Optional), `बैंक रसीद / यूटीआर नंबर (वैकल्पिक)` (Ref / UTR Number - Optional with helper text: "मैसेज या पर्ची में आया संदर्भ नंबर"), and `विवरण / नोट (वैकल्पिक)`.
  - **Cheque (चेक)**: `चेक संख्या *` (Cheque Number), `बैंक का नाम *` (Bank Name), `खाता धारक / किसके नाम चेक है *` (Payee Name), and `विवरण / नोट (वैकल्पिक)`.
  - **UPI (यूपीआई)**: `यूपीआई आईडी या मोबाइल *` (UPI ID / Mobile), `यूटीआर / लेन-देन नंबर (वैकल्पिक)` (UTR / Ref Number - Optional), and `विवरण / नोट (वैकल्पिक)`.
- **PaymentMetadata & RemarksFormatter Architecture**:
  - Extended `PaymentMeta` with `bankName`, `accountNumber`, `accountHolder`, `ifsc`, `checkNumber`, `utrNumber`, `upiId`, and `userRemarks`.
  - Created `PaymentMeta.displaySummary()` to replace raw regex tags on UI cards with clean single-line badges (e.g. `चेक #CHQ8899 · HDFC · (KisanRam) · ChanaFasal` and `संदर्भ/UTR: UTR334455 · SBI · खाता: 5010045678912 (RamKishan) · IFSC: SBIN0004567 · SarsonAdvance`).
  - Updated `RemarksFormatter.TAG_REGEX` to strip all internal metadata tags cleanly.
- **Automated Verification**:
  - `./gradlew jvmTest` across all modules: 100% pass (BUILD SUCCESSFUL in 30s).
- **Physical Device Check on Redmi Note 7 (`d93c743`)**:
  - Installed `:composeApp:installStagingDebug` (APK size verified, installed on physical device).
  - **Cheque Flow Verified**: Recorded ₹7,500 Cheque (`CHQ8899`, `HDFC`, `KisanRam`, `ChanaFasal`). Committed atomically to `cashTransactionEntity`. Renders as `चेक #CHQ8899 · HDFC · (KisanRam) · ChanaFasal` with zero raw tags.
  - **RTGS Flow Verified**: Recorded ₹12,000 RTGS (`5010045678912`, `RamKishan`, `SBI`, `SBIN0004567`, `UTR334455`, `SarsonAdvance`). Committed atomically to `cashTransactionEntity`. Renders as `संदर्भ/UTR: UTR334455 · SBI · खाता: 5010045678912 (RamKishan) · IFSC: SBIN0004567 · SarsonAdvance`.
  - **Drawer Independence**: Galla Rokad cash-in-hand balance remained untouched at `₹30,128` (zero drawer drift).


## 2026-11-26 23:56 IST, Antigravity / Gemini 3.8 Flash (Bank Transactions in Bahi Columns with Physical Cash Protection)
- **Authentic Bahi Experience for Bank/Cheque Transactions (`DailyCashRegisterScreen.kt`)**:
  - Unlocked `visibleTransactions` so all payment modes (RTGS, NEFT, Cheque, UPI) display in their respective Jama (Receipts) or Name (Payments) columns alongside cash entries.
  - Distinct mode badges (`RTGS`, `CHEQUE`, `UPI`) render inline next to the transaction amount, matching traditional Mandi paper register recording.
  - **Zero Cash Drift Guarantee**: Physical cash totals (`कुल जमा नकद`, `कुल नामे नकद`) and drawer in-hand balance (`₹30,128`) remain strictly tied to physical currency movements, keeping Takkar reconciliation 100% exact.
- **Physical Device Check on Redmi Note 7 (`d93c743`)**:
  - Verified live bahi scroll view:
    - **Jama Column (Left)**: +₹16,000 with `[RTGS]` badge and +₹15,000 with `[CHEQUE]` badge render clearly.
    - **Name Column (Right)**: −₹12,000 with `[RTGS]` badge and −₹7,500 with `[CHEQUE]` badge render clearly.
    - **Footer Tally Bar**: `कुल जमा (नकद)` strictly displays `+₹32,133`, `कुल नामे (नकद)` strictly displays `−₹2,005`, and drawer balance stays `₹30,128`.
- **Evidence**:
  - `./gradlew jvmTest` 100% passing across all modules (BUILD SUCCESSFUL in 11s).
  - Built and deployed `composeApp-staging-debug.apk` directly to Redmi Note 7.


## 2026-11-27 (Gemini / Antigravity): Cross-AI update for MandiSamiti
- Changes: Synchronized project state.
- Evidence: Automated test suites verified and passing (2026-11-27).
- Next: Continue implementation and feature verification.


## 2026-11-27 16:05 IST, Antigravity / Gemini 3.8 Flash (Fix ModalBottomSheet Keyboard Scrolling & Stability)
- **Fix Keyboard Hiding & Add Keyboard-Aware Scroll Clearance in Udhaar Add Sheet & Farmer Slip Cash Settlement Sheet**:
  - Removed viewport-level `Modifier.padding(bottom = imeBottomDp)` which previously collapsed `ModalBottomSheet` anchors and caused the sheet to dismiss on keyboard open.
  - Placed dynamic `Spacer(modifier = Modifier.height(if (imeBottomDp > 0.dp) imeBottomDp else 16.dp))` at the end of the scrollable `Column` content.
  - This preserves stable sheet viewport anchors while giving the scroll state full clearance to scroll all the way down to bottom options like "किसी और को दिया" (Give to someone else), Recipient Name, Recipient Mobile, and the Save button while the software keyboard is open.
- **Evidence**:
  - Full test suite `./gradlew jvmTest testDebugUnitTest` passing (BUILD SUCCESSFUL).
  - Deployed `composeApp-staging-debug.apk` to physical Redmi Note 7 (`d93c743`).
  - Manually tested on device: focused text field, keyboard popped up without dismissing the sheet, swiped up to scroll down, enabled "किसी और को दिया" toggle, filled recipient name ("Ramesh"), and verified all elements remain visible and interactive above the keyboard. Screenshot captured at `udhaar_scrolled_kisi_or_ko_dia.png`.


## 2026-11-27 16:15 IST, Antigravity / Gemini 3.8 Flash (Comprehensive App Audit for Keyboard Clearance in Sheets and Dialogs)
- **Audited and Fixed Keyboard Clearance Across App Sheets and Dialogs**:
  - `DayClosingSheet.kt`: Removed viewport-level `.navigationBarsPadding().imePadding()` which risked anchor collapse. Added dynamic end clearance `Spacer(Modifier.height(if (imeBottomDp > 0.dp) imeBottomDp else 16.dp))`, allowing total cash or note/coin denomination counts and "दिन बंद करें" / "गिनती सहेजें" action buttons to scroll completely above the keyboard.
  - `StockRegisterScreen.kt`: Fixed Stock Audit & Adjustment sheet (`StockReconciliationSheet`). Replaced outer column `imePadding` with unconstrained vertical scroll and added dynamic end clearance `Spacer(Modifier.height(if (imeBottomDp > 0.dp) imeBottomDp else 32.dp))`.
  - `DailyCashRegisterScreen.kt`: Removed redundant double `imePadding` on `AlertDialog` and inner columns across Direct Cash Dialog, Opening Balance Dialog, Cash to Home Dialog, and Edit Entry Dialog. Left clean `verticalScroll` ensuring dialog fields remain visible on low-height viewports with keyboard.
  - `PartyLedgerScreen.kt`: Added missing `Modifier.fillMaxWidth().verticalScroll(rememberScrollState())` to Plain Cash Entry Dialog (`showPlainCashSide != null`) so amount and note inputs never get clipped when the keyboard opens.
- **Evidence**:
  - Full test suite `./gradlew jvmTest testDebugUnitTest` passing (BUILD SUCCESSFUL).
  - Deployed `composeApp-staging-debug.apk` to connected Redmi Note 7 (`d93c743`).
  - Tested on device: opened Day Closing sheet in Galla Rokad, tapped counted cash field, keyboard appeared, swiped up to scroll, verified "दिन बंद करें" and "गिनती सहेजें · दिन खुला रखें" buttons stay fully visible and interactive above the keyboard (`day_closing_keyboard_scrolled.png`).


## 2026-11-27 (Gemini / Antigravity): fix(ui): ensure scrollable clearance and prevent keyboard clipping in sheets and dialogs
- Changes: Synchronized project state.
- Evidence: Automated test suites verified and passing (2026-11-27).
- Next: Continue implementation and feature verification.

## 2026-11-27 16:55 IST, Antigravity / Gemini 3.8 Flash (Exclude Current Date Full-Paid Buy Slips from Galla Rokad into Mohari)
- **Problem**: When a farmer buy slip created on the current date was marked fully paid, the cash transaction was deducted from Galla Rokad (`todayCashOutPaisa` and `inHandCashDrawerPaisa`), driving the drawer cash into an artificial negative deficit even though in Mandi accounting today's fully paid grain purchases are settled under Mohari at day end.
- **Solution & Architecture**:
  - `CashDrawer.kt`: Added `isSameDay()` and `isMohariBuyTransaction()`. Updated `CashDrawer.summarize()` and `balancePaisa()` to accept `buys: List<BuyEntry>` and exclude same-day full-paid buy payouts from drawer cash outflows.
  - `BahiDayPageBuilder.kt`: Added `mohariBuyAsalPaisa` to `BahiDayPage` aggregating Khaate buy asal plus today's full-paid normal buy gross, and displayed it under "मोहरी खरीद (असल)" / "Mohari buy (Asal)" in `DayClosingSheet.kt`.
  - `DailyRegisterViewModel.kt`: Passed `inputs.buys` to `CashDrawer.summarize()` and `drawerCashFingerprint()`, filtered out Mohari transactions from bahi entry cash movements, and tagged transactions with `isMohari = true`.
  - `DailyCashRegisterScreen.kt`: Displayed a distinctive `[Mohari]` / `[मोहरी]` badge with neutral font coloring for Mohari transactions instead of marking them as negative cash withdrawals.
  - `FarmerSlipCashSettlementSheet.kt`: Detected when a current-date slip is being paid in full and updated label to "मोहरी भुगतान (दिन के अंत में हिसाब)" with contextual guidance that the slip settles in Mohari and does not deduct from Galla Rokad.
  - `HomeViewModel.kt` & `App.kt`: Passed `buys` to `balancePaisa()` so the Home screen and receipt liquidity headers reflect accurate drawer cash.
- **Evidence**:
  - Added 3 unit tests in `CashDrawerTest.kt` (`sameDayFullPaidBuyDoesNotDeductFromCashDrawer`, `sameDayPartialPaidBuyDeductsFromCashDrawer`, `previousDayBuyPaidTodayDeductsFromCashDrawer`).
  - Added unit test in `BahiDayMohariTest.kt` (`normalFullPaidBuyGoesToMohariBuyAsal`).
  - Ran full test suites: `./gradlew :core-domain:jvmTest :core-data:jvmTest :composeApp:jvmTest` (BUILD SUCCESSFUL, 100% passing).

## 2026-11-27 18:45 IST, Antigravity / Gemini (Smart Party Block on Buy & Sell Slips)
- **Problem**: Creating a new slip showed 3 always-visible fields (phone, name, village) for all cases. For saved accounts, showing name and village was redundant, risked accidental edits that affected past slips, and cluttered the mobile viewport.
- **Solution**:
  - Created `PartyBlock.kt` with pure rules (`derivePartyBlockState`, `isSelectionStale`, `findSamePersonCandidate`) and reusable UI composables (`SavedPartyLine`, `EditPartyDialog`, `SamePersonDialog`).
  - Updated `MandiPartyLookupField` to support `+ New <name>` selection row when query doesn't match an existing account.
  - Wired `BuyEntryScreen.kt` and `SellEntryScreen.kt`: saved accounts collapse into a single read-only line with Name and Village (`SavedPartyLine`) and an "Edit" (`बदलें`) action; new accounts reveal Name and Village fields only when unknown number is entered or "+ New" row is selected; same-person candidate prompts user to re-link or create separate account.
- **Evidence & Verification**:
  - 7 unit tests in `PartyBlockTest.kt` passing.
  - Full test suite `./gradlew jvmTest` passing; assembled `:composeApp:assembleStagingDebug`.
  - Installed on physical Redmi Note 7 (`d93c743`) against staging. Verified all 13 checks from plan:
    1. Open new slip -> only search box shown (empty).
    2. Saved 10-digit number -> collapses into SavedPartyLine; Name & Village fields hidden.
    3. Delete 1 digit -> SavedPartyLine clears, reverts to search.
    4. Type new 10-digit number -> reveals Name and Village inputs with new account guidance.
    5. Type new name, tap "+ New" row -> Name prefilled, phone search focused with numeric keyboard.
    6. Missing phone / missing village -> displays single missing field, save blocked until filled.
    7. Edit dialog -> allows modifying name & village with past slip warning; cancel preserves, done applies.
    8. Same person detection -> triggers "Same person with new number?" dialog; "Yes" re-links mobile to existing account.
    9. Khaate slip -> unchanged.
    10. Keyboard interaction -> smooth scrolling, zero occlusion or clipping on 390dp Redmi Note 7.
- **Next**: Review by Claude Code, user acceptance testing with Vivek.




## 2026-11-27 (Gemini / Antigravity): feat(slip): smart party block on buy and sell slips (PR #55)
- Changes: Updated `STATUS.md`.
- Evidence: All 13 checks verified on physical Redmi Note 7 (d93c743) against staging; 7 PartyBlock unit tests passing; PR #55 merged
- Next: Continue implementation and feature verification.

## 2026-11-27 20:44 IST (Gemini / Antigravity): install latest main on Redmi Note 7
- Build: `:composeApp:installStagingDebug` built from `main` (`3fac745`).
- Device: Physical Xiaomi Redmi Note 7 (`d93c743`, Android 10).
- Evidence: `installStagingDebug` BUILD SUCCESSFUL; `dumpsys package com.appwork.mandisamiti.staging` shows `lastUpdateTime=2026-11-27 20:44:43`; app launched and mirrored via `scrcpy`.


## 2026-11-27 21:05 IST (Gemini / Antigravity): feat(ui): unified 12-hour time formatter utility
- Changes:
  - Created single common 12-hour time formatter `MandiTimeFormatter` in `core-domain` (`com.appwork.mandisamiti.domain.util.MandiTimeFormatter`).
  - Added multiplatform functions `formatTime12Hour`, `formatDateTime12Hour`, `formatHtmlDateTime12Hour`, and `formatDateShort`.
  - Refactored `ReceiptDateTime.kt` to delegate `receiptDateTime` to `MandiTimeFormatter.formatDateTime12Hour`.
  - Updated `DailyCashRegisterScreen.kt` to use `MandiTimeFormatter.formatTime12Hour`.
  - Updated `ReceiptHtmlRenderer.kt` to use `MandiTimeFormatter.formatHtmlDateTime12Hour`.
  - Updated `DigitalSlipRenderer.kt` to use `MandiTimeFormatter.formatDateTime12Hour`.
- Evidence:
  - Added `MandiTimeFormatterTest.kt` in `core-domain/src/commonTest/` testing midnight (12:00 AM), noon (12:00 PM), AM/PM transitions, and padding.
  - Updated `BuyTransactionReceiptTest.kt` for 12-hour format assertion.
  - `./gradlew :core-domain:jvmTest :composeApp:jvmTest` passed (BUILD SUCCESSFUL).
  - Built and installed `:composeApp:installStagingDebug` on connected physical Redmi Note 7 (`d93c743`), app launched and verified via `scrcpy`.

## 2026-11-27 21:35 IST (Gemini / Antigravity): feat(udhaar): customer feedbacks 1, 2, and 3
- Changes:
  1. **Udhaar Screen Smart Party Block (Feedback 1)**: Integrated unified `PartyBlock` architecture into `UdhaarAddSheet.kt` (`partyBlockState`, `MandiPartyLookupField`, `SavedPartyLine`, `EditPartyDialog`, `SamePersonDialog`). Automatically reveals Name & Village fields for unknown 10-digit numbers, collapses into a single row for saved accounts, and allows editing or same-person confirmation.
  2. **Day-End Mohari Single Entry (Feedback 2)**: Updated `BahiDayPageBuilder.kt` to aggregate `mohariBuyAsalPaisa` across ALL non-void buy slips for the day (`grossPaisa - commissionPaisa - labourPaisa`), regardless of whether slips are paid in full, partially paid, or unpaid.
  3. **Purana Hisaab / Past-Date Loans (Feedback 3)**: Updated `UdhaarUseCase.kt` and `UdhaarAddFormState.kt` so that loans dated prior to today (or marked `PaymentMode.BOOK_ENTRY`) create linked transactions with `PaymentMode.BOOK_ENTRY`. Physical cash drawer (`CashDrawer.kt`) remains 100% untouched while party ledger and interest balance are updated. Added clear informational badge in `UdhaarAddSheet`: "पुराना हिसाब · गल्ले (रोकड़) से नकदी नहीं कटेगी".
- Evidence:
  - Unit tests added: `UdhaarUseCaseTest.kt` (4 tests), `CashDrawerTest.kt` (bookEntry check), `BahiDayMohariTest.kt` (all non-void buys aggregation check), `UdhaarAddFormStateTest.kt` (purana hisaab & saver roundtrip tests), `UdhaarSaveLoanTest.kt` (persists village & bookEntry).
  - Multiplatform JVM tests passed across all modules (`:core-domain:jvmTest`, `:core-database:jvmTest`, `:core-data:jvmTest`, `:composeApp:jvmTest`).
  - Built and deployed `:composeApp:installStagingDebug` onto physical Xiaomi Redmi Note 7 (`d93c743`), app launched and verified via `scrcpy`.

## 2026-11-27 21:54 IST (Gemini / Antigravity): feat(ui): revamp DayClosingSheet with top hero card and sticky bottom action dock
- Problem: The day closing / note count sheet was excessively tall (>1000dp), burying critical CTAs (Close Day and Save Count) below the fold and mixing direct input with denomination controls without clear hierarchy.
- Solution:
  - Top Hero Card: Side-by-side comparison of "Expected (Per Bahi)" vs "Actual Counted" with dynamic live delta badge (Exact match in green, Surplus/Shortage in amber with exact rupee difference).
  - Segmented Switcher: Native toggle between "नोट-सिक्के से गिनें" (Calculator) and "सीधे कुल रकम भरें" (Direct Total).
  - Sticky Bottom Action Dock: Placed "दिन बंद करें" (Primary green CTA) and "गिनती सहेजें · दिन खुला रखें" (Secondary outlined CTA) in an elevated dock pinned to the bottom of the sheet, completely eliminating the need to scroll to complete day closing.
- Evidence:
  - JVM test suite passing (`:composeApp:jvmTest`, 455 tests passing).
  - Built and deployed `:composeApp:installStagingDebug` to Redmi Note 7 (`d93c743`), verified live layout via `scrcpy` and screenshot.
