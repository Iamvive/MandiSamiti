package com.appwork.mandisamiti.ui.ledger

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the repay dialog split. The interest field holds the interest owed (Rs 220) and does not change
 * with the amount typed; the preview line shows how that payment lands. Seen on emulator 2026-11-14.
 */
class RepayPreviewTest {
    private fun rs(r: Long) = r * 100L

    @Test
    fun partPaymentBelowInterestGoesAllToInterest() {
        val p = repayPreview(rs(100), rs(220), rs(5_000))
        assertEquals(rs(100), p.interestPaisa)
        assertEquals(0L, p.principalPaisa)
        assertEquals(rs(5_000), p.remainingPrincipalPaisa)
        assertEquals(0L, p.generalJamaPaisa)
    }

    @Test
    fun largePaymentClearsInterestThenPrincipal() {
        val p = repayPreview(rs(5_120), rs(120), rs(5_000))
        assertEquals(rs(120), p.interestPaisa)
        assertEquals(rs(5_000), p.principalPaisa)
        assertEquals(0L, p.remainingPrincipalPaisa)
        assertEquals(0L, p.generalJamaPaisa)
    }

    @Test
    fun overpaymentGoesToGeneralJama() {
        val p = repayPreview(rs(6_000), rs(120), rs(5_000))
        assertEquals(rs(880), p.generalJamaPaisa)
    }
}
