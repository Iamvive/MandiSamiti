package com.appwork.mandisamiti.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun receiptDateTime(epochMillis: Long): String {
    val value = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    fun Int.twoDigits() = toString().padStart(2, '0')
    return "${value.dayOfMonth.twoDigits()}/${value.monthNumber.twoDigits()}/${value.year} ${value.hour.twoDigits()}:${value.minute.twoDigits()}"
}
