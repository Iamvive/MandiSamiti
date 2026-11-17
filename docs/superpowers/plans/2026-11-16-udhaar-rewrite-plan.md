# Udhaar Rewrite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split slip money from udhaar, and give udhaar its own simple screens: a landing pager (दिया / लिया), a short add form, one-button repayment (oldest loan first), and a Kisaan khata with separate slip and udhaar balances and a tagged ledger.

**Architecture:** `lendEntryEntity` becomes the single loan record for both directions (new `direction`, `kind`, receiver-note columns; interest off means rate 0). Every udhaar cash row gets a real `lend_id` link, so no balance or tag depends on Hindi text in remarks. A pure `UdhaarAllocator` splits a repayment across loans (oldest first, interest before principal). The party balance becomes two numbers: slip balance and udhaar balance.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, SQLDelight (`AppDatabase.sq` plus numbered `.sqm` migrations), kotlin.test, Python/Alembic backend.

Spec: `docs/superpowers/specs/2026-11-16-udhaar-rewrite-design.md`. Read it first.

## Global Constraints

Paths are relative to `projects/MandiSamiti/`.

- A slip owns its own money story. Slip payments never appear in Udhaar screens or totals. Udhaar balance and slip balance are never added together in the khata.
- Interest defaults **off** (stored as `monthly_interest_rate = 0.0`). When the trader switches it on, the suggested rate is `RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT` (2.0).
- Repayment: oldest loan first, interest before principal inside each loan. Overpayment is **blocked** with a message, never turned into a general jama.
- Every ledger row carries either a "पर्ची #..." tag or an "उधार" tag, never both.
- A loan cut from a slip is a repayment row with `paymentMode = BOOK_ENTRY` (so it never touches the Galla drawer) and `lend_id` set, `source_entry_id` null.
- Add form fields: mobile (required, first), name, amount, date (today default, future blocked), interest switch, and on उधार दिया only a "पैसा किसी और को दिया?" switch with a receiver name (mobile optional). Receiver is a text note, not a party. No village or "on behalf of" fields.
- Amounts are whole rupees in the forms; storage is paisa (`Long`).
- Grain udhaar stays out of scope. Do not delete `GrainLendEntry` or `LendEntryScreen`; just stop linking to them from the new screens.
- Local data is test-only. No data migration. Do add the `.sqm` migration so schemas stay consistent.
- UI: NGDL tokens only (`docs/design-system/DESIGN-SYSTEM.md` §2.1, §5), contrast 4.5:1 in light and dark, vector icons, no emoji, no horizontal scroll at 390dp. Strings follow the existing inline `isEnglish` pattern.
- Tests: `./gradlew :core-domain:jvmTest :core-data:jvmTest :composeApp:jvmTest`. Never `:core-domain:allTests` (known iOS compile failure). `HomeViewModelTest.testLendEntryReflectedInReceivablesAndHonestSubtitleWhenUnlinked` is flaky: rerun once before treating it as a failure.
- Backend is **not deployed** without Vivek's explicit approval.
- Never print or log secrets. Ask Vivek one question at a time.
- Finish every session by writing PROGRESS.md and FEATURES.json evidence (Task 13).

## Open decisions (ask Vivek one at a time only when you reach the task; defaults shown)

1. **DISCOUNT_GIVEN and INTEREST_ADDED cash rows and their ledger buttons.** Default: remove the buttons from the Party Ledger and drop these two rows from the new balances (Task 4, Task 9). Not in the spec.
2. **Home "market receivable" total.** Default: slip balances only, plus a new Udhaar card showing total दिया and total लिया with a link to the landing page (Task 11).
3. **Plain cash entry when the party has a balance in the opposite direction.** Default (Task 3): "रुपये दिए" repays an outstanding उधार लिया oldest first, else creates a new उधार दिया. "रुपये मिले" repays an outstanding उधार दिया, else creates a new उधार लिया. Overpayment against the opposite loans is blocked.

## File Structure

| File | Responsibility |
|---|---|
| `core-domain/.../model/LendEntry.kt` (modify) | `LendDirection`, `LendKind`, receiver fields, interest default 0 |
| `core-domain/.../model/CashTransaction.kt` (modify) | `lendId` |
| `core-domain/.../math/UdhaarAllocator.kt` (create) | Pure repayment split |
| `core-domain/.../usecase/UdhaarUseCase.kt` (create) | Create loan, repay, plain cash entry, loan cut |
| `core-domain/.../repository/LendRepository.kt` (modify) | `settleAll`, `getLoansForParty` |
| `core-domain/.../model/Party.kt` (modify) | `PartyBalance` split in two |
| `core-domain/.../ledger/LedgerTag.kt` (create) | Pure tag rule for a cash row |
| `core-database/.../AppDatabase.sq`, `14.sqm` (modify/create) | Columns and balance queries |
| `core-data/.../repository/OfflineFirstLendRepository.kt`, `OfflineFirstCashTransactionRepository.kt`, `OfflineFirstPartyRepository.kt` (modify) | Persistence |
| `composeApp/.../ui/udhaar/` (create) | `UdhaarLandingScreen`, `UdhaarRows.kt`, `UdhaarAddSheet`, `UdhaarAddFormState.kt`, `UdhaarPersonScreen` |
| `composeApp/.../ui/ledger/PartyLedgerScreen.kt`, `PartyLedgerViewModel.kt` (modify) | New khata layout and tags |
| `composeApp/.../App.kt` (modify) | Route `Screen.LendList` to the landing page |
| `core-data/.../sync/model/SyncDto.kt`, `SyncEngine.kt`, `backend/migrations/versions/0007_udhaar_fields.py`, backend models (modify/create) | Sync the new fields |

`...` stands for `src/commonMain/kotlin/com/appwork/mandisamiti/<layer>`. Tests mirror these under each module's `jvmTest` or `commonTest`.

---

### Task 0: Branch and baseline

- [ ] **Step 1:** `cd projects/MandiSamiti && git status && git checkout -b udhaar-rewrite main`
- [ ] **Step 2:** Run `./gradlew :core-domain:jvmTest :core-data:jvmTest :composeApp:jvmTest`. Expected: BUILD SUCCESSFUL. Note the test counts in your first PROGRESS entry.
- [ ] **Step 3:** Run `graphify query "lend settle repay"` or open `OfflineFirstLendRepository.kt`, `PartyLedgerViewModel.kt` and `App.kt` around the `Screen.LendList` branch (~line 1009) so you know the code you will replace. Do not read whole folders.

