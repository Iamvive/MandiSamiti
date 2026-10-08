package com.appwork.mandisamiti.data.auth

import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.ShopProfile
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
}
