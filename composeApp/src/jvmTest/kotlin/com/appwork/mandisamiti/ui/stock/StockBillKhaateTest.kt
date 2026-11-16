package com.appwork.mandisamiti.ui.stock

import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.model.StockLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StockBillKhaateTest {

    private fun sampleBuy(id: String, crop: String, grams: Long, isKhaate: Boolean = false): BuyEntry {
        return BuyEntry(
            id = id,
            shopId = "shop-1",
            farmerId = "farmer-1",
            commodityId = crop,
            entryDate = 1_000L,
            bagsCount = 1,
            weightPerBagGrams = grams,
            extraWeightGrams = 0L,
            commissionBps = 0L,
            labourPerBagPaisa = 0L,
            advancePaidPaisa = 0L,
            totalWeightGrams = grams,
            ratePaisaPerQuintal = 200_000L,
            grossPaisa = 500_000L,
            commissionPaisa = 0L,
            labourPaisa = 0L,
            netPayablePaisa = 500_000L,
            createdAt = 1_000L,
            updatedAt = 1_000L,
            isKhaate = isKhaate
        )
    }

    private fun sampleSell(id: String, crop: String, grams: Long, isKhaate: Boolean = false): SellEntry {
        return SellEntry(
            id = id,
            shopId = "shop-1",
            buyerId = "buyer-1",
            commodityId = crop,
            entryDate = 1_000L,
            billMode = BillMode.NORMAL,
            partyKind = SellPartyKind.MERCHANT,
            bagsCount = 1,
            weightPerBagGrams = grams,
            aratBps = 0L,
            tulakPaisa = 0L,
            totalWeightGrams = grams,
            ratePaisaPerQuintal = 220_000L,
            grossPaisa = 550_000L,
            aratPaisa = 0L,
            mandiShulkPaisa = 0L,
            totalReceivablePaisa = 550_000L,
            receivedPaisa = 0L,
            balanceDuePaisa = 550_000L,
            createdAt = 1_000L,
            updatedAt = 1_000L,
            isKhaate = isKhaate
        )
    }

    @Test
    fun buildBills_marksKhaateBuysAndSellsCorrectly() {
        val buys = listOf(
            sampleBuy("b1", "गेहूं", 250_000L, isKhaate = false),
            sampleBuy("b2", "गेहूं", 100_000L, isKhaate = true)
        )
        val sells = listOf(
            sampleSell("s1", "गेहूं", 150_000L, isKhaate = false),
            sampleSell("s2", "गेहूं", 50_000L, isKhaate = true)
        )
        val billsByCrop = buildBills(buys, sells, mapOf("farmer-1" to "Ramesh", "buyer-1" to "Suresh"))
        val bills = billsByCrop["गेहूं"] ?: emptyList()

        assertEquals(4, bills.size)
        val b1 = bills.first { it.id == "b1" }
        assertFalse(b1.isKhaate)
        assertTrue(b1.isBuy)

        val b2 = bills.first { it.id == "b2" }
        assertTrue(b2.isKhaate)
        assertTrue(b2.isBuy)

        val s1 = bills.first { it.id == "s1" }
        assertFalse(s1.isKhaate)
        assertFalse(s1.isBuy)

        val s2 = bills.first { it.id == "s2" }
        assertTrue(s2.isKhaate)
        assertFalse(s2.isBuy)
    }

    @Test
    fun stockLine_khaateRowVisibilityCondition() {
        val lineWithoutKhaate = StockLine(
            commodityId = "गेहूं",
            boughtGrams = 500_000L,
            soldGrams = 200_000L,
            khaateBoughtGrams = 0L,
            khaateSoldGrams = 0L
        )
        val showRowWithout = lineWithoutKhaate.khaateBoughtGrams != 0L || lineWithoutKhaate.khaateSoldGrams != 0L
        assertFalse(showRowWithout, "Khaate row should be hidden when no khaate movement")

        val lineWithKhaateBuy = StockLine(
            commodityId = "गेहूं",
            boughtGrams = 500_000L,
            soldGrams = 200_000L,
            khaateBoughtGrams = 100_000L,
            khaateSoldGrams = 0L
        )
        val showRowBuy = lineWithKhaateBuy.khaateBoughtGrams != 0L || lineWithKhaateBuy.khaateSoldGrams != 0L
        assertTrue(showRowBuy, "Khaate row should be shown when khaateBoughtGrams != 0")

        val lineWithKhaateSell = StockLine(
            commodityId = "गेहूं",
            boughtGrams = 500_000L,
            soldGrams = 200_000L,
            khaateBoughtGrams = 0L,
            khaateSoldGrams = 50_000L
        )
        val showRowSell = lineWithKhaateSell.khaateBoughtGrams != 0L || lineWithKhaateSell.khaateSoldGrams != 0L
        assertTrue(showRowSell, "Khaate row should be shown when khaateSoldGrams != 0")
    }
}
