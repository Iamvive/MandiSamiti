# Party tax fields and master import implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan.

**Goal:** Add nullable GSTIN and mandi licence fields across the party storage and sync contract, then provide a private, dry-run-first server importer that creates party masters without exposing or overwriting shop data.

**Architecture:** Keep one shared party model for farmers and buyers. Apply additive client and server migrations, use omission-aware backend sync updates for old-client compatibility, and isolate CSV parsing and merge policy in a pure Python import module called by a thin CLI. The existing sync pull remains the only route from the server to a fresh phone.

**Tech stack:** Kotlin Multiplatform, SQLDelight, kotlinx.serialization, Python 3, FastAPI/Pydantic, SQLAlchemy, Alembic, pytest.

**Approved design:** `docs/superpowers/specs/2026-10-30-party-tax-fields-import-design.md`

## Global constraints

- Work on isolated branches. Do not edit `main` directly.
- Do not place real names, phones, villages, GSTINs, licences, sheet rows, or generated review files in the repository.
- Do not print database URLs, raw rows, personal fields, or raw exception bodies.
- Preserve `sellEntry.gstin` as the bill snapshot.
- Antigravity owns the buyer UI branch. This plan supplies its data contract and reviews its integration, but does not edit the same UI files concurrently.
- Keep all money as `Long` paisa and weights as `Long` grams.
- Use additive migrations only. Do not drop or rewrite existing client rows.

## Task 1: Add the client party fields and schema-v8 migration

**Files:**

- Create: `core-database/src/commonMain/sqldelight/com/appwork/mandisamiti/database/7.sqm`
- Create: `core-database/src/commonMain/sqldelight/databases/8.db`
- Modify: `core-database/src/commonMain/sqldelight/com/appwork/mandisamiti/database/AppDatabase.sq`
- Modify: `core-database/src/jvmTest/kotlin/com/appwork/mandisamiti/database/PilotSchemaMigrationTest.kt`
- Modify: `core-database/MIGRATIONS.md`
- Modify: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/Party.kt`
- Modify: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/repository/OfflineFirstPartyRepository.kt`
- Test: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/PartyTaxFieldsTest.kt`

### Step 1: Write the failing migration and repository tests

- [ ] Extend the v7 migration fixture with one farmer and one buyer, migrate to the current schema, and assert every old column survives.
- [ ] Assert `gstin` and `mandi_license` are null for migrated rows.
- [ ] Add a repository round-trip test for a buyer with both values and a farmer with both values null.

The domain contract becomes:

```kotlin
data class Party(
    // existing fields stay in their existing order
    val gstin: String? = null,
    val mandiLicense: String? = null,
)
```

Run:

```bash
./gradlew :core-database:jvmTest :core-data:jvmTest
```

Expected: compilation or assertions fail because the schema and model do not yet expose the fields.

### Step 2: Add the additive migration and update every SQL statement

- [ ] Create `7.sqm` with only:

```sql
ALTER TABLE partyEntity ADD COLUMN gstin TEXT;
ALTER TABLE partyEntity ADD COLUMN mandi_license TEXT;
```

- [ ] Add the columns to `partyEntity` in `AppDatabase.sq`.
- [ ] Update every party insert, local update, server apply, and backfill projection in the same file.
- [ ] Backfill helpers pass null for both fields.
- [ ] Update repository entity-to-domain and domain-to-query mappings.
- [ ] Generate `databases/8.db` with the repository's SQLDelight migration task and record the exact command in `MIGRATIONS.md`.

### Step 3: Verify client storage

Run:

```bash
./gradlew :core-database:jvmTest :core-data:jvmTest
```

Expected: PASS, including v7 to v8 data preservation and field round trip.

### Step 4: Commit the client schema slice

```bash
git add core-database core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/Party.kt core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/repository/OfflineFirstPartyRepository.kt core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/PartyTaxFieldsTest.kt
git commit -m "feat(party): add GSTIN and mandi licence storage"
```

## Task 2: Extend the backend and sync contract safely

**Files:**

- Create: `backend/migrations/versions/0002_party_tax_fields.py`
- Modify: `backend/app/models/party.py`
- Modify: `backend/app/schemas/party.py`
- Modify: `backend/app/api/v1/endpoints/sync.py`
- Modify: `backend/tests/test_migrations.py`
- Modify: `backend/tests/test_sync_push.py`
- Modify: `backend/tests/test_sync_pull.py`
- Modify: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/sync/model/SyncDto.kt`
- Modify: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/sync/SyncEngine.kt`
- Modify: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/SyncEngineTest.kt`
- Modify: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/sync/remote/KtorMandiSyncApiClientTest.kt`

### Step 1: Write failing backend contract tests

- [ ] Add a valid-value push test and assert pull returns both fields.
- [ ] Add an old-payload test that omits both keys and preserves existing server values.
- [ ] Add an explicit-null test that clears both values.
- [ ] Add invalid GSTIN and overlong mandi licence tests that return validation errors without reflecting the bad value.
- [ ] Extend migration parity assertions for both nullable columns.

The schema accepts omitted values but validates values when present:

```python
GSTIN_PATTERN = r"^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$"

