package com.appwork.mandisamiti.domain.model

import kotlinx.serialization.Serializable

enum class TransactionType {
    UDHAR_GIVEN,    // रुपये दिए / पेशगी (Cash Out)
    JAMA_RECEIVED,  // रुपये मिले / अदायगी (Cash In)
    INTEREST_ADDED, // ब्याज जोड़ा
    INTEREST_RECEIVED, // ब्याज मिला (Cash In, income; never changes the party balance)
    DISCOUNT_GIVEN  // छूट / रियायत / समझौता (Waiver)
}

enum class PaymentMode {
    CASH,
    UPI,
    RTGS,
    NEFT,
    CHEQUE,
    ONLINE, // Legacy generic online value; keep readable.
    BANK,
    BOOK_ENTRY // बही प्रविष्टि
}

@Serializable
data class CashTransaction(
    val id: String,
    val shopId: String,
    val partyId: String,
    val dealId: String? = null,
    val transactionType: TransactionType,
    val amountPaisa: Long,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val transactionDate: Long,
    val voiceNoteUri: String? = null,
    val remarks: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val syncStatus: Int = 0,
    val revision: Int = 1,
    val isVoid: Boolean = false,
    val voidReason: VoidReason? = null
)
