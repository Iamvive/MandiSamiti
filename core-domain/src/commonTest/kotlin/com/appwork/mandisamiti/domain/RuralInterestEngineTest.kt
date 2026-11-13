package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import kotlin.test.Test
import kotlin.test.assertEquals

class RuralInterestEngineTest {

    @Test
    fun testAccruedInterestCalculation() {
        // Principal: ₹50,000 = 5,000,000 paise
        // Monthly Rate: 2% (₹2 per ₹100 per month)
        // Duration: 2 months (60 days)
        // Expected Interest = 50,000 * 2% * 2 = ₹2,000 = 200,000 paise

        val principalPaisa = 5_000_000L
        val monthlyRatePercent = 2.0
        val elapsedDays = 60

        val result = RuralInterestEngine.calculateAccruedInterestByDays(
            principalPaisa = principalPaisa,
            monthlyRatePercent = monthlyRatePercent,
            elapsedDays = elapsedDays
        )

        assertEquals(200_000L, result.accruedInterestPaisa)
        assertEquals(5_200_000L, result.totalPayablePaisa)
        assertEquals(60, result.daysElapsed)
        assertEquals(2.0, result.approxMonthsElapsed, 0.01)
    }

    @Test
    fun defaultRateIsTwoPercent() {
        assertEquals(2.0, RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT)
    }

    @Test
    fun oneLoanCanCarryOneAndHalfPercent() {
        // 100,000 paise for 30 days at 1.5% = 1,500 paise
        val c = RuralInterestEngine.calculateAccruedInterestByDays(100_000L, 1.5, 30)
        assertEquals(1_500L, c.accruedInterestPaisa)
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
