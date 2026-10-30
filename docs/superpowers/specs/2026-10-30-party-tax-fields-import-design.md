# Party tax fields and master import design

Date: 2026-10-30

Status: approved design, awaiting written-spec review

## Goal

Add GSTIN and mandi licence details to party masters, import the shop's existing farmer and buyer masters safely, and verify the sheet-parity money changes independently before the pilot build is accepted.

The shop CSV exports and generated review reports stay outside the public repository in `~/mandisamiti-import/`. Tests use invented records only.

## Scope

This work includes:

- nullable GSTIN and mandi licence fields on the shared party model;
- SQLDelight schema version 8 and backend Alembic revision `0002`;
- client and server sync support that remains compatible with old payloads;
- buyer add, edit, detail, and bill-prefill behavior;
- a server-side CSV importer with dry-run and transactional apply modes;
- a fresh-phone sync-pull check for imported parties;
- an independent review of Claude's buy and sell math.

This work does not import receipts, bills, balances, loans, cash history, or opening balances. It does not modify the source Google Sheets.

## Approaches considered

### Server-side import, selected

The importer runs beside the backend, validates CSVs, writes party masters in one transaction, and lets the existing sync pull deliver them to phones. This keeps import policy in one testable place and avoids Android file permissions.

### Client-side import

An Android importer would require file-picker UI, local parsing, sync conflict handling, and device storage permissions. It increases the amount of code handling private CSV data and is rejected for the pilot.

### One-off database script

Direct SQL would be short, but it would have weak validation, poor rerun behavior, and no reusable review report. It is rejected because the real import must be auditable.

## Party data model

Add these nullable fields to every party record:

| Field | Storage | Validation | Display |
| --- | --- | --- | --- |
| `gstin` | nullable text, maximum 15 characters | uppercase ASCII GSTIN pattern | buyer forms and buyer detail |
| `mandi_license` | nullable text, maximum 100 characters | trim surrounding whitespace; blank becomes null | buyer forms and buyer detail |

The GSTIN pattern is:

```text
^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$
```

The database fields remain available for every party type so sync and import use one model. The Compose UI hides them for farmers. This avoids a second buyer table while keeping the current `PartyType` boundary.

### Client migration

- Add `gstin TEXT` and `mandi_license TEXT` to `partyEntity` in `AppDatabase.sq`.
- Add additive migration `7.sqm`; it upgrades schema version 7 to version 8 without dropping or rewriting rows.
- Generate and commit `databases/8.db`.
- Extend the schema migration test to start from the version-7 snapshot, insert representative party and transaction rows, migrate, and verify all old values survive while both new columns default to null.
- Update every party insert, update, server-apply, and backfill query. Backfills set the new fields to null.

### Backend migration

- Add nullable columns to the SQLAlchemy `Party` model.
- Add Alembic revision `0002_party_tax_fields` after `0001_baseline`.
- Upgrade adds the columns. Downgrade removes only these two columns.
- Extend migration tests to compare the new columns with `Base.metadata`.

## Sync contract

`Party`, `PartySyncDto`, `PartyBase`, `PartyCreate`, and `PartyResponse` carry both fields.

The compatibility rule distinguishes omission from clearing:

- An old client omits the fields. The backend preserves the existing server values.
- A new client includes a non-null value. The backend validates and stores it.
- A new client explicitly includes null. The backend clears that field.
- Sync pull always returns both fields.

The backend update path uses Pydantic's set-field information to distinguish an omitted field from an explicit null. Client server-apply retains the existing local-unsynced-wins rule.

Tests cover old payload omission, explicit null, valid values, invalid GSTIN rejection, pull round trip, and a local pending edit that wins over a server row.

## Buyer interface

GSTIN and mandi licence appear only when `PartyType.BUYER` is selected:

- buyer add form;
- buyer edit form;
- buyer detail page;
- Bill of Supply buyer selection, where GSTIN is prefilled from the party.

