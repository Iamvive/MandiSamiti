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
