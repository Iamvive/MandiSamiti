# MandiSamiti Complete Revamp Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Buy, Sell, Lend, Expense and Stock Inventory to MandiSamiti with full parity with the reference Mandi app, keeping paisa-exact math, append-only revisions, NGDL UI and offline-first sync.

**Spec:** `docs/superpowers/specs/2026-10-27-mandisamiti-revamp-design.md` (untracked at planning time; commit it with Task 1). Brief: `/Users/appworx/Desktop/Vivek-K/docs/handoffs/2026-10-27-mandisamiti-revamp-brief.md`.

**Architecture:** Four new entity tables (`buyEntryEntity`, `sellEntryEntity`, `lendEntryEntity` plus `lendSettlementEntity`, `expenseEntryEntity`) added by `3.sqm`. The existing `dealEntity` stays untouched because it requires a `farmer_id` and models a two-stage auction. Pure math objects (`BuySellMath`) sit beside `MandiMathEngine` and reuse its `grossAmountPaisa` and `percentageOf`. Each repository writes the row and an `entryRevisionEntity` row in one DB transaction, the same way `OfflineFirstDealRepository` does. Sync reuses `entryRevisionEntity.entry_kind` (free text) with new kinds `BUY`, `SELL`, `LEND`, `EXPENSE`, and the backend gets matching tables behind the existing `/api/v1/sync/push` and `/pull`.

**Tech Stack:** Kotlin 2.0.20 KMP, SQLDelight 2.0.2 (SQLite 3.18 dialect, no UPSERT), Compose Multiplatform, kotlin.test, FastAPI + SQLAlchemy async, pytest-asyncio.

