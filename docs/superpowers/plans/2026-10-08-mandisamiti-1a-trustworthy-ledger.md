# MandiSamiti Phase 1A: Trustworthy Ledger Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every rupee the phone records is exact, is never silently overwritten, and adds up correctly in the party khata and the daily galla. This is the local foundation that sync (Plan 1C) will ship to the server.

**Architecture:** Money math moves fully to integer paisa, basis points and half-up rounding in `MandiMathEngine`. Deals and cash entries become append-only: every create, edit or void bumps a `revision` and writes a JSON snapshot into a new `entryRevisionEntity` table in the same SQLite transaction. Voided entries stay visible but drop out of balances. The galla is computed by a pure `CashDrawer` function over today's local-day window, with a carried-forward opening balance.

**Tech Stack:** Kotlin 2.0.20 Multiplatform, Compose Multiplatform, SQLDelight 2.0.2 (with a numbered `.sqm` migration), kotlinx-datetime, kotlinx-serialization-json, kotlin.uuid, Turbine, kotlinx-coroutines-test.

**Background:** [review](../../../../../vaults/Vivek-K/wiki/concepts/mandisamiti-product-review-2026-10-08.md), sections "Correctness bugs" and "Network strategy".

**Out of scope (later plans):**
- **1B:** server tenant isolation, money as integers on the server, secrets rotation.
- **1C:** background sync, real SMS OTP, hashed MPIN and app lock, server-issued shop id. Registration and `shop_default` stay as they are until 1C.
- **Phase 2:** per-shop buyer commission, mandi cess and hammali settings; day-close; initial cash float.

## Global Constraints
- **Before Task 1:** the working tree has uncommitted Antigravity edits in `RegisterScreen.kt`, `DashboardScreen.kt`, `KhataLedgerTabScreen.kt`, `PartyLedgerScreen.kt` and `DailyCashRegisterScreen.kt`. Ask Vivek whether to commit them as their own commit or drop them. Never mix them into a 1A commit.
- No `Double`/`Float` anywhere a money or commission value is computed or stored. Weights stay in integer grams.
- Every new entry id comes from `IdGenerator.newId()` (UUID v4). No `"prefix_${now}_${random}"` ids.
- `deal_date` and `created_at` never change after an entry is first saved.
- Voided entries are never removed from lists. They are excluded from every balance and galla total.
- UI follows NGDL (`docs/design-system/DESIGN-SYSTEM.md` §2.1, §5): colour tokens from `ui/theme/Color.kt` only, no emoji in UI, touch targets ≥ 48dp, no horizontal scroll at 390dp.
- Test command (from `projects/MandiSamiti`): `./gradlew jvmTest`. Every task ends green.
- Commit messages end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## File Map
| File | Responsibility |
|---|---|
| `core-domain/.../domain/math/MandiMathEngine.kt` | Exact parsing, formatting, rounding, signed settlement |
| `core-domain/.../domain/id/IdGenerator.kt` (new) | UUID ids |
| `core-domain/.../domain/model/VoidReason.kt` (new) | Void reasons with Hindi labels |
| `core-domain/.../domain/model/Deal.kt`, `CashTransaction.kt` | `revision`, `isVoid`, `voidReason`, `farmerCommissionBps` |
| `core-domain/.../domain/repository/DealRepository.kt`, `CashTransactionRepository.kt` | `voidDeal` / `voidTransaction` replace deletes |
| `core-domain/.../domain/cash/CashDrawer.kt` (new) | Day window + galla summary |
| `core-database/.../AppDatabase.sq`, `1.sqm` (new), `databases/1.db` (generated) | Schema v2 + migration |
| `core-data/.../OfflineFirstDealRepository.kt`, `OfflineFirstCashTransactionRepository.kt` | Append-only writes + revisions |
| `composeApp/.../ui/deal/DealEntryViewModel.kt`, `DealEntryScreen.kt`, `ui/slip/DigitalSlipRenderer.kt` | Exact math, edit path, negative payable |
| `composeApp/.../ui/register/DailyRegisterViewModel.kt` | Galla from `CashDrawer` |
| `composeApp/.../ui/ledger/PartyLedgerViewModel.kt`, `PartyLedgerScreen.kt` | Void flow + voided look |
| `composeApp/.../App.kt` | Remove demo data |

(`...` = `src/commonMain/kotlin/com/appwork/mandisamiti`; for `core-database` it is `src/commonMain/sqldelight/com/appwork/mandisamiti/database`.)

---

### Task 1: Exact money math in `MandiMathEngine`

Fixes bugs 3 and 4 from the review (negative payable hidden, rate paisa lost) and the Double commission.

**Files:**
- Modify: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/math/MandiMathEngine.kt`
- Test: `core-domain/src/commonTest/kotlin/com/appwork/mandisamiti/domain/MandiMathEngineTest.kt`

**Interfaces:**
- Produces:
  - `MandiMathEngine.parseRupeesToPaisa(input: String): Long`
  - `MandiMathEngine.parsePercentToBasisPoints(input: String): Long`
  - `MandiMathEngine.paisaToInputString(paisa: Long): String` (also used for basis points → percent text)
  - `MandiMathEngine.gramsToQuintalsInputString(grams: Long): String`
  - `MandiMathEngine.grossAmountPaisa(netWeightGrams: Long, ratePaisaPerQuintal: Long): Long`
  - `MandiMathEngine.percentageOf(amountPaisa: Long, basisPoints: Long): Long`
  - `calculateSettlement(...)` now returns a **signed** `netFarmerPayablePaisa` (negative = farmer owes the shop).

- [ ] **Step 1: Write the failing tests.** Append inside `class MandiMathEngineTest`:

```kotlin
    @Test
    fun parseRupeesToPaisaKeepsPaisa() {
        assertEquals(227_550L, MandiMathEngine.parseRupeesToPaisa("2275.50"))
        assertEquals(227_550L, MandiMathEngine.parseRupeesToPaisa("2275.5"))
        assertEquals(227_500L, MandiMathEngine.parseRupeesToPaisa("2275"))
        assertEquals(5L, MandiMathEngine.parseRupeesToPaisa("0.05"))
        assertEquals(0L, MandiMathEngine.parseRupeesToPaisa(""))
        assertEquals(0L, MandiMathEngine.parseRupeesToPaisa("12.3.4"))
        assertEquals(0L, MandiMathEngine.parseRupeesToPaisa("abc"))
    }

    @Test
    fun paisaToInputStringRoundTrips() {
        assertEquals("2275.50", MandiMathEngine.paisaToInputString(227_550L))
        assertEquals("2275", MandiMathEngine.paisaToInputString(227_500L))
        assertEquals("0.05", MandiMathEngine.paisaToInputString(5L))
        listOf(0L, 1L, 5L, 99L, 227_500L, 227_550L).forEach { paisa ->
            assertEquals(paisa, MandiMathEngine.parseRupeesToPaisa(MandiMathEngine.paisaToInputString(paisa)))
        }
    }

    @Test
    fun percentIsParsedToBasisPoints() {
        assertEquals(150L, MandiMathEngine.parsePercentToBasisPoints("1.5"))
        assertEquals(200L, MandiMathEngine.parsePercentToBasisPoints("2"))
        assertEquals(25L, MandiMathEngine.parsePercentToBasisPoints("0.25"))
        assertEquals("1.50", MandiMathEngine.paisaToInputString(150L))
    }

    @Test
    fun gramsToQuintalsInputStringIsExact() {
        assertEquals("18.4", MandiMathEngine.gramsToQuintalsInputString(1_840_000L))
        assertEquals("0.35", MandiMathEngine.gramsToQuintalsInputString(35_000L))
        assertEquals("18.05123", MandiMathEngine.gramsToQuintalsInputString(1_805_123L))
        assertEquals("0", MandiMathEngine.gramsToQuintalsInputString(0L))
        listOf(1_840_000L, 35_000L, 1_805_123L).forEach { grams ->
            assertEquals(grams, MandiMathEngine.parseQuintalsStringToGrams(MandiMathEngine.gramsToQuintalsInputString(grams)))
        }
    }

    @Test
    fun commissionRoundsHalfUp() {
        // ₹97,831 x 1.5% = ₹1,467.465 -> 146_747 paisa (old Double code truncated to 146_746)
        assertEquals(146_747L, MandiMathEngine.percentageOf(9_783_100L, 150L))
        assertEquals(0L, MandiMathEngine.percentageOf(9_783_100L, 0L))
    }

    @Test
    fun grossAmountRoundsHalfUp() {
        // 1 kg at ₹2,500.50/qtl = ₹25.005 -> 2_501 paisa (old code floored to 2_500)
        assertEquals(2_501L, MandiMathEngine.grossAmountPaisa(1_000L, 250_050L))
        assertEquals(9_783_100L, MandiMathEngine.grossAmountPaisa(1_805_000L, 542_000L))
    }

    @Test
    fun farmerPayableGoesNegativeWhenDeductionsExceedGross() {
        val calc = MandiMathEngine.calculateSettlement(
            grossWeightGrams = 100_000L,          // 1 qtl
            cutWeightGrams = 0L,
            ratePaisaPerQuintal = 100_000L,       // ₹1,000/qtl
            deductions = DeductionsInput(labourChargePaisa = 150_000L) // ₹1,500
        )
        assertEquals(100_000L, calc.grossAmountPaisa)
        assertEquals(-50_000L, calc.netFarmerPayablePaisa) // farmer owes ₹500
    }
