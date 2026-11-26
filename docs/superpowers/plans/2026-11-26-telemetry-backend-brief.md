# Telemetry task A: backend

Read `docs/superpowers/specs/2026-11-26-telemetry-pipeline-design.md` first. Build only the server side in `backend/`. Smallest change that works; copy existing patterns.

## Endpoint

`POST /api/v1/events`, new router in `app/api/v1/endpoints/events.py`, included in `app/api/v1/api.py`. Auth: `get_current_user_payload`; `shop_id` comes only from the token (reject a token without one, the way sync does).

Request:

```json
{"events": [{"id": "e1", "name": "screen_view", "screen": "buy_list", "session_id": "s1",
             "occurred_at_ms": 1764140000000, "duration_ms": null, "app_version": "1.0"}]}
```

Response 200: `{"accepted": <events in the request>}`.

Validation (422 on failure):
- at most 200 events per request
- unknown fields rejected at both levels
- `id`, `session_id`: 1..64 chars of `[A-Za-z0-9-_]`
- `name`: `^[a-z0-9_]{1,64}$`
- `screen`: null or `^[a-z0-9_]{1,64}$`
- `app_version`: 1..32 chars of `[A-Za-z0-9._+-]`
- `occurred_at_ms` >= 0, `duration_ms` null or >= 0, both fit a signed 64 bit integer

Repeated ids (in one batch or across requests) are stored once and still return 200. Ids are scoped per shop: primary key `(shop_id, id)`, so one shop can never block another shop's event.

## Storage

Model `AnalyticsEvent`, table `analytics_events`, with a server `received_at_ms` column and an index that serves the digest and the purge. New Alembic revision `migrations/versions/0002_analytics_events.py` after `0001_baseline`, with upgrade and downgrade. Must work on Postgres and on the SQLite used by tests.

## Digest and retention

Extend `scripts/ops_digest.py`:
- `collect` adds, for the same day window it already uses (filter on `received_at_ms`): per shop app sessions and saved entries (`*_saved`), the 5 most viewed screens, and counts of `sync_ok` and `sync_failed`.
- `format_digest` prints them briefly. With no events it says so in one line.
- `main` deletes events with `received_at_ms` older than 90 days.

## Tests

`tests/test_events.py` and digest tests next to the existing ones: auth required, shop taken from token, repeat ids stored once, same id from two shops stored twice, unknown field and bad name and 201 events rejected, digest counts, purge keeps recent rows. If an existing test compares migrations with models, keep it passing.
