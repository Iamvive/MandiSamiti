package com.appwork.mandisamiti.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appwork.mandisamiti.data.auth.AndroidSessionStore
import com.appwork.mandisamiti.data.auth.AuthApi
import com.appwork.mandisamiti.data.auth.mandiHttpClient
import com.appwork.mandisamiti.data.sync.SyncEngine
import com.appwork.mandisamiti.data.sync.SyncResult
import com.appwork.mandisamiti.data.sync.remote.KtorMandiSyncApiClient
import com.appwork.mandisamiti.data.sync.remote.SyncAuthExpired
import com.appwork.mandisamiti.AppDatabaseHolder
import com.appwork.mandisamiti.platform.PiiScrubber
import com.appwork.mandisamiti.platform.apiBaseUrl
import io.sentry.Sentry
import io.sentry.SentryLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MandiSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sessionStore = AndroidSessionStore(applicationContext)
        val currentSession = sessionStore.current() ?: return@withContext Result.success()

        val database = AppDatabaseHolder.get(applicationContext)
        val http = mandiHttpClient()
        try {
            val syncApiClient = KtorMandiSyncApiClient(
                http = http,
                baseUrl = apiBaseUrl,
                sessionStore = sessionStore,
                refresh = AuthApi(http, apiBaseUrl)::refresh
            )
            val analytics = com.appwork.mandisamiti.data.analytics.Analytics(
                database = database,
                appVersion = com.appwork.mandisamiti.BuildConfig.VERSION_NAME
            )
            val syncEngine = SyncEngine(
                database = database,
                apiClient = syncApiClient,
                analytics = analytics
            )

            when (val r = syncEngine.syncFull(currentSession.shopId)) {
                is SyncResult.Success -> {
                    Sentry.setTag("sync_state", "ok")
                    Result.success()
                }
                is SyncResult.Failure ->
                    if (r.error is SyncAuthExpired) {
                        Sentry.setTag("sync_state", "auth_expired")
                        syncEngine.setNeedsLogin(true)
                        Result.failure()
                    } else {
                        Sentry.setTag("sync_state", "failed")
                        Sentry.captureMessage(
                            "Sync failed: ${PiiScrubber.scrub(r.error.toString())}",
                            SentryLevel.WARNING
                        )
                        Result.retry() // WorkManager's exponential backoff; no attempt cap
                    }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Sentry.setTag("sync_state", "exception")
            Sentry.captureException(e)
            Result.retry()
        } finally {
            http.close()
        }
    }
}
