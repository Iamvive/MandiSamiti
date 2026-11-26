# Telemetry task B: offline queue and upload (core-database, core-data)

Read `docs/superpowers/specs/2026-11-26-telemetry-pipeline-design.md` and `core-database/MIGRATIONS.md` first. Do not touch `composeApp/` or `backend/`. Smallest change that works; copy existing patterns.

## Database

Table `AnalyticsEvent` in `AppDatabase.sq` plus the matching `15.sqm` (additive). Queries: insert, oldest N, delete by ids, trim to the newest 5000, wipe. The test command generates the `16.db` schema snapshot, so do not create it. Extend `PilotSchemaMigrationTest` for the new table.

## API in package `com.appwork.mandisamiti.data.analytics`

- `enum class AnalyticsEventName(val wire: String)`: app_open, screen_view, login_success, buy_saved, sell_saved, payment_saved, expense_saved, udhaar_saved, slip_shared, slip_printed, entry_deleted, sync_ok, sync_failed.
- `enum class AnalyticsScreen(val wire: String)`: one entry for each subclass of `Screen` in `composeApp/.../App.kt` (read it, do not edit it), wire names in snake_case such as `buy_list`. If Home has tabs, add one entry per tab (`home_...`). Never derive names by reflection: release builds are minified.
- `class Analytics(database, appVersion, ...)` with `suspend fun track(name, screen: AnalyticsScreen? = null, durationMs: Long? = null)` and `suspend fun flush(client: MandiSyncApiClient)`. No String or free-form parameter in `track`. One session id per process. Clock and id generator injectable for tests. Both functions run off the main thread and swallow every exception except `CancellationException`.
- `track` inserts and trims to 5000.
- `flush` sends batches of up to 200, oldest first, at most 5 batches per call. 2xx: delete the batch. HTTP 400 or 422: delete the batch, so one bad row cannot block the queue. Anything else, including 401, 404 and no network: keep the rows and stop.

## Wire format

`POST $baseUrl/api/v1/events` with the same bearer token and refresh handling as sync push:

```json
{"events": [{"id": "e1", "name": "screen_view", "screen": "buy_list", "session_id": "s1",
             "occurred_at_ms": 1764140000000, "duration_ms": null, "app_version": "1.0"}]}
```

Response: `{"accepted": 1}`. `id` and `session_id` use only `[A-Za-z0-9-_]`, max 64. `app_version` max 32 chars of `[A-Za-z0-9._+-]`.

Add `pushEvents` to `MandiSyncApiClient` with a default implementation that returns failure, so existing fakes still compile; implement it in `KtorMandiSyncApiClient`.

## Sync and logout

- `SyncEngine` takes `analytics: Analytics? = null`. After a full sync run it tracks `sync_ok` or `sync_failed` with the duration, then flushes. Telemetry must never change the `SyncResult`, and a flush must not make a sync fail. Skip when the sync did not actually run.
- `LocalDataWiper.wipeAll` also wipes the queue.

## Tests (jvmTest)

Track stores a row; cap holds; flush deletes on success, keeps on network failure and 404, drops on 422; a throwing flush leaves the sync result unchanged; wipe empties the queue; the JSON field names match the wire format above.
