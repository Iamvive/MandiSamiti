package com.appwork.mandisamiti.domain.repository

import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.VoidReason
import kotlinx.coroutines.flow.Flow

interface CashTransactionRepository {
    fun getTransactionsByPartyStream(partyId: String): Flow<List<CashTransaction>>
    fun getTransactionsByShopStream(shopId: String): Flow<List<CashTransaction>>
    suspend fun recordTransaction(transaction: CashTransaction)
    /**
     * Edits a plain cash row in place and records an EDIT revision.
     * Rows linked to a lend, settlement or expense (remarks tag) are rejected: edit their owner instead.
     */
    suspend fun editTransaction(
        transactionId: String,
        amountPaisa: Long,
        paymentMode: com.appwork.mandisamiti.domain.model.PaymentMode,
        transactionDate: Long,
        remarks: String?
    )
    suspend fun voidTransaction(transactionId: String, reason: VoidReason)
}
