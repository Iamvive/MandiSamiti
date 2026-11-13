# Per-Loan Interest Rate (2% default) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every money loan stores its own monthly interest rate (default 2%), the old 1.5 default is removed everywhere, and existing loans, shops and parties move to 2%.

**Architecture:** Add `monthlyRatePercent` to `LendEntry` (SQLite column, domain model, sync DTO, backend column). Interest math keeps using `RuralInterestEngine.interestDue`, but callers pass the loan's own rate instead of the party or shop rate. One constant, `RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT = 2.0`, replaces every hardcoded 1.5. A SQLDelight migration (`11.sqm`) and an Alembic migration (`0005`) rewrite stored 1.5 values to 2.0.

**Tech Stack:** Kotlin Multiplatform, SQLDelight, Compose Multiplatform, FastAPI, SQLAlchemy, Alembic.

## Global Constraints

- Money is `Long` paisa. The rate is a `Double` percent per month (2.0 means 2%). No float money math.
- Interest: simple, no compounding, pro-rata by day (days / 30), a payment clears interest first, then principal. `RuralInterestEngine` math is NOT changed.
- Default rate for every new loan is 2.0. A user may type another rate (for example 1.5) on a single loan. No code may hardcode 1.5 as a loan or shop default.
- Existing loans, shop profiles and parties that hold 1.5 become 2.0 by migration.
- Party-level `monthly_interest_rate` stops being read for loans. The column and backend field stay in place, unused.
- Rate field is shown for MONEY loans only (grain loans carry no interest).
- NGDL tokens only, vector icons, no emoji, no horizontal scroll at 390dp, Light and Dark.
- MandiSamiti rule: build the feature first, write tests when the flow is final (Task 6). Test on Redmi `d93c743` against staging `http://64.227.142.180:8060` (never 10.0.2.2).
- Offline-first: the local write commits first, sync follows.
- `hot.md` and the parent submodule pointer are updated only after Vivek approves the finished feature.
- No API keys or secrets in any file, log or commit.

## File Map

- Modify `core-domain/.../domain/math/RuralInterestEngine.kt`: add the constant, fix the KDoc.
- Modify `core-domain/.../domain/model/LendEntry.kt`, `ShopProfile.kt`: new field, default 2.0.
- Modify `core-domain/.../domain/usecase/LendTransactionUseCase.kt`: carry the rate from the form.
- Modify `core-database/.../database/AppDatabase.sq`; create `core-database/.../database/11.sqm`.
- Modify `core-data/.../repository/OfflineFirstLendRepository.kt`, `sync/SyncEngine.kt`, `sync/model/SyncDto.kt`.
- Modify `composeApp/.../App.kt`, `ui/lend/LendTransactionsListScreen.kt`, `ui/lend/LendEntryFormState.kt`, `ui/lend/LendEntryScreen.kt`, `ui/lend/LoanAdvanceEntryScreen.kt`, `ui/ledger/PartyLedgerViewModel.kt`, `ui/ledger/PartyLedgerScreen.kt`, `jvmMain/.../DemoSimulator.kt`.
- Modify `backend/app/models/lend_entry.py`, `user.py`, `party.py`; `backend/app/schemas/entries.py`, `party.py`; `backend/app/api/v1/endpoints/sync.py`; create `backend/migrations/versions/0005_loan_interest_rate.py`.
- Tests: listed in Task 6.

All paths below are relative to `/Users/appworx/Desktop/Vivek-K/projects/MandiSamiti`, and `...` stands for `src/commonMain/kotlin/com/appwork/mandisamiti` in that module.

---

### Task 1: Domain constant, model fields, use case

**Files:**
- Modify: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/math/RuralInterestEngine.kt`
- Modify: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/LendEntry.kt`
- Modify: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/ShopProfile.kt:14`
- Modify: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/usecase/LendTransactionUseCase.kt:9-13,31-39`

**Interfaces:**
- Produces: `RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT: Double = 2.0`; `LendEntry.monthlyRatePercent: Double = RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT`; `LendForm.monthlyRatePercent: Double = RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT`.

