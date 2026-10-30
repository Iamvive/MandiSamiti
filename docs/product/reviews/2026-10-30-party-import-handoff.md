# Party tax fields and import handoff

Date: 2026-10-30

Branch: `feat/party-gst`

## Delivered

- Client schema version 8 adds nullable `gstin` and `mandi_license` party columns.
- Backend Alembic revision `0002_party_tax_fields` adds matching nullable columns.
- Push and pull sync carry both values.
- An old payload that omits the new keys preserves server values. Explicit null clears them.
- `backend/scripts/import_parties.py` parses the four private CSV export types and defaults to dry run.
- Apply mode locks the shop, allocates normal server sequence values, and creates all missing masters in one transaction.
- Reruns match deterministic IDs and natural keys. Existing app records are never updated.
- A cursor-zero pull test proves that imported parties reach a fresh client.

## Command shape

Keep all source and review files in `~/mandisamiti-import/`.

```bash
cd backend
PYTHONPATH=. .venv/bin/python scripts/import_parties.py \
  --farmers ~/mandisamiti-import/kisaan-buying-receipts.csv \
  --buyers ~/mandisamiti-import/buyer.csv \
  --gst-purpose ~/mandisamiti-import/gst-purpose.csv \
  --october-buyers ~/mandisamiti-import/october-2026.csv
```

This performs a dry run. It prints counts and the private review-file path. It does not open a database session.

Apply only after the dry-run counts and review CSV are approved:

```bash
PYTHONPATH=. .venv/bin/python scripts/import_parties.py \
  --farmers ~/mandisamiti-import/kisaan-buying-receipts.csv \
  --buyers ~/mandisamiti-import/buyer.csv \
  --gst-purpose ~/mandisamiti-import/gst-purpose.csv \
  --october-buyers ~/mandisamiti-import/october-2026.csv \
  --shop-id '<shop-id>' \
  --apply
```

## Count meanings

- `farmer_candidates`: distinct farmer masters after in-memory matching.
- `buyer_candidates`: distinct buyer masters after in-memory matching.
- `new_parties`: rows that would be created in dry run, or were created in apply mode.
- `existing_parties`: apply candidates already represented by a deterministic ID or natural key.
- `skipped_rows`: source rows without a usable name.
- `review_rows`: issues requiring human inspection.

Stdout contains no party fields. The review CSV is mode `0600`, and formula-like cells are neutralized.

## Review codes

- `missing_name`
- `invalid_phone`
- `invalid_gstin`
- `mandi_license_too_long`
- `duplicate_phone`
- `conflicting_name`
- `conflicting_phone`
- `conflicting_village`
- `conflicting_gstin`
- `conflicting_mandi_license`

The importer never guesses a corrected phone, GSTIN, licence, or identity.

## Required staging sequence

1. Complete the shop's first login so the target shop exists.
2. Back up staging with `backend/scripts/backup_staging.sh`.
3. Run dry mode against the private exports.
4. Have Vivek inspect all counts and the private review CSV.
5. Run the same command with `--shop-id` and `--apply`.
6. Run it a second time and confirm `new_parties=0`.
7. Sign in on a fresh client and confirm parties plus tax fields arrive through sync pull.

Real import remains blocked until the private exports, target shop ID, staging backup, and Vivek's review approval are all present.
