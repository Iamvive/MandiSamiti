package com.appwork.mandisamiti.ui.slip

import kotlin.test.Test
import kotlin.test.assertEquals

class SlipEquationTest {
    @Test
    fun wholeBagsNoExtra() {
        assertEquals(
            "100 × 50 + 0 = 50.00 क्विंटल × ₹2,400",
            slipEquation(bags = 100, weightPerBagGrams = 50_000, totalGrams = 5_000_000, ratePaisaPerQuintal = 240_000),
        )
    }

    @Test
    fun extraWeightAndFractionalKg() {
        // 10 x 50 kg = 500 kg, total 512.5 kg -> extra 12.5 kg, 5.125 qtl
        assertEquals(
            "10 × 50 + 12.5 = 5.13 क्विंटल × ₹2,150.50",
            slipEquation(bags = 10, weightPerBagGrams = 50_000, totalGrams = 512_500, ratePaisaPerQuintal = 215_050),
        )
    }

    @Test
    fun extraFlooredAtZeroWhenTotalBelowBags() {
        assertEquals(
            "10 × 50 + 0 = 4.90 क्विंटल × ₹2,000",
            slipEquation(bags = 10, weightPerBagGrams = 50_000, totalGrams = 490_000, ratePaisaPerQuintal = 200_000),
        )
    }
}
