package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.cash.CashDrawer
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Phase 3 C6: party, slip, linked cash rows and revisions are one transaction. */
class BuyCheckoutTest {
    private val party = Party("p1", "shop", "TEST रमेश", "9876543210", "TEST गाँव", PartyType.FARMER, createdAt = 1, updatedAt = 1)

    // Commodity worth 9,830; advance 2,000 given as cash. Net stays the full value.
    private fun slip() = BuyEntry(
        id = "b1", shopId = "shop", farmerId = "p1", commodityId = "गेहूं", entryDate = 1, bagsCount = 10,
        weightPerBagGrams = 50_000, extraWeightGrams = 0, totalWeightGrams = 500_000, ratePaisaPerQuintal = 200_000,
        commissionBps = 70, labourPerBagPaisa = 1_000, advancePaidPaisa = 200_000, grossPaisa = 1_000_000,
        commissionPaisa = 7_000, labourPaisa = 10_000, netPayablePaisa = 983_000, paidPaisa = 200_000,
        createdAt = 1, updatedAt = 1,
    )

    private fun cash(id: String, amount: Long) = CashTransaction(
        id = id, shopId = "shop", partyId = "p1", transactionType = TransactionType.UDHAR_GIVEN,
        amountPaisa = amount, transactionDate = 1, createdAt = 1, updatedAt = 1,
        sourceEntryType = "BUY", sourceEntryId = "b1",
    )

    // Loan of 5,000 to the same farmer. He asks for 3,000 to be cut: 300 interest first, 2,700 principal.
    private suspend fun loan(db: com.appwork.mandisamiti.database.AppDatabase) {
        com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository(db).saveLendEntry(
            com.appwork.mandisamiti.domain.model.LendEntry(
                id = "l1", shopId = "shop", partyId = "p1", lendType = com.appwork.mandisamiti.domain.model.LendType.MONEY,
                entryDate = 1, amountPaisa = 500_000, createdAt = 1, updatedAt = 1,
            )
        )
    }

    private fun cut(principal: Long, interest: Long) = com.appwork.mandisamiti.domain.model.LendSettlement(
        id = "s1", shopId = "shop", lendId = "l1", settledAt = 1, amountPaisa = principal + interest,
        createdAt = 1, updatedAt = 1, principalPaisa = principal, interestPaisa = interest, calculatedInterestPaisa = interest,
    )

    @Test
    fun loanCutIsAnOffsetOnTheSlipAndTheBalanceStaysTrue() = runTest {
        val db = createTestDatabase()
        loan(db)
        val repo = OfflineFirstBuyRepository(db)
        repo.saveBuyCheckout(party = party, entry = slip().copy(paidPaisa = 500_000), payments = listOf(cash("c1", 200_000)), loanCutPaisa = 300_000L)
        val q = db.appDatabaseQueries
        // Slip: 9,830 - (2,000 advance + 3,000 cut) = 4,830 owed. Loan: 5,000 - 3,000 = 2,000 owed. Slip owes 4,830 and udhaar owes 2,000, tracked separately.
        assertEquals(-483_000L, q.getPartyBalance("p1").executeAsOne().slip_balance_paisa)
        assertEquals(200_000L, q.getPartyBalance("p1").executeAsOne().udhaar_given_paisa)
        assertEquals(com.appwork.mandisamiti.domain.model.LendStatus.PARTIAL, com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository(db).getLendEntryById("l1")!!.status)
        // The advance is the cash payment linked to the slip; loan cut generates a BOOK_ENTRY cash row
        assertEquals(1, q.getCashTransactionById("c1").executeAsList().size)
    }

    @Test
    fun loanCutLargerThanTheLoanRollsBackTheWholeSlip() = runTest {
        val db = createTestDatabase()
        loan(db)
        val repo = OfflineFirstBuyRepository(db)
        assertFailsWith<Exception> {
            repo.saveBuyCheckout(slip().copy(paidPaisa = 800_000), party, listOf(cash("c1", 200_000)), loanCutPaisa = 600_000L)
        }
        assertNull(repo.getBuyEntryById("b1"))
        assertNull(db.appDatabaseQueries.getCashTransactionById("c1").executeAsOneOrNull())
        assertEquals(0, db.appDatabaseQueries.getRevisionsForEntry("b1").executeAsList().size)
    }

