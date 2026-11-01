package com.appwork.mandisamiti.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
 * Direction B final date-time format: "31 Oct 2026, 10:42".
 */
fun receiptDateTime(epochMillis: Long): String {
    val value = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    fun Int.twoDigits() = toString().padStart(2, '0')
    return "${value.dayOfMonth} ${shortMonth(value.monthNumber)} ${value.year}, ${value.hour.twoDigits()}:${value.minute.twoDigits()}"
}

/**
 * Direction B final short date format for payment rows: "31 Oct".
 */
fun receiptDateShort(epochMillis: Long): String {
    val value = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${value.dayOfMonth} ${shortMonth(value.monthNumber)}"
}
