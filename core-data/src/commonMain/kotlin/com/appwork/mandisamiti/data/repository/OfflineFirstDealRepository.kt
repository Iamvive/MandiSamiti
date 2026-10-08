package com.appwork.mandisamiti.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.DealEntity
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.model.DealStatus
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.domain.repository.DealRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

class OfflineFirstDealRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: Clock = Clock.System
) : DealRepository {

    private val queries = database.appDatabaseQueries
    private val json = Json { encodeDefaults = true }

    override fun getDealsByShopStream(shopId: String): Flow<List<Deal>> {
        return queries.getDealsByShop(shopId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getDealsByFarmerStream(farmerId: String): Flow<List<Deal>> {
        return queries.getDealsByFarmer(farmerId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getDealsByBuyerStream(buyerId: String): Flow<List<Deal>> {
        return queries.getDealsByBuyer(buyerId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getPendingDealsStream(shopId: String): Flow<List<Deal>> {
        return queries.getPendingDeals(shopId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getDealById(dealId: String): Deal? = withContext(ioDispatcher) {
        queries.getDealById(dealId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun saveDeal(deal: Deal) = withContext(ioDispatcher) {
        database.transaction {
            val created = deal.copy(revision = 1, syncStatus = 0)
            insertOrReplace(created)
            recordRevision(created, "CREATE", created.updatedAt)
        }
    }

    override suspend fun editDeal(deal: Deal) = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transaction {
            val current = queries.getDealById(deal.id).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Deal ${deal.id} does not exist")
            require(!current.isVoid) { "Deal ${deal.id} is void and cannot be edited" }
            val edited = deal.copy(
                dealDate = current.dealDate,
                createdAt = current.createdAt,
                updatedAt = now,
                revision = current.revision + 1,
                isVoid = false,
                voidReason = null,
                syncStatus = 0
            )
            insertOrReplace(edited)
            recordRevision(edited, "EDIT", now)
        }
    }

    override suspend fun voidDeal(dealId: String, reason: VoidReason) = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transaction {
            val current = queries.getDealById(dealId).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Deal $dealId does not exist")
            if (current.isVoid) return@transaction
            val voided = current.copy(
                isVoid = true, voidReason = reason, revision = current.revision + 1,
                updatedAt = now, syncStatus = 0
            )
            insertOrReplace(voided)
            recordRevision(voided, "VOID", now)
        }
    }

    private fun recordRevision(deal: Deal, changeKind: String, at: Long) {
        queries.insertRevision(
            id = IdGenerator.newId(),
            shop_id = deal.shopId,
            entry_id = deal.id,
            entry_kind = "DEAL",
            revision = deal.revision.toLong(),
            change_kind = changeKind,
            snapshot_json = json.encodeToString(Deal.serializer(), deal),
            void_reason = deal.voidReason?.name,
            changed_at = at
        )
    }

    private fun insertOrReplace(deal: Deal) {
        queries.insertDeal(
            id = deal.id,
            shop_id = deal.shopId,
            farmer_id = deal.farmerId,
            buyer_id = deal.buyerId,
            commodity_id = deal.commodityId,
            deal_status = deal.dealStatus.name,
            deal_date = deal.dealDate,
            bags_count = deal.bagsCount.toLong(),
            gross_weight_grams = deal.grossWeightGrams,
            cut_weight_grams = deal.cutWeightGrams,
            net_weight_grams = deal.netWeightGrams,
            rate_paisa_per_unit = deal.ratePaisaPerUnit,
            gross_amount_paisa = deal.grossAmountPaisa,
            farmer_commission_paisa = deal.farmerCommissionPaisa,
            buyer_commission_paisa = deal.buyerCommissionPaisa,
            labour_charge_paisa = deal.labourChargePaisa,
            weighing_charge_paisa = deal.weighingChargePaisa,
            other_deductions_paisa = deal.otherDeductionsPaisa,
            net_farmer_payable_paisa = deal.netFarmerPayablePaisa,
            net_buyer_receivable_paisa = deal.netBuyerReceivablePaisa,
            receipt_photo_uri = deal.receiptPhotoUri,
            voice_note_uri = deal.voiceNoteUri,
            remarks = deal.remarks,
            created_at = deal.createdAt,
            updated_at = deal.updatedAt,
            is_deleted = if (deal.isDeleted) 1L else 0L,
            sync_status = deal.syncStatus.toLong(),
            farmer_commission_bps = deal.farmerCommissionBps,
            revision = deal.revision.toLong(),
            is_void = if (deal.isVoid) 1L else 0L,
            void_reason = deal.voidReason?.name,
        )
    }

    private fun DealEntity.toDomain(): Deal {
        return Deal(
            id = id,
            shopId = shop_id,
            farmerId = farmer_id,
            buyerId = buyer_id,
            commodityId = commodity_id,
            dealStatus = DealStatus.valueOf(deal_status),
            dealDate = deal_date,
            bagsCount = bags_count.toInt(),
            grossWeightGrams = gross_weight_grams,
            cutWeightGrams = cut_weight_grams,
            netWeightGrams = net_weight_grams,
            ratePaisaPerUnit = rate_paisa_per_unit,
            grossAmountPaisa = gross_amount_paisa,
            farmerCommissionPaisa = farmer_commission_paisa,
            buyerCommissionPaisa = buyer_commission_paisa,
            labourChargePaisa = labour_charge_paisa,
            weighingChargePaisa = weighing_charge_paisa,
            otherDeductionsPaisa = other_deductions_paisa,
            netFarmerPayablePaisa = net_farmer_payable_paisa,
            netBuyerReceivablePaisa = net_buyer_receivable_paisa,
            receiptPhotoUri = receipt_photo_uri,
            voiceNoteUri = voice_note_uri,
            remarks = remarks,
            createdAt = created_at,
            updatedAt = updated_at,
            isDeleted = is_deleted == 1L,
            syncStatus = sync_status.toInt(),
            farmerCommissionBps = farmer_commission_bps,
            revision = revision.toInt(),
            isVoid = is_void == 1L,
            voidReason = void_reason?.let { VoidReason.valueOf(it) }
        )
    }
}
