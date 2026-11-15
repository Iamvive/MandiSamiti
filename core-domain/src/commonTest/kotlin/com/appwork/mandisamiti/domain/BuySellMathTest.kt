package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.math.*
import kotlin.test.Test
import kotlin.test.assertEquals

class BuySellMathTest {
    private fun buy(bags: Long, bagKg: Long, extraKg: Long, rateRupees: Long, bps: Long, labourRupees: Long = 10) =
        BuySellMath.calculateBuy(
            BuyMathInput(bags, bagKg * 1000, extraKg * 1000, rateRupees * 100, bps, labourRupees * 100, 0)
        )

    @Test
    fun farmerSlipSheetVectors() {
        // bags, kg/bag, extra kg, rate, bps -> gross, labour, commission, final (rupees)
        val rows = listOf(
            listOf(45L, 49L, 29L, 4001L, 70L, 89_382L, 460L, 622L, 88_300L),
            listOf(31L, 55L, 5L, 2250L, 70L, 38_475L, 310L, 265L, 37_900L),
            listOf(12L, 50L, 41L, 2220L, 100L, 14_230L, 130L, 140L, 13_960L),
        )
        for (r in rows) {
            val m = buy(r[0], r[1], r[2], r[3], r[4])
            assertEquals(r[5] * 100, m.grossPaisa, "gross $r")
            assertEquals(r[6] * 100, m.labourPaisa, "labour $r")
            assertEquals(r[7] * 100, m.commissionPaisa, "commission $r")
            assertEquals(r[8] * 100, m.netPayablePaisa, "final $r")
        }
    }

    @Test
    fun extraWeightAboveTwentyKgAddsABilledBag() {
        // 10 bags, Rs 10 per bag: 4, 12, 20 kg extra are free; 21 kg bills one more bori
        for ((extraKg, bori) in listOf(4L to 10L, 12L to 10L, 20L to 10L, 21L to 11L)) {
            assertEquals(bori * 1_000, buy(10, 50, extraKg, 1000, 0).labourPaisa, "extra $extraKg kg")
        }
    }

    private val normal = com.appwork.mandisamiti.domain.model.BillMode.NORMAL
    private val bos = com.appwork.mandisamiti.domain.model.BillMode.BILL_OF_SUPPLY

    @Test
    fun sellBillsExtraWeight() {
        // 10 bags x 50 kg + 21 kg = 5.21 qtl
        val r = BuySellMath.calculateSell(SellMathInput(10, 50_000, 200_000, normal, 0, null, 0, extraWeightGrams = 21_000))
        assertEquals(521_000, r.totalWeightGrams)
        assertEquals(1_042_000, r.grossPaisa)
    }

    @Test
    fun traderLabourAbsorbsRoundingHalfUp() {
        // 1 qtl at Rs 1,000; Cash Memo tulak Rs 1; labour L paisa. raw total = 100_100 + L
        fun total(l: Long) = BuySellMath.calculateSell(
            SellMathInput(1, 100_000, 100_000, normal, 0, null, 0, sellLabourPaisaPerQuintal = l)
        )
        val down = total(49)
        assertEquals(100_100, down.totalReceivablePaisa)
        assertEquals(0, down.labourPaisa)
        assertEquals(100, down.tulakPaisa)
        val up = total(50)
        assertEquals(100_200, up.totalReceivablePaisa)
        assertEquals(100, up.labourPaisa)
    }

    @Test
    fun traderLabourOnExtraWeightBillOfSupply() {
        // 5.21 qtl, Rs 2,000, labour Rs 5/qtl, arat 1%, BoS: whole-rupee total, labour takes the paise
        val r = BuySellMath.calculateSell(
            SellMathInput(10, 50_000, 200_000, bos, 100, null, 0,
                extraWeightGrams = 21_000, sellLabourPaisaPerQuintal = 500)
        )
        assertEquals(0L, r.totalReceivablePaisa % 100)
        assertEquals(r.grossPaisa + r.aratPaisa + r.tulakPaisa + r.mandiShulkPaisa + r.labourPaisa, r.totalReceivablePaisa)
        assertEquals(521, r.tulakPaisa)
    }

    @Test
    fun noTraderLabourKeepsOldBehaviour() {
        val r = BuySellMath.calculateSell(SellMathInput(8, 49_000, 390_100, normal, 150, null, 0))
        assertEquals(0, r.labourPaisa)
        assertEquals(1_552_500, r.totalReceivablePaisa)
    }

    @Test
    fun finalRoundsToTenRupeesWithTieUp() {
        // gross Rs 1,005, no labour, no commission: tie at Rs 5 rounds up to 1,010; commission absorbs -5
        val up = buy(1, 100, 0, 1005, 0, labourRupees = 0)
        assertEquals(101_000, up.netPayablePaisa)
        assertEquals(-500, up.commissionPaisa)
        assertEquals(100_000, buy(1, 100, 0, 1004, 0, labourRupees = 0).netPayablePaisa)
    }

    @Test
    fun advanceComesOffAfterRounding() {
        val r = BuySellMath.calculateBuy(BuyMathInput(10, 50_000, 0, 250_000, 70, 1_000, 100_000))
        // gross 12,500; labour 100; 0.7% = 87.50; due 12,312.50 -> 12,310; less advance 1,000
        assertEquals(1_231_000 - 100_000, r.netPayablePaisa)
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

    private fun sell(mode: com.appwork.mandisamiti.domain.model.BillMode, bags: Long, kg: Long, rate: Long) =
        BuySellMath.calculateSell(SellMathInput(bags, kg * 1000, rate * 100, mode, 150, null, 0))

    @Test
    fun billOfSupplySheetVector() {
        val r = sell(com.appwork.mandisamiti.domain.model.BillMode.BILL_OF_SUPPLY, 35, 49, 3751)
        assertEquals(6_432_965, r.grossPaisa)
        assertEquals(96_494, r.aratPaisa)
        assertEquals(96_494, r.mandiShulkPaisa)
        assertEquals(1_715, r.tulakPaisa)
        assertEquals(6_627_668, r.totalReceivablePaisa)
    }

    @Test
    fun cashMemoSheetVector() {
        val r = sell(com.appwork.mandisamiti.domain.model.BillMode.NORMAL, 8, 49, 3901)
        assertEquals(1_529_192, r.grossPaisa)
        assertEquals(22_938, r.aratPaisa)
        assertEquals(0, r.mandiShulkPaisa)
        assertEquals(370, r.tulakPaisa)
        assertEquals(1_552_500, r.totalReceivablePaisa)
    }

    @Test
    fun halfUpRoundingOnGross() {
        // 1 g at Rs 500/qtl = 0.5 paisa -> rounds up to 1 paisa
        assertEquals(1, MandiMathEngine.grossAmountPaisa(1, 50_000))
        // buy gross is cut down to the whole rupee, so 1 g is worth nothing
        assertEquals(0, BuySellMath.calculateBuy(BuyMathInput(0, 0, 1, 50_000, 0, 0, 0)).grossPaisa)
    }

    @Test
    fun zeroBagsWithLooseWeightOnly() {
        val r = BuySellMath.calculateBuy(BuyMathInput(0, 50_000, 25_000, 200_000, 0, 500, 0))
        assertEquals(25_000, r.totalWeightGrams)
        assertEquals(50_000, r.grossPaisa)
        assertEquals(500, r.labourPaisa) // 25 kg extra is above 20 kg: one billed bag
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
        assertEquals(-399_900, r.balanceDuePaisa) // Cash Memo adds Rs 1/qtl tulak
    }
}
