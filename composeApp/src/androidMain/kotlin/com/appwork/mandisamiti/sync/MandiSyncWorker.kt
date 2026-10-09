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
import com.appwork.mandisamiti.database.DriverFactory
import com.appwork.mandisamiti.database.createDatabase
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

        val database = createDatabase(DriverFactory(applicationContext))
        val syncApiClient = KtorMandiSyncApiClient(
            http = mandiHttpClient(),
            baseUrl = apiBaseUrl,
            sessionStore = sessionStore,
            refresh = AuthApi(mandiHttpClient(), apiBaseUrl)::refresh
        )
        val syncEngine = SyncEngine(database = database, apiClient = syncApiClient)

        when (syncEngine.syncFull(currentSession.shopId)) {
            is SyncResult.Success -> Result.success()
            is SyncResult.Failure -> {
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        }
    }
}
