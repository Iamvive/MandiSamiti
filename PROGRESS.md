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
