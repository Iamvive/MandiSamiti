# Sell Slip, Deal Labour and Galla Bank Tags Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deal labour per quintal, an editable Mandi shulk % on Bill of Supply sell slips, bill-type chips and titles, a Cash/Bank settle for sell slips, and bank rows tagged on the Galla screen.

**Architecture:** Pure money rules live in `core-domain` (`BuySellMath`, a new `DealLabour` helper, a new `SellReceiptBuilder`) and are tested first. Two nullable columns on `cashTransactionEntity` and one on `sellEntryEntity` ride a single `15.sqm` migration plus sync DTO and backend migration `0008`. UI changes are thin and call the domain functions. `CashDrawer` and `BahiDayPageBuilder` already handle non-cash rows correctly and stay unchanged; tests only prove it.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, SQLDelight, kotlinx-serialization sync DTOs, FastAPI + Alembic backend (pytest).

**Spec:** `docs/superpowers/specs/2026-11-16-sell-slip-bank-settle-design.md` (read first).

**Branch / worktree:** `sell-bank-settle` at `/Users/appworx/Desktop/Vivek-K/projects/MandiSamiti-bank-settle`, cut from `udhaar-rewrite` because it needs that branch's `14.sqm` and `CashTransaction.lendId`. Work only in this worktree. Never touch the khaate files in the main checkout.

## Global Constraints

