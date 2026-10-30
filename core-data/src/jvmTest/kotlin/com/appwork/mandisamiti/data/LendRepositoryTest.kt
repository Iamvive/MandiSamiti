package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.domain.model.LendStatus
import com.appwork.mandisamiti.domain.model.LendType
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class LendRepositoryTest {
    private val clock = object : Clock {
        override fun now() = Instant.fromEpochMilliseconds(2_000L)
    }

    private fun moneyLend(id: String, amountPaisa: Long) = LendEntry(
        id = id,
        shopId = "shop",
        partyId = "party",
        lendType = LendType.MONEY,
        entryDate = 1_000L,
        amountPaisa = amountPaisa,
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )

    private fun grainLend(id: String, weightGrams: Long) = LendEntry(
        id = id,
        shopId = "shop",
        partyId = "party",
        lendType = LendType.GRAIN,
        entryDate = 1_000L,
        commodityId = "wheat",
        bagsCount = 10L,
        weightGrams = weightGrams,
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )

    private fun settlement(
        id: String,
        lendId: String,
        amountPaisa: Long = 0L,
        weightGrams: Long = 0L,
    ) = LendSettlement(
        id = id,
        shopId = "shop",
        lendId = lendId,
        settledAt = 2_000L,
        amountPaisa = amountPaisa,
        weightGrams = weightGrams,
        createdAt = 2_000L,
        updatedAt = 2_000L,
    )

    private fun seedPartyAndCommodity(db: com.appwork.mandisamiti.database.AppDatabase) {
        db.appDatabaseQueries.insertParty(
            "party", "shop", "Party", null, null, "FARMER", null, null,
            null, null, 1L, 1L, 0L, 0L,
        )
        db.appDatabaseQueries.insertCommodity(
            "wheat", "shop", "गेहूं", "Wheat", "QUINTAL", 1L,
            1L, 1L, 0L, 0L,
        )
    }

    @Test
    fun partialRepaymentMovesToPartial_fullToSettled() = runTest {
        val db = createTestDatabase()
        seedPartyAndCommodity(db)
        val repo = OfflineFirstLendRepository(
            db,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = clock,
        )

        repo.saveLendEntry(moneyLend(id = "l1", amountPaisa = 1_000_000L))
        repo.settle(settlement("s1", "l1", amountPaisa = 400_000L))
        assertEquals(LendStatus.PARTIAL, repo.getLendEntryById("l1")!!.status)
        repo.settle(settlement("s2", "l1", amountPaisa = 600_000L))
        assertEquals(LendStatus.SETTLED, repo.getLendEntryById("l1")!!.status)
        assertEquals(3L, repo.getLendEntryById("l1")!!.revision)
    }

    @Test
    fun grainLendSettlesByWeight() = runTest {
        val db = createTestDatabase()
        seedPartyAndCommodity(db)
        val repo = OfflineFirstLendRepository(
            db,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = clock,
        )

        repo.saveLendEntry(grainLend(id = "l2", weightGrams = 500_000L))
        repo.settle(settlement("s3", "l2", weightGrams = 500_000L))

        assertEquals(LendStatus.SETTLED, repo.getLendEntryById("l2")!!.status)
    }

    @Test
    fun voidingPrincipalRowAlsoVoidsInterestRowAndSettlement() = runTest {
        val db = createTestDatabase()
        seedPartyAndCommodity(db)
        val dispatcher = StandardTestDispatcher(testScheduler)
        val lendRepo = OfflineFirstLendRepository(db, ioDispatcher = dispatcher, clock = clock)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = dispatcher, clock = clock)
        fun cash(id: String, type: TransactionType, paisa: Long) = CashTransaction(
            id = id, shopId = "shop", partyId = "party", transactionType = type, amountPaisa = paisa,
            transactionDate = 2_000L, remarks = "x [SETTLE:s9:l9]", createdAt = 2_000L, updatedAt = 2_000L,
        )

        lendRepo.saveLendEntry(moneyLend(id = "l9", amountPaisa = 1_000_000L))
        lendRepo.settle(
            settlement("s9", "l9", amountPaisa = 120_000L).copy(principalPaisa = 100_000L, interestPaisa = 20_000L),
            listOf(cash("c-principal", TransactionType.JAMA_RECEIVED, 100_000L), cash("c-interest", TransactionType.INTEREST_RECEIVED, 20_000L)),
        )
        assertEquals(LendStatus.PARTIAL, lendRepo.getLendEntryById("l9")!!.status)

        cashRepo.voidTransaction("c-principal", VoidReason.WRONG_ENTRY)

        val q = db.appDatabaseQueries
        assertEquals(1L, q.getCashTransactionById("c-principal").executeAsOne().is_void)
        assertEquals(1L, q.getCashTransactionById("c-interest").executeAsOne().is_void)
        assertEquals(1L, q.getLendSettlementById("s9").executeAsOne().is_void)
        assertEquals(LendStatus.OPEN, lendRepo.getLendEntryById("l9")!!.status)
    }
}

