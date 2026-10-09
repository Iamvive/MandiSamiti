package com.appwork.mandisamiti.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.appwork.mandisamiti.data.sync.SyncScheduler
import java.util.concurrent.TimeUnit

class AndroidSyncScheduler(private val context: Context) : SyncScheduler {

    companion object {
        const val UNIQUE_ONE_TIME_SYNC = "mandi_one_time_sync"
        const val UNIQUE_PERIODIC_SYNC = "mandi_periodic_sync"
    }

    private val workManager by lazy { WorkManager.getInstance(context) }

    private val connectedConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override fun scheduleOneTimeSync() {
        val request = OneTimeWorkRequestBuilder<MandiSyncWorker>()
            .setConstraints(connectedConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            UNIQUE_ONE_TIME_SYNC,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    override fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<MandiSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(connectedConstraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_SYNC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    override fun cancelAll() {
        workManager.cancelUniqueWork(UNIQUE_ONE_TIME_SYNC)
        workManager.cancelUniqueWork(UNIQUE_PERIODIC_SYNC)
    }
}