## Global Constraints
- **Branch:** `feat/mandisamiti-revamp` in `projects/MandiSamiti`. Commit there. Never push without Vivek's approval.
- **Commit messages** end with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>` when Claude Code commits. Other runtimes use their own trailer.
- **Money:** `Long` paisa only. Weight: `Long` grams only. Rates per quintal in paisa. Commission, Arat: basis points. No `Double`/`BigDecimal` in money paths. Rounding is half-up, via `MandiMathEngine.grossAmountPaisa` and `MandiMathEngine.percentageOf`.
- **Schema:** do NOT edit `1.sqm`, `2.sqm` or any existing `CREATE TABLE` in `AppDatabase.sq`. Add `3.sqm`, new tables and new queries only. `./gradlew :core-database:verifyCommonMainAppDatabaseMigration` must pass.
- **Audit:** every create, edit and void writes an `entryRevisionEntity` row in the same transaction. Voids are soft (`is_void = 1` plus reason), never deletes.
- **UI (NGDL):** colours from `ui/theme/Color.kt` only, no hardcoded hex; contrast >= 4.5:1 in light and dark; cards use elevation; vector icons only, no emoji; no horizontal scroll at 390dp; touch targets >= 48dp; user-facing text is Hindi with English second line where the existing screens do that.
- **Tests:** every task ends green. App: `./gradlew :<module>:jvmTest` (`:core-domain:jvmTest` for commonTest). Backend: `cd backend && .venv/bin/python -m pytest -q`.
- **Secrets:** never print, log or commit any key, token or `.env` value. Telegram uses the existing Keychain-backed `scripts/notify_telegram.py` at the hub root.
- **Out of scope:** adopting `packages/ngdl-compose` into composeApp; GST invoicing; iOS-only polish; staff roles; converting legacy `dealEntity` rows into Buy/Sell rows.

## Runtime Ownership Matrix

| Phase | Tasks | Runtime | Hands off to |
|---|---|---|---|
| A. Contracts and schema | 1-5 | Claude Code | Codex (signatures frozen after Task 3), Gemini (after Task 4) |
| B. Backend sync | 6-7 | Claude Code | Codex (Task 10) |
| C. Tests | 8-10 | Codex / OpenCode | Claude Code |
| D. UI | 11-16 | Gemini / Antigravity | Claude Code |
| E. Build, alerts, release | 17-18 | Claude Code | Vivek |

**Order:** 1 -> 2 -> 3 -> (8 in parallel with 4) -> 4 -> 5 -> (9, 6, 11 in parallel) -> 7 -> 10 -> 12-15 -> 16 -> 17 -> 18. Claude Code runs Task 17 after the end of each phase.

## File Structure

Create (all under `projects/MandiSamiti`):
- `core-database/src/commonMain/sqldelight/com/appwork/mandisamiti/database/3.sqm`
- `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/math/BuySellMath.kt`
- `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/{BuyEntry,SellEntry,LendEntry,LendSettlement,ExpenseEntry,StockLine}.kt`
- `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/repository/{Buy,Sell,Lend,Expense}Repository.kt`
- `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/usecase/{BuyTransactionUseCase,SellTransactionUseCase,LendTransactionUseCase,ExpenseUseCase,StockInventoryUseCase}.kt`
- `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/repository/OfflineFirst{Buy,Sell,Lend,Expense}Repository.kt`
- `core-domain/src/commonTest/kotlin/com/appwork/mandisamiti/domain/{BuySellMathTest,StockInventoryUseCaseTest}.kt`
- `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/{BuyEntryRepositoryTest,SellEntryRepositoryTest,LendRepositoryTest}.kt`
- `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/{buy,sell,lend,expense,stock}/*`
- `backend/app/models/{buy_entry,sell_entry,lend_entry,expense_entry}.py`, `backend/app/schemas/entries.py`, `backend/tests/test_sync_v2.py`

Modify: `AppDatabase.sq` (append only), `MandiMathEngine.kt` (add `parseKgToGrams`), `core-data/.../sync/*` (new kinds), `backend/app/api/v1/endpoints/sync.py`, `composeApp/.../App.kt`, `ui/home/HomeScreen.kt`, `ui/dashboard/DashboardScreen.kt`.

---

## Phase A: Contracts and schema (Claude Code)

### Task 1: Migration 3 and SQL queries

**Files:**
- Create: `core-database/src/commonMain/sqldelight/com/appwork/mandisamiti/database/3.sqm`
- Modify: `core-database/src/commonMain/sqldelight/com/appwork/mandisamiti/database/AppDatabase.sq` (append new tables and queries before the final `shopProfileExists` query is fine; do not touch existing statements)
- Test: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/BuyEntryRepositoryTest.kt` is written in Task 5; this task is verified by the migration check.

**Interfaces:**
- Consumes: nothing.
- Produces: generated query classes `AppDatabaseQueries` methods `insertBuyEntry`, `getBuyEntriesByShop`, `getBuyEntryById`, `insertSellEntry`, `getSellEntriesByShop`, `getSellEntryById`, `insertLendEntry`, `getLendEntriesByShop`, `getLendEntryById`, `insertLendSettlement`, `getLendSettlementsByLend`, `insertExpenseEntry`, `getExpensesByShop`, `getExpenseById`, `getStockBuyTotals`, `getStockSellTotals`, `getPending*`, `mark*Synced`.

- [ ] **Step 1: Write `3.sqm`**

```sql
CREATE TABLE buyEntryEntity (
    id TEXT PRIMARY KEY NOT NULL,
    shop_id TEXT NOT NULL,
    farmer_id TEXT NOT NULL,
    commodity_id TEXT NOT NULL,
    entry_date INTEGER NOT NULL,
    bags_count INTEGER NOT NULL,
    weight_per_bag_grams INTEGER NOT NULL,
    extra_weight_grams INTEGER NOT NULL DEFAULT 0,
    total_weight_grams INTEGER NOT NULL,
    rate_paisa_per_quintal INTEGER NOT NULL,
    commission_bps INTEGER NOT NULL DEFAULT 0,
    labour_per_bag_paisa INTEGER NOT NULL DEFAULT 0,
    advance_paid_paisa INTEGER NOT NULL DEFAULT 0,
    gross_paisa INTEGER NOT NULL,
    commission_paisa INTEGER NOT NULL,
    labour_paisa INTEGER NOT NULL,
    net_payable_paisa INTEGER NOT NULL,
    paid_paisa INTEGER NOT NULL DEFAULT 0,
    vehicle_number TEXT,
    slip_number TEXT,
    remarks TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    sync_status INTEGER NOT NULL DEFAULT 0,
    revision INTEGER NOT NULL DEFAULT 1,
    is_void INTEGER NOT NULL DEFAULT 0,
    void_reason TEXT,
    FOREIGN KEY(farmer_id) REFERENCES partyEntity(id),
    FOREIGN KEY(commodity_id) REFERENCES commodityEntity(id)
);
CREATE INDEX idx_buy_shop_date ON buyEntryEntity(shop_id, entry_date);
CREATE INDEX idx_buy_farmer ON buyEntryEntity(farmer_id);

CREATE TABLE sellEntryEntity (
    id TEXT PRIMARY KEY NOT NULL,
    shop_id TEXT NOT NULL,
    buyer_id TEXT NOT NULL,
    commodity_id TEXT NOT NULL,
    entry_date INTEGER NOT NULL,
    bill_mode TEXT NOT NULL,          -- 'NORMAL' | 'BILL_OF_SUPPLY'
    party_kind TEXT NOT NULL,         -- 'MERCHANT' | 'CUSTOMER'
    gstin TEXT,
    bags_count INTEGER NOT NULL,
    weight_per_bag_grams INTEGER NOT NULL,
    total_weight_grams INTEGER NOT NULL,
    rate_paisa_per_quintal INTEGER NOT NULL,
    arat_bps INTEGER NOT NULL DEFAULT 0,
    tulak_paisa INTEGER NOT NULL DEFAULT 0,
    gross_paisa INTEGER NOT NULL,
    arat_paisa INTEGER NOT NULL,
    mandi_shulk_paisa INTEGER NOT NULL,
    total_receivable_paisa INTEGER NOT NULL,
    received_paisa INTEGER NOT NULL DEFAULT 0,
    balance_due_paisa INTEGER NOT NULL,
    vehicle_number TEXT,
    invoice_number TEXT,
    remarks TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    sync_status INTEGER NOT NULL DEFAULT 0,
    revision INTEGER NOT NULL DEFAULT 1,
    is_void INTEGER NOT NULL DEFAULT 0,
    void_reason TEXT,
    FOREIGN KEY(buyer_id) REFERENCES partyEntity(id),
    FOREIGN KEY(commodity_id) REFERENCES commodityEntity(id)
);
CREATE INDEX idx_sell_shop_date ON sellEntryEntity(shop_id, entry_date);
CREATE INDEX idx_sell_buyer ON sellEntryEntity(buyer_id);

CREATE TABLE lendEntryEntity (
    id TEXT PRIMARY KEY NOT NULL,
    shop_id TEXT NOT NULL,
    party_id TEXT NOT NULL,
    lend_type TEXT NOT NULL,          -- 'MONEY' | 'GRAIN'
    entry_date INTEGER NOT NULL,
    amount_paisa INTEGER NOT NULL DEFAULT 0,
    commodity_id TEXT,
    bags_count INTEGER NOT NULL DEFAULT 0,
    weight_grams INTEGER NOT NULL DEFAULT 0,
    expected_return_date INTEGER,
    notes TEXT,
    status TEXT NOT NULL DEFAULT 'OPEN', -- 'OPEN' | 'PARTIAL' | 'SETTLED'
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    sync_status INTEGER NOT NULL DEFAULT 0,
    revision INTEGER NOT NULL DEFAULT 1,
    is_void INTEGER NOT NULL DEFAULT 0,
    void_reason TEXT,
    FOREIGN KEY(party_id) REFERENCES partyEntity(id)
);
CREATE INDEX idx_lend_shop_status ON lendEntryEntity(shop_id, status);

CREATE TABLE lendSettlementEntity (
    id TEXT PRIMARY KEY NOT NULL,
    shop_id TEXT NOT NULL,
    lend_id TEXT NOT NULL,
    settled_at INTEGER NOT NULL,
    amount_paisa INTEGER NOT NULL DEFAULT 0,
    weight_grams INTEGER NOT NULL DEFAULT 0,
    bags_count INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    sync_status INTEGER NOT NULL DEFAULT 0,
    revision INTEGER NOT NULL DEFAULT 1,
    is_void INTEGER NOT NULL DEFAULT 0,
    void_reason TEXT,
    FOREIGN KEY(lend_id) REFERENCES lendEntryEntity(id)
);
CREATE INDEX idx_settle_lend ON lendSettlementEntity(lend_id);

CREATE TABLE expenseEntryEntity (
    id TEXT PRIMARY KEY NOT NULL,
    shop_id TEXT NOT NULL,
    entry_date INTEGER NOT NULL,
    category TEXT NOT NULL,           -- 'HAMMALI' | 'KIRAYA' | 'BIJLI' | 'CHAI_PANI' | 'MAJDOORI' | 'OTHER'
    amount_paisa INTEGER NOT NULL,
    paid_to TEXT,
    payment_mode TEXT NOT NULL DEFAULT 'CASH', -- 'CASH' | 'ONLINE' | 'CHEQUE'
    remarks TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    sync_status INTEGER NOT NULL DEFAULT 0,
    revision INTEGER NOT NULL DEFAULT 1,
    is_void INTEGER NOT NULL DEFAULT 0,
    void_reason TEXT
);
CREATE INDEX idx_expense_shop_date ON expenseEntryEntity(shop_id, entry_date);
```

- [ ] **Step 2: Append the same five `CREATE TABLE`/`CREATE INDEX` blocks to `AppDatabase.sq`** (SQLDelight needs the full schema in the `.sq` file as well as the migration). Insert them directly above the line `-- ================= QUERIES =================`.

- [ ] **Step 3: Append queries to the end of `AppDatabase.sq`**

```sql
-- ===== Buy / Sell / Lend / Expense (migration 3) =====
getBuyEntriesByShop:
SELECT * FROM buyEntryEntity WHERE shop_id = ? AND is_deleted = 0 ORDER BY entry_date DESC;

getBuyEntryById:
SELECT * FROM buyEntryEntity WHERE id = ?;

insertBuyEntry:
INSERT OR REPLACE INTO buyEntryEntity(
    id, shop_id, farmer_id, commodity_id, entry_date, bags_count, weight_per_bag_grams, extra_weight_grams,
    total_weight_grams, rate_paisa_per_quintal, commission_bps, labour_per_bag_paisa, advance_paid_paisa,
    gross_paisa, commission_paisa, labour_paisa, net_payable_paisa, paid_paisa, vehicle_number, slip_number, remarks,
    created_at, updated_at, is_deleted, sync_status, revision, is_void, void_reason
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

getSellEntriesByShop:
SELECT * FROM sellEntryEntity WHERE shop_id = ? AND is_deleted = 0 ORDER BY entry_date DESC;

getSellEntryById:
SELECT * FROM sellEntryEntity WHERE id = ?;

insertSellEntry:
INSERT OR REPLACE INTO sellEntryEntity(
    id, shop_id, buyer_id, commodity_id, entry_date, bill_mode, party_kind, gstin, bags_count, weight_per_bag_grams,
    total_weight_grams, rate_paisa_per_quintal, arat_bps, tulak_paisa, gross_paisa, arat_paisa, mandi_shulk_paisa,
    total_receivable_paisa, received_paisa, balance_due_paisa, vehicle_number, invoice_number, remarks,
    created_at, updated_at, is_deleted, sync_status, revision, is_void, void_reason
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

getLendEntriesByShop:
SELECT * FROM lendEntryEntity WHERE shop_id = ? AND is_deleted = 0 ORDER BY entry_date DESC;

getLendEntryById:
SELECT * FROM lendEntryEntity WHERE id = ?;

insertLendEntry:
INSERT OR REPLACE INTO lendEntryEntity(
    id, shop_id, party_id, lend_type, entry_date, amount_paisa, commodity_id, bags_count, weight_grams,
    expected_return_date, notes, status, created_at, updated_at, is_deleted, sync_status, revision, is_void, void_reason
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

getLendSettlementsByLend:
SELECT * FROM lendSettlementEntity WHERE lend_id = ? AND is_void = 0 ORDER BY settled_at ASC;

insertLendSettlement:
INSERT OR REPLACE INTO lendSettlementEntity(
    id, shop_id, lend_id, settled_at, amount_paisa, weight_grams, bags_count,
    created_at, updated_at, sync_status, revision, is_void, void_reason
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

getExpensesByShop:
SELECT * FROM expenseEntryEntity WHERE shop_id = ? AND is_deleted = 0 ORDER BY entry_date DESC;

getExpenseById:
SELECT * FROM expenseEntryEntity WHERE id = ?;

insertExpenseEntry:
INSERT OR REPLACE INTO expenseEntryEntity(
    id, shop_id, entry_date, category, amount_paisa, paid_to, payment_mode, remarks,
    created_at, updated_at, is_deleted, sync_status, revision, is_void, void_reason
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

-- Godown stock: grams bought and sold per commodity (void and deleted rows excluded)
getStockBuyTotals:
SELECT commodity_id, COALESCE(SUM(total_weight_grams), 0) AS grams
FROM buyEntryEntity WHERE shop_id = ? AND is_deleted = 0 AND is_void = 0 GROUP BY commodity_id;

getStockSellTotals:
SELECT commodity_id, COALESCE(SUM(total_weight_grams), 0) AS grams
FROM sellEntryEntity WHERE shop_id = ? AND is_deleted = 0 AND is_void = 0 GROUP BY commodity_id;

-- Sync outbox
getPendingSyncBuyEntries:
SELECT * FROM buyEntryEntity WHERE sync_status = 0;
getPendingSyncSellEntries:
SELECT * FROM sellEntryEntity WHERE sync_status = 0;
getPendingSyncLendEntries:
SELECT * FROM lendEntryEntity WHERE sync_status = 0;
getPendingSyncLendSettlements:
SELECT * FROM lendSettlementEntity WHERE sync_status = 0;
getPendingSyncExpenses:
SELECT * FROM expenseEntryEntity WHERE sync_status = 0;

markBuyEntrySynced:
UPDATE buyEntryEntity SET sync_status = 1 WHERE id = ? AND revision = ?;
markSellEntrySynced:
UPDATE sellEntryEntity SET sync_status = 1 WHERE id = ? AND revision = ?;
markLendEntrySynced:
UPDATE lendEntryEntity SET sync_status = 1 WHERE id = ? AND revision = ?;
markLendSettlementSynced:
UPDATE lendSettlementEntity SET sync_status = 1 WHERE id = ? AND revision = ?;
markExpenseSynced:
UPDATE expenseEntryEntity SET sync_status = 1 WHERE id = ? AND revision = ?;

-- Sign-out wipe, children first
wipeLendSettlements:
DELETE FROM lendSettlementEntity;
wipeLendEntries:
DELETE FROM lendEntryEntity;
wipeBuyEntries:
DELETE FROM buyEntryEntity;
wipeSellEntries:
DELETE FROM sellEntryEntity;
wipeExpenses:
DELETE FROM expenseEntryEntity;
```

- [ ] **Step 4: Run the migration check**

Run: `./gradlew :core-database:verifyCommonMainAppDatabaseMigration :core-database:jvmTest`
Expected: PASS. If SQLDelight reports a column-count mismatch on an `insert*`, count the `?` marks against the column list (buy 28, sell 30, lend 19, settlement 13, expense 15).

- [ ] **Step 5: Commit**

```bash
git checkout -b feat/mandisamiti-revamp
git add core-database docs/superpowers/specs/2026-10-27-mandisamiti-revamp-design.md docs/superpowers/plans/2026-10-27-mandisamiti-complete-revamp.md
git commit -m "feat(db): migration 3 adds buy, sell, lend, expense tables"
```

### Task 2: Domain models and repository interfaces

**Files:**
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/{BuyEntry,SellEntry,LendEntry,LendSettlement,ExpenseEntry,StockLine}.kt`
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/repository/{Buy,Sell,Lend,Expense}Repository.kt`
- Reference for style: `domain/model/Deal.kt`, `domain/repository/DealRepository.kt`, `VoidReason`.

**Interfaces:**
- Consumes: `VoidReason` (existing).
- Produces: the types below. Names are frozen; later tasks must use them verbatim.

- [ ] **Step 1: Write the models**

```kotlin
package com.appwork.mandisamiti.domain.model

data class BuyEntry(
    val id: String,
    val shopId: String,
    val farmerId: String,
    val commodityId: String,
    val entryDate: Long,
    val bagsCount: Long,
    val weightPerBagGrams: Long,
    val extraWeightGrams: Long,
    val totalWeightGrams: Long,
    val ratePaisaPerQuintal: Long,
    val commissionBps: Long,
    val labourPerBagPaisa: Long,
    val advancePaidPaisa: Long,
    val grossPaisa: Long,
    val commissionPaisa: Long,
    val labourPaisa: Long,
    val netPayablePaisa: Long,
    val paidPaisa: Long = 0L,
    val vehicleNumber: String? = null,
    val slipNumber: String? = null,
    val remarks: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val isVoid: Boolean = false,
    val voidReason: String? = null,
    val syncStatus: Long = 0L,
) {
    val paymentStatus: PaymentStatus
        get() = when {
            paidPaisa <= 0L -> PaymentStatus.PENDING
            paidPaisa >= netPayablePaisa -> PaymentStatus.COMPLETED
            else -> PaymentStatus.PARTIAL
        }
}

enum class PaymentStatus { PENDING, PARTIAL, COMPLETED }
```

```kotlin
package com.appwork.mandisamiti.domain.model

enum class BillMode { NORMAL, BILL_OF_SUPPLY }
enum class SellPartyKind { MERCHANT, CUSTOMER }

data class SellEntry(
    val id: String,
    val shopId: String,
    val buyerId: String,
    val commodityId: String,
    val entryDate: Long,
    val billMode: BillMode,
    val partyKind: SellPartyKind,
    val gstin: String? = null,
    val bagsCount: Long,
    val weightPerBagGrams: Long,
    val totalWeightGrams: Long,
    val ratePaisaPerQuintal: Long,
    val aratBps: Long,
    val tulakPaisa: Long,
    val grossPaisa: Long,
    val aratPaisa: Long,
    val mandiShulkPaisa: Long,
    val totalReceivablePaisa: Long,
    val receivedPaisa: Long,
    val balanceDuePaisa: Long,
    val vehicleNumber: String? = null,
    val invoiceNumber: String? = null,
    val remarks: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val isVoid: Boolean = false,
    val voidReason: String? = null,
    val syncStatus: Long = 0L,
)
```

```kotlin
package com.appwork.mandisamiti.domain.model

enum class LendType { MONEY, GRAIN }
enum class LendStatus { OPEN, PARTIAL, SETTLED }

data class LendEntry(
    val id: String,
    val shopId: String,
    val partyId: String,
    val lendType: LendType,
    val entryDate: Long,
    val amountPaisa: Long = 0L,
    val commodityId: String? = null,
    val bagsCount: Long = 0L,
    val weightGrams: Long = 0L,
    val expectedReturnDate: Long? = null,
    val notes: String? = null,
    val status: LendStatus = LendStatus.OPEN,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val isVoid: Boolean = false,
    val voidReason: String? = null,
    val syncStatus: Long = 0L,
)

data class LendSettlement(
    val id: String,
    val shopId: String,
    val lendId: String,
    val settledAt: Long,
    val amountPaisa: Long = 0L,
    val weightGrams: Long = 0L,
    val bagsCount: Long = 0L,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val isVoid: Boolean = false,
    val voidReason: String? = null,
    val syncStatus: Long = 0L,
)
```

```kotlin
package com.appwork.mandisamiti.domain.model

enum class ExpenseCategory { HAMMALI, KIRAYA, BIJLI, CHAI_PANI, MAJDOORI, OTHER }
enum class ExpensePaymentMode { CASH, ONLINE, CHEQUE }

data class ExpenseEntry(
    val id: String,
    val shopId: String,
    val entryDate: Long,
    val category: ExpenseCategory,
    val amountPaisa: Long,
    val paidTo: String? = null,
    val paymentMode: ExpensePaymentMode = ExpensePaymentMode.CASH,
    val remarks: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val isVoid: Boolean = false,
    val voidReason: String? = null,
    val syncStatus: Long = 0L,
)

data class StockLine(val commodityId: String, val boughtGrams: Long, val soldGrams: Long) {
    val onHandGrams: Long get() = boughtGrams - soldGrams
}
```

- [ ] **Step 2: Write the repository interfaces**

```kotlin
package com.appwork.mandisamiti.domain.repository

import com.appwork.mandisamiti.domain.model.*
import kotlinx.coroutines.flow.Flow

interface BuyRepository {
    fun getBuyEntriesStream(shopId: String): Flow<List<BuyEntry>>
    suspend fun getBuyEntryById(id: String): BuyEntry?
    suspend fun saveBuyEntry(entry: BuyEntry)
    suspend fun editBuyEntry(entry: BuyEntry)
    suspend fun voidBuyEntry(id: String, reason: VoidReason)
}

interface SellRepository {
    fun getSellEntriesStream(shopId: String): Flow<List<SellEntry>>
    suspend fun getSellEntryById(id: String): SellEntry?
    suspend fun saveSellEntry(entry: SellEntry)
    suspend fun editSellEntry(entry: SellEntry)
    suspend fun voidSellEntry(id: String, reason: VoidReason)
}

interface LendRepository {
    fun getLendEntriesStream(shopId: String): Flow<List<LendEntry>>
    suspend fun getLendEntryById(id: String): LendEntry?
    suspend fun getSettlements(lendId: String): List<LendSettlement>
    suspend fun saveLendEntry(entry: LendEntry)
    /** Records a repayment and moves the lend to PARTIAL or SETTLED in one transaction. */
    suspend fun settle(settlement: LendSettlement)
    suspend fun voidLendEntry(id: String, reason: VoidReason)
}

