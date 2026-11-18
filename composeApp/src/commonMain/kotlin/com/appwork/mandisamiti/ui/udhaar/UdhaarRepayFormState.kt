package com.appwork.mandisamiti.ui.udhaar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.appwork.mandisamiti.domain.math.MandiCurrencyFormatter
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.usecase.UdhaarOverpaymentException
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

fun isSameOrAfterDay(
    dateMs: Long,
    minDateMs: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Boolean {
    val pickedDate = Instant.fromEpochMilliseconds(dateMs).toLocalDateTime(timeZone).date
    val minDate = Instant.fromEpochMilliseconds(minDateMs).toLocalDateTime(timeZone).date
    return pickedDate >= minDate
}

fun normalisePickerDate(
    utcMillis: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Long {
    val date = Instant.fromEpochMilliseconds(utcMillis).toLocalDateTime(TimeZone.UTC).date
    return date.atStartOfDayIn(timeZone).toEpochMilliseconds()
}

fun localInstantToPickerUtc(
    instantMs: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Long {
    val localDate = Instant.fromEpochMilliseconds(instantMs).toLocalDateTime(timeZone).date
    return localDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
}

fun isSameDay(
    dateMs: Long,
    otherMs: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Boolean {
    val d1 = Instant.fromEpochMilliseconds(dateMs).toLocalDateTime(timeZone).date
    val d2 = Instant.fromEpochMilliseconds(otherMs).toLocalDateTime(timeZone).date
    return d1 == d2
}

class UdhaarRepayFormState(
    totalDuePaisa: Long,
    val todayMs: Long,
    minDateMs: Long? = null,
) {
    var totalDuePaisa by mutableStateOf(totalDuePaisa)
    var minDateMs by mutableStateOf(minDateMs)
    var amountRupees by mutableStateOf("")
    var dateMs by mutableStateOf(todayMs)
    private var serverOverpaymentDue by mutableStateOf<Long?>(null)

    val paymentPaisa: Long
        get() = MandiMathEngine.parseRupeesToPaisa(amountRupees)

    val isDateValid: Boolean
        get() {
            val min = minDateMs
            return isSameOrPastDay(dateMs, todayMs) && (min == null || isSameOrAfterDay(dateMs, min))
        }

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
        val min = minDateMs
        if (min != null && !isSameOrAfterDay(dateMs, min)) {
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
