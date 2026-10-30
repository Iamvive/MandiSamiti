package com.appwork.mandisamiti.domain.model

/** Last 10 digits of an Indian mobile number, or null if fewer than 10 digits. */
fun normalizePhone(raw: String?): String? {
    val digits = raw?.filter { it.isDigit() } ?: return null
    return if (digits.length < 10) null else digits.takeLast(10)
}
