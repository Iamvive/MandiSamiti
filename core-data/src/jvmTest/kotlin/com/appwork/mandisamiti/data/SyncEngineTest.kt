package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstDealRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.sync.SyncEngine
import com.appwork.mandisamiti.data.sync.model.*
import com.appwork.mandisamiti.data.sync.remote.MandiSyncApiClient
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeSyncApiClient : MandiSyncApiClient {
    var lastPushedRequest: SyncPushRequestDto? = null
    var pushResponseToReturn: Result<SyncPushResponseDto> = Result.success(SyncPushResponseDto(success = true))
    var pullResponseToReturn: Result<SyncPullResponseDto> = Result.success(SyncPullResponseDto())

    override suspend fun pushSync(request: SyncPushRequestDto): Result<SyncPushResponseDto> {
        lastPushedRequest = request
        return pushResponseToReturn
    }

    override suspend fun pullSync(sinceMs: Long): Result<SyncPullResponseDto> {
        return pullResponseToReturn
    }
}

class SyncEngineTest {

    @Test
    fun testPendingSyncSummaryAndPushReconciliation() = runTest {
        val database = createTestDatabase()
        val partyRepo = OfflineFirstPartyRepository(database)
        val dealRepo = OfflineFirstDealRepository(database)
        val cashRepo = OfflineFirstCashTransactionRepository(database)
        val fakeClient = FakeSyncApiClient()
        val syncEngine = SyncEngine(database = database, apiClient = fakeClient)

        // 1. Create a party, a deal, and a transaction
        partyRepo.saveParty(Party(id = "farmer-1", shopId = "shop-1", name = "रामवीर सिंह", village = "राया", partyType = PartyType.FARMER, createdAt = 1000L, updatedAt = 1000L))
        partyRepo.saveParty(Party(id = "buyer-1", shopId = "shop-1", name = "अग्रवाल ट्रेडर्स", village = "मथुरा", partyType = PartyType.BUYER, createdAt = 1000L, updatedAt = 1000L))
        
        dealRepo.saveDeal(
            Deal(
                id = "deal-1",
                shopId = "shop-1",
                farmerId = "farmer-1",
                buyerId = "buyer-1",
                commodityId = "comm-1",
                dealDate = 1000L,
                bagsCount = 10,
                grossWeightGrams = 500000L,
                netWeightGrams = 500000L,
                ratePaisaPerUnit = 227550L,
                grossAmountPaisa = 1137750L,
                netFarmerPayablePaisa = 1100000L,
                netBuyerReceivablePaisa = 1150000L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-1",
                shopId = "shop-1",
                partyId = "farmer-1",
                transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 200000L,
                transactionDate = 1000L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        // 2. Summary count check
        val summary = syncEngine.getPendingSyncSummary()
        assertEquals(2, summary.pendingPartiesCount)
        assertEquals(1, summary.pendingDealsCount)
        assertEquals(1, summary.pendingTransactionsCount)
        assertEquals(2, summary.pendingRevisionsCount) // 1 deal revision + 1 cash revision
        assertEquals(6, summary.totalPending)

        // 3. Configure Fake API response
        fakeClient.pushResponseToReturn = Result.success(
            SyncPushResponseDto(
                success = true,
                synced_parties = listOf("farmer-1", "buyer-1"),
                synced_deals = listOf("deal-1"),
                synced_transactions = listOf("tx-1"),
                synced_revisions = fakeClient.lastPushedRequest?.revisions?.map { it.id } ?: emptyList()
            )
        )

        // 4. Push pending changes
        val pushResult = syncEngine.pushPendingChanges()
        assertTrue(pushResult.isSuccess)

        // 5. Verify pushed DTO contents
        val pushed = fakeClient.lastPushedRequest!!
        assertEquals(2, pushed.parties.size)
        assertEquals(1, pushed.deals.size)
        assertEquals(227550L, pushed.deals[0].rate_paisa_per_unit)
        assertEquals(200000L, pushed.transactions[0].amount_paisa)

        // 6. After sync, pending counts drop to 0 for marked entities
        val afterSummary = syncEngine.getPendingSyncSummary()
        assertEquals(0, afterSummary.pendingPartiesCount)
        assertEquals(0, afterSummary.pendingDealsCount)
        assertEquals(0, afterSummary.pendingTransactionsCount)
    }

    @Test
    fun testPullRemoteChangesMergesIntoLocalDatabase() = runTest {
        val database = createTestDatabase()
        val partyRepo = OfflineFirstPartyRepository(database)
        val fakeClient = FakeSyncApiClient()
        val syncEngine = SyncEngine(database = database, apiClient = fakeClient)

        fakeClient.pullResponseToReturn = Result.success(
            SyncPullResponseDto(
                last_sync_timestamp = 1000L,
                parties = listOf(
                    PartySyncDto(
                        id = "remote-farmer-1",
                        name = "सुरेश कुमार",
                        phone = "9876500001",
                        role = "FARMER",
                        village = "बलदेव"
                    )
                ),
                server_sync_time = 2000L
            )
        )

        val pullResult = syncEngine.pullRemoteChanges(sinceMs = 1000L, shopId = "srv-shop-7")
        assertTrue(pullResult.isSuccess)

        val localParties = partyRepo.getPartiesStream("srv-shop-7").first()
        assertEquals(1, localParties.size)
        assertEquals("सुरेश कुमार", localParties[0].name)
        assertEquals("बलदेव", localParties[0].village)
    }
}
