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
