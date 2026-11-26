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
    val accountNumber: String? = null,
    val accountHolder: String? = null,
    val ifsc: String? = null,
    val checkNumber: String? = null,
    val utrNumber: String? = null,
    val upiId: String? = null,
    val userRemarks: String? = null
) {
    /**
     * Human-readable single-line summary of payment details for display in UI cards / registers
     */
    fun displaySummary(isEnglish: Boolean = false): String? {
        val details = mutableListOf<String>()
        if (!checkNumber.isNullOrBlank()) {
            details.add(if (isEnglish) "Chq #$checkNumber" else "चेक #$checkNumber")
        }
        if (!utrNumber.isNullOrBlank()) {
            details.add(if (isEnglish) "Ref/UTR: $utrNumber" else "संदर्भ/UTR: $utrNumber")
        }
        if (!upiId.isNullOrBlank()) {
            details.add("UPI: $upiId")
        }
        if (!bankName.isNullOrBlank()) {
            details.add(bankName)
        }
        if (!accountNumber.isNullOrBlank()) {
            val accStr = if (isEnglish) "A/c: $accountNumber" else "खाता: $accountNumber"
            val holderStr = if (!accountHolder.isNullOrBlank()) " ($accountHolder)" else ""
            details.add("$accStr$holderStr")
        } else if (!accountHolder.isNullOrBlank()) {
            details.add("($accountHolder)")
        }
        if (!ifsc.isNullOrBlank()) {
            details.add("IFSC: $ifsc")
        }
        if (!userRemarks.isNullOrBlank()) {
            details.add(userRemarks)
        }
        return if (details.isEmpty()) null else details.joinToString(" · ")
    }
}

object PaymentMetadata {
    private val BANK_TAG = Regex("""\[BANK:([^\]]+)\]""")
    private val ACC_TAG = Regex("""\[ACC:([^\]]+)\]""")
    private val HOLDER_TAG = Regex("""\[HOLDER:([^\]]+)\]""")
    private val IFSC_TAG = Regex("""\[IFSC:([^\]]+)\]""")
    private val CHK_TAG = Regex("""\[CHK:([^\]]+)\]""")
    private val UTR_TAG = Regex("""\[UTR:([^\]]+)\]""")
    private val UPI_TAG = Regex("""\[UPI:([^\]]+)\]""")
    private val CONTRA_TAG = Regex("""\[CONTRA:[^\]]+\]""")

    fun format(
        bankName: String? = null,
        accountNumber: String? = null,
        accountHolder: String? = null,
        ifsc: String? = null,
        checkNumber: String? = null,
        utrNumber: String? = null,
        upiId: String? = null,
        userRemarks: String? = null
    ): String? {
        val parts = buildString {
            if (!bankName.isNullOrBlank()) append("[BANK:${bankName.trim()}]")
            if (!accountNumber.isNullOrBlank()) append("[ACC:${accountNumber.trim()}]")
            if (!accountHolder.isNullOrBlank()) append("[HOLDER:${accountHolder.trim()}]")
            if (!ifsc.isNullOrBlank()) append("[IFSC:${ifsc.trim()}]")
            if (!checkNumber.isNullOrBlank()) append("[CHK:${checkNumber.trim()}]")
            if (!utrNumber.isNullOrBlank()) append("[UTR:${utrNumber.trim()}]")
            if (!upiId.isNullOrBlank()) append("[UPI:${upiId.trim()}]")
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
        val acc = ACC_TAG.find(remarks)?.groupValues?.get(1)
        val holder = HOLDER_TAG.find(remarks)?.groupValues?.get(1)
        val ifsc = IFSC_TAG.find(remarks)?.groupValues?.get(1)
        val chk = CHK_TAG.find(remarks)?.groupValues?.get(1)
        val utr = UTR_TAG.find(remarks)?.groupValues?.get(1)
        val upi = UPI_TAG.find(remarks)?.groupValues?.get(1)
        val clean = remarks
            .replace(BANK_TAG, "")
            .replace(ACC_TAG, "")
            .replace(HOLDER_TAG, "")
            .replace(IFSC_TAG, "")
            .replace(CHK_TAG, "")
            .replace(UTR_TAG, "")
            .replace(UPI_TAG, "")
            .replace(CONTRA_TAG, "")
            .trim()
        return PaymentMeta(
            bankName = bank,
            accountNumber = acc,
            accountHolder = holder,
            ifsc = ifsc,
            checkNumber = chk,
            utrNumber = utr,
            upiId = upi,
            userRemarks = clean.ifBlank { null }
        )
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
