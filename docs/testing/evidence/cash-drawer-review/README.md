# Cash Drawer review evidence

All screenshots use synthetic demo data rendered by `DrawerLayoutTest`. They do not show a pilot shop or a connected phone.

The test renders the real Compose screen at 390px and 1000px in Hindi and English, light and dark themes. It checks text bounds, switches to the transaction list, filters cash, enters note quantities, saves a count, and confirms closing. The counting dialog scrolls; its save action stays in the footer.

Validation: 361 JVM tests passed across the four modules, debug APK assembled, and five NGDL token checks passed. Audio was disabled. Physical Android installation, system keyboard behavior, and accessibility font scaling remain unverified.

Counts, count times, closing times, and reasons are stored locally per shop and date in the existing metadata table. There is no schema migration or server sync for these observations. Cash additions, edits, voids, deletions, or carry-forward changes require a fresh count. Closing preserves any shortage or surplus and creates no automatic ledger adjustment.
