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
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RegisterViewModelTest {
    private class TestClock(var ms: Long = 1_000_000L) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(ms)
        fun tap() { ms += 1_000L }
    }

    private val sessionJson =
        """{"access_token":"a","refresh_token":"r","shop":{"id":"s1","shop_name":"Shop","owner_name":"O","mandi_name":"M","phone_number":"9837123456"}}"""

    private fun MockRequestHandleScope.json(status: HttpStatusCode, body: String): HttpResponseData =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private class Fixture(val vm: RegisterViewModel, val clock: TestClock)

    private fun TestScope.fixture(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): Fixture {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        val repo = AuthRepository(
            api = AuthApi(mandiHttpClient(MockEngine { handler(it) }), "https://api.test"),
            sessionStore = InMemorySessionStore(),
            shopProfileRepository = OfflineFirstShopProfileRepository(db, io),
            wiper = LocalDataWiper(db),
            ioDispatcher = io,
        )
        val clock = TestClock()
        return Fixture(RegisterViewModel(repo, SoundboxTtsManager(), backgroundScope, clock), clock)
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
}