---

### Task 1: Model and schema columns

**Files:**
- Modify: `core-domain/.../model/LendEntry.kt`, `core-domain/.../model/CashTransaction.kt`
- Modify: `core-database/src/commonMain/sqldelight/com/appwork/mandisamiti/database/AppDatabase.sq` (tables `lendEntryEntity`, `cashTransactionEntity`; queries `insertLendEntry`, `insertCashTransaction`, `insertCashTransactionIfAbsent`)
- Create: `core-database/src/commonMain/sqldelight/com/appwork/mandisamiti/database/14.sqm`
- Modify: mappers `toDomain()` and insert calls in `OfflineFirstLendRepository.kt` and `OfflineFirstCashTransactionRepository.kt`
- Test: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/UdhaarSchemaRoundTripTest.kt`

**Interfaces:**
- Produces: `enum class LendDirection { GIVEN, TAKEN }`, `enum class LendKind { LOAN, PESGI, CASH }`; `LendEntry` gains `direction: LendDirection = GIVEN`, `kind: LendKind = LOAN`, `receiverName: String? = null`, `receiverPhone: String? = null`, and `monthlyRatePercent` default changes to `0.0`; `CashTransaction` gains `lendId: String? = null`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class UdhaarSchemaRoundTripTest {
    private val clock = object : Clock { override fun now() = Instant.fromEpochMilliseconds(2_000L) }

    @Test
    fun newLendAndCashFieldsSurviveSaveAndRead() = runTest {
        val db = createTestDatabase()
        db.appDatabaseQueries.insertParty("party", "shop", "Party", null, null, "FARMER", null, null, null, null, 1L, 1L, 0L, 0L)
        val d = StandardTestDispatcher(testScheduler)
        val lendRepo = OfflineFirstLendRepository(db, ioDispatcher = d, clock = clock)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = d, clock = clock)

        lendRepo.saveLendEntry(LendEntry(
            id = "l1", shopId = "shop", partyId = "party", lendType = LendType.MONEY, entryDate = 1_000L,
            amountPaisa = 500_000L, createdAt = 1_000L, updatedAt = 1_000L,
            direction = LendDirection.TAKEN, kind = LendKind.PESGI,
            receiverName = "Suresh", receiverPhone = "9876543210", monthlyRatePercent = 0.0,
        ))
        cashRepo.save(CashTransaction(
            id = "c1", shopId = "shop", partyId = "party", transactionType = TransactionType.JAMA_RECEIVED,
            amountPaisa = 500_000L, transactionDate = 1_000L, createdAt = 1_000L, updatedAt = 1_000L, lendId = "l1",
        ))

        val lend = lendRepo.getLendEntryById("l1")!!
        assertEquals(LendDirection.TAKEN, lend.direction)
        assertEquals(LendKind.PESGI, lend.kind)
        assertEquals("Suresh", lend.receiverName)
        assertEquals("9876543210", lend.receiverPhone)
        assertEquals(0.0, lend.monthlyRatePercent)
        assertEquals("l1", cashRepo.getById("c1")!!.lendId)
    }
}
```

Before running: open `OfflineFirstCashTransactionRepository.kt` and replace `save` and `getById` with the real method names for inserting and loading one row. Keep everything else.

- [ ] **Step 2:** Run `./gradlew :core-data:jvmTest --tests "*UdhaarSchemaRoundTripTest*"`. Expected: FAIL (compile error, fields missing).
- [ ] **Step 3: Implement.**
  - `LendEntry.kt`: add the two enums and four fields; change the `monthlyRatePercent` default to `0.0`; drop the `RuralInterestEngine` import if unused.
  - `CashTransaction.kt`: add `val lendId: String? = null` at the end.
  - `AppDatabase.sq`, in `lendEntryEntity`, add after `monthly_interest_rate` (and change its default to `0.0`): `direction TEXT NOT NULL DEFAULT 'GIVEN', kind TEXT NOT NULL DEFAULT 'LOAN', receiver_name TEXT, receiver_phone TEXT`. In `cashTransactionEntity` add `lend_id TEXT,` after `source_entry_id`, and `CREATE INDEX idx_tx_lend ON cashTransactionEntity(lend_id);`.
  - Extend the column list and `VALUES` placeholders of `insertLendEntry` (four more `?`), `insertCashTransaction` and `insertCashTransactionIfAbsent` (one more `?`).
  - `14.sqm`:
    ```sql
    ALTER TABLE lendEntryEntity ADD COLUMN direction TEXT NOT NULL DEFAULT 'GIVEN';
    ALTER TABLE lendEntryEntity ADD COLUMN kind TEXT NOT NULL DEFAULT 'LOAN';
    ALTER TABLE lendEntryEntity ADD COLUMN receiver_name TEXT;
    ALTER TABLE lendEntryEntity ADD COLUMN receiver_phone TEXT;
    ALTER TABLE cashTransactionEntity ADD COLUMN lend_id TEXT;
    CREATE INDEX idx_tx_lend ON cashTransactionEntity(lend_id);
    ```
  - Update the mappers and every `insertLendEntry(...)` / `insertCashTransaction*(...)` call site (`grep -rn "insertLendEntry\|insertCashTransaction" core-data core-database composeApp`). Enums are stored with `.name` and read with `valueOf`.
- [ ] **Step 4:** Rerun the test. Expected: PASS. Then run `./gradlew :core-domain:jvmTest :core-data:jvmTest` and fix tests that assumed the old 2.0 default rate (set the rate explicitly in those tests).
- [ ] **Step 5: Commit** `feat(mandisamiti): udhaar direction, kind, receiver note and cash lend link`

---

### Task 2: UdhaarAllocator (pure)

**Files:**
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/math/UdhaarAllocator.kt`
- Test: `core-domain/src/commonTest/kotlin/com/appwork/mandisamiti/domain/UdhaarAllocatorTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class OpenLoan(val lendId: String, val entryDate: Long, val outstandingPrincipalPaisa: Long, val interestDuePaisa: Long)
  data class LoanPayment(val lendId: String, val principalPaisa: Long, val interestPaisa: Long)
  sealed interface AllocationResult {
      data class Ok(val parts: List<LoanPayment>) : AllocationResult
      data class Over(val totalDuePaisa: Long) : AllocationResult
  }
  object UdhaarAllocator { fun allocate(loans: List<OpenLoan>, paymentPaisa: Long): AllocationResult }
  ```

- [ ] **Step 1: Write the failing test**

```kotlin
package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.math.*
import kotlin.test.*