The bill continues to store its own GSTIN snapshot. Later edits to the party do not rewrite an existing bill.

Input behavior:

- GSTIN is uppercased as the user types and limited to 15 ASCII characters.
- Blank values save as null.
- Invalid GSTIN blocks save with a direct Hindi or English error.
- Mandi licence is trimmed and limited to 100 characters.
- Existing NGDL fields, spacing, error colors, card elevation, and vector icons are reused. No token values are copied.

UI verification covers Hindi and English, light and dark themes, 390px width, keyboard navigation, field errors, buyer save and edit, farmer-field hiding, bill prefill, and `tests/ngdl-tokens`.

## Import command

Create `backend/scripts/import_parties.py`. It accepts explicit CSV paths:

```text
python backend/scripts/import_parties.py \
  --farmers ~/mandisamiti-import/kisaan-buying-receipts.csv \
  --buyers ~/mandisamiti-import/buyer.csv \
  --gst-purpose ~/mandisamiti-import/gst-purpose.csv \
  --october-buyers ~/mandisamiti-import/october-2026.csv \
  --shop-id <shop-id>
```

Dry run is the default. `--apply` enables the database transaction. Every path must resolve to a regular file under `~/mandisamiti-import/`.

### Boundary controls

- Accept UTF-8 and UTF-8 with BOM.
- Reject missing or ambiguous required headers before parsing rows.
- Limit each file to 10 MiB and 50,000 rows.
- Use Python's standard `csv` module. No spreadsheet dependency is added.
- Never print names, phones, villages, GSTINs, licences, raw rows, database URLs, or error bodies.
- Write the review CSV with mode `0600`. Neutralize cells beginning with `=`, `+`, `-`, or `@` to prevent spreadsheet-formula execution.
- Reject paths outside the import directory, directories, devices, and symlinks.
- Use parameterized ORM queries and one database transaction for apply mode.
- Confirm the target shop exists before processing.

### Header adapters

Each sheet tab gets a small adapter that maps its exported column names to the canonical fields. Header matching trims whitespace and is case-insensitive for English headings. A missing required header stops the run with the header name only; it never prints row data.

The real exports determine the final alias lists. Until they arrive, tests define representative Hindi and English headings without using shop data.

### Normalization and identity

Names retain the sheet spelling after trimming. A separate normalized value is used only for matching: Unicode-normalized, whitespace-collapsed, and case-folded.

Phones use the app's Indian-phone normalization. The known placeholder and every invalid phone become null and are listed for review without being printed.

Farmers are matched in this order:

1. valid normalized phone;
2. normalized name plus normalized village when no phone exists.

Buyers are matched in this order:

1. valid GSTIN;
2. normalized name when GSTIN is absent or invalid.

Same-name farmers in different villages remain separate. A phone shared by different names belongs to the first row; later rows are created without a phone and added to the review report. Invalid GSTIN becomes null and is added to review. The importer never guesses a correction.

### Safe reruns

The importer creates only missing party masters. If a natural-key match or a deterministic import ID already exists, it counts the row as existing and does not update it. This preserves every party edited in the app and makes reruns idempotent.

Within a single run, source priority is:

1. Buyer master for buyer licence data;
2. GST Purpose for buyer GSTIN evidence;
3. October buyers for names absent from the first two sources;
4. Kisaan Buying Receipts for farmers.

Rows merge in memory before any write. Conflicting non-empty values go to review; no value silently wins except where the source priority above explicitly applies.

### Output

Stdout contains counts only:

- farmer candidates;
- buyer candidates;
- new parties;
- existing parties;
- skipped rows;
- review rows;
- apply or dry-run status.

The private review CSV includes source filename, source row number, issue code, and the original values needed by Vivek to resolve the issue. Its path is printed, but its contents are not.

Apply mode writes all new parties with a fresh `updated_at`, server sequence values from the existing sync mechanism, and no transaction history. Any validation or database error rolls back the full run.