- Scope is **sell slips only**. Buy-slip farmer payments stay cash-only (confirmed by Vivek 2026-11-16).
- Migration numbers: mobile `15.sqm` (`14.sqm` is the Udhaar plan's, already on the base branch); backend `0008` (`0007` is reserved by the Udhaar plan; `0006` is the latest on disk). If the Udhaar plan lands a different number first, renumber, never share.
- Money is `Long` paisa. Rates are basis points (`Long`). No floating point in money code.
- Bank settle modes allowed: `NEFT`, `RTGS`, `CHEQUE`, `UPI`. Reference is required and non-blank. Bank name is optional free text. There is no bank accounts list.
- A bank settle row stores its real `paymentMode`, never `CASH`, and always carries `partyId` (the buyer).
- `CashDrawer` counts `PaymentMode.CASH` only. Do not change it.
- Labour on Deal = rate (paisa per quintal) × net quintals via `MandiMathEngine.grossAmountPaisa(netGrams, ratePaisaPerQuintal)`. Existing deals keep stored `labourChargePaisa`.
- Settings default `sellLabourPaisaPerQuintal` becomes `100L` (₹1) on fresh installs only. Existing stored settings are not rewritten.
- Defaults confirmed by Vivek 2026-11-16: Mandi shulk 2% (`200` bps), sell commission 1.5% (already `buyerCommissionBps`/`customerCommissionBps` = 150 in `TradeSettings`; no change needed).
- Mandi shulk field is visible only for Bill of Supply to a merchant. Default 2% (`200` bps). Rows that existed before the migration keep the rate they were actually charged (see Task 2 and the open question at the end).
- Copy (Hindi, exact): "सप्लाई बिल", "कैश मेमो", "कैश मेमो (Cash Memo)", "हम्माली (₹ प्रति क्विंटल)", "मंडी शुल्क %", "नकद", "बैंक", "बैंक/चेक/UPI: ₹X (नकद में नहीं)", receipt line "मंडी शुल्क (1.5%)".
- NGDL (`docs/design-system/DESIGN-SYSTEM.md` §2.1 tokens, §5 guardrails): tokens only, contrast 4.5:1 in light and dark, no emoji, vector icons, no horizontal scroll at 390dp, cards use `--ngdl-card-shadow` equivalents already used by neighbouring composables.
- Backend deploy needs Vivek's explicit approval. Do not run `project_deploy` or `sync_to_vps.sh`.
- Commit messages end with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Feature harness: finish by appending to `PROGRESS.md` and `FEATURES.json` (Task 9).

## Test commands

```bash
cd /Users/appworx/Desktop/Vivek-K/projects/MandiSamiti-bank-settle
./gradlew :core-domain:jvmTest
./gradlew :core-data:jvmTest :core-database:jvmTest
./gradlew :composeApp:jvmTest
cd backend && python -m pytest -q
```

Baseline before this work (from the previous session, cached): core-domain 97, core-data 145, composeApp 354 tests passing. Re-run in Task 0 for fresh figures.

## File Structure

| File | Change |
|---|---|
| `core-domain/.../math/BuySellMath.kt` | `SellMathInput.mandiShulkBps`, `DEFAULT_MANDI_SHULK_BPS`, use in `calculateSell` |
| `core-domain/.../math/DealLabour.kt` | New. `DealLabour.resolvePaisa(...)` |
| `core-domain/.../model/SellEntry.kt` | `mandiShulkBps: Long = 200L` |
| `core-domain/.../model/CashTransaction.kt` | `referenceNo: String?`, `bankName: String?` |
| `core-domain/.../model/TradeSettings.kt` | `sellLabourPaisaPerQuintal` default `100L` |
| `core-domain/.../usecase/SellReceiptBuilder.kt` | New. Builds the settle `CashTransaction`, validates bank input |
| `core-database/.../database/15.sqm` | New migration |
| `core-database/.../database/AppDatabase.sq` | Columns + insert queries |
| `core-data/.../repository/OfflineFirstCashTransactionRepository.kt` | map 2 columns (lines ~229, ~254) |
| `core-data/.../repository/OfflineFirstSellRepository.kt` | map `mandi_shulk_bps` (lines ~150, ~183) |
| `core-data/.../sync/model/SyncDto.kt`, `sync/SyncEngine.kt` | new fields push + pull |
| `backend/migrations/versions/0008_bank_settle_fields.py`, `backend/app/models/transaction.py`, `sell_entry.py`, `schemas/transaction.py`, `schemas/entries.py`, `api/v1/endpoints/sync.py` | new fields |
| `composeApp/.../ui/parchi/FarmerSlipCashSettlementSheet.kt` | Cash/Bank switch on `TraderSlipCashSettlementSheet` |
| `composeApp/.../ui/home/HomeViewModel.kt`, `ui/ledger/PartyLedgerViewModel.kt` | call `SellReceiptBuilder` |
| `composeApp/.../ui/sell/SellEntryFormState.kt`, `SellEntryScreen.kt` | Mandi shulk % field |
| `composeApp/.../ui/sell/SellTransactionsListScreen.kt`, `SellTransactionReceiptScreen.kt`, receipts `ReceiptHtmlRenderer` | chips, titles, rate text |
| `composeApp/.../ui/deal/DealEntryViewModel.kt`, `DealEntryScreen.kt`, `ui/settings/SettingsScreen.kt` | labour per quintal |
| `composeApp/.../ui/register/DailyRegisterViewModel.kt`, `DailyCashRegisterScreen.kt` | non-cash rows + tag + subtotal |

---

### Task 0: Baseline

**Files:** none.

- [ ] **Step 1:** In the worktree run all three Gradle test commands and the backend pytest. Record the counts.
- [ ] **Step 2:** Expected: all green. If anything fails on the untouched base branch, stop and report; do not fix unrelated failures.
- [ ] **Step 3:** No commit.

---

### Task 1: Domain money rules (mandi shulk rate, deal labour, settle builder)

**Files:**
- Modify: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/math/BuySellMath.kt:25-52,86`
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/math/DealLabour.kt`
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/usecase/SellReceiptBuilder.kt`
- Modify: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/CashTransaction.kt` (add two fields)
- Modify: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/SellEntry.kt` (add `mandiShulkBps`)
- Modify: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/TradeSettings.kt:11`
- Test: `core-domain/src/commonTest/kotlin/com/appwork/mandisamiti/domain/BuySellMathTest.kt` (extend), plus new `DealLabourTest.kt`, `SellReceiptBuilderTest.kt` in the same folder.

**Interfaces:**
- Produces:
  - `SellMathInput(..., val mandiShulkBps: Long = BuySellMath.DEFAULT_MANDI_SHULK_BPS)` (last parameter, defaulted so existing call sites compile)
  - `BuySellMath.DEFAULT_MANDI_SHULK_BPS = 200L` (replaces `MANDI_SHULK_BPS = 150L`; the old constant is deleted and every use migrated, grep `MANDI_SHULK_BPS`)
  - `DealLabour.resolvePaisa(netGrams: Long, ratePaisaPerQuintal: Long, storedPaisa: Long?): Long` returns `storedPaisa` when non-null, else `MandiMathEngine.grossAmountPaisa(netGrams, ratePaisaPerQuintal)`
  - `CashTransaction.referenceNo: String? = null`, `CashTransaction.bankName: String? = null`
  - `SellEntry.mandiShulkBps: Long = BuySellMath.DEFAULT_MANDI_SHULK_BPS`
  - `sealed interface SettleMethod { object Cash; data class Bank(val mode: PaymentMode, val referenceNo: String, val bankName: String?) }`
  - `SellReceiptBuilder.build(id: String, shopId: String, sell: SellEntry, partyId: String, amountPaisa: Long, method: SettleMethod, slipRef: String, note: String?, nowMs: Long): Result<CashTransaction>` fails with `IllegalArgumentException` when Bank mode is not NEFT/RTGS/CHEQUE/UPI or the reference is blank.

- [ ] **Step 1: Write failing tests**

Add to `BuySellMathTest.kt`:

```kotlin
@Test
fun bosShulkUsesEnteredRate() {
    val r = BuySellMath.calculateSell(
        SellMathInput(10, 50_000, 200_000, BillMode.BILL_OF_SUPPLY, 0, null, 0, mandiShulkBps = 150)
    )
    assertEquals(MandiMathEngine.percentageOf(r.grossPaisa, 150), r.mandiShulkPaisa)
}

@Test
fun bosShulkDefaultsToTwoPercent() {
    val r = BuySellMath.calculateSell(
        SellMathInput(10, 50_000, 200_000, BillMode.BILL_OF_SUPPLY, 0, null, 0)
    )
    assertEquals(MandiMathEngine.percentageOf(r.grossPaisa, 200), r.mandiShulkPaisa)
}

@Test
fun cashMemoChargesNoShulkEvenWithRate() {
    val r = BuySellMath.calculateSell(
        SellMathInput(10, 50_000, 200_000, BillMode.NORMAL, 0, null, 0, mandiShulkBps = 200)
    )
    assertEquals(0L, r.mandiShulkPaisa)
}
```

New `DealLabourTest.kt`:

```kotlin
package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.math.DealLabour
import kotlin.test.Test
import kotlin.test.assertEquals

class DealLabourTest {
    @Test fun fiftyQuintalsAtOneRupeeIsFiftyRupees() =
        assertEquals(5_000L, DealLabour.resolvePaisa(5_000_000L, 100L, null))

    @Test fun zeroRateIsZero() =
        assertEquals(0L, DealLabour.resolvePaisa(5_000_000L, 0L, null))

    @Test fun storedAmountWinsForOldDeals() =
        assertEquals(15_000L, DealLabour.resolvePaisa(5_000_000L, 100L, 15_000L))
}
```

New `SellReceiptBuilderTest.kt` (use a minimal `SellEntry` helper; copy the constructor call from `BuySellMathTest`/an existing SellEntry test fixture, grep `SellEntry(` in `core-domain/src/commonTest`):

```kotlin
class SellReceiptBuilderTest {
    private val sell = /* fixture SellEntry with id "s1", buyerId "b1" */

    @Test fun cashBuildsCashRow() {
        val tx = SellReceiptBuilder.build("t1", "shop", sell, "b1", 100_000L, SettleMethod.Cash, "#4", null, 1L).getOrThrow()
        assertEquals(PaymentMode.CASH, tx.paymentMode)
        assertEquals(TransactionType.JAMA_RECEIVED, tx.transactionType)
        assertEquals("SELL", tx.sourceEntryType)
        assertEquals("s1", tx.sourceEntryId)
        assertNull(tx.referenceNo)
    }

    @Test fun bankKeepsModeReferenceAndBankName() {
        val tx = SellReceiptBuilder.build(
            "t1", "shop", sell, "b1", 100_000L,
            SettleMethod.Bank(PaymentMode.NEFT, "UTR123", "SBI"), "#4", null, 1L
        ).getOrThrow()
        assertEquals(PaymentMode.NEFT, tx.paymentMode)
        assertEquals("UTR123", tx.referenceNo)
        assertEquals("SBI", tx.bankName)
        assertEquals("b1", tx.partyId)
    }

    @Test fun bankWithoutReferenceIsRejected() {
        val r = SellReceiptBuilder.build(
            "t1", "shop", sell, "b1", 100_000L,
            SettleMethod.Bank(PaymentMode.RTGS, "  ", null), "#4", null, 1L
        )
        assertTrue(r.isFailure)
    }

    @Test fun bankWithCashModeIsRejected() {
        val r = SellReceiptBuilder.build(
            "t1", "shop", sell, "b1", 100_000L,
            SettleMethod.Bank(PaymentMode.CASH, "X", null), "#4", null, 1L
        )
        assertTrue(r.isFailure)
    }

    @Test fun bankSettleLeavesDrawerUnchangedCashRaisesIt() {
        val window = DayWindow(0L, 10L)
        val bank = SellReceiptBuilder.build("t1", "shop", sell, "b1", 100_000L,
            SettleMethod.Bank(PaymentMode.UPI, "u@bank", null), "#4", null, 5L).getOrThrow()
        val cash = SellReceiptBuilder.build("t2", "shop", sell, "b1", 100_000L, SettleMethod.Cash, "#4", null, 5L).getOrThrow()
        assertEquals(0L, CashDrawer.summarize(listOf(bank), window).closingPaisa)
        assertEquals(100_000L, CashDrawer.summarize(listOf(cash), window).closingPaisa)
    }
}
```

Add one `TradeSettings()` default test to an existing settings test: `assertEquals(100L, TradeSettings().sellLabourPaisaPerQuintal)`.

- [ ] **Step 2: Run to confirm failure**

Run: `./gradlew :core-domain:jvmTest`
Expected: compile errors (`mandiShulkBps`, `DealLabour`, `SellReceiptBuilder` unresolved).

- [ ] **Step 3: Implement**

`BuySellMath.kt`: replace `const val MANDI_SHULK_BPS = 150L` with `const val DEFAULT_MANDI_SHULK_BPS = 200L`; add `val mandiShulkBps: Long = DEFAULT_MANDI_SHULK_BPS` as the last `SellMathInput` field; change line 86 to `MandiMathEngine.percentageOf(gross, input.mandiShulkBps)`.

`DealLabour.kt`:

```kotlin
package com.appwork.mandisamiti.domain.math

object DealLabour {
    fun resolvePaisa(netGrams: Long, ratePaisaPerQuintal: Long, storedPaisa: Long?): Long =
        storedPaisa ?: MandiMathEngine.grossAmountPaisa(netGrams, ratePaisaPerQuintal)
}
```

`SellReceiptBuilder.kt`:

```kotlin
package com.appwork.mandisamiti.domain.usecase

import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.TransactionType

sealed interface SettleMethod {
    data object Cash : SettleMethod
    data class Bank(val mode: PaymentMode, val referenceNo: String, val bankName: String?) : SettleMethod
}

object SellReceiptBuilder {
    private val bankModes = setOf(PaymentMode.NEFT, PaymentMode.RTGS, PaymentMode.CHEQUE, PaymentMode.UPI)

    fun build(
        id: String, shopId: String, sell: SellEntry, partyId: String, amountPaisa: Long,
        method: SettleMethod, slipRef: String, note: String?, nowMs: Long,
    ): Result<CashTransaction> {
        if (amountPaisa <= 0L) return Result.failure(IllegalArgumentException("amount"))
        val mode: PaymentMode
        var ref: String? = null
        var bank: String? = null
        when (method) {
            SettleMethod.Cash -> mode = PaymentMode.CASH
            is SettleMethod.Bank -> {
                if (method.mode !in bankModes) return Result.failure(IllegalArgumentException("mode"))
                if (method.referenceNo.isBlank()) return Result.failure(IllegalArgumentException("reference"))
                mode = method.mode
                ref = method.referenceNo.trim()
                bank = method.bankName?.trim()?.takeIf { it.isNotEmpty() }
            }
        }
        val label = if (mode == PaymentMode.CASH) "नकद प्राप्त" else "${mode.name} प्राप्त"
        val noteSuffix = if (!note.isNullOrBlank()) ": $note" else ""
        return Result.success(
            CashTransaction(
                id = id, shopId = shopId, partyId = partyId,
                transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = amountPaisa, paymentMode = mode, transactionDate = nowMs,
                remarks = "बिक्री पर्ची $slipRef $label$noteSuffix",
                createdAt = nowMs, updatedAt = nowMs,
                sourceEntryType = "SELL", sourceEntryId = sell.id,
                referenceNo = ref, bankName = bank,
            )
        )
    }
}
```

Models: add `val referenceNo: String? = null, val bankName: String? = null` after `lendId` in `CashTransaction`; add `val mandiShulkBps: Long = BuySellMath.DEFAULT_MANDI_SHULK_BPS` at the end of `SellEntry`; set `sellLabourPaisaPerQuintal: Long = 100L` and update its comment in `TradeSettings.kt:11`.

Then `grep -rn MANDI_SHULK_BPS --include=*.kt .` and replace remaining uses (`SellEntryFormState.kt:285` is migrated in Task 6; for now point it at `BuySellMath.DEFAULT_MANDI_SHULK_BPS` so the build compiles).

- [ ] **Step 4: Run to confirm pass**

Run: `./gradlew :core-domain:jvmTest :composeApp:compileKotlinJvm`
Expected: PASS. If a test asserts the old `sellLabourPaisaPerQuintal == 0L` default, update that expectation to `100L` and note it in the commit.

- [ ] **Step 5: Commit**

```bash
git add core-domain composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/sell/SellEntryFormState.kt
git commit -m "feat(mandisamiti): mandi shulk rate, deal labour and sell settle builder in domain

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Schema, migration and repositories

**Files:**
- Create: `core-database/src/commonMain/sqldelight/com/appwork/mandisamiti/database/15.sqm`
- Modify: `core-database/src/commonMain/sqldelight/com/appwork/mandisamiti/database/AppDatabase.sq` (tables lines ~110-133 and ~193-229; `insertCashTransaction` ~412; `insertSellEntry` ~737; `insertSellEntryIfAbsent` ~819 and any cash `IfAbsent` variant, find with `grep -n "cashTransactionEntity(" AppDatabase.sq`)
- Modify: `core-data/.../repository/OfflineFirstCashTransactionRepository.kt` (~229 write, ~254 read)
- Modify: `core-data/.../repository/OfflineFirstSellRepository.kt` (~150 write, ~183 read)
- Test: `core-data/src/jvmTest/.../BankSettleRoundTripTest.kt` (new), pattern: `UdhaarSchemaRoundTripTest.kt` in the same folder.

**Interfaces:**
- Consumes: `CashTransaction.referenceNo/bankName`, `SellEntry.mandiShulkBps` from Task 1.
- Produces: persisted columns `reference_no`, `bank_name`, `mandi_shulk_bps`.

- [ ] **Step 1: Write failing round-trip test.** Copy the setup of `UdhaarSchemaRoundTripTest`. Record a `CashTransaction(paymentMode = NEFT, referenceNo = "UTR1", bankName = "SBI", ...)` through `OfflineFirstCashTransactionRepository.recordTransaction` and read it back; assert both fields. Insert a `SellEntry(mandiShulkBps = 150)` through the sell repository and read it back; assert `150`. Add a third test that inserts an old-shape row with `mandi_shulk_bps` omitted and asserts the DB default is `200` (new rows via SQL default).
- [ ] **Step 2:** Run `./gradlew :core-data:jvmTest --tests '*BankSettleRoundTripTest*'`. Expected: FAIL (no columns).
- [ ] **Step 3: Implement.**

`15.sqm`:

```sql
ALTER TABLE cashTransactionEntity ADD COLUMN reference_no TEXT;
ALTER TABLE cashTransactionEntity ADD COLUMN bank_name TEXT;
ALTER TABLE sellEntryEntity ADD COLUMN mandi_shulk_bps INTEGER NOT NULL DEFAULT 200;
-- Slips saved before this migration were charged at the old fixed 1.5%. Keep that so reprints stay true.
UPDATE sellEntryEntity SET mandi_shulk_bps = 150;
```

The `UPDATE` runs once at migration time, after the `ALTER`, so only pre-existing rows get 150. New installs create the table from `AppDatabase.sq` with default 200.

In `AppDatabase.sq` add `reference_no TEXT, bank_name TEXT` after `lend_id TEXT` in `cashTransactionEntity`; add `mandi_shulk_bps INTEGER NOT NULL DEFAULT 200` after `is_khaate` in `sellEntryEntity`; extend the column lists and the `VALUES (?, ...)` placeholders of each insert query that names these tables (add 2 placeholders to cash inserts, 1 to sell inserts). Update the repository mappers with the new fields (`reference_no = transaction.referenceNo`, `referenceNo = reference_no`, and so on).

- [ ] **Step 4:** Run `./gradlew :core-database:jvmTest :core-data:jvmTest`. Expected: PASS including `PilotSchemaMigrationTest` (add a migration case: build a v14 DB with one sell row, migrate to 15, assert `mandi_shulk_bps == 150` and new cash columns are null).
- [ ] **Step 5: Commit** `feat(mandisamiti): bank reference, bank name and mandi shulk rate columns (15.sqm)`.

---

### Task 3: Sync DTOs and backend (migration 0008)

**Files:**
- Modify: `core-data/.../sync/model/SyncDto.kt` (cash DTO near line 68; sell DTO)
- Modify: `core-data/.../sync/SyncEngine.kt` (push ~150 cash, ~178 sell; pull ~405 cash, ~453 sell)
- Create: `backend/migrations/versions/0008_bank_settle_fields.py`
- Modify: `backend/app/models/transaction.py`, `backend/app/models/sell_entry.py`, `backend/app/schemas/transaction.py`, `backend/app/schemas/entries.py` (sell schema near line 56), `backend/app/api/v1/endpoints/sync.py` (cash ~224/~251, sell upsert block)
- Test: `backend/tests/test_sync_bank_settle.py` (new), pattern `test_sync_payment_link.py`; `core-data/src/jvmTest/.../SyncEngineTest.kt` (extend).

**Interfaces:**
- Produces: wire fields `reference_no: String? = null`, `bank_name: String? = null` on the cash DTO and `mandi_shulk_bps: Long = 200` on the sell DTO. Old clients omit them; backend defaults apply.

- [ ] **Step 1: Write failing tests.**
  - Backend `test_sync_bank_settle.py`: push a cash transaction with `payment_mode="NEFT"`, `reference_no="UTR1"`, `bank_name="SBI"`; pull it back; assert fields. Push one without the fields; assert `None`. Push a sell entry with `mandi_shulk_bps=150`; pull; assert 150; push one without it; assert 200.
  - `SyncEngineTest`: a transaction with reference and bank name survives the push DTO and a pull applies them locally.
- [ ] **Step 2:** `cd backend && python -m pytest tests/test_sync_bank_settle.py -q` and `./gradlew :core-data:jvmTest --tests '*SyncEngineTest*'`. Expected: FAIL.
- [ ] **Step 3: Implement.** Mirror exactly how `source_entry_id` (cash) and `bill_mode` (sell) flow through every file listed (use `grep -n "source_entry_id"` and `grep -n "bill_mode"` as the checklist; each hit needs a sibling line). `0008_bank_settle_fields.py`:

```python
"""bank settle fields"""
from alembic import op
import sqlalchemy as sa

revision = "0008"
down_revision = "0006"  # change to "0007" only if the Udhaar migration is on disk when you start
branch_labels = None
depends_on = None


def upgrade():
    op.add_column("cash_transactions", sa.Column("reference_no", sa.String(), nullable=True))
    op.add_column("cash_transactions", sa.Column("bank_name", sa.String(), nullable=True))
    op.add_column("sell_entries", sa.Column("mandi_shulk_bps", sa.Integer(), nullable=False, server_default="200"))


def downgrade():
    op.drop_column("sell_entries", "mandi_shulk_bps")
    op.drop_column("cash_transactions", "bank_name")
    op.drop_column("cash_transactions", "reference_no")
```

Check the real table names with `grep -n __tablename__ backend/app/models/sell_entry.py backend/app/models/transaction.py` and match the revision chain with `ls backend/migrations/versions`. `test_migrations.py` must pass.

- [ ] **Step 4:** Run backend `python -m pytest -q` and `./gradlew :core-data:jvmTest`. Expected: PASS.
- [ ] **Step 5: Commit** `feat(mandisamiti): sync bank settle fields and mandi shulk rate (backend 0008)`. Do not deploy.

---

### Task 4: Settle logic in both view models

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/home/HomeViewModel.kt:526-575` (`settleSellSlipCash`)
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/ledger/PartyLedgerViewModel.kt:368-406` (`settleSellSlip`)
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/home/HomeViewModelSellSettlementTest.kt` (extend), `.../ui/ledger/PartyLedgerViewModelTest.kt` (extend)

**Interfaces:**
- Consumes: `SellReceiptBuilder`, `SettleMethod` from Task 1.
- Produces:
  - `HomeViewModel.settleSellSlip(sellId: String, amountPaisa: Long, method: SettleMethod, onComplete: (() -> Unit)? = null)`. `settleSellSlipCash(sellId, amount, onComplete)` stays as a one-line delegate with `SettleMethod.Cash` so existing tests and call sites keep compiling.
  - `PartyLedgerViewModel.settleSellSlip(sellId: String, amountPaisa: Long, note: String? = null, method: SettleMethod = SettleMethod.Cash)`.

- [ ] **Step 1: Write failing tests** in `HomeViewModelSellSettlementTest` using its `r.vm` fixture:

```kotlin
@Test fun bankSettleStoresModeReferenceAndDoesNotMoveGalla() = runTest {
    val before = r.cashRepo.currentDrawerClosing()   // use the helper this test file already uses for galla assertions
    r.vm.settleSellSlip("s1", 400_000L, SettleMethod.Bank(PaymentMode.NEFT, "UTR9", "SBI")) { done = true }
    val tx = r.cashRepo.recorded.last()               // adapt to the fixture's recorded list
    assertEquals(PaymentMode.NEFT, tx.paymentMode)
    assertEquals("UTR9", tx.referenceNo)
    assertEquals("SBI", tx.bankName)
    assertEquals(before, r.cashRepo.currentDrawerClosing())
    assertEquals(400_000L, r.sellRepo.get("s1").receivedPaisa)   // slip still reduced
}

@Test fun bankSettleWithoutReferenceWritesNothing() = runTest {
    r.vm.settleSellSlip("s1", 100_000L, SettleMethod.Bank(PaymentMode.RTGS, "", null))
    assertTrue(r.cashRepo.recorded.isEmpty())
    assertEquals(0L, r.sellRepo.get("s1").receivedPaisa)
}
```

Read the first 100 lines of the test file and adapt the helper names (`r.cashRepo`, galla check) to what exists; the assertions above are the contract. Add the same two cases to `PartyLedgerViewModelTest`.

- [ ] **Step 2:** `./gradlew :composeApp:jvmTest --tests '*HomeViewModelSellSettlementTest*' --tests '*PartyLedgerViewModelTest*'`. Expected: FAIL (no overload).
- [ ] **Step 3: Implement.** In each VM, build the row with `SellReceiptBuilder.build(...)` first. On `isFailure`, return before touching the sell entry so a bad bank input writes nothing. Then edit the sell entry (existing logic) and `recordTransaction(tx)`. Remove the inline `CashTransaction(...)` construction. Keep the existing cap `minOf(amount, balanceDue)` in Home and the existing note handling in Ledger (pass `note`). Add an Android-safe `IdGenerator.newId()` call as before.
- [ ] **Step 4:** Run the same command. Expected: PASS, and the older cash tests still pass.
- [ ] **Step 5: Commit** `feat(mandisamiti): settle sell slips by cash or bank in view models`.

---

### Task 5: Settle sheet UI (Cash / Bank switch)

**Files:**
- Modify: `composeApp/.../ui/parchi/FarmerSlipCashSettlementSheet.kt` (`TraderSlipCashSettlementSheet` at line 82; private `SlipCashSettlementSheet` below it). Farmer sheet is unchanged.
- Modify call sites: `ParchiListScreen.kt:560`, `SellTransactionReceiptScreen.kt:160`, `HomeScreen.kt:232`, `PartyLedgerScreen.kt:1236`.
- Test: `composeApp/src/jvmTest/.../ui/parchi/SettleBankInputTest.kt` (new, pure state holder test).

**Interfaces:**
- Consumes: `SettleMethod` (Task 1), VM overloads (Task 4).
- Produces: `TraderSlipCashSettlementSheet(..., onConfirmReceipt: (amountPaisa: Long, method: SettleMethod) -> Unit)`. A pure `internal class BankInputState` / function `internal fun bankInputValid(mode: PaymentMode?, referenceNo: String): Boolean` so validity is testable without Compose.

- [ ] **Step 1: Failing test:**

```kotlin
class SettleBankInputTest {
    @Test fun needsModeAndReference() {
        assertFalse(bankInputValid(null, "UTR1"))
        assertFalse(bankInputValid(PaymentMode.NEFT, " "))
        assertTrue(bankInputValid(PaymentMode.CHEQUE, "000123"))
        assertFalse(bankInputValid(PaymentMode.CASH, "000123"))
    }
}
```

- [ ] **Step 2:** Run `./gradlew :composeApp:jvmTest --tests '*SettleBankInputTest*'`. Expected: FAIL.
- [ ] **Step 3: Implement.** Add `internal fun bankInputValid(mode: PaymentMode?, referenceNo: String) = mode in setOf(NEFT, RTGS, CHEQUE, UPI) && referenceNo.isNotBlank()`. In `SlipCashSettlementSheet` add a two-segment switch "नकद" / "बैंक" (default नकद) using the same segmented-control composable other sheets use (grep `SingleChoiceSegmentedButtonRow` in `composeApp`; reuse the project's wrapper if one exists). When Bank is selected: four mode chips (NEFT, RTGS, चेक = CHEQUE, UPI), a required "संदर्भ संख्या (UTR / चेक नंबर / UPI ID)" field, an optional "बैंक का नाम" field. Hide the Galla-liquidity card in Bank mode (not relevant). The confirm button is enabled when the amount is valid and (Cash or `bankInputValid`). Title text: "बैंक प्राप्त" for Bank. Only the receiving side (`isReceiving = true`) shows the switch; the farmer sheet passes `showBankOption = false`. Tokens only, vector icons only, content scrolls (`verticalScroll` already present), fields use `Modifier.fillMaxWidth()` so nothing overflows at 390dp.

Update the four call sites to pass `onConfirmReceipt = { amount, method -> vm.settleSellSlip(sellId, amount, method) }` (Home and Parchi use `HomeViewModel`, ledger uses its own).

- [ ] **Step 4:** `./gradlew :composeApp:jvmTest`. Expected: PASS. Then run the desktop target if one exists (`./gradlew :composeApp:run`) or the Android emulator, open a sell slip, switch to Bank, confirm: no reference blocks the button, reference enables it.
- [ ] **Step 5: Commit** `feat(mandisamiti): cash or bank switch on sell slip settle sheet`.

---

### Task 6: Sell entry Mandi shulk field, chips, titles and receipt text

**Files:**
- Modify: `composeApp/.../ui/sell/SellEntryFormState.kt` (line ~285 and the input state near ~155-190)
- Modify: `composeApp/.../ui/sell/SellEntryScreen.kt` (charges row ~659)
- Modify: `composeApp/.../ui/sell/SellTransactionsListScreen.kt` (chips ~202)
- Modify: `composeApp/.../ui/sell/SellTransactionReceiptScreen.kt` and `ReceiptHtmlRenderer` (find with `grep -rn "Mandi Sale Bill\|मंडी प्रभार" composeApp/src`)
- Test: `composeApp/src/commonTest/.../ui/sell/SellEntryFormStateTest.kt` (extend), `composeApp/src/jvmTest/.../ui/receipts/` (new `SellReceiptWordingTest.kt`, pattern `ReceiptChargesAndDefaultsTest.kt`)

**Interfaces:**
- Consumes: `SellMathInput.mandiShulkBps`, `SellEntry.mandiShulkBps` (Task 1).
- Produces: `SellEntryFormState.mandiShulkPercent: String` (default `"2"`), `bpsOf(percent)` via `MandiMathEngine.parsePercentToBasisPoints`; `SellEntry.mandiShulkBps` persisted on save; `internal fun billTypeLabel(billMode: BillMode): String` returning "सप्लाई बिल" or "कैश मेमो"; `internal fun shulkLabel(bps: Long): String` returning for example "मंडी शुल्क (1.5%)".

- [ ] **Step 1: Failing tests.**

```kotlin
@Test fun shulkRateFromFormChangesCharge() {
    val s = bosMerchantState(bags = 10, weightGrams = 50_000, rate = 200_000).copy(mandiShulkPercent = "1.5")
    assertEquals(MandiMathEngine.percentageOf(s.grossPaisa, 150), s.mandiShulkPaisa)
}
@Test fun cashMemoIgnoresShulkField() { /* NORMAL bill mode with mandiShulkPercent = "2" gives 0 */ }
@Test fun billTypeLabels() {
    assertEquals("सप्लाई बिल", billTypeLabel(BillMode.BILL_OF_SUPPLY))
    assertEquals("कैश मेमो", billTypeLabel(BillMode.NORMAL))
}
@Test fun shulkLabelPrintsEnteredRate() {
    assertEquals("मंडी शुल्क (1.5%)", shulkLabel(150))
    assertEquals("मंडी शुल्क (2%)", shulkLabel(200))
}
@Test fun cashMemoTitle() { /* rendered receipt for a NORMAL slip contains "कैश मेमो (Cash Memo)" and not "बिक्री पर्चा" */ }
```

Build `bosMerchantState` from the existing helpers in `SellEntryFormStateTest` (lines ~222-281 show the pattern with `tradeSettings`).

- [ ] **Step 2:** Run `./gradlew :composeApp:jvmTest :composeApp:allTests`. Expected: FAIL (field and helpers missing).
- [ ] **Step 3: Implement.**
  - Form state: add `mandiShulkPercent`, compute `mandiShulkPaisa` from it (line ~285), include `mandiShulkBps` in the saved `SellEntry`. When opening an existing slip, load `entry.mandiShulkBps` into the field so edits keep the stored rate.
  - Screen: in the charges row, when `isBos && partyKind == MERCHANT`, render three equal-weight fields (`Modifier.weight(1f)`, each `minWidth = 0.dp`): कमीशन %, मंडी शुल्क %, मजदूरी (रु/क्विंटल). Otherwise keep the two-field row.
  - List: render a `billTypeLabel` chip before the party chip for every slip, styled like the existing "सप्लाई बिल" chip, with a neutral (non-accent) token for कैश मेमो. No emoji.
  - Receipt screen + HTML renderer: replace the fixed "मंडी प्रभार एवं उपकर (2%)" with `shulkLabel(entry.mandiShulkBps)`; change the non-BoS title to "कैश मेमो (Cash Memo)".
  - `shulkLabel`: whole percent prints without decimals; otherwise strip trailing zeros (`bps / 100.0` formatting done with integer math: `val whole = bps / 100; val frac = bps % 100`, then `"$whole"` or `"$whole.${frac.toString().padStart(2,'0').trimEnd('0')}"`).
- [ ] **Step 4:** Run `./gradlew :composeApp:jvmTest`. Expected: PASS. Manually view at 390dp light and dark: three fields fit without horizontal scroll.
- [ ] **Step 5: Commit** `feat(mandisamiti): editable mandi shulk rate, bill-type chips, cash memo title`.

---

### Task 7: Deal labour per quintal

**Files:**
- Modify: `composeApp/.../ui/deal/DealEntryViewModel.kt` (state line 56, defaults ~118, edit load ~173, recalc/save ~357, ~392, ~408, ~431, keypad cases ~250-294)
- Modify: `composeApp/.../ui/deal/DealEntryScreen.kt` (field ~399, invoice row ~427-432)
- Modify: `composeApp/.../ui/settings/SettingsScreen.kt` (label near ~573-598: relabel to "व्यापारी हम्माली (₹ प्रति क्विंटल)", prefill shows ₹1 when stored value is the new default)
- Test: `composeApp/src/jvmTest/.../ui/deal/DealEntryViewModelTest.kt` (extend; fixture near line 143 shows how settings are injected)

**Interfaces:**
- Consumes: `DealLabour.resolvePaisa` (Task 1), `TradeSettings.sellLabourPaisaPerQuintal`.
- Produces: `DealEntryUiState.labourRateText: String` (₹ per quintal, replaces the meaning of `labourChargesText`; keep the property name `labourChargesText` for the keypad plumbing but treat it as the **rate**), `DealEntryUiState.storedLabourPaisa: Long?` (non-null only for an opened existing deal until the rate is edited), and `DealEntryUiState.labourPaisa: Long` computed through `DealLabour.resolvePaisa(netWeightGrams, parsedRate, storedLabourPaisa)`.

- [ ] **Step 1: Failing tests:**

```kotlin
@Test fun labourDefaultsToSettingsRatePerQuintal() = runTest {
    val vm = newVm(settings = TradeSettings(sellLabourPaisaPerQuintal = 100L))
    // enter 50 quintals net (follow the keypad helper this file already uses)
    assertEquals("1", vm.uiState.value.labourChargesText)
    assertEquals(5_000L, vm.uiState.value.labourPaisa)
}

@Test fun zeroRateGivesZeroLabour() = runTest { /* rate "0" or "" gives labourPaisa 0 */ }

@Test fun editingOldDealKeepsStoredLabourUntilRateTouched() = runTest {
    // existing deal with labourChargePaisa = 15_000; open for edit
    assertEquals(15_000L, vm.uiState.value.labourPaisa)
    // type a new rate
    assertEquals(/* rate x quintals */, vm.uiState.value.labourPaisa)
}

@Test fun savedDealStoresComputedLabourTotal() = runTest {
    // save a new deal at rate 1, 50 quintals, assert repository got labourChargePaisa = 5_000
}
```

- [ ] **Step 2:** `./gradlew :composeApp:jvmTest --tests '*DealEntryViewModelTest*'`. Expected: FAIL.
- [ ] **Step 3: Implement.** Default (line ~118) becomes `paisaToInputString(settings.sellLabourPaisaPerQuintal)`. Replace both `parseRupeesToPaisa(state.labourChargesText)` uses (lines ~357 and ~392) with `state.labourPaisa`-style resolution through `DealLabour.resolvePaisa`. In `loadExistingDeal` (~173) set `storedLabourPaisa = deal.labourChargePaisa` and `labourChargesText` to the per-quintal rate if `netWeightGrams > 0` (stored ÷ quintals, rounded), otherwise `""`. The first keypad edit on `ActiveInputField.LABOUR_CHARGES` sets `storedLabourPaisa = null`. Screen: relabel the field "हम्माली (₹ प्रति क्विंटल)"; invoice row text `"− हम्माली ${quintals} क्विं × ₹${rate} = ₹${total}"` using existing weight-formatting helpers (grep the screen for how quintals are already shown). Hide the row when labour is 0.
- [ ] **Step 4:** `./gradlew :composeApp:jvmTest`. Expected: PASS. Manually: open Settings on a fresh install, field shows ₹1; open an old deal, total unchanged.
- [ ] **Step 5: Commit** `feat(mandisamiti): deal labour per quintal with ₹1 default`.

---

### Task 8: Galla bank rows and non-cash line

**Files:**
- Modify: `composeApp/.../ui/register/DailyRegisterViewModel.kt` (state near line 125)
- Modify: `composeApp/.../ui/register/DailyCashRegisterScreen.kt` (filter at line 153, lists below it, cash total area ~492)
- Test: `composeApp/src/jvmTest/.../ui/register/` (new `NonCashRowsTest.kt`, pattern: existing register VM tests, `ls composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/register`), `core-domain/src/commonTest/.../bahi/` (extend with the balance case)

**Interfaces:**
- Consumes: `CashTransaction.paymentMode/referenceNo/bankName` (Task 1).
- Produces: `internal fun nonCashTotalPaisa(txs: List<CashTransaction>): Long` (live JAMA minus live UDHAR for non-cash/non-book-entry rows is NOT used; spec wants a plain display total, so: sum of non-cash JAMA_RECEIVED amounts minus non-cash UDHAR_GIVEN amounts, voided and deleted excluded, `BOOK_ENTRY` excluded); `internal fun modeTag(mode: PaymentMode): String` ("Cash", "NEFT", "RTGS", "Cheque", "UPI"); `DailyRegisterUiState.nonCashTotalPaisa: Long`.

- [ ] **Step 1: Failing tests:**

```kotlin
@Test fun visibleRowsIncludeBankRowsWithTagAndCashTotalExcludesThem() {
    val cash = tx("c1", JAMA_RECEIVED, 100_000L, PaymentMode.CASH)
    val bank = tx("b1", JAMA_RECEIVED, 250_000L, PaymentMode.NEFT)
    assertEquals(250_000L, nonCashTotalPaisa(listOf(cash, bank)))
    assertEquals(100_000L, CashDrawer.summarize(listOf(cash, bank), DayWindow(0, Long.MAX_VALUE)).closingPaisa)
    assertEquals("NEFT", modeTag(PaymentMode.NEFT))
}
@Test fun voidedBankRowIsIgnored() { /* isVoid = true gives 0 */ }
```

Bahi test in `core-domain/src/commonTest/.../bahi/`: build a day with one bank settle (NEFT, buyer-named) using the existing builder test fixtures and assert the day difference is 0. Per the spec, `BahiDayPageBuilder` is not edited; if this test fails, stop and report instead of changing the builder.

- [ ] **Step 2:** Run both modules' tests. Expected: FAIL for the new helpers; the Bahi test may already pass (that is the proof the builder is fine).
- [ ] **Step 3: Implement.** Change the `visibleTransactions` filter (line ~153) to drop the `paymentMode == CASH || BOOK_ENTRY` restriction while still excluding `ONLINE`/legacy modes only if they are not tracked elsewhere (check git blame on that line; if legacy `ONLINE`/`BANK` rows should appear, include them with tag "Bank"). Keep the `nameList` filter's `UDHAR_GIVEN` rule but without the `CASH` restriction so outgoing bank rows appear in Out. Each row gets a small tag chip from `modeTag` (show for non-cash rows only to keep the cash list unchanged visually) plus, for bank rows, the line "[Buyer] · NEFT · Ref 123456 · SBI" built from party name, mode, `referenceNo`, `bankName` (skip missing parts). Under the cash total add a `Text` "बैंक/चेक/UPI: ₹X (नकद में नहीं)" using a secondary text token, shown only when `nonCashTotalPaisa != 0`. The cash totals (`totalJamaSidePaisa`, drawer closing) keep using `CashDrawer` so they exclude non-cash rows; verify `totalJamaSidePaisa` in `DailyRegisterViewModel` still sums cash only and add a test if it does not.
- [ ] **Step 4:** `./gradlew :core-domain:jvmTest :composeApp:jvmTest`. Expected: PASS. Manually: settle one slip by NEFT, open Galla, see the tagged row in In and the subtotal line while the cash total did not move.
- [ ] **Step 5: Commit** `feat(mandisamiti): bank, cheque and UPI rows tagged on Galla`.

---

### Task 9: Full verification, NGDL pass and logging

**Files:**
- Modify: `projects/MandiSamiti/PROGRESS.md`, `projects/MandiSamiti/FEATURES.json` (paths relative to the worktree root; confirm they exist with `ls`), `vaults/Vivek-K/wiki/log.md`, `vaults/Vivek-K/wiki/hot.md`

- [ ] **Step 1:** Run every test command in "Test commands". Expected: all pass. Record counts against the baseline.
- [ ] **Step 2:** NGDL Definition of Done (§5.7) for the five touched screens (Deal, Sell entry, Sell list, Settle sheet, Galla): view light and dark at desktop width and 390dp; check no horizontal scroll, contrast, no emoji, tokens only. Run `tests/ngdl-tokens` if the project has it. List anything not checked.
- [ ] **Step 3:** Append a `PROGRESS.md` session entry (timestamp, runtime Claude Code, changes, evidence counts, next steps) and add `FEATURES.json` entries: `sell-bank-settle`, `mandi-shulk-rate`, `deal-labour-per-quintal`, `galla-bank-tags`, each with status `passing` only for what was verified, else `failing` with the reason.
- [ ] **Step 4:** Add a `log.md` entry in the vault and refresh `hot.md` (remove the older "spec not yet planned" handoff note). Run `python3 scripts/vault_lint.py` from the repo root; it must report 0 errors.
- [ ] **Step 5:** Commit `docs: log sell slip bank settle verification`. Ask Vivek before any push or backend deploy.

---

## Self-review

- **Spec coverage:** §1 labour → Task 7. §2 shulk % → Tasks 1, 6. §3 tags and title → Task 6. §4 settle → Tasks 1, 4, 5. §5 data → Tasks 2, 3. §6 Galla → Task 8. §7 tests → each task's Step 1 (labour 50 qtl, rate 0, old deal; shulk 1.5% / cash memo none / default 2%; bank leaves drawer, cash raises it; no-reference rejected; Bahi difference 0; register tags and cash total). §8 out of scope respected. §9 open item resolved by Vivek (sell only).
- **Placeholders:** the few `/* ... */` bodies in tests name the exact assertion and the fixture to copy; they are the only spots where helper names must be read from the existing test file first.
- **Type consistency:** `SettleMethod`, `SellReceiptBuilder.build`, `mandiShulkBps`, `referenceNo`, `bankName`, `DealLabour.resolvePaisa`, `bankInputValid`, `nonCashTotalPaisa`, `modeTag`, `billTypeLabel`, `shulkLabel` are used with the same names in every task.