- [ ] **Step 1: Add the constant and fix the KDoc**

In `RuralInterestEngine.kt`, next to `const val DAYS_IN_MONTH = 30.0`:

```kotlin
    /** Rate every new loan starts with, in percent per month. A single loan may be given another rate. */
    const val DEFAULT_MONTHLY_RATE_PERCENT = 2.0
```

Change the KDoc line `(e.g. ₹1.50 per ₹100 per month)` to `(e.g. ₹2 per ₹100 per month)` and the comment `(1.5% -> 150)` to `(2% -> 200)`.

- [ ] **Step 2: Add the field to LendEntry**

In `LendEntry.kt` add the import `import com.appwork.mandisamiti.domain.math.RuralInterestEngine` and, after `voidReason`:

```kotlin
    val monthlyRatePercent: Double = RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT,
```

Put it before `syncStatus` so named-argument call sites keep working.

- [ ] **Step 3: Change the shop default**

`ShopProfile.kt:14`: `val defaultMonthlyInterestRate: Double = RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT,` (add the import).

- [ ] **Step 4: Carry the rate through the use case**

In `LendForm` add `val monthlyRatePercent: Double = RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT,` as the last parameter. In `LendTransactionUseCase.lend`, add `monthlyRatePercent = form.monthlyRatePercent,` to the `LendEntry(...)` call. Add above `val now = nowMillis()`:

```kotlin
        require(form.monthlyRatePercent > 0.0) { "Interest rate must be above zero" }
```

- [ ] **Step 5: Compile**

Run: `./gradlew :core-domain:compileKotlinJvm -q`
Expected: success.

- [ ] **Step 6: Commit**

```bash
git add core-domain
git commit -m "feat(lend): per-loan monthly rate in domain, default 2%"
```

---

### Task 2: Database column, migration, repository mapping

**Files:**
- Modify: `core-database/src/commonMain/sqldelight/com/appwork/mandisamiti/database/AppDatabase.sq` (line 10, the `lendEntryEntity` table around 226-248, `insertLendEntry` ~695, `insertLendEntryIfAbsent` ~770)
- Create: `core-database/src/commonMain/sqldelight/com/appwork/mandisamiti/database/11.sqm`
- Modify: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/repository/OfflineFirstLendRepository.kt` (`insertOrReplace` ~385, `toDomain` ~436)
- Modify: `core-database/src/jvmTest/kotlin/com/appwork/mandisamiti/database/V1Schema.kt:17`, `DatabaseTest.kt:31,50` (test fixtures that say 1.5)

**Interfaces:**
- Consumes: `LendEntry.monthlyRatePercent` from Task 1.
- Produces: column `lendEntryEntity.monthly_interest_rate REAL NOT NULL DEFAULT 2.0`; both insert queries take it as the last parameter, after `void_reason`.

- [ ] **Step 1: Fresh-install schema**

In `AppDatabase.sq`: line 10 becomes `default_monthly_interest_rate REAL NOT NULL DEFAULT 2.0,`. In `lendEntryEntity`, add after `void_reason TEXT,`:

```sql
    monthly_interest_rate REAL NOT NULL DEFAULT 2.0,
