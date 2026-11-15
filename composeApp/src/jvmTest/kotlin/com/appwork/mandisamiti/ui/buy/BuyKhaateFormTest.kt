package com.appwork.mandisamiti.ui.buy

import com.appwork.mandisamiti.domain.model.Khaate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuyKhaateFormTest {
    private fun khaateForm() = BuyEntryFormState().apply {
        isKhaate = true
        bags = "5"; weightPerBagKg = "50"; ratePerQuintal = "2400"
    }

    @Test fun khaateBuyHasNoChargesAndNoParty() {
        val e = khaateForm().toBuyEntry(shopId = "s")
        assertTrue(e.isKhaate)
        assertEquals(Khaate.partyId("s"), e.farmerId)
        assertEquals(0L, e.commissionPaisa); assertEquals(0L, e.labourPaisa)
        assertEquals(600_000L, e.grossPaisa); assertEquals(600_000L, e.netPayablePaisa)
        assertEquals(0L, e.paidPaisa); assertEquals(0L, e.advancePaidPaisa)
    }

    @Test fun khaateBuyIgnoresTypedCommissionLabourAndAdvance() {
        val f = khaateForm().apply { commissionPercent = "5"; labourPerBag = "20"; advancePaid = "1000" }
        val e = f.toBuyEntry(shopId = "s")
        assertEquals(0L, e.commissionPaisa); assertEquals(0L, e.labourPaisa)
        assertEquals(0L, e.paidPaisa); assertEquals(e.grossPaisa, e.netPayablePaisa)
    }

    @Test fun khaateBuySavesWithoutPartyDetails() {
        assertTrue(khaateForm().canSave())
        assertFalse(BuyEntryFormState().apply { bags = "5"; ratePerQuintal = "2400" }.canSave())
    }

    @Test fun uiStateCarriesKhaateFlag() {
        assertTrue(khaateForm().toUiState().isKhaate)
        assertEquals(600_000L, khaateForm().toUiState().payoutPaisa)
    }
}
