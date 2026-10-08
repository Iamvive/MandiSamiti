package com.appwork.mandisamiti.data.sync

import com.appwork.mandisamiti.data.sync.model.*
import com.appwork.mandisamiti.data.sync.remote.MandiSyncApiClient
import com.appwork.mandisamiti.database.AppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class SyncPayload(
    val pendingPartiesCount: Int,
    val pendingDealsCount: Int,
    val pendingTransactionsCount: Int,
    val pendingRevisionsCount: Int = 0
) {
    val totalPending: Int
        get() = pendingPartiesCount + pendingDealsCount + pendingTransactionsCount + pendingRevisionsCount
}

sealed interface SyncResult {
    data class Success(val pushedCount: Int, val pulledCount: Int, val syncTimeMs: Long) : SyncResult
    data class Failure(val error: Throwable) : SyncResult
}

class SyncEngine(
    private val database: AppDatabase,
    private val apiClient: MandiSyncApiClient? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val queries = database.appDatabaseQueries

    suspend fun getPendingSyncSummary(): SyncPayload = withContext(ioDispatcher) {
        val parties = queries.getPendingSyncParties().executeAsList()
        val deals = queries.getPendingSyncDeals().executeAsList()
        val txs = queries.getPendingSyncTransactions().executeAsList()
        val revisions = queries.getPendingSyncRevisions().executeAsList()
        SyncPayload(
            pendingPartiesCount = parties.size,
            pendingDealsCount = deals.size,
            pendingTransactionsCount = txs.size,
            pendingRevisionsCount = revisions.size
        )
    }

    suspend fun getPendingCount(): Long = withContext(ioDispatcher) {
        queries.getPendingSyncCount().executeAsOne()
    }

    suspend fun pushPendingChanges(): Result<Int> = withContext(ioDispatcher) {
        val client = apiClient ?: return@withContext Result.failure(IllegalStateException("No remote API client configured"))

        val parties = queries.getPendingSyncParties().executeAsList().map {
            PartySyncDto(
                id = it.id,
                name = it.name,
                phone = it.phone,
                role = it.party_type,
                village = it.village,
                monthly_interest_rate = it.monthly_interest_rate,
                photo_uri = it.photo_uri,
                is_deleted = it.is_deleted.toInt()
            )
        }

        val deals = queries.getPendingSyncDeals().executeAsList().map {
            DealSyncDto(
                id = it.id,
                farmer_id = it.farmer_id,
                buyer_id = it.buyer_id,
                commodity = it.commodity_id,
                deal_status = it.deal_status,
                deal_date = it.deal_date,
                bags_count = it.bags_count.toInt(),
                gross_weight_grams = it.gross_weight_grams,
                cut_weight_grams = it.cut_weight_grams,
                net_weight_grams = it.net_weight_grams,
                rate_paisa_per_unit = it.rate_paisa_per_unit ?: 0L,
                gross_amount_paisa = it.gross_amount_paisa,
                farmer_commission_bps = it.farmer_commission_bps.toInt(),
                farmer_commission_paisa = it.farmer_commission_paisa,
                buyer_commission_paisa = it.buyer_commission_paisa,
                labour_charge_paisa = it.labour_charge_paisa,
                weighing_charge_paisa = it.weighing_charge_paisa,
                other_deductions_paisa = it.other_deductions_paisa,
                net_farmer_payable_paisa = it.net_farmer_payable_paisa,
                net_buyer_receivable_paisa = it.net_buyer_receivable_paisa,
                receipt_photo_uri = it.receipt_photo_uri,
                voice_note_uri = it.voice_note_uri,
                remarks = it.remarks,
                is_void = it.is_void.toInt(),
                void_reason = it.void_reason,
                revision = it.revision.toInt(),
                is_deleted = it.is_deleted.toInt()
            )
        }

        val txs = queries.getPendingSyncTransactions().executeAsList().map {
            CashTransactionSyncDto(
                id = it.id,
                party_id = it.party_id,
                deal_id = it.deal_id,
                transaction_type = it.transaction_type,
                amount_paisa = it.amount_paisa,
                payment_mode = it.payment_mode,
                transaction_date = it.transaction_date,
                voice_note_uri = it.voice_note_uri,
                remarks = it.remarks,
                is_void = it.is_void.toInt(),
                void_reason = it.void_reason,
                revision = it.revision.toInt(),
                is_deleted = it.is_deleted.toInt()
            )
        }

        val revisions = queries.getPendingSyncRevisions().executeAsList().map {
            EntryRevisionSyncDto(
                id = it.id,
                entry_id = it.entry_id,
                entry_kind = it.entry_kind,
                revision = it.revision.toInt(),
                change_kind = it.change_kind,
                snapshot_json = it.snapshot_json,
                void_reason = it.void_reason,
                changed_at = it.changed_at
            )
        }

        if (parties.isEmpty() && deals.isEmpty() && txs.isEmpty() && revisions.isEmpty()) {
            return@withContext Result.success(0)
        }

        val req = SyncPushRequestDto(
            parties = parties,
            deals = deals,
            transactions = txs,
            revisions = revisions
        )

        client.pushSync(req).map { res ->
            res.synced_parties.forEach { queries.markPartySynced(it) }
            deals.filter { res.synced_deals.contains(it.id) }.forEach {
                queries.markDealSynced(it.id, it.revision.toLong())
            }
            txs.filter { res.synced_transactions.contains(it.id) }.forEach {
                queries.markTransactionSynced(it.id, it.revision.toLong())
            }
            res.synced_revisions.forEach { queries.markRevisionSynced(it) }
            res.synced_parties.size + res.synced_deals.size + res.synced_transactions.size + res.synced_revisions.size
        }
    }

    suspend fun pullRemoteChanges(sinceMs: Long): Result<Int> = withContext(ioDispatcher) {
        val client = apiClient ?: return@withContext Result.failure(IllegalStateException("No remote API client configured"))

        client.pullSync(sinceMs).map { res ->
            database.transaction {
                res.parties.forEach { p ->
                    queries.insertParty(
                        id = p.id,
                        shop_id = "shop_default",
                        name = p.name,
                        phone = p.phone,
                        village = p.village,
                        party_type = p.role,
                        monthly_interest_rate = p.monthly_interest_rate,
                        photo_uri = p.photo_uri,
                        created_at = sinceMs,
                        updated_at = res.server_sync_time,
                        is_deleted = p.is_deleted.toLong(),
                        sync_status = 1L
                    )
                }
            }
            res.parties.size + res.deals.size + res.transactions.size + res.revisions.size
        }
    }

    suspend fun syncFull(sinceMs: Long): SyncResult = withContext(ioDispatcher) {
        try {
            val pushed = pushPendingChanges().getOrThrow()
            val pulled = pullRemoteChanges(sinceMs).getOrThrow()
            SyncResult.Success(pushedCount = pushed, pulledCount = pulled, syncTimeMs = sinceMs)
        } catch (t: Throwable) {
            SyncResult.Failure(t)
        }
    }

    suspend fun markAllBatchSynced(partyIds: List<String>, deals: List<Pair<String, Int>>, txs: List<Pair<String, Int>>) = withContext(ioDispatcher) {
        partyIds.forEach { queries.markPartySynced(it) }
        deals.forEach { (id, revision) -> queries.markDealSynced(id, revision.toLong()) }
        txs.forEach { (id, revision) -> queries.markTransactionSynced(id, revision.toLong()) }
    }
}

