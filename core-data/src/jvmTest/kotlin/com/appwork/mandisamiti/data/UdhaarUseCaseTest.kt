package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.*
import com.appwork.mandisamiti.domain.repository.LendRepository
import com.appwork.mandisamiti.domain.usecase.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.*

class UdhaarUseCaseTest {
    private val day = 24L * 60 * 60 * 1000
    private val fixedNow = 2_592_001_000L
    private val clock = object : Clock { override fun now() = Instant.fromEpochMilliseconds(fixedNow) }
    private var idCounter = 0

    private data class Fixture(
        val repository: OfflineFirstLendRepository,
        val cashRepository: OfflineFirstCashTransactionRepository,
        val useCase: UdhaarUseCase,
    )

    private fun fixture(testScheduler: kotlinx.coroutines.test.TestCoroutineScheduler): Fixture {
        val db = createTestDatabase()
        db.appDatabaseQueries.insertParty("party", "shop", "Party", null, null, "FARMER", null, null, null, null, 1L, 1L, 0L, 0L)
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = OfflineFirstLendRepository(db, ioDispatcher = dispatcher, clock = clock)
        val cashRepository = OfflineFirstCashTransactionRepository(db, ioDispatcher = dispatcher, clock = clock)
        idCounter = 0
        return Fixture(repository, cashRepository, UdhaarUseCase(repository, { "id-${++idCounter}" }, { fixedNow }))
    }

    private suspend fun Fixture.cashRows() =
        cashRepository.getTransactionsByPartyStream("party").first()

    private fun form(
        direction: LendDirection = LendDirection.GIVEN,
        amountPaisa: Long = 1_000_000L,
        date: Long = fixedNow,
        monthlyRatePercent: Double = 0.0,
        kind: LendKind = LendKind.LOAN,
    ) = UdhaarLoanForm("shop", "party", direction, amountPaisa, date, kind, monthlyRatePercent)

    @Test
    fun createGivenLoanWritesLinkedCashOut() = runTest {
        val f = fixture(testScheduler)
        val lend = f.useCase.createLoan(form())
        val rows = f.cashRows()
        assertEquals(LendDirection.GIVEN, lend.direction)
        assertEquals(0.0, lend.monthlyRatePercent)
        assertEquals(1, rows.size)
        assertEquals(TransactionType.UDHAR_GIVEN, rows.single().transactionType)
        assertEquals(1_000_000L, rows.single().amountPaisa)
        assertEquals(lend.id, rows.single().lendId)
        assertNull(rows.single().sourceEntryId)
    }

    @Test
    fun createGivenPesgiStoresKindPesgiAndWritesLinkedCashOut() = runTest {
        val f = fixture(testScheduler)
        val lend = f.useCase.createLoan(form(kind = LendKind.PESGI))
        val rows = f.cashRows()
        assertEquals(LendDirection.GIVEN, lend.direction)
        assertEquals(LendKind.PESGI, lend.kind)
        assertEquals(1, rows.size)
        assertEquals(TransactionType.UDHAR_GIVEN, rows.single().transactionType)
        assertEquals(1_000_000L, rows.single().amountPaisa)
        assertEquals(lend.id, rows.single().lendId)
        assertNull(rows.single().sourceEntryId)
    }

    @Test
    fun createTakenLoanWritesCashIn() = runTest {
        val f = fixture(testScheduler)
        f.useCase.createLoan(form(direction = LendDirection.TAKEN))
        assertEquals(TransactionType.JAMA_RECEIVED, f.cashRows().single().transactionType)
    }

