package com.appwork.mandisamiti.domain.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Common 12-hour time formatting utility for MandiSamiti UI and slips.
 * Formats timestamps into 12-hour AM/PM representations across all UI surfaces.
 */
object MandiTimeFormatter {

    private fun shortMonth(monthNumber: Int): String = when (monthNumber) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> ""
    }

    /**
     * Formats epoch milliseconds into a 12-hour time string: "hh:mm AM/PM".
     * Example: 14:05 -> "02:05 PM", 09:30 -> "09:30 AM", 00:00 -> "12:00 AM", 12:00 -> "12:00 PM".
     */
    fun formatTime12Hour(
        epochMillis: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        val ldt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
        val hour12 = when (val h = ldt.hour % 12) {
            0 -> 12
            else -> h
        }
        val hourStr = hour12.toString().padStart(2, '0')
        val minuteStr = ldt.minute.toString().padStart(2, '0')
        val amPm = if (ldt.hour < 12) "AM" else "PM"
        return "$hourStr:$minuteStr $amPm"
    }

    /**
     * Formats epoch milliseconds into date with 12-hour time: "31 Oct 2026, 02:30 PM".
     */
    fun formatDateTime12Hour(
        epochMillis: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        val ldt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
        val timePart = formatTime12Hour(epochMillis, timeZone)
        return "${ldt.dayOfMonth} ${shortMonth(ldt.monthNumber)} ${ldt.year}, $timePart"
    }

    /**
     * Formats epoch milliseconds into short date: "31 Oct".
     */
    fun formatDateShort(
        epochMillis: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        val ldt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
        return "${ldt.dayOfMonth} ${shortMonth(ldt.monthNumber)}"
    }

    /**
     * Formats epoch milliseconds into numeric date with 12-hour time: "31/10/2026 02:30 PM".
     */
    fun formatHtmlDateTime12Hour(
        epochMillis: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        val ldt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
        val day = ldt.dayOfMonth.toString().padStart(2, '0')
        val month = ldt.monthNumber.toString().padStart(2, '0')
        val year = ldt.year
        val timePart = formatTime12Hour(epochMillis, timeZone)
        return "$day/$month/$year $timePart"
    }
}