interface ExpenseRepository {
    fun getExpensesStream(shopId: String): Flow<List<ExpenseEntry>>
    suspend fun getExpenseById(id: String): ExpenseEntry?
    suspend fun saveExpense(entry: ExpenseEntry)
    suspend fun voidExpense(id: String, reason: VoidReason)
    suspend fun stockTotals(shopId: String): StockTotals
}

data class StockTotals(val boughtGramsByCommodity: Map<String, Long>, val soldGramsByCommodity: Map<String, Long>)
```

`stockTotals` lives on `ExpenseRepository` only to avoid a fifth interface; it is a read over the buy/sell tables. If a reviewer rejects that, move it to a `StockRepository` in Task 5 and keep `StockTotals` unchanged.

- [ ] **Step 3: Compile**

Run: `./gradlew :core-domain:compileKotlinJvm`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add core-domain
git commit -m "feat(domain): buy, sell, lend, expense models and repository contracts"
```

### Task 3: Buy/Sell math contract

**Files:**
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/math/BuySellMath.kt`
- Modify: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/math/MandiMathEngine.kt` (add `parseKgToGrams` next to `parseQuintalsStringToGrams`)
- Test: `core-domain/src/commonTest/kotlin/com/appwork/mandisamiti/domain/BuySellMathTest.kt` (written by Codex in Task 8; Claude writes only the two worked examples below so the contract is pinned)

**Interfaces:**
- Consumes: `MandiMathEngine.grossAmountPaisa(netWeightGrams, ratePaisaPerQuintal)`, `MandiMathEngine.percentageOf(amountPaisa, basisPoints)`.
- Produces:
  - `BuySellMath.totalWeightGrams(bags: Long, weightPerBagGrams: Long, extraWeightGrams: Long): Long`
  - `BuySellMath.calculateBuy(input: BuyMathInput): BuyMathResult`
  - `BuySellMath.calculateSell(input: SellMathInput): SellMathResult`
  - `BuySellMath.defaultTulakPaisa(totalWeightGrams: Long): Long` (Rs 1 per quintal)
  - `MandiMathEngine.parseKgToGrams(input: String): Long`

- [ ] **Step 1: Write the failing test (worked examples)**

```kotlin
package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.math.*
import kotlin.test.Test
import kotlin.test.assertEquals

class BuySellMathTest {
    @Test
    fun buyWorkedExample() {
        // 10 bags x 50 kg + 5 kg loose = 505 kg; Rs 2,500/qtl; 2% commission; Rs 5/bag labour; Rs 1,000 advance
        val r = BuySellMath.calculateBuy(
            BuyMathInput(
                bags = 10, weightPerBagGrams = 50_000, extraWeightGrams = 5_000,
                ratePaisaPerQuintal = 250_000, commissionBps = 200,
                labourPerBagPaisa = 500, advancePaidPaisa = 100_000,
            )
        )
        assertEquals(505_000, r.totalWeightGrams)
        assertEquals(1_262_500, r.grossPaisa)
        assertEquals(25_250, r.commissionPaisa)
        assertEquals(5_000, r.labourPaisa)
        assertEquals(1_132_250, r.netPayablePaisa)
    }

    @Test
    fun sellBillOfSupplyWorkedExample() {
        // 20 bags x 50 kg = 1000 kg = 10 qtl; Rs 2,600/qtl; Arat 1%; Tulak default Rs 1/qtl; Mandi tax 1.5%; Rs 10,000 received
        val r = BuySellMath.calculateSell(
            SellMathInput(
                bags = 20, weightPerBagGrams = 50_000, ratePaisaPerQuintal = 260_000,
                billMode = com.appwork.mandisamiti.domain.model.BillMode.BILL_OF_SUPPLY,
                aratBps = 100, tulakPaisaOverride = null, receivedPaisa = 1_000_000,
            )
        )
        assertEquals(2_600_000, r.grossPaisa)
        assertEquals(26_000, r.aratPaisa)
        assertEquals(1_000, r.tulakPaisa)
        assertEquals(39_000, r.mandiShulkPaisa)
        assertEquals(2_666_000, r.totalReceivablePaisa)
        assertEquals(1_666_000, r.balanceDuePaisa)
    }

    @Test
    fun normalBillHasNoAdditions() {
        val r = BuySellMath.calculateSell(
            SellMathInput(
                bags = 20, weightPerBagGrams = 50_000, ratePaisaPerQuintal = 260_000,
                billMode = com.appwork.mandisamiti.domain.model.BillMode.NORMAL,
                aratBps = 100, tulakPaisaOverride = null, receivedPaisa = 0,
            )
        )
        assertEquals(2_600_000, r.totalReceivablePaisa)
        assertEquals(0, r.aratPaisa)
        assertEquals(0, r.tulakPaisa)
        assertEquals(0, r.mandiShulkPaisa)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :core-domain:jvmTest --tests "*BuySellMathTest*"`
Expected: FAIL, unresolved reference `BuySellMath`.

- [ ] **Step 3: Implement**

