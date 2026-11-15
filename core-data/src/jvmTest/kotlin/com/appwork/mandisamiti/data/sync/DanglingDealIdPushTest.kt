package com.appwork.mandisamiti.data.sync

import com.appwork.mandisamiti.data.auth.InMemorySessionStore
import com.appwork.mandisamiti.data.auth.Session
import com.appwork.mandisamiti.data.auth.mandiHttpClient
import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.sync.remote.KtorMandiSyncApiClient
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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertTrue

/** A payment pointing at a deal that no longer exists locally must not make the server reject the whole batch. */
class DanglingDealIdPushTest {
    @Test
    fun pushDropsDealIdWhenNoLocalDealExists() = runTest {
        val bodies = mutableListOf<String>()
        val mock = MockEngine { req ->
            (req.body as? TextContent)?.let { bodies += it.text }
            respond(
                """{"success":true,"synced_transactions":["tx1"],"server_sync_time":1,"server_seq":1}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val store = InMemorySessionStore().apply { save(Session("shop-1", "test-token", "r")) }
        val api = KtorMandiSyncApiClient(mandiHttpClient(mock), "https://api.test", store) {
            Result.failure(IllegalStateException("unexpected refresh"))
        }
        val db = createTestDatabase()
        OfflineFirstCashTransactionRepository(db).recordTransaction(
            CashTransaction(
                id = "tx1", shopId = "shop-1", partyId = "p1", dealId = "ghost-deal",
                transactionType = TransactionType.UDHAR_GIVEN, amountPaisa = 25_000L,
                transactionDate = 1000L, createdAt = 1000L, updatedAt = 1000L,
            )
        )

        SyncEngine(db, api).pushPendingChanges().getOrThrow()

        val body = bodies.map { Json.parseToJsonElement(it).jsonObject }.single { "transactions" in it }
        val row = body.getValue("transactions").jsonArray.single().jsonObject
        assertTrue(row["deal_id"] == null || row["deal_id"] == JsonNull, "deal_id sent: ${row["deal_id"]}")
    }
}
