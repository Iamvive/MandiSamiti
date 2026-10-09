package com.appwork.mandisamiti.data.auth

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AuthApiTest {
    private val base = "https://api.test"
    private val sessionJson =
        """{"access_token":"a","refresh_token":"r","shop":{"id":"s1","shop_name":"Shop","owner_name":"O","mandi_name":"M","phone_number":"9"}}"""

    private fun MockRequestHandleScope.json(status: HttpStatusCode, body: String): HttpResponseData =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun api(
        captured: MutableList<HttpRequestData> = mutableListOf(),
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): AuthApi {
        val engine = MockEngine { req -> captured += req; handler(req) }
        return AuthApi(mandiHttpClient(engine), base)
    }

    private fun HttpRequestData.bodyJson(): JsonObject =
        Json.parseToJsonElement((body as OutgoingContent.ByteArrayContent).bytes().decodeToString()).jsonObject

    private fun <T> Result<T>.error(): AuthError = exceptionOrNull() as AuthError

    @Test
    fun sendOtp_postsPhone_andSucceeds() = runTest {
        val reqs = mutableListOf<HttpRequestData>()
        val r = api(reqs) { json(HttpStatusCode.OK, """{"sent":true,"cooldown_s":30}""") }.sendOtp("9876543210")
        assertTrue(r.isSuccess)
        assertEquals("$base/api/v1/auth/otp/send", reqs.single().url.toString())
        assertEquals(HttpMethod.Post, reqs.single().method)
        assertEquals(setOf("phone"), reqs.single().bodyJson().keys)
    }

    @Test
    fun sendOtp_429_isRateLimited() = runTest {
        val r = api { json(HttpStatusCode.TooManyRequests, """{"detail":"slow"}""") }.sendOtp("1")
        assertEquals(AuthError.RateLimited, r.error())
    }

    @Test
    fun sendOtp_503_isNetworkOtpUnavailable_and400_isHttp400() = runTest {
        val r503 = api { json(HttpStatusCode.ServiceUnavailable, "{}") }.sendOtp("1")
        assertEquals(AuthError.Network("OTP service unavailable"), r503.error())
        val r400 = api { json(HttpStatusCode.BadRequest, """{"detail":"bad phone"}""") }.sendOtp("1")
        assertEquals(AuthError.Network("HTTP 400"), r400.error())
    }

    @Test
    fun verifyOtp_new_and_existing() = runTest {
        val reqs = mutableListOf<HttpRequestData>()
        val n = api(reqs) { json(HttpStatusCode.OK, """{"status":"NEW","signup_pass":"p"}""") }.verifyOtp("9", "123456")
        assertEquals(OtpVerifyResult.NewShop("p"), n.getOrThrow())
        assertEquals("$base/api/v1/auth/otp/verify", reqs.single().url.toString())
        assertEquals(setOf("phone", "otp"), reqs.single().bodyJson().keys)
        val e = api { json(HttpStatusCode.OK, """{"status":"EXISTING","login_pass":"l"}""") }.verifyOtp("9", "1")
        assertEquals(OtpVerifyResult.ExistingShop("l"), e.getOrThrow())
    }

    @Test
    fun verifyOtp_400_isOtpInvalid() = runTest {
        val r = api { json(HttpStatusCode.BadRequest, """{"detail":"OTP_INVALID"}""") }.verifyOtp("9", "0")
        assertEquals(AuthError.OtpInvalid, r.error())
    }

    @Test
    fun signup_postsSnakeCaseBody_andParsesSession() = runTest {
        val reqs = mutableListOf<HttpRequestData>()
        val r = api(reqs) { json(HttpStatusCode.OK, sessionJson) }.signup("sp", "Shop", "Owner", "Mandi", "1234")
        assertEquals("s1", r.getOrThrow().shop.id)
        assertEquals("a", r.getOrThrow().accessToken)
        val req = reqs.single()
        assertEquals("$base/api/v1/auth/signup", req.url.toString())
        assertEquals(HttpMethod.Post, req.method)
        assertEquals(setOf("signup_pass", "shop_name", "owner_name", "mandi_name", "mpin"), req.bodyJson().keys)
    }

    @Test
    fun signup_409_and_401() = runTest {
        assertEquals(
            AuthError.PhoneAlreadyRegistered,
            api { json(HttpStatusCode.Conflict, "{}") }.signup("p", "s", "o", "m", "1").error(),
        )
        assertEquals(
            AuthError.PassBurned,
            api { json(HttpStatusCode.Unauthorized, """{"detail":"invalid"}""") }.signup("p", "s", "o", "m", "1").error(),
        )
    }

    @Test
    fun login_mpinInvalid_and_passBurned() = runTest {
        val reqs = mutableListOf<HttpRequestData>()
        val r = api(reqs) {
            json(HttpStatusCode.Unauthorized, """{"detail":{"code":"MPIN_INVALID","attempts_left":3}}""")
        }.login("lp", "0000")
        assertEquals(AuthError.MpinInvalid(3), r.error())
        assertEquals("$base/api/v1/auth/login", reqs.single().url.toString())
        assertEquals(setOf("login_pass", "mpin"), reqs.single().bodyJson().keys)
        val b = api { json(HttpStatusCode.Unauthorized, """{"detail":{"code":"PASS_BURNED"}}""") }.login("lp", "0")
        assertEquals(AuthError.PassBurned, b.error())
    }

    @Test
    fun login_deadPass_variants_mapToPassBurned() = runTest {
        for (body in listOf("""{"detail":"INVALID_PASS"}""", """{"detail":{"code":"WHATEVER"}}""", "not json")) {
            val r = api { json(HttpStatusCode.Unauthorized, body) }.login("lp", "1")
            assertEquals(AuthError.PassBurned, r.error(), body)
        }
    }

    @Test
    fun verifyOtp_429_isRateLimited() = runTest {
        val r = api { json(HttpStatusCode.TooManyRequests, "{}") }.verifyOtp("9", "1")
        assertEquals(AuthError.RateLimited, r.error())
    }

    @Test
    fun malformed2xxBody_isNetwork() = runTest {
        val r = api { json(HttpStatusCode.OK, """{"unexpected":1}""") }.login("lp", "1")
        assertIs<AuthError.Network>(r.error())
        val v = api { json(HttpStatusCode.OK, """{"status":"NEW"}""") }.verifyOtp("9", "1")
        assertEquals(AuthError.Network("Malformed response"), v.error())
    }

    @Test
    fun cancellationFromEngine_propagates() = runTest {
        val client = mandiHttpClient(MockEngine { throw CancellationException("cancelled") })
        assertFailsWith<CancellationException> { AuthApi(client, base).login("lp", "1") }
        assertFailsWith<CancellationException> { AuthApi(client, base).refresh("r") }
    }

    @Test
    fun login_success_parsesSession() = runTest {
        val r = api { json(HttpStatusCode.OK, sessionJson) }.login("lp", "1234")
        assertEquals("r", r.getOrThrow().refreshToken)
    }

    @Test
    fun refresh_success_and_401() = runTest {
        val reqs = mutableListOf<HttpRequestData>()
        val ok = api(reqs) { json(HttpStatusCode.OK, """{"access_token":"a2","refresh_token":"r2"}""") }.refresh("r")
        assertEquals("a2" to "r2", ok.getOrThrow())
        assertEquals("$base/api/v1/auth/refresh", reqs.single().url.toString())
        assertEquals(setOf("refresh_token"), reqs.single().bodyJson().keys)
        assertEquals(AuthError.SessionExpired, api { json(HttpStatusCode.Unauthorized, "{}") }.refresh("r").error())
    }

    @Test
    fun logout_204_succeeds() = runTest {
        val reqs = mutableListOf<HttpRequestData>()
        val r = api(reqs) { respond(ByteReadChannel(""), HttpStatusCode.NoContent) }.logout("r")
        assertTrue(r.isSuccess)
        assertEquals("$base/api/v1/auth/logout", reqs.single().url.toString())
        assertEquals(setOf("refresh_token"), reqs.single().bodyJson().keys)
    }

    @Test
    fun ioException_isNetwork() = runTest {
        val client = mandiHttpClient(MockEngine { throw IOException("offline") })
        val r = AuthApi(client, base).login("lp", "1")
        assertIs<AuthError.Network>(r.error())
        assertEquals("offline", (r.error() as AuthError.Network).causeMessage)
    }

    @Test
    fun login_accountLocked_isAccountLocked() = runTest {
        val r = api { json(HttpStatusCode.Unauthorized, """{"detail":{"code":"ACCOUNT_LOCKED"}}""") }.login("lp", "1234")
        assertEquals(AuthError.AccountLocked, r.error())
    }
}
