package com.appwork.mandisamiti.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.BuyEntryEntity
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.domain.repository.BuyRepository
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
     */
    suspend fun saveBuyCheckout(entry: BuyEntry, party: Party?, payments: List<CashTransaction>) =
        withContext(ioDispatcher) {
            require(entry.paidPaisa == payments.sumOf { it.amountPaisa }) { "Slip paid must equal its cash rows" }
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
                        source_entry_type = row.sourceEntryType, source_entry_id = row.sourceEntryId,
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
        voidReason = void_reason,
        syncStatus = sync_status,
    )
}
