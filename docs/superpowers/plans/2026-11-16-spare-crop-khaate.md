# Spare Crop (Khaate) Entries Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the trader record day-end spare crop as a khaate Buy or Sell: no party, no phone, no commission, no labour, tracked on its own khaate line per crop in the Godown, counted as Mohari (Procured) in day-end close-out.

**Architecture:** Reuse the existing Buy and Sell entries with a new `is_khaate` flag (SQLDelight migration 10) and one hidden system party per shop to satisfy the `farmer_id` / `buyer_id` foreign keys. Stock queries split totals by the flag, so `StockLine` carries a normal balance and a separate khaate balance. Khaate entries never create Galla cash rows and never touch a party khata.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, SQLDelight (`core-database`), `core-domain` commonMain/commonTest, FastAPI + Alembic backend (`backend/`).

**Spec:** `docs/superpowers/specs/2026-11-16-spare-crop-khaate-design.md`

## Global Constraints

- Khaate entries: commission = 0, labour = 0, tulak = 0, mandi shulk = 0. Amount (Asal) = weight x rate.
- No party, no phone number in the khaate form. Never shown in party lists, Khata, or party search.
- Spare buy = khaate stock in. Spare sell = khaate stock out. Khaate entries move only the khaate line; normal entries move only the normal line.
- Spare sell larger than the khaate line: warning only, save allowed, line may go negative. Normal Sell keeps blocking (`sell-validates-stock` unchanged).
- No Galla (cash) effect: `paidPaisa = 0`, `receivedPaisa = 0`, no `cashTransactionEntity` row.
- Close-out: khaate amount counts under Procured, label "मोहरी" (Hindi) / "Mohari (Procured)" (English). Never under Payable. Gross label is "असल" / "Asal".
- Crops: free-form commodity ids as today. New crop can be added from the form. No Settings screen.
- Edit and void use the existing revision trail (`entryRevisionEntity`) and soft void.
- Daily view only; no cross-day khaate running balance screen.
- MandiSamiti feature-velocity rule applies: domain tests are written in the task; UI tests follow once Vivek signs off on device.
- NGDL §5: tokens only, contrast ≥ 4.5:1 both themes, no emoji, no horizontal scroll at 390dp.
- Run tests with `./gradlew :core-domain:jvmTest :composeApp:jvmTest`. Do not use `:core-domain:allTests` (known iOS compile failure in `PilotDayMoneyInvariantTest.kt`). `HomeViewModelTest.testLendEntryReflectedInReceivablesAndHonestSubtitleWhenUnlinked` is known flaky; rerun it alone before treating it as a failure.

---

## File Structure

| File | Change | Responsibility |
|---|---|---|
| `core-domain/.../domain/model/Khaate.kt` | Create | System party id, `khaateAsalPaisa()` math |
| `core-domain/.../domain/model/BuyEntry.kt`, `SellEntry.kt` | Modify | `isKhaate: Boolean = false` |
| `core-database/.../database/10.sqm` | Create | `is_khaate` columns |
| `core-database/.../database/AppDatabase.sq` | Modify | Insert/select carry `is_khaate`; stock queries split; party lists exclude system party |
| `core-domain/.../domain/repository/ExpenseRepository.kt` | Modify | `StockTotals` gains khaate maps |
| `core-domain/.../domain/model/StockLine.kt` | Modify | Khaate grams and bags |
| `core-domain/.../domain/usecase/StockInventoryUseCase.kt` | Modify | Fill khaate fields |
| `core-domain/.../domain/model/StockAudit.kt` | Modify | Khaate physical count |
| `composeApp/.../ui/buy/BuyEntryFormState.kt`, `BuyEntryScreen.kt` | Modify | Khaate toggle |
| `composeApp/.../ui/sell/SellEntryFormState.kt`, `SellEntryScreen.kt` | Modify | Khaate toggle + warning |
| `composeApp/.../ui/stock/StockRegisterScreen.kt`, `StockBreakdownSheet.kt` | Modify | Khaate line, movement tag, two-count audit |
| `composeApp/.../ui/register/DayClosingSheet.kt`, `DailyRegisterViewModel.kt`, `core-domain/.../bahi/BahiDayPageBuilder.kt` | Modify | Mohari row |
| `composeApp/src/commonMain/composeResources/values/strings.xml`, `values-en/strings.xml` | Modify | New labels |
| `backend/migrations/versions/<new>.py`, `backend/app/...` sync models | Modify | Accept `is_khaate` |

