package com.appwork.mandisamiti.ui.register

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DayClosingPresentationTest {
    @Test
    fun countedLessThanExpectedShowsShortage() {
        assertEquals("गिनती: ₹10,000 · कमी ₹200", dayClosingCountLabel(1_000_000L, 1_020_000L, false))
    }

    @Test
    fun countedMoreThanExpectedShowsSurplus() {
        assertEquals("गिनती: ₹10,200 · अधिक ₹200", dayClosingCountLabel(1_020_000L, 1_000_000L, false))
    }

    @Test
    fun equalCountShowsBalanced() {
        assertEquals("गिनती: ₹10,000 · मिलान सही", dayClosingCountLabel(1_000_000L, 1_000_000L, false))
    }

    @Test
    fun drawerDenominationsIncludes2000() {
        assertEquals(2000, drawerDenominations.first())
        assertEquals(listOf(2000, 500, 200, 100, 50, 20, 10), drawerDenominations)
    }

    @Test
    fun gapBelowTenRupeesNeedsNoReason() {
        assertFalse(closeNeedsReason(-999L))
        assertFalse(closeNeedsReason(999L))
        assertFalse(closeNeedsReason(0L))
        assertFalse(closeNeedsReason(null))
    }

    @Test
    fun gapOfTenRupeesOrMoreNeedsReason() {
        assertTrue(closeNeedsReason(-1_000L))
        assertTrue(closeNeedsReason(1_000L))
        assertTrue(closeNeedsReason(-157_500L))
    }
}
