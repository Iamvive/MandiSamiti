package com.appwork.mandisamiti.domain.bahi

import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import kotlin.test.Test
import kotlin.test.assertEquals

class BahiDayMohariTest {
    private fun buy(gross: Long, isKhaate: Boolean, paid: Long = 0L) = BuyEntry(
        id = "b-${gross}-${isKhaate}",
        shopId = "s",
        farmerId = "f",
        commodityId = "c",
        entryDate = 1_000L,
        bagsCount = 1,
        weightPerBagGrams = 50_000L,
        extraWeightGrams = 0L,
        totalWeightGrams = 50_000L,
        ratePaisaPerQuintal = 200_000L,
        commissionBps = 0L,
        labourPerBagPaisa = 0L,
        advancePaidPaisa = 0L,
        grossPaisa = gross,
        commissionPaisa = 0L,
        labourPaisa = 0L,
        netPayablePaisa = gross,
        paidPaisa = paid,
        createdAt = 1_000L,
        updatedAt = 1_000L,
        isKhaate = isKhaate
    )

    private fun sell(gross: Long, isKhaate: Boolean) = SellEntry(
        id = "s-${gross}-${isKhaate}",
        shopId = "s",
        buyerId = "b",
        commodityId = "c",
        entryDate = 1_000L,
        billMode = BillMode.NORMAL,
        partyKind = SellPartyKind.MERCHANT,
        bagsCount = 1,
        weightPerBagGrams = 50_000L,
        aratBps = 0L,
        tulakPaisa = 0L,
        totalWeightGrams = 50_000L,
        ratePaisaPerQuintal = 200_000L,
        grossPaisa = gross,
        aratPaisa = 0L,
        mandiShulkPaisa = 0L,
        totalReceivablePaisa = gross,
        receivedPaisa = 0L,
        balanceDuePaisa = gross,
        createdAt = 1_000L,
        updatedAt = 1_000L,
        isKhaate = isKhaate
    )

    @Test
    fun khaateGoesToMohariNotPayableAndNotCash() {
        val page = BahiDayPageBuilder.build(
            dateEpochDay = 1L,
            openingPaisa = 0L,
            closingPaisa = 0L,
            entries = emptyList(),
            buys = listOf(buy(gross = 600_000L, isKhaate = true), buy(gross = 1_000_000L, isKhaate = false, paid = 0L)),
            sells = listOf(sell(gross = 300_000L, isKhaate = true))
        )
        assertEquals(600_000L, page.khaateBuyAsalPaisa)
        assertEquals(300_000L, page.khaateSellAsalPaisa)
        assertEquals(1_000_000L - 0L, page.farmerPayablePaisa) // only the normal buy
        assertEquals(0L, page.cashInPaisa)
        assertEquals(0L, page.cashOutPaisa)
    }
}
