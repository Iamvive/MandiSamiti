package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.math.*
import kotlin.test.Test
import kotlin.test.assertEquals

class BuySellMathTest {
    @Test
    fun buyWorkedExample() {
        // 10 bags x 50 kg + 5 kg loose = 505 kg; Rs 2,500/qtl; 2% commission; Rs 5/bag labour; Rs 1,000 advance
        val r = BuySellMath.calculateBuy(
            BuyMathInput(
                bags = 10, weightPerBagGrams = 50_000, extraWeightGrams = 5_000,
                ratePaisaPerQuintal = 250_000, commissionBps = 200,
                labourPerBagPaisa = 500, advancePaidPaisa = 100_000,
            )
        )
        assertEquals(505_000, r.totalWeightGrams)
        assertEquals(1_262_500, r.grossPaisa)
        assertEquals(25_250, r.commissionPaisa)
        assertEquals(5_000, r.labourPaisa)
        assertEquals(1_132_250, r.netPayablePaisa)
    }

    @Test
    fun sellBillOfSupplyWorkedExample() {
        // 20 bags x 50 kg = 1000 kg = 10 qtl; Rs 2,600/qtl; Arat 1%; Tulak default Rs 1/qtl; Mandi tax 1.5%; Rs 10,000 received
        val r = BuySellMath.calculateSell(
            SellMathInput(
                bags = 20, weightPerBagGrams = 50_000, ratePaisaPerQuintal = 260_000,
                billMode = com.appwork.mandisamiti.domain.model.BillMode.BILL_OF_SUPPLY,
                aratBps = 100, tulakPaisaOverride = null, receivedPaisa = 1_000_000,
            )
        )
        assertEquals(2_600_000, r.grossPaisa)
        assertEquals(26_000, r.aratPaisa)
        assertEquals(1_000, r.tulakPaisa)
        assertEquals(39_000, r.mandiShulkPaisa)
        assertEquals(2_666_000, r.totalReceivablePaisa)
        assertEquals(1_666_000, r.balanceDuePaisa)
    }

    @Test
    fun normalBillHasNoAdditions() {
        val r = BuySellMath.calculateSell(
            SellMathInput(
                bags = 20, weightPerBagGrams = 50_000, ratePaisaPerQuintal = 260_000,
                billMode = com.appwork.mandisamiti.domain.model.BillMode.NORMAL,
                aratBps = 100, tulakPaisaOverride = null, receivedPaisa = 0,
            )
        )
        assertEquals(2_600_000, r.totalReceivablePaisa)
        assertEquals(0, r.aratPaisa)
        assertEquals(0, r.tulakPaisa)
        assertEquals(0, r.mandiShulkPaisa)
    }

    @Test
    fun halfUpRoundingOnGross() {
        // 1 g at Rs 500/qtl = 0.5 paisa -> rounds up to 1 paisa
        assertEquals(1, MandiMathEngine.grossAmountPaisa(1, 50_000))
        val r = BuySellMath.calculateBuy(BuyMathInput(0, 0, 1, 50_000, 0, 0, 0))
        assertEquals(1, r.grossPaisa)
    }

    @Test
    fun zeroBagsWithLooseWeightOnly() {
        val r = BuySellMath.calculateBuy(BuyMathInput(0, 50_000, 25_000, 200_000, 0, 500, 0))
        assertEquals(25_000, r.totalWeightGrams)
        assertEquals(50_000, r.grossPaisa)
        assertEquals(0, r.labourPaisa)
    }

    @Test
    fun advanceLargerThanDueGivesNegativeNet() {
        val r = BuySellMath.calculateBuy(BuyMathInput(1, 100_000, 0, 100_000, 0, 0, 500_000))
        assertEquals(100_000, r.grossPaisa)
        assertEquals(-400_000, r.netPayablePaisa)
    }

    @Test
    fun commissionRoundsHalfUp() {
        // gross 1_262_500, 1.5% -> 18_937.5 -> 18_938
        assertEquals(18_938, MandiMathEngine.percentageOf(1_262_500, 150))
    }

    @Test
    fun tulakOverrideBeatsDefault() {
        val r = BuySellMath.calculateSell(
            SellMathInput(
                20,
                50_000,
                260_000,
                com.appwork.mandisamiti.domain.model.BillMode.BILL_OF_SUPPLY,
                0,
                5_000,
                0,
            )
        )
        assertEquals(5_000, r.tulakPaisa)
    }

    @Test
    fun kgParsing() {
        assertEquals(50_000, MandiMathEngine.parseKgToGrams("50"))
        assertEquals(50_500, MandiMathEngine.parseKgToGrams("50.5"))
    }

    @Test
    fun sellBalanceNegativeWhenOverReceived() {
        val r = BuySellMath.calculateSell(
            SellMathInput(
                1,
                100_000,
                100_000,
                com.appwork.mandisamiti.domain.model.BillMode.NORMAL,
                0,
                null,
                500_000,
            )
        )
        assertEquals(-400_000, r.balanceDuePaisa)
    }
}
