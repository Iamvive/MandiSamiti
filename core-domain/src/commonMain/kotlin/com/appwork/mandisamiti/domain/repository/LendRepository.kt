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
    /**
     * Edits the amount and date of one payment on a money loan and recalculates every payment after it,
     * in one transaction. Linked cash rows (principal and interest) and the loan status follow.
     * Throws [com.appwork.mandisamiti.domain.usecase.LendException.OverRepayment] and writes nothing
     * when a payment would exceed the outstanding principal.
     */
    suspend fun editSettlement(settlementId: String, newAmountPaisa: Long, newSettledAt: Long, monthlyRatePercent: Double): com.appwork.mandisamiti.domain.math.RepaymentEditPlan
    /** One snapshot per revision of a payment, oldest first. The last one is the current state. */
    suspend fun getSettlementHistory(settlementId: String): List<LendSettlement>
}
