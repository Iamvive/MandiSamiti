# SQLDelight migrations

The pilot baseline was schema version 7. Schema version 8 adds nullable party GSTIN and mandi licence columns through `7.sqm`. The committed snapshots allow direct upgrade tests from versions 1 and 7.

For every schema change:

1. Add `N.sqm`, where `N` is the current `AppDatabase.Schema.version`. Use additive SQL where possible. Never edit an older migration after a build has shipped.
2. Make the matching change in `AppDatabase.sq`. Keep currency columns as `INTEGER`/Kotlin `Long` paisa.
3. Run `./gradlew generateCommonMainAppDatabaseSchema` once with a timeout. Commit the generated `core-database/src/commonMain/sqldelight/databases/N+1.db` snapshot. If generation hangs, stop and report it instead of retrying in a loop.
4. Extend `PilotSchemaMigrationTest` with old-version rows for any new table, preserved values for changed tables, and assertions for each new column's default.
5. Run `./gradlew :core-database:jvmTest :core-data:jvmTest` before committing.

Migrations must not drop columns, rename tables, or change `Long` paisa columns.
