# Observability: actions pending on Vivek

Last updated 2026-10-13. Spec: [observability design](../superpowers/specs/2026-10-13-mandisamiti-observability-design.md).

The code can't go live until the items below are done. Each one needs an account, a secret or a decision that only Vivek can provide. Secrets go into the server `backend/.env` only, never into git or chat.

## 1. Decisions (spec §7)

- [ ] **Ops bot.** A new Telegram bot, or reuse an existing bot with a separate chat? Recommendation: a new bot, so a leaked token can't touch the finance bot.
- [ ] **Sentry account.** Which email or org owns the Sentry project.
- [ ] **Off-site backup location.** Where the daily `pg_dump` lands (path on the VPS, or a bucket). The backup check in Phase B reads from there. If no off-site backup exists yet, that is a pilot-plan §2 gap to close first.
- [ ] **Android crash reporting.** Crashlytics or Sentry Android before the pilot (out of scope in the spec, but shops run the app on their own phones).

## 2. Accounts and secrets (Phase A)

- [ ] **Telegram ops bot.** Create it with @BotFather, add it to the ops chat, send it one message, then read the chat id from `https://api.telegram.org/bot<TOKEN>/getUpdates`. Set `OPS_TELEGRAM_BOT_TOKEN` and `OPS_TELEGRAM_CHAT_ID` in the server `.env`.
- [ ] **Sentry.** Create a FastAPI project on the free tier. Set `SENTRY_DSN` (and `SENTRY_ENVIRONMENT=prod`) in the server `.env`. In project settings, keep "Send default PII" off.
- [ ] **UptimeRobot.** Add an HTTP(S) monitor for `https://mandi-api.appworx.co.in/health/ready`, every 5 minutes, and attach a Telegram alert contact. This works only after the deploy below, because `/health/ready` is new.

## 3. Ship it

- [ ] **Approve the commit.** Phase A and the Phase B digest are uncommitted in `backend/` (87 tests passing). Say "commit" and it goes in with the spec.
- [ ] **Deploy.** Run `/project-deploy` (or `backend/scripts/deploy_prod.sh`) after the `.env` values are set. `pip install` picks up `sentry-sdk[fastapi]`.
- [ ] **Check it live.**
  - `curl https://mandi-api.appworx.co.in/health/ready` returns 200 with `db` and `redis` ok.
  - `docker compose logs api` shows JSON lines with `request_id`.
  - Any response carries an `X-Request-ID` header.
  - Stop Redis for a minute: UptimeRobot alerts within about 10 minutes, then clears.

## 4. Phase B (after the code lands)

- [ ] **Cron for the daily digest.** After deploy, add this line to the VPS crontab (server clock in UTC, so 15:30 = 21:00 IST):
  `30 15 * * * cd /opt/mandisamiti-prod && docker compose -f docker-compose.prod.yml exec -T api python -m scripts.ops_digest`
- [ ] **Backup path for the digest.** Once §1 settles where dumps land, mount that folder into the `api` container and set `BACKUP_DIR` in `.env`. Until then the digest reports "Backup: not configured".

## Done without waiting on Vivek

- `backend/scripts/ops_digest.py` with 7 tests: active shops today with entry counts, shops silent for 2+ days, sync conflicts in the last 24h, backup freshness (under 26h) and size (no drop over 10%).
- Left out on purpose: sync lag, because `deals` and `cash_transactions` have no server-side received timestamp. Adding one needs a migration; worth doing only if the pilot shows sync complaints. Error count, because Sentry already has it.
