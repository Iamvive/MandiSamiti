package com.appwork.mandisamiti.data.sync

import com.appwork.mandisamiti.data.auth.InMemorySessionStore
import com.appwork.mandisamiti.data.auth.Session
import com.appwork.mandisamiti.data.auth.mandiHttpClient
import com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstSellRepository
import com.appwork.mandisamiti.data.sync.remote.KtorMandiSyncApiClient
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** P3: after a reinstall the first pull restores old slips; the next number must continue after them. */
class SlipNumberRestoreTest {
    private fun engine(db: AppDatabase, responseBody: String): SyncEngine {
        val mock = MockEngine {
            respond(responseBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val store = InMemorySessionStore().apply { save(Session("shop-1", "test-token", "r")) }
        val api = KtorMandiSyncApiClient(mandiHttpClient(mock), "https://api.test", store) {
            Result.failure(IllegalStateException("unexpected refresh"))
        }
        return SyncEngine(db, api)
    }

    private val pull = """{"after_seq":0,"next_seq":9,"has_more":false,"server_sync_time":3000,
        "buy_entries":[
          {"id":"b1","farmer_id":"farmer-a","commodity_id":"wheat","entry_date":2000,"slip_no":1,"created_at":2000,"updated_at":2000},
          {"id":"b2","farmer_id":"farmer-a","commodity_id":"wheat","entry_date":2001,"slip_no":2,"created_at":2001,"updated_at":2001}],
        "sell_entries":[{"id":"s1","buyer_id":"buyer-a","commodity_id":"wheat","entry_date":2000,"slip_no":4,"created_at":2000,"updated_at":2000}],
        "lend_entries":[{"id":"l1","party_id":"p1","entry_date":2000,"slip_no":6,"created_at":2000,"updated_at":2000}]}"""

    private fun buy(id: String) = BuyEntry(
        id = id, shopId = "shop-1", farmerId = "farmer-a", commodityId = "wheat", entryDate = 5000L,
        bagsCount = 1L, weightPerBagGrams = 50_000L, extraWeightGrams = 0L, totalWeightGrams = 50_000L,
        ratePaisaPerQuintal = 200_000L, commissionBps = 0L, labourPerBagPaisa = 0L, advancePaidPaisa = 0L,
        grossPaisa = 100_000L, commissionPaisa = 0L, labourPaisa = 0L, netPayablePaisa = 100_000L,
        createdAt = 5000L, updatedAt = 5000L,
    )

    private fun sell(id: String) = SellEntry(
        id = id, shopId = "shop-1", buyerId = "buyer-a", commodityId = "wheat", entryDate = 5000L,
        billMode = BillMode.NORMAL, partyKind = SellPartyKind.MERCHANT, bagsCount = 1L,
        weightPerBagGrams = 50_000L, totalWeightGrams = 50_000L, ratePaisaPerQuintal = 200_000L,
        aratBps = 0L, tulakPaisa = 0L, grossPaisa = 100_000L, aratPaisa = 0L, mandiShulkPaisa = 0L,
        totalReceivablePaisa = 100_000L, receivedPaisa = 0L, balanceDuePaisa = 100_000L,
        createdAt = 5000L, updatedAt = 5000L,
    )

    private fun lend(id: String) = LendEntry(
        id = id, shopId = "shop-1", partyId = "p1", lendType = LendType.MONEY, entryDate = 5000L,
        amountPaisa = 100_000L, createdAt = 5000L, updatedAt = 5000L,
    )

    @Test
    fun nextBuySellLendNumbersContinueAfterRestoredSlips() = runTest {
        val db = createTestDatabase()
        db.appDatabaseQueries.insertShopProfile("shop-1", "दुकान", "मालिक", "मंडी", null, "9000000001", "", 1.5, 1L, 0L, 0L, 1L)
        engine(db, pull).pullRemoteChanges("shop-1").getOrThrow()

        val buys = OfflineFirstBuyRepository(db)
        val sells = OfflineFirstSellRepository(db)
        val lends = OfflineFirstLendRepository(db)
        buys.saveBuyEntry(buy("b3"))
        sells.saveSellEntry(sell("s2"))
        lends.saveLendEntry(lend("l2"))

        assertEquals(3L, buys.getBuyEntryById("b3")?.slipNo)
        assertEquals(5L, sells.getSellEntryById("s2")?.slipNo)
        assertEquals(7L, lends.getLendEntryById("l2")?.slipNo)
    }
}
