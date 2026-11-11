package com.appwork.mandisamiti.ui.register

import kotlin.test.Test
import kotlin.test.assertEquals

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
        assertEquals(listOf(2000, 500, 200, 100, 50, 20, 10, 5, 2, 1), drawerDenominations)
    }

    @Test
    fun adjustDenominationIncrementsAndDecrements() {
        val initial = mapOf(500 to "2")
        val inc = adjustDenomination(initial, 500, 1)
        assertEquals("3", inc[500])

        val dec = adjustDenomination(inc, 500, -1)
        assertEquals("2", dec[500])

        val zeroed = adjustDenomination(mapOf(500 to "0"), 500, -1)
        assertEquals("0", zeroed[500])
    }
}
