package com.appwork.mandisamiti.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.BuyEntryEntity
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.model.BuyEntry
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
            val created = entry.copy(revision = 1, syncStatus = 0)
            insertOrReplace(created)
            recordRevision(created, "CREATE", created.updatedAt)
        }
    }

    override suspend fun editBuyEntry(entry: BuyEntry) = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transaction {
            val current = queries.getBuyEntryById(entry.id).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Buy entry ${entry.id} does not exist")
            require(!current.isVoid) { "Buy entry ${entry.id} is void and cannot be edited" }
            val edited = entry.copy(
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
        remarks = remarks,
        createdAt = created_at,
        updatedAt = updated_at,
        revision = revision,
        isVoid = is_void == 1L,
        voidReason = void_reason,
        syncStatus = sync_status,
    )
}