```kotlin
package com.appwork.mandisamiti.domain.math

import com.appwork.mandisamiti.domain.model.BillMode

data class BuyMathInput(
    val bags: Long,
    val weightPerBagGrams: Long,
    val extraWeightGrams: Long,
    val ratePaisaPerQuintal: Long,
    val commissionBps: Long,
    val labourPerBagPaisa: Long,
    val advancePaidPaisa: Long,
)

data class BuyMathResult(
    val totalWeightGrams: Long,
    val grossPaisa: Long,
    val commissionPaisa: Long,
    val labourPaisa: Long,
    /** May be negative when the advance exceeds the amount due; the UI must flag that, not hide it. */
    val netPayablePaisa: Long,
)

data class SellMathInput(
    val bags: Long,
    val weightPerBagGrams: Long,
    val ratePaisaPerQuintal: Long,
    val billMode: BillMode,
    val aratBps: Long,
    /** null = use the Rs 1 per quintal default. */
    val tulakPaisaOverride: Long?,
    val receivedPaisa: Long,
)

data class SellMathResult(
    val totalWeightGrams: Long,
    val grossPaisa: Long,
    val aratPaisa: Long,
    val tulakPaisa: Long,
    val mandiShulkPaisa: Long,
    val totalReceivablePaisa: Long,
    val balanceDuePaisa: Long,
)

object BuySellMath {
    const val MANDI_SHULK_BPS = 150L
    private const val TULAK_PAISA_PER_QUINTAL = 100L

    fun totalWeightGrams(bags: Long, weightPerBagGrams: Long, extraWeightGrams: Long): Long =
        bags * weightPerBagGrams + extraWeightGrams

    fun defaultTulakPaisa(totalWeightGrams: Long): Long =
        MandiMathEngine.grossAmountPaisa(totalWeightGrams, TULAK_PAISA_PER_QUINTAL)

    fun calculateBuy(input: BuyMathInput): BuyMathResult {
        val grams = totalWeightGrams(input.bags, input.weightPerBagGrams, input.extraWeightGrams)
        val gross = MandiMathEngine.grossAmountPaisa(grams, input.ratePaisaPerQuintal)
        val commission = MandiMathEngine.percentageOf(gross, input.commissionBps)
        val labour = input.bags * input.labourPerBagPaisa
        return BuyMathResult(grams, gross, commission, labour, gross - commission - labour - input.advancePaidPaisa)
    }

    fun calculateSell(input: SellMathInput): SellMathResult {
        val grams = totalWeightGrams(input.bags, input.weightPerBagGrams, 0L)
        val gross = MandiMathEngine.grossAmountPaisa(grams, input.ratePaisaPerQuintal)
        val bos = input.billMode == BillMode.BILL_OF_SUPPLY
        val arat = if (bos) MandiMathEngine.percentageOf(gross, input.aratBps) else 0L
        val tulak = if (bos) input.tulakPaisaOverride ?: defaultTulakPaisa(grams) else 0L
        val shulk = if (bos) MandiMathEngine.percentageOf(gross, MANDI_SHULK_BPS) else 0L
        val total = gross + arat + tulak + shulk
        return SellMathResult(grams, gross, arat, tulak, shulk, total, total - input.receivedPaisa)
    }
}
```

Add to `MandiMathEngine` (beside `parseQuintalsStringToGrams`, using its private `parseFixedTwoDecimals`, which returns value x 100):

```kotlin
    /** "50" -> 50_000, "50.5" -> 50_500 (kg to grams, two decimals kept). */
    fun parseKgToGrams(input: String): Long = parseFixedTwoDecimals(input) * 10
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :core-domain:jvmTest`
Expected: PASS, including the existing `MandiMathEngineTest`.

- [ ] **Step 5: Commit**

```bash
git add core-domain
git commit -m "feat(math): buy and sell paisa math with worked examples"
```

### Task 4: Use cases

**Files:**
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/usecase/{BuyTransactionUseCase,SellTransactionUseCase,LendTransactionUseCase,ExpenseUseCase,StockInventoryUseCase}.kt`
- Test: `core-domain/src/commonTest/kotlin/com/appwork/mandisamiti/domain/StockInventoryUseCaseTest.kt`

**Interfaces:**
- Consumes: Task 2 repositories and models, Task 3 `BuySellMath`, `IdGenerator` (existing, `domain/id/IdGenerator.kt`; check its method name before use: `grep -n "fun " core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/id/IdGenerator.kt`).
- Produces:
  - `BuyTransactionUseCase(repo: BuyRepository, newId: () -> String, nowMillis: () -> Long)` with `suspend fun create(form: BuyForm): BuyEntry`, `suspend fun recordPayment(id: String, paidPaisa: Long)`.
  - `SellTransactionUseCase(repo: SellRepository, newId, nowMillis)` with `suspend fun create(form: SellForm): SellEntry`.
  - `LendTransactionUseCase(repo: LendRepository, newId, nowMillis)` with `suspend fun lend(form: LendForm): LendEntry`, `suspend fun repay(lendId: String, amountPaisa: Long, weightGrams: Long, bags: Long): LendSettlement`.
  - `ExpenseUseCase(repo: ExpenseRepository, newId, nowMillis)` with `suspend fun add(form: ExpenseForm): ExpenseEntry`.
  - `StockInventoryUseCase(repo: ExpenseRepository)` with `suspend fun lines(shopId: String): List<StockLine>`.
  - Form classes `BuyForm`, `SellForm`, `LendForm`, `ExpenseForm` hold already-parsed `Long`s (the UI parses text with `MandiMathEngine.parse*`).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.repository.*
import com.appwork.mandisamiti.domain.usecase.StockInventoryUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StockInventoryUseCaseTest {
    private class FakeStock(val totals: StockTotals) : ExpenseRepository {
        override fun getExpensesStream(shopId: String) = throw NotImplementedError()
        override suspend fun getExpenseById(id: String) = null
        override suspend fun saveExpense(entry: com.appwork.mandisamiti.domain.model.ExpenseEntry) = Unit
        override suspend fun voidExpense(id: String, reason: com.appwork.mandisamiti.domain.model.VoidReason) = Unit
        override suspend fun stockTotals(shopId: String) = totals
    }

    @Test
    fun onHandIsBoughtMinusSold_andIncludesCommoditiesOnlyBought() = runTest {
        val repo = FakeStock(StockTotals(mapOf("wheat" to 1_000_000L, "mustard" to 200_000L), mapOf("wheat" to 400_000L)))
        val lines = StockInventoryUseCase(repo).lines("shop").associateBy { it.commodityId }
        assertEquals(600_000L, lines.getValue("wheat").onHandGrams)
        assertEquals(200_000L, lines.getValue("mustard").onHandGrams)
    }

    @Test
    fun oversoldCommodityShowsNegative_notHidden() = runTest {
        val repo = FakeStock(StockTotals(mapOf("paddy" to 100_000L), mapOf("paddy" to 300_000L)))
        assertEquals(-200_000L, StockInventoryUseCase(repo).lines("shop").single().onHandGrams)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :core-domain:jvmTest --tests "*StockInventoryUseCaseTest*"`
Expected: FAIL, unresolved reference `StockInventoryUseCase`.

- [ ] **Step 3: Implement the use cases**

```kotlin
package com.appwork.mandisamiti.domain.usecase

import com.appwork.mandisamiti.domain.model.StockLine
import com.appwork.mandisamiti.domain.repository.ExpenseRepository

class StockInventoryUseCase(private val repo: ExpenseRepository) {
    suspend fun lines(shopId: String): List<StockLine> {
        val t = repo.stockTotals(shopId)
        return (t.boughtGramsByCommodity.keys + t.soldGramsByCommodity.keys).sorted().map {
            StockLine(it, t.boughtGramsByCommodity[it] ?: 0L, t.soldGramsByCommodity[it] ?: 0L)
        }
    }
}
```

```kotlin
package com.appwork.mandisamiti.domain.usecase

import com.appwork.mandisamiti.domain.math.*
import com.appwork.mandisamiti.domain.model.*
import com.appwork.mandisamiti.domain.repository.BuyRepository

data class BuyForm(
    val shopId: String, val farmerId: String, val commodityId: String,
    val bags: Long, val weightPerBagGrams: Long, val extraWeightGrams: Long,
    val ratePaisaPerQuintal: Long, val commissionBps: Long,
    val labourPerBagPaisa: Long, val advancePaidPaisa: Long,
    val vehicleNumber: String? = null, val slipNumber: String? = null, val remarks: String? = null,
)

class BuyTransactionUseCase(
    private val repo: BuyRepository,
    private val newId: () -> String,
    private val nowMillis: () -> Long,
) {
    suspend fun create(form: BuyForm): BuyEntry {
        require(form.bags > 0 || form.extraWeightGrams > 0) { "Weight is required" }
        require(form.ratePaisaPerQuintal > 0) { "Rate is required" }
        val m = BuySellMath.calculateBuy(
            BuyMathInput(form.bags, form.weightPerBagGrams, form.extraWeightGrams, form.ratePaisaPerQuintal,
                form.commissionBps, form.labourPerBagPaisa, form.advancePaidPaisa)
        )
        val now = nowMillis()
        val entry = BuyEntry(
            id = newId(), shopId = form.shopId, farmerId = form.farmerId, commodityId = form.commodityId,
            entryDate = now, bagsCount = form.bags, weightPerBagGrams = form.weightPerBagGrams,
            extraWeightGrams = form.extraWeightGrams, totalWeightGrams = m.totalWeightGrams,
            ratePaisaPerQuintal = form.ratePaisaPerQuintal, commissionBps = form.commissionBps,
            labourPerBagPaisa = form.labourPerBagPaisa, advancePaidPaisa = form.advancePaidPaisa,
            grossPaisa = m.grossPaisa, commissionPaisa = m.commissionPaisa, labourPaisa = m.labourPaisa,
            netPayablePaisa = m.netPayablePaisa, vehicleNumber = form.vehicleNumber,
            slipNumber = form.slipNumber, remarks = form.remarks, createdAt = now, updatedAt = now,
        )
        repo.saveBuyEntry(entry)
        return entry
    }

    suspend fun recordPayment(id: String, paidPaisa: Long) {
        val current = repo.getBuyEntryById(id) ?: throw IllegalArgumentException("Buy entry $id does not exist")
        require(paidPaisa >= 0) { "Payment cannot be negative" }
        repo.editBuyEntry(current.copy(paidPaisa = paidPaisa))
    }
}
```

```kotlin
package com.appwork.mandisamiti.domain.usecase

import com.appwork.mandisamiti.domain.math.*
import com.appwork.mandisamiti.domain.model.*
import com.appwork.mandisamiti.domain.repository.SellRepository

data class SellForm(
    val shopId: String, val buyerId: String, val commodityId: String,
    val billMode: BillMode, val partyKind: SellPartyKind, val gstin: String? = null,
    val bags: Long, val weightPerBagGrams: Long, val ratePaisaPerQuintal: Long,
    val aratBps: Long, val tulakPaisaOverride: Long? = null, val receivedPaisa: Long,
    val vehicleNumber: String? = null, val invoiceNumber: String? = null, val remarks: String? = null,
)

class SellTransactionUseCase(
    private val repo: SellRepository,
    private val newId: () -> String,
    private val nowMillis: () -> Long,
) {
    suspend fun create(form: SellForm): SellEntry {
        require(form.bags > 0) { "Bags are required" }
        require(form.ratePaisaPerQuintal > 0) { "Rate is required" }
        val m = BuySellMath.calculateSell(
            SellMathInput(form.bags, form.weightPerBagGrams, form.ratePaisaPerQuintal, form.billMode,
                form.aratBps, form.tulakPaisaOverride, form.receivedPaisa)
        )
        val now = nowMillis()
        val entry = SellEntry(
            id = newId(), shopId = form.shopId, buyerId = form.buyerId, commodityId = form.commodityId,
            entryDate = now, billMode = form.billMode, partyKind = form.partyKind, gstin = form.gstin,
            bagsCount = form.bags, weightPerBagGrams = form.weightPerBagGrams,
            totalWeightGrams = m.totalWeightGrams, ratePaisaPerQuintal = form.ratePaisaPerQuintal,
            aratBps = form.aratBps, tulakPaisa = m.tulakPaisa, grossPaisa = m.grossPaisa,
            aratPaisa = m.aratPaisa, mandiShulkPaisa = m.mandiShulkPaisa,
            totalReceivablePaisa = m.totalReceivablePaisa, receivedPaisa = form.receivedPaisa,
            balanceDuePaisa = m.balanceDuePaisa, vehicleNumber = form.vehicleNumber,
            invoiceNumber = form.invoiceNumber, remarks = form.remarks, createdAt = now, updatedAt = now,
        )
        repo.saveSellEntry(entry)
        return entry
    }
}
```