```

In `insertLendEntry` and `insertLendEntryIfAbsent`, add `monthly_interest_rate` to the column list after `void_reason` and one more `?` to VALUES (21 columns, 21 placeholders).

- [ ] **Step 2: Upgrade migration**

Create `11.sqm`:

```sql
ALTER TABLE lendEntryEntity ADD COLUMN monthly_interest_rate REAL NOT NULL DEFAULT 2.0;
UPDATE lendEntryEntity SET monthly_interest_rate = 2.0;
UPDATE shopProfileEntity SET default_monthly_interest_rate = 2.0 WHERE default_monthly_interest_rate = 1.5;
UPDATE partyEntity SET monthly_interest_rate = 2.0 WHERE monthly_interest_rate = 1.5;
```

The second line sets every existing loan to 2.0 (decided with Vivek: no 1.5 survives on existing loans). The ALTER default already gives 2.0, the UPDATE makes the intent explicit and safe on re-run. Sync flags and revisions are not touched: the backend migration (Task 5) applies the same rewrite on the server, so both sides agree without a push.

- [ ] **Step 3: Repository mapping**

In `insertOrReplace`, after `void_reason = entry.voidReason,` add `monthly_interest_rate = entry.monthlyRatePercent,`. In `toDomain`, after `voidReason = void_reason,` add `monthlyRatePercent = monthly_interest_rate,`. Grep `queries.insertLendEntry` and `insertLendEntryIfAbsent` in `core-data` and add the new argument at every call (SyncEngine pull path, around line 193).

- [ ] **Step 4: Test fixtures**

`V1Schema.kt:17`, `DatabaseTest.kt:31` and `:50`: change `1.5` to `2.0`.

- [ ] **Step 5: Compile and run the database tests**

Run: `./gradlew :core-database:jvmTest :core-data:compileKotlinJvm -q`
Expected: success. If SQLDelight complains about argument counts, count the columns again.

- [ ] **Step 6: Commit**

```bash
git add core-database core-data
git commit -m "feat(lend): monthly_interest_rate column, migration 11 rewrites 1.5 to 2"
```

---

### Task 3: Use the loan's own rate everywhere

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/App.kt:485-486, 695-696, 945-948`
- Modify: `composeApp/.../ui/lend/LendTransactionsListScreen.kt:85, 215, 534, 543`
- Modify: `composeApp/.../ui/ledger/PartyLedgerViewModel.kt:86, 135, 164, 237-238, 427`
- Modify: `composeApp/.../ui/ledger/PartyLedgerScreen.kt:147, 341, 1291`
- Modify: `composeApp/src/jvmMain/kotlin/com/appwork/mandisamiti/DemoSimulator.kt:67`

**Interfaces:**
- Consumes: `LendEntry.monthlyRatePercent`, `RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT`.
- Produces: no function takes a party or shop rate for loan interest any more. `getMonthlyRatePercent` is deleted.

- [ ] **Step 1: App.kt, two interest-due blocks**

At lines 485-486 and 695-696 replace the two-line `val rate = ... ?: 1.5` with:

```kotlin
                        val rate = lend.monthlyRatePercent
```

The `interestDue(lend.amountPaisa, rate, ...)` lines below stay.

- [ ] **Step 2: Remove the `getMonthlyRatePercent` parameter**

In `App.kt` delete the `getMonthlyRatePercent = { partyId -> ... }` lambda (945-948). In `LendTransactionsListScreen.kt` delete the parameter at lines 85 and 534, the pass-through at 215, and replace line 543 `rate = getMonthlyRatePercent(lend.partyId)` with `rate = lend.monthlyRatePercent`. If line 543 sits in a `LaunchedEffect`/`produceState` that only existed to call the suspend lambda, simplify it to a plain value.

- [ ] **Step 3: Party ledger**

`PartyLedgerViewModel.kt`: delete `defaultMonthlyRatePercent` (line 86) and its assignment inside the profile collector at line 135 (keep `isSoundEnabled`). Delete `currentMonthlyRate()` (237-238). At line 164 and line 427 the code computes interest for a specific loan: read the surrounding function, and replace `currentMonthlyRate()` with that loan's `monthlyRatePercent` (the `LendEntry` is already in scope in both, because the function calls `interestDue` for it). If one of them has no loan in scope (a new-loan preview), use `RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT`.

`PartyLedgerScreen.kt:147`: `mutableStateOf("1.5")` becomes `mutableStateOf(RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT.toString())` with a trimmed trailing `.0` if the field shows `2.0` (use `"2"`). At 341 and 1291 replace `uiState.party?.monthlyInterestRate ?: uiState.defaultMonthlyRatePercent` with the selected loan's `monthlyRatePercent`.

- [ ] **Step 4: Demo data**

