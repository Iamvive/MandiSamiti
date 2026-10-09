package com.appwork.mandisamiti.data.sync

interface SyncScheduler {
    fun scheduleOneTimeSync()
    fun schedulePeriodicSync()
    fun cancelAll()
}

class NoOpSyncScheduler : SyncScheduler {
    override fun scheduleOneTimeSync() {}
    override fun schedulePeriodicSync() {}
    override fun cancelAll() {}
}