    @Test
    fun repaymentAllocatesOldestFirstAcrossLoans() = runTest {
        val f = fixture(testScheduler)
        val first = f.useCase.createLoan(form(amountPaisa = 500_000L, date = 1L))
        val second = f.useCase.createLoan(form(amountPaisa = 500_000L, date = 2L))
        val parts = f.useCase.repay("shop", "party", LendDirection.GIVEN, 600_000L, fixedNow)
        assertEquals(listOf(first.id, second.id), parts.map { it.lendId })
        assertEquals(1, f.repository.getSettlements(first.id).size)
        assertEquals(1, f.repository.getSettlements(second.id).size)
        assertEquals(0L, f.repository.getLendEntryById(first.id)!!.amountPaisa - f.repository.getSettlements(first.id).sumOf { it.principalPaisa })
        assertEquals(400_000L, f.repository.getLendEntryById(second.id)!!.amountPaisa - f.repository.getSettlements(second.id).sumOf { it.principalPaisa })
        val repaymentRows = f.cashRows().filter { it.lendId == first.id || it.lendId == second.id }
            .filter { it.transactionDate == fixedNow }
        assertEquals(2, repaymentRows.size)
        assertTrue(repaymentRows.all { it.transactionType == TransactionType.JAMA_RECEIVED })
    }

    @Test
    fun overpaymentWritesNoSettlementOrCash() = runTest {
        val f = fixture(testScheduler)
        val lend = f.useCase.createLoan(form(amountPaisa = 100_000L))
        val cashCount = f.cashRows().size
        val exception = assertFailsWith<UdhaarOverpaymentException> {
            f.useCase.repay("shop", "party", LendDirection.GIVEN, 100_001L, fixedNow)
        }
        assertEquals(100_000L, exception.totalDuePaisa)
        assertTrue(f.repository.getSettlements(lend.id).isEmpty())
        assertEquals(cashCount, f.cashRows().size)
    }

    @Test
    fun bookEntryModeIsPreservedOnRepaymentCashRows() = runTest {
        val f = fixture(testScheduler)
        f.useCase.createLoan(form(amountPaisa = 100_000L))
        f.useCase.repay("shop", "party", LendDirection.GIVEN, 25_000L, fixedNow, mode = PaymentMode.BOOK_ENTRY)
        assertTrue(f.cashRows().any { it.paymentMode == PaymentMode.BOOK_ENTRY && it.transactionDate == fixedNow })
    }

    @Test
    fun interestIsPaidBeforePrincipal() = runTest {
        val f = fixture(testScheduler)
        val lend = f.useCase.createLoan(form(
            amountPaisa = 1_000_000L,
            date = fixedNow - 30L * day,
            monthlyRatePercent = 2.0,
        ))
        val parts = f.useCase.repay("shop", "party", LendDirection.GIVEN, 1_020_000L, fixedNow)
        assertEquals(1, parts.size)
        assertEquals(20_000L, parts.single().interestPaisa)
        assertEquals(1_000_000L, parts.single().principalPaisa)
        val settlement = f.repository.getSettlements(lend.id).single()
        assertEquals(20_000L, settlement.interestPaisa)
        assertEquals(1_000_000L, settlement.principalPaisa)
    }

    @Test
    fun plainCashGaveCreatesGivenCashLoanOrRepaysTakenLoan() = runTest {
        val f = fixture(testScheduler)
        val newLoan = (f.useCase.plainCash("shop", "party", CashSide.GAVE, 5_000L, fixedNow) as PlainCashResult.NewLoan).loan
        assertEquals(LendDirection.GIVEN, newLoan.direction)
        assertEquals(LendKind.CASH, newLoan.kind)
        assertEquals(TransactionType.UDHAR_GIVEN, f.cashRows().last().transactionType)

        val taken = f.useCase.createLoan(form(direction = LendDirection.TAKEN, amountPaisa = 10_000L))
        val result = f.useCase.plainCash("shop", "party", CashSide.GAVE, 5_000L, fixedNow)
        assertIs<PlainCashResult.Repaid>(result)
        assertEquals(taken.id, result.parts.single().lendId)
        assertEquals(5_000L, f.repository.getSettlements(taken.id).single().principalPaisa)
    }

    @Test
    fun plainCashGotRepaysGivenOrCreatesTakenCashLoan() = runTest {
        val f = fixture(testScheduler)
        val given = f.useCase.createLoan(form(amountPaisa = 10_000L))
        val result = f.useCase.plainCash("shop", "party", CashSide.GOT, 5_000L, fixedNow)
        assertIs<PlainCashResult.Repaid>(result)
        assertEquals(given.id, result.parts.single().lendId)
        assertEquals(5_000L, f.repository.getSettlements(given.id).single().principalPaisa)
        assertIs<PlainCashResult.Repaid>(f.useCase.plainCash("shop", "party", CashSide.GOT, 5_000L, fixedNow))

        val taken = (f.useCase.plainCash("shop", "party", CashSide.GOT, 3_000L, fixedNow) as PlainCashResult.NewLoan).loan
        assertEquals(LendDirection.TAKEN, taken.direction)
        assertEquals(LendKind.CASH, taken.kind)
        assertEquals(TransactionType.JAMA_RECEIVED, f.cashRows().last().transactionType)
    }