class UdhaarAllocatorTest {
    private fun loan(id: String, date: Long, principal: Long, interest: Long = 0L) = OpenLoan(id, date, principal, interest)

    @Test fun oldestLoanFirstInterestBeforePrincipal() {
        // a (older): Rs 1000 principal + Rs 50 interest. b (newer): Rs 500 principal. Pay Rs 1100.
        val r = UdhaarAllocator.allocate(listOf(loan("b", 2, 50_000), loan("a", 1, 100_000, 5_000)), 110_000)
        assertEquals(
            AllocationResult.Ok(listOf(LoanPayment("a", 100_000, 5_000), LoanPayment("b", 5_000, 0))), r,
        )
    }

    @Test fun smallPaymentClearsInterestOnly() {
        val r = UdhaarAllocator.allocate(listOf(loan("a", 1, 100_000, 5_000)), 3_000)
        assertEquals(AllocationResult.Ok(listOf(LoanPayment("a", 0, 3_000))), r)
    }

    @Test fun overpaymentIsBlockedWithTotalDue() {
        val r = UdhaarAllocator.allocate(listOf(loan("a", 1, 100_000, 5_000)), 105_001)
        assertEquals(AllocationResult.Over(105_000), r)
    }

    @Test fun exactPayoffIsAllowed() {
        val r = UdhaarAllocator.allocate(listOf(loan("a", 1, 100_000, 5_000)), 105_000)
        assertEquals(AllocationResult.Ok(listOf(LoanPayment("a", 100_000, 5_000))), r)
    }

    @Test fun paidLoansAreSkipped() {
        val r = UdhaarAllocator.allocate(listOf(loan("a", 1, 0, 0), loan("b", 2, 10_000)), 4_000)
        assertEquals(AllocationResult.Ok(listOf(LoanPayment("b", 4_000, 0))), r)
    }

    @Test fun zeroPaymentIsRejected() {
        assertFailsWith<IllegalArgumentException> { UdhaarAllocator.allocate(listOf(loan("a", 1, 100)), 0) }
    }
}
```

- [ ] **Step 2:** Run `./gradlew :core-domain:jvmTest --tests "*UdhaarAllocatorTest*"`. Expected: FAIL (unresolved reference).
- [ ] **Step 3: Implement**

```kotlin
package com.appwork.mandisamiti.domain.math

data class OpenLoan(val lendId: String, val entryDate: Long, val outstandingPrincipalPaisa: Long, val interestDuePaisa: Long)
data class LoanPayment(val lendId: String, val principalPaisa: Long, val interestPaisa: Long)

sealed interface AllocationResult {
    data class Ok(val parts: List<LoanPayment>) : AllocationResult
    data class Over(val totalDuePaisa: Long) : AllocationResult
}

/** Oldest loan first; inside one loan, interest before principal. Overpayment is reported, never absorbed. */
object UdhaarAllocator {
    fun allocate(loans: List<OpenLoan>, paymentPaisa: Long): AllocationResult {
        require(paymentPaisa > 0) { "Payment must be above zero" }
        val live = loans.filter { it.outstandingPrincipalPaisa > 0 || it.interestDuePaisa > 0 }.sortedBy { it.entryDate }
        val totalDue = live.sumOf { it.outstandingPrincipalPaisa + it.interestDuePaisa }
        if (paymentPaisa > totalDue) return AllocationResult.Over(totalDue)
        var left = paymentPaisa
        val parts = mutableListOf<LoanPayment>()
        for (loan in live) {
            if (left == 0L) break
            val interest = minOf(left, loan.interestDuePaisa); left -= interest
            val principal = minOf(left, loan.outstandingPrincipalPaisa); left -= principal
            if (interest + principal > 0) parts += LoanPayment(loan.lendId, principal, interest)
        }
        return AllocationResult.Ok(parts)
    }
}
```
- [ ] **Step 4:** Rerun. Expected: PASS (6 tests).
- [ ] **Step 5: Commit** `feat(mandisamiti): udhaar repayment allocator`

---

### Task 3: UdhaarUseCase (create loan, repay, plain cash, loan cut)

**Files:**
- Modify: `core-domain/.../repository/LendRepository.kt`, `core-data/.../repository/OfflineFirstLendRepository.kt`
- Create: `core-domain/.../usecase/UdhaarUseCase.kt`
- Test: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/UdhaarUseCaseTest.kt`

**Interfaces:**
- Consumes: Task 1 fields, Task 2 `UdhaarAllocator`, `RuralInterestEngine.interestDue(principalPaisa, monthlyRatePercent, lentAtMs, settlements, nowMs)`.
- Produces:
  ```kotlin
  // LendRepository additions
  suspend fun getLoansForParty(shopId: String, partyId: String, direction: LendDirection): List<LendEntry>   // non-void, non-deleted, MONEY only
  suspend fun settleAll(items: List<Pair<LendSettlement, List<CashTransaction>>>)                         // one DB transaction, reuses settleInTransaction
  suspend fun saveLoanWithCash(entry: LendEntry, cash: CashTransaction)                                   // one DB transaction

  class UdhaarOverpaymentException(val totalDuePaisa: Long) : IllegalArgumentException("Payment is above the amount due")

  data class UdhaarLoanForm(
      val shopId: String, val partyId: String, val direction: LendDirection, val amountPaisa: Long,
      val date: Long, val kind: LendKind = LendKind.LOAN, val monthlyRatePercent: Double = 0.0,
      val receiverName: String? = null, val receiverPhone: String? = null,
  )
  enum class CashSide { GAVE, GOT }

  class UdhaarUseCase(repo: LendRepository, newId: () -> String, nowMillis: () -> Long) {
      suspend fun createLoan(form: UdhaarLoanForm): LendEntry
      suspend fun repay(shopId: String, partyId: String, direction: LendDirection, paymentPaisa: Long,
                        paidAt: Long, mode: PaymentMode = PaymentMode.CASH, remarks: String? = null): List<LoanPayment>
      suspend fun plainCash(shopId: String, partyId: String, side: CashSide, amountPaisa: Long, date: Long): PlainCashResult
  }
  sealed interface PlainCashResult { data class Repaid(val parts: List<LoanPayment>) : PlainCashResult; data class NewLoan(val loan: LendEntry) : PlainCashResult }
  ```

