package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.usecase.LendException
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.domain.model.LendStatus
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.VoidReason
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LendEntryRepositoryTest {
    private val fixedNow = Instant.parse("2026-10-08T05:30:00Z")
    private val clock = object : Clock { override fun now() = fixedNow }

    private fun sampleLendEntry(id: String, amountPaisa: Long = 1_000_000L) = LendEntry(
        id = id, shopId = "shop", partyId = "party-1", lendType = LendType.MONEY,
        entryDate = 1_000L, amountPaisa = amountPaisa, createdAt = 1_000L, updatedAt = 1_000L,
    )

    @Test
    fun saveWritesRowAndCreateRevision() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstLendRepository(db, clock = clock)
        repo.saveLendEntry(sampleLendEntry(id = "l1"))
        assertEquals(1, db.appDatabaseQueries.getRevisionsForEntry("l1").executeAsList().size)
        assertEquals("CREATE", db.appDatabaseQueries.getRevisionsForEntry("l1").executeAsOne().change_kind)
        assertEquals("LEND", db.appDatabaseQueries.getRevisionsForEntry("l1").executeAsOne().entry_kind)
        assertEquals(LendStatus.OPEN, repo.getLendEntryById("l1")!!.status)
    }

    @Test
    fun partialRepaymentLeavesLendPartial() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstLendRepository(db, clock = clock)
        repo.saveLendEntry(sampleLendEntry(id = "l1", amountPaisa = 1_000_000L))
        repo.settle(LendSettlement(id = "settle-1", shopId = "shop", lendId = "l1", settledAt = 2_000L, amountPaisa = 400_000L, createdAt = 2_000L, updatedAt = 2_000L))
        assertEquals(LendStatus.PARTIAL, repo.getLendEntryById("l1")!!.status)
        assertEquals(1, repo.getSettlements("l1").size)
    }

    @Test
    fun fullRepaymentSettlesLend() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstLendRepository(db, clock = clock)
        repo.saveLendEntry(sampleLendEntry(id = "l1", amountPaisa = 1_000_000L))
        repo.settle(LendSettlement(id = "settle-1", shopId = "shop", lendId = "l1", settledAt = 2_000L, amountPaisa = 400_000L, createdAt = 2_000L, updatedAt = 2_000L))
        repo.settle(LendSettlement(id = "settle-2", shopId = "shop", lendId = "l1", settledAt = 3_000L, amountPaisa = 600_000L, createdAt = 3_000L, updatedAt = 3_000L))
        assertEquals(LendStatus.SETTLED, repo.getLendEntryById("l1")!!.status)
        assertEquals(3, repo.getLendEntryById("l1")!!.revision)
        assertEquals(2, repo.getSettlements("l1").size)
    }

    @Test
    fun overRepaymentIsRejectedAndWritesNothing() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstLendRepository(db, clock = clock)
        repo.saveLendEntry(sampleLendEntry(id = "l1", amountPaisa = 500_000L))
        val cash = CashTransaction(
            id = "c1", shopId = "shop", partyId = "party", transactionType = TransactionType.JAMA_RECEIVED,
            amountPaisa = 700_000L, paymentMode = PaymentMode.CASH, transactionDate = 2_000L,
            remarks = "x [SETTLE:settle-1:l1]", createdAt = 2_000L, updatedAt = 2_000L,
        )
        assertFailsWith<LendException.OverRepayment> {
            repo.settle(LendSettlement(id = "settle-1", shopId = "shop", lendId = "l1", settledAt = 2_000L, amountPaisa = 700_000L, createdAt = 2_000L, updatedAt = 2_000L), listOf(cash))
        }
        assertEquals(0, repo.getSettlements("l1").size)
        assertNull(db.appDatabaseQueries.getCashTransactionById("c1").executeAsOneOrNull())
        assertEquals(LendStatus.OPEN, repo.getLendEntryById("l1")!!.status)
    }

    @Test
    fun repaymentAfterFullRepaymentIsRejectedAndCashRowIsWrittenWithSettlement() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstLendRepository(db, clock = clock)
        repo.saveLendEntry(sampleLendEntry(id = "l1", amountPaisa = 500_000L))
        fun cash(id: String, paisa: Long) = CashTransaction(
            id = id, shopId = "shop", partyId = "party", transactionType = TransactionType.JAMA_RECEIVED,
            amountPaisa = paisa, paymentMode = PaymentMode.CASH, transactionDate = 2_000L, createdAt = 2_000L, updatedAt = 2_000L,
        )
        fun settle(id: String, paisa: Long) = LendSettlement(id = id, shopId = "shop", lendId = "l1", settledAt = 2_000L, amountPaisa = paisa, createdAt = 2_000L, updatedAt = 2_000L)
        repo.settle(settle("s1", 200_000L), listOf(cash("c1", 200_000L)))
        repo.settle(settle("s2", 300_000L), listOf(cash("c2", 300_000L)))
        assertEquals(LendStatus.SETTLED, repo.getLendEntryById("l1")!!.status)
        assertNotNull(db.appDatabaseQueries.getCashTransactionById("c2").executeAsOneOrNull())
        assertFailsWith<LendException.OverRepayment> { repo.settle(settle("s3", 100L), listOf(cash("c3", 100L))) }
        assertNull(db.appDatabaseQueries.getCashTransactionById("c3").executeAsOneOrNull())
    }

    @Test
    fun voidedLendCannotBeSettled() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstLendRepository(db, clock = clock)
        repo.saveLendEntry(sampleLendEntry(id = "l1"))
        repo.voidLendEntry("l1", VoidReason.WRONG_ENTRY)
        assertFailsWith<IllegalArgumentException> {
            repo.settle(LendSettlement(id = "settle-1", shopId = "shop", lendId = "l1", settledAt = 2_000L, amountPaisa = 100_000L, createdAt = 2_000L, updatedAt = 2_000L))
        }
    }

    @Test
    fun saveLendEntryAutoProvisionsPartyAccountAndReflectsReceivable() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstLendRepository(db, clock = clock)
        val entry = sampleLendEntry("l-unseeded", amountPaisa = 1_000_000L).copy(
            partyId = "कैलाश किसान"
        )
        repo.saveLendEntry(entry)

        val party = db.appDatabaseQueries.getPartyById("कैलाश किसान").executeAsOneOrNull()
        kotlin.test.assertNotNull(party)
        assertEquals("कैलाश किसान", party.name)
        assertEquals("FARMER", party.party_type)

        val balance = db.appDatabaseQueries.getPartyBalance("कैलाश किसान").executeAsOne()
        assertEquals(1_000_000L, balance.balance_paisa)
    }
}