    @Test
    fun plainCashOverpaymentAgainstOppositeDirectionIsBlocked() = runTest {
        val f = fixture(testScheduler)
        f.useCase.createLoan(form(amountPaisa = 10_000L))
        val rowsBefore = f.cashRows().size
        val exception = assertFailsWith<UdhaarOverpaymentException> {
            f.useCase.plainCash("shop", "party", CashSide.GOT, 10_001L, fixedNow)
        }
        assertEquals(10_000L, exception.totalDuePaisa)
        assertEquals(rowsBefore, f.cashRows().size)
    }

    @Test
    fun totalDueReturnsPrincipalAndAccruedInterestAtGivenDate() = runTest {
        val f = fixture(testScheduler)
        f.useCase.createLoan(form(
            amountPaisa = 1_000_000L,
            date = fixedNow - 30L * day,
            monthlyRatePercent = 2.0,
        ))
        val dueAtNow = f.useCase.totalDue("shop", "party", LendDirection.GIVEN, fixedNow)
        assertEquals(1_000_000L, dueAtNow.principalPaisa)
        assertEquals(20_000L, dueAtNow.interestPaisa)
        assertEquals(1_020_000L, dueAtNow.totalPaisa)

        // At earlier date (only 15 days elapsed), interest is half (10,000L)
        val dueEarlier = f.useCase.totalDue("shop", "party", LendDirection.GIVEN, fixedNow - 15L * day)
        assertEquals(1_000_000L, dueEarlier.principalPaisa)
        assertEquals(10_000L, dueEarlier.interestPaisa)
        assertEquals(1_010_000L, dueEarlier.totalPaisa)
    }

    @Test
    fun edgeCase5_partialPaymentOfOlderLoanOnlyTouchesOlderLoan() = runTest {
        val f = fixture(testScheduler)
        val older = f.useCase.createLoan(form(amountPaisa = 100_000L, date = 1000L))
        val newer = f.useCase.createLoan(form(amountPaisa = 200_000L, date = 2000L))

        // Pay 40,000L (< older amount of 100,000L)
        val parts = f.useCase.repay("shop", "party", LendDirection.GIVEN, 40_000L, fixedNow)
        assertEquals(1, parts.size)
        assertEquals(older.id, parts.single().lendId)
        assertEquals(40_000L, parts.single().principalPaisa)

        // Only older loan has a settlement row; newer loan has none
        assertEquals(1, f.repository.getSettlements(older.id).size)
        assertTrue(f.repository.getSettlements(newer.id).isEmpty())

        // Check total due remaining: older has 60,000L left, newer has 200,000L -> total 260,000L
        val due = f.useCase.totalDue("shop", "party", LendDirection.GIVEN, fixedNow)
        assertEquals(260_000L, due.totalPaisa)
    }

    @Test
    fun edgeCase6_payingExactTotalLeavesZeroDue() = runTest {
        val f = fixture(testScheduler)
        f.useCase.createLoan(form(amountPaisa = 100_000L, date = 1000L))
        f.useCase.createLoan(form(amountPaisa = 200_000L, date = 2000L))

        val dueBefore = f.useCase.totalDue("shop", "party", LendDirection.GIVEN, fixedNow)
        assertEquals(300_000L, dueBefore.totalPaisa)

        // Pay exact rest
        f.useCase.repay("shop", "party", LendDirection.GIVEN, 300_000L, fixedNow)

        val dueAfter = f.useCase.totalDue("shop", "party", LendDirection.GIVEN, fixedNow)
        assertEquals(0L, dueAfter.principalPaisa)
        assertEquals(0L, dueAfter.interestPaisa)
        assertEquals(0L, dueAfter.totalPaisa)
    }
}