```kotlin
package com.appwork.mandisamiti.domain.usecase

import com.appwork.mandisamiti.domain.model.*
import com.appwork.mandisamiti.domain.repository.LendRepository

data class LendForm(
    val shopId: String, val partyId: String, val lendType: LendType,
    val amountPaisa: Long = 0L, val commodityId: String? = null, val bags: Long = 0L, val weightGrams: Long = 0L,
    val expectedReturnDate: Long? = null, val notes: String? = null,
)

class LendTransactionUseCase(
    private val repo: LendRepository,
    private val newId: () -> String,
    private val nowMillis: () -> Long,
) {
    suspend fun lend(form: LendForm): LendEntry {
        when (form.lendType) {
            LendType.MONEY -> require(form.amountPaisa > 0) { "Amount is required" }
            LendType.GRAIN -> require(form.weightGrams > 0 && form.commodityId != null) { "Grain and weight are required" }
        }
        val now = nowMillis()
        val entry = LendEntry(
            id = newId(), shopId = form.shopId, partyId = form.partyId, lendType = form.lendType, entryDate = now,
            amountPaisa = form.amountPaisa, commodityId = form.commodityId, bagsCount = form.bags,
            weightGrams = form.weightGrams, expectedReturnDate = form.expectedReturnDate, notes = form.notes,
            createdAt = now, updatedAt = now,
        )
        repo.saveLendEntry(entry)
        return entry
    }

    suspend fun repay(lendId: String, amountPaisa: Long, weightGrams: Long, bags: Long): LendSettlement {
        val lend = repo.getLendEntryById(lendId) ?: throw IllegalArgumentException("Lend $lendId does not exist")
        require(!lend.isVoid) { "Lend $lendId is void" }
        require(amountPaisa > 0 || weightGrams > 0) { "Nothing to settle" }
        val now = nowMillis()
        val s = LendSettlement(newId(), lend.shopId, lendId, now, amountPaisa, weightGrams, bags, now, now)
        repo.settle(s)
        return s
    }
}
```

```kotlin
package com.appwork.mandisamiti.domain.usecase

import com.appwork.mandisamiti.domain.model.*
import com.appwork.mandisamiti.domain.repository.ExpenseRepository

data class ExpenseForm(
    val shopId: String, val category: ExpenseCategory, val amountPaisa: Long,
    val paidTo: String? = null, val paymentMode: ExpensePaymentMode = ExpensePaymentMode.CASH,
    val remarks: String? = null,
)

class ExpenseUseCase(
    private val repo: ExpenseRepository,
    private val newId: () -> String,
    private val nowMillis: () -> Long,
) {
    suspend fun add(form: ExpenseForm): ExpenseEntry {
        require(form.amountPaisa > 0) { "Amount is required" }
        val now = nowMillis()
        val e = ExpenseEntry(newId(), form.shopId, now, form.category, form.amountPaisa, form.paidTo,
            form.paymentMode, form.remarks, now, now)
        repo.saveExpense(e)
        return e
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :core-domain:jvmTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core-domain
git commit -m "feat(domain): buy, sell, lend, expense and stock use cases"
```

### Task 5: Offline-first repositories and local sync

**Files:**
- Create: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/repository/OfflineFirst{Buy,Sell,Lend,Expense}Repository.kt`
- Modify: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/sync/` (extend `SyncEngine` push/pull with the new kinds; read the existing `SyncEngine.kt` and `model/` first and follow the Deal handling line by line)
- Modify: the logout wipe (`LogoutUseCase` / `wipe*` call site) to call the five new `wipe*` queries, children first.
- Test: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/BuyEntryRepositoryTest.kt` (and Sell/Lend), modelled on `RepositoryTest.kt` and `LedgerAuditTest.kt`.

**Interfaces:**
- Consumes: Task 1 queries, Task 2 interfaces. Constructor shape copies `OfflineFirstDealRepository(database, clock, ioDispatcher)`.
- Produces: `OfflineFirstBuyRepository`, `OfflineFirstSellRepository`, `OfflineFirstLendRepository`, `OfflineFirstExpenseRepository`; sync DTO types `SyncBuyEntry`, `SyncSellEntry`, `SyncLendEntry`, `SyncLendSettlement`, `SyncExpenseEntry` (field names equal to the DB column names in camelCase, matching the backend schemas in Task 6).

- [ ] **Step 1: Write the failing test**

```kotlin
// BuyEntryRepositoryTest.kt: copy the in-memory database + dispatcher setup from RepositoryTest.kt
@Test
fun saveWritesRowAndCreateRevision() = runTest {
    val repo = OfflineFirstBuyRepository(db, clock, dispatcher)
    repo.saveBuyEntry(sampleBuyEntry(id = "b1"))
    assertEquals(1, db.appDatabaseQueries.getRevisionsForEntry("b1").executeAsList().size)
    assertEquals("CREATE", db.appDatabaseQueries.getRevisionsForEntry("b1").executeAsOne().change_kind)
    assertEquals("BUY", db.appDatabaseQueries.getRevisionsForEntry("b1").executeAsOne().entry_kind)
}

@Test
fun editBumpsRevisionAndKeepsHistory() = runTest {
    val repo = OfflineFirstBuyRepository(db, clock, dispatcher)
    repo.saveBuyEntry(sampleBuyEntry(id = "b1"))
    repo.editBuyEntry(sampleBuyEntry(id = "b1").copy(paidPaisa = 500))
    assertEquals(2, repo.getBuyEntryById("b1")!!.revision)
    assertEquals(listOf(1L, 2L), db.appDatabaseQueries.getRevisionsForEntry("b1").executeAsList().map { it.revision })
}

@Test
fun voidedBuyLeavesStockTotals() = runTest {
    val repo = OfflineFirstBuyRepository(db, clock, dispatcher)
    repo.saveBuyEntry(sampleBuyEntry(id = "b1"))
    repo.voidBuyEntry("b1", VoidReason.ENTERED_BY_MISTAKE)
    assertTrue(db.appDatabaseQueries.getStockBuyTotals("shop").executeAsList().isEmpty())
}
```

`sampleBuyEntry(...)` is a private helper in the test file returning a `BuyEntry` built from the Task 3 buy worked example; seed the farmer party and commodity rows first (foreign keys), as `RepositoryTest.kt` does for deals. Check `VoidReason` for the exact enum constant names with `grep -n "" core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/VoidReason.kt` and use a real one.

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :core-data:jvmTest --tests "*BuyEntryRepositoryTest*"`
Expected: FAIL, unresolved reference `OfflineFirstBuyRepository`.

- [ ] **Step 3: Implement.** Follow `OfflineFirstDealRepository` exactly: `saveX` sets `revision = 1, syncStatus = 0`, inserts and records a `CREATE` revision with `entry_kind` `"BUY"`/`"SELL"`/`"LEND"`/`"EXPENSE"` and the entry as `snapshot_json` using the same JSON helper the deal repository uses; `editX` loads current, rejects void, bumps `revision`, sets `syncStatus = 0`, records `EDIT`; `voidX` sets `is_void = 1` plus reason and records `VOID`. `OfflineFirstLendRepository.settle` runs inside one `database.transaction`: insert the settlement (revision row kind `LEND_SETTLEMENT`), sum settlements, then `copy(status = SETTLED)` when repaid amount >= `amountPaisa` (MONEY) or repaid grams >= `weightGrams` (GRAIN), else `PARTIAL`, bump the lend revision and record an `EDIT`. `OfflineFirstExpenseRepository.stockTotals` maps `getStockBuyTotals` and `getStockSellTotals` rows into `StockTotals`.

- [ ] **Step 4: Add the sync kinds.** In `SyncEngine`, add each new table to the push read (`getPendingSync*`), the batch of 100, the ack (`mark*Synced` with id and revision), and the pull apply using delete-if-synced then insert-if-absent (add the matching `deleteSynced*` and `insert*IfAbsent` queries to `AppDatabase.sq` using the same pattern as `deleteSyncedDeal` and `insertDealIfAbsent`). Add one `SyncEngineTest` case per kind: a pending row is pushed and acked, and a pulled row never overwrites a pending local row.

- [ ] **Step 5: Run everything**

Run: `./gradlew :core-database:verifyCommonMainAppDatabaseMigration :core-domain:jvmTest :core-data:jvmTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add core-data core-database
git commit -m "feat(data): offline-first buy, sell, lend, expense repositories and sync kinds"
```

---

## Phase B: Backend sync (Claude Code)

### Task 6: Server models and schemas

**Files:**
- Create: `backend/app/models/{buy_entry,sell_entry,lend_entry,expense_entry}.py`, `backend/app/schemas/entries.py`
- Modify: `backend/app/models/__init__.py` so `Base.metadata.create_all` sees the new models.
- Reference: `backend/app/models/deal.py`, `backend/app/schemas/deal.py` (copy column style, shop scoping, `server_seq` column, revision and void columns).
- Test: `backend/tests/test_entries_models.py`

**Interfaces:**
- Consumes: nothing from Kotlin. The JSON field names must equal the `Sync*` DTO names from Task 5 in snake_case.
- Produces: SQLAlchemy models `BuyEntry`, `SellEntry`, `LendEntry`, `LendSettlement`, `ExpenseEntry`; Pydantic `BuyEntryIn`, `SellEntryIn`, `LendEntryIn`, `LendSettlementIn`, `ExpenseEntryIn` and matching `*Out`.

- [ ] **Step 1: Write the failing test**

```python
import pytest
from sqlalchemy import select
from app.models.buy_entry import BuyEntry

@pytest.mark.asyncio
async def test_buy_entry_roundtrip(db_session):
    db_session.add(BuyEntry(
        id="b1", shop_id="s1", farmer_id="f1", commodity_id="c1", entry_date=1,
        bags_count=10, weight_per_bag_grams=50000, extra_weight_grams=5000, total_weight_grams=505000,
        rate_paisa_per_quintal=250000, commission_bps=200, labour_per_bag_paisa=500, advance_paid_paisa=100000,
        gross_paisa=1262500, commission_paisa=25250, labour_paisa=5000, net_payable_paisa=1132250,
        paid_paisa=0, created_at=1, updated_at=1, revision=1,
    ))
    await db_session.commit()
    row = (await db_session.execute(select(BuyEntry).where(BuyEntry.id == "b1"))).scalar_one()
    assert row.net_payable_paisa == 1132250
    assert row.server_seq is None or isinstance(row.server_seq, int)
```

