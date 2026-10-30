# MandiSamiti Database Migrations (Alembic)

## Overview
This directory contains Alembic database migrations for the MandiSamiti backend.

- Baseline revision: `0001_baseline`
- Matches current models across all 13 tables, including:
  - `lend_settlements`: `principal_paisa`, `interest_paisa`, `calculated_interest_paisa`
  - `buy_entries`, `sell_entries`, `lend_entries`: `slip_no`

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

### 2. Existing Staging Database Stamp Path
For an existing database where tables were previously created via `Base.metadata.create_all`:
**DO NOT run `create_all`. Stamp the baseline so Alembic tracks future migrations without attempting to recreate existing tables:**
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

