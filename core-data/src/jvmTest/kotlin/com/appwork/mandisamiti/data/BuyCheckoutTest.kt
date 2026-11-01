package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
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
