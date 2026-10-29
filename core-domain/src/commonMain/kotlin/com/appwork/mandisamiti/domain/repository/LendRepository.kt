package com.appwork.mandisamiti.domain.repository

import com.appwork.mandisamiti.domain.model.*
import kotlinx.coroutines.flow.Flow

interface LendRepository {
    fun getLendEntriesStream(shopId: String): Flow<List<LendEntry>>
    fun getSettlementsStream(shopId: String): Flow<List<LendSettlement>>
    suspend fun getLendEntryById(id: String): LendEntry?
    suspend fun getSettlements(lendId: String): List<LendSettlement>
    suspend fun saveLendEntry(entry: LendEntry)
    /**
     * Records a repayment and moves the lend to PARTIAL or SETTLED in one transaction.
     * Rejects repayments above the outstanding balance with [com.appwork.mandisamiti.domain.usecase.LendException]
     * and writes nothing. [cashEntry], when given, is written in the same transaction.
     */
    suspend fun settle(settlement: LendSettlement, cashEntry: CashTransaction? = null)
    suspend fun voidLendEntry(id: String, reason: VoidReason)
}
