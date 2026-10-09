package com.appwork.mandisamiti.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface OtpVerifyResult {
    data class NewShop(val signupPass: String) : OtpVerifyResult
    data class ExistingShop(val loginPass: String) : OtpVerifyResult
}

@Serializable
data class ShopDto(
    val id: String,
    @SerialName("shop_name") val shopName: String,
    @SerialName("owner_name") val ownerName: String? = null,
    @SerialName("mandi_name") val mandiName: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
)

@Serializable
data class AuthSessionDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    val shop: ShopDto,
)

sealed class AuthError(message: String) : Exception(message) {
    object OtpInvalid : AuthError("OTP_INVALID")
    object RateLimited : AuthError("RATE_LIMITED")
    data class MpinInvalid(val attemptsLeft: Int) : AuthError("MPIN_INVALID")
    object PassBurned : AuthError("PASS_BURNED")
    /** Pass expired or unknown (INVALID_PASS / unrecognised 401): get a fresh OTP. */
    object PassExpired : AuthError("PASS_EXPIRED")
    /** Too many wrong MPINs for this phone across passes; server locks it for 24h. */
    object AccountLocked : AuthError("ACCOUNT_LOCKED")
    /** Server refused: account deactivated, or legacy staff account that cannot self re-onboard. */
    object AccountDisabled : AuthError("ACCOUNT_DISABLED")
    object PhoneAlreadyRegistered : AuthError("PHONE_ALREADY_REGISTERED")
    object SessionExpired : AuthError("SESSION_EXPIRED")
    /** Server rejected the input (400 other than OTP_INVALID, or 422 validation). */
    object Invalid : AuthError("INVALID")
    /** This phone holds another server shop's unsynced entries; login refused so they are not wiped. */
    object UnsyncedOtherShop : AuthError("UNSYNCED_OTHER_SHOP")
    data class Network(val causeMessage: String) : AuthError("NETWORK")
}

@Serializable internal data class SendOtpRequest(val phone: String)
@Serializable internal data class VerifyOtpRequest(val phone: String, val otp: String)
@Serializable internal data class VerifyOtpResponse(
    val status: String,
    @SerialName("signup_pass") val signupPass: String? = null,
    @SerialName("login_pass") val loginPass: String? = null,
)
@Serializable internal data class SignupRequest(
    @SerialName("signup_pass") val signupPass: String,
    @SerialName("shop_name") val shopName: String,
    @SerialName("owner_name") val ownerName: String,
    @SerialName("mandi_name") val mandiName: String,
    val mpin: String,
)
@Serializable internal data class LoginRequest(
    @SerialName("login_pass") val loginPass: String,
    val mpin: String,
)
@Serializable internal data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)
@Serializable internal data class RefreshResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)
