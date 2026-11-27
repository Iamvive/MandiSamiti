package com.appwork.mandisamiti.domain.util

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class MandiTimeFormatterTest {

    private val ist = TimeZone.of("Asia/Kolkata")
    private val utc = TimeZone.UTC

    @Test
    fun testFormatTime12Hour_am_pm_and_padding() {
        // 2026-11-27 00:05:00 UTC = 1795737900000L
        // Hour 0 should format as 12:05 AM
        assertEquals("12:05 AM", MandiTimeFormatter.formatTime12Hour(1795737900000L, utc))

        // 2026-11-27 09:30:00 UTC = 1795771800000L
        // Hour 9 should format as 09:30 AM
        assertEquals("09:30 AM", MandiTimeFormatter.formatTime12Hour(1795771800000L, utc))

        // 2026-11-27 12:00:00 UTC = 1795780800000L
        // Hour 12 should format as 12:00 PM
        assertEquals("12:00 PM", MandiTimeFormatter.formatTime12Hour(1795780800000L, utc))

        // 2026-11-27 15:45:00 UTC = 1795794300000L
        // Hour 15 should format as 03:45 PM
        assertEquals("03:45 PM", MandiTimeFormatter.formatTime12Hour(1795794300000L, utc))

        // 2026-11-27 23:59:00 UTC = 1795823940000L
        // Hour 23 should format as 11:59 PM
        assertEquals("11:59 PM", MandiTimeFormatter.formatTime12Hour(1795823940000L, utc))
    }

    @Test
    fun testFormatDateTime12Hour() {
        // 2026-10-31 10:42:00 UTC
        val ms = 1793443320000L
        assertEquals("31 Oct 2026, 10:42 AM", MandiTimeFormatter.formatDateTime12Hour(ms, utc))
    }

    @Test
    fun testFormatHtmlDateTime12Hour() {
        // 2026-10-31 14:05:00 UTC
        val ms = 1793455500000L
        assertEquals("31/10/2026 02:05 PM", MandiTimeFormatter.formatHtmlDateTime12Hour(ms, utc))
    }

    @Test
    fun testFormatDateShort() {
        val ms = 1793443320000L
        assertEquals("31 Oct", MandiTimeFormatter.formatDateShort(ms, utc))
    }
}
