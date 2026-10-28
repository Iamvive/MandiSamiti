package com.appwork.mandisamiti.domain.repository

import com.appwork.mandisamiti.domain.model.*
import kotlinx.coroutines.flow.Flow

interface LendRepository {
    fun getLendEntriesStream(shopId: String): Flow<List<LendEntry>>
    suspend fun getLendEntryById(id: String): LendEntry?
    suspend fun getSettlements(lendId: String): List<LendSettlement>
    suspend fun saveLendEntry(entry: LendEntry)
    /** Records a repayment and moves the lend to PARTIAL or SETTLED in one transaction. */
    suspend fun settle(settlement: LendSettlement)
    suspend fun voidLendEntry(id: String, reason: VoidReason)
}
