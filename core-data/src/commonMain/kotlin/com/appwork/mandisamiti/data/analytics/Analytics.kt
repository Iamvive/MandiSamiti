package com.appwork.mandisamiti.data.analytics

import com.appwork.mandisamiti.data.sync.remote.MandiSyncApiClient
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.domain.id.IdGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class Analytics(
    private val database: AppDatabase,
    appVersion: String,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val idGenerator: () -> String = { IdGenerator.newId() },
    sessionId: String = processSessionId,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val queries = database.appDatabaseQueries
    private val sanitizedSessionId = sanitizeId(sessionId)
    private val sanitizedAppVersion = sanitizeAppVersion(appVersion)
    private val flushMutex = Mutex()

    suspend fun track(
        name: AnalyticsEventName,
        screen: AnalyticsScreen? = null,
        durationMs: Long? = null,
    ) {
        try {
            withContext(ioDispatcher) {
                val id = sanitizeId(idGenerator())
                val occurredAtMs = clock()
                database.transaction {
                    queries.insertAnalyticsEvent(
                        id = id,
                        name = name.wire,
                        screen = screen?.wire,
                        session_id = sanitizedSessionId,
                        occurred_at_ms = occurredAtMs,
                        duration_ms = durationMs?.coerceAtLeast(0L),
                        app_version = sanitizedAppVersion,
                    )
                    queries.trimAnalyticsEvents()
                }
            }
        } catch (c: CancellationException) {
            throw c
        } catch (_: Throwable) {
            // Tracking never blocks or fails a caller.
        }
    }

    suspend fun flush(client: MandiSyncApiClient) {
        try {
            withContext(ioDispatcher) {
                flushMutex.withLock {
                    repeat(MAX_BATCHES_PER_FLUSH) {
                        val batch = queries.getOldestAnalyticsEvents(BATCH_SIZE).executeAsList()
                        if (batch.isEmpty()) return@withLock

                        val request = AnalyticsEventsPushRequestDto(
                            events = batch.map { row ->
                                AnalyticsEventDto(
                                    id = row.id,
                                    name = row.name,
                                    screen = row.screen,
                                    sessionId = row.session_id,
                                    occurredAtMs = row.occurred_at_ms,
                                    durationMs = row.duration_ms,
                                    appVersion = row.app_version,
                                )
                            }
                        )

                        val result = client.pushEvents(request)
                        val err = result.exceptionOrNull()
                        if (err is CancellationException) throw err

                        val dropBatch = result.isSuccess ||
                            (err is EventsPushHttpException && (err.statusCode == 400 || err.statusCode == 422))
                        if (dropBatch) {
                            queries.deleteAnalyticsEventsByIds(batch.map { it.id })
                        } else {
                            return@withLock
                        }
                    }
                }
            }
        } catch (c: CancellationException) {
            throw c
        } catch (_: Throwable) {
            // Flush failures keep rows queued and never fail the caller.
        }
    }

    companion object {
        const val MAX_QUEUE_SIZE = 5000
        const val BATCH_SIZE = 200L
        const val MAX_BATCHES_PER_FLUSH = 5

        private val processSessionId: String = sanitizeId(IdGenerator.newId())

        internal fun sanitizeId(raw: String): String =
            raw.filter { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '-' || it == '_' }
                .take(64)
                .ifEmpty { IdGenerator.newId().take(64) }

        internal fun sanitizeAppVersion(raw: String): String =
            raw.filter {
                it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' ||
                    it == '.' || it == '_' || it == '+' || it == '-'
            }
                .take(32)
                .ifEmpty { "0" }
    }
}
