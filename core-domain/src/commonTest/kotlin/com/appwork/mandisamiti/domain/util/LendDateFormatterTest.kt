package com.appwork.mandisamiti.domain.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LendDateFormatterTest {

    private val baseNow = Instant.parse("2026-10-29T12:00:00Z").toEpochMilliseconds()
    private val utc = TimeZone.UTC

    @Test
    fun formatsNullOrZeroAsNull() {
        assertNull(LendDateFormatter.formatDueDate(null, baseNow))
        assertNull(LendDateFormatter.formatDueDate(0L, baseNow))
        assertNull(LendDateFormatter.formatDueDate(-100L, baseNow))
    }

    @Test
    fun formatsFutureDateWithRemainingDays() {
        // 30 days ahead: 2026-11-28
        val returnMillis = baseNow + 30L * 24 * 3600 * 1000L
        val formatted = LendDateFormatter.formatDueDate(returnMillis, baseNow, utc)
        assertEquals("वापसी: 28 Nov (30 दिन बाकी)", formatted)
    }

    @Test
    fun formats15DaysAhead() {
        val returnMillis = baseNow + 15L * 24 * 3600 * 1000L
        val formatted = LendDateFormatter.formatDueDate(returnMillis, baseNow, utc)
        assertEquals("वापसी: 13 Nov (15 दिन बाकी)", formatted)
    }

    @Test
    fun formatsOverdueDate() {
        // 2 days ago: 2026-10-27
        val returnMillis = baseNow - 2L * 24 * 3600 * 1000L
        val formatted = LendDateFormatter.formatDueDate(returnMillis, baseNow, utc)
        assertEquals("वापसी: 27 Oct (2 दिन अतिदेय)", formatted)
    }

    @Test
    fun formatsTodayDate() {
        val returnMillis = baseNow + 1000L // same day
        val formatted = LendDateFormatter.formatDueDate(returnMillis, baseNow, utc)
        assertEquals("वापसी: 29 Oct (आज)", formatted)
    }
}
