package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstExpenseRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstSellRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.model.VoidReason
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class StockQueryTest {
    private val clock = object : Clock {
        override fun now() = Instant.fromEpochMilliseconds(2_000L)
    }

    private fun sampleBuy(id: String, commodity: String, grams: Long) = BuyEntry(
        id = id,
        shopId = "shop",
        farmerId = "farmer",
        commodityId = commodity,
        entryDate = 1_000L,
        bagsCount = 1L,
        weightPerBagGrams = grams,
        extraWeightGrams = 0L,
        totalWeightGrams = grams,
        ratePaisaPerQuintal = 250_000L,
        commissionBps = 0L,
        labourPerBagPaisa = 0L,
        advancePaidPaisa = 0L,
        grossPaisa = 250_000L,
        commissionPaisa = 0L,
        labourPaisa = 0L,
        netPayablePaisa = 250_000L,
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )

    private fun sampleSell(id: String, commodity: String, grams: Long) = SellEntry(
        id = id,
        shopId = "shop",
        buyerId = "buyer",
        commodityId = commodity,
        entryDate = 1_000L,
        billMode = BillMode.NORMAL,
        partyKind = SellPartyKind.MERCHANT,
        bagsCount = 1L,
        weightPerBagGrams = grams,
        totalWeightGrams = grams,
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

    private fun seedPartiesAndCommodity(db: com.appwork.mandisamiti.database.AppDatabase) {
        db.appDatabaseQueries.insertParty(
            "farmer", "shop", "Farmer", null, null, "FARMER", null, null,
            1L, 1L, 0L, 0L,
        )
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
    fun stockExcludesVoidedAndDeletedRows() = runTest {
        val db = createTestDatabase()
        seedPartiesAndCommodity(db)
        val dispatcher = StandardTestDispatcher(testScheduler)
        val buy = OfflineFirstBuyRepository(db, ioDispatcher = dispatcher, clock = clock)
        val sell = OfflineFirstSellRepository(db, ioDispatcher = dispatcher, clock = clock)
        val expense = OfflineFirstExpenseRepository(db, ioDispatcher = dispatcher, clock = clock)

        buy.saveBuyEntry(sampleBuy("b1", commodity = "wheat", grams = 1_000_000L))
        buy.saveBuyEntry(sampleBuy("b2", commodity = "wheat", grams = 500_000L))
        buy.voidBuyEntry("b2", VoidReason.WRONG_ENTRY)
        sell.saveSellEntry(sampleSell("s1", commodity = "wheat", grams = 300_000L))

        val totals = expense.stockTotals("shop")
        assertEquals(1_000_000L, totals.boughtGramsByCommodity["wheat"])
        assertEquals(300_000L, totals.soldGramsByCommodity["wheat"])
    }
}
