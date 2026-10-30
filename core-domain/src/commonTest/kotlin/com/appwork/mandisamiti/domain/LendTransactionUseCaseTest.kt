package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.model.*
import com.appwork.mandisamiti.domain.repository.LendRepository
import com.appwork.mandisamiti.domain.usecase.DepositSplit
import com.appwork.mandisamiti.domain.usecase.LendException
import com.appwork.mandisamiti.domain.usecase.splitDeposit
import com.appwork.mandisamiti.domain.usecase.LendForm
import com.appwork.mandisamiti.domain.usecase.LendTransactionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LendTransactionUseCaseTest {

    private class FakeLendRepository : LendRepository {
        val lends = mutableMapOf<String, LendEntry>()
        val settlements = mutableListOf<LendSettlement>()
        val cash = mutableListOf<CashTransaction>()

        override fun getLendEntriesStream(shopId: String): Flow<List<LendEntry>> = flowOf(lends.values.toList())
        override fun getSettlementsStream(shopId: String): Flow<List<LendSettlement>> = flowOf(settlements)
        override suspend fun getLendEntryById(id: String): LendEntry? = lends[id]
        override suspend fun getSettlements(lendId: String): List<LendSettlement> =
            settlements.filter { it.lendId == lendId && !it.isVoid }

        override suspend fun saveLendEntry(entry: LendEntry) {
            lends[entry.id] = entry
        }

        /** Mirrors the real repo: only principal counts against the loan; 0/0 legacy rows are all principal. */
        private fun LendSettlement.principal() = if (principalPaisa == 0L && interestPaisa == 0L) amountPaisa else principalPaisa

        override suspend fun settle(settlement: LendSettlement, cashEntries: List<CashTransaction>) {
            val live = settlements.filter { it.lendId == settlement.lendId && !it.isVoid }.sumOf { it.principal() }
            val outstanding = ((lends[settlement.lendId]?.amountPaisa ?: 0L) - live).coerceAtLeast(0L)
            if (settlement.principal() > outstanding) throw LendException.OverRepayment(settlement.principal(), outstanding)
            cash.addAll(cashEntries)
            settlements.add(settlement)
            val lend = lends[settlement.lendId] ?: return
            val totalRepaid = settlements.filter { it.lendId == lend.id && !it.isVoid }.sumOf { it.principal() }
            val newStatus = if (totalRepaid >= lend.amountPaisa) LendStatus.SETTLED else LendStatus.PARTIAL
            lends[lend.id] = lend.copy(status = newStatus)
        }

        override suspend fun editLendEntry(id: String, amountPaisa: Long, notes: String?) {}
        override suspend fun voidLendEntry(id: String, reason: VoidReason) {
            lends[id]?.let { lends[id] = it.copy(isVoid = true) }
        }
    }

    @Test
    fun repay_exceedingOutstanding_throwsTypedOverRepaymentError() = runTest {
        val repo = FakeLendRepository()
        var idCounter = 0
        val useCase = LendTransactionUseCase(
            repo = repo,
            newId = { "id-${++idCounter}" },
            nowMillis = { 1000L }
        )

        // Lend ₹5,000 (500,000 paisa)
        val entry = useCase.lend(
            LendForm(
                shopId = "s1",
                partyId = "p1",
                lendType = LendType.MONEY,
                amountPaisa = 500_000L
            )
        )

        // Attempting to repay ₹7,000 (700,000 paisa) against ₹5,000 loan must fail with OverRepayment
        val ex = assertFailsWith<LendException.OverRepayment> {
            useCase.repay(
                lendId = entry.id,
                amountPaisa = 700_000L,
                weightGrams = 0L,
                bags = 0L
            )
        }
        assertEquals(700_000L, ex.requestedPaisa)
        assertEquals(500_000L, ex.outstandingPaisa)
    }

    @Test
    fun splitDeposit_capsLoanPartAtOutstandingAndReturnsRestAsGeneral() {
        assertEquals(DepositSplit(300_000L, 200_000L), splitDeposit(500_000L, 300_000L))
        assertEquals(DepositSplit(300_000L, 0L), splitDeposit(300_000L, 300_000L))
        assertEquals(DepositSplit(100_000L, 0L), splitDeposit(100_000L, 300_000L))
        assertEquals(DepositSplit(0L, 100_000L), splitDeposit(100_000L, 0L))
    }

    @Test
    fun repayWithOverflow_writesSettlementAndGeneralJamaTogether() = runTest {
        val repo = FakeLendRepository()
        var n = 0
        val useCase = LendTransactionUseCase(repo, { "id-${++n}" }, { 1000L })
        val lend = useCase.lend(LendForm(shopId = "s1", partyId = "p1", lendType = LendType.MONEY, amountPaisa = 300_000L))

        val split = useCase.repayWithOverflow(lend.id, 500_000L, "नोट")

        assertEquals(DepositSplit(300_000L, 200_000L), split)
        assertEquals(listOf(300_000L), repo.settlements.map { it.amountPaisa })
        assertEquals(listOf(300_000L, 200_000L), repo.cash.map { it.amountPaisa })
        assertEquals(true, repo.cash[0].remarks!!.contains("[SETTLE:"))
        assertEquals(false, repo.cash[1].remarks!!.contains("[SETTLE:"))
        assertEquals(LendStatus.SETTLED, repo.lends[lend.id]!!.status)
    }

    private suspend fun loanOf(repo: FakeLendRepository, paisa: Long): Pair<LendTransactionUseCase, LendEntry> {
        var n = 0
        val useCase = LendTransactionUseCase(repo, newId = { "id-${++n}" }, nowMillis = { 1000L })
        val entry = useCase.lend(LendForm("s1", "p1", LendType.MONEY, amountPaisa = paisa))
        return useCase to entry
    }

    @Test
    fun repayWithInterest_writesPrincipalRowThenInterestRow() = runTest {
        val repo = FakeLendRepository()
        val (useCase, entry) = loanOf(repo, 10_000_00L)
        val split = useCase.repayWithInterest(entry.id, 1_200_00L, 200_00L, 200_00L)
        assertEquals(DepositSplit(1_000_00L, 0L, 200_00L), split)
        assertEquals(listOf(TransactionType.JAMA_RECEIVED, TransactionType.INTEREST_RECEIVED), repo.cash.map { it.transactionType })
        assertEquals(listOf(1_000_00L, 200_00L), repo.cash.map { it.amountPaisa })
        val s = repo.settlements.single()
        assertEquals(1_000_00L, s.principalPaisa)
        assertEquals(200_00L, s.interestPaisa)
        assertEquals(1_200_00L, s.amountPaisa)
        assertEquals(true, repo.cash.all { it.remarks!!.contains("[SETTLE:${s.id}:${entry.id}]") })
    }

    @Test
    fun repayWithInterest_adjustedInterestKeepsCalculatedValue() = runTest {
        val repo = FakeLendRepository()
        val (useCase, entry) = loanOf(repo, 10_000_00L)
        useCase.repayWithInterest(entry.id, 1_100_00L, 100_00L, 200_00L)
        val s = repo.settlements.single()
        assertEquals(200_00L, s.calculatedInterestPaisa)
        assertEquals(100_00L, s.interestPaisa)
        assertEquals(1_000_00L, s.principalPaisa)
    }

    @Test
    fun repayWithInterest_smallPaymentIsAllInterestAndNoPrincipalRow() = runTest {
        val repo = FakeLendRepository()
        val (useCase, entry) = loanOf(repo, 10_000_00L)
        useCase.repayWithInterest(entry.id, 150_00L, 200_00L, 200_00L)
        assertEquals(listOf(TransactionType.INTEREST_RECEIVED), repo.cash.map { it.transactionType })
        assertEquals(0L, repo.settlements.single().principalPaisa)
        assertEquals(150_00L, repo.settlements.single().interestPaisa)
        assertEquals(LendStatus.PARTIAL, repo.lends[entry.id]!!.status)
    }

    @Test
    fun repayWithInterest_excessOverPrincipalBecomesGeneralJama() = runTest {
        val repo = FakeLendRepository()
        val (useCase, entry) = loanOf(repo, 1_000_00L)
        val split = useCase.repayWithInterest(entry.id, 1_700_00L, 200_00L, 200_00L)
        assertEquals(DepositSplit(1_000_00L, 500_00L, 200_00L), split)
        assertEquals(listOf(1_000_00L, 200_00L, 500_00L), repo.cash.map { it.amountPaisa })
        assertEquals(1_000_00L, repo.settlements.single().principalPaisa)
        assertEquals(LendStatus.SETTLED, repo.lends[entry.id]!!.status)
    }

    @Test
    fun repayWithInterest_nothingToApplyIsRejected() = runTest {
        val repo = FakeLendRepository()
        val (useCase, entry) = loanOf(repo, 1_000_00L)
        useCase.repayWithInterest(entry.id, 1_000_00L, 0L, 0L)
        assertFailsWith<IllegalArgumentException> { useCase.repayWithInterest(entry.id, 50_00L, 0L, 0L) }
    }

    @Test
    fun repayWithOverflow_stillSplitsLoanAndGeneral() = runTest {
        val repo = FakeLendRepository()
        val (useCase, entry) = loanOf(repo, 1_000_00L)
        assertEquals(DepositSplit(1_000_00L, 400_00L), useCase.repayWithOverflow(entry.id, 1_400_00L))
        assertEquals(0L, repo.settlements.single().interestPaisa)
    }
}
