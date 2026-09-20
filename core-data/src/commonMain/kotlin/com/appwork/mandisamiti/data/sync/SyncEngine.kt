package com.appwork.mandisamiti.data.sync

import com.appwork.mandisamiti.database.AppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class SyncPayload(
    val pendingPartiesCount: Int,
    val pendingDealsCount: Int,
    val pendingTransactionsCount: Int
)

class SyncEngine(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val queries = database.appDatabaseQueries

    suspend fun getPendingSyncSummary(): SyncPayload = withContext(ioDispatcher) {
        val parties = queries.getPendingSyncParties().executeAsList()
        val deals = queries.getPendingSyncDeals().executeAsList()
        val txs = queries.getPendingSyncTransactions().executeAsList()
        SyncPayload(
            pendingPartiesCount = parties.size,
            pendingDealsCount = deals.size,
            pendingTransactionsCount = txs.size
        )
    }

    suspend fun markAllBatchSynced(partyIds: List<String>, dealIds: List<String>, txIds: List<String>) = withContext(ioDispatcher) {
        partyIds.forEach { queries.markPartySynced(it) }
        dealIds.forEach { queries.markDealSynced(it) }
        txIds.forEach { queries.markTransactionSynced(it) }
    }
}
