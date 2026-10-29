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
}
