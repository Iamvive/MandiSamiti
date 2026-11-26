package com.appwork.mandisamiti.data.sync

import com.appwork.mandisamiti.data.analytics.Analytics
import com.appwork.mandisamiti.data.analytics.AnalyticsEventsPushRequestDto
import com.appwork.mandisamiti.data.analytics.AnalyticsEventsPushResponseDto
import com.appwork.mandisamiti.data.sync.model.SyncPullResponseDto
import com.appwork.mandisamiti.data.sync.model.SyncPushRequestDto
import com.appwork.mandisamiti.data.sync.model.SyncPushResponseDto
import com.appwork.mandisamiti.data.sync.remote.MandiSyncApiClient
import com.appwork.mandisamiti.database.createTestDatabase
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SyncEngineAnalyticsTest {

    private class FakeSyncApiClient(
        private val pullResult: Result<SyncPullResponseDto> = Result.success(SyncPullResponseDto()),
        private val pushEventsHandler: suspend (AnalyticsEventsPushRequestDto) -> Result<AnalyticsEventsPushResponseDto> =
            { Result.success(AnalyticsEventsPushResponseDto(it.events.size)) },
    ) : MandiSyncApiClient {
        val pushedEventBatches = mutableListOf<AnalyticsEventsPushRequestDto>()

        override suspend fun pushSync(request: SyncPushRequestDto): Result<SyncPushResponseDto> =
            Result.success(SyncPushResponseDto())

        override suspend fun pullSync(afterSeq: Long, limit: Int): Result<SyncPullResponseDto> =
            pullResult

        override suspend fun pushEvents(request: AnalyticsEventsPushRequestDto): Result<AnalyticsEventsPushResponseDto> {
            pushedEventBatches += request
            return pushEventsHandler(request)
        }
    }

    private fun seedShop(db: com.appwork.mandisamiti.database.AppDatabase, shopId: String = "shop-1") {
        db.appDatabaseQueries.insertShopProfile(
            id = shopId,
            shop_name = "Shop",
            owner_name = "Owner",
            mandi_name = "Mandi",
            shop_number = null,
            phone_number = "9000000000",
            pin_hash = "",
            default_monthly_interest_rate = 2.0,
            is_sound_enabled = 1L,
            created_at = 1L,
            updated_at = 1L,
            sync_status = 1L,
            address = null,
            gstin = null,
            licence_no = null,
        )
    }

    @Test
    fun syncFull_successTracksSyncOkWithDurationAndFlushes() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        seedShop(db)
        val analytics = Analytics(db, "1.0", clock = { 1000L }, idGenerator = { "e1" }, sessionId = "s1", ioDispatcher = io)
        val client = FakeSyncApiClient()
        val engine = SyncEngine(db, client, ioDispatcher = io, analytics = analytics)

        val result = engine.syncFull("shop-1")

        assertIs<SyncResult.Success>(result)
        assertEquals(1, client.pushedEventBatches.size)
        val event = client.pushedEventBatches.single().events.single()
        assertEquals("sync_ok", event.name)
        assertNotNull(event.durationMs)
        assertTrue(event.durationMs!! >= 0L)
        assertEquals(0, db.appDatabaseQueries.getOldestAnalyticsEvents(10).executeAsList().size)
    }

    @Test
    fun syncFull_failureTracksSyncFailedAndFlushes() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        val analytics = Analytics(db, "1.0", clock = { 1000L }, idGenerator = { "e1" }, sessionId = "s1", ioDispatcher = io)
        val client = FakeSyncApiClient(pullResult = Result.failure(IOException("network down")))
        val engine = SyncEngine(db, client, ioDispatcher = io, analytics = analytics)

        val result = engine.syncFull("shop-1")

        assertIs<SyncResult.Failure>(result)
        assertEquals(1, client.pushedEventBatches.size)
        val event = client.pushedEventBatches.single().events.single()
        assertEquals("sync_failed", event.name)
        assertNotNull(event.durationMs)
    }

    @Test
    fun syncFull_throwingFlushLeavesSyncResultUnchanged() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        seedShop(db)
        val analytics = Analytics(db, "1.0", clock = { 1000L }, idGenerator = { "e1" }, sessionId = "s1", ioDispatcher = io)
        val throwingClient = FakeSyncApiClient(
            pushEventsHandler = { throw IllegalStateException("flush exploded") }
        )
        val engine = SyncEngine(db, throwingClient, ioDispatcher = io, analytics = analytics)

        val result = engine.syncFull("shop-1")

        val ok = assertIs<SyncResult.Success>(result)
        assertEquals(0, ok.pushedCount)
        assertEquals(0, ok.pulledCount)
        assertEquals(1, db.appDatabaseQueries.getOldestAnalyticsEvents(10).executeAsList().size)

        val failClient = FakeSyncApiClient(
            pullResult = Result.failure(IOException("sync down")),
            pushEventsHandler = { throw IllegalStateException("flush exploded") }
        )
        val failEngine = SyncEngine(db, failClient, ioDispatcher = io, analytics = analytics)
        val failResult = failEngine.syncFull("shop-1")
        val fail = assertIs<SyncResult.Failure>(failResult)
        assertIs<IOException>(fail.error)
    }

    @Test
    fun syncFull_withoutApiClientSkipsTelemetry() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        val analytics = Analytics(db, "1.0", clock = { 1000L }, idGenerator = { "e1" }, sessionId = "s1", ioDispatcher = io)
        val engine = SyncEngine(db, apiClient = null, ioDispatcher = io, analytics = analytics)

        val result = engine.syncFull("shop-1")

        assertIs<SyncResult.Failure>(result)
        assertEquals(0, db.appDatabaseQueries.getOldestAnalyticsEvents(10).executeAsList().size)
    }
}
