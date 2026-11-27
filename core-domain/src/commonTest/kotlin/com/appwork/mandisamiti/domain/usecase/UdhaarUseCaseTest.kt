package com.appwork.mandisamiti.domain.usecase

import com.appwork.mandisamiti.domain.cash.CashDrawer
import com.appwork.mandisamiti.domain.math.RepaymentEditPlan
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.domain.repository.LendRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class UdhaarUseCaseTest {

    private class FakeLendRepository : LendRepository {
        val loans = mutableListOf<LendEntry>()
        val cashRows = mutableListOf<CashTransaction>()
        val settlements = mutableListOf<LendSettlement>()

        override fun getLendEntriesStream(shopId: String): Flow<List<LendEntry>> = flowOf(loans)
        override fun getSettlementsStream(shopId: String): Flow<List<LendSettlement>> = flowOf(settlements)
        override suspend fun getLendEntryById(id: String): LendEntry? = loans.find { it.id == id }
        override suspend fun getSettlements(lendId: String): List<LendSettlement> = settlements.filter { it.lendId == lendId }
        override suspend fun getLoansForParty(shopId: String, partyId: String, direction: LendDirection): List<LendEntry> =
            loans.filter { it.shopId == shopId && it.partyId == partyId && it.direction == direction }

        override suspend fun settleAll(items: List<Pair<LendSettlement, List<CashTransaction>>>) {
            items.forEach { (s, rows) ->
                settlements += s
                cashRows += rows
            }
        }

        override suspend fun saveLoanWithCash(entry: LendEntry, cash: CashTransaction) {
            loans += entry
            cashRows += cash
        }

        override suspend fun saveLendEntry(entry: LendEntry) {
            loans += entry
        }

        override suspend fun settle(settlement: LendSettlement, cashEntries: List<CashTransaction>) {
            settlements += settlement
            cashRows += cashEntries
        }

        override suspend fun editLendEntry(id: String, amountPaisa: Long, notes: String?) = Unit
        override suspend fun voidLendEntry(id: String, reason: VoidReason) = Unit
        override suspend fun editSettlement(
            settlementId: String,
            newAmountPaisa: Long,
            newSettledAt: Long,
            monthlyRatePercent: Double,
        ): RepaymentEditPlan = error("Unused in test")

        override suspend fun getSettlementHistory(settlementId: String): List<LendSettlement> = emptyList()
    }

    private val ist = TimeZone.of("Asia/Kolkata")
    private val todayNoonIst = Instant.parse("2026-11-27T06:30:00Z").toEpochMilliseconds() // 12:00 IST on 27 Nov

    private fun newUseCase(repo: FakeLendRepository, nowMs: Long = todayNoonIst): UdhaarUseCase {
        var seq = 0
        return UdhaarUseCase(
            repo = repo,
            newId = { "id-${++seq}" },
            nowMillis = { nowMs },
            timeZone = ist,
        )
    }

    @Test
    fun todayLoanDefaultsToCashPaymentMode() = runTest {
        val repo = FakeLendRepository()
        val useCase = newUseCase(repo)

        useCase.createLoan(
            UdhaarLoanForm(
                shopId = "shop-1",
                partyId = "party-1",
                direction = LendDirection.GIVEN,
                amountPaisa = 500_000L,
                date = todayNoonIst,
            )
        )

        val cash = repo.cashRows.single()
        assertEquals(PaymentMode.CASH, cash.paymentMode)
        assertEquals(TransactionType.UDHAR_GIVEN, cash.transactionType)
    }

    @Test
    fun priorDateLoanAutomaticallyUsesBookEntryPaymentMode() = runTest {
        val repo = FakeLendRepository()
        val useCase = newUseCase(repo)
        val yesterdayLateIst = Instant.parse("2026-11-26T18:29:00Z").toEpochMilliseconds() // 23:59 IST on 26 Nov

        useCase.createLoan(
            UdhaarLoanForm(
                shopId = "shop-1",
                partyId = "party-1",
                direction = LendDirection.GIVEN,
                amountPaisa = 500_000L,
                date = yesterdayLateIst,
            )
        )

        val cash = repo.cashRows.single()
        assertEquals(PaymentMode.BOOK_ENTRY, cash.paymentMode)

        // Must not affect physical drawer opening or daily cash
        val window = CashDrawer.dayWindow(Instant.fromEpochMilliseconds(todayNoonIst), ist)
        val summary = CashDrawer.summarize(repo.cashRows, window, timeZone = ist)
        assertEquals(0L, summary.openingPaisa)
        assertEquals(0L, summary.cashInPaisa)
        assertEquals(0L, summary.cashOutPaisa)
        assertEquals(0L, summary.closingPaisa)
    }

    @Test
    fun sameDayEarlyMorningIstStillUsesCash() = runTest {
        val repo = FakeLendRepository()
        val lateNightTodayIst = Instant.parse("2026-11-27T18:00:00Z").toEpochMilliseconds() // 23:30 IST on 27 Nov
        val earlyMorningTodayIst = Instant.parse("2026-11-26T18:31:00Z").toEpochMilliseconds() // 00:01 IST on 27 Nov
        val useCase = newUseCase(repo, nowMs = lateNightTodayIst)

        useCase.createLoan(
            UdhaarLoanForm(
                shopId = "shop-1",
                partyId = "party-1",
                direction = LendDirection.TAKEN,
                amountPaisa = 250_000L,
                date = earlyMorningTodayIst,
            )
        )

        assertEquals(PaymentMode.CASH, repo.cashRows.single().paymentMode)
    }

    @Test
    fun explicitBookEntryOnTodayDateIsPreserved() = runTest {
        val repo = FakeLendRepository()
        val useCase = newUseCase(repo)

        useCase.createLoan(
            UdhaarLoanForm(
                shopId = "shop-1",
                partyId = "party-1",
                direction = LendDirection.GIVEN,
                amountPaisa = 300_000L,
                date = todayNoonIst,
                paymentMode = PaymentMode.BOOK_ENTRY,
            )
        )

        val cash = repo.cashRows.single()
        assertEquals(PaymentMode.BOOK_ENTRY, cash.paymentMode)

        val window = CashDrawer.dayWindow(Instant.fromEpochMilliseconds(todayNoonIst), ist)
        val summary = CashDrawer.summarize(repo.cashRows, window, timeZone = ist)
        assertEquals(0L, summary.openingPaisa)
        assertEquals(0L, summary.cashInPaisa)
        assertEquals(0L, summary.cashOutPaisa)
        assertEquals(0L, summary.closingPaisa)
    }
}
