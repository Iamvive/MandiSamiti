package com.appwork.mandisamiti.domain.model

import kotlinx.serialization.Serializable

enum class DealStatus {
    PENDING_SETTLEMENT, // कच्चा पर्चा (Weighed, awaiting auction rate & buyer)
    SETTLED,            // पक्का पर्चा (Auction rate & buyer finalized)
    CANCELLED           // रद्द
}

@Serializable
data class Deal(
    val id: String,
    val shopId: String,
    val farmerId: String,
    val buyerId: String? = null,
    val commodityId: String,
    val dealStatus: DealStatus = DealStatus.PENDING_SETTLEMENT,
    val dealDate: Long,

    // Weight parameters (Stored in Grams)
    val bagsCount: Int,
    val grossWeightGrams: Long,
    val cutWeightGrams: Long = 0L,
    val netWeightGrams: Long,

    // Pricing & Deductions (Stored in Paisa)
    val ratePaisaPerUnit: Long? = null, // ₹ / Quintal in paise
    val grossAmountPaisa: Long = 0L,
    val farmerCommissionPaisa: Long = 0L,
    val buyerCommissionPaisa: Long = 0L,
    val labourChargePaisa: Long = 0L,
    val weighingChargePaisa: Long = 0L,
    val otherDeductionsPaisa: Long = 0L,

    // Settlements
    val netFarmerPayablePaisa: Long = 0L,
    val netBuyerReceivablePaisa: Long = 0L,

    // Attachments
    val receiptPhotoUri: String? = null,
    val voiceNoteUri: String? = null,
    val remarks: String? = null,

    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val syncStatus: Int = 0,
    val farmerCommissionBps: Long = 0L, // commission % in basis points (150 = 1.5%)
    val revision: Int = 1,
    val isVoid: Boolean = false,
    val voidReason: VoidReason? = null
)
