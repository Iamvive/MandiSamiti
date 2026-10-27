package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.LendEntry
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
    fun voidedLendCannotBeSettled() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstLendRepository(db, clock = clock)
        repo.saveLendEntry(sampleLendEntry(id = "l1"))
        repo.voidLendEntry("l1", VoidReason.WRONG_ENTRY)
        assertFailsWith<IllegalArgumentException> {
            repo.settle(LendSettlement(id = "settle-1", shopId = "shop", lendId = "l1", settledAt = 2_000L, amountPaisa = 100_000L, createdAt = 2_000L, updatedAt = 2_000L))
        }
    }
}
