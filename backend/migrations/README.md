# MandiSamiti Database Migrations (Alembic)

## Overview
This directory contains Alembic database migrations for the MandiSamiti backend.

- Baseline revision: `0001_baseline`
- On 2026-11-19 the old chain `0002` through `0008` was folded into this one file. An empty database reaches today's schema with `alembic upgrade head`.
- Matches current models across all 13 tables, including:
  - `lend_settlements`: `principal_paisa`, `interest_paisa`, `calculated_interest_paisa`
  - `buy_entries`, `sell_entries`, `lend_entries`: `slip_no`
  - `buy_entries`, `sell_entries`: `is_khaate`
  - `lend_entries`: `direction`, `kind`, `receiver_name`, `receiver_phone`
  - `cash_transactions`: `lend_id`

A database whose `alembic_version` is still `0008_udhaar_fields` already has this schema. Stamp it `0001_baseline` before the next upgrade. Do not run `upgrade` on that database until the stamp. This repository change does not stamp staging.

## Commands

### 1. New / Empty Database Setup
To initialize a fresh database from scratch to the latest schema:
```bash
alembic upgrade head
```
Or via script:
```bash
bash scripts/migrate.sh
```

### 2. Existing database stamp path
For a database that already has the full schema, including one stamped `0008_udhaar_fields` or one created with `Base.metadata.create_all`:
**Do not run `upgrade` until the version row says `0001_baseline`. Stamp it so Alembic does not try to recreate the tables:**
```bash
alembic stamp 0001_baseline
```
Inside the staging container:
```bash
docker compose --env-file .env.staging -f docker-compose.staging.yml run --rm api alembic stamp 0001_baseline
```

### 3. Adding New Migrations
When changing models in `app/models/`:
```bash
alembic revision --autogenerate -m "describe_change"
```
Review the generated file in `migrations/versions/`, verify upgrade and downgrade methods, and test with `pytest tests/test_migrations.py`.