```

- [ ] **Step 2: Run the tests and confirm they fail.**
Run: `./gradlew :core-domain:jvmTest`
Expected: compilation FAIL with `Unresolved reference: parseRupeesToPaisa` (and the other new names).

- [ ] **Step 3: Implement.** In `MandiMathEngine.kt`:
  - Add `const val BASIS_POINTS_PER_UNIT = 10_000L` next to the other constants.
  - Add these functions inside `object MandiMathEngine`:

```kotlin
    /** "2275.50" -> 227_550. Blank or malformed input -> 0. Digits past 2 decimals are dropped (the keypad never produces them). */
    fun parseRupeesToPaisa(input: String): Long = parseFixedTwoDecimals(input)

    /** "1.5" (%) -> 150 basis points. */
    fun parsePercentToBasisPoints(input: String): Long = parseFixedTwoDecimals(input)

    /** 227_550 -> "2275.50", 227_500 -> "2275". Inverse of [parseRupeesToPaisa]; also turns basis points into percent text. */
    fun paisaToInputString(paisa: Long): String {
        val whole = paisa / PAISA_PER_RUPEE
        val fraction = paisa % PAISA_PER_RUPEE
        return if (fraction == 0L) "$whole" else "$whole.${fraction.toString().padStart(2, '0')}"
    }

    /** 1_840_000 g -> "18.4". Inverse of [parseQuintalsStringToGrams], no Double involved. */
    fun gramsToQuintalsInputString(grams: Long): String {
        val whole = grams / GRAMS_PER_QUINTAL
        val fraction = (grams % GRAMS_PER_QUINTAL).toString().padStart(5, '0').trimEnd('0')
        return if (fraction.isEmpty()) "$whole" else "$whole.$fraction"
    }

    /** Net weight x rate, rounded half-up to the nearest paisa. */
    fun grossAmountPaisa(netWeightGrams: Long, ratePaisaPerQuintal: Long): Long =
        (netWeightGrams * ratePaisaPerQuintal + GRAMS_PER_QUINTAL / 2) / GRAMS_PER_QUINTAL

    /** amount x basisPoints / 10_000, rounded half-up. */
    fun percentageOf(amountPaisa: Long, basisPoints: Long): Long =
        (amountPaisa * basisPoints + BASIS_POINTS_PER_UNIT / 2) / BASIS_POINTS_PER_UNIT

    private fun parseFixedTwoDecimals(input: String): Long {
        val parts = input.trim().split(".")
        if (parts.size > 2) return 0L
        val whole = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return 0L
        val fraction = if (parts.size == 2) parts[1].padEnd(2, '0').take(2).toLongOrNull() ?: return 0L else 0L
        return whole * 100L + fraction
    }
```

  - In `calculateSettlement`, replace the gross line and the clamped net line:

```kotlin
        val grossAmountPaisa = grossAmountPaisa(netWeightGrams, ratePaisaPerQuintal)
```
```kotlin
        // Signed: negative means the farmer owes the shop (deductions exceed crop value).
        val netFarmerPayable = grossAmountPaisa - totalFarmerDeductions
```

- [ ] **Step 4: Run the tests and confirm they pass.**
Run: `./gradlew :core-domain:jvmTest`
Expected: PASS. If an older test asserted the clamp-to-zero or floor behaviour, it was asserting the bug. Update its expected value to the half-up/signed result and note it in the commit message.

- [ ] **Step 5: Commit.**
```bash
git add core-domain
git commit -m "fix(math): exact paisa parsing, half-up rounding, signed farmer payable"
```

---

### Task 2: Schema v2 — revisions, voids, commission basis points

**Files:**
- Modify: `core-database/build.gradle.kts` (sqldelight block)
- Create: `core-database/src/commonMain/sqldelight/databases/1.db` (generated, committed)
- Modify: `core-database/src/commonMain/sqldelight/com/appwork/mandisamiti/database/AppDatabase.sq`
- Create: `core-database/src/commonMain/sqldelight/com/appwork/mandisamiti/database/1.sqm`
- Modify (keep compiling only): `core-data/.../data/repository/OfflineFirstDealRepository.kt`, `OfflineFirstCashTransactionRepository.kt`
- Test: `core-database/src/jvmTest/kotlin/com/appwork/mandisamiti/database/DatabaseTest.kt`

**Interfaces:**
- Produces:
  - Columns: `dealEntity.farmer_commission_bps`, `revision`, `is_void`, `void_reason`; `cashTransactionEntity.revision`, `is_void`, `void_reason`.
  - Table: `entryRevisionEntity`.
  - Queries: `insertRevision`, `getRevisionsForEntry(entry_id)`.
  - `insertDeal` / `insertCashTransaction` gain the new columns as named parameters.
  - `getPartyBalance` ignores voided rows.
  - `AppDatabase.Schema.version == 2L`.

- [ ] **Step 1: Snapshot the v1 schema before touching the `.sq`.** Change the sqldelight block in `core-database/build.gradle.kts` to:

```kotlin
sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.appwork.mandisamiti.database")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}
```
Run: `./gradlew :core-database:generateCommonMainAppDatabaseSchema`
Expected: `core-database/src/commonMain/sqldelight/databases/1.db` exists. (If Gradle says the task doesn't exist, find the right name with `./gradlew :core-database:tasks --all | grep -i schema`. Do the same for the migration-verify task in Step 6 with `grep -i migration`.) This is the shape of the database already on the Redmi.

- [ ] **Step 2: Write the failing test.** Append inside the test class in `DatabaseTest.kt` (add imports `kotlin.test.assertEquals` if missing):

```kotlin
    @Test
    fun schemaIsVersion2AndBalanceIgnoresVoids() {
        val db = createTestDatabase()
        val q = db.appDatabaseQueries
        assertEquals(2L, AppDatabase.Schema.version)

        q.insertParty("farmer-1", "shop-1", "रामवीर", null, null, "FARMER", null, null, 1L, 1L, 0L, 0L)
        q.insertCashTransaction(
            id = "tx-1", shop_id = "shop-1", party_id = "farmer-1", deal_id = null,
            transaction_type = "UDHAR_GIVEN", amount_paisa = 500_000L, payment_mode = "CASH",
            transaction_date = 1L, voice_note_uri = null, remarks = null,
            created_at = 1L, updated_at = 1L, is_deleted = 0L, sync_status = 0L,
            revision = 1L, is_void = 0L, void_reason = null
        )
        q.insertCashTransaction(
            id = "tx-2", shop_id = "shop-1", party_id = "farmer-1", deal_id = null,
            transaction_type = "UDHAR_GIVEN", amount_paisa = 99_900L, payment_mode = "CASH",
            transaction_date = 2L, voice_note_uri = null, remarks = null,
            created_at = 2L, updated_at = 2L, is_deleted = 0L, sync_status = 0L,
            revision = 2L, is_void = 1L, void_reason = "WRONG_ENTRY"
        )
        assertEquals(500_000L, q.getPartyBalance("farmer-1").executeAsOne().balance_paisa)

        q.insertRevision(
            id = "rev-1", shop_id = "shop-1", entry_id = "tx-2", entry_kind = "CASH",
            revision = 1L, change_kind = "CREATE", snapshot_json = "{}", void_reason = null, changed_at = 2L
        )
        q.insertRevision(
            id = "rev-2", shop_id = "shop-1", entry_id = "tx-2", entry_kind = "CASH",
            revision = 2L, change_kind = "VOID", snapshot_json = "{}", void_reason = "WRONG_ENTRY", changed_at = 3L
        )
        assertEquals(listOf("CREATE", "VOID"), q.getRevisionsForEntry("tx-2").executeAsList().map { it.change_kind })
    }
```

- [ ] **Step 3: Run the test and confirm it fails.**
Run: `./gradlew :core-database:jvmTest`
Expected: compilation FAIL (`No parameter with name 'revision'`, `Unresolved reference: insertRevision`).

- [ ] **Step 4: Update `AppDatabase.sq`.**
  - In `CREATE TABLE dealEntity`, add these lines right after `sync_status INTEGER NOT NULL DEFAULT 0,` and before the `FOREIGN KEY` lines. Columns go last so they match `ALTER TABLE ... ADD COLUMN`:
```sql
    farmer_commission_bps INTEGER NOT NULL DEFAULT 0,
    revision INTEGER NOT NULL DEFAULT 1,
    is_void INTEGER NOT NULL DEFAULT 0,
    void_reason TEXT,
```
  - In `CREATE TABLE cashTransactionEntity`, the same position:
```sql
    revision INTEGER NOT NULL DEFAULT 1,
    is_void INTEGER NOT NULL DEFAULT 0,
    void_reason TEXT,
