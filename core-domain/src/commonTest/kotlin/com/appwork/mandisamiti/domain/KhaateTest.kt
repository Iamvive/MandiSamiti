package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.model.Khaate
import kotlin.test.Test
import kotlin.test.assertEquals

class KhaateTest {
    @Test fun partyIdIsStablePerShop() {
        assertEquals("khaate-shop1", Khaate.partyId("shop1"))
        assertEquals(Khaate.partyId("shop1"), Khaate.partyId("shop1"))
    }

    @Test fun asalIsWeightTimesRateOnly() {
        // 5 bags x 50 kg = 250 kg = 2.5 quintal at Rs 2,400/qtl = Rs 6,000
        assertEquals(600_000L, Khaate.asalPaisa(totalWeightGrams = 250_000L, ratePaisaPerQuintal = 240_000L))
    }

    @Test fun asalRoundsHalfUpToPaisa() {
        // 1 kg at Rs 2,401/qtl = Rs 24.01 -> 2401 paisa
        assertEquals(2_401L, Khaate.asalPaisa(1_000L, 240_100L))
    }
}