Use the fixture name that `backend/tests/conftest.py` actually provides (check it first; replace `db_session` if it differs).

- [ ] **Step 2: Run to verify it fails**

Run: `cd backend && .venv/bin/python -m pytest tests/test_entries_models.py -q`
Expected: FAIL, `ModuleNotFoundError: app.models.buy_entry`.

- [ ] **Step 3: Implement.** One model per table in `3.sqm`, with every column of the SQLite table (Integer columns as `BigInteger`, text as `String`/`Text`), plus `server_seq = Column(BigInteger, index=True)`, composite primary key not needed (`id` is a client UUID, scope reads by `shop_id`). Schemas: `*In` mirrors the model fields without `server_seq`; `*Out` adds `server_seq`. Apply `max_length` limits to free-text fields the same way `schemas/deal.py` does.

- [ ] **Step 4: Run to verify it passes**

Run: `cd backend && .venv/bin/python -m pytest -q`
Expected: PASS, no regression in the existing sync tests.

- [ ] **Step 5: Commit**

```bash
git add backend
git commit -m "feat(backend): buy, sell, lend, expense models and schemas"
```

### Task 7: Extend push and pull

**Files:**
- Modify: `backend/app/schemas/sync.py` (add five optional lists to `SyncPushRequest`, `SyncPushResponse`, `SyncPullResponse`, each `max_length=100` on push), `backend/app/api/v1/endpoints/sync.py`.
- Test: `backend/tests/test_sync_v2.py` (Task 10 owns the full suite; this task adds the first failing case).

**Interfaces:**
- Consumes: Task 6 models and schemas; the existing per-shop locked counter in `app/core/sync_seq.py`.
- Produces: `buy_entries`, `sell_entries`, `lend_entries`, `lend_settlements`, `expense_entries` keys in push and pull JSON; response ack lists `synced_buy_entries` etc. holding ids.

- [ ] **Step 1: Write the failing test**

```python
@pytest.mark.asyncio
async def test_push_buy_entry_then_pull_returns_it(client, auth_headers, buy_payload):
    r = await client.post("/api/v1/sync/push", json={"buy_entries": [buy_payload]}, headers=auth_headers)
    assert r.status_code == 200
    assert r.json()["synced_buy_entries"] == [buy_payload["id"]]
    p = await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)
    assert [e["id"] for e in p.json()["buy_entries"]] == [buy_payload["id"]]
```

`client`, `auth_headers` come from `conftest.py` (confirm names); `buy_payload` is a fixture returning the Task 6 field set as a dict.

- [ ] **Step 2: Run to verify it fails**

Run: `cd backend && .venv/bin/python -m pytest tests/test_sync_v2.py -q`
Expected: FAIL (422 or missing key).

- [ ] **Step 3: Implement** by following how `deals` is handled in `endpoints/sync.py` for each new list: apply only if incoming `revision` is newer, ack anything already held, assign `server_seq` from the locked shop counter, and include the new tables in the single `server_seq`-ordered pull stream. Never log request bodies.

- [ ] **Step 4: Run to verify it passes**

Run: `cd backend && .venv/bin/python -m pytest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend
git commit -m "feat(backend): sync push and pull for buy, sell, lend, expense"
```

---

## Phase C: Tests (Codex / OpenCode)

Rule for this phase: tests are written against the frozen signatures in Tasks 2 to 4. A failing test caused by a wrong signature goes back to Claude Code as a plan fix, not a signature change.

### Task 8: Buy and Sell math tests

**Files:**
- Modify: `core-domain/src/commonTest/kotlin/com/appwork/mandisamiti/domain/BuySellMathTest.kt` (keep the 3 tests from Task 3, add below)

- [ ] **Step 1: Add the failing cases**

```kotlin
@Test fun halfUpRoundingOnGross() {
    // 1 g at Rs 500/qtl = 0.5 paisa -> rounds up to 1 paisa
    assertEquals(1, MandiMathEngine.grossAmountPaisa(1, 50_000))
    val r = BuySellMath.calculateBuy(BuyMathInput(0, 0, 1, 50_000, 0, 0, 0))
    assertEquals(1, r.grossPaisa)
}

@Test fun zeroBagsWithLooseWeightOnly() {
    val r = BuySellMath.calculateBuy(BuyMathInput(0, 50_000, 25_000, 200_000, 0, 500, 0))
    assertEquals(25_000, r.totalWeightGrams)
    assertEquals(50_000, r.grossPaisa)
    assertEquals(0, r.labourPaisa)
}

@Test fun advanceLargerThanDueGivesNegativeNet() {
    val r = BuySellMath.calculateBuy(BuyMathInput(1, 100_000, 0, 100_000, 0, 0, 500_000))
    assertEquals(100_000, r.grossPaisa)
    assertEquals(-400_000, r.netPayablePaisa)
}

@Test fun commissionRoundsHalfUp() {
    // gross 1_262_500, 1.5% -> 18_937.5 -> 18_938
    assertEquals(18_938, MandiMathEngine.percentageOf(1_262_500, 150))
}

@Test fun tulakOverrideBeatsDefault() {
    val r = BuySellMath.calculateSell(
        SellMathInput(20, 50_000, 260_000, com.appwork.mandisamiti.domain.model.BillMode.BILL_OF_SUPPLY, 0, 5_000, 0)
    )
    assertEquals(5_000, r.tulakPaisa)
}

@Test fun kgParsing() {
    assertEquals(50_000, MandiMathEngine.parseKgToGrams("50"))
    assertEquals(50_500, MandiMathEngine.parseKgToGrams("50.5"))
}

@Test fun sellBalanceNegativeWhenOverReceived() {
    val r = BuySellMath.calculateSell(
        SellMathInput(1, 100_000, 100_000, com.appwork.mandisamiti.domain.model.BillMode.NORMAL, 0, null, 500_000)
    )
    assertEquals(-400_000, r.balanceDuePaisa)
}
```

- [ ] **Step 2: Run**

Run: `./gradlew :core-domain:jvmTest --tests "*BuySellMathTest*"`
Expected: PASS (math already exists from Task 3). If any case fails, report the exact assertion to Claude Code; do not edit `BuySellMath.kt`.

- [ ] **Step 3: Commit**

```bash
git add core-domain
git commit -m "test(math): buy and sell edge cases"
```

### Task 9: Repository, stock, lend and void tests

**Files:**
- Create: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/{SellEntryRepositoryTest,LendRepositoryTest,StockQueryTest}.kt`

- [ ] **Step 1: Write the tests** (setup copied from `RepositoryTest.kt`; seed party and commodity rows first):

```kotlin
// LendRepositoryTest
@Test fun partialRepaymentMovesToPartial_fullToSettled() = runTest {
    val repo = OfflineFirstLendRepository(db, clock, dispatcher)
    repo.saveLendEntry(moneyLend(id = "l1", amountPaisa = 1_000_000))
    repo.settle(settlement("s1", "l1", amountPaisa = 400_000))
    assertEquals(LendStatus.PARTIAL, repo.getLendEntryById("l1")!!.status)
    repo.settle(settlement("s2", "l1", amountPaisa = 600_000))
    assertEquals(LendStatus.SETTLED, repo.getLendEntryById("l1")!!.status)
    assertEquals(3, repo.getLendEntryById("l1")!!.revision)
}

@Test fun grainLendSettlesByWeight() = runTest {
    val repo = OfflineFirstLendRepository(db, clock, dispatcher)
    repo.saveLendEntry(grainLend(id = "l2", weightGrams = 500_000))
    repo.settle(settlement("s3", "l2", weightGrams = 500_000))
    assertEquals(LendStatus.SETTLED, repo.getLendEntryById("l2")!!.status)
}

// StockQueryTest
@Test fun stockExcludesVoidedAndDeletedRows() = runTest {
    buy.saveBuyEntry(sampleBuy("b1", commodity = "wheat", grams = 1_000_000))
    buy.saveBuyEntry(sampleBuy("b2", commodity = "wheat", grams = 500_000))
    buy.voidBuyEntry("b2", VoidReason.ENTERED_BY_MISTAKE)
    sell.saveSellEntry(sampleSell("s1", commodity = "wheat", grams = 300_000))
    val t = expense.stockTotals("shop")
    assertEquals(1_000_000L, t.boughtGramsByCommodity["wheat"])
    assertEquals(300_000L, t.soldGramsByCommodity["wheat"])
}

// SellEntryRepositoryTest
@Test fun voidKeepsRowAndWritesVoidRevision() = runTest {
    sell.saveSellEntry(sampleSell("s1"))
    sell.voidSellEntry("s1", VoidReason.ENTERED_BY_MISTAKE)
    val row = sell.getSellEntryById("s1")!!
    assertTrue(row.isVoid)
    assertEquals("VOID", db.appDatabaseQueries.getRevisionsForEntry("s1").executeAsList().last().change_kind)
}

@Test fun editingVoidEntryIsRejected() = runTest {
    sell.saveSellEntry(sampleSell("s1"))
    sell.voidSellEntry("s1", VoidReason.ENTERED_BY_MISTAKE)
    assertFailsWith<IllegalArgumentException> { sell.editSellEntry(sampleSell("s1")) }
}
```

Helpers (`moneyLend`, `grainLend`, `settlement`, `sampleBuy`, `sampleSell`) are private functions in each test file building the models from Task 2 with fixed timestamps. Use a real `VoidReason` constant (see Task 5 step 1).

- [ ] **Step 2: Run**

Run: `./gradlew :core-data:jvmTest`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add core-data
git commit -m "test(data): lend settlement, stock totals and void rules"
```

### Task 10: Backend sync suite and token lint

**Files:**
- Modify: `backend/tests/test_sync_v2.py`
- Run: `tests/ngdl-tokens` from the hub root.

- [ ] **Step 1: Add the cases** (fixtures as in Task 7):