Cash row rules: loan GIVEN creates `UDHAR_GIVEN` (cash out); loan TAKEN creates `JAMA_RECEIVED` (cash in); repaying a GIVEN loan writes `JAMA_RECEIVED`; repaying a TAKEN loan writes `UDHAR_GIVEN`. Every row has `lendId` set and `sourceEntryId = null`. Principal and interest are separate rows (interest row type `INTEREST_RECEIVED` for GIVEN loans; for TAKEN loans interest paid is `UDHAR_GIVEN` too, since it is cash out).

- [ ] **Step 1: Write the failing tests** in `UdhaarUseCaseTest` (same setup as `UdhaarSchemaRoundTripTest`: `createTestDatabase()`, seed party `"party"`, shop `"shop"`, a counter-based `newId`, fixed `nowMillis`). Cases, each asserting on `LendRepository` and cash rows read back:
  1. `createLoan` GIVEN Rs 10,000, rate 0.0: one `LendEntry` (direction GIVEN, rate 0.0) and one `UDHAR_GIVEN` cash row with `lendId` set, `amountPaisa = 1_000_000`.
  2. `createLoan` TAKEN: cash row type is `JAMA_RECEIVED`.
  3. `repay` Rs 6,000 across two GIVEN loans (Rs 5,000 on day 1, Rs 5,000 on day 2, interest off): first loan settled fully, second reduced by Rs 1,000; two settlements, two `JAMA_RECEIVED` rows with the right `lendId`s.
  4. `repay` above the total due throws `UdhaarOverpaymentException(totalDue)` and writes no settlement and no cash row.
  5. `repay(..., mode = BOOK_ENTRY)` writes rows with `paymentMode = BOOK_ENTRY`.
  6. Interest on: loan Rs 10,000 at 2.0 percent, 30 days old, `repay` Rs 10,200 clears interest Rs 200 first (use `nowMillis` = entryDate + 30 days).
  7. `plainCash(GAVE)` with no loans creates a GIVEN loan of kind `CASH`; with an outstanding TAKEN loan it repays that loan instead; `plainCash(GOT)` mirrors this; an amount above the opposite loan's due throws `UdhaarOverpaymentException`.
- [ ] **Step 2:** Run `./gradlew :core-data:jvmTest --tests "*UdhaarUseCaseTest*"`. Expected: FAIL (unresolved references).
- [ ] **Step 3: Implement.**
  - `OfflineFirstLendRepository`: `getLoansForParty` filters `queries.getLendEntriesByShop(shopId)`; `settleAll` is `withContext(ioDispatcher) { database.transaction { val now = clock.now().toEpochMilliseconds(); items.forEach { (s, c) -> settleInTransaction(s, c, now) } } }`; `saveLoanWithCash` inserts the lend row and cash row (`insertCashEntry`) in one `database.transaction`.
  - `UdhaarUseCase.repay`: load loans with `getLoansForParty`, build `OpenLoan` per loan (`outstanding = amountPaisa - sum(principalPaisa of non-void settlements)`, `interestDue = RuralInterestEngine.interestDue(...)`), call `UdhaarAllocator.allocate`, map `Over` to `UdhaarOverpaymentException`, and build one `LendSettlement` plus its cash rows per `LoanPayment` (`amountPaisa = principal + interest`, `principalPaisa`, `interestPaisa`, `calculatedInterestPaisa = interestPaisa`), then `repo.settleAll(...)`.
  - `plainCash`: pick the opposite direction (GAVE repays TAKEN; GOT repays GIVEN). If `getLoansForParty(...)` has any loan with due above zero, call `repay`; otherwise `createLoan` with `kind = CASH`.
- [ ] **Step 4:** Rerun. Expected: PASS. Then `./gradlew :core-domain:jvmTest :core-data:jvmTest`.
- [ ] **Step 5: Commit** `feat(mandisamiti): udhaar use case with atomic multi-loan repayment`

---

### Task 4: Two balances (slip and udhaar)

