# MandiSamiti Observability & Monitoring — Design

Date: 2026-10-13 · Status: approved direction, spec under review · Goal row: [feature-status.md](../../product/feature-status.md)

## 1. Decision

Use free or cheap tools for the pilot, and keep the app code vendor-neutral, so that moving to a bigger setup later means changing env config and where the data goes. The backend never imports a vendor-specific API except through one small module per concern.

Rules:

1. **Telemetry goes out through open standards.** Errors use the Sentry SDK protocol (GlitchTip and self-hosted Sentry accept the same DSN). Metrics and traces, when added, use OpenTelemetry with an OTLP exporter set by env.
2. **Logs are JSON lines on stdout.** Docker collects them. Any later log stack (Loki, CloudWatch, Datadog) reads stdout, so the app does not change.
3. **Every vendor is set by env var.** Unset means off. Tests and local dev run with everything off.
4. **Alerts go to one Telegram ops chat** through one function, `notify_ops()`. Moving to PagerDuty or Slack later changes that function only.
5. **Business numbers come from Postgres**, not from a metrics tool. Per-shop activity and sync lag are SQL over tables we already have.

## 2. Phase A: pilot gate (before the first real shop)

This is the minimum listed in [pilot-plan.md §2](../../product/pilot-plan.md): an uptime ping and error alerts to Telegram.

| Piece | What we build | Vendor now | Swap later |
| :--- | :--- | :--- | :--- |
| Readiness endpoint | `GET /health/ready`: runs `SELECT 1` on Postgres and `PING` on Redis, 2s timeout each; 200 or 503 with which check failed. `/health` stays as liveness | none | none |
| Uptime check | External ping of `https://mandi-api.appworx.co.in/health/ready` every 5 min, alert after 2 failures | UptimeRobot free (has a Telegram integration) | Better Stack, Pingdom, k8s probes |
| Error tracking | `sentry-sdk[fastapi]` init in `app/core/observability.py` when `SENTRY_DSN` is set; tag events with `shop_id`, never phone numbers or amounts | Sentry free tier (5k errors/month) | GlitchTip or self-hosted Sentry: change `SENTRY_DSN` |
| Error alert to Telegram | Logging handler on `ERROR` and above that calls `notify_ops()`, at most one message per error type per 10 min (dedupe key in Redis) | Telegram Bot API | Change `notify_ops()` |
| Structured logs | JSON formatter on the root logger with `request_id`, `shop_id`, `path`, `status`, `duration_ms`; replace the `print()` calls in `main.py` lifespan | stdout + Docker `json-file` with rotation (`max-size: 10m`, `max-file: 5`) | Loki / CloudWatch read stdout |

New env vars (all optional): `SENTRY_DSN`, `SENTRY_ENVIRONMENT`, `OPS_TELEGRAM_BOT_TOKEN`, `OPS_TELEGRAM_CHAT_ID`. The bot token is a secret and lives only in the server `.env`, never in git.

## 3. Phase B: pilot weeks 1–3

| Piece | What we build | Vendor now |
| :--- | :--- | :--- |
| Daily ops digest | `scripts/ops_digest.py` run by cron at 21:00 IST: active shops today, bills per shop, shops silent for 2+ days, max and p95 sync lag, open sync conflicts, error count from the last 24h. Sent via `notify_ops()`. Replaces Vivek's manual SQL check | Postgres + Telegram |
| Backup check | Same cron checks the newest off-site `pg_dump`: younger than 26h and larger than the previous one minus 10%. Alerts if not | Telegram |
| Sync lag definition | `server received_at − client created_at` per pushed row, computed in SQL. Needs a `received_at` column if the sync tables lack one (check during planning) | Postgres |

Phase B adds no new vendor. It is SQL and one script.

## 4. Phase C: when scale needs it (not now)

Triggers: more than one API instance, more than ~200 shops, or a latency problem we cannot explain from logs.

- Add `opentelemetry-instrumentation-fastapi`, `-sqlalchemy`, `-redis` with the OTLP exporter. Target: Grafana Cloud free tier, or a self-hosted OTel Collector feeding Prometheus + Tempo.
- Move the digest numbers into dashboards; keep the Telegram digest.
- Uptime moves to the platform's probes if we move to Kubernetes.

Nothing in Phase A or B has to be rewritten for this.

## 5. Out of scope

- Android crash reporting (Crashlytics or Sentry Android). Worth a separate decision before the pilot since shops run the app on their own phones.
- Paid APM, on-call rotation, status page.

## 6. Testing

- `/health/ready`: 200 when DB and Redis are up; 503 with the failing check named when either is down (fake Redis disconnected, bad DB URL).
- `notify_ops()`: no-op when env unset; posts once and dedupes a repeat within 10 min (httpx mocked).
- Sentry: not initialised when `SENTRY_DSN` is unset; `before_send` strips phone numbers.
- JSON log lines parse and carry `request_id`.
- `ops_digest.py`: SQL run against a seeded test DB returns the expected counts; backup check flags a stale and a shrunken dump.

## 7. Open questions

1. Separate ops bot or reuse an existing Telegram bot with a separate chat? Recommendation: a new bot, so a leaked token can't touch the finance bot.
2. Sentry account under which email/org.
3. Where the off-site `pg_dump` lands (affects the backup check path).
