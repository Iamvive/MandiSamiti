package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstSellRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.model.VoidReason
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SlipNumberTest {
    private fun buy(id: String, farmer: String, date: Long = 1L) = BuyEntry(
        id = id, shopId = "shop", farmerId = farmer, commodityId = "wheat", entryDate = date,
        bagsCount = 1L, weightPerBagGrams = 50_000L, extraWeightGrams = 0L,
        totalWeightGrams = 50_000L, ratePaisaPerQuintal = 200_000L, commissionBps = 0L,
        labourPerBagPaisa = 0L, advancePaidPaisa = 0L, grossPaisa = 100_000L,
        commissionPaisa = 0L, labourPaisa = 0L, netPayablePaisa = 100_000L,
        createdAt = date, updatedAt = date,
    )

    private fun sell(id: String, buyer: String, date: Long = 1L) = SellEntry(
        id = id, shopId = "shop", buyerId = buyer, commodityId = "wheat", entryDate = date,
        billMode = BillMode.NORMAL, partyKind = SellPartyKind.MERCHANT, bagsCount = 1L,
        weightPerBagGrams = 50_000L, totalWeightGrams = 50_000L, ratePaisaPerQuintal = 200_000L,
        aratBps = 0L, tulakPaisa = 0L, grossPaisa = 100_000L, aratPaisa = 0L,
        mandiShulkPaisa = 0L, totalReceivablePaisa = 100_000L, receivedPaisa = 0L,
        balanceDuePaisa = 100_000L, createdAt = date, updatedAt = date,
    )

    private fun lend(id: String, date: Long) = LendEntry(
        id = id, shopId = "shop", partyId = "borrower", lendType = LendType.MONEY,
        entryDate = date, amountPaisa = 100_000L, createdAt = date, updatedAt = date,
    )

    @Test
    fun farmerSeriesIsPerFarmerAndVoidsNeverReuseNumbers() = runTest {
        val repo = OfflineFirstBuyRepository(createTestDatabase())
        repo.saveBuyEntry(buy("a1", "farmer-a"))
        repo.saveBuyEntry(buy("a2", "farmer-a"))
        repo.saveBuyEntry(buy("b1", "farmer-b"))
        repo.voidBuyEntry("a2", VoidReason.WRONG_ENTRY)
        repo.saveBuyEntry(buy("a3", "farmer-a"))

        assertEquals(1L, repo.getBuyEntryById("a1")?.slipNo)
        assertEquals(2L, repo.getBuyEntryById("a2")?.slipNo)
        assertEquals(1L, repo.getBuyEntryById("b1")?.slipNo)
        assertEquals(3L, repo.getBuyEntryById("a3")?.slipNo)
    }

    @Test
    fun sellSeriesIsSharedAcrossBuyersAndEditKeepsNumber() = runTest {
        val repo = OfflineFirstSellRepository(createTestDatabase())
        repo.saveSellEntry(sell("s1", "buyer-a"))
        repo.saveSellEntry(sell("s2", "buyer-b"))
        repo.saveSellEntry(sell("s3", "buyer-a"))
        repo.editSellEntry(sell("s2", "buyer-b").copy(receivedPaisa = 10L))

        assertEquals(listOf(1L, 2L, 3L), listOf("s1", "s2", "s3").map { repo.getSellEntryById(it)?.slipNo })
        assertEquals(2L, repo.getSellEntryById("s2")?.slipNo)
    }

    @Test
    fun lendSeriesContinuesAcrossFirstApril() = runTest {
        val repo = OfflineFirstLendRepository(createTestDatabase())
        repo.saveLendEntry(lend("l1", 1_743_361_200_000L))
        repo.saveLendEntry(lend("l2", 1_743_447_600_000L))

        assertEquals(1L, repo.getLendEntryById("l1")?.slipNo)
        assertEquals(2L, repo.getLendEntryById("l2")?.slipNo)
    }
}
