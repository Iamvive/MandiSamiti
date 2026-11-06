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

    private class Fixture(
        val vm: RegisterViewModel,
        val clock: TestClock,
        val calls: MutableList<Pair<String, String>>,
        val db: com.appwork.mandisamiti.database.AppDatabase,
    ) {
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
        return Fixture(RegisterViewModel(repo, SoundboxTtsManager(), backgroundScope, clock), clock, calls, db)
    }

    private fun Fixture.enterPhone() {
        vm.onPhoneNumberChanged("9837123456")
        clock.tap(); vm.submitPhone()
    }

    private fun MockRequestHandleScope.checkPhone(isNew: Boolean) =
        json(HttpStatusCode.OK, """{"is_new_user":$isNew}""")

    private suspend fun Fixture.toNewShop() {
        enterPhone()
        vm.uiState.first { it.step == AuthStep.NEW_SHOP && !it.isLoading }
    }

    private suspend fun Fixture.toEnterMpin() {
        enterPhone()
        vm.uiState.first { it.step == AuthStep.ENTER_MPIN && !it.isLoading }
    }

    private fun Fixture.fillShop(shop: String = "राम आढ़त", owner: String = "राम", mpin: String = "1234", confirm: String = "1234", mandi: String = "मथुरा") {
        vm.onShopNameChanged(shop); vm.onOwnerNameChanged(owner); vm.onMandiNameChanged(mandi)
        vm.onMpinChanged(mpin); vm.onConfirmMpinChanged(confirm)
    }

    private val signupOk: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { req ->
        if (req.url.encodedPath.endsWith("signup")) json(HttpStatusCode.OK, sessionJson) else checkPhone(true)
    }

    private fun loginHandler(loginResponse: MockRequestHandleScope.() -> HttpResponseData): MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { req ->
        if (req.url.encodedPath.endsWith("login")) loginResponse() else checkPhone(false)
    }

    @Test
    fun newPhone_goesToNewShop_thenSignupCompletes() = runTest {
        val f = fixture(signupOk)
        f.toNewShop()
        f.fillShop()
        f.clock.tap(); f.vm.submitNewShop()
        assertTrue(f.vm.uiState.first { it.isRegistrationComplete }.isRegistrationComplete)
        assertEquals(1, f.count("check-phone"))
    }

    @Test
    fun existingPhone_goesToEnterMpin_thenLoginCompletes() = runTest {
        val f = fixture(loginHandler { json(HttpStatusCode.OK, sessionJson) })
        f.toEnterMpin()
        f.vm.onMpinChanged("4321")
        f.clock.tap(); f.vm.submitMpin()
        f.vm.uiState.first { it.isRegistrationComplete }
        val body = f.bodyOf("login").single()
        assertTrue("\"phone\":\"9837123456\"" in body, body)
        assertTrue("\"mpin\":\"4321\"" in body, body)
    }

    @Test
    fun wrongMpin_showsAttemptsLeft_andStaysOnEnterMpin() = runTest {
        val f = fixture(loginHandler {
            json(HttpStatusCode.Unauthorized, """{"detail":{"code":"MPIN_INVALID","attempts_left":2}}""")
        })
        f.toEnterMpin()
        f.vm.onMpinChanged("0000")
        f.clock.tap(); f.vm.submitMpin()
        val s = f.vm.uiState.first { it.generalErrorMessage != null }
        assertEquals("MPIN गलत है — 2 कोशिश बाकी", s.generalErrorMessage)
        assertEquals(AuthStep.ENTER_MPIN, s.step)
        assertEquals("", s.mpin)
    }

    @Test
    fun accountLocked_returnsToPhone_with24hMessage() = runTest {
        val f = fixture(loginHandler { json(HttpStatusCode.Unauthorized, """{"detail":{"code":"ACCOUNT_LOCKED"}}""") })
        f.toEnterMpin()
        f.vm.onMpinChanged("4826")
        f.clock.tap(); f.vm.submitMpin()
        val s = f.vm.uiState.first { it.step == AuthStep.PHONE }
        assertEquals("बहुत ज़्यादा गलत MPIN — 24 घंटे बाद कोशिश करें", s.generalErrorMessage)
    }

    @Test
    fun accountDisabled_returnsToPhone_withContactOwnerMessage() = runTest {
        val f = fixture(loginHandler { json(HttpStatusCode.Unauthorized, """{"detail":{"code":"ACCOUNT_DISABLED"}}""") })
        f.toEnterMpin()
        f.vm.onMpinChanged("4826")
        f.clock.tap(); f.vm.submitMpin()
        val s = f.vm.uiState.first { it.step == AuthStep.PHONE }
        assertEquals("यह खाता बंद है — दुकान मालिक से संपर्क करें", s.generalErrorMessage)
    }

    @Test
    fun signup_phoneAlreadyRegistered_returnsToPhone() = runTest {
        val f = fixture { req ->
            if (req.url.encodedPath.endsWith("signup")) json(HttpStatusCode.Conflict, "{}") else checkPhone(true)
        }
        f.toNewShop()
        f.fillShop()
        f.clock.tap(); f.vm.submitNewShop()
        val s = f.vm.uiState.first { it.step == AuthStep.PHONE }
        assertEquals("यह नंबर पहले से रजिस्टर है — दोबारा कोशिश करें", s.generalErrorMessage)
    }

    @Test
    fun signup_sendsShopFieldsAndMpin() = runTest {
        val f = fixture(signupOk)
        f.toNewShop()
        f.fillShop()
        f.clock.tap(); f.vm.submitNewShop()
        f.vm.uiState.first { it.isRegistrationComplete }
        val body = f.bodyOf("signup").single()
        assertTrue("\"phone\":\"9837123456\"" in body, body)
        assertTrue("owner_name" in body && "mandi_name" in body && "shop_name" in body, body)
        assertTrue("\"mpin\":\"1234\"" in body, body)
    }

    @Test
    fun signup_422_showsCheckDetailsMessage() = runTest {
        val f = fixture { req ->
            if (req.url.encodedPath.endsWith("signup")) json(HttpStatusCode.UnprocessableEntity, """{"detail":[]}""") else checkPhone(true)
        }
        f.toNewShop()
        f.fillShop()
        f.clock.tap(); f.vm.submitNewShop()
        val s = f.vm.uiState.first { it.generalErrorMessage != null }
        assertEquals("जानकारी जाँचें और दोबारा कोशिश करें", s.generalErrorMessage)
        assertEquals(AuthStep.NEW_SHOP, s.step)
    }

    @Test
    fun networkFailure_showsNoNetworkMessage() = runTest {
        val f = fixture { json(HttpStatusCode.ServiceUnavailable, "{}") }
        f.enterPhone()
        val s = f.vm.uiState.first { it.generalErrorMessage != null }
        assertEquals("नेटवर्क नहीं है — दोबारा कोशिश करें", s.generalErrorMessage)
        assertEquals(AuthStep.PHONE, s.step)
    }

    @Test
    fun rapidSecondTap_isThrottled() = runTest {
        val f = fixture { checkPhone(true) }
        f.vm.onPhoneNumberChanged("9837123456")
        f.clock.tap(); f.vm.submitPhone()
        f.vm.submitPhone() // same instant: throttled
        f.vm.uiState.first { it.step == AuthStep.NEW_SHOP && !it.isLoading }
        assertEquals(1, f.count("check-phone"))
        assertNull(f.vm.uiState.value.generalErrorMessage)
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

    @Test
    fun newShop_blankMandiName_rejectedWithFieldError_noNetwork() = runTest {
        val f = fixture(signupOk)
        f.toNewShop()
        f.fillShop(mandi = "   ")
        f.clock.tap(); f.vm.submitNewShop()
        advanceUntilIdle()
        assertEquals(0, f.count("signup"))
        assertEquals("मंडी का नाम लिखें", f.vm.uiState.value.mandiNameError)
    }

    private fun phoneCase(number: String) = runTest {
        val f = fixture { checkPhone(true) }
        f.vm.onPhoneNumberChanged(number)
        f.clock.tap(); f.vm.submitPhone()
        advanceUntilIdle()
        assertEquals(0, f.count("check-phone"))
        assertEquals(AuthStep.PHONE, f.vm.uiState.value.step)
        assertTrue(f.vm.uiState.value.generalErrorMessage != null)
    }

    @Test fun phone_startingWithFive_rejected() = phoneCase("5837123456")
    @Test fun phone_nineDigits_rejected() = phoneCase("983712345")

    @Test
    fun enterMpin_threeDigits_rejected() = runTest {
        val f = fixture(loginHandler { json(HttpStatusCode.OK, sessionJson) })
        f.toEnterMpin()
        f.vm.onMpinChanged("123")
        f.clock.tap(); f.vm.submitMpin()
        advanceUntilIdle()
        assertEquals(0, f.count("login"))
        assertEquals(AuthStep.ENTER_MPIN, f.vm.uiState.value.step)
        assertTrue(f.vm.uiState.value.generalErrorMessage != null)
    }

    @Test
    fun login_otherShopUnsynced_returnsToPhone_withSyncFirstMessage_andKeepsData() = runTest {
        val f = fixture { req ->
            val p = req.url.encodedPath
            if (p.endsWith("login")) json(HttpStatusCode.OK, sessionJson) else checkPhone(false)
        }
        val io = StandardTestDispatcher(testScheduler)
        com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository(f.db, io).saveShopProfile(
            com.appwork.mandisamiti.domain.model.ShopProfile(
                id = "other-shop", shopName = "Other", ownerName = "O", mandiName = "M", phoneNumber = "9",
                pinHash = "", createdAt = 1L, updatedAt = 1L, syncStatus = 1,
            )
        )
        com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository(f.db, io).saveParty(
            com.appwork.mandisamiti.domain.model.Party(
                id = "p1", shopId = "other-shop", name = "R", village = "V",
                partyType = com.appwork.mandisamiti.domain.model.PartyType.FARMER, createdAt = 1L, updatedAt = 1L,
            )
        )
        f.toEnterMpin()
        f.vm.onMpinChanged("4826")
        f.clock.tap(); f.vm.submitMpin()

        val s = f.vm.uiState.first { it.step == AuthStep.PHONE }
        assertEquals(
            "इस फ़ोन पर दूसरी दुकान की प्रविष्टियाँ अभी सर्वर पर नहीं गईं — पहले उस दुकान से लॉगिन करके सिंक करें",
            s.generalErrorMessage,
        )
        assertEquals(false, s.isRegistrationComplete)
        assertEquals(1, f.db.appDatabaseQueries.getAllParties("other-shop").executeAsList().size)
    }

    @Test
    fun login_success_exposesSessionFromRepository() = runTest {
        val f = fixture { req ->
            val p = req.url.encodedPath
            if (p.endsWith("login")) json(HttpStatusCode.OK, sessionJson) else checkPhone(false)
        }
        f.toEnterMpin()
        f.vm.onMpinChanged("4826")
        f.clock.tap(); f.vm.submitMpin()

        val s = f.vm.uiState.first { it.isRegistrationComplete }
        assertEquals(com.appwork.mandisamiti.data.auth.Session("s1", "a", "r"), s.completedSession)
    }

    @Test
    fun initialPhoneNumberPrefillsPhoneNumber() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        val repo = AuthRepository(
            api = AuthApi(mandiHttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }), "https://api.test"),
            sessionStore = InMemorySessionStore(),
            shopProfileRepository = OfflineFirstShopProfileRepository(db, io),
            wiper = LocalDataWiper(db),
            ioDispatcher = io,
        )
        val vm = RegisterViewModel(
            authRepository = repo,
            ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope,
            initialPhoneNumber = "9876543210"
        )
        assertEquals("9876543210", vm.uiState.value.phoneNumber)
    }
}
