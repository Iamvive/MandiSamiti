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
    val pushed = mutableListOf<SyncPushRequestDto>()
    val pushResponses = ArrayDeque<Result<SyncPushResponseDto>>()
    val pullResponses = ArrayDeque<Result<SyncPullResponseDto>>()
    val pullCursors = mutableListOf<Long>()
    val calls = mutableListOf<String>()
    var beforePull: (suspend (Long) -> Unit)? = null

    // Default when a queue is empty: ack everything that was sent / empty page.
    override suspend fun pushSync(request: SyncPushRequestDto): Result<SyncPushResponseDto> {
        pushed += request
        calls += "push"
        return pushResponses.removeFirstOrNull() ?: Result.success(
            SyncPushResponseDto(
                synced_parties = request.parties.map { it.id }, synced_deals = request.deals.map { it.id },
                synced_transactions = request.transactions.map { it.id }, synced_revisions = request.revisions.map { it.id }
            )
        )
    }

    override suspend fun pullSync(afterSeq: Long, limit: Int): Result<SyncPullResponseDto> {
        pullCursors += afterSeq
        calls += "pull"
        beforePull?.invoke(afterSeq)
        return pullResponses.removeFirstOrNull() ?: Result.success(SyncPullResponseDto(after_seq = afterSeq, next_seq = afterSeq))
    }
}

/** Pulled rows only land while the shop profile they belong to exists locally. */
private fun seedShop(db: com.appwork.mandisamiti.database.AppDatabase, id: String) =
    db.appDatabaseQueries.insertShopProfile(id, "दुकान", "मालिक", "मंडी", null, "9000000001", "", 1.5, 1L, 0L, 0L, 1L)

private fun farmer() = Party(id = "farmer-1", shopId = "shop-1", name = "रामवीर सिंह", village = "राया", partyType = PartyType.FARMER, createdAt = 1000L, updatedAt = 1000L)

