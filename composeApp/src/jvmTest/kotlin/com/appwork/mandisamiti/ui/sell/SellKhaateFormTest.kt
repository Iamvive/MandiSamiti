package com.appwork.mandisamiti.ui.sell

import com.appwork.mandisamiti.domain.model.Khaate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SellKhaateFormTest {
    private fun form(khaate: Boolean) = SellEntryFormState().apply {
        isKhaate = khaate
        lots[0].bags = "8"; lots[0].weightPerBagKg = "50"; lots[0].ratePerQuintal = "2400"
    }

    @Test fun khaateSellOverStockWarnsButIsValid() {
        val s = form(true).apply { khaateOnHandGrams = 250_000L }
        assertTrue(s.khaateShortWarning)
        assertTrue(s.canSave())
    }

    @Test fun khaateSellWithEnoughStockDoesNotWarn() {
        assertFalse(form(true).apply { khaateOnHandGrams = 400_000L }.khaateShortWarning)
    }

    @Test fun normalSellNeverShowsKhaateWarning() {
        assertFalse(form(false).apply { khaateOnHandGrams = 0L }.khaateShortWarning)
    }

    @Test fun khaateSellHasNoChargesAndNoParty() {
        val e = form(true).apply { commissionPercent = "5"; advanceReceived = "500" }.toSellEntry(shopId = "s")
        assertTrue(e.isKhaate)
        assertEquals(Khaate.partyId("s"), e.buyerId)
        assertEquals(0L, e.aratPaisa); assertEquals(0L, e.tulakPaisa); assertEquals(0L, e.mandiShulkPaisa)
        assertEquals(0L, e.receivedPaisa)
        assertEquals(960_000L, e.grossPaisa); assertEquals(960_000L, e.totalReceivablePaisa)
    }
}
