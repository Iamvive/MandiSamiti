package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstSellRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.model.VoidReason
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SellEntryRepositoryTest {
    private val fixedNow = Instant.parse("2026-10-08T05:30:00Z")
    private val clock = object : Clock { override fun now() = fixedNow }

    private fun sampleSellEntry(id: String) = SellEntry(
        id = id, shopId = "shop", buyerId = "buyer-1", commodityId = "comm-1",
        entryDate = 1_000L, billMode = BillMode.NORMAL, partyKind = SellPartyKind.MERCHANT,
        bagsCount = 20, weightPerBagGrams = 50_000, totalWeightGrams = 1_000_000,
        ratePaisaPerQuintal = 260_000, aratBps = 100, tulakPaisa = 0, grossPaisa = 2_600_000,
        aratPaisa = 0, mandiShulkPaisa = 0, totalReceivablePaisa = 2_600_000, receivedPaisa = 0,
        balanceDuePaisa = 2_600_000, createdAt = 1_000L, updatedAt = 1_000L,
    )

    @Test
    fun saveWritesRowAndCreateRevision() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstSellRepository(db, clock = clock)
        repo.saveSellEntry(sampleSellEntry(id = "s1"))
        assertEquals(1, db.appDatabaseQueries.getRevisionsForEntry("s1").executeAsList().size)
        assertEquals("CREATE", db.appDatabaseQueries.getRevisionsForEntry("s1").executeAsOne().change_kind)
        assertEquals("SELL", db.appDatabaseQueries.getRevisionsForEntry("s1").executeAsOne().entry_kind)
    }

    @Test
    fun editBumpsRevisionAndKeepsHistory() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstSellRepository(db, clock = clock)
        repo.saveSellEntry(sampleSellEntry(id = "s1"))
        repo.editSellEntry(sampleSellEntry(id = "s1").copy(receivedPaisa = 500_000))
        assertEquals(2, repo.getSellEntryById("s1")!!.revision)
        assertEquals(listOf(1L, 2L), db.appDatabaseQueries.getRevisionsForEntry("s1").executeAsList().map { it.revision })
    }

    @Test
    fun voidedSellLeavesStockTotals() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstSellRepository(db, clock = clock)
        repo.saveSellEntry(sampleSellEntry(id = "s1"))
        repo.voidSellEntry("s1", VoidReason.WRONG_ENTRY)
        assertTrue(db.appDatabaseQueries.getStockSellTotals("shop").executeAsList().isEmpty())
    }

    @Test
    fun voidedEntryCannotBeEdited() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstSellRepository(db, clock = clock)
        repo.saveSellEntry(sampleSellEntry(id = "s1"))
        repo.voidSellEntry("s1", VoidReason.WRONG_ENTRY)
        assertFailsWith<IllegalArgumentException> {
            repo.editSellEntry(sampleSellEntry(id = "s1").copy(receivedPaisa = 1L))
        }
    }

    @Test
    fun voidKeepsRowAndWritesVoidRevision() = runTest {
        val db = createTestDatabase()
        val sell = OfflineFirstSellRepository(db, clock = clock)

        sell.saveSellEntry(sampleSellEntry("s1"))
        sell.voidSellEntry("s1", VoidReason.WRONG_ENTRY)

        val row = sell.getSellEntryById("s1")!!
        assertTrue(row.isVoid)
        assertEquals(
            "VOID",
            db.appDatabaseQueries.getRevisionsForEntry("s1").executeAsList().last().change_kind,
        )
    }

    @Test
    fun saveSellEntryAutoProvisionsBuyerPartyAccountAndReflectsReceivable() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstSellRepository(db, clock = clock)
        val entry = sampleSellEntry("s-unseeded").copy(
            buyerId = "सुनील ट्रेडर्स",
            totalReceivablePaisa = 2_600_000L,
            receivedPaisa = 600_000L
        )
        repo.saveSellEntry(entry)

        val party = db.appDatabaseQueries.getPartyById("सुनील ट्रेडर्स").executeAsOneOrNull()
        kotlin.test.assertNotNull(party)
        assertEquals("सुनील ट्रेडर्स", party.name)
        assertEquals("BUYER", party.party_type)

        val balance = db.appDatabaseQueries.getPartyBalance("सुनील ट्रेडर्स").executeAsOne()
        assertEquals(2_000_000L, balance.balance_paisa)
    }
}
