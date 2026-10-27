# Revamp test blockers

Updated: 2026-10-27 20:23 IST

## Task 9

`./gradlew :core-data:jvmTest` currently stops at `:core-data:compileTestKotlinJvm` because `revamp-data-ready` is not open and Task 5 has not added these production classes yet:

- `OfflineFirstBuyRepository`
- `OfflineFirstSellRepository`
- `OfflineFirstLendRepository`
- `OfflineFirstExpenseRepository`

The unresolved member and type-inference errors in the three Task 9 files are downstream compiler errors from those missing classes. Rerun the suite after merging `feat/mandisamiti-revamp` when Claude sets `revamp-data-ready`.
