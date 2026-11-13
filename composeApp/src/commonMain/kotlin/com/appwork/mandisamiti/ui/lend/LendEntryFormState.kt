package com.appwork.mandisamiti.ui.lend

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendStatus
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.validation.MandiValidators
import com.appwork.mandisamiti.domain.validation.ValidationResult
import kotlinx.datetime.Clock

/**
 * State and validation for Lend Entry screen adhering to Direction B phone-first rules.
 */
class LendEntryFormState {

    var lendType by mutableStateOf(LendType.MONEY)

    private var _phone by mutableStateOf("")
    val phone: String get() = _phone

    fun updatePhone(input: String) {
        val digits = input.filter { it.isDigit() }.take(10)
        _phone = digits
    }

    var partyName by mutableStateOf("")
    var village by mutableStateOf("")
    var selectedPartyId by mutableStateOf<String?>(null)

    var amountRupees by mutableStateOf("")
    var commodityName by mutableStateOf("")
    var bagsCount by mutableStateOf("")
    var weightKg by mutableStateOf("")

    var selectedDaysChip by mutableStateOf<Int?>(null)
    var expectedReturnMillis by mutableStateOf<Long?>(null)
    var notes by mutableStateOf("")

    var rateInput by mutableStateOf("2")

    /** Null when the text is not a number above zero and at most 100. */
    val parsedRate: Double?
        get() = parseLoanRate(rateInput)

    val phoneValidation: ValidationResult
        get() = MandiValidators.validatePhone(phone, isRequired = true)

    val nameValidation: ValidationResult
        get() = if (partyName.trim().isBlank()) {
            ValidationResult.Invalid("नाम दर्ज करें")
        } else {
            ValidationResult.Valid
        }

    val villageValidation: ValidationResult
        get() = if (MandiValidators.sanitizeVillage(village).isBlank()) {
            ValidationResult.Invalid("गाँव का नाम दर्ज करें")
        } else {
            ValidationResult.Valid
        }

    fun canSave(): Boolean {
        if (phoneValidation !is ValidationResult.Valid) return false
        if (nameValidation !is ValidationResult.Valid) return false
        if (villageValidation !is ValidationResult.Valid) return false

        return if (lendType == LendType.MONEY) {
            amountRupees.isNotBlank() && MandiMathEngine.parseRupeesToPaisa(amountRupees) > 0L &&
                parsedRate != null
        } else {
            commodityName.trim().isNotBlank() && MandiMathEngine.parseKgToGrams(weightKg) > 0L
        }
    }

    fun validationReason(isEnglish: Boolean = false): String {
        if (phone.isBlank()) {
            return if (isEnglish) "Add number to save" else "10 अंकों का नंबर डालें"
        }
        if (phoneValidation !is ValidationResult.Valid) {
            return if (isEnglish) "Enter valid 10-digit number" else "मान्य 10 अंकों का मोबाइल नंबर दर्ज करें"
        }
        if (nameValidation !is ValidationResult.Valid || villageValidation !is ValidationResult.Valid) {
            return if (isEnglish) "Add name and village to save" else "नाम और गांव डालने पर सुरक्षित होगा"
        }
        if (lendType == LendType.MONEY) {
            if (amountRupees.isBlank() || MandiMathEngine.parseRupeesToPaisa(amountRupees) <= 0L) {
                return if (isEnglish) "Enter loan amount (₹)" else "उधार राशि (रु) दर्ज करें"
            }
            if (parsedRate == null) {
                return if (isEnglish) "Enter interest rate (1 to 100)" else "ब्याज दर (1 से 100) दर्ज करें"
            }
        } else {
            if (commodityName.trim().isBlank() || MandiMathEngine.parseKgToGrams(weightKg) <= 0L) {
                return if (isEnglish) "Enter crop and weight" else "जिंस और कुल वजन दर्ज करें"
            }
        }
        return if (isEnglish) "Save Loan / Advance" else "उधार सुरक्षित करें"
    }

    val isDirty: Boolean
        get() = phone.isNotBlank() ||
            partyName.isNotBlank() ||
            village.isNotBlank() ||
            amountRupees.isNotBlank() ||
            commodityName.isNotBlank() ||
            bagsCount.isNotBlank() ||
            weightKg.isNotBlank() ||
            expectedReturnMillis != null ||
            notes.isNotBlank()

    fun reset() {
        lendType = LendType.MONEY
        _phone = ""
        partyName = ""
        village = ""
        selectedPartyId = null
        amountRupees = ""
        commodityName = ""
        bagsCount = ""
        weightKg = ""
        selectedDaysChip = null
        expectedReturnMillis = null
        notes = ""
        rateInput = "2"
    }

    fun toLendEntry(
        shopId: String,
        partyId: String,
        now: Long = Clock.System.now().toEpochMilliseconds(),
        id: String = IdGenerator.newId(),
    ): LendEntry {
        val isMoney = lendType == LendType.MONEY
        return LendEntry(
            id = id,
            shopId = shopId,
            partyId = partyId,
            lendType = lendType,
            entryDate = now,
            amountPaisa = if (isMoney) MandiMathEngine.parseRupeesToPaisa(amountRupees) else 0L,
            commodityId = if (!isMoney) commodityName.trim() else null,
            bagsCount = if (!isMoney) bagsCount.trim().toLongOrNull() ?: 0L else 0L,
            weightGrams = if (!isMoney) MandiMathEngine.parseKgToGrams(weightKg) else 0L,
            expectedReturnDate = expectedReturnMillis,
            notes = notes.trim().ifEmpty { null },
            monthlyRatePercent = if (isMoney) parsedRate ?: RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT
            else RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT,
            status = LendStatus.OPEN,
            createdAt = now,
            updatedAt = now,
        )
    }
}


/** Monthly loan rate typed by hand: decimal comma allowed, above 0 and at most 100, else null. */
fun parseLoanRate(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 && it <= 100.0 }