`...` = `src/commonMain/kotlin/com/appwork/mandisamiti` (tests: `src/commonTest/...` or `src/jvmTest/...`).

---

### Task 1: Domain model and khaate math

**Files:**
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/Khaate.kt`
- Modify: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/BuyEntry.kt`, `SellEntry.kt`
- Test: `core-domain/src/commonTest/kotlin/com/appwork/mandisamiti/domain/KhaateTest.kt`

**Interfaces:**
- Produces: `object Khaate { fun partyId(shopId: String): String; const val PARTY_NAME: String; fun asalPaisa(totalWeightGrams: Long, ratePaisaPerQuintal: Long): Long }`, `BuyEntry.isKhaate`, `SellEntry.isKhaate`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.model.Khaate
import kotlin.test.Test
import kotlin.test.assertEquals

class KhaateTest {
    @Test fun partyIdIsStablePerShop() {
        assertEquals("khaate-shop1", Khaate.partyId("shop1"))
        assertEquals(Khaate.partyId("shop1"), Khaate.partyId("shop1"))
    }

    @Test fun asalIsWeightTimesRateOnly() {
        // 5 bags x 50 kg = 250 kg = 2.5 quintal at Rs 2,400/qtl = Rs 6,000
        assertEquals(600_000L, Khaate.asalPaisa(totalWeightGrams = 250_000L, ratePaisaPerQuintal = 240_000L))
    }

    @Test fun asalRoundsHalfUpToPaisa() {
        // 1 kg at Rs 2,401/qtl = Rs 24.01 -> 2401 paisa
        assertEquals(2_401L, Khaate.asalPaisa(1_000L, 240_100L))
    }
}
```

- [ ] **Step 2: Run, expect FAIL** (`Unresolved reference: Khaate`)

Run: `./gradlew :core-domain:jvmTest --tests "*KhaateTest*"`

- [ ] **Step 3: Implement**

```kotlin
package com.appwork.mandisamiti.domain.model

/** Day-end spare crop. No party, no commission, no labour, no Galla effect. */
object Khaate {
    const val PARTY_NAME = "खाते"
    fun partyId(shopId: String): String = "khaate-$shopId"

