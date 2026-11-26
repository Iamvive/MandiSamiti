# Telemetry: instrumentation, crash reporting, clickstream

Date: 2026-11-26. Status: awaiting Vivek's approval.

## Goal

Know which screens and actions shops use, whether sync works on their phones, and when the app crashes, without sending trade data or personal data anywhere.

## Decisions

- Clickstream goes to our own FastAPI + Postgres (Vivek, 2026-11-26).
- Crash reporting uses the Sentry Android work on `appworx/android-sentry-crash-reporting`. It stays off until a DSN is set.

## Data flow

```
UI / sync code
   | analytics.track(EventName, screen, durationMs?)
   v
SQLDelight table AnalyticsEvent (offline queue, max 5000, oldest dropped)
   | upload after a sync run, batches of 200, delete on 2xx
   v
POST /api/v1/events  (access token, shop_id taken from the token)
   v
Postgres analytics_events (90 day retention)
   v
ops_digest.py -> daily Telegram summary

Crashes: app -> Sentry (PII scrubbed)    Backend errors: existing observability.py
```

## Event contract

| Field | Type | Note |
|---|---|---|
| id | string, max 64 | generated on the phone; server ignores repeats |
| name | enum | list below |
| screen | enum or null | `Screen` subclass name only, never its arguments |
| sessionId | string, max 64 | new per app process |
| occurredAtMs | long | phone clock |
| durationMs | long or null | sync events only |
| appVersion | string, max 32 | |

Names: `app_open`, `screen_view`, `login_success`, `buy_saved`, `sell_saved`, `payment_saved`, `expense_saved`, `udhaar_saved`, `slip_shared`, `slip_printed`, `entry_deleted`, `sync_ok`, `sync_failed`.

There is no free-text field. Party names, phone numbers, amounts, ids of business records and error messages cannot enter an event, because the Kotlin API accepts only enums and numbers and the server rejects unknown fields and names that fail `^[a-z0-9_]{1,64}$`.

## Rules

- Tracking never blocks or fails a user action. Every error inside it is swallowed.
- Upload failure keeps the rows for the next run. It never changes the sync result.
- Logout wipes the queue, so events cannot be sent under another shop.
- The server caps a batch at 200 events.
- Database change is additive (`15.sqm`), following `core-database/MIGRATIONS.md`.
- Server change is a new Alembic revision after `0001_baseline`.

## Out of scope

Dashboards, funnels, third-party analytics SDKs, performance tracing, iOS crash reporting, per-button tap tracking beyond the names above.

## Verification

- Each task has a test command that must pass before its commit.
- End to end on staging: use the app, see rows in staging Postgres, see them in the digest.
- Crash check on a real phone needs `mandi.sentryDsn` (owner action).

## Owner actions

1. Create the Sentry Android project and set `mandi.sentryDsn` in `local.properties`.
2. Telegram ops bot values on the server, if the digest should be delivered.
3. Review the PR. Prod deploy stays manual.