**Files:**
- Modify: `AppDatabase.sq` (query `getPartyBalance`, ~line 412), `core-domain/.../model/Party.kt` (`PartyBalance`), `OfflineFirstPartyRepository.kt` (~line 35)
- Modify call sites that compile-fail: `HomeViewModel.kt` (~303), `PartyLedgerViewModel.kt` (~145), plus tests that used `balancePaisa` (`PartyLedgerBalanceF01Test`, `LedgerAuditTest`, `PilotRepositoryMoneyInvariantTest`, `RepositoryTest` and others; find with `grep -rn "balancePaisa\|getPartyBalance" --include=*.kt .`)
- Test: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/PartyBalanceSplitTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class PartyBalance(
      val party: Party,
      val slipBalancePaisa: Long,    // + they owe us on slips, - we owe them
      val udhaarGivenPaisa: Long,    // principal still owed to us
      val udhaarTakenPaisa: Long,    // principal we still owe them
      val lastTransactionDate: Long?,
  ) { val udhaarNetPaisa: Long get() = udhaarGivenPaisa - udhaarTakenPaisa }
  ```
  `balancePaisa` is **removed** so the compiler finds every old use.

- [ ] **Step 1: Write the failing test** `PartyBalanceSplitTest`. Seed a party, then assert via `OfflineFirstPartyRepository.getPartyBalanceStream("party").first()`:
  1. A GIVEN loan of Rs 10,000 gives `udhaarGivenPaisa = 1_000_000`, `slipBalancePaisa = 0`.
  2. A buy slip (`net_payable 1_000_000`, `paid 400_000`, not khaate) gives `slipBalancePaisa = -600_000` and leaves `udhaarGivenPaisa` unchanged. This is the core regression: **a slip payment never changes the udhaar balance**.
  3. A repayment settlement of principal Rs 4,000 on the GIVEN loan gives `udhaarGivenPaisa = 600_000` and leaves `slipBalancePaisa` unchanged.
  4. A TAKEN loan of Rs 2,000 gives `udhaarTakenPaisa = 200_000` and `udhaarNetPaisa = 400_000`.
  5. A voided loan is ignored.
  Use `db.appDatabaseQueries` inserts like `LendRepositoryTest` (parties via `insertParty`) and `OfflineFirstBuyRepository` or direct insert for the slip row; copy an existing slip fixture from `PilotRepositoryMoneyInvariantTest`.
- [ ] **Step 2:** Run it. Expected: FAIL (compile error).
- [ ] **Step 3: Implement.** In `getPartyBalance`, replace the one `balance_paisa` expression with three columns:
  - `slip_balance_paisa`: only the old terms 5, 6, 7, 8 (deals as buyer and farmer, sell entries, buy entries; keep each term's existing filters, including `is_khaate = 0`). Terms 1 to 4, 9 and 10 are removed from this column.
  - `udhaar_given_paisa` and `udhaar_taken_paisa`, one subquery each:
    ```sql
    COALESCE((SELECT SUM(l.amount_paisa) FROM lendEntryEntity l
              WHERE l.party_id = p.id AND l.direction = 'GIVEN' AND l.lend_type = 'MONEY' AND l.is_deleted = 0 AND l.is_void = 0), 0)
    - COALESCE((SELECT SUM(CASE WHEN s.principal_paisa != 0 OR s.interest_paisa != 0 THEN s.principal_paisa ELSE s.amount_paisa END)
                FROM lendSettlementEntity s JOIN lendEntryEntity l ON s.lend_id = l.id
                WHERE l.party_id = p.id AND l.direction = 'GIVEN' AND l.lend_type = 'MONEY'
                  AND s.is_void = 0 AND l.is_void = 0 AND l.is_deleted = 0), 0) AS udhaar_given_paisa
    ```
    and the same with `'TAKEN'` for `udhaar_taken_paisa`.
  - Update the mapper and `PartyBalance`. Apply Open decision 1 for `INTEREST_ADDED` and `DISCOUNT_GIVEN` (default: not counted). Fix every compile error: `HomeViewModel` uses `slipBalancePaisa` for its receivable total for now, `PartyLedgerViewModel` carries all three numbers in `PartyLedgerUiState` (replace `balancePaisa` with `slipBalancePaisa`, `udhaarGivenPaisa`, `udhaarTakenPaisa`).
- [ ] **Step 4:** Rerun the new test and then `./gradlew :core-domain:jvmTest :core-data:jvmTest :composeApp:jvmTest`. Fix the old tests that asserted the single combined balance by splitting their expectations. Do not weaken them; assert the slip and udhaar numbers separately.
- [ ] **Step 5: Commit** `feat(mandisamiti): separate slip balance and udhaar balance`

---

### Task 5: Ledger tag rule

**Files:**
- Create: `core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/ledger/LedgerTag.kt`
- Modify: `composeApp/.../ui/ledger/PartyLedgerViewModel.kt` (`LedgerItem.CashItem`, `LendItem`), `PartyLedgerScreen.kt` (row rendering)
- Test: `core-domain/src/commonTest/kotlin/com/appwork/mandisamiti/domain/LedgerTagTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  sealed interface LedgerTag { data class Slip(val slipType: String, val slipId: String) : LedgerTag; data object Udhaar : LedgerTag }
  fun ledgerTagFor(cash: CashTransaction): LedgerTag?   // Slip if sourceEntryId != null, else Udhaar if lendId != null, else null
  ```
  A row with both set is a data error: `sourceEntryId` wins (slip), and the test pins that choice.

- [ ] **Step 1: Write the failing test** covering: slip row returns `Slip("BUY", id)`; lend row returns `Udhaar`; a row with neither returns `null`; a row with both returns `Slip`.
- [ ] **Step 2:** Run it, expect FAIL.
- [ ] **Step 3: Implement** the function, then in `PartyLedgerViewModel` expose `tag: LedgerTag?` on `CashItem` (`ledgerTagFor(cash)`) and `LedgerTag.Udhaar` on `LendItem`. In `PartyLedgerScreen` render a small chip next to the title: "पर्ची #<slipNo>" (look up the slip number from the loaded buys/sells by id) or "उधार". Use NGDL chip tokens already used by `MandiChip`; vector-free text chip, no emoji.
- [ ] **Step 4:** Run the test (PASS) and `./gradlew :composeApp:jvmTest`.
- [ ] **Step 5: Commit** `feat(mandisamiti): slip and udhaar tags on ledger rows`

---

### Task 6: Udhaar landing page (replaces LendList)

**Files:**
- Create: `composeApp/.../ui/udhaar/UdhaarRows.kt`, `composeApp/.../ui/udhaar/UdhaarLandingScreen.kt`
- Modify: `composeApp/.../App.kt` (`Screen.LendList` branch ~1009-1045; `Screen.LendEntry` branch ~944-990)
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/udhaar/UdhaarRowsTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class UdhaarPersonRow(
      val partyId: String, val name: String, val village: String?,
      val principalDuePaisa: Long, val interestDuePaisa: Long, val interestOn: Boolean,
  )
  fun buildUdhaarRows(
      direction: LendDirection, parties: List<Party>, loans: List<LendEntry>,
      settlements: List<LendSettlement>, nowMs: Long,
  ): List<UdhaarPersonRow>      // one row per party with principalDue + interestDue > 0, largest due first
  ```

- [ ] **Step 1: Write the failing test** `UdhaarRowsTest`: (a) two GIVEN loans for one party collapse into one row with summed principal; (b) a TAKEN loan does not appear in the GIVEN list; (c) a fully repaid party is absent; (d) `interestOn` is true only when some open loan has a rate above 0; (e) with a 2.0 percent loan 30 days old, `interestDuePaisa` equals `RuralInterestEngine.interestDue(...)`; (f) rows are sorted by total due, descending; (g) a voided loan is ignored.
- [ ] **Step 2:** Run it, expect FAIL.
- [ ] **Step 3: Implement** `buildUdhaarRows` (group loans by `partyId`, filter by direction and `!isVoid` and `MONEY`, compute outstanding and interest per loan as `PartyLedgerViewModel` does at ~163-175, then drop zero rows). Then build `UdhaarLandingScreen`: top bar (title "उधार" / "Udhaar", back to Home), `TabRow` with "उधार दिया" and "उधार लिया" over a `HorizontalPager` (same pager pattern as `LoanAdvanceEntryScreen.kt` ~246-260, tabs switch on tap and swipe), each page a `LazyColumn` of person rows (name, village, "बाकी ₹X", small "ब्याज" chip when `interestOn`; tap opens the person screen from Task 8), a total line at the top of each page, an honest empty state, and a FAB or bottom button "जोड़ें" that opens the add sheet from Task 7. Data comes from `LendRepository.getLendEntriesStream`, `getSettlementsStream` and the party repository, combined in a small `UdhaarLandingViewModel` in the same file. In `App.kt`, make `Screen.LendList` render this screen (keep `onBack = Screen.Home`), and point the Home callback `onNavigateToLendList` at it. Leave the grain screens in place but unreachable from here.
- [ ] **Step 4:** Run the test (PASS), build `./gradlew :composeApp:jvmTest`. On the emulator, view light and dark at desktop width and 390dp: no horizontal scroll, contrast checked. Save screenshots under `docs/testing/screenshots/udhaar/` as `t6-landing-<tab>-<light|dark>-390.png`.
- [ ] **Step 5: Commit** `feat(mandisamiti): udhaar landing page with diya and liya pager`

