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
        val s = form(true).apply { khaateOnHandByCrop = mapOf("गेहूं" to 250_000L) }
        assertTrue(s.khaateShortWarning)
        assertTrue(s.canSave())
    }

    @Test fun khaateSellWithEnoughStockDoesNotWarn() {
        assertFalse(form(true).apply { khaateOnHandByCrop = mapOf("गेहूं" to 400_000L) }.khaateShortWarning)
    }

    @Test fun normalSellNeverShowsKhaateWarning() {
        assertFalse(form(false).apply { khaateOnHandByCrop = emptyMap() }.khaateShortWarning)
    }

    @Test fun khaateSellHasNoChargesAndNoParty() {
        val e = form(true).apply { commissionPercent = "5"; advanceReceived = "500" }.toSellEntry(shopId = "s")
        assertTrue(e.isKhaate)
        assertEquals(Khaate.partyId("s"), e.buyerId)
        assertEquals(0L, e.aratPaisa); assertEquals(0L, e.tulakPaisa); assertEquals(0L, e.mandiShulkPaisa)
        assertEquals(0L, e.receivedPaisa)
        assertEquals(960_000L, e.grossPaisa); assertEquals(960_000L, e.totalReceivablePaisa)
    }

    @Test fun surplusInOneCropDoesNotHideShortageInAnother() {
        val s = form(true).apply {
            addLot("धान")
            lots[1].bags = "8"; lots[1].weightPerBagKg = "50"; lots[1].ratePerQuintal = "2000"
            khaateOnHandByCrop = mapOf("गेहूं" to 900_000L, "धान" to 100_000L)
        }
        assertTrue(s.khaateShortWarning)
    }

    @Test fun editingKhaateSaleAddsBackItsOwnGrams() {
        val saved = form(true).toSellEntry(shopId = "s")
        val edit = SellEntryFormState().apply {
            populateFrom(saved)
            khaateOnHandByCrop = mapOf("गेहूं" to 0L) // pile already has this sale subtracted
        }
        assertTrue(edit.isKhaate)
        assertFalse(edit.khaateShortWarning)
    }
}
