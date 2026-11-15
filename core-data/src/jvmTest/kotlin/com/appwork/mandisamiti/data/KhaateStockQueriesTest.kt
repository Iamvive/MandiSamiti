package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstExpenseRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstSellRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.Khaate
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.model.VoidReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KhaateStockQueriesTest {
    private val shop = "shop"
    private val db = createTestDatabase()
    private val buyRepo = OfflineFirstBuyRepository(db)
    private val sellRepo = OfflineFirstSellRepository(db)
    private val partyRepo = OfflineFirstPartyRepository(db)
    private val stockRepo = OfflineFirstExpenseRepository(db)
    private var seq = 0

    private suspend fun insertBuy(commodity: String, grams: Long, isKhaate: Boolean): String {
        val id = "b${seq++}"
        buyRepo.saveBuyEntry(
            BuyEntry(
                id = id, shopId = shop, farmerId = if (isKhaate) Khaate.partyId(shop) else "farmer-1",
                commodityId = commodity, entryDate = 1_000L, bagsCount = 1, weightPerBagGrams = grams, extraWeightGrams = 0, commissionBps = 0,
                labourPerBagPaisa = 0, advancePaidPaisa = 0,
                totalWeightGrams = grams, ratePaisaPerQuintal = 250_000, grossPaisa = 0, commissionPaisa = 0,
                labourPaisa = 0, netPayablePaisa = 0, createdAt = 1_000L, updatedAt = 1_000L, isKhaate = isKhaate,
            )
        )
        return id
    }

    private suspend fun insertSell(commodity: String, grams: Long, isKhaate: Boolean) {
        sellRepo.saveSellEntry(
            SellEntry(
                id = "s${seq++}", shopId = shop, buyerId = if (isKhaate) Khaate.partyId(shop) else "buyer-1",
                commodityId = commodity, entryDate = 1_000L, billMode = BillMode.NORMAL,
                partyKind = SellPartyKind.MERCHANT, bagsCount = 1, weightPerBagGrams = grams, aratBps = 0, tulakPaisa = 0,
                totalWeightGrams = grams, ratePaisaPerQuintal = 260_000, grossPaisa = 0, aratPaisa = 0,
                mandiShulkPaisa = 0, totalReceivablePaisa = 0, receivedPaisa = 0, balanceDuePaisa = 0,
                createdAt = 1_000L, updatedAt = 1_000L, isKhaate = isKhaate,
            )
        )
    }

    @Test
    fun khaateRowsLandInKhaateMapsOnly() = runTest {
        insertBuy("गेहूं", 5_000_000L, isKhaate = false)
        insertBuy("गेहूं", 250_000L, isKhaate = true)
        insertSell("गेहूं", 400_000L, isKhaate = true)
        val t = stockRepo.stockTotals(shop)
        assertEquals(5_000_000L, t.boughtGramsByCommodity["गेहूं"])
        assertNull(t.soldGramsByCommodity["गेहूं"])
        assertEquals(250_000L, t.khaateBoughtGramsByCommodity["गेहूं"])
        assertEquals(400_000L, t.khaateSoldGramsByCommodity["गेहूं"])
    }

    @Test
    fun systemPartyNeverListed() = runTest {
        partyRepo.ensureKhaateParty(shop, "FARMER")
        partyRepo.ensureKhaateParty(shop, "FARMER")
        assertEquals(1L, db.appDatabaseQueries.getPartyByIdAny(Khaate.partyId(shop)).executeAsList().size.toLong())
        assertTrue(partyRepo.getPartiesStream(shop).first().none { it.id.startsWith("khaate-") })
    }

    @Test
    fun voidedKhaateRowLeavesTotals() = runTest {
        val id = insertBuy("धान", 100_000L, isKhaate = true)
        buyRepo.voidBuyEntry(id, VoidReason.WRONG_ENTRY)
        assertNull(stockRepo.stockTotals(shop).khaateBoughtGramsByCommodity["धान"])
    }

    @Test
    fun khaateRowsDoNotAffectPartyBalance() = runTest {
        partyRepo.ensureKhaateParty(shop, "FARMER")
        buyRepo.saveBuyEntry(
            BuyEntry(
                id = "bk", shopId = shop, farmerId = Khaate.partyId(shop), commodityId = "c", entryDate = 1L,
                bagsCount = 1, weightPerBagGrams = 1, extraWeightGrams = 0, commissionBps = 0,
                labourPerBagPaisa = 0, advancePaidPaisa = 0, totalWeightGrams = 1, ratePaisaPerQuintal = 1,
                grossPaisa = 900, commissionPaisa = 0, labourPaisa = 0, netPayablePaisa = 900,
                createdAt = 1L, updatedAt = 1L, isKhaate = true,
            )
        )
        val bal = db.appDatabaseQueries.getPartyBalance(Khaate.partyId(shop)).executeAsOne()
        assertEquals(0L, bal.balance_paisa ?: 0L)
    }
}
