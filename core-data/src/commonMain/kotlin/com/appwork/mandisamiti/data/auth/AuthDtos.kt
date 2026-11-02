package com.appwork.mandisamiti.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    data class MpinInvalid(val attemptsLeft: Int) : AuthError("MPIN_INVALID")
    /** Too many wrong MPINs for this phone; server locks it for 24h. */
    object AccountLocked : AuthError("ACCOUNT_LOCKED")
    /** Server refused: account deactivated, or legacy staff account that cannot self re-onboard. */
    object AccountDisabled : AuthError("ACCOUNT_DISABLED")
    object PhoneAlreadyRegistered : AuthError("PHONE_ALREADY_REGISTERED")
    object SessionExpired : AuthError("SESSION_EXPIRED")
    /** Server rejected the input (400 or 422 validation). */
    object Invalid : AuthError("INVALID")
    /** This phone holds another server shop's unsynced entries; login refused so they are not wiped. */
    object UnsyncedOtherShop : AuthError("UNSYNCED_OTHER_SHOP")
    data class Network(val causeMessage: String) : AuthError("NETWORK")
}

@Serializable internal data class CheckPhoneRequest(val phone: String)
@Serializable internal data class CheckPhoneResponse(@SerialName("is_new_user") val isNewUser: Boolean)
@Serializable internal data class SignupRequest(
    val phone: String,
    @SerialName("shop_name") val shopName: String,
    @SerialName("owner_name") val ownerName: String,
    @SerialName("mandi_name") val mandiName: String,
    val mpin: String,
)
@Serializable internal data class LoginRequest(
    val phone: String,
    val mpin: String,
)
@Serializable internal data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)
@Serializable internal data class RefreshResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)
