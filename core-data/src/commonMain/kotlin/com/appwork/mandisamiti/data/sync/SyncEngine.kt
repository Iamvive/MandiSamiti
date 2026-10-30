package com.appwork.mandisamiti.data.sync

import com.appwork.mandisamiti.data.sync.model.*
import com.appwork.mandisamiti.domain.model.normalizePhone
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
        /** Pull cursor is per shop, so another shop's (or a stale) cursor can never start a download late. */
        fun lastServerSeqKey(shopId: String) = "last_server_seq:$shopId"
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

    suspend fun getLastServerSeq(shopId: String): Long = withContext(ioDispatcher) {
        queries.getSyncMetadataLong(lastServerSeqKey(shopId)).executeAsOneOrNull() ?: 0L
    }

    suspend fun setLastServerSeq(shopId: String, seq: Long) = withContext(ioDispatcher) {
        queries.setSyncMetadataLong(lastServerSeqKey(shopId), seq)
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

        val buyEntries = queries.getPendingSyncBuyEntries().executeAsList().map {
            BuyEntrySyncDto(
                id = it.id, farmer_id = it.farmer_id, commodity_id = it.commodity_id, entry_date = it.entry_date,
                bags_count = it.bags_count, weight_per_bag_grams = it.weight_per_bag_grams,
                extra_weight_grams = it.extra_weight_grams, total_weight_grams = it.total_weight_grams,
                rate_paisa_per_quintal = it.rate_paisa_per_quintal, commission_bps = it.commission_bps,
                labour_per_bag_paisa = it.labour_per_bag_paisa, advance_paid_paisa = it.advance_paid_paisa,
                gross_paisa = it.gross_paisa, commission_paisa = it.commission_paisa, labour_paisa = it.labour_paisa,
                net_payable_paisa = it.net_payable_paisa, paid_paisa = it.paid_paisa,
                vehicle_number = it.vehicle_number, slip_number = it.slip_number, slip_no = it.slip_no, remarks = it.remarks,
                is_void = it.is_void.toInt(), void_reason = it.void_reason, revision = it.revision.toInt(),
                is_deleted = it.is_deleted.toInt(), created_at = it.created_at, updated_at = it.updated_at
            )
        }

        val sellEntries = queries.getPendingSyncSellEntries().executeAsList().map {
            SellEntrySyncDto(
                id = it.id, buyer_id = it.buyer_id, commodity_id = it.commodity_id, entry_date = it.entry_date,
                bill_mode = it.bill_mode, party_kind = it.party_kind, gstin = it.gstin, bags_count = it.bags_count,
                weight_per_bag_grams = it.weight_per_bag_grams, total_weight_grams = it.total_weight_grams,
                rate_paisa_per_quintal = it.rate_paisa_per_quintal, arat_bps = it.arat_bps, tulak_paisa = it.tulak_paisa,
                gross_paisa = it.gross_paisa, arat_paisa = it.arat_paisa, mandi_shulk_paisa = it.mandi_shulk_paisa,
                total_receivable_paisa = it.total_receivable_paisa, received_paisa = it.received_paisa,
                balance_due_paisa = it.balance_due_paisa, vehicle_number = it.vehicle_number,
                invoice_number = it.invoice_number, slip_no = it.slip_no, remarks = it.remarks, is_void = it.is_void.toInt(),
                void_reason = it.void_reason, revision = it.revision.toInt(), is_deleted = it.is_deleted.toInt(),
                created_at = it.created_at, updated_at = it.updated_at
            )
        }

        val lendEntries = queries.getPendingSyncLendEntries().executeAsList().map {
            LendEntrySyncDto(
                id = it.id, party_id = it.party_id, lend_type = it.lend_type, entry_date = it.entry_date,
                amount_paisa = it.amount_paisa, commodity_id = it.commodity_id, bags_count = it.bags_count,
                weight_grams = it.weight_grams, expected_return_date = it.expected_return_date, slip_no = it.slip_no, notes = it.notes,
                status = it.status, is_void = it.is_void.toInt(), void_reason = it.void_reason,
                revision = it.revision.toInt(), is_deleted = it.is_deleted.toInt(),
                created_at = it.created_at, updated_at = it.updated_at
            )
        }

        val lendSettlements = queries.getPendingSyncLendSettlements().executeAsList().map {
            LendSettlementSyncDto(
                id = it.id, lend_id = it.lend_id, settled_at = it.settled_at, amount_paisa = it.amount_paisa,
                weight_grams = it.weight_grams, bags_count = it.bags_count, is_void = it.is_void.toInt(),
                void_reason = it.void_reason, revision = it.revision.toInt(),
                created_at = it.created_at, updated_at = it.updated_at,
                principal_paisa = it.principal_paisa,
                interest_paisa = it.interest_paisa,
                calculated_interest_paisa = it.calculated_interest_paisa
            )
        }

        val expenses = queries.getPendingSyncExpenses().executeAsList().map {
            ExpenseEntrySyncDto(
                id = it.id, entry_date = it.entry_date, category = it.category, amount_paisa = it.amount_paisa,
                paid_to = it.paid_to, payment_mode = it.payment_mode, remarks = it.remarks,
                is_void = it.is_void.toInt(), void_reason = it.void_reason, revision = it.revision.toInt(),
                is_deleted = it.is_deleted.toInt(), created_at = it.created_at, updated_at = it.updated_at
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

        if (parties.isEmpty() && deals.isEmpty() && txs.isEmpty() && buyEntries.isEmpty() && sellEntries.isEmpty() &&
            lendEntries.isEmpty() && lendSettlements.isEmpty() && expenses.isEmpty() && revisions.isEmpty()
        ) {
            return@withContext Result.success(0)
        }

        var acked = 0
        // One kind per request, parents before children (parties -> deals -> cash -> buy/sell -> lend -> settlements -> expenses -> revisions),
        // so the server's foreign keys are always satisfied by earlier, already-acked requests.
        val requests = parties.chunked(PUSH_BATCH).map { SyncPushRequestDto(parties = it) } +
            deals.chunked(PUSH_BATCH).map { SyncPushRequestDto(deals = it) } +
            txs.chunked(PUSH_BATCH).map { SyncPushRequestDto(transactions = it) } +
            buyEntries.chunked(PUSH_BATCH).map { SyncPushRequestDto(buy_entries = it) } +
            sellEntries.chunked(PUSH_BATCH).map { SyncPushRequestDto(sell_entries = it) } +
            lendEntries.chunked(PUSH_BATCH).map { SyncPushRequestDto(lend_entries = it) } +
            lendSettlements.chunked(PUSH_BATCH).map { SyncPushRequestDto(lend_settlements = it) } +
            expenses.chunked(PUSH_BATCH).map { SyncPushRequestDto(expenses = it) } +
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
                req.buy_entries.filter { res.synced_buy_entries.contains(it.id) }.forEach {
                    queries.markBuyEntrySynced(it.id, it.revision.toLong())
                    acked++
                }
                req.sell_entries.filter { res.synced_sell_entries.contains(it.id) }.forEach {
                    queries.markSellEntrySynced(it.id, it.revision.toLong())
                    acked++
                }
                req.lend_entries.filter { res.synced_lend_entries.contains(it.id) }.forEach {
                    queries.markLendEntrySynced(it.id, it.revision.toLong())
                    acked++
                }
                req.lend_settlements.filter { res.synced_lend_settlements.contains(it.id) }.forEach {
                    queries.markLendSettlementSynced(it.id, it.revision.toLong())
                    acked++
                }
                req.expenses.filter { res.synced_expenses.contains(it.id) }.forEach {
                    queries.markExpenseSynced(it.id, it.revision.toLong())
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
            val currentSeq = getLastServerSeq(shopId)
            val pageResult = client.pullSync(afterSeq = currentSeq, limit = DEFAULT_PAGE_LIMIT)

            if (pageResult.isFailure) {
                return@withContext Result.failure(pageResult.exceptionOrNull()!!)
            }

            val res = pageResult.getOrThrow()
            val pageItemsCount = res.parties.size + res.deals.size + res.transactions.size +
                res.buy_entries.size + res.sell_entries.size + res.lend_entries.size +
                res.lend_settlements.size + res.expenses.size + res.revisions.size

            val appliedResult = runCatching { database.transactionWithResult {
                // Logged out / switched shop while this page was in flight: write nothing, not even the cursor.
                if (!queries.shopProfileExists(shopId).executeAsOne()) rollback(false)
                res.parties.forEach { p ->
                    queries.deleteSyncedParty(p.id)
                    // Preserve a pending local edit with the same id. Every other collision must fail the page so
                    // the transaction rolls back and the server cursor is not advanced past a lost party.
                    if (queries.getPartyByIdAny(p.id).executeAsOneOrNull()?.sync_status != 0L) {
                        queries.insertPartyFromServer(
                            id = p.id,
                            shop_id = shopId,
                            name = p.name,
                            phone = normalizePhone(p.phone),
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

                res.buy_entries.forEach { b ->
                    queries.deleteSyncedBuyEntry(b.id)
                    queries.insertBuyEntryIfAbsent(
                        id = b.id,
                        shop_id = shopId,
                        farmer_id = b.farmer_id,
                        commodity_id = b.commodity_id,
                        entry_date = b.entry_date.takeIf { it > 0 } ?: res.server_sync_time,
                        bags_count = b.bags_count,
                        weight_per_bag_grams = b.weight_per_bag_grams,
                        extra_weight_grams = b.extra_weight_grams,
                        total_weight_grams = b.total_weight_grams,
                        rate_paisa_per_quintal = b.rate_paisa_per_quintal,
                        commission_bps = b.commission_bps,
                        labour_per_bag_paisa = b.labour_per_bag_paisa,
                        advance_paid_paisa = b.advance_paid_paisa,
                        gross_paisa = b.gross_paisa,
                        commission_paisa = b.commission_paisa,
                        labour_paisa = b.labour_paisa,
                        net_payable_paisa = b.net_payable_paisa,
                        paid_paisa = b.paid_paisa,
                        vehicle_number = b.vehicle_number,
                        slip_number = b.slip_number,
                        slip_no = b.slip_no,
                        remarks = b.remarks,
                        created_at = b.created_at.takeIf { it > 0 } ?: res.server_sync_time,
                        updated_at = b.updated_at.takeIf { it > 0 } ?: res.server_sync_time,
                        is_deleted = b.is_deleted.toLong(),
                        sync_status = 1L,
                        revision = b.revision.toLong(),
                        is_void = b.is_void.toLong(),
                        void_reason = b.void_reason
                    )
                }

                res.sell_entries.forEach { s ->
                    queries.deleteSyncedSellEntry(s.id)
                    queries.insertSellEntryIfAbsent(
                        id = s.id,
                        shop_id = shopId,
                        buyer_id = s.buyer_id,
                        commodity_id = s.commodity_id,
                        entry_date = s.entry_date.takeIf { it > 0 } ?: res.server_sync_time,
                        bill_mode = s.bill_mode,
                        party_kind = s.party_kind,
                        gstin = s.gstin,
                        bags_count = s.bags_count,
                        weight_per_bag_grams = s.weight_per_bag_grams,
                        total_weight_grams = s.total_weight_grams,
                        rate_paisa_per_quintal = s.rate_paisa_per_quintal,
                        arat_bps = s.arat_bps,
                        tulak_paisa = s.tulak_paisa,
                        gross_paisa = s.gross_paisa,
                        arat_paisa = s.arat_paisa,
                        mandi_shulk_paisa = s.mandi_shulk_paisa,
                        total_receivable_paisa = s.total_receivable_paisa,
                        received_paisa = s.received_paisa,
                        balance_due_paisa = s.balance_due_paisa,
                        vehicle_number = s.vehicle_number,
                        invoice_number = s.invoice_number,
                        slip_no = s.slip_no,
                        remarks = s.remarks,
                        created_at = s.created_at.takeIf { it > 0 } ?: res.server_sync_time,
                        updated_at = s.updated_at.takeIf { it > 0 } ?: res.server_sync_time,
                        is_deleted = s.is_deleted.toLong(),
                        sync_status = 1L,
                        revision = s.revision.toLong(),
                        is_void = s.is_void.toLong(),
                        void_reason = s.void_reason
                    )
                }

                res.lend_entries.forEach { l ->
                    queries.deleteSyncedLendEntry(l.id)
                    queries.insertLendEntryIfAbsent(
                        id = l.id,
                        shop_id = shopId,
                        party_id = l.party_id,
                        lend_type = l.lend_type,
                        entry_date = l.entry_date.takeIf { it > 0 } ?: res.server_sync_time,
                        amount_paisa = l.amount_paisa,
                        commodity_id = l.commodity_id,
                        bags_count = l.bags_count,
                        weight_grams = l.weight_grams,
                        expected_return_date = l.expected_return_date,
                        slip_no = l.slip_no,
                        notes = l.notes,
                        status = l.status,
                        created_at = l.created_at.takeIf { it > 0 } ?: res.server_sync_time,
                        updated_at = l.updated_at.takeIf { it > 0 } ?: res.server_sync_time,
                        is_deleted = l.is_deleted.toLong(),
                        sync_status = 1L,
                        revision = l.revision.toLong(),
                        is_void = l.is_void.toLong(),
                        void_reason = l.void_reason
                    )
                }

                res.lend_settlements.forEach { s ->
                    queries.deleteSyncedLendSettlement(s.id)
                    queries.insertLendSettlementIfAbsent(
                        id = s.id,
                        shop_id = shopId,
                        lend_id = s.lend_id,
                        settled_at = s.settled_at.takeIf { it > 0 } ?: res.server_sync_time,
                        amount_paisa = s.amount_paisa,
                        weight_grams = s.weight_grams,
                        bags_count = s.bags_count,
                        created_at = s.created_at.takeIf { it > 0 } ?: res.server_sync_time,
                        updated_at = s.updated_at.takeIf { it > 0 } ?: res.server_sync_time,
                        sync_status = 1L,
                        revision = s.revision.toLong(),
                        is_void = s.is_void.toLong(),
                        void_reason = s.void_reason,
                        principal_paisa = if (s.principal_paisa != 0L || s.interest_paisa != 0L) s.principal_paisa else s.amount_paisa,
                        interest_paisa = s.interest_paisa,
                        calculated_interest_paisa = s.calculated_interest_paisa,
                    )
                }

                res.expenses.forEach { e ->
                    queries.deleteSyncedExpenseEntry(e.id)
                    queries.insertExpenseEntryIfAbsent(
                        id = e.id,
                        shop_id = shopId,
                        entry_date = e.entry_date.takeIf { it > 0 } ?: res.server_sync_time,
                        category = e.category,
                        amount_paisa = e.amount_paisa,
                        paid_to = e.paid_to,
                        payment_mode = e.payment_mode,
                        remarks = e.remarks,
                        created_at = e.created_at.takeIf { it > 0 } ?: res.server_sync_time,
                        updated_at = e.updated_at.takeIf { it > 0 } ?: res.server_sync_time,
                        is_deleted = e.is_deleted.toLong(),
                        sync_status = 1L,
                        revision = e.revision.toLong(),
                        is_void = e.is_void.toLong(),
                        void_reason = e.void_reason
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
                    queries.setSyncMetadataLong(lastServerSeqKey(shopId), res.next_seq)
                }
                true
            } }
            if (appliedResult.isFailure) {
                return@withContext Result.failure(appliedResult.exceptionOrNull()!!)
            }
            val applied = appliedResult.getOrThrow()
            if (!applied) {
                return@withContext Result.failure(IllegalStateException("Shop $shopId is no longer on this phone"))
            }

            if (pageItemsCount > 0) {
                totalPulled += pageItemsCount
                onProgress?.invoke(totalPulled)
            }

            hasMore = res.has_more && res.next_seq > currentSeq
        }

        Result.success(totalPulled)
    }

    /** Pending local edits go up first, so a pull can't leave a stale local copy that a later push wrongly acks. */
    suspend fun pushThenPull(shopId: String, onProgress: ((Int) -> Unit)? = null): Result<Int> =
        pushPendingChanges().fold(onSuccess = { pullRemoteChanges(shopId, onProgress) }, onFailure = { Result.failure(it) })

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
