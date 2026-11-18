package com.appwork.mandisamiti.ui.udhaar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.appwork.mandisamiti.domain.math.MandiCurrencyFormatter
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.usecase.UdhaarOverpaymentException
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun isSameOrAfterDay(
    dateMs: Long,
    minDateMs: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Boolean {
    if (dateMs >= minDateMs) return true
    val minDate = Instant.fromEpochMilliseconds(minDateMs).toLocalDateTime(timeZone).date
    val pickedUtcDate = Instant.fromEpochMilliseconds(dateMs).toLocalDateTime(TimeZone.UTC).date
    val pickedLocalDate = Instant.fromEpochMilliseconds(dateMs).toLocalDateTime(timeZone).date
    return pickedUtcDate >= minDate || pickedLocalDate >= minDate
}

class UdhaarRepayFormState(
    var totalDuePaisa: Long,
    val todayMs: Long,
    val minDateMs: Long? = null,
) {
    var amountRupees by mutableStateOf("")
    var dateMs by mutableStateOf(todayMs)
    private var serverOverpaymentDue by mutableStateOf<Long?>(null)

    val paymentPaisa: Long
        get() = MandiMathEngine.parseRupeesToPaisa(amountRupees)

    val isDateValid: Boolean
        get() = isSameOrPastDay(dateMs, todayMs) && (minDateMs == null || isSameOrAfterDay(dateMs, minDateMs))

    fun onOverpayment(e: UdhaarOverpaymentException) {
        totalDuePaisa = e.totalDuePaisa
        serverOverpaymentDue = e.totalDuePaisa
    }

    fun updateTotalDue(newDuePaisa: Long) {
        totalDuePaisa = newDuePaisa
        serverOverpaymentDue = null
    }

    fun validationReason(isEnglish: Boolean = false): String? {
        val trimmed = amountRupees.trim()
        if (trimmed.isEmpty()) {
            return if (isEnglish) "Enter repayment amount" else "वापसी राशि दर्ज करें"
        }
        val isNumeric = trimmed.count { it == '.' || it == ',' } <= 1 &&
            trimmed.replace(".", "").replace(",", "").all { it.isDigit() }
        if (!isNumeric) {
            return if (isEnglish) "Enter valid amount" else "मान्य राशि दर्ज करें"
        }
        val parsed = paymentPaisa
        if (parsed <= 0L) {
            return if (isEnglish) "Amount must be greater than 0" else "राशि 0 से अधिक होनी चाहिए"
        }
        if (parsed > totalDuePaisa) {
            val dueFormatted = MandiCurrencyFormatter.formatRupees(totalDuePaisa)
            return if (isEnglish) "Only $dueFormatted is due" else "बाकी सिर्फ $dueFormatted है"
        }
        if (!isSameOrPastDay(dateMs, todayMs)) {
            return if (isEnglish) "Date cannot be in the future" else "भविष्य की तारीख अमान्य है"
        }
        if (minDateMs != null && !isSameOrAfterDay(dateMs, minDateMs)) {
            return if (isEnglish) "Date cannot be before the loan date" else "तारीख उधार की तारीख से पहले नहीं हो सकती"
        }
        return null
    }

    val error: String?
        get() = validationReason(false)

    fun error(isEnglish: Boolean): String? = validationReason(isEnglish)

    val isValid: Boolean
        get() = validationReason() == null
}
