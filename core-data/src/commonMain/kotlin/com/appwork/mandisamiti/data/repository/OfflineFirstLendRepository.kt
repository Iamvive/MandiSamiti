package com.appwork.mandisamiti.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.LendEntryEntity
import com.appwork.mandisamiti.database.LendSettlementEntity
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.math.RepaymentEditPlan
import com.appwork.mandisamiti.domain.math.RepaymentEditPlanner
import com.appwork.mandisamiti.domain.math.effectivePrincipalPaisa
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
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
            val slipNo = queries.getLendEntryById(entry.id).executeAsOneOrNull()?.slip_no
                ?: entry.slipNo
                ?: queries.nextLendSlipNo(entry.shopId).executeAsOne()
            val created = entry.copy(slipNo = slipNo, revision = 1, syncStatus = 0)
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
                    gstin = null,
                    mandi_license = null,
                    created_at = created.createdAt,
                    updated_at = created.updatedAt,
                    is_deleted = 0L,
                    sync_status = 0L
                )
            }
        }
    }

    override suspend fun settle(rawSettlement: LendSettlement, cashEntries: List<CashTransaction>) = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transaction { settleInTransaction(rawSettlement, cashEntries, now) }
    }

    /** The settle body without its own transaction, so a Buy checkout can run it inside its own. */
    internal fun settleInTransaction(rawSettlement: LendSettlement, cashEntries: List<CashTransaction>, now: Long) {
        val settlement = rawSettlement.withSplit()
        run {
            val lend = queries.getLendEntryById(settlement.lendId).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Lend ${settlement.lendId} does not exist")
            require(!lend.isVoid) { "Lend ${settlement.lendId} is void" }

            val live = queries.getLendSettlementsByLend(lend.id).executeAsList().map { it.toDomain() }.filter { !it.isVoid }
            if (lend.lendType == LendType.MONEY) {
                // Only principal reduces the loan; interest is paid on top of it.
                val outstanding = (lend.amountPaisa - live.sumOf { it.principalPaisa }).coerceAtLeast(0L)
                if (settlement.principalPaisa > outstanding) throw LendException.OverRepayment(settlement.principalPaisa, outstanding)
            } else {
                val outstanding = (lend.weightGrams - live.sumOf { it.weightGrams }).coerceAtLeast(0L)
                if (settlement.weightGrams > outstanding) throw LendException.OverRepaymentWeight(settlement.weightGrams, outstanding)
            }

            val created = settlement.copy(revision = 1, syncStatus = 0)
            insertSettlement(created)
            cashEntries.forEach { insertCashEntry(it.copy(revision = 1, syncStatus = 0), now) }
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
            val repaidAmount = settlements.sumOf { it.principalPaisa }
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
            revision = tx.revision.toLong(), is_void = 0L, void_reason = null, source_entry_type = null, source_entry_id = null
        )
        queries.insertRevision(
            id = IdGenerator.newId(), shop_id = tx.shopId, entry_id = tx.id, entry_kind = "CASH",
            revision = tx.revision.toLong(), change_kind = "CREATE",
            snapshot_json = json.encodeToString(CashTransaction.serializer(), tx),
            void_reason = null, changed_at = now,
        )
    }

    override suspend fun editLendEntry(id: String, amountPaisa: Long, notes: String?) = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transaction {
            val current = queries.getLendEntryById(id).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Lend $id does not exist")
            require(!current.isVoid) { "Voided lend cannot be edited" }
            val isMoney = current.lendType == LendType.MONEY
            val newAmount = if (isMoney) amountPaisa else current.amountPaisa
            if (newAmount != current.amountPaisa) {
                require(newAmount > 0) { "Amount must be positive" }
                val hasRepayments = queries.getLendSettlementsByLend(id).executeAsList().any { it.is_void == 0L }
                require(!hasRepayments) { "Lend with repayments cannot change amount" }
            }
            val cleanNotes = notes?.trim()?.ifEmpty { null }
            val edited = current.copy(
                amountPaisa = newAmount, notes = cleanNotes,
                revision = current.revision + 1, updatedAt = now, syncStatus = 0
            )
            insertOrReplace(edited)
            recordLendRevision(edited, "EDIT", now)

            if (isMoney) {
                val rem = if (cleanNotes == null) "उधार ऋण" else "उधार ऋण: $cleanNotes"
                queries.getCashTransactionsByTag("[LEND:$id]").executeAsList()
                    .filter { it.is_void == 0L }
                    .forEach { tx ->
                        queries.insertCashTransaction(
                            id = tx.id, shop_id = tx.shop_id, party_id = tx.party_id, deal_id = tx.deal_id,
                            transaction_type = tx.transaction_type, amount_paisa = newAmount,
                            payment_mode = tx.payment_mode, transaction_date = tx.transaction_date,
                            voice_note_uri = tx.voice_note_uri, remarks = "$rem [LEND:$id]",
                            created_at = tx.created_at, updated_at = now, is_deleted = tx.is_deleted,
                            sync_status = 0L, revision = tx.revision + 1L, is_void = 0L, void_reason = null, source_entry_type = tx.source_entry_type, source_entry_id = tx.source_entry_id
                        )
                        queries.insertRevision(
                            id = IdGenerator.newId(), shop_id = tx.shop_id, entry_id = tx.id, entry_kind = "CASH",
                            revision = tx.revision + 1L, change_kind = "EDIT", snapshot_json = "",
                            void_reason = null, changed_at = now,
                        )
                    }
            }
        }
    }

    override suspend fun voidLendEntry(id: String, reason: VoidReason) = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transaction { voidLendInTransaction(id, reason, now) }
    }

    override suspend fun editSettlement(
        settlementId: String, newAmountPaisa: Long, newSettledAt: Long, monthlyRatePercent: Double,
    ): RepaymentEditPlan = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transactionWithResult {
            val target = queries.getLendSettlementById(settlementId).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Payment $settlementId does not exist")
            require(!target.isVoid) { "Voided payment cannot be edited" }
            val lend = queries.getLendEntryById(target.lendId).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Lend ${target.lendId} does not exist")
            require(!lend.isVoid && lend.lendType == LendType.MONEY) { "Only payments on a live money loan can be edited" }

            val live = queries.getLendSettlementsByLend(lend.id).executeAsList().map { it.toDomain() }
            val plan = RepaymentEditPlanner.plan(lend, live, settlementId, newAmountPaisa, newSettledAt, monthlyRatePercent, now)
            val before = live.associateBy { it.id }

            plan.changedSettlements.forEach { s ->
                insertSettlement(s)
                queries.insertRevision(
                    id = IdGenerator.newId(), shop_id = s.shopId, entry_id = s.id, entry_kind = "LEND_SETTLEMENT",
                    revision = s.revision, change_kind = "EDIT",
                    snapshot_json = json.encodeToString(LendSettlement.serializer(), s),
                    void_reason = null, changed_at = now,
                )
                syncSettlementCashRows(lend, s, dateChanged = before.getValue(s.id).settledAt != s.settledAt, now = now)
            }

            val changedById = plan.changedSettlements.associateBy { it.id }
            val repaid = live.sumOf { (changedById[it.id] ?: it).effectivePrincipalPaisa() }
            val newStatus = if (repaid >= lend.amountPaisa) LendStatus.SETTLED else LendStatus.PARTIAL
            if (newStatus != lend.status) {
                val updatedLend = lend.copy(status = newStatus, revision = lend.revision + 1, updatedAt = now, syncStatus = 0)
                insertOrReplace(updatedLend)
                recordLendRevision(updatedLend, "EDIT", now)
            }
            plan
        }
    }

    override suspend fun getSettlementHistory(settlementId: String): List<LendSettlement> = withContext(ioDispatcher) {
        queries.getRevisionsForEntry(settlementId).executeAsList()
            .filter { it.entry_kind == "LEND_SETTLEMENT" && it.snapshot_json.isNotBlank() }
            .map { json.decodeFromString(LendSettlement.serializer(), it.snapshot_json) }
    }

    /**
     * Makes the tagged cash rows of one payment match its new split: the JAMA_RECEIVED row carries the principal,
     * the INTEREST_RECEIVED row carries the interest. A row that drops to 0 is voided; a row that is needed
     * and missing is created. The general-jama overflow row has no tag and is never touched.
     */
    private fun syncSettlementCashRows(lend: LendEntry, s: LendSettlement, dateChanged: Boolean, now: Long) {
        val tag = "[SETTLE:${s.id}:${s.lendId}]"
        val rows = queries.getCashTransactionsByTag(tag).executeAsList()
        val date = if (dateChanged) s.settledAt else null

        fun adjust(type: TransactionType, newPaisa: Long, remarks: String) {
            val row = rows.firstOrNull { it.transaction_type == type.name }
            if (row == null) {
                if (newPaisa > 0L) {
                    insertCashEntry(
                        CashTransaction(
                            id = IdGenerator.newId(), shopId = s.shopId,
                            partyId = rows.firstOrNull()?.party_id ?: lend.partyId,
                            transactionType = type, amountPaisa = newPaisa,
                            paymentMode = PaymentMode.valueOf(rows.firstOrNull()?.payment_mode ?: PaymentMode.CASH.name),
                            transactionDate = s.settledAt, remarks = "$remarks $tag", createdAt = now, updatedAt = now,
                        ),
                        now,
                    )
                }
                return
            }
            if (row.amount_paisa == newPaisa && date == null) return
            val void = newPaisa == 0L
            queries.insertCashTransaction(
                id = row.id, shop_id = row.shop_id, party_id = row.party_id, deal_id = row.deal_id,
                transaction_type = row.transaction_type, amount_paisa = if (void) row.amount_paisa else newPaisa,
                payment_mode = row.payment_mode, transaction_date = date ?: row.transaction_date,
                voice_note_uri = row.voice_note_uri, remarks = row.remarks, created_at = row.created_at,
                updated_at = now, is_deleted = row.is_deleted, sync_status = 0L, revision = row.revision + 1L,
                is_void = if (void) 1L else 0L, void_reason = if (void) "REPAYMENT_EDITED" else null,
                source_entry_type = row.source_entry_type, source_entry_id = row.source_entry_id,
            )
            queries.insertRevision(
                id = IdGenerator.newId(), shop_id = row.shop_id, entry_id = row.id, entry_kind = "CASH",
                revision = row.revision + 1L, change_kind = if (void) "VOID" else "EDIT", snapshot_json = "",
                void_reason = if (void) "REPAYMENT_EDITED" else null, changed_at = now,
            )
        }
        adjust(TransactionType.JAMA_RECEIVED, s.principalPaisa, "उधार वापसी जमा")
        adjust(TransactionType.INTEREST_RECEIVED, s.interestPaisa, "ब्याज जमा")
    }

    /** Voids the lend, its settlements and every linked cash row. Must run inside the caller's DB transaction. */
    internal fun voidLendInTransaction(id: String, reason: VoidReason, now: Long) {
        val current = queries.getLendEntryById(id).executeAsOneOrNull()?.toDomain()
            ?: throw IllegalArgumentException("Lend $id does not exist")
        if (current.isVoid) return
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
            queries.voidLendSettlement(reason.name, voidedSettlement.revision, now, s.id)
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
                void_reason = reason.name, source_entry_type = null, source_entry_id = null
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
            slip_no = entry.slipNo,
            notes = entry.notes,
            status = entry.status.name,
            created_at = entry.createdAt,
            updated_at = entry.updatedAt,
            is_deleted = 0L,
            sync_status = entry.syncStatus,
            revision = entry.revision,
            is_void = if (entry.isVoid) 1L else 0L,
            void_reason = entry.voidReason,
            monthly_interest_rate = entry.monthlyRatePercent,
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
            principal_paisa = s.principalPaisa,
            interest_paisa = s.interestPaisa,
            calculated_interest_paisa = s.calculatedInterestPaisa,
        )
    }

    /** Legacy callers pass only [LendSettlement.amountPaisa]; that is all principal. */
    private fun LendSettlement.withSplit(): LendSettlement {
        if (principalPaisa == 0L && interestPaisa == 0L) return copy(principalPaisa = amountPaisa)
        require(principalPaisa + interestPaisa == amountPaisa) { "principal + interest must equal the settlement amount" }
        return this
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
        slipNo = slip_no,
        notes = notes,
        status = LendStatus.valueOf(status),
        createdAt = created_at,
        updatedAt = updated_at,
        revision = revision,
        isVoid = is_void == 1L,
        voidReason = void_reason,
        monthlyRatePercent = monthly_interest_rate,
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
        principalPaisa = principal_paisa,
        interestPaisa = interest_paisa,
        calculatedInterestPaisa = calculated_interest_paisa,
    )
}
