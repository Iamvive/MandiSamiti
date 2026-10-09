package com.appwork.mandisamiti.ui.settings

import com.appwork.mandisamiti.data.auth.AuthApi
import com.appwork.mandisamiti.data.auth.InMemorySessionStore
import com.appwork.mandisamiti.data.auth.LocalDataWiper
import com.appwork.mandisamiti.data.auth.Session
import com.appwork.mandisamiti.data.auth.mandiHttpClient
import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.data.sync.SyncEngine
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.TransactionType
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LogoutUseCaseTest {
    private val shopId = "srv-shop-1"
    private val session = Session(shopId, "acc", "ref-1")

    private class Fixture(
        val db: AppDatabase,
        val store: InMemorySessionStore,
        val requests: MutableList<HttpRequestData>,
        val logout: LogoutUseCase,
    )

    private suspend fun TestScope.fixture(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = {
            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        },
    ): Fixture {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { req -> requests += req; handler(req) }
        val store = InMemorySessionStore().apply { save(session) }
        OfflineFirstShopProfileRepository(db, io).saveShopProfile(
            ShopProfile(
                id = shopId, shopName = "S", ownerName = "O", mandiName = "M",
                phoneNumber = "9837000000", pinHash = "", createdAt = 1L, updatedAt = 1L
            )
        )
        OfflineFirstPartyRepository(db, io).saveParty(
            Party(
                id = "farmer-1", shopId = shopId, name = "रामवीर", village = "राया",
                partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L
            )
        )
        val logout = LogoutUseCase(
            syncEngine = SyncEngine(db, ioDispatcher = io),
            authApi = AuthApi(mandiHttpClient(engine), "https://api.test"),
            sessionStore = store,
            wiper = LocalDataWiper(db),
        )
        return Fixture(db, store, requests, logout)
    }

    private fun AppDatabase.partyCount() = appDatabaseQueries.getAllParties(shopId).executeAsList().size

    @Test
    fun pendingEntryBlocksLogoutAndChangesNothing() = runTest {
        val f = fixture()
        assertEquals(1L, SyncEngine(f.db).getPendingCount())

        val result = f.logout()

        assertEquals(LogoutResult.Blocked(1), result)
        assertEquals(session, f.store.current())
        assertEquals(1, f.db.partyCount())
        assertNotNull(f.db.appDatabaseQueries.getShopProfile().executeAsOneOrNull())
        assertTrue(f.requests.isEmpty(), "blocked logout must not call the server")
    }

    @Test
    fun nothingPendingWipesDataClearsSessionAndRevokesToken() = runTest {
        val f = fixture()
        f.db.appDatabaseQueries.markPartySynced("farmer-1")

        val result = f.logout()

        assertEquals(LogoutResult.LoggedOut, result)
        assertNull(f.store.current())
        assertEquals(0, f.db.partyCount())
        assertNull(f.db.appDatabaseQueries.getShopProfile().executeAsOneOrNull())
        assertEquals(1, f.requests.size)
        assertTrue(f.requests[0].url.encodedPath.endsWith("/api/v1/auth/logout"))
        assertTrue((f.requests[0].body as TextContent).text.contains("ref-1"))
    }

    @Test
    fun networkErrorStillLogsOut() = runTest {
        val f = fixture { throw java.io.IOException("no network") }
        f.db.appDatabaseQueries.markPartySynced("farmer-1")

        val result = f.logout()

        assertEquals(LogoutResult.LoggedOut, result)
        assertNull(f.store.current())
        assertEquals(0, f.db.partyCount())
    }

    @Test
    fun sessionIsClearedBeforeLocalDataIsWiped() = runTest {
        val f = fixture()
        f.db.appDatabaseQueries.markPartySynced("farmer-1")
        var partiesAtClear = -1
        val recordingStore = object : com.appwork.mandisamiti.data.auth.SessionStore by f.store {
            override fun clear() {
                partiesAtClear = f.db.partyCount()
                f.store.clear()
            }
        }
        val logout = LogoutUseCase(
            syncEngine = SyncEngine(f.db, ioDispatcher = StandardTestDispatcher(testScheduler)),
            authApi = AuthApi(mandiHttpClient(MockEngine { respond("", HttpStatusCode.NoContent) }), "https://api.test"),
            sessionStore = recordingStore,
            wiper = LocalDataWiper(f.db),
        )

        assertEquals(LogoutResult.LoggedOut, logout())

        // A crash between the two steps must leave "no session" (safe), never "session but empty DB".
        assertEquals(1, partiesAtClear, "session must be cleared while local data still exists")
        assertNull(f.store.current())
        assertEquals(0, f.db.partyCount())
    }

    @Test
    fun oneCashEntryReportsOneEntryNotRows() = runTest {
        val f = fixture()
        f.db.appDatabaseQueries.markPartySynced("farmer-1")
        OfflineFirstCashTransactionRepository(f.db, StandardTestDispatcher(testScheduler)).recordTransaction(
            CashTransaction(
                id = "tx-1", shopId = shopId, partyId = "farmer-1",
                transactionType = TransactionType.UDHAR_GIVEN, amountPaisa = 100_00L,
                transactionDate = 1L, createdAt = 1L, updatedAt = 1L
            )
        )
        // Row + its revision are both unsynced (the old count was 2)
        assertEquals(1, f.db.appDatabaseQueries.getPendingSyncTransactions().executeAsList().size)
        assertEquals(1, f.db.appDatabaseQueries.getPendingSyncRevisions().executeAsList().size)

        val result = f.logout()

        assertEquals(LogoutResult.Blocked(1), result)
        assertEquals(session, f.store.current())
        assertEquals(1, f.db.partyCount())
        assertEquals(1, f.db.appDatabaseQueries.getPendingSyncTransactions().executeAsList().size)
    }

    @Test
    fun pendingRevisionAloneStillBlocksLogout() = runTest {
        val f = fixture()
        val q = f.db.appDatabaseQueries
        q.markPartySynced("farmer-1")
        OfflineFirstCashTransactionRepository(f.db, StandardTestDispatcher(testScheduler)).recordTransaction(
            CashTransaction(
                id = "tx-1", shopId = shopId, partyId = "farmer-1",
                transactionType = TransactionType.UDHAR_GIVEN, amountPaisa = 100_00L,
                transactionDate = 1L, createdAt = 1L, updatedAt = 1L
            )
        )
        q.markTransactionSynced("tx-1", 1L)
        assertEquals(0, q.getPendingSyncTransactions().executeAsList().size)
        assertEquals(1, q.getPendingSyncRevisions().executeAsList().size)

        val result = f.logout()

        assertTrue(result is LogoutResult.Blocked, "unsynced revision must block logout, was $result")
        assertEquals(session, f.store.current())
        assertEquals(1, f.db.partyCount())
    }
}