class PartyBase(BaseModel):
    gstin: str | None = Field(default=None, max_length=15, pattern=GSTIN_PATTERN)
    mandi_license: str | None = Field(default=None, max_length=100)
```

Blank normalization belongs in validators: trim, uppercase GSTIN, and convert blank strings to `None`.

Run:

```bash
cd backend
PYTHONPATH=. venv/bin/pytest -q tests/test_migrations.py tests/test_sync_push.py tests/test_sync_pull.py
```

Expected: FAIL until model, migration, and update semantics exist.

### Step 2: Implement backend storage and omission-aware updates

- [ ] Add nullable model columns and Alembic revision `0002_party_tax_fields`, with `down_revision = "0001_baseline"`.
- [ ] Keep downgrade limited to these two columns.
- [ ] Extend all party request and response schemas.
- [ ] On an existing party, update each new field only if its key is in `p_in.model_fields_set`.
- [ ] Keep current shop scoping, timestamp conflict handling, and server sequence behavior unchanged.
- [ ] Ensure pull always serializes both keys.

The critical update pattern is:

```python
fields_set = p_in.model_fields_set
if "gstin" in fields_set:
    existing.gstin = p_in.gstin
if "mandi_license" in fields_set:
    existing.mandi_license = p_in.mandi_license
```

### Step 3: Implement client sync mappings

- [ ] Add `gstin` and `mandi_license` with null defaults to `PartySyncDto`.
- [ ] Map `mandi_license` to domain `mandiLicense` in both directions.
- [ ] Pass both values through server-apply SQL.
- [ ] Extend sync tests for push, pull, and the existing local-unsynced-wins rule.

### Step 4: Verify backend and client sync

Run:

```bash
cd backend
PYTHONPATH=. venv/bin/pytest -q tests/test_migrations.py tests/test_sync_push.py tests/test_sync_pull.py
cd ..
./gradlew :core-data:jvmTest
```

Expected: PASS. The omission test must prove that an old client cannot erase server tax details.

### Step 5: Commit the sync slice

```bash
git add backend core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/sync core-data/src/jvmTest
git commit -m "feat(sync): carry party tax fields safely"
```

## Task 3: Build the pure CSV import core and dry run

**Files:**

- Create: `backend/app/importers/__init__.py`
- Create: `backend/app/importers/party_import.py`
- Create: `backend/scripts/import_parties.py`
- Create: `backend/tests/test_import_parties.py`

### Step 1: Define the import API and invented fixtures

- [ ] Write tests using invented values only.
- [ ] Cover UTF-8 and BOM, header aliases, normalization, invalid phones, the known placeholder rule, duplicate phones, same-name farmers in different villages, GSTIN-first buyer matching, invalid GSTIN review, and source priority.
- [ ] Cover missing headers, oversized files, more than 50,000 rows, bad encoding, symlinks, non-files, and paths outside the allowlisted directory.
- [ ] Assert dry run performs no writes and stdout contains counts only.

Use small immutable records at the parsing boundary:

```python
@dataclass(frozen=True)
class PartyCandidate:
    source: str
    row_number: int
    role: str
    name: str
    normalized_name: str
    phone: str | None
    village: str | None
    gstin: str | None
    mandi_license: str | None

@dataclass(frozen=True)
class ReviewIssue:
    source: str
    row_number: int
    code: str
    values: tuple[str, ...]
