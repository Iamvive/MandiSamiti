package com.appwork.mandisamiti.ui.buy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuyLabSlipTest {
    private fun labForm(lab: String, base: String) = BuyEntryFormState().apply {
        commissionPercent = "2"
        labourPerBag = "5"
        lots.clear()
        lots.add(
            BuyLotItemState(initialRatePerQuintal = "2500").apply {
                labMode = true
                labValueQuintals = lab
                baseValueQuintals = base
            }
        )
    }

    @Test
    fun labAndBaseAreEnteredInQuintalsAndWeightIsTheLabValue() {
        // 45 qtl = 4,500,000 g; weight = lab / base * base, so base does not change it.
        assertEquals(4_500_000L, labForm("45", "50").aggregatePreview().totalWeightGrams)
    }

    @Test
    fun labSlipCommissionAndLabourAreNeverNegative() {
        val p = labForm("45", "50").aggregatePreview()
        assertTrue(p.commissionPaisa >= 0L, "commission was ${p.commissionPaisa}")
        assertTrue(p.labourPaisa >= 0L, "labour was ${p.labourPaisa}")
    }

    @Test
    fun labSlipHasNoBagLabour() {
        // Lab slip has no bags, so majoori is 0 (only the lot's bag count drives labour).
        assertEquals(0L, labForm("45", "50").aggregatePreview().labourPaisa)
    }
}
