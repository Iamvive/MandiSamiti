package com.appwork.mandisamiti.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.CashTransactionEntity
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.domain.repository.CashTransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

class OfflineFirstCashTransactionRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: Clock = Clock.System
) : CashTransactionRepository {

    private val queries = database.appDatabaseQueries
    private val json = Json { encodeDefaults = true }

    override fun getTransactionsByPartyStream(partyId: String): Flow<List<CashTransaction>> {
        return queries.getCashTransactionsByParty(partyId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByShopStream(shopId: String): Flow<List<CashTransaction>> {
        return queries.getCashTransactionsByShop(shopId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun recordTransaction(transaction: CashTransaction) = withContext(ioDispatcher) {
        database.transaction {
            val created = transaction.copy(revision = 1, syncStatus = 0)
            insertOrReplace(created)
            recordRevision(created, "CREATE", created.updatedAt)
        }
    }

    override suspend fun voidTransaction(transactionId: String, reason: VoidReason) = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transaction {
            val current = queries.getCashTransactionById(transactionId).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Transaction $transactionId does not exist")
            if (current.isVoid) return@transaction
            ensureBaselineRevision(current)
            val voided = current.copy(
                isVoid = true, voidReason = reason, revision = current.revision + 1,
                updatedAt = now, syncStatus = 0
            )
            insertOrReplace(voided)
            recordRevision(voided, "VOID", now)

            // Cascade to settlements / lends / expenses
            val remarks = current.remarks ?: ""
            if (remarks.contains("[SETTLE:")) {
                val tag = remarks.substringAfter("[SETTLE:").substringBefore("]")
                val parts = tag.split(":")
                val settleId = parts.getOrNull(0)
                val lendId = parts.getOrNull(1)
                if (settleId != null) {
                    val sEntity = queries.getLendSettlementById(settleId).executeAsOneOrNull()
                    if (sEntity != null && sEntity.is_void == 0L) {
                        queries.insertLendSettlement(
                            id = sEntity.id,
                            shop_id = sEntity.shop_id,
                            lend_id = sEntity.lend_id,
                            settled_at = sEntity.settled_at,
                            amount_paisa = sEntity.amount_paisa,
                            weight_grams = sEntity.weight_grams,
                            bags_count = sEntity.bags_count,
                            created_at = sEntity.created_at,
                            updated_at = now,
                            sync_status = 0L,
                            revision = sEntity.revision + 1L,
                            is_void = 1L,
                            void_reason = reason.name
                        )
                        cascadeRevision(sEntity.shop_id, sEntity.id, "LEND_SETTLEMENT", sEntity.revision + 1L, "VOID", reason, now)
                    }
                    if (lendId != null) {
                        val lendEntity = queries.getLendEntryById(lendId).executeAsOneOrNull()
                        if (lendEntity != null && lendEntity.is_void == 0L) {
                            val activeSettlements = queries.getLendSettlementsByLend(lendId).executeAsList()
                                .filter { it.id != settleId && it.is_void == 0L }
                            val repaidAmount = activeSettlements.sumOf { it.amount_paisa }
                            val repaidGrams = activeSettlements.sumOf { it.weight_grams }
                            val newStatus = when (lendEntity.lend_type) {
                                "MONEY" -> when {
                                    repaidAmount >= lendEntity.amount_paisa -> "SETTLED"
                                    repaidAmount > 0 -> "PARTIAL"
                                    else -> "OPEN"
                                }
                                else -> when {
                                    repaidGrams >= lendEntity.weight_grams -> "SETTLED"
                                    repaidGrams > 0 -> "PARTIAL"
                                    else -> "OPEN"
                                }
                            }
                            queries.insertLendEntry(
                                id = lendEntity.id,
                                shop_id = lendEntity.shop_id,
                                party_id = lendEntity.party_id,
                                lend_type = lendEntity.lend_type,
                                entry_date = lendEntity.entry_date,
                                amount_paisa = lendEntity.amount_paisa,
                                commodity_id = lendEntity.commodity_id,
                                bags_count = lendEntity.bags_count,
                                weight_grams = lendEntity.weight_grams,
                                expected_return_date = lendEntity.expected_return_date,
                                notes = lendEntity.notes,
                                status = newStatus,
                                created_at = lendEntity.created_at,
                                updated_at = now,
                                is_deleted = lendEntity.is_deleted,
                                sync_status = 0L,
                                revision = lendEntity.revision + 1L,
                                is_void = lendEntity.is_void,
                                void_reason = lendEntity.void_reason
                            )
                            cascadeRevision(lendEntity.shop_id, lendEntity.id, "LEND", lendEntity.revision + 1L, "EDIT", null, now)
                        }
                    }
                }
            } else if (remarks.contains("[LEND:")) {
                val lendId = remarks.substringAfter("[LEND:").substringBefore("]")
                // Shared cascade: voids the lend, its settlements and their [SETTLE:] cash rows, with revisions.
                OfflineFirstLendRepository(database, ioDispatcher, clock).voidLendInTransaction(lendId, reason, now)
            } else if (remarks.contains("[EXPENSE:")) {
                val expId = remarks.substringAfter("[EXPENSE:").substringBefore("]")
                OfflineFirstExpenseRepository(database, ioDispatcher, clock).voidExpenseInTransaction(expId, reason, now)
            }
        }
    }

    private fun cascadeRevision(shopId: String, entryId: String, kind: String, revision: Long, change: String, reason: VoidReason?, at: Long) {
        queries.insertRevision(
            id = IdGenerator.newId(), shop_id = shopId, entry_id = entryId, entry_kind = kind,
            revision = revision, change_kind = change, snapshot_json = "", void_reason = reason?.name, changed_at = at,
        )
    }

    /** Rows written before schema v2 have no audit trail; record them as-is as their CREATE revision. */
    private fun ensureBaselineRevision(current: CashTransaction) {
        if (queries.getRevisionsForEntry(current.id).executeAsList().isEmpty()) {
            recordRevision(current, "CREATE", current.createdAt)
        }
    }

    private fun recordRevision(tx: CashTransaction, changeKind: String, at: Long) {
        queries.insertRevision(
            id = IdGenerator.newId(),
            shop_id = tx.shopId,
            entry_id = tx.id,
            entry_kind = "CASH",
            revision = tx.revision.toLong(),
            change_kind = changeKind,
            snapshot_json = json.encodeToString(CashTransaction.serializer(), tx),
            void_reason = tx.voidReason?.name,
            changed_at = at
        )
    }

    private fun insertOrReplace(transaction: CashTransaction) {
        queries.insertCashTransaction(
            id = transaction.id,
            shop_id = transaction.shopId,
            party_id = transaction.partyId,
            deal_id = transaction.dealId,
            transaction_type = transaction.transactionType.name,
            amount_paisa = transaction.amountPaisa,
            payment_mode = transaction.paymentMode.name,
            transaction_date = transaction.transactionDate,
            voice_note_uri = transaction.voiceNoteUri,
            remarks = transaction.remarks,
            created_at = transaction.createdAt,
            updated_at = transaction.updatedAt,
            is_deleted = if (transaction.isDeleted) 1L else 0L,
            sync_status = transaction.syncStatus.toLong(),
            revision = transaction.revision.toLong(),
            is_void = if (transaction.isVoid) 1L else 0L,
            void_reason = transaction.voidReason?.name,
        )
    }

    private fun CashTransactionEntity.toDomain(): CashTransaction {
        return CashTransaction(
            id = id,
            shopId = shop_id,
            partyId = party_id,
            dealId = deal_id,
            transactionType = TransactionType.valueOf(transaction_type),
            amountPaisa = amount_paisa,
            paymentMode = PaymentMode.valueOf(payment_mode),
            transactionDate = transaction_date,
            voiceNoteUri = voice_note_uri,
            remarks = remarks,
            createdAt = created_at,
            updatedAt = updated_at,
            isDeleted = is_deleted == 1L,
            syncStatus = sync_status.toInt(),
            revision = revision.toInt(),
            isVoid = is_void == 1L,
            voidReason = void_reason?.let { VoidReason.valueOf(it) }
        )
    }
}
