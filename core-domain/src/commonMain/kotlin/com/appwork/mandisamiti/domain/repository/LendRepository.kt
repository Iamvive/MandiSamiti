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
     * and writes nothing. [cashEntries] are written in the same transaction.
     */
    suspend fun settle(settlement: LendSettlement, cashEntries: List<CashTransaction> = emptyList())
    /**
     * Edits the amount and note of a lend and its linked [LEND:id] cash row in one transaction.
     * The amount can only change on a MONEY lend with no active repayments; otherwise it must stay the same.
     */
    suspend fun editLendEntry(id: String, amountPaisa: Long, notes: String?)
    suspend fun voidLendEntry(id: String, reason: VoidReason)
}