---

### Task 7: Short add form

**Files:**
- Create: `composeApp/.../ui/udhaar/UdhaarAddFormState.kt`, `composeApp/.../ui/udhaar/UdhaarAddSheet.kt`
- Modify: `UdhaarLandingScreen.kt` (open the sheet; call `UdhaarUseCase.createLoan`), `App.kt` (wire the use case)
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/udhaar/UdhaarAddFormStateTest.kt`

**Interfaces:**
- Consumes: `UdhaarLoanForm`, `UdhaarUseCase.createLoan`, party repository lookup by phone (see `LendPartyResolver.kt` for the existing find-or-create pattern; reuse it, do not write a second resolver).
- Produces:
  ```kotlin
  class UdhaarAddFormState(val direction: LendDirection, val todayMs: Long) {
      var phone: String; fun updatePhone(input: String)         // digits only, max 10
      var name: String; var amountRupees: String
      var dateMs: Long = todayMs
      var interestOn: Boolean = false; var rateInput: String = "2"
      var giveToOther: Boolean = false; var receiverName: String = ""; var receiverPhone: String = ""
      val isValid: Boolean
      fun toForm(shopId: String, partyId: String): UdhaarLoanForm
  }
  ```

- [ ] **Step 1: Write the failing test.** Cases: invalid until phone has 10 digits, name is non-blank and amount is a whole positive number; `updatePhone("98-76 543210x")` keeps digits only (`"9876543210"`); future `dateMs` makes the form invalid; with `interestOn = false`, `toForm(...).monthlyRatePercent == 0.0`; with `interestOn = true` and rate `"2.5"` it is `2.5`, and a non-numeric or zero rate makes the form invalid; `giveToOther = true` with a blank receiver name is invalid; with it on, `toForm` carries `receiverName` and `receiverPhone` (blank phone becomes null); for a TAKEN form `giveToOther` is ignored and receiver fields are null.
- [ ] **Step 2:** Run it, expect FAIL.
- [ ] **Step 3: Implement** the state class (adapt the phone validity check to the shared validator if `PhoneNumber.kt` already exposes one: `grep -n "fun " core-domain/src/commonMain/kotlin/com/appwork/mandisamiti/domain/model/PhoneNumber.kt`). Build `UdhaarAddSheet` as a `ModalBottomSheet` with only these fields in this order: mobile (number keyboard, on a saved match fill the name), name, amount, date (tap to change, same date picker as the old form), interest switch with the rate field shown only when on, and on the दिया tab only the "पैसा किसी और को दिया?" switch with receiver name and optional mobile. One primary "सेव करें" button. Save resolves or creates the party (farmer type) and calls `createLoan`; on success close the sheet and show the person in the list.
- [ ] **Step 4:** Run the test (PASS). Device check: add one loan with interest off, one with interest on, one with a receiver; confirm each shows correctly in the list; screenshots `t7-add-<case>-<light|dark>-390.png`.
- [ ] **Step 5: Commit** `feat(mandisamiti): simple udhaar add form`

---

### Task 8: Person screen and one-button repayment

**Files:**
- Create: `composeApp/.../ui/udhaar/UdhaarPersonScreen.kt`, `composeApp/.../ui/udhaar/UdhaarRepayFormState.kt`
- Modify: `App.kt` (new `Screen.UdhaarPerson(partyId, direction)`; add to the back-navigation map near line 107)
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/udhaar/UdhaarRepayFormStateTest.kt`

**Interfaces:**
- Consumes: `UdhaarUseCase.repay`, `UdhaarOverpaymentException`, `buildUdhaarRows`.
- Produces: `class UdhaarRepayFormState(val totalDuePaisa: Long, val todayMs: Long)` with `amountRupees`, `dateMs`, `error: String?`, `isValid`, `paymentPaisa`, and `fun onOverpayment(e: UdhaarOverpaymentException)` which sets the message "बाकी सिर्फ ₹X है" (English: "Only ₹X is due").

- [ ] **Step 1: Write the failing test.** Cases: blank or zero amount is invalid; an amount above `totalDuePaisa` is invalid and sets the message with the due amount; exactly the due amount is valid; a future date is invalid; `onOverpayment` sets the same message.
- [ ] **Step 2:** Run it, expect FAIL.
- [ ] **Step 3: Implement** the state and the screen: header with the person's name, village and phone; one balance line "बाकी ₹X" with "+ ब्याज ₹Y" under it when interest is on; the list of that person's loans and repayments in this direction (each loan row shows date, amount, "पेशगी" chip for `LendKind.PESGI`, and "प्राप्तकर्ता: <name>" when `receiverName` is set); and one button, "पैसे मिले" on the दिया side and "पैसे दिए" on the लिया side, opening a small dialog with amount and date only. Save calls `UdhaarUseCase.repay`. Keep edit and void on repayment rows by reusing `EditRepaymentDialog.kt`, `RepaymentHistoryDialog.kt` and the void flow from `PartyLedgerScreen.kt`; do not rewrite them. Delete nothing from the old `RepayLoanDialog.kt` until Task 9 stops using it.
- [ ] **Step 4:** Run the test (PASS). Device check: two loans, pay part (check oldest first), pay the exact rest, try to overpay (message shows, nothing saved); screenshots `t8-person-<case>-<light|dark>-390.png`.
- [ ] **Step 5: Commit** `feat(mandisamiti): udhaar person screen and oldest-first repayment`

