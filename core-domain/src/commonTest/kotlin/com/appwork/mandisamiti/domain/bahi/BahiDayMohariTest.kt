package com.appwork.mandisamiti.domain.bahi

import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import kotlin.test.Test
import kotlin.test.assertEquals

class BahiDayMohariTest {
    private fun buy(
        gross: Long,
        isKhaate: Boolean,
        paid: Long = 0L,
        commission: Long = 0L,
        labour: Long = 0L,
        advance: Long = 0L,
        isVoid: Boolean = false,
    ) = BuyEntry(
        id = "b-${gross}-${isKhaate}-${paid}-${isVoid}",
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
        advancePaidPaisa = advance,
        grossPaisa = gross,
        commissionPaisa = commission,
        labourPaisa = labour,
        netPayablePaisa = gross - commission - labour - advance,
        paidPaisa = paid,
        createdAt = 1_000L,
        updatedAt = 1_000L,
        isVoid = isVoid,
        isKhaate = isKhaate,
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
        assertEquals(1_600_000L, page.mohariBuyAsalPaisa)
        assertEquals(1_000_000L - 0L, page.farmerPayablePaisa) // only the normal buy
        assertEquals(0L, page.cashInPaisa)
        assertEquals(0L, page.cashOutPaisa)
    }

    @Test
    fun mohariIncludesAllNonVoidBuysExcludingCommissionAndLabourRegardlessOfPaymentStatus() {
        val page = BahiDayPageBuilder.build(
            dateEpochDay = 1L,
            openingPaisa = 0L,
            closingPaisa = 0L,
            entries = emptyList(),
            buys = listOf(
                buy(gross = 600_000L, isKhaate = true, commission = 10_000L, labour = 5_000L), // 585k
                buy(gross = 1_000_000L, isKhaate = false, paid = 970_000L, commission = 20_000L, labour = 10_000L), // full paid -> 970k
                buy(gross = 500_000L, isKhaate = false, paid = 200_000L, commission = 15_000L, labour = 5_000L), // partial paid -> 480k
                buy(gross = 400_000L, isKhaate = false, paid = 0L, commission = 8_000L, labour = 2_000L, advance = 50_000L), // unpaid with advance -> 390k (advance not subtracted)
                buy(gross = 300_000L, isKhaate = false, paid = 0L, isVoid = true), // voided -> excluded
                buy(gross = 10_000L, isKhaate = false, commission = 8_000L, labour = 5_000L), // negative clamped to 0
            ),
            sells = listOf(sell(gross = 300_000L, isKhaate = true))
        )
        // Khaate buy asal is only khaate gross
        assertEquals(600_000L, page.khaateBuyAsalPaisa)
        // Mohari buy asal sums (gross - commission - labour).coerceAtLeast(0L) across all non-void buys regardless of payment status
        assertEquals(585_000L + 970_000L + 480_000L + 390_000L, page.mohariBuyAsalPaisa)
        // Farmer payable has remaining due on non-khaate buys:
        // full paid: 970k - 970k = 0
        // partial paid: 480k - 200k = 280k
        // unpaid with advance: (390k - 50k) - 0 = 340k
        // negative net: 0
        assertEquals(280_000L + 340_000L, page.farmerPayablePaisa)
    }
}
