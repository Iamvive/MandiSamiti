# Telemetry task C: app hooks (composeApp)

Read `docs/superpowers/specs/2026-11-26-telemetry-pipeline-design.md` and the package `core-data/.../data/analytics` first. Only edit `composeApp/`. No UI changes. Smallest diff that works.

- Create one `Analytics` instance where `App.kt` builds `SyncEngine` and pass it in. Do the same in the Android `MandiSyncWorker`. `appVersion` is the real version name on Android (`BuildConfig.VERSION_NAME`), passed into `App` with a default for other targets.
- `app_open` once per app process, not on each recomposition or activity recreation.
- `screen_view` whenever the visible screen changes (and the Home tab, if `AnalyticsScreen` has tab entries). One exhaustive `when` maps `Screen` to `AnalyticsScreen`, with no `else`, so a new screen fails to compile until mapped. Editing the same screen's arguments or recomposing must not record again.
- Record each of these only after the action succeeded, once per action: `login_success`, `buy_saved`, `sell_saved`, `payment_saved`, `expense_saved`, `udhaar_saved`, `slip_shared`, `slip_printed`, `entry_deleted`. Cover every path that performs the action (list, receipt, ledger, galla-rokad, udhaar). Prefer the one shared function all callers use over a hook in each screen.
- Tracking is launched in a coroutine and never delays or blocks the action.
- Pass no names, amounts, ids or text into analytics.

Test (jvmTest): the screen mapping, and that at least the buy save and the delete path record their event once and record nothing when the action fails.
