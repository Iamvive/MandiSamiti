package com.appwork.mandisamiti.data.auth

import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.repository.ShopProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class AuthRepository(
    private val api: AuthApi,
    private val sessionStore: SessionStore,
    private val shopProfileRepository: ShopProfileRepository,
    private val wiper: LocalDataWiper,
    private val clock: Clock = Clock.System,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend fun sendOtp(phone: String): Result<Unit> = api.sendOtp(phone)

    suspend fun verifyOtp(phone: String, otp: String): Result<OtpVerifyResult> = api.verifyOtp(phone, otp)

    suspend fun signup(pass: String, shopName: String, ownerName: String, mandiName: String, mpin: String): Result<Session> =
        api.signup(pass, shopName, ownerName, mandiName, mpin).mapCatchingSuspend { adopt(it, phone = "") }

    suspend fun login(pass: String, mpin: String): Result<Session> =
        api.login(pass, mpin).mapCatchingSuspend { adopt(it, phone = "") }

    /** Makes the server's shop the local one: wipes foreign/demo data, saves the profile, then the session. */
    private suspend fun adopt(dto: AuthSessionDto, phone: String): Session = withContext(ioDispatcher) {
        val shop = dto.shop
        val localId = shopProfileRepository.getShopProfileStream().first()?.id
        if (localId != shop.id) wiper.wipeAll()
        val now = clock.now().toEpochMilliseconds()
        shopProfileRepository.saveShopProfile(
            ShopProfile(
                id = shop.id,
                shopName = shop.shopName,
                ownerName = shop.ownerName ?: "",
                mandiName = shop.mandiName ?: "",
                phoneNumber = shop.phoneNumber ?: phone,
                pinHash = "",
                createdAt = now,
                updatedAt = now,
                syncStatus = 1,
            )
        )
        Session(shop.id, dto.accessToken, dto.refreshToken).also { sessionStore.save(it) }
    }

    /** Like Result.mapCatching but for suspend transforms; CancellationException is rethrown, not captured. */
    private suspend fun <T, R> Result<T>.mapCatchingSuspend(transform: suspend (T) -> R): Result<R> =
        fold(
            onSuccess = {
                try {
                    Result.success(transform(it))
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(e)
                }
            },
            onFailure = { Result.failure(it) },
        )
}
