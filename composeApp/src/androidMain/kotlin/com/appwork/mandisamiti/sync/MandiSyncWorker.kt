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
import com.appwork.mandisamiti.platform.apiBaseUrl
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
            val syncEngine = SyncEngine(database = database, apiClient = syncApiClient)

            when (val r = syncEngine.syncFull(currentSession.shopId)) {
                is SyncResult.Success -> Result.success()
                is SyncResult.Failure ->
                    if (r.error is SyncAuthExpired) {
                        syncEngine.setNeedsLogin(true)
                        Result.failure()
                    } else {
                        Result.retry() // WorkManager's exponential backoff; no attempt cap
                    }
            }
        } finally {
            http.close()
        }
    }
}
