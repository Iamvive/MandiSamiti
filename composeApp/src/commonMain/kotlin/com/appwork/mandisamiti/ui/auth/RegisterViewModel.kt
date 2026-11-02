package com.appwork.mandisamiti.ui.auth

import com.appwork.mandisamiti.data.auth.AuthError
import com.appwork.mandisamiti.data.auth.AuthRepository
import com.appwork.mandisamiti.data.auth.OtpVerifyResult
import com.appwork.mandisamiti.data.auth.Session
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
    NEW_SHOP,
    ENTER_MPIN
}

data class RegisterUiState(
    val step: AuthStep = AuthStep.PHONE,
    val phoneNumber: String = "",
    val shopName: String = "",
    val ownerName: String = "",
    val mandiName: String = "",
    val mpin: String = "",
    val confirmMpin: String = "",

    // Field-level Validation Errors
    val phoneError: String? = null,
    val shopNameError: String? = null,
    val ownerNameError: String? = null,
    val mandiNameError: String? = null,
    val mpinError: String? = null,
    val confirmMpinError: String? = null,
    val generalErrorMessage: String? = null,

    // Progress Status
    val isLoading: Boolean = false,
    val isRegistrationComplete: Boolean = false,
    /** The session AuthRepository saved; handed to App so it never re-reads the store. */
    val completedSession: Session? = null
) {
    val isPhoneValid: Boolean
        get() = phoneNumber.length == 10 &&
                (phoneNumber.startsWith("6") || phoneNumber.startsWith("7") || phoneNumber.startsWith("8") || phoneNumber.startsWith("9"))

    val isNewShopValid: Boolean
        get() = shopName.trim().length >= 3 && ownerName.trim().length >= 2 && mandiName.isNotBlank() && isMpinValid

    val isEnterMpinValid: Boolean
        get() = mpin.length == 4

    val isMpinValid: Boolean
        get() = mpin.length == 4 && confirmMpin.length == 4 && mpin == confirmMpin
}

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val ttsManager: SoundboxTtsManager,
    private val viewModelScope: CoroutineScope,
    private val clock: Clock = Clock.System,
    initialPhoneNumber: String? = null
) {
    private val _uiState = MutableStateFlow(
        RegisterUiState(
            phoneNumber = initialPhoneNumber?.filter { it.isDigit() }?.take(10) ?: ""
        )
    )
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var lastClickTime = Long.MIN_VALUE

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
        _uiState.update { it.copy(mandiName = value, mandiNameError = null, generalErrorMessage = null) }
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

    /** PHONE: validate the number, then ask the server to check if it's a new or existing user. */
    fun submitPhone() {
        if (isClickThrottled()) return
        val s = _uiState.value
        if (s.isLoading) return
        if (!s.isPhoneValid) {
            _uiState.update {
                it.copy(
                    generalErrorMessage = "कृपया सभी आवश्यक विवरण सही से भरें",
                    phoneError = "10 अंकों का सही मोबाइल नंबर भरें"
                )
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, generalErrorMessage = null) }
        viewModelScope.launch {
            authRepository.checkPhone(s.phoneNumber).fold(
                onSuccess = { isNewUser ->
                    val nextStep = if (isNewUser) AuthStep.NEW_SHOP else AuthStep.ENTER_MPIN
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = nextStep,
                            mpin = "",
                            confirmMpin = "",
                            shopName = "",
                            ownerName = "",
                            mandiName = ""
                        )
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
        if (!s.isNewShopValid) {
            _uiState.update {
                it.copy(
                    generalErrorMessage = "कृपया सभी आवश्यक विवरण सही से भरें और 4 अंकों का MPIN दर्ज करके पुष्टि करें",
                    shopNameError = if (s.shopName.trim().length < 3) "फर्म का नाम दर्ज करें" else null,
                    ownerNameError = if (s.ownerName.trim().length < 2) "व्यापारी का नाम दर्ज करें" else null,
                    mandiNameError = if (s.mandiName.isBlank()) "मंडी का नाम लिखें" else null,
                    confirmMpinError = if (s.mpin != s.confirmMpin) "MPIN मेल नहीं खा रहा है" else null
                )
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, generalErrorMessage = null) }
        viewModelScope.launch {
            authRepository.signup(s.phoneNumber, s.shopName.trim(), s.ownerName.trim(), s.mandiName.trim(), s.mpin).fold(
                onSuccess = { session ->
                    ttsManager.speak("नमस्ते ${s.ownerName} जी, आपकी फर्म ${s.shopName} का पंजीयन सफल रहा।", isSoundEnabled = true)
                    _uiState.update { it.copy(isLoading = false, isRegistrationComplete = true, completedSession = session) }
                },
                onFailure = { fail(it) }
            )
        }
    }

    /** ENTER_MPIN: 4 digits, then log in. */
    fun submitMpin() {
        if (isClickThrottled()) return
        val s = _uiState.value
        if (s.isLoading) return
        if (!s.isEnterMpinValid) {
            _uiState.update { it.copy(generalErrorMessage = "कृपया 4 अंकों का MPIN दर्ज करें") }
            return
        }
        _uiState.update { it.copy(isLoading = true, generalErrorMessage = null) }
        viewModelScope.launch {
            authRepository.login(s.phoneNumber, s.mpin).fold(
                onSuccess = { session ->
                    _uiState.update { it.copy(isLoading = false, isRegistrationComplete = true, completedSession = session) }
                },
                onFailure = { fail(it) }
            )
        }
    }

    private fun fail(error: Throwable) {
        when (error) {
            is AuthError.MpinInvalid -> {
                _uiState.update { it.copy(mpin = "") }
                showError("MPIN गलत है — ${error.attemptsLeft} कोशिश बाकी")
            }
            is AuthError.AccountDisabled ->
                resetToPhone("यह खाता बंद है — दुकान मालिक से संपर्क करें")
            is AuthError.AccountLocked ->
                resetToPhone("बहुत ज़्यादा गलत MPIN — 24 घंटे बाद कोशिश करें")
            is AuthError.Invalid ->
                showError("जानकारी जाँचें और दोबारा कोशिश करें")
            is AuthError.UnsyncedOtherShop ->
                resetToPhone("इस फ़ोन पर दूसरी दुकान की प्रविष्टियाँ अभी सर्वर पर नहीं गईं — पहले उस दुकान से लॉगिन करके सिंक करें")
            is AuthError.PhoneAlreadyRegistered ->
                resetToPhone("यह नंबर पहले से रजिस्टर है — दोबारा कोशिश करें")
            else ->
                showError("नेटवर्क नहीं है — दोबारा कोशिश करें")
        }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(isLoading = false, generalErrorMessage = message) }
    }

    private fun resetToPhone(message: String?) {
        _uiState.update {
            it.copy(
                step = AuthStep.PHONE,
                mpin = "",
                confirmMpin = "",
                shopName = "",
                ownerName = "",
                mandiName = "",
                isLoading = false,
                generalErrorMessage = message
            )
        }
    }

    fun goBackToPhone() = resetToPhone(null)
}
