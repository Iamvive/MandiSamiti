package com.appwork.mandisamiti.audit

import com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstSellRepository
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.model.VoidReason
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * X4 Audit Test Suite for Finding P3 (Slip Number Continuity & Shop-Wide Series).
 * Conforms to Decision 2026-11-01:
 * - Buy slip numbers use one sequence for the entire shop (like Sell).
 * - Restoring from sync raises the local sequence to MAX + 1.
 * - Two different farmers never both receive slip #1.
 * - Voided slips never cause sequence numbers to be reused.
 */
class P3SlipNumberAuditTest {

    private suspend fun seedShopAndParties(db: AppDatabase, shopId: String = "shop-p3") {
        db.appDatabaseQueries.insertShopProfile(
            shopId, "परीक्षण आढ़त", "मुनीम", "मंडी", null, "9876543210", "", 1.5, 1L, 0L, 0L, 1L
        )
        val partyRepo = OfflineFirstPartyRepository(db)
        partyRepo.saveParty(
            Party(
                id = "farmer-a",
                shopId = shopId,
                name = "TEST किसान क",
                phone = "9876543211",
                village = "गांव 1",
                partyType = PartyType.FARMER,
                createdAt = 1L,
                updatedAt = 1L
            )
        )
        partyRepo.saveParty(
            Party(
                id = "farmer-b",
                shopId = shopId,
                name = "TEST किसान ख",
                phone = "9876543212",
                village = "गांव 2",
                partyType = PartyType.FARMER,
                createdAt = 1L,
                updatedAt = 1L
            )
        )
    }

    private fun buy(id: String, farmer: String, date: Long = 1000L) = BuyEntry(
        id = id,
        shopId = "shop-p3",
        farmerId = farmer,
        commodityId = "wheat",
        entryDate = date,
        bagsCount = 10L,
        weightPerBagGrams = 50_000L,
        extraWeightGrams = 0L,
        totalWeightGrams = 500_000L,
        ratePaisaPerQuintal = 200_000L,
        commissionBps = 0L,
        labourPerBagPaisa = 0L,
        advancePaidPaisa = 0L,
        grossPaisa = 100_000L,
        commissionPaisa = 0L,
        labourPaisa = 0L,
        netPayablePaisa = 100_000L,
        createdAt = date,
        updatedAt = date
    )

    private fun sell(id: String, buyer: String, date: Long = 1000L) = SellEntry(
        id = id,
        shopId = "shop-p3",
        buyerId = buyer,
        commodityId = "wheat",
        entryDate = date,
        billMode = BillMode.NORMAL,
        partyKind = SellPartyKind.MERCHANT,
        bagsCount = 10L,
        weightPerBagGrams = 50_000L,
        totalWeightGrams = 500_000L,
        ratePaisaPerQuintal = 200_000L,
        aratBps = 0L,
        tulakPaisa = 0L,
        grossPaisa = 100_000L,
        aratPaisa = 0L,
        mandiShulkPaisa = 0L,
        totalReceivablePaisa = 100_000L,
        receivedPaisa = 0L,
        balanceDuePaisa = 100_000L,
        createdAt = date,
        updatedAt = date
    )

    private fun lend(id: String, date: Long = 1000L) = LendEntry(
        id = id,
        shopId = "shop-p3",
        partyId = "farmer-a",
        lendType = LendType.MONEY,
        entryDate = date,
        amountPaisa = 100_000L,
        createdAt = date,
        updatedAt = date
    )

    @Test
    fun auditP3_buySlipNumbersUseOneSeriesForEntireShop() = runTest {
        val db = createTestDatabase()
        seedShopAndParties(db)
        val buyRepo = OfflineFirstBuyRepository(db)

        // Farmer A receives first slip
        buyRepo.saveBuyEntry(buy("b1", "farmer-a"))
        val slip1No = buyRepo.getBuyEntryById("b1")?.slipNo
        assertEquals(1L, slip1No, "First slip must be 1")

        // Farmer B receives second slip in same shop
        buyRepo.saveBuyEntry(buy("b2", "farmer-b"))
        val slip2No = buyRepo.getBuyEntryById("b2")?.slipNo
        assertEquals(2L, slip2No, "Second farmer in same shop must get slip #2")
        assertNotEquals(1L, slip2No, "P3 violation: different farmers must not both hold slip #1")

        // Farmer A receives third slip
        buyRepo.saveBuyEntry(buy("b3", "farmer-a"))
        val slip3No = buyRepo.getBuyEntryById("b3")?.slipNo
        assertEquals(3L, slip3No, "Third slip must increment monotonically to #3")
    }

    @Test
    fun auditP3_restoringFromSyncSetsNextSlipToMaxPlusOne() = runTest {
        val db = createTestDatabase()
        seedShopAndParties(db)
        val buyRepo = OfflineFirstBuyRepository(db)

        // Seed an existing restored slip #7 directly with slipNo = 7L
        buyRepo.saveBuyEntry(buy("b-restored", "farmer-a").copy(slipNo = 7L))

        // Next slip created locally without explicit slipNo must be 8
        buyRepo.saveBuyEntry(buy("b-next", "farmer-b"))
        val nextSlipNo = buyRepo.getBuyEntryById("b-next")?.slipNo
        assertEquals(8L, nextSlipNo, "Post-sync slip number must be MAX(slip_no) + 1")
    }

    @Test
    fun auditP3_voidedSlipsNeverCauseNumberReuse() = runTest {
        val db = createTestDatabase()
        seedShopAndParties(db)
        val buyRepo = OfflineFirstBuyRepository(db)

        buyRepo.saveBuyEntry(buy("b-void-1", "farmer-a"))
        val slip1 = buyRepo.getBuyEntryById("b-void-1")?.slipNo
        assertEquals(1L, slip1)

        // Void slip #1
        buyRepo.voidBuyEntry("b-void-1", VoidReason.WRONG_ENTRY)

        // Next slip must be 2, never reusing voided #1
        buyRepo.saveBuyEntry(buy("b-after-void", "farmer-a"))
        val slip2 = buyRepo.getBuyEntryById("b-after-void")?.slipNo
        assertEquals(2L, slip2, "Voided slip number must never be reused")
    }

    @Test
    fun auditP3_sellAndLendSeriesIncrementIndependentlyWithoutRestarting() = runTest {
        val db = createTestDatabase()
        seedShopAndParties(db)
        val sellRepo = OfflineFirstSellRepository(db)
        val lendRepo = OfflineFirstLendRepository(db)

        sellRepo.saveSellEntry(sell("s1", "buyer-1"))
        sellRepo.saveSellEntry(sell("s2", "buyer-2"))
        assertEquals(1L, sellRepo.getSellEntryById("s1")?.slipNo)
        assertEquals(2L, sellRepo.getSellEntryById("s2")?.slipNo)

        lendRepo.saveLendEntry(lend("l1"))
        lendRepo.saveLendEntry(lend("l2"))
        assertEquals(1L, lendRepo.getLendEntryById("l1")?.slipNo)
        assertEquals(2L, lendRepo.getLendEntryById("l2")?.slipNo)
    }
}
