package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.LendStatus
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.usecase.LendException
import com.appwork.mandisamiti.domain.usecase.LendForm
import com.appwork.mandisamiti.domain.usecase.LendTransactionUseCase
import com.appwork.mandisamiti.domain.model.PaymentMode
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RepaymentEditRepositoryTest {
    private val day = 86_400_000L
    private val clock = object : Clock { override fun now() = Instant.parse("2026-11-13T05:30:00Z") }

    private class Fixture(
        val db: com.appwork.mandisamiti.database.AppDatabase,
        val repo: OfflineFirstLendRepository,
        val useCase: LendTransactionUseCase,
        val setNow: (Long) -> Unit,
    )

    private fun fixture(): Fixture {
        val db = createTestDatabase()
        val repo = OfflineFirstLendRepository(db, clock = clock)
        var t = 0L
        var n = 0
        val useCase = LendTransactionUseCase(repo, { "id${++n}" }, { t })
        return Fixture(db, repo, useCase) { t = it }
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
        val f = fixture()
        val db = f.db
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
        val f = fixture()
        val db = f.db
        val lendId = f.threePayments()
        val b = f.repo.getSettlements(lendId)[1]
        f.repo.editSettlement(b.id, 500_000L, 45 * day, 2.0)
        val row = db.appDatabaseQueries.getCashTransactionsByTag("[SETTLE:${b.id}:$lendId]").executeAsList().first()
        assertEquals(45 * day, row.transaction_date)
        assertEquals(45 * day, f.repo.getSettlements(lendId)[1].settledAt)
    }

    @Test
    fun overRepaymentEditWritesNothing() = runTest {
        val f = fixture()
        val lendId = f.threePayments()
        val b = f.repo.getSettlements(lendId)[1]
        assertFailsWith<LendException.OverRepayment> { f.repo.editSettlement(b.id, 9_000_000L, 60 * day, 2.0) }
        assertEquals(1L, f.repo.getSettlements(lendId)[1].revision)
        assertEquals(1, f.repo.getSettlementHistory(b.id).size)
    }

    @Test
    fun editThatClearsTheLoanSettlesIt() = runTest {
        val f = fixture()
        val lendId = f.threePayments()
        val c = f.repo.getSettlements(lendId)[2]
        // Principal left after day 60 is 3,682,000 and day-90 interest is 73,640, so 3,755,640 clears the loan exactly.
        f.repo.editSettlement(c.id, 3_755_640L, 90 * day, 2.0)
        assertEquals(LendStatus.SETTLED, f.repo.getLendEntryById(lendId)!!.status)
    }

    private fun Fixture.allCash() = db.appDatabaseQueries.getCashTransactionsByShop("shop").executeAsList().sortedBy { it.id }
    private fun Fixture.tagged(id: String, lend: String) = db.appDatabaseQueries.getCashTransactionsByTag("[SETTLE:$id:$lend]").executeAsList()

    @Test
    fun interestRowThatDropsToZeroIsVoided() = runTest {
        val f = fixture()
        val lendId = f.threePayments()
        val (a, b, _) = f.repo.getSettlements(lendId)
        val interestRow = f.tagged(b.id, lendId).single { it.transaction_type == "INTEREST_RECEIVED" }
        // Same day as the first payment: nothing has accrued since, so no interest is due.
        f.repo.editSettlement(b.id, 500_000L, 30 * day, 2.0)
        val voided = f.db.appDatabaseQueries.getCashTransactionById(interestRow.id).executeAsOne()
        assertEquals(1L, voided.is_void)
        assertEquals("REPAYMENT_EDITED", voided.void_reason)
        assertEquals(interestRow.revision + 1, voided.revision)
        assertEquals(0L, voided.sync_status)
        assertEquals(listOf("JAMA_RECEIVED"), f.tagged(b.id, lendId).map { it.transaction_type })
        assertEquals(500_000L, f.tagged(b.id, lendId).single().amount_paisa)
        assertEquals(0L, f.repo.getSettlements(lendId).first { it.id == b.id }.interestPaisa)
        assertEquals(2, f.tagged(a.id, lendId).size)
    }

    @Test
    fun missingInterestRowIsCreatedWithPartyModeAndTag() = runTest {
        val f = fixture()
        f.db.appDatabaseQueries.insertPartyIfAbsent(
            id = "payer", shop_id = "shop", name = "payer", phone = null, village = null, party_type = "FARMER",
            monthly_interest_rate = null, photo_uri = null, gstin = null, mandi_license = null,
            created_at = 0L, updated_at = 0L, is_deleted = 0L, sync_status = 0L,
        )
        val lend = f.useCase.lend(LendForm(shopId = "shop", partyId = "p", lendType = LendType.MONEY, amountPaisa = 5_000_000L))
        f.setNow(30 * day); f.useCase.repayWithInterest(lend.id, 1_000_000L, 100_000L, 100_000L)
        // Interest waived: 82,000 was calculated but none taken, so this payment has no INTEREST_RECEIVED row.
        f.setNow(60 * day); f.useCase.repayWithInterest(lend.id, 500_000L, 0L, 82_000L, cashPartyId = "payer", paymentMode = PaymentMode.UPI)
        val b = f.repo.getSettlements(lend.id)[1]
        assertEquals(listOf("JAMA_RECEIVED"), f.tagged(b.id, lend.id).map { it.transaction_type })

        f.repo.editSettlement(b.id, 600_000L, 60 * day, 2.0)

        val rows = f.tagged(b.id, lend.id).associateBy { it.transaction_type }
        val interest = rows.getValue("INTEREST_RECEIVED")
        assertEquals(82_000L, interest.amount_paisa)
        assertEquals("payer", interest.party_id)
        assertEquals("UPI", interest.payment_mode)
        assertEquals(60 * day, interest.transaction_date)
        assertEquals(0L, interest.is_void)
        assertEquals(518_000L, rows.getValue("JAMA_RECEIVED").amount_paisa)
    }

    @Test
    fun touchedCashRowsBumpRevisionAndSyncUntouchedStay() = runTest {
        val f = fixture()
        val lendId = f.threePayments()
        val (a, b, c) = f.repo.getSettlements(lendId)
        val before = f.allCash().associateBy { it.id }
        f.repo.editSettlement(b.id, 550_000L, 60 * day, 2.0)
        val after = f.allCash().associateBy { it.id }
        assertEquals(before.keys, after.keys)
        // b's interest (82,000) is unchanged, so that row is not touched even though the payment is.
        val bInterest = f.tagged(b.id, lendId).single { it.transaction_type == "INTEREST_RECEIVED" }.id
        for (id in listOf(b, c).flatMap { f.tagged(it.id, lendId) }.map { it.id }.filter { it != bInterest }) {
            assertEquals(before.getValue(id).revision + 1, after.getValue(id).revision)
            assertEquals(0L, after.getValue(id).sync_status)
        }
        assertEquals(before.getValue(bInterest), after.getValue(bInterest))
        for (row in f.tagged(a.id, lendId)) assertEquals(before.getValue(row.id), after.getValue(row.id))
    }

    @Test
    fun generalJamaOverflowRowIsNeverTouched() = runTest {
        val f = fixture()
        val lend = f.useCase.lend(LendForm(shopId = "shop", partyId = "p", lendType = LendType.MONEY, amountPaisa = 1_000_000L))
        f.setNow(30 * day)
        val split = f.useCase.repayWithInterest(lend.id, 1_200_000L, 0L, 0L)
        assertEquals(200_000L, split.generalPaisa)
        val s = f.repo.getSettlements(lend.id).single()
        val overflow = f.allCash().single { !it.remarks.orEmpty().contains("[SETTLE:") && it.transaction_type == "JAMA_RECEIVED" && it.remarks.orEmpty() != "" && it.amount_paisa == 200_000L }

        f.repo.editSettlement(s.id, 800_000L, 30 * day, 2.0)

        assertEquals(overflow, f.db.appDatabaseQueries.getCashTransactionById(overflow.id).executeAsOne())
        assertEquals(800_000L, f.tagged(s.id, lend.id).single().amount_paisa)
        assertEquals(LendStatus.PARTIAL, f.repo.getLendEntryById(lend.id)!!.status)
    }

    @Test
    fun overRepaymentRollsBackCashSettlementsAndStatus() = runTest {
        val f = fixture()
        val lendId = f.threePayments()
        val b = f.repo.getSettlements(lendId)[1]
        val cashBefore = f.allCash()
        val settlementsBefore = f.repo.getSettlements(lendId)
        val lendBefore = f.repo.getLendEntryById(lendId)
        val revisionsBefore = settlementsBefore.map { f.repo.getSettlementHistory(it.id).size }
        assertFailsWith<LendException.OverRepayment> { f.repo.editSettlement(b.id, 9_000_000L, 60 * day, 2.0) }
        assertEquals(cashBefore, f.allCash())
        assertEquals(settlementsBefore, f.repo.getSettlements(lendId))
        assertEquals(lendBefore, f.repo.getLendEntryById(lendId))
        assertEquals(revisionsBefore, settlementsBefore.map { f.repo.getSettlementHistory(it.id).size })
    }

    @Test
    fun reducingAPaymentOnASettledLoanRevertsItToPartial() = runTest {
        val f = fixture()
        val lendId = f.threePayments()
        val (a, _, c) = f.repo.getSettlements(lendId)
        f.repo.editSettlement(c.id, 3_755_640L, 90 * day, 2.0)
        assertEquals(LendStatus.SETTLED, f.repo.getLendEntryById(lendId)!!.status)
        val revision = f.repo.getLendEntryById(lendId)!!.revision

        f.repo.editSettlement(a.id, 500_000L, 30 * day, 2.0)

        val lend = f.repo.getLendEntryById(lendId)!!
        assertEquals(LendStatus.PARTIAL, lend.status)
        assertEquals(revision + 1, lend.revision)
    }

    @Test
    fun recalculatedLaterPaymentKeepsTwoHistoryEntries() = runTest {
        val f = fixture()
        val lendId = f.threePayments()
        val (_, b, c) = f.repo.getSettlements(lendId)
        f.repo.editSettlement(b.id, 550_000L, 60 * day, 2.0)
        val history = f.repo.getSettlementHistory(c.id)
        assertEquals(listOf(1L, 2L), history.map { it.revision })
        assertEquals(listOf(73_640L, 72_640L), history.map { it.interestPaisa })
    }
}