`DemoSimulator.kt:67`: `monthlyInterestRate = 1.5,` becomes `monthlyInterestRate = 2.0,` (party field, kept for data shape).

- [ ] **Step 5: Prove no loan code reads the old sources**

Run: `grep -rnE "getMonthlyRatePercent|defaultMonthlyRatePercent|currentMonthlyRate|\?\: 1\.5" --include='*.kt' composeApp/src/commonMain composeApp/src/jvmMain`
Expected: no output.

- [ ] **Step 6: Compile and commit**

Run: `./gradlew :composeApp:compileKotlinJvm -q` (expected success), then:

```bash
git add composeApp
git commit -m "feat(lend): interest uses each loan's own rate"
```

---

### Task 4: Rate field on the Lend form

**Files:**
- Modify: `composeApp/.../ui/lend/LendEntryFormState.kt` (state fields, `reset()` ~104-119, `toLendEntry` ~122-145, the existing "can save" check)
- Modify: `composeApp/.../ui/lend/LendEntryScreen.kt` and `LoanAdvanceEntryScreen.kt` (whichever hosts the MONEY amount field; find with `grep -n "amountRupees" composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/lend/*.kt`)

**Interfaces:**
- Consumes: `LendEntry.monthlyRatePercent`, `DEFAULT_MONTHLY_RATE_PERCENT`.
- Produces: `LendEntryFormState.rateInput: String` (default `"2"`), `LendEntryFormState.parsedRate: Double?`.

- [ ] **Step 1: State**

In `LendEntryFormState`, next to `notes`, add the field in the same style as its neighbours (a `var ... by mutableStateOf` if the others are):

```kotlin
    var rateInput by mutableStateOf("2")

    /** Null when the text is not a number above zero. */
    val parsedRate: Double?
        get() = rateInput.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 && it <= 100.0 }
```

In `reset()` add `rateInput = "2"`. In `toLendEntry` add to the `LendEntry(...)` call:

```kotlin
            monthlyRatePercent = if (isMoney) parsedRate ?: RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT
                                 else RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT,
```

Extend the existing save-enabled condition so a MONEY loan with `parsedRate == null` cannot be saved. Do not count `rateInput` in the "has unsaved input" check (it always has a value).

- [ ] **Step 2: UI field**

Under the amount field, shown only when `lendType == LendType.MONEY`, add a decimal text field. Use the same field composable and keyboard options as the amount field, with `keyboardType = KeyboardType.Decimal`, label "ब्याज दर (% प्रति माह)" (English: "Interest rate (% per month)"; follow how the screen already switches `isEnglish`), value `form.rateInput`, `isError = form.parsedRate == null`. Tokens only from `ui/theme/Color.kt`. It must wrap at 390dp (`Modifier.fillMaxWidth()`).

- [ ] **Step 3: Compile and commit**

Run: `./gradlew :composeApp:compileKotlinJvm -q` (expected success).

```bash
git add composeApp
git commit -m "feat(lend): rate field on the lend form, pre-filled 2"
```

---

### Task 5: Sync and backend

**Files:**
- Modify: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/sync/model/SyncDto.kt:13,151`
- Modify: `core-data/.../sync/SyncEngine.kt:193, 492`
- Modify: `backend/app/models/lend_entry.py`, `user.py:17`, `party.py:16`
- Modify: `backend/app/schemas/entries.py` (`LendEntryBase`, ~line 99-118), `backend/app/schemas/party.py:11`
- Modify: `backend/app/api/v1/endpoints/sync.py` (lend update ~476, create ~501)
- Create: `backend/migrations/versions/0005_loan_interest_rate.py`

**Interfaces:**
- Consumes: `LendEntry.monthlyRatePercent`, `lendEntryEntity.monthly_interest_rate`.
- Produces: wire field `monthly_interest_rate: float` on lend entries, default 2.0; Alembic head `0005_loan_interest_rate`.

- [ ] **Step 1: Client DTO and engine**

`SyncDto.kt:13`: `val monthly_interest_rate: Double? = 2.0,` (party). In the lend DTO next to `expected_return_date` (line 151) add `val monthly_interest_rate: Double = 2.0,`. `SyncEngine.kt` push mapping (~line 492, inside the lend DTO construction) add `monthly_interest_rate = l.monthly_interest_rate,`; pull mapping (~line 193) add `monthly_interest_rate = it.monthly_interest_rate` to the `insertLendEntryIfAbsent` call as the last argument.

- [ ] **Step 2: Backend model and schemas**

`lend_entry.py` in `LendEntry`, after `void_reason`:

```python
    monthly_interest_rate = Column(Float, nullable=False, default=2.0, server_default="2.0")