    /** grams / 100_000 = quintal. Half-up to the paisa. */
    fun asalPaisa(totalWeightGrams: Long, ratePaisaPerQuintal: Long): Long =
        (totalWeightGrams * ratePaisaPerQuintal + 50_000L) / 100_000L
}
```

Add `val isKhaate: Boolean = false,` after `syncStatus` in both `BuyEntry` and `SellEntry`.

Before relying on `asalPaisa`, grep for the existing gross formula (`grep -rn "grossPaisa =" core-domain/src/commonMain`) and, if one exists in `BuySellMath.kt`, have `asalPaisa` call it so rounding matches normal entries. Adjust the rounding test to that function's behaviour.

- [ ] **Step 4: Run, expect PASS**
- [ ] **Step 5: Commit** `feat(mandisamiti): khaate domain model and asal math`

---

### Task 2: Database migration, system party, stock split

**Files:**
- Create: `core-database/src/commonMain/sqldelight/com/appwork/mandisamiti/database/10.sqm`
- Modify: `core-database/.../AppDatabase.sq` (`buyEntryEntity` line ~153, `sellEntryEntity` ~190, `getAllParties` ~326, stock queries ~843-849, insert/upsert statements for both entries)
- Modify: repository implementation of `stockTotals` (find with `grep -rn "getStockBuyTotals" composeApp/src core-database/src --include="*.kt"`), entry mappers
- Modify: `core-domain/.../domain/repository/ExpenseRepository.kt`
- Test: existing database jvmTest location (find with `ls core-database/src/*Test`); add `KhaateStockQueriesTest.kt`

**Interfaces:**
- Consumes: `Khaate.partyId`, `isKhaate`
- Produces:
  ```kotlin
  data class StockTotals(
      val boughtGramsByCommodity: Map<String, Long>,
      val soldGramsByCommodity: Map<String, Long>,
      val khaateBoughtGramsByCommodity: Map<String, Long> = emptyMap(),
      val khaateSoldGramsByCommodity: Map<String, Long> = emptyMap()
  )
  ```
  and `suspend fun ensureKhaateParty(shopId: String, partyType: String)` on the party repository.

- [ ] **Step 1: Migration**

`10.sqm`:
```sql
ALTER TABLE buyEntryEntity ADD COLUMN is_khaate INTEGER NOT NULL DEFAULT 0;
ALTER TABLE sellEntryEntity ADD COLUMN is_khaate INTEGER NOT NULL DEFAULT 0;
```
Add `is_khaate INTEGER NOT NULL DEFAULT 0,` to both `CREATE TABLE` blocks in `AppDatabase.sq` so fresh installs match. Bump the schema version wherever `9` is configured (`grep -rn "version" core-database/build.gradle.kts`), and generate `10.db` with `./gradlew :core-database:generateCommonMainAppDatabaseSchema` if the project keeps schema snapshots in `sqldelight/databases/`.

- [ ] **Step 2: Split stock queries**

```sql
getStockBuyTotals:
SELECT commodity_id, is_khaate, COALESCE(SUM(total_weight_grams), 0) AS grams
FROM buyEntryEntity WHERE shop_id = ? AND is_deleted = 0 AND is_void = 0
GROUP BY commodity_id, is_khaate;

getStockSellTotals:
SELECT commodity_id, is_khaate, COALESCE(SUM(total_weight_grams), 0) AS grams
FROM sellEntryEntity WHERE shop_id = ? AND is_deleted = 0 AND is_void = 0
GROUP BY commodity_id, is_khaate;
```
In the repository, route rows with `is_khaate = 1` into the `khaate*` maps and the rest into the existing maps.

- [ ] **Step 3: Hide the system party**

Append `AND id NOT LIKE 'khaate-%'` to `getAllParties` and to every party search/list query (`grep -n "FROM partyEntity" AppDatabase.sq`). Party balance and Khata queries that join entries must add `AND is_khaate = 0`.

- [ ] **Step 4: System party upsert**

`ensureKhaateParty` inserts, if missing, a row with `id = Khaate.partyId(shopId)`, `name = Khaate.PARTY_NAME`, `phone = NULL`, `party_type = partyType`. Use `INSERT OR IGNORE`. Buy and Sell need different `party_type` values ('FARMER' / 'BUYER'); if a single row cannot serve both, use ids `khaate-<shop>` for buy and `khaate-<shop>-sell` for sell and update `Khaate.partyId` to take a `isBuy: Boolean` (update Task 1's test in the same commit).

- [ ] **Step 5: Write the failing test** (in-memory JDBC driver, same pattern as existing database tests)

```kotlin
@Test fun khaateRowsLandInKhaateMapsOnly() = runTest {
    insertBuy(commodity = "गेहूं", grams = 5_000_000L, isKhaate = false)
    insertBuy(commodity = "गेहूं", grams = 250_000L, isKhaate = true)
    insertSell(commodity = "गेहूं", grams = 400_000L, isKhaate = true)
    val t = repo.stockTotals(SHOP)
    assertEquals(5_000_000L, t.boughtGramsByCommodity["गेहूं"])
    assertEquals(null, t.soldGramsByCommodity["गेहूं"])
    assertEquals(250_000L, t.khaateBoughtGramsByCommodity["गेहूं"])
    assertEquals(400_000L, t.khaateSoldGramsByCommodity["गेहूं"])
}

@Test fun systemPartyNeverListed() = runTest {
    repo.ensureKhaateParty(SHOP, "FARMER")
    assertTrue(partyRepo.allParties(SHOP).none { it.id.startsWith("khaate-") })
}

@Test fun voidedKhaateRowLeavesTotals() = runTest {
    val id = insertBuy(commodity = "धान", grams = 100_000L, isKhaate = true)
    voidBuy(id)
    assertEquals(null, repo.stockTotals(SHOP).khaateBoughtGramsByCommodity["धान"])
}
```
`insertBuy`, `insertSell`, `voidBuy` are private helpers in the test that call the real repository save/void functions with a minimal valid entry.

- [ ] **Step 6: Run, expect FAIL; implement Steps 1-4; run, expect PASS**

Run: `./gradlew :core-database:jvmTest --tests "*KhaateStockQueriesTest*"` (use the module's actual test task name).

- [ ] **Step 7: Migration check** — install the current release build on the emulator, add one buy, then install this build and confirm the app opens and the buy is still there. Note the result in PROGRESS.md.
- [ ] **Step 8: Commit** `feat(mandisamiti): is_khaate column, system party, split stock totals`

---

### Task 3: StockLine carries a khaate line

**Files:**
- Modify: `core-domain/.../domain/model/StockLine.kt`, `core-domain/.../domain/usecase/StockInventoryUseCase.kt`
- Test: `core-domain/src/commonTest/kotlin/com/appwork/mandisamiti/domain/StockInventoryUseCaseTest.kt` (extend)

**Interfaces:**
- Consumes: `StockTotals.khaate*` maps
- Produces on `StockLine`: `khaateBoughtGrams: Long = 0L`, `khaateSoldGrams: Long = 0L`, `khaateAdjustmentGrams: Long = 0L`, `val khaateOnHandGrams: Long`, `val khaateOnHandBags: Long`, `val isKhaateNegative: Boolean`.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun khaateLineIsSeparateFromNormalBalance() = runTest {
    val repo = FakeStock(StockTotals(
        boughtGramsByCommodity = mapOf("गेहूं" to 5_000_000L),
        soldGramsByCommodity = emptyMap(),
        khaateBoughtGramsByCommodity = mapOf("गेहूं" to 250_000L),
        khaateSoldGramsByCommodity = emptyMap()
    ))
    val line = StockInventoryUseCase(repo).lines("s").single()
    assertEquals(100L, line.onHandBags)       // normal: 5,000 kg / 50
    assertEquals(5L, line.khaateOnHandBags)   // khaate: 250 kg / 50
}

@Test fun khaateLineMayGoNegative() = runTest {
    val repo = FakeStock(StockTotals(emptyMap(), emptyMap(),
        mapOf("गेहूं" to 250_000L), mapOf("गेहूं" to 400_000L)))
    val line = StockInventoryUseCase(repo).lines("s").single()
    assertEquals(-150_000L, line.khaateOnHandGrams)
    assertTrue(line.isKhaateNegative)
}

@Test fun khaateOnlyCropStillGetsALine() = runTest {
    val repo = FakeStock(StockTotals(emptyMap(), emptyMap(), mapOf("जिंस" to 100_000L), emptyMap()))
    assertEquals(listOf("जिंस"), StockInventoryUseCase(repo).lines("s").map { it.commodityId })
}
```

- [ ] **Step 2: Run, expect FAIL**
- [ ] **Step 3: Implement**

`StockLine` additions:
```kotlin
val khaateBoughtGrams: Long = 0L,
val khaateSoldGrams: Long = 0L,
val khaateAdjustmentGrams: Long = 0L,
```
```kotlin
val khaateOnHandGrams: Long get() = khaateBoughtGrams - khaateSoldGrams + khaateAdjustmentGrams
val khaateOnHandBags: Long get() = if (standardBagKg > 0) (khaateOnHandGrams / (standardBagKg * 1000.0)).toLong() else 0L
val isKhaateNegative: Boolean get() = khaateOnHandGrams < 0L
```
Note: `khaateOnHandBags` truncates toward zero, so -150 kg shows as -3 bags at 50 kg. That is intended.

In `StockInventoryUseCase.lines`, add the khaate map keys to `allRawKeys`, and sum `khaateBought`/`khaateSold` per base commodity the same way `b`/`s` are summed, passing them into `StockLine`.

- [ ] **Step 4: Run, expect PASS** (also run `DailyStockCarryForwardAndNegativeWarningTest` to confirm normal behaviour is unchanged)
- [ ] **Step 5: Commit** `feat(mandisamiti): separate khaate stock line per crop`

---

### Task 4: Two-count stock audit

**Files:**
- Modify: `core-domain/.../domain/model/StockAudit.kt`, `StockInventoryUseCase.kt`, audit save path (`grep -rn "onSaveStockAudit" composeApp/src/commonMain`), `StockAuditRepository` implementation
- Test: `StockInventoryUseCaseTest.kt` (extend)

**Interfaces:**
- Produces on `StockAudit`: `khaatePhysicalBags: Long = 0L`, `khaateDiffGrams: Long = 0L` (both default, so old serialized audits still decode). `onSaveStockAudit` gains `khaatePhysicalBags: Long`.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun auditAdjustsEachLineSeparately() = runTest {
    val totals = StockTotals(mapOf("गेहूं" to 5_000_000L), emptyMap(), mapOf("गेहूं" to 250_000L), emptyMap())
    val audit = StockAudit(
        commodityId = "गेहूं", physicalBags = 98, bagWeightKg = 50.0,
        physicalGrams = 4_900_000L, diffGrams = -100_000L, reason = "count",
        auditedAt = 1L, khaatePhysicalBags = 6, khaateDiffGrams = 50_000L
    )
    val line = StockInventoryUseCase(FakeStock(totals), FakeAudits(mapOf("गेहूं" to audit))).lines("s").single()
    assertEquals(98L, line.onHandBags)
    assertEquals(6L, line.khaateOnHandBags)
}
```
(`FakeAudits` implements `StockAuditRepository` returning the given map; add it next to `FakeStock` if not present.)

- [ ] **Step 2: Run, expect FAIL**
- [ ] **Step 3: Implement** — in `lines`, set `khaateAdjustmentGrams = audit?.khaateDiffGrams ?: 0L`. Where the audit is saved, compute `khaateDiffGrams = khaatePhysicalBags * bagKg * 1000 - khaateBookGrams` using the same formula the code already uses for `diffGrams`.
- [ ] **Step 4: Run, expect PASS**
- [ ] **Step 5: Commit** `feat(mandisamiti): stock audit counts khaate bags separately`

---

### Task 5: Khaate toggle on Buy and Sell forms

**Files:**
- Modify: `composeApp/.../ui/buy/BuyEntryFormState.kt`, `BuyEntryScreen.kt`, `BuyEntryUiMapper.kt`, `BuyEntryActionsAdapter.kt`
- Modify: `composeApp/.../ui/sell/SellEntryFormState.kt`, `SellEntryScreen.kt`
- Modify: `strings.xml`, `values-en/strings.xml`
- Test: `composeApp/src/jvmTest/.../ui/buy/BuyKhaateFormTest.kt`, `.../ui/sell/SellKhaateFormTest.kt`

**Interfaces:**
- Consumes: `Khaate`, `isKhaate`, `ensureKhaateParty`, `StockLine.khaateOnHandGrams`
- Produces: `BuyEntryFormState.isKhaate`, `SellEntryFormState.isKhaate`, `SellEntryFormState.khaateShortWarning: Boolean`

Strings (Hindi first):
```xml
<string name="khaate_toggle">खाते (बचा माल)</string>
<string name="khaate_short_warning">खाते में इतना माल नहीं है। फिर भी सेव होगा।</string>
```
English:
```xml
<string name="khaate_toggle">Khaate (spare crop)</string>
<string name="khaate_short_warning">Khaate stock is lower than this sale. It will still be saved.</string>
```

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun khaateBuyHasNoChargesAndNoParty() {
    val s = BuyEntryFormState().copy(isKhaate = true, bags = "5", weightPerBagKg = "50", rate = "2400")
    val e = s.toEntry(shopId = "s", now = 1L)
    assertTrue(e.isKhaate)
    assertEquals(Khaate.partyId("s"), e.farmerId)
    assertEquals(0L, e.commissionPaisa); assertEquals(0L, e.labourPaisa)
    assertEquals(600_000L, e.grossPaisa); assertEquals(600_000L, e.netPayablePaisa)
    assertEquals(0L, e.paidPaisa)
}

@Test fun khaateSellOverStockWarnsButIsValid() {
    val s = SellEntryFormState().copy(isKhaate = true, bags = "8", weightPerBagKg = "50", rate = "2400",
        khaateOnHandGrams = 250_000L)
    assertTrue(s.khaateShortWarning)
    assertTrue(s.canSave)
}

@Test fun normalSellStillBlocksOnShortStock() {
    val s = SellEntryFormState().copy(isKhaate = false, bags = "8", weightPerBagKg = "50", rate = "2400",
        normalOnHandGrams = 250_000L)
    assertFalse(s.canSave)
}
```
Field names (`bags`, `weightPerBagKg`, `rate`, `toEntry`, `canSave`, `normalOnHandGrams`) must match the real form state; read `BuyEntryFormState.kt` and `SellEntryFormState.kt` first and rename in the test to the existing names. Keep the assertions.

- [ ] **Step 2: Run, expect FAIL**

Run: `./gradlew :composeApp:jvmTest --tests "*KhaateFormTest*"`

- [ ] **Step 3: Implement**
  - Form state: `isKhaate: Boolean = false`. When true: party id = `Khaate.partyId(shopId)`, commission/arat/tulak/mandi shulk/labour = 0, paid/received = 0, gross = `Khaate.asalPaisa(...)`.
  - Sell validation: if `isKhaate`, skip the existing stock block and set `khaateShortWarning = totalWeightGrams > khaateOnHandGrams`. Normal path unchanged.
  - Save path: call `ensureKhaateParty` before insert. Do not create a `cashTransactionEntity` row when `isKhaate` (check `BuyEntryActionsAdapter` and the Sell save path for the paid/received cash insert and guard it).
  - Screen: an `FilterChip`-style toggle at the top labelled `khaate_toggle`. When on, hide party picker, phone, commission, labour and payment fields. Crop picker stays and keeps its existing "add new crop" behaviour. Show `khaate_short_warning` in an NGDL warning banner (tokens only).
- [ ] **Step 4: Run, expect PASS**; also run the full suite.
- [ ] **Step 5: Commit** `feat(mandisamiti): khaate toggle on Buy and Sell forms`

---

### Task 6: Godown shows the khaate line and movement tag

**Files:**
- Modify: `composeApp/.../ui/stock/StockRegisterScreen.kt` (`GodownCommodityLedgerCard` ~588, movement list ~416, audit sheet call ~431), `StockBreakdownSheet.kt`
- Modify: the movement bill model (`grep -n "isBuy" StockRegisterScreen.kt` to find its type) to carry `isKhaate`
- Modify: strings

Strings: `khaate_line` = "खाते" / "Khaate"; `khaate_tag` = "खाते" / "Khaate"; `khaate_count_label` = "खाते बोरी गिनती" / "Khaate bags counted".

- [ ] **Step 1: Implement**
  - In each commodity card, below the normal In/Out/Balance row, add a second row "खाते: N बोरी" when `khaateBoughtGrams != 0 || khaateSoldGrams != 0`. Negative value uses the existing negative-warning colour token.
  - Movement list: khaate bills get a `khaate_tag` chip (vector, tokens only, no emoji).
  - Audit sheet: second number field `khaate_count_label`, prefilled with `khaateOnHandBags`, passed to `onSaveStockAudit`.
- [ ] **Step 2: Device check** on emulator: light, dark, 390dp; card with and without khaate; negative khaate; audit with both counts. Screenshots to `docs/testing/screenshots/khaate/`.
- [ ] **Step 3: Commit** `feat(mandisamiti): khaate line and tag in Godown`

---

### Task 7: Mohari in day-end close-out

**Files:**
- Modify: `core-domain/.../domain/bahi/BahiDayPageBuilder.kt`, `composeApp/.../ui/register/DailyRegisterViewModel.kt`, `DayClosingSheet.kt` (Details rows ~213)
- Test: `core-domain/src/commonTest/.../BahiDayMohariTest.kt`

**Interfaces:**
- Produces on the day page / close-out state: `khaateBuyAsalPaisa: Long` and `khaateSellAsalPaisa: Long`, each the sum of `grossPaisa` of the day's non-void khaate buys or sells.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun khaateGoesToMohariNotPayableAndNotCash() {
    val page = buildDayPage(
        buys = listOf(buy(gross = 600_000L, isKhaate = true), buy(gross = 1_000_000L, isKhaate = false, paid = 0L)),
        sells = listOf(sell(gross = 300_000L, isKhaate = true))
    )
    assertEquals(600_000L, page.khaateBuyAsalPaisa)
    assertEquals(300_000L, page.khaateSellAsalPaisa)
    assertEquals(1_000_000L - 0L, page.farmerPayablePaisa) // only the normal buy
    assertEquals(0L, page.cashInPaisa); assertEquals(0L, page.cashOutPaisa)
}
```
Read `BahiDayPageBuilder.kt` first and use its real builder name and field names in place of `buildDayPage`, `farmerPayablePaisa`, `cashInPaisa`, `cashOutPaisa`, `buy(...)`, `sell(...)`.

- [ ] **Step 2: Run, expect FAIL**
- [ ] **Step 3: Implement** — exclude `isKhaate` entries from payable/receivable sums; add the two khaate sums. In `DayClosingSheet` Details, add rows "मोहरी खरीद (असल)" / "Mohari buy (Asal)" and "मोहरी बिक्री (असल)" / "Mohari sell (Asal)". Expected cash stays unchanged.
- [ ] **Step 4: Run, expect PASS**
- [ ] **Step 5: Commit** `feat(mandisamiti): mohari rows in day-end close-out`

---

### Task 8: Edit and void for khaate entries

**Files:**
- Modify: Buy and Sell edit paths (`grep -rn "entryRevisionEntity\|insertRevision" composeApp/src core-database/src`), list screens `BuyTransactionsListScreen.kt`, `SellTransactionsListScreen.kt`, `ParchiListScreen.kt`
- Test: `composeApp/src/jvmTest/.../KhaateEditVoidTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun editingKhaateBuyWritesRevisionAndMovesKhaateLine() = runTest {
    val id = saveKhaateBuy(grams = 250_000L)
    editBuy(id) { it.copy(totalWeightGrams = 300_000L) }
    assertEquals(2L, loadBuy(id).revision)
    assertEquals(1, revisionsFor(id).size)
    assertEquals(300_000L, stockTotals().khaateBoughtGramsByCommodity["गेहूं"])
}

@Test fun voidingKhaateSellRestoresKhaateLine() = runTest {
    saveKhaateBuy(grams = 250_000L)
    val sellId = saveKhaateSell(grams = 100_000L)
    voidSell(sellId, reason = "galat entry")
    assertEquals(null, stockTotals().khaateSoldGramsByCommodity["गेहूं"])
}

@Test fun editKeepsKhaateFlagAndZeroCharges() = runTest {
    val id = saveKhaateBuy(grams = 250_000L)
    editBuy(id) { it.copy(ratePaisaPerQuintal = 250_000L) }
    val e = loadBuy(id)
    assertTrue(e.isKhaate); assertEquals(0L, e.commissionPaisa); assertEquals(0L, e.labourPaisa)
}
```
Helpers wrap the real ViewModel or repository functions used by the existing edit/void tests; copy their setup.

- [ ] **Step 2: Run, expect FAIL**
- [ ] **Step 3: Implement** — make sure the edit form reopens with `isKhaate = true` (mapper reads the flag), the revision snapshot includes `is_khaate`, and list screens show the `khaate_tag` chip and "खाते" in place of a party name.
- [ ] **Step 4: Run, expect PASS**
- [ ] **Step 5: Commit** `feat(mandisamiti): edit and void for khaate entries`

---

### Task 9: Backend sync accepts is_khaate

**Files:**
- Create: `backend/migrations/versions/<rev>_add_is_khaate.py`
- Modify: backend models and sync schemas for buy and sell entries (`grep -rn "is_void" backend/app`)
- Modify: client sync payload mapper (`grep -rn "getPendingSyncBuyEntries" composeApp/src --include="*.kt"`)
- Test: backend pytest for sync round trip (next to existing sync tests)

- [ ] **Step 1: Write the failing test**

```python
def test_sync_round_trips_is_khaate(client, shop_token):
    payload = make_buy_payload(is_khaate=True, farmer_id="khaate-shop1")
    r = client.post("/sync/push", json={"buy_entries": [payload]}, headers=shop_token)
    assert r.status_code == 200
    pulled = client.get("/sync/pull", headers=shop_token).json()
    assert pulled["buy_entries"][0]["is_khaate"] is True
```
Match the real endpoint paths and fixtures in `backend/tests/`.

- [ ] **Step 2: Run, expect FAIL** (`cd backend && pytest -k is_khaate`)
- [ ] **Step 3: Implement** — Alembic migration adds `is_khaate BOOLEAN NOT NULL DEFAULT false` to both entry tables; schemas default it to `False` so old clients still sync; client mapper sends and reads the field. The system party syncs as a normal party row.
- [ ] **Step 4: Run, expect PASS**
- [ ] **Step 5: Commit** `feat(backend): sync is_khaate on buy and sell entries`

Deploying the backend migration is a separate `/project-deploy` step and needs Vivek's confirmation.

---

### Task 10: Tracker, docs, device sign-off

**Files:**
- Modify: `FEATURES.json`, `PROGRESS.md`, spec status line, `vaults/Vivek-K/wiki/log.md`

- [ ] **Step 1:** Append to `FEATURES.json`:
```json
{"id": "spare-crop-khaate", "description": "Day-end spare crop as khaate Buy/Sell: no party, no charges, own Godown line, Mohari in close-out", "status": "failing", "evidence": "Domain and UI built; device check by Vivek pending"}
```
- [ ] **Step 2:** `python3 scripts/check_tracker.py` → expect `tracker ok (30 features)`.
- [ ] **Step 3:** PROGRESS.md session entry with commits and test counts. Set spec status to "Built, device check pending".
- [ ] **Step 4:** Ask Vivek to check on device: khaate buy 5 bags Gehu, khaate sell 8 bags (warning shows, saves, line -3), Godown two lines, audit two counts, close-out Mohari rows, Galla cash unchanged, edit then void. Flip to `passing` only after his report.
- [ ] **Step 5: Commit** `docs(mandisamiti): track spare-crop-khaate`

---

## Self-review notes

- Spec coverage: no party/phone (T5), zero charges (T1, T5), Stock not Galla (T2, T5, T7), separate khaate line (T3), two-count audit (T4), warning not block (T5), Mohari/Asal in close-out (T7), crops free-form (T5 reuses existing picker), edit/void (T8), daily view (T6 movement list). Sync (T9) added because entries sync to the VPS.
- Risk: the `party_type` FK question in Task 2 Step 4 may change `Khaate.partyId`'s signature; that task owns the Task 1 test update.
- Risk: Khata and party balance queries that do not filter `is_khaate` would show the system party's totals. Task 2 Step 3 covers the known queries; Task 8 list checks catch stragglers.