    @Test
    fun slipAdvanceStaysOnTheBillAndOutOfPesgi() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstBuyRepository(db)
        repo.saveBuyCheckout(slip(), party, listOf(cash("c1", 200_000)))
        db.appDatabaseQueries.insertCashTransaction(
            id = "c-legacy", shop_id = "shop", party_id = "p1", deal_id = null,
            transaction_type = TransactionType.UDHAR_GIVEN.name, amount_paisa = 50_000L,
            payment_mode = PaymentMode.CASH.name, transaction_date = 1L,
            voice_note_uri = null, remarks = "पुरानी पेशगी", created_at = 1L, updated_at = 1L,
            is_deleted = 0L, sync_status = 0L, revision = 1L, is_void = 0L, void_reason = null,
            source_entry_type = null, source_entry_id = null, lend_id = null,
        )

        val q = db.appDatabaseQueries
        assertEquals(200_000L, repo.getBuyEntryById("b1")!!.advancePaidPaisa)
        assertEquals(0, q.getLendEntriesByShop("shop").executeAsList().size)
        assertEquals(0L, q.getPartyBalance("p1").executeAsOne().udhaar_given_paisa)

        val saved = q.getCashTransactionById("c1").executeAsOne()
        assertEquals("b1", saved.source_entry_id)
        val drawerTx = CashTransaction(
            id = saved.id, shopId = saved.shop_id, partyId = saved.party_id!!,
            transactionType = TransactionType.valueOf(saved.transaction_type),
            amountPaisa = saved.amount_paisa, paymentMode = PaymentMode.valueOf(saved.payment_mode),
            transactionDate = saved.transaction_date, createdAt = saved.created_at, updatedAt = saved.updated_at,
            isDeleted = saved.is_deleted == 1L, isVoid = saved.is_void == 1L,
        )
        assertEquals(-200_000L, CashDrawer.balancePaisa(listOf(drawerTx)))

        val advances = db.bahiDayQueriesQueries.getUnlinkedFarmerAdvancesBefore(
            shopId = "shop", farmerId = "p1", purchaseMillis = 10L,
        ).executeAsList()
        assertEquals(listOf("c-legacy"), advances.map { it.id })
    }

    @Test
    fun partyBalanceCountsTheAdvanceOnce() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstBuyRepository(db)
        repo.saveBuyCheckout(slip(), party, listOf(cash("c1", 200_000)))
        // Owed 9,830 for wheat, 2,000 already handed over: shop owes 7,830.
        assertEquals(-783_000L, db.appDatabaseQueries.getPartyBalance("p1").executeAsOne().slip_balance_paisa)
    }

    @Test
    fun writesPartySlipAndLinkedCashRowTogether() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstBuyRepository(db)
        repo.saveBuyCheckout(slip(), party, listOf(cash("c1", 200_000)))
        val q = db.appDatabaseQueries
        assertEquals("TEST रमेश", q.getPartyById("p1").executeAsOne().name)
        assertEquals(983_000L, repo.getBuyEntryById("b1")!!.netPayablePaisa)
        val c = q.getCashTransactionById("c1").executeAsOne()
        assertEquals("b1", c.source_entry_id)
        assertEquals(1, q.getRevisionsForEntry("c1").executeAsList().size)
        assertEquals(1, q.getRevisionsForEntry("b1").executeAsList().size)
    }

    @Test
    fun aFailureMidSaveLeavesNothingBehind() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstBuyRepository(db)
        assertFailsWith<IllegalArgumentException> {
            repo.saveBuyCheckout(slip(), party, listOf(cash("c1", 200_000), cash("c2", 0)).let { it })
        }
        val q = db.appDatabaseQueries
        assertNull(q.getPartyById("p1").executeAsOneOrNull())
        assertNull(repo.getBuyEntryById("b1"))
        assertNull(q.getCashTransactionById("c1").executeAsOneOrNull())
        assertEquals(0, q.getRevisionsForEntry("b1").executeAsList().size)
    }

    @Test
    fun paidMustEqualTheCashRows() = runTest {
        val repo = OfflineFirstBuyRepository(createTestDatabase())
        assertFailsWith<IllegalArgumentException> {
            repo.saveBuyCheckout(slip().copy(paidPaisa = 100), party, listOf(cash("c1", 200_000)))
        }
    }

    @Test
    fun renamedPartyIsUpdatedNotDuplicated() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstBuyRepository(db)
        repo.saveBuyCheckout(slip(), party, listOf(cash("c1", 200_000)))
        repo.saveBuyCheckout(slip().copy(id = "b2", paidPaisa = 0, advancePaidPaisa = 0), party.copy(name = "TEST रमेश कुमार"), emptyList())
        assertEquals("TEST रमेश कुमार", db.appDatabaseQueries.getPartyById("p1").executeAsOne().name)
        assertEquals(1, db.appDatabaseQueries.getAllParties("shop").executeAsList().size)
    }
}