```
  - After the cash transaction indexes, add:
```sql
-- Append-only audit trail: one row per create / edit / void of a deal or cash entry
CREATE TABLE entryRevisionEntity (
    id TEXT PRIMARY KEY NOT NULL,
    shop_id TEXT NOT NULL,
    entry_id TEXT NOT NULL,
    entry_kind TEXT NOT NULL, -- 'DEAL' | 'CASH'
    revision INTEGER NOT NULL,
    change_kind TEXT NOT NULL, -- 'CREATE' | 'EDIT' | 'VOID'
    snapshot_json TEXT NOT NULL,
    void_reason TEXT,
    changed_at INTEGER NOT NULL,
    sync_status INTEGER NOT NULL DEFAULT 0,
    UNIQUE(entry_id, revision)
);
CREATE INDEX idx_revision_entry ON entryRevisionEntity(entry_id, revision);
```
  - Replace `insertDeal` with:
```sql
insertDeal:
INSERT OR REPLACE INTO dealEntity(
    id, shop_id, farmer_id, buyer_id, commodity_id, deal_status, deal_date,
    bags_count, gross_weight_grams, cut_weight_grams, net_weight_grams,
    rate_paisa_per_unit, gross_amount_paisa,
    farmer_commission_paisa, buyer_commission_paisa, labour_charge_paisa, weighing_charge_paisa, other_deductions_paisa,
    net_farmer_payable_paisa, net_buyer_receivable_paisa,
    receipt_photo_uri, voice_note_uri, remarks,
    created_at, updated_at, is_deleted, sync_status,
    farmer_commission_bps, revision, is_void, void_reason
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
```
  - Replace `insertCashTransaction` with:
```sql
insertCashTransaction:
INSERT OR REPLACE INTO cashTransactionEntity(id, shop_id, party_id, deal_id, transaction_type, amount_paisa, payment_mode, transaction_date, voice_note_uri, remarks, created_at, updated_at, is_deleted, sync_status, revision, is_void, void_reason)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
```
  - In `getPartyBalance`, add `AND is_void = 0` to each of the six sub-selects, right after `AND is_deleted = 0`.
  - Add at the end:
```sql
-- Revision Queries
insertRevision:
INSERT INTO entryRevisionEntity(id, shop_id, entry_id, entry_kind, revision, change_kind, snapshot_json, void_reason, changed_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);

getRevisionsForEntry:
SELECT * FROM entryRevisionEntity WHERE entry_id = ? ORDER BY revision ASC;
```

- [ ] **Step 5: Create the migration `1.sqm`** (same folder as `AppDatabase.sq`):
```sql
ALTER TABLE dealEntity ADD COLUMN farmer_commission_bps INTEGER NOT NULL DEFAULT 0;
ALTER TABLE dealEntity ADD COLUMN revision INTEGER NOT NULL DEFAULT 1;
ALTER TABLE dealEntity ADD COLUMN is_void INTEGER NOT NULL DEFAULT 0;
ALTER TABLE dealEntity ADD COLUMN void_reason TEXT;

ALTER TABLE cashTransactionEntity ADD COLUMN revision INTEGER NOT NULL DEFAULT 1;
ALTER TABLE cashTransactionEntity ADD COLUMN is_void INTEGER NOT NULL DEFAULT 0;
ALTER TABLE cashTransactionEntity ADD COLUMN void_reason TEXT;

CREATE TABLE entryRevisionEntity (
    id TEXT PRIMARY KEY NOT NULL,
    shop_id TEXT NOT NULL,
    entry_id TEXT NOT NULL,
    entry_kind TEXT NOT NULL,
    revision INTEGER NOT NULL,
    change_kind TEXT NOT NULL,
    snapshot_json TEXT NOT NULL,
    void_reason TEXT,
    changed_at INTEGER NOT NULL,
    sync_status INTEGER NOT NULL DEFAULT 0,
    UNIQUE(entry_id, revision)
);
CREATE INDEX idx_revision_entry ON entryRevisionEntity(entry_id, revision);
```

- [ ] **Step 6: Keep the rest of the build compiling.** The insert queries gained parameters. Until Task 3 gives the models real fields, pass fixed values:
  - in `OfflineFirstDealRepository.insertOrReplace`, add `farmer_commission_bps = 0L, revision = 1L, is_void = 0L, void_reason = null,`
  - in `OfflineFirstCashTransactionRepository.recordTransaction`, add `revision = 1L, is_void = 0L, void_reason = null,`

- [ ] **Step 7: Run the tests and the migration check.**
Run: `./gradlew jvmTest :core-database:verifyCommonMainAppDatabaseMigration`
Expected: all PASS. The verify task proves `1.db` + `1.sqm` produces exactly the new `.sq` schema. If it reports a column-order mismatch, move the new columns in the `.sq` so they sit last in the column list.

- [ ] **Step 8: Commit.**
```bash
git add core-database core-data
git commit -m "feat(db): schema v2 with entry revisions, voids and commission basis points"
```

---

### Task 3: Append-only repositories (create / edit / void keep history)

Fixes bugs 1 (edit rewrites history, at the storage layer) and 7 (no void/audit).

**Files:**
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/id/IdGenerator.kt`
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/VoidReason.kt`
- Modify: `core-domain/.../domain/model/Deal.kt`, `CashTransaction.kt`
- Modify: `core-domain/.../domain/repository/DealRepository.kt`, `CashTransactionRepository.kt`
- Modify: `core-data/.../data/repository/OfflineFirstDealRepository.kt`, `OfflineFirstCashTransactionRepository.kt`
- Modify: `core-database/.../AppDatabase.sq` (add `getCashTransactionById`; delete `softDeleteDeal` / `softDeleteCashTransaction`, which voids replace)
- Test: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/LedgerAuditTest.kt` (new)

**Interfaces:**
- Consumes: Task 2 queries.
- Produces:
  - `IdGenerator.newId(): String`
  - `enum class VoidReason(val labelHi: String) { WEIGHING_ERROR("तौल त्रुटि"), WRONG_ENTRY("गलत प्रविष्टि"), DEAL_CANCELLED("सौदा निरस्त") }`
  - `Deal` gains `farmerCommissionBps: Long = 0L`, `revision: Int = 1`, `isVoid: Boolean = false`, `voidReason: VoidReason? = null`.
  - `CashTransaction` gains `revision: Int = 1`, `isVoid: Boolean = false`, `voidReason: VoidReason? = null`.
  - `DealRepository.voidDeal(dealId: String, reason: VoidReason)` replaces `deleteDeal`.
  - `DealRepository.editDeal(deal: Deal)` keeps the stored `dealDate`/`createdAt`, bumps `revision`, and throws `IllegalArgumentException` on a voided deal.
  - `CashTransactionRepository.voidTransaction(transactionId: String, reason: VoidReason)` replaces `deleteTransaction`.
  - Both repository constructors gain `clock: Clock = Clock.System` (kotlinx.datetime) as the last parameter.

- [ ] **Step 1: Write the failing test** `LedgerAuditTest.kt`:

```kotlin
package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstDealRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.DealStatus
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.VoidReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LedgerAuditTest {
    private val fixedNow = Instant.parse("2026-10-08T05:30:00Z")
    private val clock = object : Clock { override fun now() = fixedNow }

    private fun farmer() = Party(
        id = "farmer-1", shopId = "shop-1", name = "रामवीर सिंह", village = "राया",
        partyType = PartyType.FARMER, createdAt = 1_000L, updatedAt = 1_000L
    )

    private fun deal(payable: Long = 100_000L) = Deal(
        id = "deal-1", shopId = "shop-1", farmerId = "farmer-1", buyerId = "buyer-1", commodityId = "comm-1",
        dealStatus = DealStatus.SETTLED, dealDate = 1_000L, bagsCount = 10,
        grossWeightGrams = 1_000_000L, netWeightGrams = 1_000_000L, ratePaisaPerUnit = 250_000L,
        grossAmountPaisa = 2_500_000L, netFarmerPayablePaisa = payable, netBuyerReceivablePaisa = 2_500_000L,
        createdAt = 1_000L, updatedAt = 1_000L
    )

    @Test
    fun editKeepsOriginalDatesAndRecordsRevision() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstDealRepository(db, clock = clock)
        repo.saveDeal(deal())

        repo.editDeal(deal(payable = 90_000L).copy(dealDate = 999_999L, createdAt = 999_999L))

        val saved = repo.getDealById("deal-1")!!
        assertEquals(1_000L, saved.dealDate)
        assertEquals(1_000L, saved.createdAt)
        assertEquals(fixedNow.toEpochMilliseconds(), saved.updatedAt)
        assertEquals(2, saved.revision)
        assertEquals(90_000L, saved.netFarmerPayablePaisa)

        val revisions = db.appDatabaseQueries.getRevisionsForEntry("deal-1").executeAsList()
        assertEquals(listOf("CREATE", "EDIT"), revisions.map { it.change_kind })
        val original = Json.decodeFromString(Deal.serializer(), revisions[0].snapshot_json)
        assertEquals(100_000L, original.netFarmerPayablePaisa)
    }

    @Test
    fun voidedDealLeavesBalanceButStaysInLedger() = runTest {
        val db = createTestDatabase()
        val partyRepo = OfflineFirstPartyRepository(db)
        val repo = OfflineFirstDealRepository(db, clock = clock)
        partyRepo.saveParty(farmer())
        repo.saveDeal(deal())
        assertEquals(-100_000L, partyRepo.getPartyBalanceStream("farmer-1").first()!!.balancePaisa)

        repo.voidDeal("deal-1", VoidReason.WEIGHING_ERROR)

        assertEquals(0L, partyRepo.getPartyBalanceStream("farmer-1").first()!!.balancePaisa)
        val listed = repo.getDealsByFarmerStream("farmer-1").first().single()
        assertTrue(listed.isVoid)
        assertEquals(VoidReason.WEIGHING_ERROR, listed.voidReason)
        assertEquals(
            listOf("CREATE", "VOID"),
            db.appDatabaseQueries.getRevisionsForEntry("deal-1").executeAsList().map { it.change_kind }
        )
    }

    @Test
    fun voidedDealCannotBeEdited() = runTest {
        val repo = OfflineFirstDealRepository(createTestDatabase(), clock = clock)
        repo.saveDeal(deal())
        repo.voidDeal("deal-1", VoidReason.DEAL_CANCELLED)
        assertFailsWith<IllegalArgumentException> { repo.editDeal(deal(payable = 1L)) }
    }

    @Test
    fun voidedCashEntryLeavesBalance() = runTest {
        val db = createTestDatabase()
        val partyRepo = OfflineFirstPartyRepository(db)
        val cashRepo = OfflineFirstCashTransactionRepository(db, clock = clock)
        partyRepo.saveParty(farmer())
        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-1", shopId = "shop-1", partyId = "farmer-1",
                transactionType = TransactionType.UDHAR_GIVEN, amountPaisa = 500_000L,
                transactionDate = 1_000L, createdAt = 1_000L, updatedAt = 1_000L
            )
        )
        assertEquals(500_000L, partyRepo.getPartyBalanceStream("farmer-1").first()!!.balancePaisa)

        cashRepo.voidTransaction("tx-1", VoidReason.WRONG_ENTRY)

        assertEquals(0L, partyRepo.getPartyBalanceStream("farmer-1").first()!!.balancePaisa)
        assertTrue(cashRepo.getTransactionsByPartyStream("farmer-1").first().single().isVoid)
        assertEquals(
            listOf("CREATE", "VOID"),
            db.appDatabaseQueries.getRevisionsForEntry("tx-1").executeAsList().map { it.change_kind }
        )
    }
}
```

- [ ] **Step 2: Run it and confirm it fails.**
Run: `./gradlew :core-data:jvmTest --tests "*LedgerAuditTest*"`
Expected: compilation FAIL (`Unresolved reference: VoidReason`, `voidDeal`, `clock`).

- [ ] **Step 3: Add the domain types.**
`IdGenerator.kt`:
```kotlin
package com.appwork.mandisamiti.domain.id

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Globally unique entry ids, so entries made offline on different phones never collide when synced. */
object IdGenerator {
    @OptIn(ExperimentalUuidApi::class)
    fun newId(): String = Uuid.random().toString()
}
```
`VoidReason.kt`:
```kotlin
package com.appwork.mandisamiti.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class VoidReason(val labelHi: String) {
    WEIGHING_ERROR("तौल त्रुटि"),
    WRONG_ENTRY("गलत प्रविष्टि"),
    DEAL_CANCELLED("सौदा निरस्त")
}
```
In `Deal.kt`, replace the last constructor line `val syncStatus: Int = 0` with:
```kotlin
    val syncStatus: Int = 0,
    val farmerCommissionBps: Long = 0L, // commission % in basis points (150 = 1.5%)
    val revision: Int = 1,
    val isVoid: Boolean = false,
    val voidReason: VoidReason? = null
```
In `CashTransaction.kt`, replace `val syncStatus: Int = 0` with:
```kotlin
    val syncStatus: Int = 0,
    val revision: Int = 1,
    val isVoid: Boolean = false,
    val voidReason: VoidReason? = null
```

In `DealRepository.kt`, replace `suspend fun deleteDeal(dealId: String)` with:
```kotlin
    /** Marks the deal void with a reason; it stays in lists but leaves all balances. */
    suspend fun voidDeal(dealId: String, reason: VoidReason)
```
In `CashTransactionRepository.kt`, replace `suspend fun deleteTransaction(transactionId: String)` with:
```kotlin
    suspend fun voidTransaction(transactionId: String, reason: VoidReason)
```

- [ ] **Step 4: Implement `OfflineFirstDealRepository`.**
  - Constructor: add `private val clock: Clock = Clock.System` (import `kotlinx.datetime.Clock`).
  - Add `private val json = Json { encodeDefaults = true }` (import `kotlinx.serialization.json.Json`).
  - Replace `saveDeal`, `editDeal` and `deleteDeal` with:

```kotlin
    override suspend fun saveDeal(deal: Deal) = withContext(ioDispatcher) {
        database.transaction {
            val created = deal.copy(revision = 1, syncStatus = 0)
            insertOrReplace(created)
            recordRevision(created, "CREATE", created.updatedAt)
        }
    }

    override suspend fun editDeal(deal: Deal) = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transaction {
            val current = queries.getDealById(deal.id).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Deal ${deal.id} does not exist")
            require(!current.isVoid) { "Deal ${deal.id} is void and cannot be edited" }
            val edited = deal.copy(
                dealDate = current.dealDate,
                createdAt = current.createdAt,
                updatedAt = now,
                revision = current.revision + 1,
                isVoid = false,
                voidReason = null,
                syncStatus = 0
            )
            insertOrReplace(edited)
            recordRevision(edited, "EDIT", now)
        }
    }

    override suspend fun voidDeal(dealId: String, reason: VoidReason) = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transaction {
            val current = queries.getDealById(dealId).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Deal $dealId does not exist")
            if (current.isVoid) return@transaction
            val voided = current.copy(
                isVoid = true, voidReason = reason, revision = current.revision + 1,
                updatedAt = now, syncStatus = 0
            )
            insertOrReplace(voided)
            recordRevision(voided, "VOID", now)
        }
    }

    private fun recordRevision(deal: Deal, changeKind: String, at: Long) {
        queries.insertRevision(
            id = IdGenerator.newId(),
            shop_id = deal.shopId,
            entry_id = deal.id,
            entry_kind = "DEAL",
            revision = deal.revision.toLong(),
            change_kind = changeKind,
            snapshot_json = json.encodeToString(Deal.serializer(), deal),
            void_reason = deal.voidReason?.name,
            changed_at = at
        )
    }
```
  - In `insertOrReplace`, replace the Task 2 placeholder values with:
```kotlin
            farmer_commission_bps = deal.farmerCommissionBps,
            revision = deal.revision.toLong(),
            is_void = if (deal.isVoid) 1L else 0L,
            void_reason = deal.voidReason?.name,
```
  - In `toDomain()`, add:
```kotlin
            farmerCommissionBps = farmer_commission_bps,
            revision = revision.toInt(),
            isVoid = is_void == 1L,
            voidReason = void_reason?.let { VoidReason.valueOf(it) },
```

- [ ] **Step 5: Implement `OfflineFirstCashTransactionRepository` the same way.**
  - Constructor gains `private val clock: Clock = Clock.System`. Add the same `json` field.
  - `recordTransaction`: inside `database.transaction { }`, write the row with `revision = 1`, then `recordRevision(tx, "CREATE", tx.updatedAt)`.
  - Add this query to `AppDatabase.sq`, and delete the `softDeleteDeal:` and `softDeleteCashTransaction:` queries there:
```sql
getCashTransactionById:
SELECT * FROM cashTransactionEntity WHERE id = ?;
```
  - Replace `deleteTransaction` with `voidTransaction(transactionId, reason)`, mirroring `voidDeal`. Use `queries.getCashTransactionById(transactionId).executeAsOneOrNull()?.toDomain()`, copy with `isVoid = true, voidReason = reason, revision + 1, updatedAt = now, syncStatus = 0`, write it, and record a `"VOID"` revision with `entry_kind = "CASH"` and `CashTransaction.serializer()`.
  - Extract the `insertCashTransaction(...)` call into `private fun insertOrReplace(tx: CashTransaction)` (with the three new named args), so record and void share it.
  - `toDomain()` maps `revision`, `isVoid`, `voidReason` as for deals.

- [ ] **Step 6: Run the tests and confirm they pass.**
Run: `./gradlew :core-data:jvmTest`
Expected: PASS, including the existing `RepositoryTest`.

- [ ] **Step 7: Commit.**
```bash
git add core-domain core-data core-database
git commit -m "feat(ledger): append-only deal and cash entries with revision history and voids"
```

---

### Task 4: Deal entry uses exact math and a true edit path

Fixes bugs 1 (edit moved the deal to today) and 4 (rate lost paisa), the Double commission, and shows a negative payable honestly.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/deal/DealEntryViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/deal/DealEntryScreen.kt:375-381`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/slip/DigitalSlipRenderer.kt:20,25,49`
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/deal/DealEntryViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1 engine functions; Task 3 `Deal` fields, `DealRepository.editDeal`, `IdGenerator`.
- Produces: the `DealEntryViewModel` public API is unchanged.

- [ ] **Step 1: Write the failing tests.** Append inside `class DealEntryViewModelTest` (add imports `com.appwork.mandisamiti.domain.model.Deal`, `kotlinx.coroutines.flow.first`):

```kotlin
    private suspend fun seedShopAndParties(shopRepo: OfflineFirstShopProfileRepository, partyRepo: OfflineFirstPartyRepository) {
        shopRepo.saveShopProfile(
            ShopProfile(
                id = "shop-1", shopName = "श्री गणेश ट्रेडिंग", ownerName = "लाला जी", mandiName = "मथुरा मंडी",
                phoneNumber = "9837000000", pinHash = "1234", createdAt = 1000L, updatedAt = 1000L
            )
        )
        partyRepo.saveParty(Party(id = "farmer-1", shopId = "shop-1", name = "रामवीर सिंह", village = "राया", partyType = PartyType.FARMER, createdAt = 1000L, updatedAt = 1000L))
        partyRepo.saveParty(Party(id = "buyer-1", shopId = "shop-1", name = "अग्रवाल ट्रेडर्स", village = "मथुरा", partyType = PartyType.BUYER, createdAt = 1000L, updatedAt = 1000L))
    }

    private fun existingDeal() = Deal(
        id = "deal-old", shopId = "shop-1", farmerId = "farmer-1", buyerId = "buyer-1", commodityId = "comm_wheat",
        dealStatus = DealStatus.SETTLED, dealDate = 1_000L, bagsCount = 35,
        grossWeightGrams = 1_840_000L, cutWeightGrams = 35_000L, netWeightGrams = 1_805_000L,
        ratePaisaPerUnit = 227_550L, labourChargePaisa = 15_050L, farmerCommissionBps = 150L,
        createdAt = 1_000L, updatedAt = 1_000L
    )

    @Test
    fun editReloadKeepsPaisaInRateLabourAndCommission() = runTest {
        val database = createTestDatabase()
        val shopRepo = OfflineFirstShopProfileRepository(database)
        val partyRepo = OfflineFirstPartyRepository(database)
        val dealRepo = OfflineFirstDealRepository(database)
        seedShopAndParties(shopRepo, partyRepo)
        dealRepo.saveDeal(existingDeal())

        val viewModel = DealEntryViewModel(
            shopId = "shop-1", existingDealId = "deal-old", dealRepository = dealRepo,
            partyRepository = partyRepo, shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(), viewModelScope = backgroundScope
        )
        val state = viewModel.uiState.first { it.isEditMode }

        assertEquals("2275.50", state.ratePerQuintalText)
        assertEquals("150.50", state.labourChargesText)
        assertEquals("1.50", state.commissionPercentText)
        assertEquals("18.4", state.grossWeightText)
    }

    @Test
    fun savingAnEditKeepsTheOriginalDealDate() = runTest {
        val database = createTestDatabase()
        val shopRepo = OfflineFirstShopProfileRepository(database)
        val partyRepo = OfflineFirstPartyRepository(database)
        val dealRepo = OfflineFirstDealRepository(database)
        seedShopAndParties(shopRepo, partyRepo)
        dealRepo.saveDeal(existingDeal())

        val viewModel = DealEntryViewModel(
            shopId = "shop-1", existingDealId = "deal-old", dealRepository = dealRepo,
            partyRepository = partyRepo, shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(), viewModelScope = backgroundScope
        )
        viewModel.uiState.first { it.isEditMode && it.selectedFarmer != null }

        viewModel.events.test {
            viewModel.saveDeal()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val saved = dealRepo.getDealById("deal-old")!!
        assertEquals(1_000L, saved.dealDate)
        assertEquals(1_000L, saved.createdAt)
        assertEquals(2, saved.revision)
        assertEquals(227_550L, saved.ratePaisaPerUnit)
        assertEquals(1, dealRepo.getDealsByShopStream("shop-1").first().size) // edited, not duplicated
    }

    @Test
    fun newDealCommissionIsExactAndUsesUuid() = runTest {
        val database = createTestDatabase()
        val shopRepo = OfflineFirstShopProfileRepository(database)
        val partyRepo = OfflineFirstPartyRepository(database)
        val dealRepo = OfflineFirstDealRepository(database)
        seedShopAndParties(shopRepo, partyRepo)

        val viewModel = DealEntryViewModel(
            shopId = "shop-1", existingDealId = null, dealRepository = dealRepo,
            partyRepository = partyRepo, shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(), viewModelScope = backgroundScope
        )
        viewModel.uiState.first { it.selectedFarmer != null }

        viewModel.onFocusField(ActiveInputField.GROSS_WEIGHT)
        listOf(KeypadAction.DIGIT_1, KeypadAction.DIGIT_8, KeypadAction.DECIMAL, KeypadAction.DIGIT_4)
            .forEach(viewModel::onKeypadAction)
        viewModel.toggleSettlementStage(true)
        viewModel.onFocusField(ActiveInputField.RATE_PER_QUINTAL)
        listOf(KeypadAction.DIGIT_5, KeypadAction.DIGIT_4, KeypadAction.DIGIT_2, KeypadAction.DIGIT_0)
            .forEach(viewModel::onKeypadAction)

        // net 18.05 qtl x ₹5,420 = ₹97,831.00; 1.5% = ₹1,467.465 -> ₹1,467.47; labour ₹150
        val state = viewModel.uiState.value
        assertEquals(9_783_100L, state.grossAmountPaisa)
        assertEquals(9_783_100L - 146_747L - 15_000L, state.netFarmerPayablePaisa)

        viewModel.events.test {
            viewModel.saveDeal()
            val deal = (awaitItem() as DealEntryEvent.DealSavedSuccess).deal
            assertEquals(146_747L, deal.farmerCommissionPaisa)
            assertEquals(150L, deal.farmerCommissionBps)
            assertEquals(36, deal.id.length) // UUID, not "deal_<time>_<rand>"
            cancelAndIgnoreRemainingEvents()
        }
    }
```

- [ ] **Step 2: Run them and confirm they fail.**
Run: `./gradlew :composeApp:jvmTest --tests "*DealEntryViewModelTest*"`
Expected: FAIL. Rate reloads as `"2275"`, the edit `dealDate` equals now, and the commission is `146_746`.

- [ ] **Step 3: Implement in `DealEntryViewModel.kt`.**
  - Add the field `private var originalDeal: Deal? = null`.
  - In `loadExistingDeal`, set `originalDeal = deal` and replace the three text conversions:
```kotlin
                val grossQ = MandiMathEngine.gramsToQuintalsInputString(deal.grossWeightGrams)
                val tareQ = MandiMathEngine.gramsToQuintalsInputString(deal.cutWeightGrams)
                val rate = deal.ratePaisaPerUnit?.let { MandiMathEngine.paisaToInputString(it) } ?: ""
```
  Add these to the `copy(...)`:
```kotlin
                    labourChargesText = MandiMathEngine.paisaToInputString(deal.labourChargePaisa),
                    commissionPercentText = MandiMathEngine.paisaToInputString(deal.farmerCommissionBps),
```
  - In `appendKey`, change `RATE_PER_QUINTAL` and `LABOUR_CHARGES` to `allowDecimal = true`.
  - Add one settlement function that both preview and save use:
```kotlin
    private fun computeSettlement(state: DealEntryUiState): SettlementCalculation? {
        val grossGrams = MandiMathEngine.parseQuintalsStringToGrams(state.grossWeightText)
        val tareGrams = MandiMathEngine.parseQuintalsStringToGrams(state.tareWeightText)
        val netGrams = (grossGrams - tareGrams).coerceAtLeast(0L)
        val ratePaisa = MandiMathEngine.parseRupeesToPaisa(state.ratePerQuintalText)
        if (ratePaisa <= 0L || netGrams <= 0L) return null
        val commissionBps = MandiMathEngine.parsePercentToBasisPoints(state.commissionPercentText)
        return MandiMathEngine.calculateSettlement(
            grossWeightGrams = grossGrams,
            cutWeightGrams = tareGrams,
            ratePaisaPerQuintal = ratePaisa,
            deductions = DeductionsInput(
                farmerCommissionPaisa = MandiMathEngine.percentageOf(MandiMathEngine.grossAmountPaisa(netGrams, ratePaisa), commissionBps),
                labourChargePaisa = MandiMathEngine.parseRupeesToPaisa(state.labourChargesText)
            )
        )
    }
```
  (import `com.appwork.mandisamiti.domain.math.SettlementCalculation` and `com.appwork.mandisamiti.domain.id.IdGenerator`.)
  - In `recalculate()`, delete the `rateRs`/`ratePaisa`/`labourRs`/`labourPaisa`/`commPercent` lines and the `if (ratePaisa > 0L && netGrams > 0L) { ... }` block. Replace them with:
```kotlin
        val calc = computeSettlement(state)
        _uiState.value = _uiState.value.copy(
            netWeightQuintals = netQuintalsStr,
            grossAmountPaisa = calc?.grossAmountPaisa ?: 0L,
            netFarmerPayablePaisa = calc?.netFarmerPayablePaisa ?: 0L,
            netBuyerReceivablePaisa = calc?.netBuyerReceivablePaisa ?: 0L
        )
```
  - In `saveDeal()`, replace everything from `val isSettled = ...` through the `Deal(...)` construction with:
```kotlin
        val calc = computeSettlement(state)
        val isSettled = state.isSettledStage && calc != null
        val now = Clock.System.now().toEpochMilliseconds()
        val original = originalDeal

        val deal = Deal(
            id = original?.id ?: IdGenerator.newId(),
            shopId = state.shopId,
            farmerId = farmer.id,
            buyerId = state.selectedBuyer?.id,
            commodityId = state.selectedCommodity?.id ?: original?.commodityId ?: "comm_wheat",
            dealStatus = if (isSettled) DealStatus.SETTLED else DealStatus.PENDING_SETTLEMENT,
            dealDate = original?.dealDate ?: now,
            bagsCount = bags,
            grossWeightGrams = grossGrams,
            cutWeightGrams = tareGrams,
            netWeightGrams = netGrams,
            ratePaisaPerUnit = MandiMathEngine.parseRupeesToPaisa(state.ratePerQuintalText).takeIf { it > 0L },
            grossAmountPaisa = if (isSettled) calc!!.grossAmountPaisa else 0L,
            farmerCommissionPaisa = if (isSettled) calc!!.farmerCommissionPaisa else 0L,
            farmerCommissionBps = MandiMathEngine.parsePercentToBasisPoints(state.commissionPercentText),
            labourChargePaisa = MandiMathEngine.parseRupeesToPaisa(state.labourChargesText),
            netFarmerPayablePaisa = if (isSettled) calc!!.netFarmerPayablePaisa else 0L,
            netBuyerReceivablePaisa = if (isSettled) calc!!.netBuyerReceivablePaisa else 0L,
            receiptPhotoUri = state.receiptPhotoUri,
            createdAt = original?.createdAt ?: now,
            updatedAt = now,
            revision = original?.revision ?: 1
        )
```
  - Inside the `launch`, replace `dealRepository.saveDeal(deal)` with:
```kotlin
            if (original == null) dealRepository.saveDeal(deal) else dealRepository.editDeal(deal)
```
  The `"$farmerName, $bagsStr, ₹$amountRs ..."` voice text keeps using `state.netFarmerPayablePaisa`. When it is negative, make it say "किसान से ₹X लेना है" instead:
```kotlin
                val payable = state.netFarmerPayablePaisa
                val amountRs = MandiMathEngine.paisaToRupeesString(kotlin.math.abs(payable))
                if (payable < 0) "$farmerName, ${bagsStr}किसान से ₹$amountRs लेना है।"
                else "$farmerName, $bagsStr, ₹$amountRs पक्के हिसाब में दर्ज हुए।"
```

- [ ] **Step 4: Show a negative payable honestly.**
In `DealEntryScreen.kt`, replace the `"किसान को शुद्ध देय"` `InvoiceRow` with:
```kotlin
                                val farmerOwesShop = uiState.netFarmerPayablePaisa < 0
                                InvoiceRow(
                                    label = if (farmerOwesShop) "किसान से लेना है" else "किसान को शुद्ध देय",
                                    amountPaisa = kotlin.math.abs(uiState.netFarmerPayablePaisa),
                                    isBold = true,
                                    color = if (farmerOwesShop) MandiRedReceivable else MandiGreenPayable
                                )
```
(import `com.appwork.mandisamiti.ui.theme.MandiRedReceivable` if not already imported.)
In `DigitalSlipRenderer.kt`:
  - Line 20 becomes `val rateStr = deal.ratePaisaPerUnit?.let { "₹ ${MandiMathEngine.paisaToRupeesString(it)} / कुंतल" } ?: "बाजार भाव"`.
  - Line 25 becomes `val farmerPayableRs = MandiMathEngine.paisaToRupeesString(kotlin.math.abs(deal.netFarmerPayablePaisa))`.
  - Line 49 becomes:
```kotlin
            if (deal.netFarmerPayablePaisa < 0) appendLine("*किसान से लेना है: ₹ $farmerPayableRs*")
            else appendLine("🟢 *शुद्ध देय भुगतान (Net Payable): ₹ $farmerPayableRs*")
```

- [ ] **Step 5: Run all composeApp tests.**
Run: `./gradlew :composeApp:jvmTest`
Expected: PASS, including the original `testTwoStageDealCalculationAndKeypadEntry` and `MandiSamitiE2ETest`. If the E2E test hardcoded a truncated amount, update it to the half-up value and say so in the commit.

- [ ] **Step 6: Commit.**
```bash
git add composeApp
git commit -m "fix(deal): exact commission, paisa-safe edit reload, edits keep original date"
```

---

### Task 5: Galla shows today, with a carried-forward opening balance

Fixes bug 2: all-time totals shown as today, UPI and book entries counted as drawer cash, voids counted.

**Files:**
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/cash/CashDrawer.kt`
- Test: `core-domain/src/commonTest/kotlin/com/appwork/mandisamiti/domain/CashDrawerTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/register/DailyRegisterViewModel.kt`
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/register/DailyRegisterViewModelTest.kt`

**Interfaces:**
- Produces:
  - `data class DayWindow(val startMs: Long, val endMs: Long)`
  - `data class DrawerSummary(val openingPaisa: Long, val cashInPaisa: Long, val cashOutPaisa: Long)` with `val closingPaisa: Long`
  - `CashDrawer.dayWindow(now: Instant, timeZone: TimeZone): DayWindow`
  - `CashDrawer.summarize(transactions: List<CashTransaction>, window: DayWindow): DrawerSummary`
  - `DailyRegisterUiState.openingCashPaisa: Long` (new field). `todayTransactions` now holds only today's entries.
  - `DailyRegisterViewModel(..., clock: Clock = Clock.System, timeZone: TimeZone = TimeZone.currentSystemDefault())`, added after `viewModelScope`.

- [ ] **Step 1: Write the failing domain test** `CashDrawerTest.kt`:
```kotlin
package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.cash.CashDrawer
import com.appwork.mandisamiti.domain.cash.DayWindow
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class CashDrawerTest {
    private val ist = TimeZone.of("Asia/Kolkata")

    private fun tx(type: TransactionType, rupees: Long, at: Long, mode: PaymentMode = PaymentMode.CASH, void: Boolean = false) =
        CashTransaction(
            id = "tx-$at-$rupees", shopId = "shop-1", partyId = "p", transactionType = type,
            amountPaisa = rupees * 100, paymentMode = mode, transactionDate = at,
            createdAt = at, updatedAt = at, isVoid = void
        )

    @Test
    fun dayWindowFollowsIndianMidnightNotUtc() {
        // 00:10 IST on 8 Oct = 18:40 UTC on 7 Oct
        val window = CashDrawer.dayWindow(Instant.parse("2026-10-07T18:40:00Z"), ist)
        assertEquals(Instant.parse("2026-10-07T18:30:00Z").toEpochMilliseconds(), window.startMs)
        assertEquals(Instant.parse("2026-10-08T18:30:00Z").toEpochMilliseconds(), window.endMs)
    }

    @Test
    fun summaryCountsOnlyTodaysLiveCashAndCarriesYesterdayForward() {
        val window = DayWindow(startMs = 1_000, endMs = 2_000)
        val summary = CashDrawer.summarize(
            listOf(
                tx(TransactionType.JAMA_RECEIVED, 10_000, at = 500),                      // yesterday -> opening
                tx(TransactionType.UDHAR_GIVEN, 2_000, at = 600),                         // yesterday -> opening
                tx(TransactionType.JAMA_RECEIVED, 20_000, at = 1_100),                    // today in
                tx(TransactionType.UDHAR_GIVEN, 5_000, at = 1_200),                       // today out
                tx(TransactionType.JAMA_RECEIVED, 3_000, at = 1_300, mode = PaymentMode.UPI), // not drawer cash
                tx(TransactionType.JAMA_RECEIVED, 1_000, at = 1_400, void = true),        // voided
                tx(TransactionType.INTEREST_ADDED, 500, at = 1_500),                      // book entry, no cash moves
                tx(TransactionType.JAMA_RECEIVED, 9_999, at = 2_100)                      // tomorrow
            ),
            window
        )
        assertEquals(800_000L, summary.openingPaisa)
        assertEquals(2_000_000L, summary.cashInPaisa)
        assertEquals(500_000L, summary.cashOutPaisa)
        assertEquals(2_300_000L, summary.closingPaisa)
    }
}
```

- [ ] **Step 2: Run it and confirm it fails.**
Run: `./gradlew :core-domain:jvmTest --tests "*CashDrawerTest*"`
Expected: compilation FAIL (`Unresolved reference: CashDrawer`).

- [ ] **Step 3: Implement `CashDrawer.kt`.**
```kotlin
package com.appwork.mandisamiti.domain.cash

import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class DayWindow(val startMs: Long, val endMs: Long)

data class DrawerSummary(val openingPaisa: Long, val cashInPaisa: Long, val cashOutPaisa: Long) {
    val closingPaisa: Long get() = openingPaisa + cashInPaisa - cashOutPaisa
}

/** The physical galla: only live CASH entries move money in or out of the drawer. */
object CashDrawer {

    fun dayWindow(now: Instant, timeZone: TimeZone): DayWindow {
        val today = now.toLocalDateTime(timeZone).date
        return DayWindow(
            startMs = today.atStartOfDayIn(timeZone).toEpochMilliseconds(),
            endMs = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone).toEpochMilliseconds()
        )
    }

    fun summarize(transactions: List<CashTransaction>, window: DayWindow): DrawerSummary {
        var opening = 0L
        var cashIn = 0L
        var cashOut = 0L
        transactions
            .filter { it.paymentMode == PaymentMode.CASH && !it.isVoid && !it.isDeleted }
            .forEach { tx ->
                val signed = when (tx.transactionType) {
                    TransactionType.JAMA_RECEIVED -> tx.amountPaisa
                    TransactionType.UDHAR_GIVEN -> -tx.amountPaisa
                    TransactionType.INTEREST_ADDED, TransactionType.DISCOUNT_GIVEN -> 0L
                }
                when {
                    tx.transactionDate < window.startMs -> opening += signed
                    tx.transactionDate < window.endMs -> if (signed >= 0) cashIn += signed else cashOut -= signed
                }
            }
        return DrawerSummary(opening, cashIn, cashOut)
    }
}
```
Run: `./gradlew :core-domain:jvmTest --tests "*CashDrawerTest*"`. Expected: PASS.

- [ ] **Step 4: Write the failing ViewModel test.** Append inside `DailyRegisterViewModelTest` (imports: `kotlinx.datetime.Clock`, `kotlinx.datetime.Instant`, `kotlinx.datetime.TimeZone`, `com.appwork.mandisamiti.domain.model.CashTransaction`, `com.appwork.mandisamiti.domain.model.VoidReason`):
```kotlin
    @Test
    fun gallaShowsOnlyTodayWithOpeningCarriedForward() = runTest {
        val database = createTestDatabase()
        val shopRepo = OfflineFirstShopProfileRepository(database)
        val partyRepo = OfflineFirstPartyRepository(database)
        val cashRepo = OfflineFirstCashTransactionRepository(database)
        val now = Instant.parse("2026-10-08T05:30:00Z") // 11:00 IST
        val clock = object : Clock { override fun now() = now }
        val yesterday = Instant.parse("2026-10-07T10:00:00Z").toEpochMilliseconds()
        val today = Instant.parse("2026-10-08T04:00:00Z").toEpochMilliseconds()

        partyRepo.saveParty(Party(id = "farmer-1", shopId = "shop-1", name = "रामवीर सिंह", village = "राया", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L))
        fun cash(id: String, type: TransactionType, rupees: Long, at: Long) = CashTransaction(
            id = id, shopId = "shop-1", partyId = "farmer-1", transactionType = type,
            amountPaisa = rupees * 100, transactionDate = at, createdAt = at, updatedAt = at
        )
        cashRepo.recordTransaction(cash("y1", TransactionType.JAMA_RECEIVED, 10_000, yesterday))
        cashRepo.recordTransaction(cash("t1", TransactionType.JAMA_RECEIVED, 20_000, today))
        cashRepo.recordTransaction(cash("t2", TransactionType.UDHAR_GIVEN, 5_000, today))
        cashRepo.recordTransaction(cash("t3", TransactionType.JAMA_RECEIVED, 1_000, today))
        cashRepo.voidTransaction("t3", VoidReason.WRONG_ENTRY)

        val viewModel = DailyRegisterViewModel(
            shopId = "shop-1", cashRepository = cashRepo, partyRepository = partyRepo,
            shopProfileRepository = shopRepo, ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope, clock = clock, timeZone = TimeZone.of("Asia/Kolkata")
        )
        val state = viewModel.uiState.first { it.todayTransactions.isNotEmpty() }

        assertEquals(1_000_000L, state.openingCashPaisa)
        assertEquals(2_000_000L, state.todayCashInPaisa)
        assertEquals(500_000L, state.todayCashOutPaisa)
        assertEquals(2_500_000L, state.inHandCashDrawerPaisa)
        assertEquals(setOf("t1", "t2", "t3"), state.todayTransactions.map { it.transaction.id }.toSet()) // voided stays visible
    }
```

- [ ] **Step 5: Run it and confirm it fails.**
Run: `./gradlew :composeApp:jvmTest --tests "*DailyRegisterViewModelTest*"`
Expected: compilation FAIL (`No parameter with name 'clock'`).

- [ ] **Step 6: Implement in `DailyRegisterViewModel.kt`.**
  - Add the constructor params `private val clock: Clock = Clock.System` and `private val timeZone: TimeZone = TimeZone.currentSystemDefault()` after `viewModelScope`.
  - Add the field `val openingCashPaisa: Long = 0L` to `DailyRegisterUiState`.
  - Replace the whole `cashRepository.getTransactionsByShopStream(shopId).onEach { ... }` body with:
```kotlin
            .onEach { allTx ->
                val window = CashDrawer.dayWindow(clock.now(), timeZone)
                val summary = CashDrawer.summarize(allTx, window)
                val partyMap = _uiState.value.availableParties.associateBy { it.id }
                val todays = allTx
                    .filter { it.transactionDate >= window.startMs && it.transactionDate < window.endMs }
                    .map { tx ->
                        val party = partyMap[tx.partyId]
                        TransactionWithParty(transaction = tx, partyName = party?.name ?: "खाता", village = party?.village)
                    }
                _uiState.value = _uiState.value.copy(
                    openingCashPaisa = summary.openingPaisa,
                    todayCashInPaisa = summary.cashInPaisa,
                    todayCashOutPaisa = summary.cashOutPaisa,
                    inHandCashDrawerPaisa = summary.closingPaisa,
                    todayTransactions = todays.sortedByDescending { it.transaction.transactionDate }
                )
            }
```
  - In `recordDailyEntry`, use `clock.now().toEpochMilliseconds()` and `id = IdGenerator.newId()`.
  - In `DailyCashRegisterScreen.kt`, show the opening balance as the first row of the existing summary card. Label `"पिछला शेष (Opening)"`, value `uiState.openingCashPaisa` formatted with `MandiMathEngine.paisaToRupeesString`, styled like the existing cash-in row and using its text tokens. Voided rows in the list get the same muted + strikethrough treatment as Task 6, Step 4.

- [ ] **Step 7: Run the composeApp tests.**
Run: `./gradlew :composeApp:jvmTest`
Expected: PASS. The original `testDailyRegisterCashDrawerReconciliation` still passes because its entries are recorded "now", which is today.

- [ ] **Step 8: Commit.**
```bash
git add core-domain composeApp
git commit -m "fix(galla): today-only drawer with carried-forward opening, cash mode only, voids excluded"
```

---

### Task 6: Void an entry from the party khata

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/ledger/PartyLedgerViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/ledger/PartyLedgerScreen.kt`
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/ledger/PartyLedgerViewModelTest.kt`

**Interfaces:**
- Consumes: `DealRepository.voidDeal`, `CashTransactionRepository.voidTransaction`, `VoidReason`.
- Produces: `PartyLedgerViewModel.voidEntry(item: LedgerItem, reason: VoidReason)`.

- [ ] **Step 1: Write the failing test.** Append inside `PartyLedgerViewModelTest`. Add imports for `CashTransaction`, `TransactionType`, `VoidReason`, `Party`, `PartyType`, the four `OfflineFirst*Repository` classes, `createTestDatabase`, `SoundboxTtsManager`, `kotlinx.coroutines.flow.first` and `kotlin.test.assertTrue` if the file lacks them.
```kotlin
    @Test
    fun voidingACashEntryClearsItFromBalanceButKeepsItListed() = runTest {
        val database = createTestDatabase()
        val shopRepo = OfflineFirstShopProfileRepository(database)
        val partyRepo = OfflineFirstPartyRepository(database)
        val cashRepo = OfflineFirstCashTransactionRepository(database)
        val dealRepo = OfflineFirstDealRepository(database)
        partyRepo.saveParty(Party(id = "farmer-1", shopId = "shop-1", name = "रामवीर सिंह", village = "राया", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L))
        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-1", shopId = "shop-1", partyId = "farmer-1", transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 500_000L, transactionDate = 1L, createdAt = 1L, updatedAt = 1L
            )
        )
        val viewModel = PartyLedgerViewModel(
            shopId = "shop-1", partyId = "farmer-1", partyRepository = partyRepo, cashRepository = cashRepo,
            dealRepository = dealRepo, shopProfileRepository = shopRepo, ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope
        )
        val item = viewModel.uiState.first { it.ledgerItems.isNotEmpty() && it.balancePaisa == 500_000L }.ledgerItems.single()

        viewModel.voidEntry(item, VoidReason.WRONG_ENTRY)

        val after = viewModel.uiState.first { it.balancePaisa == 0L }
        val listed = after.ledgerItems.single() as LedgerItem.CashItem
        assertTrue(listed.transaction.isVoid)
    }
```

- [ ] **Step 2: Run it and confirm it fails.**
Run: `./gradlew :composeApp:jvmTest --tests "*PartyLedgerViewModelTest*"`
Expected: compilation FAIL (`Unresolved reference: voidEntry`).

- [ ] **Step 3: Implement `voidEntry` in `PartyLedgerViewModel.kt`.**
```kotlin
    fun voidEntry(item: LedgerItem, reason: VoidReason) {
        viewModelScope.launch {
            when (item) {
                is LedgerItem.DealItem -> dealRepository.voidDeal(item.deal.id, reason)
                is LedgerItem.CashItem -> cashRepository.voidTransaction(item.transaction.id, reason)
            }
            val speech = "प्रविष्टि रद्द की गई: ${reason.labelHi}"
            ttsManager.speak(speech, _uiState.value.isSoundEnabled)
            _events.emit(PartyLedgerEvent.TransactionRecorded(speech))
        }
    }
```
Also replace the two `"tx_${now}_..."` / `"tx_int_${now}_..."` ids with `IdGenerator.newId()`.
Run the test again. Expected: PASS.

- [ ] **Step 4: Screen: long-press to void, and a visibly voided card.** In `PartyLedgerScreen.kt`:
  - Add `var voidTarget by remember { mutableStateOf<LedgerItem?>(null) }` next to the other `remember` state.
  - Give `FintechDealCard` and `FintechCashCard` a new last parameter `onLongPress: () -> Unit`. In each card's outer `Box` modifier, replace nothing else. Add this after `.clip(shape)`:
```kotlin
            .combinedClickable(onClick = {}, onLongClick = { if (!isVoid) onLongPress() })
            .alpha(if (isVoid) 0.55f else 1f)
```
  `isVoid` is `deal.isVoid` / `transaction.isVoid`. Add `@OptIn(ExperimentalFoundationApi::class)` to both cards, plus imports `androidx.compose.foundation.ExperimentalFoundationApi`, `androidx.compose.foundation.combinedClickable`, `androidx.compose.ui.draw.alpha`.
  - In each card's amount `Text`, add `textDecoration = if (isVoid) TextDecoration.LineThrough else null` (import `androidx.compose.ui.text.style.TextDecoration`).
  - Under the amount in each card, when void, show the tag:
```kotlin
            if (isVoid) {
                Text(
                    text = "रद्द · ${voidReason?.labelHi ?: ""}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MandiTextMuted
                )
            }
```
  - In `FintechDealCard`, hide the share-slip button when `deal.isVoid`.
  - Pass `onLongPress = { voidTarget = item }` from the `items(uiState.ledgerItems)` block.
  - After the `Scaffold` content, add the reason dialog. Each choice is a full-width button ≥ 48dp, and it uses the existing button colours from the file:
```kotlin
    voidTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { voidTarget = null },
            title = { Text("प्रविष्टि रद्द करें?", fontWeight = FontWeight.Bold, color = MandiTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("रद्द प्रविष्टि खाते में दिखेगी, पर हिसाब से हट जाएगी।", color = MandiTextSecondary, fontSize = 14.sp)
                    VoidReason.entries.forEach { reason ->
                        OutlinedButton(
                            onClick = { viewModel.voidEntry(target, reason); voidTarget = null },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) { Text(reason.labelHi, color = MandiTextPrimary, fontSize = 16.sp) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { voidTarget = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("वापस", color = MandiTextSecondary)
                }
            }
        )
    }
```
  (Import `VoidReason`, `AlertDialog`, `OutlinedButton`, `TextButton`, `heightIn` as needed.)

- [ ] **Step 5: Run the tests.**
Run: `./gradlew :composeApp:jvmTest`
Expected: PASS.

- [ ] **Step 6: Commit.**
```bash
git add composeApp
git commit -m "feat(khata): void an entry with a reason; voided entries stay visible and leave the balance"
```

---

### Task 7: No demo data in the real app

Fixes bug 6.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/App.kt`

- [ ] **Step 1: Delete the demo seeding.** Remove the whole first `LaunchedEffect(Unit) { coroutineScope.launch { ... partyRepo.saveParty(...) x5 ... } }` block, plus any imports that are now unused.

- [ ] **Step 2: Remove the fake shop fallback** in the `Screen.ReceiptPreview` branch:
```kotlin
            is Screen.ReceiptPreview -> {
                MandiBackHandler { currentScreen = Screen.Home }
                val shopProfile by shopRepo.getShopProfileStream().collectAsState(initial = null)
                shopProfile?.let { profile ->
                    ReceiptPreviewScreen(
                        shopProfile = profile,
                        farmer = screen.farmer,
                        buyer = screen.buyer,
                        deal = screen.deal,
                        whatsAppShareManager = whatsAppShareManager,
                        ttsManager = ttsManager,
                        onNavigateBack = { currentScreen = Screen.Home }
                    )
                }
            }
```
(import `androidx.compose.runtime.collectAsState`.)

- [ ] **Step 3: Confirm no other fake data remains.**
Run: `grep -rn "श्री गणेश ट्रेडिंग\|9837000\|रामवीर" composeApp/src/commonMain`
Expected: no matches. Test files may keep them.

- [ ] **Step 4: Build and run the tests.**
Run: `./gradlew jvmTest`
Expected: PASS.

- [ ] **Step 5: Commit.**
```bash
git add composeApp
git commit -m "fix(app): stop seeding demo parties and faking a shop profile on real installs"
```

---

### Task 8: Prove it on the phone, then make the docs honest

**Files:**
- Modify: `docs/MANDISAMITI-MASTER-SPEC.md` (§2 feature matrix)
- Vault: `vaults/Vivek-K/wiki/concepts/mandisamiti-product-review-2026-10-08.md`, `wiki/log.md`, `wiki/hot.md`

- [ ] **Step 1: Run the full suite and the migration check.**
Run: `./gradlew jvmTest testDebugUnitTest :core-database:verifyCommonMainAppDatabaseMigration`
Expected: BUILD SUCCESSFUL. Record the test count from `build/test-results` in the commit message.

- [ ] **Step 2: Test the upgrade path on the Redmi Note 7.** First, install over the existing v1 app *without* uninstalling: `./gradlew :composeApp:installDebug`. The app must open, and the old entries must still show. This proves `1.sqm` migrated the real device database. Then uninstall and reinstall clean (`adb uninstall com.appwork.mandisamiti && ./gradlew :composeApp:installDebug`). After registering, the khata list must be empty, with no demo parties.

- [ ] **Step 3: Click-through on the phone** (light + dark theme, mirrored with scrcpy). Write the results down. Do not assume them.
  1. Add a farmer and a buyer. Settle a deal at ₹2,275.50/qtl. Reopen it: the rate shows `2275.50`, and saving keeps its original date.
  2. Settle a tiny deal where labour > crop value. The preview shows "किसान से लेना है" in red, the slip says the same, and the voice says "लेना है".
  3. Record ₹500 cash out, long-press it, void it with "गलत प्रविष्टि". It stays listed, struck through, with the "रद्द" tag, and the balance drops by ₹500.
  4. Galla: today's in/out match the entries made today, and the opening balance shows yesterday's carry-forward.
  5. At 390dp width, nothing scrolls sideways. Every new control is ≥ 48dp.

- [ ] **Step 4: Make the spec's feature matrix honest.** In `docs/MANDISAMITI-MASTER-SPEC.md` §2:
  - Set `Authentication & Sign Out` → `🟡 UI only (fake OTP, plaintext MPIN) — Plan 1C`.
  - Set `Language Switcher` → `🟡 UI only (not persisted, strings not wired)`.
  - Set `FastAPI + PostgreSQL Sync` → `⚪ Not connected — Plans 1B/1C`.
  - Set `Entry Deletion & Void Audit` → `✅ Void + revision history (1A)`.
  - Add a row: `Exact Money Math (half-up, signed payable, paisa rates)` → `✅ (1A)`.

- [ ] **Step 5: Update the vault.** In the review page, mark bugs 1–4, 6 and 7 as fixed, with commit hashes. Bug 5 (buyer charges) moves to Phase 2. Append a `log.md` entry and refresh `hot.md`. Run `python3 scripts/vault_lint.py` from the hub root. Expected: `0 errors`.

- [ ] **Step 6: Commit** (in the MandiSamiti repo: spec only; the vault is committed from the hub repo):
```bash
git add docs/MANDISAMITI-MASTER-SPEC.md
git commit -m "docs(spec): feature matrix reflects what actually works after 1A"
```
