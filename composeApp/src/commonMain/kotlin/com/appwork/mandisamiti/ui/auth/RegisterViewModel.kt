package com.appwork.mandisamiti.ui.auth

import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.repository.ShopProfileRepository
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthStep {
    PHONE_AND_SHOP,
    OTP_VERIFICATION,
    MPIN_SETUP
}

data class RegisterUiState(
    val step: AuthStep = AuthStep.PHONE_AND_SHOP,
    val phoneNumber: String = "",
    val shopName: String = "",
    val ownerName: String = "",
    val mandiName: String = "",
    val otp: String = "",
    val mpin: String = "",
    val confirmMpin: String = "",
    
    // Field-level Validation Errors
    val phoneError: String? = null,
    val shopNameError: String? = null,
    val ownerNameError: String? = null,
    val otpError: String? = null,
    val mpinError: String? = null,
    val confirmMpinError: String? = null,
    val generalErrorMessage: String? = null,
    
    // Throttling & Cooldowns
    val resendCooldownSeconds: Int = 0,
    val isResendEnabled: Boolean = true,
    
    // Progress Status
    val isLoading: Boolean = false,
    val isRegistrationComplete: Boolean = false
) {
    val isStep1Valid: Boolean
        get() = phoneNumber.length == 10 &&
                (phoneNumber.startsWith("6") || phoneNumber.startsWith("7") || phoneNumber.startsWith("8") || phoneNumber.startsWith("9")) &&
                shopName.trim().length >= 3 &&
                ownerName.trim().length >= 2

    val isOtpValid: Boolean
        get() = otp.length == 6

    val isMpinValid: Boolean
        get() = mpin.length == 4 && confirmMpin.length == 4 && mpin == confirmMpin
}

