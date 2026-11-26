package com.appwork.mandisamiti.data.analytics

import com.appwork.mandisamiti.data.sync.model.SyncPullResponseDto
import com.appwork.mandisamiti.data.sync.model.SyncPushRequestDto
import com.appwork.mandisamiti.data.sync.model.SyncPushResponseDto
import com.appwork.mandisamiti.data.sync.remote.MandiSyncApiClient
import com.appwork.mandisamiti.data.sync.remote.SyncAuthExpired
import com.appwork.mandisamiti.database.createTestDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnalyticsTest {

    private class RecordingApiClient(
        private val handler: suspend (AnalyticsEventsPushRequestDto) -> Result<AnalyticsEventsPushResponseDto>,
    ) : MandiSyncApiClient {
        val batches = mutableListOf<AnalyticsEventsPushRequestDto>()

        override suspend fun pushSync(request: SyncPushRequestDto): Result<SyncPushResponseDto> =
            Result.success(SyncPushResponseDto())

        override suspend fun pullSync(afterSeq: Long, limit: Int): Result<SyncPullResponseDto> =
            Result.success(SyncPullResponseDto())

        override suspend fun pushEvents(request: AnalyticsEventsPushRequestDto): Result<AnalyticsEventsPushResponseDto> {
            batches += request
            return handler(request)
        }
    }

    @Test
    fun track_storesRowWithExpectedFields() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        val analytics = Analytics(
            database = db,
            appVersion = "1.0",
            clock = { 1764140000000L },
            idGenerator = { "e1" },
            sessionId = "s1",
            ioDispatcher = io,
        )

        analytics.track(AnalyticsEventName.SCREEN_VIEW, screen = AnalyticsScreen.BUY_LIST)

        val rows = db.appDatabaseQueries.getOldestAnalyticsEvents(10).executeAsList()
        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("e1", row.id)
        assertEquals("screen_view", row.name)
        assertEquals("buy_list", row.screen)
        assertEquals("s1", row.session_id)
        assertEquals(1764140000000L, row.occurred_at_ms)
        assertNull(row.duration_ms)
        assertEquals("1.0", row.app_version)
    }

    @Test
    fun track_enforces5000CapDroppingOldest() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        var clockSeq = 0L
        var idSeq = 0L
        val analytics = Analytics(
            database = db,
            appVersion = "1.0",
            clock = { ++clockSeq },
            idGenerator = { "id-${++idSeq}" },
            sessionId = "s1",
            ioDispatcher = io,
        )

        repeat(5005) {
            analytics.track(AnalyticsEventName.APP_OPEN)
        }

        val all = db.appDatabaseQueries.getOldestAnalyticsEvents(10_000).executeAsList()
        assertEquals(5000, all.size)
        assertEquals("id-6", all.first().id)
        assertEquals(6L, all.first().occurred_at_ms)
        assertEquals("id-5005", all.last().id)
        assertEquals(5005L, all.last().occurred_at_ms)
    }

    @Test
    fun track_swallowsErrorsExceptCancellation() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        val failing = Analytics(
            database = db,
            appVersion = "1.0",
            clock = { throw IllegalStateException("clock boom") },
            ioDispatcher = io,
        )
        failing.track(AnalyticsEventName.APP_OPEN)
        assertEquals(0, db.appDatabaseQueries.getOldestAnalyticsEvents(10).executeAsList().size)

        val cancelled = Analytics(
            database = db,
            appVersion = "1.0",
            clock = { throw CancellationException("cancelled") },
            ioDispatcher = io,
        )
        assertFailsWith<CancellationException> {
            cancelled.track(AnalyticsEventName.APP_OPEN)
        }
    }

    @Test
    fun flush_sendsOldestFirstMax5BatchesOf200AndDeletesOnSuccess() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        var clockSeq = 0L
        var idSeq = 0L
        val analytics = Analytics(
            database = db,
            appVersion = "1.0",
            clock = { ++clockSeq },
            idGenerator = { "e-${++idSeq}" },
            sessionId = "s1",
            ioDispatcher = io,
        )
        repeat(1050) {
            analytics.track(AnalyticsEventName.BUY_SAVED)
        }

        val client = RecordingApiClient { req ->
            Result.success(AnalyticsEventsPushResponseDto(accepted = req.events.size))
        }
        analytics.flush(client)

        assertEquals(5, client.batches.size)
        assertTrue(client.batches.all { it.events.size == 200 })
        assertEquals("e-1", client.batches.first().events.first().id)
        assertEquals("e-1000", client.batches.last().events.last().id)

        val remaining = db.appDatabaseQueries.getOldestAnalyticsEvents(100).executeAsList()
        assertEquals(50, remaining.size)
        assertEquals("e-1001", remaining.first().id)
    }

    @Test
    fun flush_keepsRowsAndStopsOnNetworkFailure404And401() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        var clockSeq = 0L
        var idSeq = 0L
        val analytics = Analytics(
            database = db,
            appVersion = "1.0",
            clock = { ++clockSeq },
            idGenerator = { "e-${++idSeq}" },
            sessionId = "s1",
            ioDispatcher = io,
        )
        repeat(205) {
            analytics.track(AnalyticsEventName.SELL_SAVED)
        }

        val networkFailClient = RecordingApiClient { Result.failure(IOException("offline")) }
        analytics.flush(networkFailClient)
        assertEquals(1, networkFailClient.batches.size)
        assertEquals(205, db.appDatabaseQueries.getOldestAnalyticsEvents(500).executeAsList().size)

        val notFoundClient = RecordingApiClient { Result.failure(EventsPushHttpException(404, "not found")) }
        analytics.flush(notFoundClient)
        assertEquals(1, notFoundClient.batches.size)
        assertEquals(205, db.appDatabaseQueries.getOldestAnalyticsEvents(500).executeAsList().size)

        val authExpiredClient = RecordingApiClient { Result.failure(SyncAuthExpired()) }
        analytics.flush(authExpiredClient)
        assertEquals(1, authExpiredClient.batches.size)
        assertEquals(205, db.appDatabaseQueries.getOldestAnalyticsEvents(500).executeAsList().size)
    }

    @Test
    fun flush_dropsBadBatchOn400And422SoLaterBatchesAreSent() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        var clockSeq = 0L
        var idSeq = 0L
        val analytics = Analytics(
            database = db,
            appVersion = "1.0",
            clock = { ++clockSeq },
            idGenerator = { "e-${++idSeq}" },
            sessionId = "s1",
            ioDispatcher = io,
        )
        repeat(401) {
            analytics.track(AnalyticsEventName.PAYMENT_SAVED)
        }

        var callIndex = 0
        val client = RecordingApiClient { req ->
            when (callIndex++) {
                0 -> Result.failure(EventsPushHttpException(422, "unprocessable"))
                1 -> Result.failure(EventsPushHttpException(400, "bad request"))
                else -> Result.success(AnalyticsEventsPushResponseDto(accepted = req.events.size))
            }
        }
        analytics.flush(client)

        assertEquals(3, client.batches.size)
        assertEquals(0, db.appDatabaseQueries.getOldestAnalyticsEvents(500).executeAsList().size)
    }

    @Test
    fun flush_swallowsThrowingClientAndKeepsRows() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        val analytics = Analytics(
            database = db,
            appVersion = "1.0",
            clock = { 100L },
            idGenerator = { "e1" },
            sessionId = "s1",
            ioDispatcher = io,
        )
        analytics.track(AnalyticsEventName.EXPENSE_SAVED)

        val throwingClient = RecordingApiClient { throw IllegalStateException("unexpected crash") }
        analytics.flush(throwingClient)

        assertEquals(1, db.appDatabaseQueries.getOldestAnalyticsEvents(10).executeAsList().size)
    }

    @Test
    fun sessionIdIsSharedPerProcessAndFieldsAreSanitized() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        val longRawId = "id!@#\$" + "a".repeat(80)
        val a1 = Analytics(
            database = db,
            appVersion = "1.2.3-beta+45!@#$" + "x".repeat(40),
            clock = { 10L },
            idGenerator = { longRawId },
            ioDispatcher = io,
        )
        val a2 = Analytics(
            database = db,
            appVersion = "1.0",
            clock = { 20L },
            idGenerator = { "valid-id_2" },
            ioDispatcher = io,
        )

        a1.track(AnalyticsEventName.APP_OPEN)
        a2.track(AnalyticsEventName.LOGIN_SUCCESS)

        val rows = db.appDatabaseQueries.getOldestAnalyticsEvents(10).executeAsList()
        assertEquals(2, rows.size)
        assertEquals(rows[0].session_id, rows[1].session_id)
        assertTrue(rows[0].session_id.matches(Regex("^[A-Za-z0-9-_]{1,64}$")))
        assertEquals(64, rows[0].id.length)
        assertTrue(rows[0].id.matches(Regex("^[A-Za-z0-9-_]{1,64}$")))
        assertEquals(32, rows[0].app_version.length)
        assertTrue(rows[0].app_version.matches(Regex("^[A-Za-z0-9._+-]{1,32}$")))
    }
}
