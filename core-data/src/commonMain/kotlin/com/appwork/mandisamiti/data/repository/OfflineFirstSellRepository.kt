package com.appwork.mandisamiti.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.SellEntryEntity
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.domain.repository.SellRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

class OfflineFirstSellRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: Clock = Clock.System,
) : SellRepository {

    private val queries = database.appDatabaseQueries
    private val json = Json { encodeDefaults = true }

    override fun getSellEntriesStream(shopId: String): Flow<List<SellEntry>> =
        queries.getSellEntriesByShop(shopId).asFlow().mapToList(ioDispatcher).map { list -> list.map { it.toDomain() } }

    override suspend fun getSellEntryById(id: String): SellEntry? = withContext(ioDispatcher) {
        queries.getSellEntryById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun saveSellEntry(entry: SellEntry) = withContext(ioDispatcher) {
        database.transaction {
            val created = entry.copy(revision = 1, syncStatus = 0)
            insertOrReplace(created)
            recordRevision(created, "CREATE", created.updatedAt)
        }
    }

    override suspend fun editSellEntry(entry: SellEntry) = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transaction {
            val current = queries.getSellEntryById(entry.id).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Sell entry ${entry.id} does not exist")
            require(!current.isVoid) { "Sell entry ${entry.id} is void and cannot be edited" }
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

    override suspend fun voidSellEntry(id: String, reason: VoidReason) = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transaction {
            val current = queries.getSellEntryById(id).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Sell entry $id does not exist")
            if (current.isVoid) return@transaction
            val voided = current.copy(
                isVoid = true, voidReason = reason.name, revision = current.revision + 1,
                updatedAt = now, syncStatus = 0,
            )
            insertOrReplace(voided)
            recordRevision(voided, "VOID", now)
        }
    }

    private fun recordRevision(entry: SellEntry, changeKind: String, at: Long) {
        queries.insertRevision(
            id = IdGenerator.newId(),
            shop_id = entry.shopId,
            entry_id = entry.id,
            entry_kind = "SELL",
            revision = entry.revision,
            change_kind = changeKind,
            snapshot_json = json.encodeToString(SellEntry.serializer(), entry),
            void_reason = entry.voidReason,
            changed_at = at,
        )
    }

    private fun insertOrReplace(entry: SellEntry) {
        queries.insertSellEntry(
            id = entry.id,
            shop_id = entry.shopId,
            buyer_id = entry.buyerId,
            commodity_id = entry.commodityId,
            entry_date = entry.entryDate,
            bill_mode = entry.billMode.name,
            party_kind = entry.partyKind.name,
            gstin = entry.gstin,
            bags_count = entry.bagsCount,
            weight_per_bag_grams = entry.weightPerBagGrams,
            total_weight_grams = entry.totalWeightGrams,
            rate_paisa_per_quintal = entry.ratePaisaPerQuintal,
            arat_bps = entry.aratBps,
            tulak_paisa = entry.tulakPaisa,
            gross_paisa = entry.grossPaisa,
            arat_paisa = entry.aratPaisa,
            mandi_shulk_paisa = entry.mandiShulkPaisa,
            total_receivable_paisa = entry.totalReceivablePaisa,
            received_paisa = entry.receivedPaisa,
            balance_due_paisa = entry.balanceDuePaisa,
            vehicle_number = entry.vehicleNumber,
            invoice_number = entry.invoiceNumber,
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

    private fun SellEntryEntity.toDomain(): SellEntry = SellEntry(
        id = id,
        shopId = shop_id,
        buyerId = buyer_id,
        commodityId = commodity_id,
        entryDate = entry_date,
        billMode = BillMode.valueOf(bill_mode),
        partyKind = SellPartyKind.valueOf(party_kind),
        gstin = gstin,
        bagsCount = bags_count,
        weightPerBagGrams = weight_per_bag_grams,
        totalWeightGrams = total_weight_grams,
        ratePaisaPerQuintal = rate_paisa_per_quintal,
        aratBps = arat_bps,
        tulakPaisa = tulak_paisa,
        grossPaisa = gross_paisa,
        aratPaisa = arat_paisa,
        mandiShulkPaisa = mandi_shulk_paisa,
        totalReceivablePaisa = total_receivable_paisa,
        receivedPaisa = received_paisa,
        balanceDuePaisa = balance_due_paisa,
        vehicleNumber = vehicle_number,
        invoiceNumber = invoice_number,
        remarks = remarks,
        createdAt = created_at,
        updatedAt = updated_at,
        revision = revision,
        isVoid = is_void == 1L,
        voidReason = void_reason,
        syncStatus = sync_status,
    )
}
