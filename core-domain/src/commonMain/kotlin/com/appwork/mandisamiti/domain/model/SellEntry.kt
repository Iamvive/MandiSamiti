package com.appwork.mandisamiti.domain.model

import kotlinx.serialization.Serializable

enum class BillMode { NORMAL, BILL_OF_SUPPLY }
enum class SellPartyKind { MERCHANT, CUSTOMER }

@Serializable
data class SellEntry(
    val id: String,
    val shopId: String,
    val buyerId: String,
    val commodityId: String,
    val entryDate: Long,
    val billMode: BillMode,
    val partyKind: SellPartyKind,
    val gstin: String? = null,
    val bagsCount: Long,
    val weightPerBagGrams: Long,
    val totalWeightGrams: Long,
    val ratePaisaPerQuintal: Long,
    val aratBps: Long,
    val tulakPaisa: Long,
    val grossPaisa: Long,
    val aratPaisa: Long,
    val mandiShulkPaisa: Long,
    val totalReceivablePaisa: Long,
    val receivedPaisa: Long,
    val balanceDuePaisa: Long,
    val vehicleNumber: String? = null,
    val invoiceNumber: String? = null,
    val remarks: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val isVoid: Boolean = false,
    val voidReason: String? = null,
    val syncStatus: Long = 0L,
)
