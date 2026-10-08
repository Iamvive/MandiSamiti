package com.appwork.mandisamiti.domain.repository

import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.VoidReason
import kotlinx.coroutines.flow.Flow

interface CashTransactionRepository {
    fun getTransactionsByPartyStream(partyId: String): Flow<List<CashTransaction>>
    fun getTransactionsByShopStream(shopId: String): Flow<List<CashTransaction>>
    suspend fun recordTransaction(transaction: CashTransaction)
    suspend fun voidTransaction(transactionId: String, reason: VoidReason)
}
