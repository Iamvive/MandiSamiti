package com.appwork.mandisamiti.ui.sell

import com.appwork.mandisamiti.domain.model.BillMode
import kotlin.test.Test
import kotlin.test.assertEquals

class SellEntryFormStateTest {
    @Test
    fun previewMatchesTheBillOfSupplyWorkedExample() {
        val f = SellEntryFormState().apply {
            billMode = BillMode.BILL_OF_SUPPLY
            bags = "20"
            weightPerBagKg = "50"
            ratePerQuintal = "2600"
            aratPercent = "1"
            received = "10000"
        }
        assertEquals(2_666_000, f.preview().totalReceivablePaisa)
        assertEquals(1_666_000, f.preview().balanceDuePaisa)
    }

    @Test
    fun blankTulakUsesDefault() {
        val f = SellEntryFormState().apply {
            billMode = BillMode.BILL_OF_SUPPLY
            bags = "20"
            weightPerBagKg = "50"
            ratePerQuintal = "2600"
        }
        assertEquals(1_000, f.preview().tulakPaisa)
    }
}
