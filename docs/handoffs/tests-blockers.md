# Revamp test blockers

Updated: 2026-10-27 20:23 IST

## Task 9

`./gradlew :core-data:jvmTest` currently stops at `:core-data:compileTestKotlinJvm` because `revamp-data-ready` is not open and Task 5 has not added these production classes yet:

- `OfflineFirstBuyRepository`
- `OfflineFirstSellRepository`
- `OfflineFirstLendRepository`
- `OfflineFirstExpenseRepository`

The unresolved member and type-inference errors in the three Task 9 files are downstream compiler errors from those missing classes. Rerun the suite after merging `feat/mandisamiti-revamp` when Claude sets `revamp-data-ready`.

## Task 10

The full backend run completed with 87 passing tests and six expected failures in `test_sync_v2.py`. The new sync contract is still absent, so five tests cannot find `synced_buy_entries` or `buy_entries`; the 101-row push also returns 200 instead of 422. Claude's Tasks 6 and 7 own those production fields and validation.

The NGDL token baseline passed with 5 tests and 59 subtests.
