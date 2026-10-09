package com.appwork.mandisamiti.ui.auth

import com.appwork.mandisamiti.data.auth.AuthApi
import com.appwork.mandisamiti.data.auth.AuthRepository
import com.appwork.mandisamiti.data.auth.InMemorySessionStore
import com.appwork.mandisamiti.data.auth.LocalDataWiper
import com.appwork.mandisamiti.data.auth.mandiHttpClient
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegisterViewModelTest {
    private class TestClock(var ms: Long = 1_000_000L) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(ms)
        fun tap() { ms += 1_000L }
    }

    private val sessionJson =
        """{"access_token":"a","refresh_token":"r","shop":{"id":"s1","shop_name":"Shop","owner_name":"O","mandi_name":"M","phone_number":"9837123456"}}"""

    private fun MockRequestHandleScope.json(status: HttpStatusCode, body: String): HttpResponseData =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private class Fixture(val vm: RegisterViewModel, val clock: TestClock, val calls: MutableList<Pair<String, String>>) {
        fun bodyOf(suffix: String) = calls.filter { it.first.endsWith(suffix) }.map { it.second }
        fun count(suffix: String) = calls.count { it.first.endsWith(suffix) }
    }

    private fun TestScope.fixture(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): Fixture {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        val calls = mutableListOf<Pair<String, String>>()
        val repo = AuthRepository(
            api = AuthApi(mandiHttpClient(MockEngine {
                calls += it.url.encodedPath to ((it.body as? TextContent)?.text ?: "")
                handler(it)
            }), "https://api.test"),
            sessionStore = InMemorySessionStore(),
            shopProfileRepository = OfflineFirstShopProfileRepository(db, io),
            wiper = LocalDataWiper(db),
            ioDispatcher = io,
        )
        val clock = TestClock()
        return Fixture(RegisterViewModel(repo, SoundboxTtsManager(), backgroundScope, clock), clock, calls)
    }

    private fun Fixture.enterPhone() {
        vm.onPhoneNumberChanged("9837123456")
        clock.tap(); vm.submitPhone()
    }

    private suspend fun Fixture.toOtpStep() {
        enterPhone()
        vm.uiState.first { it.step == AuthStep.OTP && !it.isLoading }
    }

    private fun MockRequestHandleScope.verifyBody(path: String, status: String) = when {
        path.endsWith("otp/send") -> json(HttpStatusCode.OK, """{"sent":true,"cooldown_s":30}""")
        path.endsWith("otp/verify") && status == "NEW" -> json(HttpStatusCode.OK, """{"status":"NEW","signup_pass":"sp"}""")
        path.endsWith("otp/verify") -> json(HttpStatusCode.OK, """{"status":"EXISTING","login_pass":"lp"}""")
        else -> json(HttpStatusCode.NotFound, "{}")
    }

    @Test
    fun newShop_phoneToOtpToNewShopToComplete() = runTest {
        val f = fixture { req ->
            val p = req.url.encodedPath
            if (p.endsWith("signup")) json(HttpStatusCode.OK, sessionJson) else verifyBody(p, "NEW")
        }
        f.toOtpStep()
        assertEquals(30, f.vm.uiState.value.resendCooldownSeconds)
        assertEquals(false, f.vm.uiState.value.isResendEnabled)

        f.clock.tap(); f.vm.onOtpChanged("123456")
        f.vm.uiState.first { it.step == AuthStep.NEW_SHOP }

        f.vm.onShopNameChanged("राम आढ़त")
        f.vm.onOwnerNameChanged("राम")
        f.vm.onMpinChanged("1234")
        f.vm.onConfirmMpinChanged("1234")
        f.clock.tap(); f.vm.submitNewShop()

        assertEquals(true, f.vm.uiState.first { it.isRegistrationComplete }.isRegistrationComplete)
    }

    @Test
    fun existingShop_wrongMpin_showsAttemptsLeft() = runTest {
        val f = fixture { req ->
            val p = req.url.encodedPath
            if (p.endsWith("login")) {
                json(HttpStatusCode.Unauthorized, """{"detail":{"code":"MPIN_INVALID","attempts_left":2}}""")
            } else verifyBody(p, "EXISTING")
        }
        f.toOtpStep()
        f.clock.tap(); f.vm.onOtpChanged("123456")
        f.vm.uiState.first { it.step == AuthStep.ENTER_MPIN }

        f.vm.onMpinChanged("0000")
        f.clock.tap(); f.vm.submitMpin()

        val s = f.vm.uiState.first { it.generalErrorMessage != null }
        assertEquals("MPIN गलत है — 2 कोशिश बाकी", s.generalErrorMessage)
        assertEquals(AuthStep.ENTER_MPIN, s.step)
    }

    @Test
    fun passBurned_returnsToPhone() = runTest {
        val f = fixture { req ->
            val p = req.url.encodedPath
            if (p.endsWith("login")) {
                json(HttpStatusCode.Unauthorized, """{"detail":{"code":"PASS_BURNED"}}""")
            } else verifyBody(p, "EXISTING")
        }
        f.toOtpStep()
        f.clock.tap(); f.vm.onOtpChanged("123456")
        f.vm.uiState.first { it.step == AuthStep.ENTER_MPIN }
        f.vm.onMpinChanged("0000")
        f.clock.tap(); f.vm.submitMpin()

        val s = f.vm.uiState.first { it.step == AuthStep.PHONE }
        assertEquals("बहुत गलत MPIN — दोबारा OTP लें", s.generalErrorMessage)
    }

    @Test
    fun signup_passBurned_returnsToPhone() = runTest {
        val f = fixture { req ->
            val p = req.url.encodedPath
            if (p.endsWith("signup")) json(HttpStatusCode.Unauthorized, "{}") else verifyBody(p, "NEW")
        }
        f.toOtpStep()
        f.clock.tap(); f.vm.onOtpChanged("123456")
        f.vm.uiState.first { it.step == AuthStep.NEW_SHOP }
        f.vm.onShopNameChanged("राम आढ़त"); f.vm.onOwnerNameChanged("राम")
        f.vm.onMpinChanged("1234"); f.vm.onConfirmMpinChanged("1234")
        f.clock.tap(); f.vm.submitNewShop()

        val s = f.vm.uiState.first { it.step == AuthStep.PHONE }
        assertEquals("समय समाप्त — दोबारा OTP लें", s.generalErrorMessage)
    }

    @Test
    fun signup_phoneAlreadyRegistered_returnsToPhone() = runTest {
        val f = fixture { req ->
            val p = req.url.encodedPath
            if (p.endsWith("signup")) json(HttpStatusCode.Conflict, "{}") else verifyBody(p, "NEW")
        }
        f.toOtpStep()
        f.clock.tap(); f.vm.onOtpChanged("123456")
        f.vm.uiState.first { it.step == AuthStep.NEW_SHOP }
        f.vm.onShopNameChanged("राम आढ़त"); f.vm.onOwnerNameChanged("राम")
        f.vm.onMpinChanged("1234"); f.vm.onConfirmMpinChanged("1234")
        f.clock.tap(); f.vm.submitNewShop()

        val s = f.vm.uiState.first { it.step == AuthStep.PHONE }
        assertEquals("यह नंबर पहले से रजिस्टर है — दोबारा OTP लेकर लॉगिन करें", s.generalErrorMessage)
    }

    @Test
    fun rateLimited_showsFiveMinuteMessage() = runTest {
        val f = fixture { json(HttpStatusCode.TooManyRequests, "{}") }
        f.enterPhone()

        val s = f.vm.uiState.first { it.generalErrorMessage != null }
        assertEquals("बहुत बार OTP माँगा गया, 5 मिनट बाद कोशिश करें", s.generalErrorMessage)
        assertEquals(AuthStep.PHONE, s.step)
    }

    @Test
    fun wrongOtp_showsMessage_andStaysOnOtp() = runTest {
        val f = fixture { req ->
            val p = req.url.encodedPath
            if (p.endsWith("otp/verify")) json(HttpStatusCode.BadRequest, """{"detail":"OTP_INVALID"}""") else verifyBody(p, "NEW")
        }
        f.toOtpStep()
        f.clock.tap(); f.vm.onOtpChanged("123456")

        val s = f.vm.uiState.first { it.generalErrorMessage != null }
        assertEquals("OTP गलत है", s.generalErrorMessage)
        assertEquals(AuthStep.OTP, s.step)
    }

    @Test
    fun networkFailure_showsNoNetworkMessage() = runTest {
        val f = fixture { json(HttpStatusCode.ServiceUnavailable, "{}") }
        f.enterPhone()

        val s = f.vm.uiState.first { it.generalErrorMessage != null }
        assertEquals("नेटवर्क नहीं है — दोबारा कोशिश करें", s.generalErrorMessage)
    }

    @Test
    fun rapidSecondTap_isThrottled() = runTest {
        var sends = 0
        val f = fixture { sends++; json(HttpStatusCode.OK, """{"sent":true,"cooldown_s":30}""") }
        f.vm.onPhoneNumberChanged("9837123456")
        f.clock.tap(); f.vm.submitPhone()
        f.vm.submitPhone() // same instant: throttled
        f.vm.uiState.first { it.step == AuthStep.OTP && !it.isLoading }
        assertEquals(1, sends)
        assertNull(f.vm.uiState.value.generalErrorMessage)
    }

    // ---- helpers for the added coverage ----
    private val signupOk: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { req ->
        val p = req.url.encodedPath
        if (p.endsWith("signup")) json(HttpStatusCode.OK, sessionJson) else verifyBody(p, "NEW")
    }

    private suspend fun Fixture.toNewShop() {
        toOtpStep()
        clock.tap(); vm.onOtpChanged("123456")
        vm.uiState.first { it.step == AuthStep.NEW_SHOP }
    }

    private suspend fun Fixture.toEnterMpin() {
        toOtpStep()
        clock.tap(); vm.onOtpChanged("123456")
        vm.uiState.first { it.step == AuthStep.ENTER_MPIN }
    }

    private fun Fixture.fillShop(shop: String = "राम आढ़त", owner: String = "राम", mpin: String = "1234", confirm: String = "1234") {
        vm.onShopNameChanged(shop); vm.onOwnerNameChanged(owner); vm.onMandiNameChanged("मथुरा")
        vm.onMpinChanged(mpin); vm.onConfirmMpinChanged(confirm)
    }

    @Test
    fun signup_sendsSignupPassAndShopFields() = runTest {
        val f = fixture(signupOk)
        f.toNewShop()
        f.fillShop()
        f.clock.tap(); f.vm.submitNewShop()
        f.vm.uiState.first { it.isRegistrationComplete }

        val body = f.bodyOf("signup").single()
        assertTrue("\"signup_pass\":\"sp\"" in body, body)
        assertTrue("\"shop_name\":\"राम आढ़त\"" in body || "\"shop_name\":\"\\u" in body, body)
        assertTrue("owner_name" in body && "mandi_name" in body, body)
        assertTrue("\"mpin\":\"1234\"" in body, body)
    }

    @Test
    fun login_sendsLoginPassNotSignupPass() = runTest {
        val f = fixture { req ->
            val p = req.url.encodedPath
            if (p.endsWith("login")) json(HttpStatusCode.OK, sessionJson) else verifyBody(p, "EXISTING")
        }
        f.toEnterMpin()
        f.vm.onMpinChanged("4321")
        f.clock.tap(); f.vm.submitMpin()
        f.vm.uiState.first { it.isRegistrationComplete }

        val body = f.bodyOf("login").single()
        assertTrue("\"login_pass\":\"lp\"" in body, body)
        assertTrue("sp" !in body.replace("login_pass", ""), body)
        assertTrue("\"mpin\":\"4321\"" in body, body)
    }

    @Test
    fun newShop_backToPhone_thenSubmit_doesNotCallSignup() = runTest {
        val f = fixture(signupOk)
        f.toNewShop()
        f.fillShop()
        f.vm.goBackToPhone()
        f.clock.tap(); f.vm.submitNewShop()

        advanceUntilIdle()
        assertEquals(0, f.count("signup"))
        assertEquals(AuthStep.PHONE, f.vm.uiState.value.step)
        assertEquals("समय समाप्त — दोबारा OTP लें", f.vm.uiState.value.generalErrorMessage)
    }

    @Test
    fun afterPassBurned_submitMpin_doesNotReuseBurnedPass() = runTest {
        val f = fixture { req ->
            val p = req.url.encodedPath
            if (p.endsWith("login")) json(HttpStatusCode.Unauthorized, """{"detail":{"code":"PASS_BURNED"}}""") else verifyBody(p, "EXISTING")
        }
        f.toEnterMpin()
        f.vm.onMpinChanged("0000")
        f.clock.tap(); f.vm.submitMpin()
        f.vm.uiState.first { it.step == AuthStep.PHONE }
        assertEquals(1, f.count("login"))

        f.vm.onMpinChanged("0000")
        f.clock.tap(); f.vm.submitMpin()

        advanceUntilIdle()
        assertEquals(1, f.count("login"))
        assertEquals(AuthStep.PHONE, f.vm.uiState.value.step)
        assertEquals("बहुत गलत MPIN — दोबारा OTP लें", f.vm.uiState.value.generalErrorMessage)
    }

    private fun newShopCase(shop: String, owner: String, mpin: String, confirm: String) = runTest {
        val f = fixture(signupOk)
        f.toNewShop()
        f.fillShop(shop, owner, mpin, confirm)
        f.clock.tap(); f.vm.submitNewShop()

        advanceUntilIdle()
        assertEquals(0, f.count("signup"))
        assertEquals(AuthStep.NEW_SHOP, f.vm.uiState.value.step)
        assertEquals(false, f.vm.uiState.value.isRegistrationComplete)
        assertTrue(f.vm.uiState.value.generalErrorMessage != null)
    }

    @Test fun newShop_shortShopName_rejected() = newShopCase("ab", "राम", "1234", "1234")
    @Test fun newShop_shortOwnerName_rejected() = newShopCase("राम आढ़त", "र", "1234", "1234")
    @Test fun newShop_mpinMismatch_rejected() = newShopCase("राम आढ़त", "राम", "1234", "1235")
    @Test fun newShop_mpinNotFourDigits_rejected() = newShopCase("राम आढ़त", "राम", "123", "123")

    private fun phoneCase(number: String) = runTest {
        val f = fixture { json(HttpStatusCode.OK, """{"sent":true,"cooldown_s":30}""") }
        f.vm.onPhoneNumberChanged(number)
        f.clock.tap(); f.vm.submitPhone()

        advanceUntilIdle()
        assertEquals(0, f.count("otp/send"))
        assertEquals(AuthStep.PHONE, f.vm.uiState.value.step)
        assertTrue(f.vm.uiState.value.generalErrorMessage != null)
    }

    @Test fun phone_startingWithFive_rejected() = phoneCase("5837123456")
    @Test fun phone_nineDigits_rejected() = phoneCase("983712345")

    @Test
    fun enterMpin_threeDigits_rejected() = runTest {
        val f = fixture { req ->
            val p = req.url.encodedPath
            if (p.endsWith("login")) json(HttpStatusCode.OK, sessionJson) else verifyBody(p, "EXISTING")
        }
        f.toEnterMpin()
        f.vm.onMpinChanged("123")
        f.clock.tap(); f.vm.submitMpin()

        advanceUntilIdle()
        assertEquals(0, f.count("login"))
        assertEquals(AuthStep.ENTER_MPIN, f.vm.uiState.value.step)
        assertTrue(f.vm.uiState.value.generalErrorMessage != null)
    }
}
