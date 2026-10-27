package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstSellRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.model.VoidReason
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SellEntryRepositoryTest {
    private val clock = object : Clock {
        override fun now() = Instant.fromEpochMilliseconds(2_000L)
    }

    private fun sampleSell(id: String = "s1") = SellEntry(
        id = id,
        shopId = "shop",
        buyerId = "buyer",
        commodityId = "wheat",
        entryDate = 1_000L,
        billMode = BillMode.NORMAL,
        partyKind = SellPartyKind.MERCHANT,
        bagsCount = 1L,
        weightPerBagGrams = 100_000L,
        totalWeightGrams = 100_000L,
        ratePaisaPerQuintal = 250_000L,
        aratBps = 0L,
        tulakPaisa = 0L,
        grossPaisa = 250_000L,
        aratPaisa = 0L,
        mandiShulkPaisa = 0L,
        totalReceivablePaisa = 250_000L,
        receivedPaisa = 0L,
        balanceDuePaisa = 250_000L,
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )

    private fun seedBuyerAndCommodity(db: com.appwork.mandisamiti.database.AppDatabase) {
        db.appDatabaseQueries.insertParty(
            "buyer", "shop", "Buyer", null, null, "BUYER", null, null,
            1L, 1L, 0L, 0L,
        )
        db.appDatabaseQueries.insertCommodity(
            "wheat", "shop", "गेहूं", "Wheat", "QUINTAL", 1L,
            1L, 1L, 0L, 0L,
        )
    }

    @Test
    fun voidKeepsRowAndWritesVoidRevision() = runTest {
        val db = createTestDatabase()
        seedBuyerAndCommodity(db)
        val sell = OfflineFirstSellRepository(
            db,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = clock,
        )

        sell.saveSellEntry(sampleSell())
        sell.voidSellEntry("s1", VoidReason.WRONG_ENTRY)

        val row = sell.getSellEntryById("s1")!!
        assertTrue(row.isVoid)
        assertEquals(
            "VOID",
            db.appDatabaseQueries.getRevisionsForEntry("s1").executeAsList().last().change_kind,
        )
    }

    @Test
    fun editingVoidEntryIsRejected() = runTest {
        val db = createTestDatabase()
        seedBuyerAndCommodity(db)
        val sell = OfflineFirstSellRepository(
            db,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = clock,
        )

        sell.saveSellEntry(sampleSell())
        sell.voidSellEntry("s1", VoidReason.WRONG_ENTRY)

        assertFailsWith<IllegalArgumentException> { sell.editSellEntry(sampleSell()) }
    }
}
