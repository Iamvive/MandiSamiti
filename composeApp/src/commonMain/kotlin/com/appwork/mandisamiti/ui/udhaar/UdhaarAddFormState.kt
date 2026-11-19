package com.appwork.mandisamiti.ui.udhaar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.domain.model.LendKind
import com.appwork.mandisamiti.domain.usecase.UdhaarLoanForm
import com.appwork.mandisamiti.ui.lend.parseLoanRate
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

const val MAX_UDHAAR_RUPEES = 99_999_999L

internal fun isSameOrPastDay(
    dateMs: Long,
    todayMs: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Boolean {
    val pickedDate = Instant.fromEpochMilliseconds(dateMs).toLocalDateTime(timeZone).date
    val todayDate = Instant.fromEpochMilliseconds(todayMs).toLocalDateTime(timeZone).date
    return pickedDate <= todayDate
}

class UdhaarAddFormState(
    val direction: LendDirection,
    val todayMs: Long,
) {
    var kind by mutableStateOf(LendKind.LOAN)
    var phone by mutableStateOf("")

    fun updatePhone(input: String) {
        val digits = input.filter { it.isDigit() }.take(10)
        phone = digits
    }

    var name by mutableStateOf("")
    var selectedPartyId by mutableStateOf<String?>(null)
    var amountRupees by mutableStateOf("")
    var dateMs by mutableStateOf(todayMs)
    var interestOn by mutableStateOf(false)
    var rateInput by mutableStateOf("2")
    var giveToOther by mutableStateOf(false)
    var receiverName by mutableStateOf("")
    var receiverPhone by mutableStateOf("")

    fun updateReceiverPhone(input: String) {
        val digits = input.filter { it.isDigit() }.take(10)
        receiverPhone = digits
    }

    val parsedRate: Double?
        get() = parseLoanRate(rateInput)

    val isPhoneValid: Boolean
        get() = phone.length == 10 && phone.all { it.isDigit() }

    val isNameValid: Boolean
        get() = name.trim().isNotBlank()

    val parsedAmountRupees: Long?
        get() {
            val trimmed = amountRupees.trim()
            if (trimmed.isEmpty() || !trimmed.all { it.isDigit() }) return null
            val value = trimmed.toLongOrNull() ?: return null
            return if (value in 1L..MAX_UDHAAR_RUPEES) value else null
        }

    val isAmountValid: Boolean
        get() = parsedAmountRupees != null

    val isDateValid: Boolean
        get() = isSameOrPastDay(dateMs, todayMs)

    val isRateValid: Boolean
        get() = !interestOn || parsedRate != null

    val isReceiverValid: Boolean
        get() {
            if (direction != LendDirection.GIVEN || !giveToOther) return true
            val nameOk = receiverName.trim().isNotBlank()
            val phoneOk = receiverPhone.isBlank() || (receiverPhone.length == 10 && receiverPhone.all { it.isDigit() })
            return nameOk && phoneOk
        }

    val isValid: Boolean
        get() = isPhoneValid && isNameValid && isAmountValid && isDateValid && isRateValid && isReceiverValid

    fun validationReason(isEnglish: Boolean = false): String? {
        if (!isPhoneValid) {
            return if (isEnglish) "Enter valid 10-digit mobile number" else "10 अंकों का मान्य मोबाइल नंबर दर्ज करें"
        }
        if (!isNameValid) {
            return if (isEnglish) "Enter name" else "नाम दर्ज करें"
        }
        val trimmedAmount = amountRupees.trim()
        if (trimmedAmount.isEmpty()) {
            return if (isEnglish) "Enter loan amount" else "उधार राशि दर्ज करें"
        }
        if (!trimmedAmount.all { it.isDigit() }) {
            return if (isEnglish) "Enter whole rupees only (no decimals)" else "केवल पूर्ण रुपये दर्ज करें (दशमलव नहीं)"
        }
        val amountVal = trimmedAmount.toLongOrNull() ?: 0L
        if (amountVal <= 0L) {
            return if (isEnglish) "Amount must be greater than 0" else "राशि 0 से अधिक होनी चाहिए"
        }
        if (amountVal > MAX_UDHAAR_RUPEES) {
            return if (isEnglish) "Amount cannot exceed ₹9,99,99,999" else "राशि ₹9,99,99,999 से अधिक नहीं हो सकती"
        }
        if (!isDateValid) {
            return if (isEnglish) "Date cannot be in the future" else "भविष्य की तारीख अमान्य है"
        }
        if (interestOn && parsedRate == null) {
            return if (isEnglish) "Enter valid interest rate (above 0 up to 100)" else "मान्य ब्याज दर दर्ज करें (0 से अधिक और 100 तक)"
        }
        if (direction == LendDirection.GIVEN && giveToOther) {
            if (receiverName.trim().isBlank()) {
                return if (isEnglish) "Enter recipient name" else "पैसा लेने वाले का नाम दर्ज करें"
            }
            if (receiverPhone.isNotBlank() && (receiverPhone.length != 10 || !receiverPhone.all { it.isDigit() })) {
                return if (isEnglish) "Enter valid 10-digit recipient phone" else "पैसा लेने वाले का 10 अंकों का नंबर दर्ज करें"
            }
        }
        return null
    }

    fun toForm(shopId: String, partyId: String): UdhaarLoanForm {
        val amountWhole = parsedAmountRupees ?: 0L
        val effectiveRate = if (interestOn) parsedRate ?: 0.0 else 0.0
        val isGivenWithOther = direction == LendDirection.GIVEN && giveToOther
        return UdhaarLoanForm(
            shopId = shopId,
            partyId = partyId,
            direction = direction,
            amountPaisa = amountWhole * 100L,
            date = dateMs,
            kind = if (direction == LendDirection.GIVEN) kind else LendKind.LOAN,
            monthlyRatePercent = effectiveRate,
            receiverName = if (isGivenWithOther) receiverName.trim().takeIf { it.isNotEmpty() } else null,
            receiverPhone = if (isGivenWithOther) receiverPhone.trim().takeIf { it.isNotEmpty() } else null,
        )
    }
}
