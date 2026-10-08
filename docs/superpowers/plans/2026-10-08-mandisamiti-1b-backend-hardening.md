# MandiSamiti Phase 1B Implementation Plan — Backend Hardening & Data Contract Alignment

> **Goal:** Harden the MandiSamiti FastAPI + PostgreSQL backend to achieve strict multi-tenant isolation, paisa-exact integer schema matching the KMP client (Schema v2), append-only entry revisions sync, secure environment secret management, and deterministic test coverage.
>
> **Architecture Standard:** NoGravity Design Language (NGDL v1.2), Cross-AI One-Brain Protocol, Append-Only Ledger Contract.
>
> **Target Package:** `com.appwork.mandisamiti` (Client) & `backend/app/` (FastAPI Server)

---

## 1. Context & Baseline Audit

From our product & code review (`vaults/Vivek-K/wiki/concepts/mandisamiti-product-review-2026-10-08.md`), the backend currently has 4 critical security and correctness vulnerabilities:
1. **Cross-Tenant Overwrites:** `sync_push` queries rows by `id == p_id` without filtering by `shop_id == current_user.shop_id`.
2. **Schema & Money Math Mismatch:** Server stores money as `Float` rupees, while the client uses exact 64-bit integer `paisa` (`Long`) and basis points (`farmerCommissionBps`).
3. **Missing Schema v2 Features:** Server lacks `entry_revisions` table, vernacular `void_reason`, `is_void` flags, `deal_date_ms`, and `cut_per_bag_grams`.
4. **Hardcoded Secrets & In-Memory OTP:** DB passwords and JWT secrets are hardcoded in compose files; OTP is stored in an un-persisted dict without rate limiting.

---

## 2. Proposed Architecture & Data Contract

### 2.1 Integer Paisa / Gram Schema (PostgreSQL & SQLite Parity)

| Client Model (KMP) | SQLite v2 Column | Backend Model (SQLAlchemy) | PostgreSQL Type | Notes |
|---|---|---|---|---|
| `Deal.farmerCommissionBps` | `farmer_commission_bps` | `Deal.farmer_commission_bps` | `Integer` | Basis points (250 = 2.50%) |
| `Deal.ratePaisa` | `rate_paisa` | `Deal.rate_paisa` | `BigInteger` | Paisa per quintal |
| `Deal.cutPerBagGrams` | `cut_per_bag_grams` | `Deal.cut_per_bag_grams` | `Integer` | Grams deduction per bag |
| `Deal.grossWeightGrams` | `gross_weight_grams` | `Deal.gross_weight_grams` | `BigInteger` | Exact grams |
| `Deal.netWeightGrams` | `net_weight_grams` | `Deal.net_weight_grams` | `BigInteger` | Exact grams |
| `Deal.farmerTotalPaisa` | `net_farmer_payable_paisa`| `Deal.net_farmer_payable_paisa`| `BigInteger` | Signed (can be negative) |
| `Deal.buyerTotalPaisa` | `net_buyer_receivable_paisa`|`Deal.net_buyer_receivable_paisa`|`BigInteger`| Gross + Mandi Cess + Comm |
| `CashTx.amountPaisa` | `amount_paisa` | `CashTransaction.amount_paisa`| `BigInteger` | Exact paisa |
| `EntryRevision` | `entryRevisionEntity` | `EntryRevision` | Table | Append-only audit history |

---

## 3. Implementation Tasks & Verification

### Task 1: Environment & Secret Hardening
**Files:**
- Modify: `backend/app/config.py`
- Modify: `backend/.env.example`
- Modify: `backend/docker-compose.yml`
- Modify: `backend/docker-compose.staging.yml`
- Modify: `backend/docker-compose.prod.yml`

- [ ] **Step 1:** Update `config.py` using `pydantic_settings.BaseSettings` with strict env loading: `SECRET_KEY`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `POSTGRES_HOST`, `POSTGRES_PORT`.
- [ ] **Step 2:** Remove default hardcoded passwords from `docker-compose*.yml` files and wire them strictly via `.env`.
- [ ] **Step 3:** Commit:
  ```bash
  git commit -m "sec(backend): remove hardcoded secrets and load configuration from environment"
  ```

---

