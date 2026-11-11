package com.appwork.mandisamiti.ui.home

import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.PaymentStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeViewModelSettlementTest {

    @Test
    fun testPaymentStatusCalculation() {
        val pendingBuy = BuyEntry(
            id = "b1",
            shopId = "s1",
            farmerId = "f1",
            commodityId = "Wheat",
            entryDate = 1000L,
            bagsCount = 10,
            weightPerBagGrams = 50000,
            extraWeightGrams = 0,
            totalWeightGrams = 500000,
            ratePaisaPerQuintal = 200000,
            commissionBps = 0,
            labourPerBagPaisa = 0,
            advancePaidPaisa = 0,
            grossPaisa = 1000000,
            commissionPaisa = 0,
            labourPaisa = 0,
            netPayablePaisa = 1000000, // Rs 10,000
            paidPaisa = 0L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        assertEquals(PaymentStatus.PENDING, pendingBuy.paymentStatus)

        val partialBuy = pendingBuy.copy(paidPaisa = 400000L) // Rs 4,000 paid
        assertEquals(PaymentStatus.PARTIAL, partialBuy.paymentStatus)

        val completedBuy = pendingBuy.copy(paidPaisa = 1000000L) // Rs 10,000 paid
        assertEquals(PaymentStatus.COMPLETED, completedBuy.paymentStatus)
    }
}
