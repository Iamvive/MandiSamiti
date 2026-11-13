package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.LendStatus
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.usecase.LendException
import com.appwork.mandisamiti.domain.usecase.LendForm
import com.appwork.mandisamiti.domain.usecase.LendTransactionUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RepaymentEditRepositoryTest {
    private val day = 86_400_000L
    private val clock = object : Clock { override fun now() = Instant.parse("2026-11-13T05:30:00Z") }

    private class Fixture(val repo: OfflineFirstLendRepository, val useCase: LendTransactionUseCase, val setNow: (Long) -> Unit)

    private fun fixture(): Pair<Fixture, com.appwork.mandisamiti.database.AppDatabase> {
        val db = createTestDatabase()
        val repo = OfflineFirstLendRepository(db, clock = clock)
        var t = 0L
        var n = 0
        val useCase = LendTransactionUseCase(repo, { "id${++n}" }, { t })
        return Fixture(repo, useCase) { t = it } to db
    }

    private suspend fun Fixture.threePayments(): String {
        val lend = useCase.lend(LendForm(shopId = "shop", partyId = "p", lendType = LendType.MONEY, amountPaisa = 5_000_000L))
        setNow(30 * day); useCase.repayWithInterest(lend.id, 1_000_000L, 100_000L, 100_000L)
        setNow(60 * day); useCase.repayWithInterest(lend.id, 500_000L, 82_000L, 82_000L)
        setNow(90 * day); useCase.repayWithInterest(lend.id, 500_000L, 73_640L, 73_640L)
        return lend.id
    }

    @Test
    fun editMiddlePaymentMovesCashAndRecalculatesLater() = runTest {
        val (f, db) = fixture()
        val lendId = f.threePayments()
        val q = db.appDatabaseQueries
        val (a, b, c) = f.repo.getSettlements(lendId)

        fun received(id: String, lend: String) = q.getCashTransactionsByTag("[SETTLE:$id:$lend]").executeAsList()
            .associate { it.transaction_type to it.amount_paisa }
        val totalBefore = listOf(a, b, c).sumOf { s -> received(s.id, lendId).values.sum() }

        val plan = f.repo.editSettlement(b.id, 550_000L, 60 * day, 2.0)

        assertEquals(50_000L, plan.gallaDeltaPaisa)
        assertEquals(mapOf("JAMA_RECEIVED" to 468_000L, "INTEREST_RECEIVED" to 82_000L), received(b.id, lendId))
        assertEquals(mapOf("JAMA_RECEIVED" to 427_360L, "INTEREST_RECEIVED" to 72_640L), received(c.id, lendId))
        assertEquals(received(a.id, lendId), mapOf("JAMA_RECEIVED" to 900_000L, "INTEREST_RECEIVED" to 100_000L))
        val totalAfter = listOf(a, b, c).sumOf { s -> received(s.id, lendId).values.sum() }
        assertEquals(totalBefore + 50_000L, totalAfter)

        val history = f.repo.getSettlementHistory(b.id)
        assertEquals(listOf(500_000L, 550_000L), history.map { it.amountPaisa })
        assertEquals(2L, f.repo.getSettlements(lendId)[1].revision)
        assertEquals(LendStatus.PARTIAL, f.repo.getLendEntryById(lendId)!!.status)
    }

    @Test
    fun editingDateReordersAndMovesCashRowDate() = runTest {
        val (f, db) = fixture()
        val lendId = f.threePayments()
        val b = f.repo.getSettlements(lendId)[1]
        f.repo.editSettlement(b.id, 500_000L, 45 * day, 2.0)
        val row = db.appDatabaseQueries.getCashTransactionsByTag("[SETTLE:${b.id}:$lendId]").executeAsList().first()
        assertEquals(45 * day, row.transaction_date)
        assertEquals(45 * day, f.repo.getSettlements(lendId)[1].settledAt)
    }

    @Test
    fun overRepaymentEditWritesNothing() = runTest {
        val (f, db) = fixture()
        val lendId = f.threePayments()
        val b = f.repo.getSettlements(lendId)[1]
        assertFailsWith<LendException.OverRepayment> { f.repo.editSettlement(b.id, 9_000_000L, 60 * day, 2.0) }
        assertEquals(1L, f.repo.getSettlements(lendId)[1].revision)
        assertEquals(1, f.repo.getSettlementHistory(b.id).size)
    }

    @Test
    fun editThatClearsTheLoanSettlesIt() = runTest {
        val (f, _) = fixture()
        val lendId = f.threePayments()
        val c = f.repo.getSettlements(lendId)[2]
        // Principal left after day 60 is 3,682,000 and day-90 interest is 73,640, so 3,755,640 clears the loan exactly.
        f.repo.editSettlement(c.id, 3_755_640L, 90 * day, 2.0)
        assertEquals(LendStatus.SETTLED, f.repo.getLendEntryById(lendId)!!.status)
    }
}
