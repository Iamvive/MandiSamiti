package com.appwork.mandisamiti.domain.model

import com.appwork.mandisamiti.domain.util.MandiTimeFormatter

/**
 * Direction B final date-time format: "31 Oct 2026, 10:42 AM" (12-hour format).
 */
fun receiptDateTime(epochMillis: Long): String {
    return MandiTimeFormatter.formatDateTime12Hour(epochMillis)
}

/**
 * Direction B final short date format for payment rows: "31 Oct".
 */
fun receiptDateShort(epochMillis: Long): String {
    return MandiTimeFormatter.formatDateShort(epochMillis)
}
