package com.appwork.mandisamiti.domain.validation

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val errorMessage: String) : ValidationResult

    val isValid: Boolean get() = this is Valid
    val errorOrNull: String? get() = (this as? Invalid)?.errorMessage
}

object MandiValidators {
    private val INDIAN_MOBILE_REGEX = Regex("^[6-9]\\d{9}$")

    fun validateFarmerName(name: String): ValidationResult {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult.Invalid("किसान का नाम दर्ज करें")
            trimmed.length < 2 -> ValidationResult.Invalid("नाम कम से कम 2 अक्षरों का होना चाहिए")
            else -> ValidationResult.Valid
        }
    }

    fun validateBuyerName(name: String, isMerchant: Boolean): ValidationResult {
        val trimmed = name.trim()
        val actor = if (isMerchant) "व्यापारी" else "ग्राहक"
        return when {
            trimmed.isEmpty() -> ValidationResult.Invalid("$actor का नाम दर्ज करें")
            trimmed.length < 2 -> ValidationResult.Invalid("नाम कम से कम 2 अक्षरों का होना चाहिए")
            else -> ValidationResult.Valid
        }
    }

    fun validateGstin(gstin: String, isRequired: Boolean): ValidationResult {
        val clean = gstin.trim().uppercase()
        if (clean.isEmpty()) {
            return if (isRequired) ValidationResult.Invalid("व्यापारी GSTIN दर्ज करें") else ValidationResult.Valid
        }
        val gstinRegex = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
        return if (clean.length == 15 && gstinRegex.matches(clean)) {
            ValidationResult.Valid
        } else if (clean.length == 15) {
            ValidationResult.Valid // Accept standard 15-char if format slight variant
        } else {
            ValidationResult.Invalid("GSTIN 15 अक्षरों का होना चाहिए (जैसे 23AAAAA0000A1Z5)")
        }
    }

    fun validateStateCode(stateCode: String, isRequired: Boolean): ValidationResult {
        val clean = stateCode.trim()
        if (clean.isEmpty()) {
            return if (isRequired) ValidationResult.Invalid("राज्य कोड (State Code) दर्ज करें") else ValidationResult.Valid
        }
        return if (clean.length in 1..2 && clean.all { it.isDigit() }) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("राज्य कोड 2 अंकों का होना चाहिए (उदा. 23)")
        }
    }

    fun validatePhone(phone: String, isRequired: Boolean = false): ValidationResult {
        val clean = phone.trim().filter { it.isDigit() }
        if (clean.isEmpty()) {
            return if (isRequired) ValidationResult.Invalid("मोबाइल नंबर दर्ज करें") else ValidationResult.Valid
        }
        return if (clean.length == 10 && INDIAN_MOBILE_REGEX.matches(clean)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("मोबाइल नंबर 10 अंकों का होना चाहिए (6-9 से शुरू)")
        }
    }

    fun validatePositiveNumber(input: String, fieldName: String): ValidationResult {
        val num = input.toDoubleOrNull()
        return when {
            input.isBlank() -> ValidationResult.Invalid("$fieldName दर्ज करें")
            num == null || num <= 0.0 -> ValidationResult.Invalid("$fieldName शून्य से अधिक होना चाहिए")
            else -> ValidationResult.Valid
        }
    }

    const val RATE_OUTLIER_THRESHOLD_PAISA: Long = 20_000_00L // ₹20,000 / quintal
    const val MIN_RATE_SANITY_PAISA: Long = 500_00L // ₹500 / quintal
    const val MAX_BAGS_SANITY_LIMIT: Long = 2_000L
    const val MIN_WEIGHT_PER_BAG_KG: Double = 20.0
    const val MAX_WEIGHT_PER_BAG_KG: Double = 120.0

    fun sanitizeVillage(input: String): String {
        return input.replace(Regex("[0-9].*"), "").trim()
    }

    fun isRateOutlier(ratePaisa: Long): Boolean {
        return ratePaisa < MIN_RATE_SANITY_PAISA || ratePaisa > RATE_OUTLIER_THRESHOLD_PAISA
    }

    fun isBagsOutlier(bags: Long): Boolean {
        return bags > MAX_BAGS_SANITY_LIMIT
    }

    fun isWeightPerBagOutlier(weightKg: Double): Boolean {
        return weightKg < MIN_WEIGHT_PER_BAG_KG || weightKg > MAX_WEIGHT_PER_BAG_KG
    }

    fun validateNonNegativeNumber(input: String, fieldName: String): ValidationResult {
        if (input.isBlank()) return ValidationResult.Valid
        val num = input.toDoubleOrNull()
        return when {
            num == null || num < 0.0 -> ValidationResult.Invalid("$fieldName ऋणात्मक नहीं हो सकता")
            else -> ValidationResult.Valid
        }
    }
}
