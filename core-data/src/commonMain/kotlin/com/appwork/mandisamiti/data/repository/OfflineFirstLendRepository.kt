package com.appwork.mandisamiti.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.LendEntryEntity
import com.appwork.mandisamiti.database.LendSettlementEntity
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.domain.model.LendStatus
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.domain.repository.LendRepository
import com.appwork.mandisamiti.domain.usecase.LendException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

class OfflineFirstLendRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: Clock = Clock.System,
) : LendRepository {

    private val queries = database.appDatabaseQueries
    private val json = Json { encodeDefaults = true }

    override fun getLendEntriesStream(shopId: String): Flow<List<LendEntry>> =
        queries.getLendEntriesByShop(shopId).asFlow().mapToList(ioDispatcher).map { list -> list.map { it.toDomain() } }

    override fun getSettlementsStream(shopId: String): Flow<List<LendSettlement>> =
        queries.getLendSettlementsByShop(shopId).asFlow().mapToList(ioDispatcher).map { list -> list.map { it.toDomain() } }

    override suspend fun getLendEntryById(id: String): LendEntry? = withContext(ioDispatcher) {
        queries.getLendEntryById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getSettlements(lendId: String): List<LendSettlement> = withContext(ioDispatcher) {
        queries.getLendSettlementsByLend(lendId).executeAsList().map { it.toDomain() }
    }

    override suspend fun saveLendEntry(entry: LendEntry) = withContext(ioDispatcher) {
        database.transaction {
            val created = entry.copy(revision = 1, syncStatus = 0)
            insertOrReplace(created)
            recordLendRevision(created, "CREATE", created.updatedAt)

            if (entry.partyId.isNotBlank()) {
                queries.insertPartyIfAbsent(
                    id = entry.partyId,
                    shop_id = entry.shopId,
                    name = entry.partyId,
                    phone = null,
                    village = null,
                    party_type = "FARMER",
                    monthly_interest_rate = null,
                    photo_uri = null,
                    created_at = created.createdAt,
                    updated_at = created.updatedAt,
                    is_deleted = 0L,
                    sync_status = 0L
                )
            }
        }
    }

    override suspend fun settle(settlement: LendSettlement, cashEntry: CashTransaction?) = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transaction {
            val lend = queries.getLendEntryById(settlement.lendId).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Lend ${settlement.lendId} does not exist")
            require(!lend.isVoid) { "Lend ${settlement.lendId} is void" }

            val live = queries.getLendSettlementsByLend(lend.id).executeAsList().map { it.toDomain() }.filter { !it.isVoid }
            if (lend.lendType == LendType.MONEY) {
                val outstanding = (lend.amountPaisa - live.sumOf { it.amountPaisa }).coerceAtLeast(0L)
                if (settlement.amountPaisa > outstanding) throw LendException.OverRepayment(settlement.amountPaisa, outstanding)
            } else {
                val outstanding = (lend.weightGrams - live.sumOf { it.weightGrams }).coerceAtLeast(0L)
                if (settlement.weightGrams > outstanding) throw LendException.OverRepaymentWeight(settlement.weightGrams, outstanding)
            }

            val created = settlement.copy(revision = 1, syncStatus = 0)
            insertSettlement(created)
            if (cashEntry != null) insertCashEntry(cashEntry.copy(revision = 1, syncStatus = 0), now)
            queries.insertRevision(
                id = IdGenerator.newId(),
                shop_id = created.shopId,
                entry_id = created.id,
                entry_kind = "LEND_SETTLEMENT",
                revision = created.revision,
                change_kind = "CREATE",
                snapshot_json = json.encodeToString(LendSettlement.serializer(), created),
                void_reason = created.voidReason,
                changed_at = now,
            )

            val settlements = queries.getLendSettlementsByLend(lend.id).executeAsList().map { it.toDomain() }
                .filter { !it.isVoid }
            val repaidAmount = settlements.sumOf { it.amountPaisa }
            val repaidGrams = settlements.sumOf { it.weightGrams }
            val isSettled = when (lend.lendType) {
                LendType.MONEY -> repaidAmount >= lend.amountPaisa
                LendType.GRAIN -> repaidGrams >= lend.weightGrams
            }
            val updatedLend = lend.copy(
                status = if (isSettled) LendStatus.SETTLED else LendStatus.PARTIAL,
                revision = lend.revision + 1,
                updatedAt = now,
                syncStatus = 0,
            )
            insertOrReplace(updatedLend)
            recordLendRevision(updatedLend, "EDIT", now)
        }
    }

    private fun insertCashEntry(tx: CashTransaction, now: Long) {
        queries.insertCashTransaction(
            id = tx.id, shop_id = tx.shopId, party_id = tx.partyId, deal_id = tx.dealId,
            transaction_type = tx.transactionType.name, amount_paisa = tx.amountPaisa,
            payment_mode = tx.paymentMode.name, transaction_date = tx.transactionDate,
            voice_note_uri = tx.voiceNoteUri, remarks = tx.remarks, created_at = tx.createdAt,
            updated_at = tx.updatedAt, is_deleted = 0L, sync_status = tx.syncStatus.toLong(),
            revision = tx.revision.toLong(), is_void = 0L, void_reason = null,
        )
        queries.insertRevision(
            id = IdGenerator.newId(), shop_id = tx.shopId, entry_id = tx.id, entry_kind = "CASH",
            revision = tx.revision.toLong(), change_kind = "CREATE",
            snapshot_json = json.encodeToString(CashTransaction.serializer(), tx),
            void_reason = null, changed_at = now,
        )
    }

    override suspend fun voidLendEntry(id: String, reason: VoidReason) = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transaction {
            val current = queries.getLendEntryById(id).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Lend $id does not exist")
            if (current.isVoid) return@transaction
            val voided = current.copy(
                isVoid = true, voidReason = reason.name, revision = current.revision + 1,
                updatedAt = now, syncStatus = 0,
            )
            insertOrReplace(voided)
            recordLendRevision(voided, "VOID", now)

            // Cascade void all settlements for this lend
            val settlements = queries.getAllLendSettlementsByLend(id).executeAsList()
            settlements.filter { it.is_void == 0L }.forEach { sEntity ->
                val s = sEntity.toDomain()
                val voidedSettlement = s.copy(
                    isVoid = true,
                    voidReason = reason.name,
                    revision = s.revision + 1,
                    updatedAt = now,
                    syncStatus = 0
                )
                insertSettlement(voidedSettlement)
                queries.insertRevision(
                    id = IdGenerator.newId(),
                    shop_id = voidedSettlement.shopId,
                    entry_id = voidedSettlement.id,
                    entry_kind = "LEND_SETTLEMENT",
                    revision = voidedSettlement.revision,
                    change_kind = "VOID",
                    snapshot_json = json.encodeToString(LendSettlement.serializer(), voidedSettlement),
                    void_reason = voidedSettlement.voidReason,
                    changed_at = now,
                )
            }

            // Cascade void linked cash transactions
            val lendTagMatches = queries.getCashTransactionsByTag("[LEND:$id]").executeAsList()
            val settleTagMatches = queries.getCashTransactionsByTag(":$id]").executeAsList()
            val allLinked = (lendTagMatches + settleTagMatches).distinctBy { it.id }
            allLinked.filter { it.is_void == 0L }.forEach { txEntity ->
                queries.insertCashTransaction(
                    id = txEntity.id,
                    shop_id = txEntity.shop_id,
                    party_id = txEntity.party_id,
                    deal_id = txEntity.deal_id,
                    transaction_type = txEntity.transaction_type,
                    amount_paisa = txEntity.amount_paisa,
                    payment_mode = txEntity.payment_mode,
                    transaction_date = txEntity.transaction_date,
                    voice_note_uri = txEntity.voice_note_uri,
                    remarks = txEntity.remarks,
                    created_at = txEntity.created_at,
                    updated_at = now,
                    is_deleted = txEntity.is_deleted,
                    sync_status = 0L,
                    revision = txEntity.revision + 1L,
                    is_void = 1L,
                    void_reason = reason.name
                )
                queries.insertRevision(
                    id = IdGenerator.newId(),
                    shop_id = txEntity.shop_id,
                    entry_id = txEntity.id,
                    entry_kind = "CASH",
                    revision = txEntity.revision + 1L,
                    change_kind = "VOID",
                    snapshot_json = "",
                    void_reason = reason.name,
                    changed_at = now
                )
            }
        }
    }

    private fun recordLendRevision(entry: LendEntry, changeKind: String, at: Long) {
        queries.insertRevision(
            id = IdGenerator.newId(),
            shop_id = entry.shopId,
            entry_id = entry.id,
            entry_kind = "LEND",
            revision = entry.revision,
            change_kind = changeKind,
            snapshot_json = json.encodeToString(LendEntry.serializer(), entry),
            void_reason = entry.voidReason,
            changed_at = at,
        )
    }

    private fun insertOrReplace(entry: LendEntry) {
        queries.insertLendEntry(
            id = entry.id,
            shop_id = entry.shopId,
            party_id = entry.partyId,
            lend_type = entry.lendType.name,
            entry_date = entry.entryDate,
            amount_paisa = entry.amountPaisa,
            commodity_id = entry.commodityId,
            bags_count = entry.bagsCount,
            weight_grams = entry.weightGrams,
            expected_return_date = entry.expectedReturnDate,
            notes = entry.notes,
            status = entry.status.name,
            created_at = entry.createdAt,
            updated_at = entry.updatedAt,
            is_deleted = 0L,
            sync_status = entry.syncStatus,
            revision = entry.revision,
            is_void = if (entry.isVoid) 1L else 0L,
            void_reason = entry.voidReason,
        )
    }

    private fun insertSettlement(s: LendSettlement) {
        queries.insertLendSettlement(
            id = s.id,
            shop_id = s.shopId,
            lend_id = s.lendId,
            settled_at = s.settledAt,
            amount_paisa = s.amountPaisa,
            weight_grams = s.weightGrams,
            bags_count = s.bagsCount,
            created_at = s.createdAt,
            updated_at = s.updatedAt,
            sync_status = s.syncStatus,
            revision = s.revision,
            is_void = if (s.isVoid) 1L else 0L,
            void_reason = s.voidReason,
        )
    }

    private fun LendEntryEntity.toDomain(): LendEntry = LendEntry(
        id = id,
        shopId = shop_id,
        partyId = party_id,
        lendType = LendType.valueOf(lend_type),
        entryDate = entry_date,
        amountPaisa = amount_paisa,
        commodityId = commodity_id,
        bagsCount = bags_count,
        weightGrams = weight_grams,
        expectedReturnDate = expected_return_date,
        notes = notes,
        status = LendStatus.valueOf(status),
        createdAt = created_at,
        updatedAt = updated_at,
        revision = revision,
        isVoid = is_void == 1L,
        voidReason = void_reason,
        syncStatus = sync_status,
    )

    private fun LendSettlementEntity.toDomain(): LendSettlement = LendSettlement(
        id = id,
        shopId = shop_id,
        lendId = lend_id,
        settledAt = settled_at,
        amountPaisa = amount_paisa,
        weightGrams = weight_grams,
        bagsCount = bags_count,
        createdAt = created_at,
        updatedAt = updated_at,
        revision = revision,
        isVoid = is_void == 1L,
        voidReason = void_reason,
        syncStatus = sync_status,
    )
}