class RegisterViewModel(
    private val shopProfileRepository: ShopProfileRepository,
    private val ttsManager: SoundboxTtsManager,
    private val viewModelScope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var lastClickTime = 0L
    private var cooldownJob: Job? = null

    // Click Debouncing / Throttling (Prevents duplicate requests within 600ms)
    private fun isClickThrottled(): Boolean {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        if (now - lastClickTime < 600L) {
            return true
        }
        lastClickTime = now
        return false
    }

    fun onPhoneNumberChanged(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(10)
        val error = when {
            digitsOnly.isNotEmpty() && !digitsOnly.startsWith("6") && !digitsOnly.startsWith("7") && !digitsOnly.startsWith("8") && !digitsOnly.startsWith("9") ->
                "मोबाइल नंबर 6, 7, 8 या 9 से शुरू होना चाहिए"
            digitsOnly.length in 1..9 ->
                "10 अंकों का नंबर दर्ज करें (${digitsOnly.length}/10)"
            else -> null
        }
        _uiState.update { it.copy(phoneNumber = digitsOnly, phoneError = error, generalErrorMessage = null) }
    }

    fun onShopNameChanged(value: String) {
        val error = if (value.isNotBlank() && value.trim().length < 3) "दुकान / फर्म का नाम कम से कम 3 अक्षरों का हो" else null
        _uiState.update { it.copy(shopName = value, shopNameError = error, generalErrorMessage = null) }
    }

    fun onOwnerNameChanged(value: String) {
        val error = if (value.isNotBlank() && value.trim().length < 2) "व्यापारी का नाम कम से कम 2 अक्षरों का हो" else null
        _uiState.update { it.copy(ownerName = value, ownerNameError = error, generalErrorMessage = null) }
    }

    fun onMandiNameChanged(value: String) {
        _uiState.update { it.copy(mandiName = value, generalErrorMessage = null) }
    }

    fun onOtpChanged(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(6)
        val error = if (digitsOnly.isNotEmpty() && digitsOnly.length < 6) "6-अंकीय कोड पूरा दर्ज करें (${digitsOnly.length}/6)" else null
        _uiState.update { it.copy(otp = digitsOnly, otpError = error, generalErrorMessage = null) }
        
        // Auto-advance when 6 digits are typed
        if (digitsOnly.length == 6) {
            verifyOtp()
        }
    }

    fun onMpinChanged(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(4)
        _uiState.update { s ->
            val confirmErr = if (s.confirmMpin.isNotEmpty() && digitsOnly != s.confirmMpin) "MPIN मेल नहीं खा रहा है" else null
            s.copy(mpin = digitsOnly, confirmMpinError = confirmErr, generalErrorMessage = null)
        }
    }

    fun onConfirmMpinChanged(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(4)
        _uiState.update { s ->
            val confirmErr = if (digitsOnly.isNotEmpty() && digitsOnly != s.mpin) "MPIN मेल नहीं खा रहा है" else null
            s.copy(confirmMpin = digitsOnly, confirmMpinError = confirmErr, generalErrorMessage = null)
        }
    }

    fun proceedToOtp() {
        if (isClickThrottled()) return

        val s = _uiState.value
        if (!s.isStep1Valid) {
            _uiState.update {
                it.copy(
                    generalErrorMessage = "कृपया सभी आवश्यक विवरण सही से भरें",
                    phoneError = if (s.phoneNumber.length != 10) "10 अंकों का सही मोबाइल नंबर भरें" else null,
                    shopNameError = if (s.shopName.trim().length < 3) "फर्म का नाम दर्ज करें" else null,
                    ownerNameError = if (s.ownerName.trim().length < 2) "व्यापारी का नाम दर्ज करें" else null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                step = AuthStep.OTP_VERIFICATION,
                otp = "",
                generalErrorMessage = null
            )
        }

        startResendCooldownTimer(30)
    }

    fun resendOtp() {
        if (isClickThrottled()) return
        if (!_uiState.value.isResendEnabled) return

        startResendCooldownTimer(30)
        _uiState.update { it.copy(generalErrorMessage = null, otp = "") }
    }

    private fun startResendCooldownTimer(seconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _uiState.update { it.copy(resendCooldownSeconds = seconds, isResendEnabled = false) }
            for (remaining in (seconds - 1) downTo 0) {
                delay(1000)
                _uiState.update {
                    it.copy(
                        resendCooldownSeconds = remaining,
                        isResendEnabled = remaining == 0
                    )
                }
            }
        }
    }

    fun verifyOtp() {
        if (isClickThrottled()) return

        val s = _uiState.value
        if (s.otp.length != 6) {
            _uiState.update { it.copy(otpError = "कृपया 6 अंकों का OTP दर्ज करें") }
            return
        }

        // Advance to MPIN step
        _uiState.update {
            it.copy(
                step = AuthStep.MPIN_SETUP,
                generalErrorMessage = null,
                otpError = null
            )
        }
    }

    fun completeRegistration() {
        if (isClickThrottled()) return

        val s = _uiState.value
        if (!s.isMpinValid) {
            _uiState.update {
                it.copy(
                    generalErrorMessage = "कृपया 4 अंकों का सुरक्षा MPIN दर्ज करें और पुष्टि करें",
                    confirmMpinError = if (s.mpin != s.confirmMpin) "MPIN मेल नहीं खा रहा है" else null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val now = 1000L
            val profile = ShopProfile(
                id = "shop_default",
                shopName = s.shopName.trim(),
                ownerName = s.ownerName.trim(),
                mandiName = s.mandiName.trim().ifBlank { "मथुरा कृषि उपज मंडी" },
                shopNumber = "A-1",
                phoneNumber = s.phoneNumber.trim(),
                pinHash = s.mpin,
                defaultMonthlyInterestRate = 1.5,
                isSoundEnabled = true,
                createdAt = now,
                updatedAt = now,
                syncStatus = 0
            )
            shopProfileRepository.saveShopProfile(profile)
            ttsManager.speak("नमस्ते ${s.ownerName} जी, आपकी फर्म ${s.shopName} का पंजीयन सफल रहा।", isSoundEnabled = true)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRegistrationComplete = true
                )
            }
        }
    }

    fun goBackToDetails() {
        _uiState.update { it.copy(step = AuthStep.PHONE_AND_SHOP, generalErrorMessage = null) }
    }

    fun goBackToOtp() {
        _uiState.update { it.copy(step = AuthStep.OTP_VERIFICATION, generalErrorMessage = null) }
    }
}
