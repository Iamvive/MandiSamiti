package com.appwork.mandisamiti.ui.auth

import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.repository.ShopProfileRepository
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import kotlinx.coroutines.CoroutineScope
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
    val phoneNumber: String = "9837123456",
    val shopName: String = "श्री गणेश ट्रेडिंग",
    val ownerName: String = "लाला मदन लाल जी",
    val mandiName: String = "मथुरा कृषि उपज मंडी",
    val otp: String = "",
    val mpin: String = "",
    val confirmMpin: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistrationComplete: Boolean = false
)

class RegisterViewModel(
    private val shopProfileRepository: ShopProfileRepository,
    private val ttsManager: SoundboxTtsManager,
    private val viewModelScope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onPhoneNumberChanged(value: String) {
        if (value.length <= 10 && value.all { it.isDigit() }) {
            _uiState.update { it.copy(phoneNumber = value, errorMessage = null) }
        }
    }

    fun onShopNameChanged(value: String) {
        _uiState.update { it.copy(shopName = value, errorMessage = null) }
    }

    fun onOwnerNameChanged(value: String) {
        _uiState.update { it.copy(ownerName = value, errorMessage = null) }
    }

    fun onMandiNameChanged(value: String) {
        _uiState.update { it.copy(mandiName = value, errorMessage = null) }
    }

    fun onOtpChanged(value: String) {
        if (value.length <= 6 && value.all { it.isDigit() }) {
            _uiState.update { it.copy(otp = value, errorMessage = null) }
        }
    }

    fun onMpinChanged(value: String) {
        if (value.length <= 4 && value.all { it.isDigit() }) {
            _uiState.update { it.copy(mpin = value, errorMessage = null) }
        }
    }

    fun onConfirmMpinChanged(value: String) {
        if (value.length <= 4 && value.all { it.isDigit() }) {
            _uiState.update { it.copy(confirmMpin = value, errorMessage = null) }
        }
    }

    fun proceedToOtp() {
        val s = _uiState.value
        if (s.phoneNumber.length != 10) {
            _uiState.update { it.copy(errorMessage = "कृपया 10 अंकों का सही मोबाइल नंबर दर्ज करें") }
            return
        }
        if (s.shopName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "कृपया अपनी दुकान / फर्म का नाम दर्ज करें") }
            return
        }
        if (s.ownerName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "कृपया व्यापारी का नाम दर्ज करें") }
            return
        }

        _uiState.update {
            it.copy(
                step = AuthStep.OTP_VERIFICATION,
                otp = "123456", // Pre-fill mock OTP in test mode
                errorMessage = null
            )
        }
    }

    fun verifyOtp() {
        val s = _uiState.value
        if (s.otp.length < 4) {
            _uiState.update { it.copy(errorMessage = "कृपया सही 6-अंकीय OTP दर्ज करें") }
            return
        }

        _uiState.update {
            it.copy(
                step = AuthStep.MPIN_SETUP,
                errorMessage = null
            )
        }
    }

    fun completeRegistration() {
        val s = _uiState.value
        if (s.mpin.length != 4) {
            _uiState.update { it.copy(errorMessage = "कृपया 4 अंकों का सुरक्षा MPIN दर्ज करें") }
            return
        }
        if (s.confirmMpin.isNotEmpty() && s.mpin != s.confirmMpin) {
            _uiState.update { it.copy(errorMessage = "MPIN मेल नहीं खा रहा है") }
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
        _uiState.update { it.copy(step = AuthStep.PHONE_AND_SHOP, errorMessage = null) }
    }

    fun goBackToOtp() {
        _uiState.update { it.copy(step = AuthStep.OTP_VERIFICATION, errorMessage = null) }
    }
}
