package com.appwork.mandisamiti.data.sync

import com.appwork.mandisamiti.data.auth.InMemorySessionStore
import com.appwork.mandisamiti.data.auth.Session
import com.appwork.mandisamiti.data.auth.mandiHttpClient
import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.data.sync.model.CashTransactionSyncDto
import com.appwork.mandisamiti.data.sync.model.LendEntrySyncDto
import com.appwork.mandisamiti.data.sync.remote.KtorMandiSyncApiClient
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendKind
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.VoidReason
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Udhaar direction, kind, receiver and the cash-to-loan link survive the real JSON in both directions. */
class UdhaarWireTest {
    private val pushBodies = mutableListOf<String>()
    private val lenient = Json { ignoreUnknownKeys = true }

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
        db.appDatabaseQueries.insertShopProfile("shop-1", "दुकान", "मालिक", "मंडी", null, "9000000001", "", 1.5, 1L, 0L, 0L, 1L, null, null, null)

    private fun pushed(key: String): List<JsonObject> =
        pushBodies.map { Json.parseToJsonElement(it).jsonObject }.filter { key in it }
            .flatMap { it.getValue(key).jsonArray.map { e -> e.jsonObject } }

    private fun lend(id: String = "l1", direction: LendDirection = LendDirection.GIVEN, kind: LendKind = LendKind.LOAN,
                     name: String? = null, phone: String? = null) = LendEntry(
        id = id, shopId = "shop-1", partyId = "p1", lendType = LendType.MONEY, entryDate = 1000L, amountPaisa = 100_000L,
        createdAt = 1000L, updatedAt = 1000L, monthlyRatePercent = 2.0,
        direction = direction, kind = kind, receiverName = name, receiverPhone = phone,
    )

    private fun lendRow(id: String, extra: String = "") =
        """{"id":"$id","shop_id":"shop-1","party_id":"p1","amount_paisa":5000,"revision":1,"created_at":2000,"updated_at":2000$extra}"""

    private suspend fun pullLends(db: AppDatabase, vararg rows: String) =
        engine(db, """{"after_seq":0,"next_seq":3,"has_more":false,"lend_entries":[${rows.joinToString(",")}],"server_sync_time":3000}""")
            .pullRemoteChanges("shop-1").getOrThrow()

    @Test
    fun pushSendsDirectionKindAndReceiver() = runTest {
        val db = createTestDatabase()
        OfflineFirstLendRepository(db).saveLendEntry(lend(direction = LendDirection.TAKEN, kind = LendKind.PESGI, name = "रामू", phone = "9876543210"))
        engine(db, """{"success":true,"synced_lend_entries":["l1"],"server_sync_time":1,"server_seq":1}""").pushPendingChanges().getOrThrow()

        val row = pushed("lend_entries").single()
        assertEquals("TAKEN", row.getValue("direction").jsonPrimitive.content)
        assertEquals("PESGI", row.getValue("kind").jsonPrimitive.content)
        assertEquals("रामू", row.getValue("receiver_name").jsonPrimitive.content)
        assertEquals("9876543210", row.getValue("receiver_phone").jsonPrimitive.content)
    }

