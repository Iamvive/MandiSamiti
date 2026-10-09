package com.appwork.mandisamiti.data.auth

import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.TransactionType
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRepositoryTest {
    private fun sessionJson(shopId: String) =
        """{"access_token":"acc","refresh_token":"ref","shop":{"id":"$shopId","shop_name":"नई दुकान","owner_name":"राम","mandi_name":"मथुरा","phone_number":"9837000000"}}"""

    private class Fixture(
        val db: AppDatabase,
        val store: InMemorySessionStore,
        val shops: OfflineFirstShopProfileRepository,
        val parties: OfflineFirstPartyRepository,
        val repo: AuthRepository,
    )

    private fun TestScope.fixture(serverShopId: String): Fixture {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        val engine = MockEngine {
            respond(sessionJson(serverShopId), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val store = InMemorySessionStore()
        val shops = OfflineFirstShopProfileRepository(db, io)
        val parties = OfflineFirstPartyRepository(db, io)
        val repo = AuthRepository(
            api = AuthApi(mandiHttpClient(engine), "https://api.test"),
            sessionStore = store,
            shopProfileRepository = shops,
            wiper = LocalDataWiper(db),
            ioDispatcher = io,
        )
        return Fixture(db, store, shops, parties, repo)
    }

    private suspend fun Fixture.seedLocalShop(id: String) {
        shops.saveShopProfile(
            ShopProfile(id = id, shopName = "Demo", ownerName = "O", mandiName = "M", phoneNumber = "9", pinHash = "", createdAt = 1L, updatedAt = 1L)
        )
        parties.saveParty(
            Party(id = "p1", shopId = id, name = "R", village = "V", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L)
        )
    }

    @Test
    fun signup_wipesDemoData_andSavesProfileAndSession() = runTest {
        val f = fixture("server-shop")
        f.seedLocalShop("shop_default")

        val result = f.repo.signup("pass", "नई दुकान", "राम", "मथुरा", "1234")

        assertTrue(result.isSuccess)
        assertEquals(0, f.db.appDatabaseQueries.getAllParties("shop_default").executeAsList().size)
        val profile = f.db.appDatabaseQueries.getShopProfile().executeAsOne()
        assertEquals("server-shop", profile.id)
        assertEquals("नई दुकान", profile.shop_name)
        assertEquals(Session("server-shop", "acc", "ref"), f.store.current())
    }

    @Test
    fun login_sameShop_keepsExistingParties() = runTest {
        val f = fixture("shop-1")
        f.seedLocalShop("shop-1")

        assertTrue(f.repo.login("pass", "1234").isSuccess)

        assertEquals(1, f.db.appDatabaseQueries.getAllParties("shop-1").executeAsList().size)
        assertEquals("shop-1", f.store.current()?.shopId)
    }

    @Test
    fun login_differentShop_wipesLocalData() = runTest {
        val f = fixture("shop-2")
        f.seedLocalShop("shop-1")

        assertTrue(f.repo.login("pass", "1234").isSuccess)

        assertEquals(0, f.db.appDatabaseQueries.getAllParties("shop-1").executeAsList().size)
        assertEquals("shop-2", f.db.appDatabaseQueries.getShopProfile().executeAsOne().id)
    }

    @Test
    fun login_withTwoLocalProfiles_wipesEvenWhenOneMatchesServerShop() = runTest {
        val f = fixture("shop-1")
        f.seedLocalShop("shop-1")
        f.shops.saveShopProfile(
            ShopProfile(id = "shop_mathura_default", shopName = "Demo2", ownerName = "O", mandiName = "M", phoneNumber = "9", pinHash = "", createdAt = 1L, updatedAt = 1L)
        )

        assertTrue(f.repo.login("pass", "1234").isSuccess)

        assertEquals(0, f.db.appDatabaseQueries.getAllParties("shop-1").executeAsList().size)
        val q = f.db.appDatabaseQueries
        assertEquals(1, q.countForeignRows("nobody").executeAsOne().toInt()) // exactly one profile row left
        assertEquals("shop-1", q.getShopProfile().executeAsOne().id)
        assertEquals(0, q.countForeignRows("shop-1").executeAsOne().toInt())
    }

    @Test
    fun login_sameShop_keepsPendingCashEntryAndItsRevision() = runTest {
        val f = fixture("shop-1")
        f.seedLocalShop("shop-1")
        val cash = OfflineFirstCashTransactionRepository(f.db, StandardTestDispatcher(testScheduler))
        cash.recordTransaction(
            CashTransaction(
                id = "t1", shopId = "shop-1", partyId = "p1", transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 500_00L, transactionDate = 1L, createdAt = 1L, updatedAt = 1L, syncStatus = 0,
            )
        )
        val q = f.db.appDatabaseQueries
        val revisionsBefore = q.getPendingSyncRevisions().executeAsList().size
        assertTrue(revisionsBefore > 0)

        assertTrue(f.repo.login("pass", "1234").isSuccess)

        assertEquals(1, q.getPendingSyncTransactions().executeAsList().size)
        assertEquals(revisionsBefore, q.getPendingSyncRevisions().executeAsList().size)
    }

    @Test
    fun login_withOrphanRowsUnderOtherShop_wipes() = runTest {
        val f = fixture("shop-1")
        f.seedLocalShop("shop-1")
        f.db.appDatabaseQueries.wipeShopProfiles() // parties now orphaned; shop-1 rows remain, none foreign
        f.parties.saveParty(
            Party(id = "p2", shopId = "other", name = "X", village = "V", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L)
        )

        assertTrue(f.repo.login("pass", "1234").isSuccess)

        assertEquals(0, f.db.appDatabaseQueries.getAllParties("other").executeAsList().size)
        assertEquals(0, f.db.appDatabaseQueries.getAllParties("shop-1").executeAsList().size)
    }

    @Test
    fun failedLogin_changesNothing() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.ContentType, "application/json")) }
        val store = InMemorySessionStore()
        val repo = AuthRepository(
            AuthApi(mandiHttpClient(engine), "https://api.test"), store,
            OfflineFirstShopProfileRepository(db, io), LocalDataWiper(db), ioDispatcher = io,
        )

        val r = repo.login("pass", "1234")

        assertTrue(r.isFailure)
        assertNull(store.current())
        assertNull(db.appDatabaseQueries.getShopProfile().executeAsOneOrNull())
    }

    private suspend fun Fixture.seedShopWithPendingCash(id: String, profileSyncStatus: Long, scope: TestScope) {
        shops.saveShopProfile(
            ShopProfile(id = id, shopName = "Other", ownerName = "O", mandiName = "M", phoneNumber = "9", pinHash = "",
                createdAt = 1L, updatedAt = 1L, syncStatus = profileSyncStatus.toInt())
        )
        parties.saveParty(
            Party(id = "p1", shopId = id, name = "R", village = "V", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L)
        )
        OfflineFirstCashTransactionRepository(db, StandardTestDispatcher(scope.testScheduler)).recordTransaction(
            CashTransaction(
                id = "t1", shopId = id, partyId = "p1", transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 500_00L, transactionDate = 1L, createdAt = 1L, updatedAt = 1L, syncStatus = 0,
            )
        )
    }

    @Test
    fun login_otherServerShopWithPendingEntries_isBlocked_andDataIntact() = runTest {
        val f = fixture("shop-2")
        f.seedShopWithPendingCash("shop-1", profileSyncStatus = 1L, scope = this)
        val q = f.db.appDatabaseQueries
        val revisionsBefore = q.getPendingSyncRevisions().executeAsList().size

        val r = f.repo.login("pass", "1234")

        assertEquals(AuthError.UnsyncedOtherShop, r.exceptionOrNull())
        assertNull(f.store.current())
        assertEquals("shop-1", q.getShopProfile().executeAsOne().id)
        assertEquals(1, q.getAllParties("shop-1").executeAsList().size)
        assertEquals(1, q.getPendingSyncTransactions().executeAsList().size)
        assertEquals(revisionsBefore, q.getPendingSyncRevisions().executeAsList().size)
    }

    @Test
    fun login_legacyUnsyncedDemoProfileWithPendingEntries_isStillWiped() = runTest {
        val f = fixture("shop-2")
        f.seedShopWithPendingCash("shop_default", profileSyncStatus = 0L, scope = this)

        assertTrue(f.repo.login("pass", "1234").isSuccess)

        val q = f.db.appDatabaseQueries
        assertEquals(0, q.getPendingSyncTransactions().executeAsList().size)
        assertEquals(0, q.getAllParties("shop_default").executeAsList().size)
        assertEquals("shop-2", q.getShopProfile().executeAsOne().id)
        assertEquals("shop-2", f.store.current()?.shopId)
    }
}
