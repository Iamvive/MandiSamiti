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

data class PaymentMeta(
    val bankName: String? = null,
    val checkNumber: String? = null,
    val userRemarks: String? = null
)

object PaymentMetadata {
    private val BANK_TAG = Regex("""\[BANK:([^\]]+)]""")
    private val CHK_TAG = Regex("""\[CHK:([^\]]+)]""")
    private val CONTRA_TAG = Regex("""\[CONTRA:[^\]]+]""")

    fun format(bankName: String?, checkNumber: String?, userRemarks: String?): String? {
        val parts = buildString {
            if (!bankName.isNullOrBlank()) append("[BANK:${bankName.trim()}]")
            if (!checkNumber.isNullOrBlank()) append("[CHK:${checkNumber.trim()}]")
            val clean = userRemarks?.trim()
            if (!clean.isNullOrBlank()) {
                if (isNotEmpty()) append(' ')
                append(clean)
            }
        }
        return parts.ifBlank { null }
    }

    fun parse(remarks: String?): PaymentMeta {
        if (remarks.isNullOrBlank()) return PaymentMeta()
        val bank = BANK_TAG.find(remarks)?.groupValues?.get(1)
        val chk = CHK_TAG.find(remarks)?.groupValues?.get(1)
        val clean = remarks
            .replace(BANK_TAG, "")
            .replace(CHK_TAG, "")
            .replace(CONTRA_TAG, "")
            .trim()
        return PaymentMeta(bank, chk, clean.ifBlank { null })
    }
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
    val voidReason: VoidReason? = null,
    /** 'BUY' or 'SELL' plus the slip id when this payment belongs to a slip; null for every other row. */
    val sourceEntryType: String? = null,
    val sourceEntryId: String? = null,
    val lendId: String? = null,
)