    @Test
    fun pullRestoresDirectionKindAndReceiver() = runTest {
        val db = createTestDatabase()
        seedShop(db)
        pullLends(db, lendRow("l9", ""","direction":"TAKEN","kind":"PESGI","receiver_name":"रामू","receiver_phone":"9876543210""""))

        val saved = db.appDatabaseQueries.getLendEntryById("l9").executeAsOne()
        assertEquals("TAKEN", saved.direction)
        assertEquals("PESGI", saved.kind)
        assertEquals("रामू", saved.receiver_name)
        assertEquals("9876543210", saved.receiver_phone)
    }

    @Test
    fun pushSendsCashLendLink() = runTest {
        val db = createTestDatabase()
        OfflineFirstCashTransactionRepository(db).recordTransaction(
            CashTransaction(id = "tx1", shopId = "shop-1", partyId = "p1", transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 500L, transactionDate = 1000L, createdAt = 1000L, updatedAt = 1000L, lendId = "l1"),
        )
        engine(db, """{"success":true,"synced_transactions":["tx1"],"server_sync_time":1,"server_seq":1}""").pushPendingChanges().getOrThrow()

        assertEquals("l1", pushed("transactions").single().getValue("lend_id").jsonPrimitive.content)
    }

    @Test
    fun pullRestoresCashLendLink() = runTest {
        val db = createTestDatabase()
        seedShop(db)
        val row = """{"id":"tx9","shop_id":"shop-1","party_id":"p1","transaction_type":"JAMA_RECEIVED","amount_paisa":500,
            "lend_id":"l1","revision":1,"created_at":2000,"updated_at":2000}"""
        engine(db, """{"after_seq":0,"next_seq":3,"has_more":false,"transactions":[$row],"server_sync_time":3000}""")
            .pullRemoteChanges("shop-1").getOrThrow()

        assertEquals("l1", db.appDatabaseQueries.getCashTransactionById("tx9").executeAsOne().lend_id)
    }

    @Test
    fun oldClientJsonDecodesWithDefaults() {
        val l = lenient.decodeFromString<LendEntrySyncDto>("""{"id":"a","party_id":"p"}""")
        assertEquals("GIVEN", l.direction)
        assertEquals("LOAN", l.kind)
        assertNull(l.receiver_name)
        assertNull(l.receiver_phone)
        val t = lenient.decodeFromString<CashTransactionSyncDto>("""{"id":"a","transaction_type":"JAMA_RECEIVED","amount_paisa":1}""")
        assertNull(t.lend_id)
    }

    @Test
    fun unknownKindFallsBackToLoanInsteadOfCrashing() = runTest {
        val db = createTestDatabase()
        seedShop(db)
        pullLends(db, lendRow("l7", ""","kind":"FUTURE","direction":"SIDEWAYS""""))

        val saved = db.appDatabaseQueries.getLendEntryById("l7").executeAsOne()
        assertEquals("LOAN", saved.kind)
        assertEquals("GIVEN", saved.direction)
        // The repository can read it back without valueOf blowing up.
        assertEquals(LendKind.LOAN, OfflineFirstLendRepository(db).getLendEntryById("l7")?.kind)
    }

    @Test
    fun voidedSettlementAndItsCashRowsArePushedVoid() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstLendRepository(db)
        repo.saveLendEntry(lend())
        val cash = CashTransaction(id = "c1", shopId = "shop-1", partyId = "p1", transactionType = TransactionType.JAMA_RECEIVED,
            amountPaisa = 40_000L, transactionDate = 2000L, createdAt = 2000L, updatedAt = 2000L, lendId = "l1", remarks = "[LEND:l1]")
        repo.settle(
            LendSettlement(id = "s1", shopId = "shop-1", lendId = "l1", settledAt = 2000L, amountPaisa = 40_000L,
                createdAt = 2000L, updatedAt = 2000L, principalPaisa = 40_000L),
            listOf(cash),
        )
        // Mark everything synced, then void the loan: settlement and cash row must go out again as void.
        db.appDatabaseQueries.markLendEntrySynced("l1", 1L)
        db.appDatabaseQueries.markLendSettlementSynced("s1", 1L)
        db.appDatabaseQueries.markTransactionSynced("c1", 1L)
        repo.voidLendEntry("l1", VoidReason.WRONG_ENTRY)

        engine(db, """{"success":true,"synced_lend_entries":["l1"],"synced_lend_settlements":["s1"],"synced_transactions":["c1"],"server_sync_time":1,"server_seq":1}""")
            .pushPendingChanges().getOrThrow()

        assertEquals(1, pushed("lend_settlements").single { it.getValue("id").jsonPrimitive.content == "s1" }.getValue("is_void").jsonPrimitive.content.toInt())
        assertEquals(1, pushed("lend_entries").single().getValue("is_void").jsonPrimitive.content.toInt())
        assertEquals(1, pushed("transactions").single { it.getValue("id").jsonPrimitive.content == "c1" }.getValue("is_void").jsonPrimitive.content.toInt())
    }
}
