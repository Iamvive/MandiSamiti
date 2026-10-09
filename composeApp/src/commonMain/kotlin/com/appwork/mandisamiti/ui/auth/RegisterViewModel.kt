package com.appwork.mandisamiti.ui.auth

import com.appwork.mandisamiti.data.auth.AuthError
import com.appwork.mandisamiti.data.auth.AuthRepository
import com.appwork.mandisamiti.data.auth.OtpVerifyResult
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

enum class AuthStep {
    PHONE,
    OTP,
    NEW_SHOP,
    ENTER_MPIN
}

data class RegisterUiState(
    val step: AuthStep = AuthStep.PHONE,
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
                (phoneNumber.startsWith("6") || phoneNumber.startsWith("7") || phoneNumber.startsWith("8") || phoneNumber.startsWith("9"))

    val isNewShopValid: Boolean
        get() = shopName.trim().length >= 3 && ownerName.trim().length >= 2 && isMpinValid

    val isEnterMpinValid: Boolean
        get() = mpin.length == 4

    val isOtpValid: Boolean
        get() = otp.length == 6

    val isMpinValid: Boolean
        get() = mpin.length == 4 && confirmMpin.length == 4 && mpin == confirmMpin
}

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val ttsManager: SoundboxTtsManager,
    private val viewModelScope: CoroutineScope,
    private val clock: Clock = Clock.System
) {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var lastClickTime = Long.MIN_VALUE
    private var cooldownJob: Job? = null
    private var signupPass: String? = null
    private var loginPass: String? = null

    // Click Debouncing / Throttling (Prevents duplicate requests within 600ms)
    private fun isClickThrottled(): Boolean {
        val now = clock.now().toEpochMilliseconds()
        if (lastClickTime != Long.MIN_VALUE && now - lastClickTime < 600L) {
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
            submitOtp()
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

    /** PHONE: validate the number, then ask the server to send an OTP. */
    fun submitPhone() {
        if (isClickThrottled()) return
        val s = _uiState.value
        if (s.isLoading) return
        if (!s.isStep1Valid) {
            _uiState.update {
                it.copy(
                    generalErrorMessage = "कृपया सभी आवश्यक विवरण सही से भरें",
                    phoneError = "10 अंकों का सही मोबाइल नंबर भरें"
                )
            }
            return
        }
        requestOtp(s.phoneNumber, moveToOtpStep = true)
    }

    fun resendOtp() {
        if (isClickThrottled()) return
        val s = _uiState.value
        if (s.isLoading || !s.isResendEnabled) return
        requestOtp(s.phoneNumber, moveToOtpStep = false)
    }

    private fun requestOtp(phone: String, moveToOtpStep: Boolean) {
        _uiState.update { it.copy(isLoading = true, generalErrorMessage = null) }
        viewModelScope.launch {
            authRepository.sendOtp(phone).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = if (moveToOtpStep) AuthStep.OTP else it.step,
                            otp = "",
                            otpError = null
                        )
                    }
                    startResendCooldownTimer(30)
                },
                onFailure = { fail(it) }
            )
        }
    }

    private fun startResendCooldownTimer(seconds: Int) {
        cooldownJob?.cancel()
        _uiState.update { it.copy(resendCooldownSeconds = seconds, isResendEnabled = false) }
        cooldownJob = viewModelScope.launch {
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

    /** OTP: 6 digits, then verify. NEW shop -> NEW_SHOP, existing shop -> ENTER_MPIN. */
    fun submitOtp() {
        if (isClickThrottled()) return
        val s = _uiState.value
        if (s.isLoading) return
        if (s.otp.length != 6) {
            _uiState.update { it.copy(otpError = "कृपया 6 अंकों का OTP दर्ज करें") }
            return
        }
        _uiState.update { it.copy(isLoading = true, generalErrorMessage = null, otpError = null) }
        viewModelScope.launch {
            authRepository.verifyOtp(s.phoneNumber, s.otp).fold(
                onSuccess = { result ->
                    cooldownJob?.cancel()
                    when (result) {
                        is OtpVerifyResult.NewShop -> {
                            signupPass = result.signupPass
                            loginPass = null
                            _uiState.update { it.copy(isLoading = false, step = AuthStep.NEW_SHOP, mpin = "", confirmMpin = "") }
                        }
                        is OtpVerifyResult.ExistingShop -> {
                            loginPass = result.loginPass
                            signupPass = null
                            _uiState.update { it.copy(isLoading = false, step = AuthStep.ENTER_MPIN, mpin = "", confirmMpin = "") }
                        }
                    }
                },
                onFailure = { fail(it) }
            )
        }
    }

    /** NEW_SHOP: validate shop details + MPIN pair, then sign up. */
    fun submitNewShop() {
        if (isClickThrottled()) return
        val s = _uiState.value
        if (s.isLoading) return
        val pass = signupPass
        if (pass == null) {
            resetToPhone("समय समाप्त — दोबारा OTP लें")
            return
        }
        if (!s.isNewShopValid) {
            _uiState.update {
                it.copy(
                    generalErrorMessage = "कृपया सभी आवश्यक विवरण सही से भरें और 4 अंकों का MPIN दर्ज करके पुष्टि करें",
                    shopNameError = if (s.shopName.trim().length < 3) "फर्म का नाम दर्ज करें" else null,
                    ownerNameError = if (s.ownerName.trim().length < 2) "व्यापारी का नाम दर्ज करें" else null,
                    confirmMpinError = if (s.mpin != s.confirmMpin) "MPIN मेल नहीं खा रहा है" else null
                )
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, generalErrorMessage = null) }
        viewModelScope.launch {
            authRepository.signup(pass, s.shopName.trim(), s.ownerName.trim(), s.mandiName.trim(), s.mpin).fold(
                onSuccess = {
                    signupPass = null
                    ttsManager.speak("नमस्ते ${s.ownerName} जी, आपकी फर्म ${s.shopName} का पंजीयन सफल रहा।", isSoundEnabled = true)
                    _uiState.update { it.copy(isLoading = false, isRegistrationComplete = true) }
                },
                onFailure = { fail(it) }
            )
        }
    }

    /** ENTER_MPIN: 4 digits, then log in with the pass from OTP verification. */
    fun submitMpin() {
        if (isClickThrottled()) return
        val s = _uiState.value
        if (s.isLoading) return
        val pass = loginPass
        if (pass == null) {
            resetToPhone("बहुत गलत MPIN — दोबारा OTP लें")
            return
        }
        if (!s.isEnterMpinValid) {
            _uiState.update { it.copy(generalErrorMessage = "कृपया 4 अंकों का MPIN दर्ज करें") }
            return
        }
        _uiState.update { it.copy(isLoading = true, generalErrorMessage = null) }
        viewModelScope.launch {
            authRepository.login(pass, s.mpin).fold(
                onSuccess = {
                    loginPass = null
                    _uiState.update { it.copy(isLoading = false, isRegistrationComplete = true) }
                },
                onFailure = { fail(it) }
            )
        }
    }

    private fun fail(error: Throwable) {
        val step = _uiState.value.step
        when (error) {
            is AuthError.RateLimited ->
                showError("बहुत बार OTP माँगा गया, 5 मिनट बाद कोशिश करें")
            is AuthError.OtpInvalid ->
                showError("OTP गलत है")
            is AuthError.MpinInvalid -> {
                _uiState.update { it.copy(mpin = "") }
                showError("MPIN गलत है — ${error.attemptsLeft} कोशिश बाकी")
            }
            is AuthError.PassBurned ->
                resetToPhone(if (step == AuthStep.NEW_SHOP) "समय समाप्त — दोबारा OTP लें" else "बहुत गलत MPIN — दोबारा OTP लें")
            is AuthError.PhoneAlreadyRegistered ->
                resetToPhone("यह नंबर पहले से रजिस्टर है — दोबारा OTP लेकर लॉगिन करें")
            else ->
                showError("नेटवर्क नहीं है — दोबारा कोशिश करें")
        }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(isLoading = false, generalErrorMessage = message) }
    }

    private fun resetToPhone(message: String?) {
        cooldownJob?.cancel()
        signupPass = null
        loginPass = null
        _uiState.update {
            it.copy(
                step = AuthStep.PHONE,
                otp = "",
                mpin = "",
                confirmMpin = "",
                isLoading = false,
                resendCooldownSeconds = 0,
                isResendEnabled = true,
                generalErrorMessage = message
            )
        }
    }

    fun goBackToPhone() = resetToPhone(null)
}