```

Keep parsing, normalization, identity selection, merge, and review-row serialization callable without a database session.

Run:

```bash
cd backend
PYTHONPATH=. venv/bin/pytest -q tests/test_import_parties.py
```

Expected: FAIL because the module and CLI do not exist.

### Step 2: Implement boundary controls

- [ ] Resolve the import root once from `~/mandisamiti-import/`.
- [ ] Reject symlinks before following them, then require a resolved regular file below the resolved root.
- [ ] Reject files over 10 MiB before opening.
- [ ] Open as `utf-8-sig`, use the standard `csv` module, and stop above 50,000 data rows.
- [ ] Match English headers case-insensitively after trimming. Keep each sheet's adapter explicit.
- [ ] Raise typed, redacted errors that contain only filename, header name, issue code, or counts.

### Step 3: Implement normalization and deterministic merge policy

- [ ] Preserve trimmed display spelling while matching on Unicode NFKC, collapsed whitespace, and case-folded text.
- [ ] Reuse or exactly mirror the app's Indian phone normalization.
- [ ] Convert the known placeholder and invalid phones to null and add review issues.
- [ ] Make the first row the phone owner when different names share one valid phone. Clear later phones and add review issues.
- [ ] Match farmers by valid phone, else normalized name plus normalized village.
- [ ] Match buyers by valid GSTIN, else normalized name.
- [ ] Apply source priority: Buyer, GST Purpose, October buyers, farmer receipts.
- [ ] Never guess a corrected GSTIN. Clear invalid GSTIN and add a review issue.

### Step 4: Implement private report writing and CLI dry run

- [ ] Default to dry run. Require `--shop-id` only with `--apply`.
- [ ] Print counts and status only.
- [ ] Create review CSV atomically under the import root with mode `0600`.
- [ ] Prefix cells beginning with `=`, `+`, `-`, or `@` with a single quote.
- [ ] Never catch and print a raw database or parser exception.

Run:

```bash
cd backend
PYTHONPATH=. venv/bin/pytest -q tests/test_import_parties.py
```

Expected: parsing, merge, report, path, and dry-run tests PASS.

### Step 5: Commit the dry-run slice

```bash
git add backend/app/importers backend/scripts/import_parties.py backend/tests/test_import_parties.py
git commit -m "feat(import): add private party CSV dry run"
```

## Task 4: Add transactional apply, rerun safety, and fresh-phone proof

**Files:**

- Modify: `backend/app/importers/party_import.py`
- Modify: `backend/scripts/import_parties.py`
- Modify: `backend/tests/test_import_parties.py`
- Modify: `backend/tests/test_sync_pull.py`
- Create: `docs/product/reviews/2026-10-30-party-import-handoff.md`

### Step 1: Write failing apply tests

- [ ] Assert a missing or foreign shop ID stops before parsing writes.
- [ ] Assert apply creates the expected farmer and buyer masters in one commit.
- [ ] Assert rerun creates zero parties and leaves all existing fields unchanged.
- [ ] Assert a natural-key match also prevents overwrite when the deterministic ID differs.
- [ ] Inject a late database failure and assert the transaction writes nothing.
- [ ] Assert created rows get fresh timestamps and the server sequence behavior needed by pull.
- [ ] Assert a sync pull from cursor zero contains imported parties and both tax fields.

### Step 2: Implement tenant-scoped, atomic apply

- [ ] Confirm the target shop exists before processing.
- [ ] Query existing parties for that shop only and index them by natural key plus deterministic import ID.
- [ ] Derive deterministic IDs from shop ID, role, and canonical natural key with UUIDv5.
- [ ] Treat any match as existing and never update it.
- [ ] Add all missing parties inside one `Session.begin()` transaction.
- [ ] Use the backend's established timestamp and sync-sequence mechanism. Do not invent a parallel cursor.
- [ ] Roll back the whole run on any exception and show a redacted failure code.

### Step 3: Verify apply and sync delivery

Run:

```bash
cd backend
PYTHONPATH=. venv/bin/pytest -q tests/test_import_parties.py tests/test_sync_pull.py tests/test_sync_push.py tests/test_sync_isolation.py
```

Expected: PASS, including a cursor-zero pull that proves imported masters reach a fresh client.

### Step 4: Write the handoff without shop data

- [ ] Document command syntax, dry-run-first operation, backup requirement, count meanings, review codes, rerun behavior, and rollback behavior.
- [ ] State that real import remains blocked on private CSV exports and the shop's first login.
- [ ] Do not include example personal data or a real shop ID.

### Step 5: Commit the apply slice

```bash
git add backend docs/product/reviews/2026-10-30-party-import-handoff.md
git commit -m "feat(import): apply party masters atomically"
```

## Task 5: Integrate and review Antigravity's buyer UI

**Files to review, owned by Antigravity:** buyer add/edit form, buyer detail screen, Bill of Supply form, translations, and UI tests on `feat/party-gst-ui`.

- [ ] Confirm Antigravity consumes `Party.gstin` and `Party.mandiLicense` without duplicating storage.
- [ ] Confirm fields appear only for buyers, GSTIN uppercases and validates, blank saves null, and licence trims to 100 characters.
- [ ] Confirm Bill of Supply copies buyer GSTIN into the existing `sellEntry.gstin` snapshot.
- [ ] Confirm later party edits do not rewrite saved bills.
- [ ] Run light and dark checks at 390px and the NGDL token tests.
- [ ] Resolve only integration conflicts on the schema branch. Do not replace Antigravity's UI design.

Run after the UI branch is available:

```bash
./gradlew jvmTest :composeApp:assembleDebug
python3 tests/ngdl-tokens/run_tests.py
```

Expected: PASS, with any device-only checks listed explicitly in the handoff.

## Task 6: Independently audit Claude's sheet-parity math

**Inputs:**

- `vaults/Vivek-K/wiki/concepts/mandisamiti-sheet-math.md`
- Claude branch `fix/sheet-parity-math`, only after independent expected values are frozen
- `prompts/mandisamiti-sheet-parity-tasks.md`

**Output:**

- Create: `docs/product/reviews/2026-10-30-sheet-math-independent-review.md`

### Step 1: Freeze independent expected values before reading Claude's diff

- [ ] Use a disposable script outside the repository.
- [ ] Recompute all 50 numeric vectors from the written rules. Do not use real names or copy private rows.
- [ ] Pin exactly 5,000 g extra versus 5,001 g, gross floor, nearest-Rs-10 half-up ties, per-row commission, advance after rounding, and commission as the rounding residual.
- [ ] Recompute Bill of Supply and Cash Memo separately, including paise versus whole-rupee behavior.
- [ ] Save only numeric vector identifiers, inputs, and results in the review evidence.

### Step 2: Review Claude's tests and implementation

- [ ] Compare all frozen results to Claude's tests.
- [ ] Inspect production callers for rounding order, overflow, editable defaults, multi-lot aggregation, billed bags, printed slip values, 6R and 9R, day-page values, and party balance.
- [ ] Run Claude's focused tests and the full gate.
- [ ] Report mismatches and missing coverage only. Do not edit Claude's branch during this audit.

### Step 3: Record the verdict

The review must say one of:

- `PASS`: all written rules and frozen vectors agree;
- `BLOCKED`: Claude's M1 branch is unavailable;
- `FAIL`: list each numeric mismatch or missing blocking case with file and test references.

No pass is inferred from test success alone.

## Task 7: Final verification and delivery

- [ ] Run the complete project gate:

```bash
./gradlew :core-database:jvmTest :core-data:jvmTest jvmTest :composeApp:assembleDebug
cd backend
PYTHONPATH=. venv/bin/pytest -q
```

- [ ] Run `git diff --check` and scan the branch for secrets and private-data fixtures.
- [ ] Run `python3 scripts/sync_skills.py` from the workspace root for project-update parity.
- [ ] Update the shared task row and add a concise handoff in `docs/product/reviews/`.
- [ ] Send the required Telegram milestone alert with pass, blocker, and next-action choices.
- [ ] Do not run a real import until staging is backed up, the shop exists, the private CSVs are present, and Vivek has approved the dry-run counts and review CSV.

## Completion gates

- Client v7 data migrates to v8 without loss.
- Backend migration matches SQLAlchemy metadata.
- GSTIN and mandi licence survive create, update, clear, push, pull, and local-unsynced conflict behavior.
- Old payload omission preserves both server values.
- Dry run writes no database rows and emits counts only.
- Apply is shop-scoped, atomic, idempotent, and never overwrites an existing party.
- Review CSV is private and formula-safe.
- A cursor-zero sync pull proves imported masters reach a fresh phone.
- Buyer UI passes its separate integration gate.
- Claude's math agrees with independently frozen results, or the pilot merge remains blocked.