### Task 2: Exact Paisa & Schema v2 Models (SQLAlchemy)
**Files:**
- Modify: `backend/app/models/deal.py`
- Modify: `backend/app/models/party.py`
- Modify: `backend/app/models/transaction.py`
- Create: `backend/app/models/revision.py`
- Modify: `backend/app/models/__init__.py`

- [ ] **Step 1:** Update `Party` model: replace `current_balance` (Float) with `balance_paisa` (`BigInteger`, default 0).
- [ ] **Step 2:** Update `Deal` model: replace Float fields with exact `BigInteger` (`rate_paisa`, `gross_weight_grams`, `net_weight_grams`, `gross_crop_value_paisa`, `labour_charges_paisa`, `bardana_charges_paisa`, `advance_paisa`, `net_farmer_payable_paisa`, `net_buyer_receivable_paisa`, `deal_date_ms`, `farmer_commission_bps`, `cut_per_bag_grams`, `is_void`, `void_reason`).
- [ ] **Step 3:** Update `CashTransaction` model: replace `amount` with `amount_paisa` (`BigInteger`), `transaction_date_ms`, `is_void`, `void_reason`, `payment_mode`.
- [ ] **Step 4:** Create `EntryRevision` model: `id`, `shop_id`, `entry_id`, `entry_type`, `revision_number`, `operation_type`, `payload_json`, `void_reason`, `created_at_ms`.
- [ ] **Step 5:** Commit:
  ```bash
  git commit -m "feat(backend): schema v2 models with exact integer paisa and revision entity"
  ```

---

### Task 3: Pydantic Schemas Alignment
**Files:**
- Modify: `backend/app/schemas/deal.py`
- Modify: `backend/app/schemas/party.py`
- Modify: `backend/app/schemas/transaction.py`
- Modify: `backend/app/schemas/sync.py`
- Create: `backend/app/schemas/revision.py`

- [ ] **Step 1:** Align all request/response schemas to consume and produce 64-bit integer paisa/grams.
- [ ] **Step 2:** Add `SyncRevisionItem` to `SyncPushRequest` and `SyncPullResponse`.
- [ ] **Step 3:** Commit:
  ```bash
  git commit -m "feat(backend): pydantic schemas aligned with KMP integer paisa data contract"
  ```

---

### Task 4: Multi-Tenant Tenant-Isolated Sync Engine
**Files:**
- Modify: `backend/app/api/v1/endpoints/sync.py`
- Create: `backend/tests/test_sync_isolation.py`

- [ ] **Step 1: Write multi-tenant isolation tests:**
  - Test that Shop A pushing an entry with `id="tx-1"` cannot overwrite Shop B's `tx-1`.
  - Test that Shop A pulling data receives ONLY Shop A's records.
- [ ] **Step 2: Update `sync_push` in `sync.py`:**
  - Add `where(Model.id == item_id, Model.shop_id == shop_id)` to all lookup queries.
  - If a record exists under another shop, raise `403 Forbidden` or reject the overwrite.
  - Insert incoming `EntryRevision` records into the server audit log.
- [ ] **Step 3: Update `sync_pull` in `sync.py`:**
  - Pull deals, parties, transactions, and revisions scoped strictly by `shop_id` and cursor `updated_at > since`.
- [ ] **Step 4:** Run tests and commit:
  ```bash
  git commit -m "fix(backend): enforce strict shop_id tenant isolation in sync push and pull"
  ```

---

### Task 5: Auth Hardening & Rate-Limited OTP Store
**Files:**
- Modify: `backend/app/api/v1/endpoints/auth.py`
- Modify: `backend/app/services/otp_service.py` (or create if needed)
- Create: `backend/tests/test_auth_security.py`

- [ ] **Step 1:** Hash MPIN with `bcrypt` / `argon2` instead of plaintext storage.
- [ ] **Step 2:** Add rate limiting to OTP request (max 3 per 5 minutes per phone) and verify attempts (max 5 failed attempts before lockout).
- [ ] **Step 3:** Commit:
  ```bash
  git commit -m "sec(backend): bcrypt MPIN hashing and rate-limited OTP verification"
  ```

---

## 4. Acceptance Criteria & Definition of Done
1. `pytest backend/tests` passes 100% with zero errors.
2. Tenant isolation test proves cross-shop overwrites are strictly rejected.
3. Sync payload schema matches client `1.sqm` SQLite v2 columns byte-for-byte.
4. Zero plaintext MPINs or credentials stored in repository or database.
5. Vault notes & sync-log updated.