```python
@pytest.mark.asyncio
async def test_repush_same_revision_is_acked_not_duplicated(client, auth_headers, buy_payload):
    for _ in range(2):
        r = await client.post("/api/v1/sync/push", json={"buy_entries": [buy_payload]}, headers=auth_headers)
        assert r.json()["synced_buy_entries"] == [buy_payload["id"]]
    p = await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)
    assert len(p.json()["buy_entries"]) == 1

@pytest.mark.asyncio
async def test_older_revision_does_not_overwrite(client, auth_headers, buy_payload):
    newer = {**buy_payload, "revision": 2, "paid_paisa": 500}
    await client.post("/api/v1/sync/push", json={"buy_entries": [newer]}, headers=auth_headers)
    await client.post("/api/v1/sync/push", json={"buy_entries": [buy_payload]}, headers=auth_headers)
    p = await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)
    assert p.json()["buy_entries"][0]["paid_paisa"] == 500

@pytest.mark.asyncio
async def test_other_shop_cannot_see_entries(client, auth_headers, other_shop_headers, buy_payload):
    await client.post("/api/v1/sync/push", json={"buy_entries": [buy_payload]}, headers=auth_headers)
    p = await client.get("/api/v1/sync/pull?server_seq=0", headers=other_shop_headers)
    assert p.json()["buy_entries"] == []

@pytest.mark.asyncio
async def test_pull_orders_all_kinds_by_server_seq(client, auth_headers, buy_payload, expense_payload):
    await client.post("/api/v1/sync/push", json={"buy_entries": [buy_payload], "expense_entries": [expense_payload]}, headers=auth_headers)
    p = (await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)).json()
    seqs = [e["server_seq"] for e in p["buy_entries"] + p["expense_entries"]]
    assert sorted(seqs) == list(set(seqs))

@pytest.mark.asyncio
async def test_batch_over_100_rejected(client, auth_headers, buy_payload):
    big = [{**buy_payload, "id": f"b{i}"} for i in range(101)]
    r = await client.post("/api/v1/sync/push", json={"buy_entries": big}, headers=auth_headers)
    assert r.status_code == 422
```

`other_shop_headers` and `expense_payload` are new fixtures added to `conftest.py` in this task; build them the same way the existing isolation test builds a second shop.

- [ ] **Step 2: Run**

Run: `cd backend && .venv/bin/python -m pytest -q`
Expected: PASS.

- [ ] **Step 3: Token lint** after Phase D UI lands: from the hub root run the `tests/ngdl-tokens` suite (command in `docs/design-system/DESIGN-SYSTEM.md` §5.7). Expected: PASS. Run it once now as a baseline and record the result in the commit message.

- [ ] **Step 4: Commit**

```bash
git add backend
git commit -m "test(backend): sync v2 suite for buy, sell, lend, expense"
```

---

## Phase D: UI (Gemini / Antigravity)

Rules for the whole phase: read `docs/design-system/DESIGN-SYSTEM.md` §2.1 and §5 and the project `DESIGN.md` first. Use the existing screens (`DealEntryScreen.kt`, `ReceiptPreviewScreen.kt`, `DashboardScreen.kt`) as the layout and theme reference. Screens never do money math: they parse text with `MandiMathEngine.parse*`, call the Phase A use cases, and show `BuySellMath.calculateBuy/Sell` results as a live preview. Use `ui/theme/Color.kt` tokens, not hex.

### Task 11: Navigation and speed-dial FAB

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/App.kt`, `ui/home/HomeScreen.kt`
- Create: `ui/components/SpeedDialFab.kt`
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/ScreenRoutingTest.kt`

**Interfaces:**
- Consumes: Phase A repositories (wired by hand in `App.kt` with `remember`, as the existing repositories are).
- Produces: `Screen` cases `BuyEntry(existingId: String?)`, `BuyList`, `BuyReceipt(id: String)`, `SellEntry(existingId: String?)`, `SellList`, `SellReceipt(id: String)`, `LendEntry`, `LendList`, `ExpenseEntry`, `StockSheet`. `SpeedDialFab(onBuy: () -> Unit, onSell: () -> Unit, onLend: () -> Unit, onExpense: () -> Unit)`.

- [ ] **Step 1: Write the failing test.** Add a pure function `fun Screen.backTarget(): Screen` next to `Screen` and test it:

```kotlin
@Test fun entryScreensReturnToTheirList() {
    assertEquals(Screen.BuyList, Screen.BuyEntry(null).backTarget())
    assertEquals(Screen.SellList, Screen.SellReceipt("x").backTarget())
    assertEquals(Screen.Home, Screen.BuyList.backTarget())
}
```

- [ ] **Step 2: Run to verify it fails.** `./gradlew :composeApp:jvmTest --tests "*ScreenRoutingTest*"`; expected FAIL, unresolved `Screen.BuyList`.
- [ ] **Step 3: Implement** the `Screen` cases, `backTarget()`, the four-action FAB (vector icons, 48dp targets, labels in Hindi: खरीद, बिक्री, उधार, खर्च), and the `when (currentScreen)` branches in `App.kt` rendering the screens from Tasks 12 to 15 (stub each with a `Text` until its task lands so the app always builds).
- [ ] **Step 4: Run to verify it passes.** `./gradlew :composeApp:jvmTest`; expected PASS.
- [ ] **Step 5: Commit.** `git add composeApp && git commit -m "feat(ui): revamp navigation and speed-dial FAB"`

### Task 12: Buy screens (S1, S2, S3)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/buy/{BuyEntryScreen,BuyTransactionsListScreen,BuyTransactionReceiptScreen,BuyEntryFormState}.kt`
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/buy/BuyEntryFormStateTest.kt`

**Interfaces:**
- Consumes: `BuyTransactionUseCase`, `BuyRepository`, `PartyRepository`, `BuySellMath`, `MandiMathEngine.parseKgToGrams/parseRupeesToPaisa/parsePercentToBasisPoints`.
- Produces: `BuyEntryFormState` (pure, testable) and the three screens:

```kotlin
class BuyEntryFormState {
    var phone by mutableStateOf("")
    var bags by mutableStateOf("")
    var weightPerBagKg by mutableStateOf("")
    var extraWeightKg by mutableStateOf("")
    var ratePerQuintal by mutableStateOf("")
    var commissionPercent by mutableStateOf("")
    var labourPerBag by mutableStateOf("")
    var advancePaid by mutableStateOf("")

    fun preview(): BuyMathResult = BuySellMath.calculateBuy(
        BuyMathInput(
            bags = bags.toLongOrNull() ?: 0L,
            weightPerBagGrams = MandiMathEngine.parseKgToGrams(weightPerBagKg.ifBlank { "0" }),
            extraWeightGrams = MandiMathEngine.parseKgToGrams(extraWeightKg.ifBlank { "0" }),
            ratePaisaPerQuintal = MandiMathEngine.parseRupeesToPaisa(ratePerQuintal.ifBlank { "0" }),
            commissionBps = MandiMathEngine.parsePercentToBasisPoints(commissionPercent.ifBlank { "0" }),
            labourPerBagPaisa = MandiMathEngine.parseRupeesToPaisa(labourPerBag.ifBlank { "0" }),
            advancePaidPaisa = MandiMathEngine.parseRupeesToPaisa(advancePaid.ifBlank { "0" }),
        )
    )
}

@Composable fun BuyEntryScreen(existingId: String?, onSaved: (String) -> Unit, onBack: () -> Unit)
@Composable fun BuyTransactionsListScreen(onOpen: (String) -> Unit, onNew: () -> Unit, onBack: () -> Unit)
@Composable fun BuyTransactionReceiptScreen(id: String, onBack: () -> Unit)
```

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun previewMatchesTheBuyWorkedExample() {
    val f = BuyEntryFormState().apply {
        bags = "10"; weightPerBagKg = "50"; extraWeightKg = "5"; ratePerQuintal = "2500"
        commissionPercent = "2"; labourPerBag = "5"; advancePaid = "1000"
    }
    assertEquals(1_132_250, f.preview().netPayablePaisa)
}
@Test fun blankFieldsPreviewAsZeroNotCrash() { assertEquals(0, BuyEntryFormState().preview().grossPaisa) }
```

- [ ] **Step 2: Run to verify it fails.** `./gradlew :composeApp:jvmTest --tests "*BuyEntryFormStateTest*"`; expected FAIL.
- [ ] **Step 3: Implement** the form state (above) and the screens. S1 behaviour: phone lookup against `PartyRepository` shows an "active loan" alert chip when the farmer has an open `LendEntry` or a negative party balance; a negative `netPayablePaisa` shows an inline warning and disables Save; the live preview card lists gross, commission, labour, advance and net payable. S2: search by phone or name, sort by date, status chips from `BuyEntry.paymentStatus` (लंबित, आंशिक, पूर्ण), tap opens S3. S3: farmer slip (किसान पर्चा) with gross, deductions (commission + labour), advance and net cash payable, plus a 1-tap WhatsApp share built from the same text as the on-screen slip. Void entries show a "रद्द" badge and drop out of totals.
- [ ] **Step 4: Run to verify it passes.** `./gradlew :composeApp:jvmTest`; expected PASS.
- [ ] **Step 5: Commit.** `git add composeApp && git commit -m "feat(ui): buy entry, list and farmer slip"`

### Task 13: Sell screens (S4, S5, S6)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/sell/{SellEntryScreen,SellTransactionsListScreen,SellTransactionReceiptScreen,SellEntryFormState}.kt`
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/sell/SellEntryFormStateTest.kt`

**Interfaces:**
- Consumes: `SellTransactionUseCase`, `SellRepository`, `BuySellMath.calculateSell`, `BillMode`, `SellPartyKind`.
- Produces: `SellEntryFormState.preview(): SellMathResult` (same pattern as Task 12; fields `billMode`, `partyKind`, `gstin`, `bags`, `weightPerBagKg`, `ratePerQuintal`, `aratPercent`, `tulak` (blank = default), `received`), and screens `SellEntryScreen(existingId, onSaved, onBack)`, `SellTransactionsListScreen(onOpen, onNew, onBack)`, `SellTransactionReceiptScreen(id, onBack)`.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun previewMatchesTheBillOfSupplyWorkedExample() {
    val f = SellEntryFormState().apply {
        billMode = BillMode.BILL_OF_SUPPLY; bags = "20"; weightPerBagKg = "50"
        ratePerQuintal = "2600"; aratPercent = "1"; received = "10000"
    }
    assertEquals(2_666_000, f.preview().totalReceivablePaisa)
    assertEquals(1_666_000, f.preview().balanceDuePaisa)
}
@Test fun blankTulakUsesDefault() {
    val f = SellEntryFormState().apply { billMode = BillMode.BILL_OF_SUPPLY; bags = "20"; weightPerBagKg = "50"; ratePerQuintal = "2600" }
    assertEquals(1_000, f.preview().tulakPaisa)
}
```

