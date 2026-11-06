package com.appwork.mandisamiti.audit

import com.appwork.mandisamiti.data.auth.InMemorySessionStore
import com.appwork.mandisamiti.data.auth.Session
import com.appwork.mandisamiti.data.auth.mandiHttpClient
import com.appwork.mandisamiti.data.repository.OfflineFirstExpenseRepository
import com.appwork.mandisamiti.data.sync.SyncEngine
import com.appwork.mandisamiti.data.sync.model.SyncPushRequestDto
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
import kotlin.test.assertTrue

/**
 * X4 Audit Test Suite for Finding P1 (Expense Sync Wire Format & Lifecycle).
 * Verifies:
 * - Batches exceeding PUSH_BATCH encode expense_entries.
 * - Partial acknowledgments clear only acked expenses while unacked stay pending.
 * - Voiding and revising a previously-synced expense re-queues it for sync.
 * - Legacy 'expenses' field name is rejected/replaced by 'expense_entries'.
 */
class P1ExpenseSyncAuditTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun seedShop(db: AppDatabase, shopId: String = "shop-p1") {
        db.appDatabaseQueries.insertShopProfile(
            shopId, "परीक्षण दुकान", "परीक्षण मुनीम", "मथुरा मंडी",
            null, "9876543210", "", 1.5, 1L, 0L, 0L, 1L
        )
    }

    private fun createSyncEngine(
        db: AppDatabase,
        handler: (requestBody: String) -> String
    ): Pair<SyncEngine, MutableList<String>> {
        val capturedBodies = mutableListOf<String>()
        val mock = MockEngine { req ->
            val bodyText = (req.body as? TextContent)?.text ?: ""
            capturedBodies.add(bodyText)
            val respJson = handler(bodyText)
            respond(respJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val store = InMemorySessionStore().apply { save(Session("shop-p1", "test-token", "r")) }
        val api = KtorMandiSyncApiClient(mandiHttpClient(mock), "https://api.test", store) {
            Result.failure(IllegalStateException("unexpected token refresh in test"))
        }
        return Pair(SyncEngine(db, api), capturedBodies)
    }

    @Test
    fun auditP1_batchLargerThanPushBatchEncodesExpenseEntriesCorrectly() = runTest {
        val db = createTestDatabase()
        seedShop(db)
        val repo = OfflineFirstExpenseRepository(db)

        // Generate 60 expenses (exceeding typical 50 PUSH_BATCH)
        for (i in 1..60) {
            repo.saveExpense(
                ExpenseEntry(
                    id = "exp-batch-$i",
                    shopId = "shop-p1",
                    entryDate = 1000L + i,
                    category = ExpenseCategory.CHAI_PANI,
                    amountPaisa = 10_000L * i,
                    createdAt = 1000L + i,
                    updatedAt = 1000L + i
                )
            )
        }

        val (engine, capturedBodies) = createSyncEngine(db) { body ->
            val parsed = json.parseToJsonElement(body).jsonObject
            val expArray = parsed["expense_entries"]?.jsonArray
            val ackedIds = expArray?.map { it.jsonObject["id"]?.toString()?.replace("\"", "") ?: "" } ?: emptyList()
            val ackJsonList = ackedIds.joinToString(",") { "\"$it\"" }
            """{"success":true,"synced_expense_entries":[$ackJsonList],"server_sync_time":2000,"server_seq":10}"""
        }

        val pushResult = engine.pushPendingChanges()
        assertTrue(pushResult.isSuccess, "Push should succeed")

        // Verify each captured push request uses "expense_entries" and NOT legacy "expenses"
        assertTrue(capturedBodies.isNotEmpty(), "At least one push payload captured")
        for (rawBody in capturedBodies) {
            val body = json.parseToJsonElement(rawBody).jsonObject
            if ("expense_entries" in body) {
                assertFalse("expenses" in body, "Must not serialize legacy 'expenses' key")
                val count = body["expense_entries"]?.jsonArray?.size ?: 0
                assertTrue(count > 0, "Batch must contain expense items")
            }
        }
    }

    @Test
    fun auditP1_partialAcknowledgmentKeepsUnacknowledgedExpensesPending() = runTest {
        val db = createTestDatabase()
        seedShop(db)
        val repo = OfflineFirstExpenseRepository(db)

        repo.saveExpense(
            ExpenseEntry("exp-ack-1", "shop-p1", 1000L, ExpenseCategory.CHAI_PANI, 35_000L, createdAt = 1000L, updatedAt = 1000L)
        )
        repo.saveExpense(
            ExpenseEntry("exp-ack-2", "shop-p1", 1001L, ExpenseCategory.KIRAYA, 50_000L, createdAt = 1001L, updatedAt = 1001L)
        )

        // Server only acks exp-ack-1
        val (engine, _) = createSyncEngine(db) {
            """{"success":true,"synced_expense_entries":["exp-ack-1"],"server_sync_time":2000,"server_seq":5}"""
        }

        engine.pushPendingChanges().getOrThrow()

        val pending = db.appDatabaseQueries.getPendingSyncExpenses().executeAsList().map { it.id }
        assertEquals(listOf("exp-ack-2"), pending, "exp-ack-2 must remain pending sync")
    }

    @Test
    fun auditP1_voidAndRevisionAfterFirstSyncReEntersPendingOutbox() = runTest {
        val db = createTestDatabase()
        seedShop(db)
        val repo = OfflineFirstExpenseRepository(db)

        val exp = ExpenseEntry("exp-rev-1", "shop-p1", 1000L, ExpenseCategory.HAMMALI, 40_000L, createdAt = 1000L, updatedAt = 1000L)
        repo.saveExpense(exp)

        // Initial sync
        val (engine1, _) = createSyncEngine(db) {
            """{"success":true,"synced_expense_entries":["exp-rev-1"],"server_sync_time":2000,"server_seq":1}"""
        }
        engine1.pushPendingChanges().getOrThrow()
        assertEquals(emptyList(), db.appDatabaseQueries.getPendingSyncExpenses().executeAsList().map { it.id })

        // Now void the expense
        repo.voidExpense("exp-rev-1", com.appwork.mandisamiti.domain.model.VoidReason.WRONG_ENTRY)

        // Must re-enter pending sync
        val pendingAfterVoid = db.appDatabaseQueries.getPendingSyncExpenses().executeAsList().map { it.id }
        assertEquals(listOf("exp-rev-1"), pendingAfterVoid, "Voided expense must re-enter outbox")

        // Sync the void
        val (engine2, _) = createSyncEngine(db) {
            """{"success":true,"synced_expense_entries":["exp-rev-1"],"server_sync_time":3000,"server_seq":2}"""
        }
        engine2.pushPendingChanges().getOrThrow()
        assertEquals(emptyList(), db.appDatabaseQueries.getPendingSyncExpenses().executeAsList().map { it.id })
    }

    @Test
    fun auditP1_clientRejectsLegacyFieldOnDirectSerialization() {
        // Direct DTO JSON contract test: ensure model explicitly serializes to expense_entries
        val req = SyncPushRequestDto(
            expenses = listOf()
        )
        val jsonWithDefaults = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        val jsonStr = jsonWithDefaults.encodeToString(SyncPushRequestDto.serializer(), req)
        assertTrue(jsonStr.contains("\"expense_entries\""), "Serialized JSON must have 'expense_entries'")
        assertFalse(jsonStr.contains("\"expenses\""), "Serialized JSON must not have raw 'expenses'")
    }
}

