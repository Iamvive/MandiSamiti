package com.appwork.mandisamiti.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BuyEntry(
    val id: String,
    val shopId: String,
    val farmerId: String,
    val commodityId: String,
    val entryDate: Long,
    val bagsCount: Long,
    val weightPerBagGrams: Long,
    val extraWeightGrams: Long,
    val totalWeightGrams: Long,
    val ratePaisaPerQuintal: Long,
    val commissionBps: Long,
    val labourPerBagPaisa: Long,
    val advancePaidPaisa: Long,
    val grossPaisa: Long,
    val commissionPaisa: Long,
    val labourPaisa: Long,
    val netPayablePaisa: Long,
    val paidPaisa: Long = 0L,
    val vehicleNumber: String? = null,
    val slipNumber: String? = null,
    val slipNo: Long? = null,
    val remarks: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val isVoid: Boolean = false,
    val voidReason: String? = null,
    val syncStatus: Long = 0L,
) {
    val paymentStatus: PaymentStatus
        get() = when {
            paidPaisa <= 0L -> PaymentStatus.PENDING
            paidPaisa >= netPayablePaisa -> PaymentStatus.COMPLETED
            else -> PaymentStatus.PARTIAL
        }
}

enum class PaymentStatus { PENDING, PARTIAL, COMPLETED }