```

and add `Float` to the `sqlalchemy` import. `entries.py` `LendEntryBase`: `monthly_interest_rate: float = 2.0`. `user.py:17` `default=2.0`. `party.py:16` `default=2.0`. `schemas/party.py:11` `= 2.0`.

- [ ] **Step 3: Backend sync handler**

In `sync.py` the update branch (after `existing_lend.slip_no = l_in.slip_no`) add `existing_lend.monthly_interest_rate = l_in.monthly_interest_rate`; in the `LendEntry(...)` create call add `monthly_interest_rate=l_in.monthly_interest_rate,`. If the pull/response builder maps fields by hand, add it there too (`grep -n "slip_no" backend/app/api/v1/endpoints/sync.py`).

- [ ] **Step 4: Alembic migration**

Create `0005_loan_interest_rate.py`:

```python
"""per-loan monthly interest rate, default 2

Revision ID: 0005_loan_interest_rate
Revises: 0004_shop_quota
Create Date: 2026-11-13
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "0005_loan_interest_rate"
down_revision: Union[str, Sequence[str], None] = "0004_shop_quota"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    with op.batch_alter_table("lend_entries") as batch_op:
        batch_op.add_column(
            sa.Column("monthly_interest_rate", sa.Float(), nullable=False, server_default="2.0")
        )
    op.execute("UPDATE shop_profiles SET default_monthly_interest_rate = 2.0 WHERE default_monthly_interest_rate = 1.5")
    op.execute("UPDATE parties SET monthly_interest_rate = 2.0 WHERE monthly_interest_rate = 1.5")


def downgrade() -> None:
    with op.batch_alter_table("lend_entries") as batch_op:
        batch_op.drop_column("monthly_interest_rate")
```

- [ ] **Step 5: Run backend tests and compile**

Run: `cd backend && python -m pytest -q` then `cd .. && ./gradlew :core-data:compileKotlinJvm -q`
Expected: pass, success. Fix `backend/tests/test_sync.py` fixtures that say `1.5` (Task 6 covers the updated asserts).

- [ ] **Step 6: Commit**

```bash
git add core-data backend
git commit -m "feat(sync): per-loan rate over the wire, Alembic 0005 rewrites 1.5 to 2"
```

---

### Task 6: Tests (feature is final, so tests are written now)

**Files:**
- Modify: `core-domain/src/commonTest/kotlin/com/appwork/mandisamiti/domain/RuralInterestEngineTest.kt:12-17`
- Modify: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/LendEntryRepositoryTest.kt`
- Create: `core-database/src/jvmTest/kotlin/com/appwork/mandisamiti/database/Migration11Test.kt`
- Modify: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/lend/LendFlowTest.kt` (or a new `LendEntryFormRateTest.kt` beside it)
- Modify: `backend/tests/test_sync.py:9`, `backend/tests/test_entries_models.py`
- Modify: every other test fixture still on 1.5: `composeApp/src/jvmTest/.../e2e/MandiSamitiE2ETest.kt:78`

- [ ] **Step 1: Engine test uses 2%**

In `RuralInterestEngineTest.kt` set `val monthlyRatePercent = 2.0`, update the comments to `Monthly Rate: 2% (₹2 per ₹100 per month)` and `50,000 * 2% * 2 = ₹2,000 = 200,000 paise`, and the expected value to `200_000L`. Add:

```kotlin
    @Test
    fun defaultRateIsTwoPercent() {
        assertEquals(2.0, RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT)
    }

    @Test
    fun oneLoanCanCarryOneAndHalfPercent() {
        // 100,000 paise for 30 days at 1.5% = 1,500 paise
        val c = RuralInterestEngine.calculateAccruedInterestByDays(100_000L, 1.5, 30)
        assertEquals(1_500L, c.accruedInterestPaisa)
    }
```

- [ ] **Step 2: Repository round trip**

In `LendEntryRepositoryTest.kt`, using the file's existing `sampleLendEntry` and `repo`:

```kotlin
    @Test
    fun monthlyRateRoundTripsAndDefaultsToTwo() = runTest {
        repo.saveLendEntry(sampleLendEntry(id = "r-default"))
        repo.saveLendEntry(sampleLendEntry(id = "r-custom").copy(monthlyRatePercent = 1.5))
        assertEquals(2.0, repo.getLendEntryById("r-default")!!.monthlyRatePercent)
        assertEquals(1.5, repo.getLendEntryById("r-custom")!!.monthlyRatePercent)
    }
```

Match the helper names and `runTest` style already used in that file (read its first 40 lines before writing).

- [ ] **Step 3: Migration test**

Create `Migration11Test.kt`:

```kotlin
package com.appwork.mandisamiti.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class Migration11Test {
    @Test
    fun rewritesOldRatesAndAddsLoanColumn() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "CREATE TABLE lendEntryEntity (id TEXT PRIMARY KEY NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE shopProfileEntity (id TEXT PRIMARY KEY NOT NULL, default_monthly_interest_rate REAL NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE partyEntity (id TEXT PRIMARY KEY NOT NULL, monthly_interest_rate REAL)", 0)
        driver.execute(null, "INSERT INTO lendEntryEntity(id) VALUES ('l1')", 0)
        driver.execute(null, "INSERT INTO shopProfileEntity VALUES ('s1', 1.5), ('s2', 3.0)", 0)
        driver.execute(null, "INSERT INTO partyEntity VALUES ('p1', 1.5), ('p2', NULL)", 0)

        val sql = File("src/commonMain/sqldelight/com/appwork/mandisamiti/database/11.sqm").readText()
        sql.split(";").map { it.trim() }.filter { it.isNotEmpty() }.forEach { driver.execute(null, it, 0) }

        fun one(q: String): Double? = driver.executeQuery(null, q, { c ->
            app.cash.sqldelight.db.QueryResult.Value(if (c.next().value) c.getDouble(0) else null)
        }, 0).value
        assertEquals(2.0, one("SELECT monthly_interest_rate FROM lendEntryEntity WHERE id='l1'"))
        assertEquals(2.0, one("SELECT default_monthly_interest_rate FROM shopProfileEntity WHERE id='s1'"))
        assertEquals(3.0, one("SELECT default_monthly_interest_rate FROM shopProfileEntity WHERE id='s2'"))
        assertEquals(2.0, one("SELECT monthly_interest_rate FROM partyEntity WHERE id='p1'"))
        assertEquals(null, one("SELECT monthly_interest_rate FROM partyEntity WHERE id='p2'"))
    }
}
```

If the SQLDelight driver API in this repo differs (check `DatabaseTest.kt` for how it opens the driver), copy that opening code.

- [ ] **Step 4: Form state test**

Add to the lend UI tests:

```kotlin
    @Test
    fun newLoanFormStartsAtTwoPercentAndAcceptsOverride() {
        val form = LendEntryFormState()
        assertEquals("2", form.rateInput)
        assertEquals(2.0, form.parsedRate)
        form.rateInput = "1.5"
        assertEquals(1.5, form.parsedRate)
        form.rateInput = "0"
        assertEquals(null, form.parsedRate)
        form.reset()
        assertEquals("2", form.rateInput)
    }
```

Use the constructor the existing tests use for `LendEntryFormState` if it takes arguments.

- [ ] **Step 5: Backend tests**

Change `test_sync.py:9` `monthly_interest_rate=1.5` to `2.0`. In `test_entries_models.py` add a test that a `LendEntryCreate` built without `monthly_interest_rate` has `.monthly_interest_rate == 2.0` and one built with `1.5` keeps `1.5`.

- [ ] **Step 6: No loan 1.5 left**

Run: `grep -rnE "\b1\.5\b" --include='*.kt' --include='*.py' --include='*.sq' --include='*.sqm' . | grep -v "/build/\|0001_baseline"`
Expected: only the intentional test lines (`oneLoanCanCarryOneAndHalfPercent`, `monthlyRateRoundTripsAndDefaultsToTwo`, the form-state and backend override tests, `Migration11Test`, and the `11.sqm` / `0005` WHERE clauses). Anything else gets fixed.

- [ ] **Step 7: Run everything and commit**

Run: `./gradlew :composeApp:jvmTest :core-data:jvmTest :core-database:jvmTest :core-domain:jvmTest -q` and `cd backend && python -m pytest -q`
Expected: no failures.

```bash
git add -A core-domain core-data core-database composeApp backend
git commit -m "test(lend): per-loan rate, 2% default, migration 11"
```

---

### Task 7: Device check on staging and tracker logging

- [ ] **Step 1: Staging backend (needs Vivek's confirmation before running)**

Alembic `0005` must reach staging `http://64.227.142.180:8060` before the app syncs. Ask Vivek to confirm, then run the project's deploy path (`/project-deploy`). Expected: staging reports Alembic `0005_loan_interest_rate` and `/health` is OK.

- [ ] **Step 2: Install on Redmi `d93c743`**

Run: `./gradlew :composeApp:installDebug -q`. The dummy shop 9999900001 gets migration 11 on first launch.

- [ ] **Step 3: Checks (Light, Dark, 390dp)**

1. Existing Lend Party loan (Rs 5,000) shows interest at 2%, not 1.5.
2. New loan form: rate field shows `2`, saves; the loan list and ledger interest match 2%.
3. New loan with rate `1.5` typed: interest on that loan uses 1.5, another loan still 2.
4. Part payment clears interest first, then principal.
5. Empty or `0` rate blocks save and shows the error state.
6. No horizontal scroll at 390dp; no emoji; NGDL tokens.
7. Staging DB: `lend_entries.monthly_interest_rate` holds 2.0 and 1.5 for the two test loans after sync.

Writes on real data are blocked: use the dummy shop only.

- [ ] **Step 4: Log**

Append a `PROGRESS.md` entry (date, Claude, changes, the evidence above, next step). Update `FEATURES.json` entry `money-slip-interest-settlement`: `status` `passing` only if every check above passed on the device, otherwise leave `failing` and list what is unverified. Add a new feature entry at the end: `loan-rate-migration-staging` (2% rewrite visible on staging). Commit both files with the final commit.

- [ ] **Step 5: Stop for approval**

Do not touch `hot.md` or the parent pointer. Ask Vivek to approve; then update `hot.md` and bump the submodule pointer.

---

## Self-Review

- Spec coverage: per-loan stored rate (Tasks 1, 2), 2% default and no hardcoded 1.5 (1, 2, 3, 5, 6 step 6), editable per loan on the form (4), existing loans, shops and parties rewritten (2 step 2, 5 step 4), party rate no longer read for loans (3), simple interest and interest-first untouched (constraint), tests (6), device and staging (7). Gap noted: no screen edits the rate of an already saved loan. It is out of scope because the approved design only sets the rate on the Lend form.
- Placeholders: Task 3 steps 3 and Task 4 steps 1-2 describe edits by location because the surrounding functions were not read in full. The executor must read those functions first, and the grep in Task 3 step 5 and Task 6 step 6 verifies the result.
- Type consistency: `monthlyRatePercent` (domain and form), `monthly_interest_rate` (SQL, DTO, backend), `DEFAULT_MONTHLY_RATE_PERCENT`, `rateInput`, `parsedRate` are used the same way in every task.
