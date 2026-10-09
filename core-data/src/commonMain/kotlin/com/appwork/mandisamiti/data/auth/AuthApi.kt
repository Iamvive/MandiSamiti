package com.appwork.mandisamiti.data.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val authJson = Json { ignoreUnknownKeys = true }

fun mandiHttpClient(engine: HttpClientEngine? = null): HttpClient {
    val config: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) { json(authJson) }
    }
    return if (engine == null) HttpClient(config) else HttpClient(engine, config)
}

class AuthApi(private val http: HttpClient, private val baseUrl: String) {

    suspend fun sendOtp(phone: String): Result<Unit> =
        call("otp/send", SendOtpRequest(phone), { status, _ ->
            when (status) {
                HttpStatusCode.TooManyRequests -> AuthError.RateLimited
                HttpStatusCode.ServiceUnavailable -> AuthError.Network("OTP service unavailable")
                else -> null
            }
        }) { }

    suspend fun verifyOtp(phone: String, otp: String): Result<OtpVerifyResult> =
        call("otp/verify", VerifyOtpRequest(phone, otp), { status, text ->
            when {
                status == HttpStatusCode.BadRequest && detailString(text) == "OTP_INVALID" -> AuthError.OtpInvalid
                status == HttpStatusCode.TooManyRequests -> AuthError.RateLimited
                else -> null
            }
        }) { resp ->
            val r: VerifyOtpResponse = resp.body()
            when (r.status) {
                "NEW" -> OtpVerifyResult.NewShop(r.signupPass ?: throw AuthError.Network("Malformed response"))
                "EXISTING" -> OtpVerifyResult.ExistingShop(r.loginPass ?: throw AuthError.Network("Malformed response"))
                else -> throw AuthError.Network("Unknown status ${r.status}")
            }
        }

    suspend fun signup(
        signupPass: String,
        shopName: String,
        ownerName: String,
        mandiName: String,
        mpin: String,
    ): Result<AuthSessionDto> =
        call("signup", SignupRequest(signupPass, shopName, ownerName, mandiName, mpin), { status, _ ->
            when (status) {
                HttpStatusCode.Unauthorized -> AuthError.PassBurned
                HttpStatusCode.Conflict -> AuthError.PhoneAlreadyRegistered
                else -> null
            }
        }) { it.body<AuthSessionDto>() }

    suspend fun login(loginPass: String, mpin: String): Result<AuthSessionDto> =
        call("login", LoginRequest(loginPass, mpin), { status, text ->
            if (status == HttpStatusCode.Unauthorized) {
                val detail = detailObject(text)
                when (detail?.get("code")?.jsonPrimitive?.contentOrNull) {
                    "MPIN_INVALID" -> AuthError.MpinInvalid(detail["attempts_left"]?.jsonPrimitive?.intOrNull ?: 0)
                    "ACCOUNT_LOCKED" -> AuthError.AccountLocked
                    // PASS_BURNED, plain "INVALID_PASS", or any unrecognised 401: the pass is dead.
                    else -> AuthError.PassBurned
                }
            } else null
        }) { it.body<AuthSessionDto>() }

    suspend fun refresh(refreshToken: String): Result<Pair<String, String>> =
        call("refresh", RefreshRequest(refreshToken), { status, _ ->
            if (status == HttpStatusCode.Unauthorized) AuthError.SessionExpired else null
        }) {
            val r: RefreshResponse = it.body()
            r.accessToken to r.refreshToken
        }

    suspend fun logout(refreshToken: String): Result<Unit> =
        call("logout", RefreshRequest(refreshToken), { _, _ -> null }) { }

    /**
     * POSTs [body]; non-2xx goes through [mapError] (null falls back to "HTTP <code>"), 2xx through [parse].
     * IO and parse failures become [AuthError]; CancellationException always propagates.
     */
    private suspend inline fun <reified Req : Any, R> call(
        path: String,
        body: Req,
        mapError: (HttpStatusCode, String) -> AuthError?,
        parse: (HttpResponse) -> R,
    ): Result<R> {
        return try {
            val response = http.post("$baseUrl/api/v1/auth/$path") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (response.status.value in 200..299) {
                Result.success(parse(response))
            } else {
                val text = try { response.bodyAsText() } catch (e: CancellationException) { throw e } catch (e: Exception) { "" }
                Result.failure(mapError(response.status, text) ?: AuthError.Network("HTTP ${response.status.value}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: AuthError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(AuthError.Network(e.message ?: e::class.simpleName ?: "IO error"))
        }
    }

    private fun detailElement(text: String) =
        runCatching { authJson.parseToJsonElement(text).jsonObject["detail"] }.getOrNull()

    private fun detailString(text: String): String? =
        runCatching { detailElement(text)?.jsonPrimitive?.contentOrNull }.getOrNull()

    private fun detailObject(text: String): JsonObject? =
        runCatching { detailElement(text)?.jsonObject }.getOrNull()
}