class LendEditTest {
    private val clock = object : Clock { override fun now() = Instant.fromEpochMilliseconds(5_000L) }

    private fun lend(id: String = "l1", type: LendType = LendType.MONEY, amount: Long = 100_000L) = LendEntry(
        id = id, shopId = "shop", partyId = "party", lendType = type, entryDate = 1_000L,
        amountPaisa = if (type == LendType.MONEY) amount else 0L,
        weightGrams = if (type == LendType.GRAIN) 50_000L else 0L,
        createdAt = 1_000L, updatedAt = 1_000L,
    )

    private fun linkedCash(lendId: String, amount: Long) = CashTransaction(
        id = "c-$lendId", shopId = "shop", partyId = "party", transactionType = TransactionType.UDHAR_GIVEN,
        amountPaisa = amount, transactionDate = 1_000L, remarks = "उधार ऋण [LEND:$lendId]",
        createdAt = 1_000L, updatedAt = 1_000L,
    )

    @Test
    fun editUpdatesLendAndLinkedCashRow() = runTest {
        val db = createTestDatabase()
        val lends = OfflineFirstLendRepository(db, clock = clock)
        val cash = OfflineFirstCashTransactionRepository(db, clock = clock)
        lends.saveLendEntry(lend())
        cash.recordTransaction(linkedCash("l1", 100_000L))

        lends.editLendEntry("l1", 150_000L, "खाद")

        val l = lends.getLendEntryById("l1")!!
        assertEquals(150_000L, l.amountPaisa)
        assertEquals("खाद", l.notes)
        assertEquals(2L, l.revision)
        val c = db.appDatabaseQueries.getCashTransactionById("c-l1").executeAsOne()
        assertEquals(150_000L, c.amount_paisa)
        assertEquals("उधार ऋण: खाद [LEND:l1]", c.remarks)
        assertEquals(2L, c.revision)
        assertEquals(0L, c.sync_status)
    }

    @Test
    fun amountLockedOnceRepaid() = runTest {
        val db = createTestDatabase()
        val lends = OfflineFirstLendRepository(db, clock = clock)
        lends.saveLendEntry(lend())
        lends.settle(LendSettlement(id = "s1", shopId = "shop", lendId = "l1", settledAt = 2_000L, amountPaisa = 10_000L, createdAt = 2_000L, updatedAt = 2_000L))
        kotlin.test.assertFailsWith<IllegalArgumentException> { lends.editLendEntry("l1", 150_000L, null) }
        lends.editLendEntry("l1", 100_000L, "note only")
        assertEquals("note only", lends.getLendEntryById("l1")!!.notes)
    }

    @Test
    fun grainLendOnlyEditsNotes() = runTest {
        val db = createTestDatabase()
        val lends = OfflineFirstLendRepository(db, clock = clock)
        lends.saveLendEntry(lend("g1", LendType.GRAIN))
        lends.editLendEntry("g1", 999L, "gehun")
        val g = lends.getLendEntryById("g1")!!
        assertEquals(0L, g.amountPaisa)
        assertEquals("gehun", g.notes)
    }
}
