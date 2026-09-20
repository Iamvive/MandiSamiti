package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import kotlin.test.Test
import kotlin.test.assertEquals

class RuralInterestEngineTest {

    @Test
    fun testAccruedInterestCalculation() {
        // Principal: ₹50,000 = 5,000,000 paise
        // Monthly Rate: 1.5% (₹1.50 per ₹100 per month)
        // Duration: 2 months (60 days)
        // Expected Interest = 50,000 * 1.5% * 2 = ₹1,500 = 150,000 paise

        val principalPaisa = 5_000_000L
        val monthlyRatePercent = 1.5
        val elapsedDays = 60

        val result = RuralInterestEngine.calculateAccruedInterestByDays(
            principalPaisa = principalPaisa,
            monthlyRatePercent = monthlyRatePercent,
            elapsedDays = elapsedDays
        )

        assertEquals(150_000L, result.accruedInterestPaisa)
        assertEquals(5_150_000L, result.totalPayablePaisa)
        assertEquals(60, result.daysElapsed)
        assertEquals(2.0, result.approxMonthsElapsed, 0.01)
    }

    @Test
    fun testZeroDaysInterest() {
        val result = RuralInterestEngine.calculateAccruedInterestByDays(
            principalPaisa = 1_000_000L,
            monthlyRatePercent = 2.0,
            elapsedDays = 0
        )
        assertEquals(0L, result.accruedInterestPaisa)
        assertEquals(1_000_000L, result.totalPayablePaisa)
    }
}
