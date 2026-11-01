package com.appwork.mandisamiti.data.sync

import com.appwork.mandisamiti.data.auth.InMemorySessionStore
import com.appwork.mandisamiti.data.auth.Session
import com.appwork.mandisamiti.data.auth.mandiHttpClient
import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.sync.remote.KtorMandiSyncApiClient
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.TransactionType
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A payment row remembers which Buy or Sell slip it pays, through the real JSON and the backend's field names. */
class PaymentLinkWireTest {
    private val pushBodies = mutableListOf<String>()

    private fun engine(db: AppDatabase, responseBody: String): SyncEngine {
        val mock = MockEngine { req ->
            (req.body as? TextContent)?.let { pushBodies += it.text }
            respond(responseBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val store = InMemorySessionStore().apply { save(Session("shop-1", "test-token", "r")) }
        val api = KtorMandiSyncApiClient(mandiHttpClient(mock), "https://api.test", store) {
            Result.failure(IllegalStateException("unexpected refresh"))
        }
        return SyncEngine(db, api)
    }

    private fun seedShop(db: AppDatabase) =
        db.appDatabaseQueries.insertShopProfile("shop-1", "दुकान", "मालिक", "मंडी", null, "9000000001", "", 1.5, 1L, 0L, 0L, 1L)

    private fun tx(sourceType: String?, sourceId: String?) = CashTransaction(
        id = "tx1", shopId = "shop-1", partyId = "p1", transactionType = TransactionType.UDHAR_GIVEN,
        amountPaisa = 25_000L, transactionDate = 1000L, createdAt = 1000L, updatedAt = 1000L,
        sourceEntryType = sourceType, sourceEntryId = sourceId,
    )

    @Test
    fun savedLinkIsReadBack() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstCashTransactionRepository(db)
        repo.recordTransaction(tx("BUY", "buy-1"))

        val saved = db.appDatabaseQueries.getCashTransactionById("tx1").executeAsOneOrNull()
        assertEquals("BUY", saved?.source_entry_type)
        assertEquals("buy-1", saved?.source_entry_id)
    }

    @Test
    fun pushSendsTheLink() = runTest {
        val db = createTestDatabase()
        OfflineFirstCashTransactionRepository(db).recordTransaction(tx("BUY", "buy-1"))
        engine(db, """{"success":true,"synced_transactions":["tx1"],"server_sync_time":1,"server_seq":1}""")
            .pushPendingChanges().getOrThrow()

        val body = pushBodies.map { Json.parseToJsonElement(it).jsonObject }.single { "transactions" in it }
        val row = body.getValue("transactions").jsonArray.single().jsonObject
        assertEquals("BUY", row.getValue("source_entry_type").jsonPrimitive.content)
        assertEquals("buy-1", row.getValue("source_entry_id").jsonPrimitive.content)
    }

    @Test
    fun pullRestoresTheLink() = runTest {
        val db = createTestDatabase()
        seedShop(db)
        val row = """{"id":"tx9","shop_id":"shop-1","party_id":"p1","transaction_type":"UDHAR_GIVEN","amount_paisa":25000,
            "payment_mode":"CASH","source_entry_type":"SELL","source_entry_id":"sell-4","revision":1,"created_at":2000,"updated_at":2000}"""
        engine(db, """{"after_seq":0,"next_seq":3,"has_more":false,"transactions":[$row],"server_sync_time":3000}""")
            .pullRemoteChanges("shop-1").getOrThrow()

        val saved = db.appDatabaseQueries.getCashTransactionById("tx9").executeAsOneOrNull()
        assertEquals("SELL", saved?.source_entry_type)
        assertEquals("sell-4", saved?.source_entry_id)
    }

    @Test
    fun rowFromOldServerHasNoLink() = runTest {
        val db = createTestDatabase()
        seedShop(db)
        val row = """{"id":"tx8","shop_id":"shop-1","party_id":"p1","transaction_type":"UDHAR_GIVEN","amount_paisa":100,
            "revision":1,"created_at":2000,"updated_at":2000}"""
        engine(db, """{"after_seq":0,"next_seq":3,"has_more":false,"transactions":[$row],"server_sync_time":3000}""")
            .pullRemoteChanges("shop-1").getOrThrow()

        assertNull(db.appDatabaseQueries.getCashTransactionById("tx8").executeAsOneOrNull()?.source_entry_type)
    }
}
