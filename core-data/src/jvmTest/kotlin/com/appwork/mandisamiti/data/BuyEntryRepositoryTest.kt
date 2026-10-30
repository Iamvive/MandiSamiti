package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.VoidReason
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuyEntryRepositoryTest {
    private val fixedNow = Instant.parse("2026-10-08T05:30:00Z")
    private val clock = object : Clock { override fun now() = fixedNow }

    private fun sampleBuyEntry(id: String) = BuyEntry(
        id = id, shopId = "shop", farmerId = "farmer-1", commodityId = "comm-1",
        entryDate = 1_000L, bagsCount = 10, weightPerBagGrams = 50_000, extraWeightGrams = 5_000,
        totalWeightGrams = 505_000, ratePaisaPerQuintal = 250_000, commissionBps = 200,
        labourPerBagPaisa = 500, advancePaidPaisa = 100_000, grossPaisa = 1_262_500,
        commissionPaisa = 25_250, labourPaisa = 5_000, netPayablePaisa = 1_132_250,
        createdAt = 1_000L, updatedAt = 1_000L,
    )

    private fun seedFarmer(db: com.appwork.mandisamiti.database.AppDatabase) {
        db.appDatabaseQueries.insertParty(
            id = "farmer-1", shop_id = "shop", name = "रामवीर सिंह", village = null, phone = null,
            party_type = PartyType.FARMER.name, monthly_interest_rate = null, photo_uri = null,
            gstin = null, mandi_license = null,
            created_at = 1_000L, updated_at = 1_000L, is_deleted = 0L, sync_status = 0L,
        )
    }

    @Test
    fun saveWritesRowAndCreateRevision() = runTest {
        val db = createTestDatabase()
        seedFarmer(db)
        val repo = OfflineFirstBuyRepository(db, clock = clock)
        repo.saveBuyEntry(sampleBuyEntry(id = "b1"))
        assertEquals(1, db.appDatabaseQueries.getRevisionsForEntry("b1").executeAsList().size)
        assertEquals("CREATE", db.appDatabaseQueries.getRevisionsForEntry("b1").executeAsOne().change_kind)
        assertEquals("BUY", db.appDatabaseQueries.getRevisionsForEntry("b1").executeAsOne().entry_kind)
    }

    @Test
    fun editBumpsRevisionAndKeepsHistory() = runTest {
        val db = createTestDatabase()
        seedFarmer(db)
        val repo = OfflineFirstBuyRepository(db, clock = clock)
        repo.saveBuyEntry(sampleBuyEntry(id = "b1"))
        repo.editBuyEntry(sampleBuyEntry(id = "b1").copy(paidPaisa = 500))
        assertEquals(2, repo.getBuyEntryById("b1")!!.revision)
        assertEquals(listOf(1L, 2L), db.appDatabaseQueries.getRevisionsForEntry("b1").executeAsList().map { it.revision })
    }

    @Test
    fun voidedBuyLeavesStockTotals() = runTest {
        val db = createTestDatabase()
        seedFarmer(db)
        val repo = OfflineFirstBuyRepository(db, clock = clock)
        repo.saveBuyEntry(sampleBuyEntry(id = "b1"))
        repo.voidBuyEntry("b1", VoidReason.WRONG_ENTRY)
        assertTrue(db.appDatabaseQueries.getStockBuyTotals("shop").executeAsList().isEmpty())
    }

    @Test
    fun voidedEntryCannotBeEdited() = runTest {
        val db = createTestDatabase()
        seedFarmer(db)
        val repo = OfflineFirstBuyRepository(db, clock = clock)
        repo.saveBuyEntry(sampleBuyEntry(id = "b1"))
        repo.voidBuyEntry("b1", VoidReason.WRONG_ENTRY)
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            repo.editBuyEntry(sampleBuyEntry(id = "b1").copy(paidPaisa = 1L))
        }
    }

    @Test
    fun saveBuyEntryAutoProvisionsPartyAccountAndReflectsNetPayable() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstBuyRepository(db, clock = clock)
        val entry = sampleBuyEntry("b-unseeded").copy(
            farmerId = "राधे श्याम",
            netPayablePaisa = 3_758_000L,
            paidPaisa = 0L,
            remarks = "गाँव: राया | DATA: []"
        )
        repo.saveBuyEntry(entry)

        val party = db.appDatabaseQueries.getPartyById("राधे श्याम").executeAsOneOrNull()
        kotlin.test.assertNotNull(party)
        assertEquals("राधे श्याम", party.name)
        assertEquals("राया", party.village)
        assertEquals("FARMER", party.party_type)

        val balance = db.appDatabaseQueries.getPartyBalance("राधे श्याम").executeAsOne()
        assertEquals(-3_758_000L, balance.balance_paisa)
    }
}
