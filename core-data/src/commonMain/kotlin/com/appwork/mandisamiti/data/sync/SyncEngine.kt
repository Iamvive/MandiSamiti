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

    companion object {
        const val KEY_LAST_SERVER_SEQ = "last_server_seq"
        const val KEY_NEEDS_LOGIN = "needs_login"
        const val DEFAULT_PAGE_LIMIT = 500
        const val PUSH_BATCH = 100
    }

    suspend fun needsLogin(): Boolean = withContext(ioDispatcher) {
        queries.getSyncMetadataLong(KEY_NEEDS_LOGIN).executeAsOneOrNull() == 1L
    }

    suspend fun setNeedsLogin(value: Boolean) = withContext(ioDispatcher) {
        queries.setSyncMetadataLong(KEY_NEEDS_LOGIN, if (value) 1L else 0L)
    }

    suspend fun getLastServerSeq(): Long = withContext(ioDispatcher) {
        queries.getSyncMetadataLong(KEY_LAST_SERVER_SEQ).executeAsOneOrNull() ?: 0L
    }

    suspend fun setLastServerSeq(seq: Long) = withContext(ioDispatcher) {
        queries.setSyncMetadataLong(KEY_LAST_SERVER_SEQ, seq)
    }

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

    /** Distinct unsynced user entries (what the user sees); [getPendingCount] stays the broad safety check. */
    suspend fun getPendingEntryCount(): Long = withContext(ioDispatcher) {
        queries.getPendingEntryCount().executeAsOne()
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
                is_deleted = it.is_deleted.toInt(),
                created_at = it.created_at,
                updated_at = it.updated_at
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
                is_deleted = it.is_deleted.toInt(),
                created_at = it.created_at,
                updated_at = it.updated_at
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
                is_deleted = it.is_deleted.toInt(),
                created_at = it.created_at,
                updated_at = it.updated_at
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

        var acked = 0
        // One kind per request, parents before children (parties -> deals -> cash -> revisions),
        // so the server's foreign keys are always satisfied by earlier, already-acked requests.
        val requests = parties.chunked(PUSH_BATCH).map { SyncPushRequestDto(parties = it) } +
            deals.chunked(PUSH_BATCH).map { SyncPushRequestDto(deals = it) } +
            txs.chunked(PUSH_BATCH).map { SyncPushRequestDto(transactions = it) } +
            revisions.chunked(PUSH_BATCH).map { SyncPushRequestDto(revisions = it) }
        for (req in requests) {
            val res = client.pushSync(req).getOrElse { return@withContext Result.failure(it) }
            database.transaction {
                req.parties.filter { res.synced_parties.contains(it.id) }.forEach {
                    queries.markPartySyncedAt(it.id, it.updated_at)
                    acked++
                }
                req.deals.filter { res.synced_deals.contains(it.id) }.forEach {
                    queries.markDealSynced(it.id, it.revision.toLong())
                    acked++
                }
                req.transactions.filter { res.synced_transactions.contains(it.id) }.forEach {
                    queries.markTransactionSynced(it.id, it.revision.toLong())
                    acked++
                }
                // Conflicted revisions are stored aside by the server and also listed here.
                res.synced_revisions.forEach {
                    queries.markRevisionSynced(it)
                    acked++
                }
            }
        }
        Result.success(acked)
    }

    /** Merges server rows into the local DB under [shopId], paginating through all available pages. */
    suspend fun pullRemoteChanges(
        shopId: String,
        onProgress: ((downloadedCount: Int) -> Unit)? = null
    ): Result<Int> = withContext(ioDispatcher) {
        val client = apiClient ?: return@withContext Result.failure(IllegalStateException("No remote API client configured"))

        var totalPulled = 0
        var hasMore = true

        while (hasMore) {
            val currentSeq = getLastServerSeq()
            val pageResult = client.pullSync(afterSeq = currentSeq, limit = DEFAULT_PAGE_LIMIT)

            if (pageResult.isFailure) {
                return@withContext Result.failure(pageResult.exceptionOrNull()!!)
            }

            val res = pageResult.getOrThrow()
            val pageItemsCount = res.parties.size + res.deals.size + res.transactions.size + res.revisions.size

            database.transaction {
                res.parties.forEach { p ->
                    queries.deleteSyncedParty(p.id)
                    queries.insertPartyIfAbsent(
                        id = p.id,
                        shop_id = shopId,
                        name = p.name,
                        phone = p.phone,
                        village = p.village,
                        party_type = p.role,
                        monthly_interest_rate = p.monthly_interest_rate,
                        photo_uri = p.photo_uri,
                        created_at = p.created_at.takeIf { it > 0 } ?: res.server_sync_time,
                        updated_at = p.updated_at.takeIf { it > 0 } ?: res.server_sync_time,
                        is_deleted = p.is_deleted.toLong(),
                        sync_status = 1L
                    )
                }

                res.deals.forEach { d ->
                    queries.deleteSyncedDeal(d.id)
                    queries.insertDealIfAbsent(
                        id = d.id,
                        shop_id = shopId,
                        farmer_id = d.farmer_id,
                        buyer_id = d.buyer_id,
                        commodity_id = d.commodity,
                        deal_status = d.deal_status,
                        deal_date = d.deal_date ?: res.server_sync_time,
                        bags_count = d.bags_count.toLong(),
                        gross_weight_grams = d.gross_weight_grams,
                        cut_weight_grams = d.cut_weight_grams,
                        net_weight_grams = d.net_weight_grams,
                        rate_paisa_per_unit = d.rate_paisa_per_unit,
                        gross_amount_paisa = d.gross_amount_paisa,
                        farmer_commission_paisa = d.farmer_commission_paisa,
                        buyer_commission_paisa = d.buyer_commission_paisa,
                        labour_charge_paisa = d.labour_charge_paisa,
                        weighing_charge_paisa = d.weighing_charge_paisa,
                        other_deductions_paisa = d.other_deductions_paisa,
                        net_farmer_payable_paisa = d.net_farmer_payable_paisa,
                        net_buyer_receivable_paisa = d.net_buyer_receivable_paisa,
                        receipt_photo_uri = d.receipt_photo_uri,
                        voice_note_uri = d.voice_note_uri,
                        remarks = d.remarks,
                        created_at = d.created_at.takeIf { it > 0 } ?: res.server_sync_time,
                        updated_at = d.updated_at.takeIf { it > 0 } ?: res.server_sync_time,
                        is_deleted = d.is_deleted.toLong(),
                        sync_status = 1L,
                        farmer_commission_bps = d.farmer_commission_bps.toLong(),
                        revision = d.revision.toLong(),
                        is_void = d.is_void.toLong(),
                        void_reason = d.void_reason
                    )
                }

                res.transactions.forEach { t ->
                    queries.deleteSyncedCashTransaction(t.id)
                    queries.insertCashTransactionIfAbsent(
                        id = t.id,
                        shop_id = shopId,
                        party_id = t.party_id ?: "",
                        deal_id = t.deal_id,
                        transaction_type = t.transaction_type,
                        amount_paisa = t.amount_paisa,
                        payment_mode = t.payment_mode,
                        transaction_date = t.transaction_date ?: res.server_sync_time,
                        voice_note_uri = t.voice_note_uri,
                        remarks = t.remarks,
                        created_at = t.created_at.takeIf { it > 0 } ?: res.server_sync_time,
                        updated_at = t.updated_at.takeIf { it > 0 } ?: res.server_sync_time,
                        is_deleted = t.is_deleted.toLong(),
                        sync_status = 1L,
                        revision = t.revision.toLong(),
                        is_void = t.is_void.toLong(),
                        void_reason = t.void_reason
                    )
                }

                res.revisions.forEach { r ->
                    queries.insertRevisionFromServer(
                        id = r.id,
                        shop_id = shopId,
                        entry_id = r.entry_id,
                        entry_kind = r.entry_kind,
                        revision = r.revision.toLong(),
                        change_kind = r.change_kind,
                        snapshot_json = r.snapshot_json,
                        void_reason = r.void_reason,
                        changed_at = r.changed_at ?: res.server_sync_time
                    )
                }

                // Same transaction as the page: a crash can't skip or replay it.
                if (res.next_seq > currentSeq) {
                    queries.setSyncMetadataLong(KEY_LAST_SERVER_SEQ, res.next_seq)
                }
            }

            if (pageItemsCount > 0) {
                totalPulled += pageItemsCount
                onProgress?.invoke(totalPulled)
            }

            hasMore = res.has_more && res.next_seq > currentSeq
        }

        Result.success(totalPulled)
    }

    suspend fun syncFull(shopId: String): SyncResult = withContext(ioDispatcher) {
        try {
            val pushed = pushPendingChanges().getOrThrow()
            val pulled = pullRemoteChanges(shopId).getOrThrow()
            setNeedsLogin(false)
            SyncResult.Success(pushedCount = pushed, pulledCount = pulled, syncTimeMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
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
