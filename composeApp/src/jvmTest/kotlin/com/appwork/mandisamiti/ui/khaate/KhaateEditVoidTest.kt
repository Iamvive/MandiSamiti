package com.appwork.mandisamiti.ui.khaate

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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KhaateEditVoidTest {
    private val shop = "shop"
    private val db = createTestDatabase()
    private val buyRepo = OfflineFirstBuyRepository(db)
    private val sellRepo = OfflineFirstSellRepository(db)
    private val partyRepo = OfflineFirstPartyRepository(db)
    private val stockRepo = OfflineFirstExpenseRepository(db)
    private var seq = 0

    private suspend fun saveKhaateBuy(grams: Long, commodity: String = "गेहूं"): String {
        val id = "bk${seq++}"
        val entry = BuyEntry(
            id = id,
            shopId = shop,
            farmerId = Khaate.partyId(shop),
            commodityId = commodity,
            entryDate = 1_000L,
            bagsCount = grams / 50_000L,
            weightPerBagGrams = 50_000L,
            extraWeightGrams = 0,
            totalWeightGrams = grams,
            ratePaisaPerQuintal = 200_000L,
            commissionBps = 0L,
            labourPerBagPaisa = 0L,
            advancePaidPaisa = 0L,
            grossPaisa = (grams * 200_000L) / 100_000L,
            commissionPaisa = 0L,
            labourPaisa = 0L,
            netPayablePaisa = (grams * 200_000L) / 100_000L,
            paidPaisa = 0L,
            createdAt = 1_000L,
            updatedAt = 1_000L,
            isKhaate = true
        )
        saveKhaateAware(true, shop, "FARMER", partyRepo) {
            buyRepo.saveBuyEntry(entry)
        }
        return id
    }

    private suspend fun saveKhaateSell(grams: Long, commodity: String = "गेहूं"): String {
        val id = "sk${seq++}"
        val entry = SellEntry(
            id = id,
            shopId = shop,
            buyerId = Khaate.partyId(shop),
            commodityId = commodity,
            entryDate = 1_000L,
            billMode = BillMode.NORMAL,
            partyKind = SellPartyKind.MERCHANT,
            bagsCount = grams / 50_000L,
            weightPerBagGrams = 50_000L,
            totalWeightGrams = grams,
            ratePaisaPerQuintal = 220_000L,
            aratBps = 0L,
            tulakPaisa = 0L,
            grossPaisa = (grams * 220_000L) / 100_000L,
            aratPaisa = 0L,
            mandiShulkPaisa = 0L,
            totalReceivablePaisa = (grams * 220_000L) / 100_000L,
            receivedPaisa = 0L,
            balanceDuePaisa = (grams * 220_000L) / 100_000L,
            createdAt = 1_000L,
            updatedAt = 1_000L,
            isKhaate = true
        )
        saveKhaateAware(true, shop, "BUYER", partyRepo) {
            sellRepo.saveSellEntry(entry)
        }
        return id
    }

    private suspend fun editBuy(id: String, transform: (BuyEntry) -> BuyEntry) {
        val current = buyRepo.getBuyEntryById(id) ?: error("Not found: $id")
        buyRepo.editBuyEntry(transform(current))
    }

    private suspend fun editSell(id: String, transform: (SellEntry) -> SellEntry) {
        val current = sellRepo.getSellEntryById(id) ?: error("Not found: $id")
        sellRepo.editSellEntry(transform(current))
    }

    private suspend fun voidBuy(id: String, reason: String = "galat entry") {
        buyRepo.voidBuyEntry(id, VoidReason.WRONG_ENTRY)
    }

    private suspend fun voidSell(id: String, reason: String = "galat entry") {
        sellRepo.voidSellEntry(id, VoidReason.WRONG_ENTRY)
    }

    private suspend fun loadBuy(id: String) = buyRepo.getBuyEntryById(id) ?: error("Not found: $id")
    private suspend fun loadSell(id: String) = sellRepo.getSellEntryById(id) ?: error("Not found: $id")

    private fun revisionsFor(id: String) = db.appDatabaseQueries.getRevisionsForEntry(id).executeAsList().filter { it.change_kind == "EDIT" }

    private suspend fun stockTotals() = stockRepo.stockTotals(shop)

    @Test
    fun editingKhaateBuyWritesRevisionAndMovesKhaateLine() = runTest {
        val id = saveKhaateBuy(grams = 250_000L)
        editBuy(id) { it.copy(totalWeightGrams = 300_000L) }
        assertEquals(2L, loadBuy(id).revision)
        assertEquals(1, revisionsFor(id).size)
        assertEquals("EDIT", revisionsFor(id).first().change_kind)
        assertTrue(revisionsFor(id).first().snapshot_json.contains("\"isKhaate\":true"))
        assertEquals(300_000L, stockTotals().khaateBoughtGramsByCommodity["गेहूं"])
    }

    @Test
    fun voidingKhaateSellRestoresKhaateLine() = runTest {
        saveKhaateBuy(grams = 250_000L)
        val sellId = saveKhaateSell(grams = 100_000L)
        assertEquals(100_000L, stockTotals().khaateSoldGramsByCommodity["गेहूं"])
        voidSell(sellId, reason = "galat entry")
        assertNull(stockTotals().khaateSoldGramsByCommodity["गेहूं"])
    }

    @Test
    fun editKeepsKhaateFlagAndZeroCharges() = runTest {
        val id = saveKhaateBuy(grams = 250_000L)
        editBuy(id) { it.copy(ratePaisaPerQuintal = 250_000L) }
        val e = loadBuy(id)
        assertTrue(e.isKhaate)
        assertEquals(0L, e.commissionPaisa)
        assertEquals(0L, e.labourPaisa)
    }

    @Test
    fun editingKhaateSellMovesKhaateLine() = runTest {
        saveKhaateBuy(grams = 500_000L)
        val sellId = saveKhaateSell(grams = 100_000L)
        editSell(sellId) { it.copy(totalWeightGrams = 150_000L) }
        assertEquals(2L, loadSell(sellId).revision)
        assertEquals(1, revisionsFor(sellId).size)
        assertEquals(150_000L, stockTotals().khaateSoldGramsByCommodity["गेहूं"])
    }
}