private fun deal() = Deal(
    id = "deal-1", shopId = "shop-1", farmerId = "farmer-1", buyerId = null, commodityId = "comm-1", dealDate = 1000L,
    bagsCount = 10, grossWeightGrams = 500000L, netWeightGrams = 500000L, ratePaisaPerUnit = 227550L,
    grossAmountPaisa = 1137750L, netFarmerPayablePaisa = 1100000L, netBuyerReceivablePaisa = 1150000L,
    createdAt = 1000L, updatedAt = 1000L
)

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
        // (default fake acks everything that was sent)

        // 4. Push pending changes
        val pushResult = syncEngine.pushPendingChanges()
        assertTrue(pushResult.isSuccess)

        // 5. Verify pushed DTO contents
        val pushed = SyncPushRequestDto(
            parties = fakeClient.pushed.flatMap { it.parties }, deals = fakeClient.pushed.flatMap { it.deals },
            transactions = fakeClient.pushed.flatMap { it.transactions }, revisions = fakeClient.pushed.flatMap { it.revisions }
        )
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

        fakeClient.pullResponses += Result.success(
            SyncPullResponseDto(
                after_seq = 0L,
                next_seq = 2000L,
                has_more = false,
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

        seedShop(database, "srv-shop-7")
        val pullResult = syncEngine.pullRemoteChanges(shopId = "srv-shop-7")
        assertTrue(pullResult.isSuccess)
        assertEquals(1, pullResult.getOrNull())

        val localParties = partyRepo.getPartiesStream("srv-shop-7").first()
        assertEquals(1, localParties.size)
        assertEquals("सुरेश कुमार", localParties[0].name)
        assertEquals("बलदेव", localParties[0].village)
        assertEquals(2000L, syncEngine.getLastServerSeq("srv-shop-7"))
    }

    @Test
    fun testPullMultiplePagesWithCursor() = runTest {
        val database = createTestDatabase()
        val partyRepo = OfflineFirstPartyRepository(database)
        val pullCalls = mutableListOf<Long>()

        val pagingClient = object : MandiSyncApiClient {
            override suspend fun pushSync(request: SyncPushRequestDto): Result<SyncPushResponseDto> = Result.success(SyncPushResponseDto())
            override suspend fun pullSync(afterSeq: Long, limit: Int): Result<SyncPullResponseDto> {
                pullCalls.add(afterSeq)
                return when (afterSeq) {
                    0L -> Result.success(
                        SyncPullResponseDto(
                            after_seq = 0L,
                            next_seq = 100L,
                            has_more = true,
                            parties = listOf(PartySyncDto(id = "p-page-1", name = "राम 1", role = "FARMER")),
                            server_sync_time = 1000L
                        )
                    )
                    100L -> Result.success(
                        SyncPullResponseDto(
                            after_seq = 100L,
                            next_seq = 200L,
                            has_more = false,
                            parties = listOf(PartySyncDto(id = "p-page-2", name = "श्याम 2", role = "BUYER")),
                            server_sync_time = 1000L
                        )
                    )
                    else -> Result.success(SyncPullResponseDto(after_seq = afterSeq, next_seq = afterSeq, has_more = false))
                }
            }
        }

        val syncEngine = SyncEngine(database = database, apiClient = pagingClient)
        val progressUpdates = mutableListOf<Int>()
        seedShop(database, "shop-page-test")
        val result = syncEngine.pullRemoteChanges("shop-page-test") { progressUpdates.add(it) }

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
        assertEquals(listOf(0L, 100L), pullCalls)
        assertEquals(listOf(1, 2), progressUpdates)
        assertEquals(200L, syncEngine.getLastServerSeq("shop-page-test"))

        val parties = partyRepo.getPartiesStream("shop-page-test").first()
        assertEquals(2, parties.size)
    }

    @Test
    fun pullingBackOurOwnRevisionDoesNotFailAndLeavesNothingPending() = runTest {
        val db = createTestDatabase(); val fake = FakeSyncApiClient(); val engine = SyncEngine(db, fake)
        seedShop(db, "shop-1")
        OfflineFirstPartyRepository(db).saveParty(farmer())
        OfflineFirstDealRepository(db).saveDeal(deal())
        assertTrue(engine.pushPendingChanges().isSuccess)
        val ownRev = db.appDatabaseQueries.getRevisionsForEntry("deal-1").executeAsOne()
        fake.pullResponses += Result.success(SyncPullResponseDto(next_seq = 3, revisions = listOf(
            EntryRevisionSyncDto(ownRev.id, "deal-1", "DEAL", 1, "CREATE", ownRev.snapshot_json, null, ownRev.changed_at))))
        assertTrue(engine.pullRemoteChanges("shop-1").isSuccess)
        assertEquals(0L, engine.getPendingCount())
        assertEquals(3L, engine.getLastServerSeq("shop-1"))
    }

    @Test
    fun pullDoesNotOverwriteAPendingLocalEdit() = runTest {
        val db = createTestDatabase(); val fake = FakeSyncApiClient(); val engine = SyncEngine(db, fake)
        seedShop(db, "shop-1")
        OfflineFirstPartyRepository(db).saveParty(farmer().copy(name = "local edit"))
        fake.pullResponses += Result.success(SyncPullResponseDto(next_seq = 1, parties = listOf(
            PartySyncDto(id = "farmer-1", name = "server", role = "FARMER", updated_at = 1L))))
        engine.pullRemoteChanges("shop-1").getOrThrow()
        assertEquals("local edit", db.appDatabaseQueries.getPartyById("farmer-1").executeAsOne().name)
        assertEquals(1L, engine.getPendingCount())
    }

    @Test
    fun pushGoesInBatchesOf100AndKeepsEarlierAcksWhenALaterBatchFails() = runTest {
        val db = createTestDatabase(); val fake = FakeSyncApiClient(); val engine = SyncEngine(db, fake)
        val repo = OfflineFirstPartyRepository(db)
        repeat(150) { repo.saveParty(farmer().copy(id = "p$it")) }
        fake.pushResponses += Result.success(SyncPushResponseDto(synced_parties = (0 until 100).map { "p$it" }))
        fake.pushResponses += Result.failure(RuntimeException("connection dropped"))
        assertTrue(engine.pushPendingChanges().isFailure)
        assertEquals(listOf(100, 50), fake.pushed.map { it.parties.size })
        assertEquals(50L, engine.getPendingCount())
    }

    @Test
    fun pullResumesFromTheLastAppliedPage() = runTest {
        val db = createTestDatabase(); val fake = FakeSyncApiClient(); val engine = SyncEngine(db, fake)
        seedShop(db, "shop-1")
        fake.pullResponses += Result.success(SyncPullResponseDto(next_seq = 500, has_more = true,
            parties = listOf(PartySyncDto(id = "a", name = "a", role = "FARMER"))))
        fake.pullResponses += Result.failure(RuntimeException("timeout"))
        assertTrue(engine.pullRemoteChanges("shop-1").isFailure)
        assertEquals(500L, engine.getLastServerSeq("shop-1"))
        engine.pullRemoteChanges("shop-1")
        assertEquals(listOf(0L, 500L, 500L), fake.pullCursors)
    }

    @Test
    fun conflictedRevisionIsAckedSoItDoesNotBlockLogout() = runTest {
        val db = createTestDatabase(); val fake = FakeSyncApiClient(); val engine = SyncEngine(db, fake)
        OfflineFirstPartyRepository(db).saveParty(farmer())
        OfflineFirstDealRepository(db).saveDeal(deal())
        val revId = db.appDatabaseQueries.getRevisionsForEntry("deal-1").executeAsOne().id
        fake.pushResponses += Result.success(SyncPushResponseDto(synced_parties = listOf("farmer-1"),
            synced_deals = listOf("deal-1"), synced_revisions = listOf(revId), conflicts = listOf(revId)))
        engine.pushPendingChanges().getOrThrow()
        assertEquals(0L, engine.getPendingCount())
    }

    @Test
    fun pushSendsParentsBeforeChildrenAndNeverMixesKinds() = runTest {
        val db = createTestDatabase(); val fake = FakeSyncApiClient(); val engine = SyncEngine(db, fake)
        val repo = OfflineFirstPartyRepository(db)
        repeat(150) { repo.saveParty(farmer().copy(id = "p$it")) }
        OfflineFirstDealRepository(db).saveDeal(deal().copy(farmerId = "p149"))
        assertTrue(engine.pushPendingChanges().isSuccess)
        val kinds = fake.pushed.map { r ->
            listOf(r.parties, r.deals, r.transactions, r.revisions).count { it.isNotEmpty() }
        }
        assertTrue(kinds.all { it == 1 })
        val p149 = fake.pushed.indexOfFirst { r -> r.parties.any { it.id == "p149" } }
        val dealReq = fake.pushed.indexOfFirst { it.deals.isNotEmpty() }
        assertTrue(p149 in 0 until dealReq)
        assertEquals(0L, engine.getPendingCount())
    }

    @Test
    fun pullCursorIsKeptPerShop() = runTest {
        val db = createTestDatabase(); val fake = FakeSyncApiClient(); val engine = SyncEngine(db, fake)
        seedShop(db, "shop-1"); seedShop(db, "shop-2")
        fake.pullResponses += Result.success(SyncPullResponseDto(next_seq = 40))
        engine.pullRemoteChanges("shop-1").getOrThrow()
        engine.pullRemoteChanges("shop-2").getOrThrow()
        assertEquals(listOf(0L, 0L), fake.pullCursors)   // shop-2 starts from its own cursor, not shop-1's
        assertEquals(40L, engine.getLastServerSeq("shop-1"))
        assertEquals(0L, engine.getLastServerSeq("shop-2"))
    }

    @Test
    fun pageArrivingAfterAWipeWritesNoRowsAndNoCursor() = runTest {
        val db = createTestDatabase(); val fake = FakeSyncApiClient(); val engine = SyncEngine(db, fake)
        seedShop(db, "shop-1")
        fake.pullResponses += Result.success(SyncPullResponseDto(next_seq = 500, has_more = true,
            parties = listOf(PartySyncDto(id = "a", name = "a", role = "FARMER"))))
        fake.pullResponses += Result.success(SyncPullResponseDto(next_seq = 900,
            parties = listOf(PartySyncDto(id = "b", name = "b", role = "FARMER"))))
        // Logout lands while the second page is in flight.
        fake.beforePull = { after -> if (after == 500L) com.appwork.mandisamiti.data.auth.LocalDataWiper(db).wipeAll() }
        assertTrue(engine.pullRemoteChanges("shop-1").isFailure)
        assertEquals(null, db.appDatabaseQueries.getPartyById("b").executeAsOneOrNull())
        assertEquals(null, db.appDatabaseQueries.getPartyById("a").executeAsOneOrNull())
        assertEquals(0L, engine.getLastServerSeq("shop-1"))
    }

    @Test
    fun firstSyncPushesPendingEditsBeforePulling() = runTest {
        val db = createTestDatabase(); val fake = FakeSyncApiClient(); val engine = SyncEngine(db, fake)
        seedShop(db, "shop-1")
        OfflineFirstPartyRepository(db).saveParty(farmer())
        assertTrue(engine.pushThenPull("shop-1").isSuccess)
        assertEquals(listOf("push", "pull"), fake.calls)
        assertEquals(0L, engine.getPendingCount())
    }

    @Test
    fun firstSyncFailsWithoutPullingWhenPushFails() = runTest {
        val db = createTestDatabase(); val fake = FakeSyncApiClient(); val engine = SyncEngine(db, fake)
        seedShop(db, "shop-1")
        OfflineFirstPartyRepository(db).saveParty(farmer())
        fake.pushResponses += Result.failure(RuntimeException("offline"))
        assertTrue(engine.pushThenPull("shop-1").isFailure)
        assertEquals(listOf("push"), fake.calls)
    }
}
