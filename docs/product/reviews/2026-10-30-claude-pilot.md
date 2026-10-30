# Claude pilot handoff (S1 to S3, K2 fix), 2026-10-30

`main` = `2d5ddf7` (pushed). Plan with row status: `prompts/mandisamiti-pilot-tasks.md`.

## Done
- **S1**: staging secrets moved to gitignored `/opt/mandisamiti-staging/.env.staging` on the VPS (mode 600). New DB password, JWT secret and private 8-digit static OTP. Public `123456` is dead on staging. `backend/.env.staging.example` has names only. `__pycache__` untracked. Old secrets are in public git history; rotated, history not rewritten.
- **S2**: K1 (client schema v7) and B1 (Alembic `0001_baseline`) reviewed and merged `--no-ff`.
- **S3**: staging DB schema reset (dump kept at `/root/mandi-backups/staging-pre-reset-20261030-111419.sql.gz` on the VPS), rebuilt by Alembic, deployed, smoke passed. New columns verified. `backend/scripts/backup_staging.sh` + nightly cron 02:30, 14-day keep. Restore test passed (14 tables, counts match).
- **K2 blockers fixed** (`fix/pilot-money`, `2d5ddf7`): `getPartyBalance` now subtracts principal only (legacy rows without split count in full); `DailyRegisterViewModel` shows principal and interest as two Left lines.

## Open
1. **Weekly Drive copy** (S3 step 4): needs Vivek's OK to install `rclone` on Mac (brew) and VPS, then `rclone authorize "drive"` (scope `drive.file`) on the Mac, token pasted to the VPS by Vivek (never in chat). Encrypt dump with `openssl aes-256-cbc -pbkdf2`, passphrase file on VPS (600), keep 4 copies, Telegram alert on failure. Weekly cron.
2. **Sync round trip with TEST party** (S3 step 6): needs a debug build on a phone. Do it with S4 or D1.
3. **S4**: pilot debug APK, install checklist, `KNOWN-ISSUES`. Wait for item 1 (or Vivek drops it for the pilot).
4. **D1** (Antigravity): device rehearsal on the Redmi after S4. Also check the day-page repayment shows two lines (principal, interest).
5. **Step 8** (Vivek): install at shop by cable, TEST-party smoke test, wipe test data.

## KNOWN-ISSUES to write in S4
- No phone allow-list on login (static OTP: anyone who knows the code and a phone number can log in).
- `idx_party_shop_phone` unique index: client migration fails on an old install with two parties sharing a phone.
- B1 migration test compares table and column names only, not types or indexes.
- `config.py` defaults `OTP_STATIC_CODE` to `123456` if the env var is unset; staging sets it, other envs must too.

## Gotchas
- Classifier blocked VPS `DROP SCHEMA` and `crontab` until Vivek approved; rule `Bash(ssh appworx-core-vps crontab:*)` is in `.claude/settings.local.json`.
- `timeout` does not exist in the sandbox shell; run Gradle without it.
- `backend/mandi_backend.db` is tracked and keeps changing locally; do not commit it.
- Leftover cleanup: worktrees `MandiSamiti-{claude,codex,bahi-claude,antigravity}` and merged `feat/*`, `fix/*`, `chore/*` branches (Vivek approves).
