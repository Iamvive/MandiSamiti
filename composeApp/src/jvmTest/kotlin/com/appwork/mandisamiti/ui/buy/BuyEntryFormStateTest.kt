package com.appwork.mandisamiti.ui.buy

import kotlin.test.Test
import kotlin.test.assertEquals

class BuyEntryFormStateTest {
    @Test
    fun previewMatchesTheBuyWorkedExample() {
        val f = BuyEntryFormState().apply {
            bags = "10"; weightPerBagKg = "50"; extraWeightKg = "5"; ratePerQuintal = "2500"
            commissionPercent = "2"; labourPerBag = "5"; advancePaid = "1000"
        }
        assertEquals(1_132_250, f.preview().netPayablePaisa)
    }

    @Test
    fun blankFieldsPreviewAsZeroNotCrash() {
        assertEquals(0, BuyEntryFormState().preview().grossPaisa)
    }
}
