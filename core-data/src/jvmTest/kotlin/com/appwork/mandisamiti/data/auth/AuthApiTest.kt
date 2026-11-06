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
    fun checkPhone_parsesIsNewUser() = runTest {
        val reqs = mutableListOf<HttpRequestData>()
        val r = api(reqs) { json(HttpStatusCode.OK, """{"is_new_user":true}""") }.checkPhone("9876543210")
        assertTrue(r.getOrThrow())
        assertEquals("$base/api/v1/auth/check-phone", reqs.single().url.toString())
        assertEquals(setOf("phone"), reqs.single().bodyJson().keys)
    }

    @Test
    fun signup_postsSnakeCaseBody_andParsesSession() = runTest {
        val reqs = mutableListOf<HttpRequestData>()
        val r = api(reqs) { json(HttpStatusCode.OK, sessionJson) }.signup("9876543210", "Shop", "Owner", "Mandi", "1234")
        assertEquals("s1", r.getOrThrow().shop.id)
        assertEquals("a", r.getOrThrow().accessToken)
        val req = reqs.single()
        assertEquals("$base/api/v1/auth/signup", req.url.toString())
        assertEquals(HttpMethod.Post, req.method)
        assertEquals(setOf("phone", "shop_name", "owner_name", "mandi_name", "mpin"), req.bodyJson().keys)
    }

    @Test
    fun signup_409_and_401() = runTest {
        assertEquals(
            AuthError.PhoneAlreadyRegistered,
            api { json(HttpStatusCode.Conflict, "{}") }.signup("p", "s", "o", "m", "1").error(),
        )
        assertEquals(
            AuthError.AccountDisabled,
            api { json(HttpStatusCode.Unauthorized, "{}") }.signup("p", "s", "o", "m", "1").error(),
        )
    }

    @Test
    fun login_mpinInvalid_and_unknown401IsNetwork() = runTest {
        val reqs = mutableListOf<HttpRequestData>()
        val r = api(reqs) {
            json(HttpStatusCode.Unauthorized, """{"detail":{"code":"MPIN_INVALID","attempts_left":3}}""")
        }.login("lp", "0000")
        assertEquals(AuthError.MpinInvalid(3), r.error())
        assertEquals("$base/api/v1/auth/login", reqs.single().url.toString())
        assertEquals(setOf("phone", "mpin"), reqs.single().bodyJson().keys)
        val u = api { json(HttpStatusCode.Unauthorized, "not json") }.login("lp", "0")
        assertEquals(AuthError.Network("HTTP 401"), u.error())
    }

    @Test
    fun malformed2xxBody_isNetwork() = runTest {
        val r = api { json(HttpStatusCode.OK, """{"unexpected":1}""") }.login("lp", "1")
        assertIs<AuthError.Network>(r.error())
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

    @Test
    fun signup_422_isInvalid_and_checkPhone400_isInvalid() = runTest {
        val r = api { json(HttpStatusCode.UnprocessableEntity, """{"detail":[{"loc":["body","mandi_name"]}]}""") }
            .signup("p", "s", "o", "", "1234")
        assertEquals(AuthError.Invalid, r.error())
        val c = api { json(HttpStatusCode.BadRequest, """{"detail":"INVALID_PHONE"}""") }.checkPhone("1")
        assertEquals(AuthError.Invalid, c.error())
    }

    @Test
    fun accountDisabled_onSignupAndLogin_isAccountDisabled() = runTest {
        val body = """{"detail":{"code":"ACCOUNT_DISABLED"}}"""
        assertEquals(AuthError.AccountDisabled, api { json(HttpStatusCode.Unauthorized, body) }.signup("p", "s", "o", "m", "1234").error())
        assertEquals(AuthError.AccountDisabled, api { json(HttpStatusCode.Unauthorized, body) }.login("lp", "1234").error())
    }
}
