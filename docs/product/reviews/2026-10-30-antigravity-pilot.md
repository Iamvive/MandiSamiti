# Task B1 Handoff: Backend Alembic Baseline

- **Agent:** Antigravity (Gemini 3.8 Flash)
- **Date:** 2026-10-30
- **Branch:** `feat/pilot-backend-baseline`
- **Worktree:** `projects/MandiSamiti-antigravity`

---

## 1. What was done
1. **Added Alembic configuration**:
   - `backend/alembic.ini` configured with POSIX script paths.
   - `backend/migrations/env.py` configured with async engine runner using `run_sync`, dynamically sourcing `settings.DATABASE_URL` (or override via `-x` / test configs) and SQLite batch mode (`render_as_batch=True`).
   - `backend/migrations/script.py.mako` template initialized.
   - `backend/requirements.txt` already contained `alembic>=1.13.0`.
2. **Created baseline migration `0001_baseline`** (`backend/migrations/versions/0001_baseline.py`):
   - Generated against clean empty schema and verified by hand against all SQLAlchemy models in `app/models/`.
   - Exactly matches all 13 tables:
     - `shop_profiles`
     - `users`
     - `refresh_tokens`
     - `parties`
     - `deals`
     - `buy_entries` (includes `slip_no` BIGINT)
     - `sell_entries` (includes `slip_no` BIGINT)
     - `lend_entries` (includes `slip_no` BIGINT)
     - `lend_settlements` (includes `principal_paisa`, `interest_paisa`, `calculated_interest_paisa` BIGINTs with server default 0)
     - `cash_transactions`
     - `expense_entries`
     - `entry_revisions`
     - `sync_conflicts`
   - Complete `downgrade()` implementation that drops all tables and indexes cleanly.
3. **Removed `create_all` from production startup**:
   - In `backend/app/main.py`, replaced `conn.run_sync(Base.metadata.create_all)` in `lifespan` with `conn.execute(text("SELECT 1"))` connection verification with retry loop.
   - Preserves `create_all` in `tests/conftest.py` and test modules for in-memory SQLite fixtures.
4. **Added automated migration tests**:
   - `backend/tests/test_migrations.py`:
     - Upgrades empty SQLite DB to `head`.
     - Asserts database tables and columns match `Base.metadata` table and column sets exactly.
     - Asserts presence of split columns (`principal_paisa`, `interest_paisa`, `calculated_interest_paisa`) and `slip_no` columns.
     - Downgrades back to `base` and asserts all application tables are dropped.
   - Pytest suite: 102 passed (101 existing + 1 migration test).
5. **Added migration runner and wired into deployment**:
   - `backend/scripts/migrate.sh`: runs `alembic upgrade head` with up to 15 retries (waiting for Postgres readiness).
   - `backend/scripts/deploy_staging.sh`: Step 3 updated to build images, execute `docker compose --env-file .env.staging -f docker-compose.staging.yml run --rm api bash scripts/migrate.sh`, and then start containers.

---

## 2. Exact Commands for Claude on VPS

### A. If keeping existing Staging DB (created with `create_all`):
Run `alembic stamp` so Alembic records `0001_baseline` without attempting to recreate existing tables:
```bash
ssh appworx-core-vps "cd /opt/mandisamiti-staging && docker compose --env-file .env.staging -f docker-compose.staging.yml run --rm api alembic stamp 0001_baseline"
```

### B. If resetting Staging DB (per S3 Step 1 reset go-ahead):
After taking `pg_dump` backup, run migration to head:
```bash
ssh appworx-core-vps "cd /opt/mandisamiti-staging && docker compose --env-file .env.staging -f docker-compose.staging.yml run --rm api alembic upgrade head"
```

---

## 3. Verification Gate Results
1. **Kotlin / Gradle Gate**:
   ```bash
   ./gradlew :core-database:jvmTest :core-data:jvmTest jvmTest :composeApp:assembleDebug
   ```
   **Result:** `BUILD SUCCESSFUL in 3s` (117 actionable tasks, 117 up-to-date).

2. **Backend Pytest Gate**:
   ```bash
   cd backend && PYTHONPATH=. venv/bin/pytest -q
   ```
   **Result:** `102 passed, 4 warnings in 13.99s`.