---

### Task 9: Kisaan khata layout and plain cash routing

**Files:**
- Modify: `composeApp/.../ui/ledger/PartyLedgerScreen.kt`, `PartyLedgerViewModel.kt`
- Delete (after references are gone): `RepayLoanDialog.kt` and any loan-only code left unused in `PartyLedgerScreen.kt`
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/ledger/PartyLedgerViewModelTest.kt` (extend)

**Interfaces:**
- Consumes: `PartyBalance` (Task 4), `ledgerTagFor` (Task 5), `UdhaarUseCase.plainCash` and `PlainCashResult` (Task 3), `buildUdhaarRows` and the person-screen pieces (Tasks 6, 8).

- [ ] **Step 1: Write the failing tests** in `PartyLedgerViewModelTest`: (a) state exposes `slipBalancePaisa` and `udhaarNetPaisa` separately and a buy slip payment moves only the slip number; (b) `onPlainCash(CashSide.GAVE, 5_000)` with no loans creates a GIVEN loan of kind CASH and raises `udhaarGivenPaisa`; (c) `onPlainCash(CashSide.GOT, ...)` above what the party owes sets an error message and writes nothing when they owe us, per Open decision 3; (d) every row in the merged ledger list has the tag from `ledgerTagFor`.
- [ ] **Step 2:** Run them, expect FAIL.
- [ ] **Step 3: Implement** the khaate layout: two balance cards side by side at the top ("पर्चियों का बाकी" and "उधार बाकी", never summed); under them an udhaar section with the दिया / लिया `HorizontalPager` for this party only (their loans, the repayment button from Task 8); below that the single date-ordered ledger with the Task 5 tags. Route the existing "रुपये दिए" and "रुपये मिले" buttons through `UdhaarUseCase.plainCash` (no more direct `UDHAR_GIVEN` or `JAMA_RECEIVED` rows without a link). Remove the "apply to loan?" dialog and the loan option of the "हिसाब चुकता" dialog (it keeps buy and sell slips only). Apply Open decision 1 to the discount and interest buttons.
- [ ] **Step 4:** Run `./gradlew :composeApp:jvmTest`. Device check light, dark, 390dp: farmer with a slip and a loan shows two cards and a correctly tagged ledger. Screenshots `t9-khata-<light|dark>-390.png`.
- [ ] **Step 5: Commit** `feat(mandisamiti): kisaan khata with separate slip and udhaar balances`

---

### Task 10: Buy slip loan cut and pesgi adjustment

**Files:**
- Modify: the Buy save path. Start from `composeApp/.../App.kt` (~line 507, `buyForm.setLoan(...)`), `ui/buy/BuyEntryScreen.kt` (~689-830, the loan card), `ui/buy/BuyPaymentCalculations.kt`, and the Buy checkout in `core-data` (`BuyCheckoutTest.kt` shows how checkout writes settlements through `settleInTransaction`).
- Test: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/BuyLoanCutTest.kt`

**Interfaces:**
- Consumes: `UdhaarAllocator`, `LendRepository.settleInTransaction` (internal, same module), `getLoansForParty`.
- Produces: when a Buy slip carries a loan cut of `X` paisa, checkout allocates `X` across the farmer's GIVEN loans (oldest first) inside the same DB transaction as the slip, writing settlement rows plus cash rows with `paymentMode = BOOK_ENTRY`, `lendId` set, `sourceEntryId = null`, and remarks "पर्ची #<n> से कटौती".

- [ ] **Step 1: Write the failing test** `BuyLoanCutTest`: farmer with a GIVEN loan of Rs 5,000 (and a second of Rs 3,000); save a buy slip with a Rs 6,000 loan cut. Assert: the slip's deduction equals Rs 6,000, loan 1 is settled and loan 2 has Rs 2,000 left, the new cash rows are `BOOK_ENTRY` with `lendId` set and `sourceEntryId == null`, the Galla drawer total (CASH rows) is unchanged by the cut, and udhaar given drops by exactly Rs 6,000 while the slip balance reflects only the slip. A second case: a cut above the loan total is rejected and nothing is saved.
- [ ] **Step 2:** Run it, expect FAIL.
- [ ] **Step 3: Implement** by replacing the current loan-cut write in checkout with the allocator path above. The Buy screen's loan card keeps its single "उधार से काटें" field, but its numbers now come from the udhaar balance (principal plus interest due) and the field is capped at that total. Also add the pesgi adjustment: when the farmer has loans of kind `PESGI`, the same card works as the adjustment; no new field. Keep the slip's existing deduction line (`उधार कटौती`) and receipt formats unchanged.
- [ ] **Step 4:** Run the test (PASS) and `./gradlew :core-data:jvmTest :composeApp:jvmTest`. Update `BuyEntryLoanSectionTest`, `DayCloseCreditBuyTest` and `BuyCheckoutTest` where they asserted the old row shapes.
- [ ] **Step 5: Commit** `feat(mandisamiti): loan cut on buy slip repays udhaar oldest first`

---

### Task 11: Home entry point and Udhaar card

**Files:**
- Modify: `composeApp/.../ui/home/HomeScreen.kt` (~line 82 `onNavigateToLend`), `HomeViewModel.kt` (~269, ~325 receivable totals)
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/home/HomeViewModelTest.kt` (extend)

- [ ] **Step 1: Write the failing test:** with one slip balance of Rs 6,000 and one GIVEN loan of Rs 10,000 and one TAKEN loan of Rs 2,000, the Home state shows `totalMarketReceivablePaisa = 600_000` (slips only), `udhaarGivenTotalPaisa = 1_000_000` and `udhaarTakenTotalPaisa = 200_000`. Apply Open decision 2 first; if Vivek chooses otherwise, adjust the assertion.
- [ ] **Step 2:** Run, expect FAIL.
- [ ] **Step 3: Implement** the two new fields in the Home state (sum `udhaarGivenPaisa` and `udhaarTakenPaisa` across parties) and an "उधार" card on Home with both totals that opens the landing page (`onNavigateToLendList`). Remove the "unlinked lend amount" special case if Task 4 made it dead (`unlinkedLendAmountPaisa`, ~line 325); loans now always link to a party.
- [ ] **Step 4:** Run the test (PASS), then `./gradlew :composeApp:jvmTest`. Device screenshots `t11-home-<light|dark>-390.png`.
- [ ] **Step 5: Commit** `feat(mandisamiti): home udhaar card opens landing page`

---

### Task 12: Sync and backend schema (no deploy)

**Files:**
- Modify: `core-data/.../sync/model/SyncDto.kt` (lend DTO near line 153, cash DTO), `core-data/.../sync/SyncEngine.kt` (lend and cash mapping near lines 91, 194, 336, 504)
- Create: `backend/migrations/versions/0007_udhaar_fields.py` (copy the shape of `0005_loan_interest_rate.py`)
- Modify: backend models and schemas found by `grep -rn "monthly_interest_rate\|source_entry_id" backend/app`
- Test: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/sync/UdhaarWireTest.kt` (copy the style of `PaymentLinkWireTest.kt`), plus a backend test next to the existing ones

