package com.appwork.mandisamiti.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.BuyEntryEntity
import com.appwork.mandisamiti.database.CashTransactionEntity
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.math.AllocationResult
import com.appwork.mandisamiti.domain.math.UdhaarAllocator
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.domain.repository.BuyRepository
import com.appwork.mandisamiti.domain.usecase.UdhaarOverpaymentException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

class OfflineFirstBuyRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: Clock = Clock.System,
    private val lendRepo: OfflineFirstLendRepository = OfflineFirstLendRepository(database, ioDispatcher, clock),
) : BuyRepository {

    private val queries = database.appDatabaseQueries
    private val json = Json { encodeDefaults = true }

    override fun getBuyEntriesStream(shopId: String): Flow<List<BuyEntry>> =
        queries.getBuyEntriesByShop(shopId).asFlow().mapToList(ioDispatcher).map { list -> list.map { it.toDomain() } }

    override suspend fun getBuyEntryById(id: String): BuyEntry? = withContext(ioDispatcher) {
        queries.getBuyEntryById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun saveBuyEntry(entry: BuyEntry) = withContext(ioDispatcher) {
        database.transaction {
            val slipNo = queries.getBuyEntryById(entry.id).executeAsOneOrNull()?.slip_no
                ?: entry.slipNo
                ?: queries.nextBuySlipNo(entry.shopId).executeAsOne()
            val created = entry.copy(slipNo = slipNo, revision = 1, syncStatus = 0)
            insertOrReplace(created)
            recordRevision(created, "CREATE", created.updatedAt)

            if (entry.farmerId.isNotBlank()) {
                queries.insertPartyIfAbsent(
                    id = entry.farmerId,
                    shop_id = entry.shopId,
                    name = entry.farmerId,
                    phone = null,
                    village = extractVillage(entry.remarks),
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

    /**
     * One transaction for a new Buy slip: the party (created, renamed or updated), the slip, and each
     * cash row linked to the slip. Anything that throws rolls all of it back. Sync picks the rows up
     * through their pending status. [BuyEntry.paidPaisa] must equal the cash rows, so the slip and Galla agree.
     * When [loanCutPaisa] > 0, allocates across the farmer's GIVEN loans oldest first, writing settlements
     * and BOOK_ENTRY cash rows.
     */
    suspend fun saveBuyCheckout(
        entry: BuyEntry,
        party: Party?,
        payments: List<CashTransaction>,
        loanCutPaisa: Long = 0L,
    ) =
        withContext(ioDispatcher) {
            require(loanCutPaisa >= 0L) { "Loan cut must be non-negative" }
            require(entry.paidPaisa == payments.sumOf { it.amountPaisa } + loanCutPaisa) {
                "Slip paid must equal its cash rows plus the loan cut"
            }
            database.transaction {
                if (party != null) {
                    if (queries.getPartyById(party.id).executeAsOneOrNull() == null) {
                        queries.insertParty(
                            id = party.id, shop_id = party.shopId, name = party.name, phone = party.phone,
                            village = party.village, party_type = party.partyType.name,
                            monthly_interest_rate = party.monthlyInterestRate, photo_uri = party.photoUri,
                            gstin = party.gstin, mandi_license = party.mandiLicense,
                            created_at = party.createdAt, updated_at = party.updatedAt,
                            is_deleted = 0L, sync_status = 0L,
                        )
                    } else {
                        queries.updateParty(
                            name = party.name, phone = party.phone, village = party.village,
                            party_type = party.partyType.name, monthly_interest_rate = party.monthlyInterestRate,
                            photo_uri = party.photoUri, gstin = party.gstin, mandi_license = party.mandiLicense,
                            updated_at = party.updatedAt, is_deleted = 0L, sync_status = 0L, id = party.id,
                        )
                    }
                }
                val slipNo = entry.slipNo ?: queries.nextBuySlipNo(entry.shopId).executeAsOne()
                val created = entry.copy(slipNo = slipNo, revision = 1, syncStatus = 0)
                insertOrReplace(created)
                recordRevision(created, "CREATE", created.updatedAt)

                val now = clock.now().toEpochMilliseconds()
                if (loanCutPaisa > 0L) {
                    val lendRepo = OfflineFirstLendRepository(database, ioDispatcher, clock)
                    val partyId = party?.id?.takeIf { it.isNotBlank() } ?: entry.farmerId
                    val (loans, openLoans) = lendRepo.getOpenMoneyLoansInTransaction(
                        shopId = entry.shopId,
                        partyId = partyId,
                        direction = LendDirection.GIVEN,
                        nowMs = now
                    )
                    val allocation = UdhaarAllocator.allocate(openLoans, loanCutPaisa)
                    if (allocation is AllocationResult.Over) {
                        throw UdhaarOverpaymentException(allocation.totalDuePaisa)
                    }
                    allocation as AllocationResult.Ok
                    for (part in allocation.parts) {
                        val loan = loans.first { it.id == part.lendId }
                        val settlementId = IdGenerator.newId()
                        val settlement = LendSettlement(
                            id = settlementId,
                            shopId = loan.shopId,
                            lendId = loan.id,
                            settledAt = now,
                            amountPaisa = part.principalPaisa + part.interestPaisa,
                            createdAt = now,
                            updatedAt = now,
                            principalPaisa = part.principalPaisa,
                            interestPaisa = part.interestPaisa,
                            calculatedInterestPaisa = part.interestPaisa,
                        )
                        val cashRows = buildList {
                            if (part.principalPaisa > 0L) {
                                add(
                                    CashTransaction(
                                        id = IdGenerator.newId(),
                                        shopId = loan.shopId,
                                        partyId = loan.partyId,
                                        transactionType = TransactionType.JAMA_RECEIVED,
                                        amountPaisa = part.principalPaisa,
                                        paymentMode = PaymentMode.BOOK_ENTRY,
                                        transactionDate = now,
                                        remarks = "पर्ची #$slipNo से कटौती [SETTLE:$settlementId:${loan.id}] [BUYCUT:${entry.id}]",
                                        createdAt = now,
                                        updatedAt = now,
                                        sourceEntryId = null,
                                        lendId = loan.id,
                                    )
                                )
                            }
                            if (part.interestPaisa > 0L) {
                                add(
                                    CashTransaction(
                                        id = IdGenerator.newId(),
                                        shopId = loan.shopId,
                                        partyId = loan.partyId,
                                        transactionType = TransactionType.INTEREST_RECEIVED,
                                        amountPaisa = part.interestPaisa,
                                        paymentMode = PaymentMode.BOOK_ENTRY,
                                        transactionDate = now,
                                        remarks = "पर्ची #$slipNo से कटौती [SETTLE:$settlementId:${loan.id}] [BUYCUT:${entry.id}]",
                                        createdAt = now,
                                        updatedAt = now,
                                        sourceEntryId = null,
                                        lendId = loan.id,
                                    )
                                )
                            }
                        }
                        lendRepo.settleInTransaction(settlement, cashRows, now)
                    }
                }

                payments.forEach { p ->
                    require(p.amountPaisa > 0) { "Payment must be positive" }
                    val row = p.copy(revision = 1, syncStatus = 0, sourceEntryType = "BUY", sourceEntryId = entry.id)
                    queries.insertCashTransaction(
                        id = row.id, shop_id = row.shopId, party_id = row.partyId, deal_id = row.dealId,
                        transaction_type = row.transactionType.name, amount_paisa = row.amountPaisa,
                        payment_mode = row.paymentMode.name, transaction_date = row.transactionDate,
                        voice_note_uri = row.voiceNoteUri, remarks = row.remarks, created_at = row.createdAt,
                        updated_at = row.updatedAt, is_deleted = 0L, sync_status = 0L, revision = 1L,
                        is_void = 0L, void_reason = null,
                        source_entry_type = row.sourceEntryType, source_entry_id = row.sourceEntryId, lend_id = row.lendId,
                    )
                    queries.insertRevision(
                        id = IdGenerator.newId(), shop_id = row.shopId, entry_id = row.id, entry_kind = "CASH",
                        revision = 1L, change_kind = "CREATE",
                        snapshot_json = json.encodeToString(CashTransaction.serializer(), row),
                        void_reason = null, changed_at = row.updatedAt,
                    )
                }
            }
        }

    private fun extractVillage(remarks: String?): String? {
        if (remarks.isNullOrBlank()) return null
        val match = Regex("""(?:गाँव|Village):\s*([^|]+)""").find(remarks)
        return match?.groupValues?.get(1)?.trim()?.ifBlank { null }
    }

    override suspend fun editBuyEntry(entry: BuyEntry) = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transaction {
            val current = queries.getBuyEntryById(entry.id).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Buy entry ${entry.id} does not exist")
            require(!current.isVoid) { "Buy entry ${entry.id} is void and cannot be edited" }
            val edited = entry.copy(
                slipNo = current.slipNo,
                entryDate = current.entryDate,
                createdAt = current.createdAt,
                updatedAt = now,
                revision = current.revision + 1,
                isVoid = false,
                voidReason = null,
                syncStatus = 0,
            )
            insertOrReplace(edited)
            recordRevision(edited, "EDIT", now)
        }
    }

    override suspend fun voidBuyEntry(id: String, reason: VoidReason) = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transaction {
            val current = queries.getBuyEntryById(id).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Buy entry $id does not exist")
            if (current.isVoid) return@transaction
            val voided = current.copy(
                isVoid = true, voidReason = reason.name, revision = current.revision + 1,
                updatedAt = now, syncStatus = 0,
            )
            insertOrReplace(voided)
            recordRevision(voided, "VOID", now)

            val cutCashRows = findCutCashTransactionsForBuy(current)
            cutCashRows.forEach { txEntity ->
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
                    void_reason = reason.name,
                    source_entry_type = txEntity.source_entry_type,
                    source_entry_id = txEntity.source_entry_id,
                    lend_id = txEntity.lend_id
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

            val settlementIds = cutCashRows.mapNotNull { row ->
                row.remarks?.let { rem ->
                    Regex("""\[SETTLE:([^:]+):""").find(rem)?.groupValues?.get(1)
                }
            }.distinct()

            settlementIds.forEach { settleId ->
                lendRepo.voidSettlementInTransaction(settleId, reason, now)
            }

            val affectedLendIds = (cutCashRows.mapNotNull { it.lend_id } +
                settlementIds.mapNotNull { queries.getLendSettlementById(it).executeAsOneOrNull()?.lend_id }).distinct()

            affectedLendIds.forEach { lendId ->
                lendRepo.recomputeLendStatusInTransaction(lendId, now)
            }
        }
    }

    fun findCutCashTransactionsForBuy(entry: BuyEntry): List<CashTransactionEntity> {
        val tagged = queries.getCashTransactionsByTag("[BUYCUT:${entry.id}]").executeAsList()
        val nonVoidTagged = tagged.filter { it.is_void == 0L }
        if (nonVoidTagged.isNotEmpty()) return nonVoidTagged

        val slipNo = entry.slipNo ?: return emptyList()
        return queries.getCashTransactionsByParty(entry.farmerId).executeAsList().filter {
            it.payment_mode == PaymentMode.BOOK_ENTRY.name &&
            it.remarks?.contains("पर्ची #$slipNo से कटौती") == true &&
            it.is_void == 0L
        }
    }

    private fun recordRevision(entry: BuyEntry, changeKind: String, at: Long) {
        queries.insertRevision(
            id = IdGenerator.newId(),
            shop_id = entry.shopId,
            entry_id = entry.id,
            entry_kind = "BUY",
            revision = entry.revision,
            change_kind = changeKind,
            snapshot_json = json.encodeToString(BuyEntry.serializer(), entry),
            void_reason = entry.voidReason,
            changed_at = at,
        )
    }

    private fun insertOrReplace(entry: BuyEntry) {
        queries.insertBuyEntry(
            id = entry.id,
            shop_id = entry.shopId,
            farmer_id = entry.farmerId,
            commodity_id = entry.commodityId,
            entry_date = entry.entryDate,
            bags_count = entry.bagsCount,
            weight_per_bag_grams = entry.weightPerBagGrams,
            extra_weight_grams = entry.extraWeightGrams,
            total_weight_grams = entry.totalWeightGrams,
            rate_paisa_per_quintal = entry.ratePaisaPerQuintal,
            commission_bps = entry.commissionBps,
            labour_per_bag_paisa = entry.labourPerBagPaisa,
            advance_paid_paisa = entry.advancePaidPaisa,
            gross_paisa = entry.grossPaisa,
            commission_paisa = entry.commissionPaisa,
            labour_paisa = entry.labourPaisa,
            net_payable_paisa = entry.netPayablePaisa,
            paid_paisa = entry.paidPaisa,
            vehicle_number = entry.vehicleNumber,
            slip_number = entry.slipNumber,
            slip_no = entry.slipNo,
            remarks = entry.remarks,
            created_at = entry.createdAt,
            updated_at = entry.updatedAt,
            is_deleted = 0L,
            sync_status = entry.syncStatus,
            revision = entry.revision,
            is_void = if (entry.isVoid) 1L else 0L,
            void_reason = entry.voidReason,
            is_khaate = if (entry.isKhaate) 1L else 0L,
        )
    }

    private fun BuyEntryEntity.toDomain(): BuyEntry = BuyEntry(
        id = id,
        shopId = shop_id,
        farmerId = farmer_id,
        commodityId = commodity_id,
        entryDate = entry_date,
        bagsCount = bags_count,
        weightPerBagGrams = weight_per_bag_grams,
        extraWeightGrams = extra_weight_grams,
        totalWeightGrams = total_weight_grams,
        ratePaisaPerQuintal = rate_paisa_per_quintal,
        commissionBps = commission_bps,
        labourPerBagPaisa = labour_per_bag_paisa,
        advancePaidPaisa = advance_paid_paisa,
        grossPaisa = gross_paisa,
        commissionPaisa = commission_paisa,
        labourPaisa = labour_paisa,
        netPayablePaisa = net_payable_paisa,
        paidPaisa = paid_paisa,
        vehicleNumber = vehicle_number,
        slipNumber = slip_number,
        slipNo = slip_no,
        remarks = remarks,
        createdAt = created_at,
        updatedAt = updated_at,
        revision = revision,
        isVoid = is_void == 1L,
        isKhaate = is_khaate == 1L,
        voidReason = void_reason,
        syncStatus = sync_status,
    )
}
