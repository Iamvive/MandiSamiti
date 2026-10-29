package com.appwork.mandisamiti.domain.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object LendDateFormatter {

    /**
     * Formats an expected lend return timestamp into a user-friendly string, e.g.:
     * "वापसी: 28 Nov (30 दिन बाकी)" or "वापसी: 28 Oct (1 दिन अतिदेय)" or "वापसी: 29 Oct (आज)"
     */
    fun formatDueDate(
        returnEpochMillis: Long?,
        currentEpochMillis: Long,
        timeZone: TimeZone = TimeZone.UTC
    ): String? {
        if (returnEpochMillis == null || returnEpochMillis <= 0L) return null

        val instant = Instant.fromEpochMilliseconds(returnEpochMillis)
        val dateTime = instant.toLocalDateTime(timeZone)
        val monthName = when (dateTime.monthNumber) {
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
        val dateStr = "${dateTime.dayOfMonth} $monthName"

        val diffMs = returnEpochMillis - currentEpochMillis
        val daysDiff = kotlin.math.round(diffMs.toDouble() / (24.0 * 3600.0 * 1000.0)).toLong()

        val suffix = when {
            daysDiff > 0 -> "($daysDiff दिन बाकी)"
            daysDiff == 0L -> "(आज)"
            else -> "(${kotlin.math.abs(daysDiff)} दिन अतिदेय)"
        }

        return "वापसी: $dateStr $suffix"
    }
}
