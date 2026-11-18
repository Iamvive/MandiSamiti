# Task 12 brief for Antigravity: sync and backend schema for udhaar (no deploy), plus one missing regression test

Plan: `docs/superpowers/plans/2026-11-16-udhaar-rewrite-plan.md`, Task 12 (lines 503-515). Read it first. Tasks 10, 11 and 11b are accepted (latest commit d91d0de).

## Step 0: missing regression test from Task 11b
The `App.kt:827` fix (sell slip deposit excludes cash rows with `lendId != null`) has no test. Add one in `composeApp/src/jvmTest/.../`: a buyer with a `JAMA_RECEIVED` deposit row and a `JAMA_RECEIVED` repayment row that has `lendId` set; the sell slip deposit counts only the first. If the deposit sum is inline in a Composable and cannot be tested as is, move that one line into a small pure function next to it and test that. Run it against the pre-fix filter to see it fail, then pass. Commit this on its own: `test(mandisamiti): sell slip deposit excludes udhaar repayment rows`.

## Verified facts (checked in code, do not re-derive)
1. **The plan's migration name is wrong.** The plan says `0007_udhaar_fields.py` with `down_revision = "0006_shop_profile_address_tax"`. Migration `0007_add_is_khaate.py` already exists (`revision = "0007_add_is_khaate"`). Create `backend/migrations/versions/0008_udhaar_fields.py` with `revision = "0008_udhaar_fields"` and `down_revision = "0007_add_is_khaate"`. Check `ls backend/migrations/versions` first in case something newer exists, and chain from the real head. Copy the shape of `0005_loan_interest_rate.py` (`op.batch_alter_table`, `server_default`).
2. **Client DTOs** are in `core-data/src/commonMain/.../data/sync/model/SyncDto.kt`:
   - `LendEntrySyncDto` (~line 143) has no `direction`, `kind`, `receiver_name`, `receiver_phone`.
   - `CashTransactionSyncDto` (~line 55) has `source_entry_id` but no `lend_id`.
   - `LendSettlementSyncDto` already carries `principal_paisa` and `interest_paisa`. Confirm the backend stores and returns those two as well. If it does not, report it and add it to this task.
3. **Client mapping** is in `SyncEngine.kt`: lend push ~line 91 and 194, lend pull `insertLendEntryIfAbsent` ~line 486; cash push ~line 137-150, cash pull `insertCashTransactionIfAbsent` ~line 386-405. Both directions must carry the new fields. Confirm the local SQLDelight queries (`insertLendEntryIfAbsent`, `insertCashTransactionIfAbsent`, the pending-sync selects) already have the columns and parameters; if a parameter is missing, add it in `AppDatabase.sq` without a destructive migration.
4. **Backend** models and schemas: `backend/app/models/lend_entry.py` (~line 25), `backend/app/models/transaction.py` (~line 22), `backend/app/schemas/entries.py`, `backend/app/schemas/transaction.py`, `backend/app/api/v1/endpoints/sync.py`. Find every place `monthly_interest_rate` and `source_entry_id` are copied (push upsert, pull serialise) and add the new fields next to them.
5. **Test templates:** client `core-data/src/jvmTest/.../data/sync/PaymentLinkWireTest.kt` (MockEngine, real JSON, backend field names); backend `backend/tests/test_sync_payment_link.py` (`client`, `auth_headers`, `db_sessionmaker` fixtures; push to `/api/v1/sync/push`, then pull). Also see `backend/tests/test_migrations.py` and read `backend/README.md` for the test command.
6. Domain fields to carry: `LendEntry.direction` (`GIVEN` or `TAKEN`), `kind` (`LOAN`, `PESGI`, `CASH`), `receiverName`, `receiverPhone`; `CashTransaction.lendId`.
7. Cut rows carry tags in `remarks` (`[SETTLE:...]`, `[BUYCUT:<buyEntryId>]`). Remarks already sync as text, so no new field is needed for them. Do not add one.