## Import tests

Tests use invented names and numbers. They cover:

- dry run performs no writes;
- apply creates farmers and buyers in one transaction;
- rerun creates no duplicates and changes no existing party;
- invalid and placeholder phones become null;
- duplicate phones create one phone owner and a review row;
- same name in different villages remains separate;
- GSTIN validation, invalid-GSTIN review, and GSTIN-first buyer matching;
- buyer licence source priority;
- missing headers, oversized files, row limits, bad encoding, symlinks, and paths outside the import directory;
- formula neutralization in the private review CSV;
- rollback on a row or database failure;
- sync pull delivers imported fields to a fresh client database.

## Threat model

| Boundary | Risk | Control |
| --- | --- | --- |
| CSV file to importer | malformed rows, oversized input, formula content | size and row caps, strict headers, standard CSV parser, review-cell neutralization |
| CLI to filesystem | path traversal or special files | resolved allowlisted directory, regular-file check, symlink rejection |
| Importer to database | wrong shop, partial writes, duplicates | shop existence check, tenant-scoped queries, one transaction, deterministic identity |
| Logs and reports | personal-data disclosure | counts-only stdout, private review file, no raw error body |
| Old client sync | clearing new server fields | omission-aware patch semantics |
| New client sync | cross-shop modification | existing authenticated shop scoping and isolation tests |

## Independent math review

The review remains separate from Claude's implementation.

1. Before reading Claude's diff, recompute every golden vector from the written sheet rules in a disposable script outside the repository. Real rows stay private; the report uses vector numbers and numeric inputs only.
2. Check the boundary cases: exactly 5 kg extra, above 5 kg, whole-rupee gross floor, nearest-Rs-10 half-up behavior, per-row commission rate, advance after rounding, and commission absorbing the rounding difference.
3. Recompute both sell modes. Bill of Supply keeps paise and includes arat, shulk, and tulak. Cash Memo excludes shulk, floors the final to a whole rupee, and stores tulak as the residual.
4. Read Claude's tests and production diff only after the independent results are frozen.
5. Compare every result, then inspect integer overflow, rounding order, editable defaults, multi-lot aggregation, slip display, 6R and 9R prices, day-page values, and party balance.
6. Report mismatches and missing coverage. Do not rewrite Claude's branch during the independent review.

The math review cannot finish until Claude publishes M1. Schema and importer work may proceed in parallel.

## Delivery order

1. Implement and verify party schema plus sync.
2. Integrate the buyer UI after the schema branch is available.
3. Build the importer after the real CSV headers are available; unit tests may start from invented fixtures earlier.
4. Review Claude's math after M1 is pushed.
5. Merge only after schema, import, UI, and math review gates pass.
6. Build the pilot APK and run the physical-device parity rehearsal.
7. At the shop, back up staging, dry-run the real files, let Vivek inspect counts and the private review report, then apply.

## Acceptance criteria

- Existing schema-v7 data upgrades to v8 without loss.
- GSTIN and mandi licence round-trip through client, backend, and sync.
- Old client payloads cannot erase either field.
- Buyer UI validates and persists both fields without exposing them on farmer forms.
- Bill GSTIN prefill works and existing bills remain unchanged after party edits.
- Import dry run writes nothing and reveals no personal data in stdout.
- Apply is atomic, tenant-scoped, idempotent, and preserves existing app-edited parties.
- Fresh-phone sync pull receives imported parties and both new fields.
- Independent math calculations agree with the accepted sheet rules, or every mismatch blocks the pilot merge.
- Full Gradle and backend gates pass, NGDL token tests pass, and device checks cover both themes at 390px.

## Open inputs, not design gaps

- Vivek must export the four sheet tabs into `~/mandisamiti-import/`.
- The exported headers finalize the four adapter alias lists.
- The target shop ID is available only after the shop's first login.
- The shop decides whether old balances will be imported in a later project. This design starts every imported party at zero.
