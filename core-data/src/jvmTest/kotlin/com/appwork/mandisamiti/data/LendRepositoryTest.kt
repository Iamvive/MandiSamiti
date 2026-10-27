package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
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
            1L, 1L, 0L, 0L,
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
}