## Steps (tests first)
1. Step 0 above.
2. **Client wire test** `core-data/src/jvmTest/.../data/sync/UdhaarWireTest.kt` (style of `PaymentLinkWireTest`), failing first:
   a. A lend with `direction = TAKEN`, `kind = PESGI`, `receiverName`, `receiverPhone` survives the push JSON and a pull round trip.
   b. A cash row with `lendId` survives both ways.
   c. A DTO JSON from an old client (fields absent) decodes with `direction = "GIVEN"`, `kind = "LOAN"`, `receiver_* = null`, `lend_id = null`.
   d. A pulled lend with an unknown `kind` string (for example "FUTURE") does not crash the pull; report how it is handled (fall back to `LOAN` or skip with a log).
3. **Backend test** next to the existing ones (for example `backend/tests/test_sync_udhaar.py`): push a lend with the new fields and a cash row with `lend_id`; pull returns them; a push without them stores `GIVEN`, `LOAN`, null, null. Also extend `test_migrations.py` style coverage: upgrade from `0007` to `0008` on a DB that already has lend rows keeps those rows with `GIVEN` and `LOAN`.
4. **Implement** DTO fields: lend gets `direction: String = "GIVEN"`, `kind: String = "LOAN"`, `receiver_name: String? = null`, `receiver_phone: String? = null`; cash gets `lend_id: String? = null`. Map both ways in `SyncEngine`. Migration `0008`: add the columns to the lend and cash tables with `server_default` `"GIVEN"` and `"LOAN"` and nullable for the rest, reversible `downgrade`. Keep `monthly_interest_rate` default 2.0 on the server. Add the fields to backend models, request and response schemas, and the sync endpoint.
5. Run all four client modules (`./gradlew :core-domain:jvmTest :core-database:jvmTest :core-data:jvmTest :composeApp:jvmTest`) and the backend tests. Report counts from the result XMLs and pytest output.
6. **Do not deploy.** No `sync_to_vps.sh`, no Docker stack, no touching the VPS. The backend change waits for Vivek's approval. Add a PROGRESS.md note: "Task 12 backend change committed, not deployed, pending Vivek's approval". Old app builds must keep working against the old and new backend (all new fields have defaults); say in the report whether that holds.
7. Log and commit: PROGRESS.md entry, new feature at the end of FEATURES.json (status `passing` only for what a test proves; the deploy and a real-device sync stay unverified and say so), commit `feat(mandisamiti): sync udhaar direction, kind, receiver and cash lend link`. Do not touch hot.md. Do not commit `__pycache__`.

## Edge cases to handle or report
1. Old server, new client: the server ignores unknown fields? Check pydantic `extra` settings; if it rejects them, an updated app would fail to sync before the backend is deployed. Report the answer and what the deploy order must be.
2. Rows already on the server (existing lends) get `GIVEN` and `LOAN` from `server_default` and the client pulls them correctly.
3. A TAKEN lend pulled to a second device keeps its receiver fields.
4. `is_void` and settlement void state still sync for rows touched by the Task 11b reversal (voided settlements and cash rows with `sync_status = 0`). Add one test that a voided cut settlement and its cash rows are pushed with `is_void = 1`.
5. Conflict rule: the existing revision-based rule applies; do not change it.
6. SQLite `batch_alter_table` in the migration must work on both SQLite and the production database; say which one the test suite uses.

## Report back (numbered)
1. Commit hashes (step 0 and Task 12).
2. Test counts per client module (from XML) and the backend test result.
3. Which existing assertions changed and why.
4. The migration file name and chain, and confirmation that the plan's `0007` name conflict was handled.
5. Edge cases 1 and 4 answers with evidence.
6. Whether the settlements `principal_paisa` and `interest_paisa` already sync on the backend.
7. What is unverified: real device sync, the VPS, any backend deploy.
8. Anything in the code that conflicts with this brief. If so, stop and ask Vivek one question.
