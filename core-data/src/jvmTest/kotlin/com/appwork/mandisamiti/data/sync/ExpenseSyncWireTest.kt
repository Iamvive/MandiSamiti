package com.appwork.mandisamiti.data.sync

import com.appwork.mandisamiti.data.auth.InMemorySessionStore
import com.appwork.mandisamiti.data.auth.Session
import com.appwork.mandisamiti.data.auth.mandiHttpClient
import com.appwork.mandisamiti.data.repository.OfflineFirstExpenseRepository
import com.appwork.mandisamiti.data.sync.remote.KtorMandiSyncApiClient
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.ExpenseCategory
import com.appwork.mandisamiti.domain.model.ExpenseEntry
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Runs expense sync through the real HTTP client and JSON config, using the field names in
 * backend/app/schemas/sync.py. The fake API client in SyncEngineTest skips JSON, so it cannot see a wire-name mismatch.
 */
class ExpenseSyncWireTest {
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

    private suspend fun saveExpense(db: AppDatabase) = OfflineFirstExpenseRepository(db).saveExpense(
        ExpenseEntry(
            id = "e1", shopId = "shop-1", entryDate = 1000L, category = ExpenseCategory.HAMMALI,
            amountPaisa = 50_000L, createdAt = 1000L, updatedAt = 1000L,
        )
    )

    @Test
    fun pushSendsExpensesUnderServerFieldName() = runTest {
        val db = createTestDatabase()
        saveExpense(db)
        engine(db, """{"success":true,"synced_expense_entries":["e1"],"server_sync_time":1,"server_seq":1}""")
            .pushPendingChanges().getOrThrow()

        val body = pushBodies.map { Json.parseToJsonElement(it).jsonObject }.single { "expense_entries" in it }
        assertEquals(1, body.getValue("expense_entries").jsonArray.size)
        assertFalse("expenses" in body, "server ignores the old field name")
    }

    @Test
    fun serverAckClearsPendingExpense() = runTest {
        val db = createTestDatabase()
        saveExpense(db)
        val engine = engine(db, """{"success":true,"synced_expense_entries":["e1"],"server_sync_time":1,"server_seq":1}""")
        assertEquals(listOf("e1"), db.appDatabaseQueries.getPendingSyncExpenses().executeAsList().map { it.id })

        engine.pushPendingChanges().getOrThrow()

        assertEquals(emptyList(), db.appDatabaseQueries.getPendingSyncExpenses().executeAsList().map { it.id })
    }

    @Test
    fun pullRestoresExpenseFromServerResponseShape() = runTest {
        val db = createTestDatabase()
        seedShop(db)
        // Same shape as ExpenseEntryResponse, including the server-only keys the DTO does not declare.
        val row = """{"id":"e9","shop_id":"shop-1","sync_version":1,"server_seq":7,"entry_date":2000,"category":"HAMMALI",
            "amount_paisa":35000,"paid_to":null,"payment_mode":"CASH","remarks":null,"is_void":0,"void_reason":null,
            "revision":1,"is_deleted":0,"created_at":2000,"updated_at":2000}"""
        val engine = engine(db, """{"after_seq":0,"next_seq":7,"has_more":false,"expense_entries":[$row],"server_sync_time":3000}""")

        engine.pullRemoteChanges("shop-1").getOrThrow()
        engine.pullRemoteChanges("shop-1").getOrThrow() // same row again must stay one row

        val rows = db.appDatabaseQueries.getExpensesByShop("shop-1").executeAsList()
        assertEquals(listOf("e9"), rows.map { it.id })
        assertEquals(35_000L, rows.single().amount_paisa)
        assertEquals(1L, rows.single().sync_status)
        assertEquals(0L, engine.getPendingCount())
    }
}