- [ ] **Step 1: Write the failing tests.** Wire test: a lend with `direction = TAKEN`, `kind = PESGI`, receiver fields and a cash row with `lendId` survive DTO round trip. A DTO missing the new fields (an old client) decodes with `direction = "GIVEN"`, `kind = "LOAN"`, `receiver_* = null`, `lend_id = null`. Backend: a push of the new fields is stored and returned.
- [ ] **Step 2:** Run, expect FAIL.
- [ ] **Step 3: Implement.** DTO fields: lend gets `direction: String = "GIVEN"`, `kind: String = "LOAN"`, `receiver_name: String? = null`, `receiver_phone: String? = null`; cash gets `lend_id: String? = null`. Map them in `SyncEngine` both ways. Migration `0007`: add the same columns to the lend and cash tables with `server_default` "GIVEN" and "LOAN" (use `op.batch_alter_table`, as in 0005), `down_revision = "0006_shop_profile_address_tax"`. Keep `monthly_interest_rate` default 2.0 on the server (the client always sends it explicitly).
- [ ] **Step 4:** Run client tests (PASS) and the backend tests (see `backend/README.md` for the command). **Do not deploy.** Note in PROGRESS.md that the backend change is pending Vivek's approval.
- [ ] **Step 5: Commit** `feat(mandisamiti): sync udhaar direction, kind, receiver and cash lend link`

---

### Task 13: Cleanup, docs, device sign-off, logging

**Files:**
- Delete when no longer referenced: `ui/lend/LoanAdvanceEntryScreen.kt`, `ui/lend/LendTransactionsListScreen.kt` and their now-dead tests (`LendEntryDirectionBTest`, `LendFlowTest` parts that only test those screens; keep tests of domain behaviour). Keep `LendEntryScreen.kt` and `GrainLendEntry` (grain scope).
- Modify: `docs/product/feature-status.md` (Udhaar row), `docs/decisions/2026-11-11-record-loan-advance.md` (add "Superseded by 2026-11-16 udhaar rewrite" at the top), `PROGRESS.md`, `FEATURES.json`
- Create: `docs/decisions/2026-11-16-udhaar-model.md` (the core rule and the three open-decision outcomes, short)

- [ ] **Step 1:** Run the full suite `./gradlew :core-domain:jvmTest :core-data:jvmTest :composeApp:jvmTest`. Expected: BUILD SUCCESSFUL, 0 failures (rerun the one flaky Home test once if it fails).
- [ ] **Step 2: NGDL Definition of Done (§5.7).** On the emulator, light and dark, desktop and 390dp, check the landing page, add form, person screen, khata and Home card: no horizontal scroll, contrast at least 4.5:1, no emoji, tokens only, controls clicked (writes on a throwaway shop, never on real data). Run the `tests/ngdl-tokens` check the repo defines (see `docs/design-system/DESIGN-SYSTEM.md` §5.7). List anything you could not verify.
- [ ] **Step 3: End-to-end walk on device**, in this order, and write the observed numbers in PROGRESS.md: (1) add a Rs 10,000 loan with interest off; (2) add a pesgi of Rs 3,000 for the same farmer; (3) make a buy slip of Rs 10,000 with a Rs 4,000 cash payment and a Rs 5,000 loan cut; (4) check the slip shows its own payment history and the deduction line; (5) check udhaar shows Rs 8,000 left (10,000 + 3,000 − 5,000) and the ledger tags are right; (6) overpay on the person screen and see the message.
- [ ] **Step 4: Logging (mandatory).** Append a dated session entry to `PROGRESS.md` (AI runtime, changes, concrete evidence with test counts, next steps, and the unverified list). Update `FEATURES.json`: add or update an "Udhaar rewrite" entry with `status` and evidence, and append any new verified check at the end. Write a durable note to `vaults/Vivek-K/wiki/log.md` and refresh `vaults/Vivek-K/wiki/hot.md`, then run `python3 scripts/vault_lint.py` from the repo root (must report 0 errors).
- [ ] **Step 5: Commit** `docs(mandisamiti): udhaar rewrite sign-off and progress log`. Do not push or deploy; ask Vivek first.

---

## Self-review against the spec

| Spec section | Task |
|---|---|
| 2 core rule, slip vs udhaar | 4 (balances), 5 (tags), 10 |
| 3 pesgi, plain cash, loan cut | 3 (`plainCash`, kinds), 9 (routing), 10 |
| 4.1 landing pager | 6 |
| 4.2 add form (interest off, receiver note) | 7 |
| 4.3 person screen, oldest-first, overpayment blocked | 2, 3, 8 |
| 4.4 Kisaan khata, tags | 5, 9 |
| 5 data model (lend link, loans for TAKEN, optional interest, receiver note) | 1, 3, 12 |
| 6 out of scope (grain, aging, receiver list) | Global Constraints, Task 13 keeps grain files |
| 7 open items (backend, NGDL, tests) | 12, 13, tests in 4 and 10 |

Type names used across tasks: `LendDirection`, `LendKind`, `UdhaarLoanForm`, `CashSide`, `PlainCashResult`, `OpenLoan`, `LoanPayment`, `AllocationResult`, `UdhaarOverpaymentException`, `PartyBalance.slipBalancePaisa / udhaarGivenPaisa / udhaarTakenPaisa / udhaarNetPaisa`, `LedgerTag`, `buildUdhaarRows`, `UdhaarPersonRow`. Tasks 6 to 11 depend on 1 to 5 and must run after them. Task 10 depends on Task 3 and Task 4.