- [ ] **Step 2: Run to verify it fails.** `./gradlew :composeApp:jvmTest --tests "*SellEntryFormStateTest*"`; expected FAIL.
- [ ] **Step 3: Implement.** S4: segmented control for Normal vs Bill of Supply; Arat, Tulak and Mandi Shulk rows appear only in Bill of Supply mode; GSTIN field appears for Merchant. Warn (do not block) when the sale grams exceed godown stock for the commodity (from `StockInventoryUseCase`), because overselling is real data to be flagged, not hidden. S5: search, date sort, "बकाया" balance column and a filter for balance > 0. S6: tax breakdown (gross, Arat, Tulak, Mandi Shulk, total, received, balance) and WhatsApp share.
- [ ] **Step 4: Run to verify it passes.** `./gradlew :composeApp:jvmTest`; expected PASS.
- [ ] **Step 5: Commit.** `git add composeApp && git commit -m "feat(ui): sell entry, list and bill"`

### Task 14: Lend and Expense screens (S7, S8, S9)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/lend/{LendEntryScreen,LendTransactionsListScreen}.kt`, `ui/expense/ExpenseEntryScreen.kt`
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/ExpenseChipsTest.kt`

**Interfaces:**
- Consumes: `LendTransactionUseCase`, `ExpenseUseCase`, `LendType`, `ExpenseCategory`.
- Produces: `ExpenseCategory.labelHi(): String` (extension in `ui/expense`) and screens `LendEntryScreen(onSaved, onBack)`, `LendTransactionsListScreen(onBack)`, `ExpenseEntryScreen(onSaved, onBack)`.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun everyCategoryHasAHindiLabel() {
    assertEquals("हम्माली", ExpenseCategory.HAMMALI.labelHi())
    assertEquals("किराया", ExpenseCategory.KIRAYA.labelHi())
    assertEquals("बिजली", ExpenseCategory.BIJLI.labelHi())
    assertEquals("चाय-पानी", ExpenseCategory.CHAI_PANI.labelHi())
    assertEquals("मजदूरी", ExpenseCategory.MAJDOORI.labelHi())
    ExpenseCategory.entries.forEach { assertTrue(it.labelHi().isNotBlank()) }
}
```

- [ ] **Step 2: Run to verify it fails.** `./gradlew :composeApp:jvmTest --tests "*ExpenseChipsTest*"`; expected FAIL.
- [ ] **Step 3: Implement.** S7: Money vs Grain toggle (money shows amount in rupees; grain shows commodity, bags and weight), expected return date picker, notes. S8: open and partial loans first, due-date badge for overdue loans, a "वापसी दर्ज करें" sheet calling `LendTransactionUseCase.repay`, settled loans in a collapsed section. S9: category chips, amount, paid-to, payment-mode segmented control (`CASH`/`ONLINE`/`CHEQUE`).
- [ ] **Step 4: Run to verify it passes.** `./gradlew :composeApp:jvmTest`; expected PASS.
- [ ] **Step 5: Commit.** `git add composeApp && git commit -m "feat(ui): lend entry, loan book and expense entry"`

### Task 15: Stock sheet and dashboard revamp (S10, S11)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/stock/StockBreakdownSheet.kt`, `ui/dashboard/DashboardKpis.kt`
- Modify: `ui/dashboard/DashboardScreen.kt`, `ui/home/HomeScreen.kt`
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/dashboard/DashboardKpisTest.kt`

**Interfaces:**
- Consumes: `BuyEntry`, `SellEntry`, `LendEntry`, `ExpenseEntry`, `StockInventoryUseCase`.
- Produces:

```kotlin
data class DashboardKpis(val totalBuyPaisa: Long, val totalSellPaisa: Long, val totalLendPaisa: Long,
                         val totalExpensePaisa: Long, val profitLossPaisa: Long)

fun computeKpis(buys: List<BuyEntry>, sells: List<SellEntry>, lends: List<LendEntry>, expenses: List<ExpenseEntry>): DashboardKpis
```

Profit and loss = `Σ sells.grossPaisa − Σ buys.grossPaisa − Σ expenses.amountPaisa`. Mandi Shulk, Arat and Tulak are excluded from P&L (pass-through charges), and lend is not an expense. Void rows are excluded everywhere. Only `MONEY` lends outstanding (`amountPaisa` minus money settlements) count toward `totalLendPaisa`; the function takes plain lists, so pass settlements through `LendEntry`-level outstanding amounts computed by the caller and documented in the screen.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun voidRowsAreExcluded_andProfitUsesGrossOnly() {
    val k = computeKpis(
        buys = listOf(buy(gross = 1_000_000), buy(gross = 500_000, void = true)),
        sells = listOf(sell(gross = 1_300_000, total = 1_340_000)),
        lends = emptyList(),
        expenses = listOf(expense(50_000), expense(70_000, void = true)),
    )
    assertEquals(1_000_000, k.totalBuyPaisa)
    assertEquals(1_300_000, k.totalSellPaisa)
    assertEquals(50_000, k.totalExpensePaisa)
    assertEquals(250_000, k.profitLossPaisa)
}
```

`buy`, `sell`, `expense` are private helpers in the test file that build the models with defaults from Task 2.

- [ ] **Step 2: Run to verify it fails.** `./gradlew :composeApp:jvmTest --tests "*DashboardKpisTest*"`; expected FAIL.
- [ ] **Step 3: Implement** `computeKpis`, then the UI: four KPI cards (खरीद, बिक्री, उधार, खर्च), a godown stock card listing the top commodities with quintals (`gramsToQuintals`) and tap-through to `StockBreakdownSheet`, a P&L card (a loss shows in the error token with a minus sign, never red-only), and the `SpeedDialFab` from Task 11. Every card uses `--ngdl-card-shadow` equivalent elevation from the existing theme. Negative on-hand stock is shown as a flagged line ("स्टॉक से ज्यादा बिक्री"), not clamped to zero.
- [ ] **Step 4: Run to verify it passes.** `./gradlew :composeApp:jvmTest`; expected PASS.
- [ ] **Step 5: Commit.** `git add composeApp && git commit -m "feat(ui): stock breakdown and dashboard revamp"`

### Task 16: Hardware verification on Redmi Note 7

**Files:** none changed unless a defect is found. Findings go to `docs/handoffs/2026-10-27-mandisamiti-revamp-verification.md`.

- [ ] **Step 1: Install.** Claude Code runs `./gradlew :composeApp:installDebug` (device serial `d93c743`).
- [ ] **Step 2: Mirror the screen.** `scrcpy -s d93c743`.
- [ ] **Step 3: Walk the checklist** in light and dark: S1 to S11 each opens; Buy and Sell previews equal the worked examples (Buy net Rs 11,322.50; Sell total Rs 26,660.00 and balance Rs 16,660.00 for the Task 3 inputs); WhatsApp share text matches the on-screen slip; Lend repayment moves PARTIAL to SETTLED; airplane-mode entry shows as pending sync and uploads after reconnect; no horizontal scroll at 390dp; touch targets >= 48dp; contrast >= 4.5:1; no emoji anywhere.
- [ ] **Step 4: Data safety.** Use a throwaway shop account for writes. On a real-data shop, only open screens and read; never save entries.
- [ ] **Step 5: Optional, last:** soundbox TTS announcements for Buy and Sell in the existing soundbox settings flow, after Steps 1 to 4 pass.
- [ ] **Step 6: Report.** Write the verification file with pass/fail per item and anything unverified listed explicitly. Fix defects as separate commits with a regression test each.

---

## Phase E: Execution and release (Claude Code)

### Task 17: Gate and alert after each phase

- [ ] **Step 1: Run the gates**

```bash
cd /Users/appworx/Desktop/Vivek-K/projects/MandiSamiti
./gradlew :core-database:verifyCommonMainAppDatabaseMigration :core-domain:jvmTest :core-database:jvmTest :core-data:jvmTest :composeApp:jvmTest
(cd backend && .venv/bin/python -m pytest -q)
```

Expected: all green. After Phase D also run `./gradlew :composeApp:installDebug` with the Redmi connected.

- [ ] **Step 2: Send the status alert** (message text contains no secrets):

```bash
python3 /Users/appworx/Desktop/Vivek-K/scripts/notify_telegram.py "MandiSamiti revamp: phase <A|B|C|D> gates green"
```

If a gate fails, send the failing test name only (no log bodies), stop, and hand the failure to the owning runtime.

### Task 18: Project update

- [ ] **Step 1:** From the hub root, `python3 scripts/project_update.py` (tests, commit, graphify, vault sync-log, Telegram).
- [ ] **Step 2:** Add a wiki note for the revamp (decision: new entry tables instead of extending `dealEntity`; Bill of Supply math; stock rule) under `vaults/Vivek-K/wiki/` with wikilinks, a `log.md` entry, and refresh `wiki/hot.md`.
- [ ] **Step 3:** `python3 scripts/vault_lint.py` must report 0 errors.
- [ ] **Step 4:** Ask Vivek before any push or `/project-deploy`.

---

## Assumptions to confirm with Vivek

1. **Normal bill mode** adds nothing to gross (no Arat, Tulak or Mandi Shulk). The brief only defines additions for Bill of Supply.
2. **Tulak default** is Rs 1 per quintal (`defaultTulakPaisa`), editable per bill.
3. **Stock** counts only the new Buy and Sell tables. Legacy `dealEntity` rows are excluded, so existing shops start at zero stock until entries are made.
4. **P&L** is Σ sell gross − Σ buy gross − Σ expenses. Arat, Tulak and Mandi Shulk are treated as pass-through, and lend is not an expense.
5. `ReceiptPreviewScreen` and `DealEntryScreen` stay for the existing auction deal flow.
6. Soundbox TTS (brief item for Gemini) is the last optional step of Task 16.

## Self-Review

**Spec coverage**
- S1 to S3 Buy: Tasks 1 to 4, 5, 12. S4 to S6 Sell: Tasks 1 to 4, 5, 13. S7, S8 Lend: Tasks 1, 4, 5, 14. S9 Expense: Tasks 1, 4, 14. S10 Stock: Tasks 1, 4, 15. S11 Dashboard and FAB: Tasks 11, 15.
- Formulas: weight, gross, labour, commission, net payable (Task 3), Arat, Tulak, Mandi Shulk, receivable, balance (Task 3); worked examples pinned in Task 3 and re-checked on device in Task 16.
- Matrix: Claude Code (Tasks 1 to 7), Gemini (11 to 16), Codex (8 to 10), and Claude Code again for the gates and release (17 to 18).
- Audit, sync and offline requirements: revisions in Task 5, sync in Tasks 5, 7, 10.

**Placeholder scan:** Tasks 5 and 6 refer to existing files for style and ask the implementer to confirm fixture names and `VoidReason` constants before use; each names the exact `grep` or file to check. No TBD items remain.

**Type consistency:** `BuyMathInput`/`BuyMathResult`, `SellMathInput`/`SellMathResult`, `BuyForm`, `SellForm`, `LendForm`, `ExpenseForm`, `StockTotals`, `StockLine`, `computeKpis` and the `Screen` cases are defined once and used with the same names in later tasks.
