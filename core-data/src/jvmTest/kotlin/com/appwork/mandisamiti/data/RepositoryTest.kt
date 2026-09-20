package com.appwork.mandisamiti.data

import app.cash.turbine.test
import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstDealRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.data.sync.SyncEngine
import com.appwork.mandisamiti.database.DriverFactory
import com.appwork.mandisamiti.database.createDatabase
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.DealStatus
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.TransactionType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RepositoryTest {

    @Test
    fun testRepositoriesReactiveFlowsAndSyncEngine() = runTest {
        val database = createDatabase(DriverFactory())
        val partyRepo = OfflineFirstPartyRepository(database)
        val dealRepo = OfflineFirstDealRepository(database)
        val cashRepo = OfflineFirstCashTransactionRepository(database)
        val shopRepo = OfflineFirstShopProfileRepository(database)
        val syncEngine = SyncEngine(database)

        val shopId = "shop-1"
        val farmerId = "farmer-1"
        val buyerId = "buyer-1"
        val commodityId = "comm-1"

        // 1. Save Shop Profile
        shopRepo.saveShopProfile(
            ShopProfile(
                id = shopId,
                shopName = "श्री गणेश ट्रेडिंग",
                ownerName = "लाला जी",
                mandiName = "मथुरा मंडी",
                phoneNumber = "9837000000",
                pinHash = "1234",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        shopRepo.getShopProfileStream().test {
            val profile = awaitItem()
            assertNotNull(profile)
            assertEquals("श्री गणेश ट्रेडिंग", profile.shopName)
        }

        // 2. Save Farmer
        val farmer = Party(
            id = farmerId,
            shopId = shopId,
            name = "रामवीर सिंह",
            village = "राया",
            partyType = PartyType.FARMER,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        partyRepo.saveParty(farmer)

        partyRepo.getPartiesStream(shopId).test {
            val parties = awaitItem()
            assertEquals(1, parties.size)
            assertEquals("रामवीर सिंह", parties[0].name)
        }

        // 3. Save Deal
        val deal = Deal(
            id = "deal-1",
            shopId = shopId,
            farmerId = farmerId,
            buyerId = buyerId,
            commodityId = commodityId,
            dealStatus = DealStatus.SETTLED,
            dealDate = 1000L,
            bagsCount = 35,
            grossWeightGrams = 1_840_000L,
            netWeightGrams = 1_805_000L,
            grossAmountPaisa = 9_783_100L,
            netFarmerPayablePaisa = 9_509_000L,
            netBuyerReceivablePaisa = 9_929_800L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        dealRepo.saveDeal(deal)

        partyRepo.getPartyBalanceStream(farmerId).test {
            val balance = awaitItem()
            assertNotNull(balance)
            assertEquals(-9_509_000L, balance.balancePaisa)
        }

        // 4. Record Cash Advance
        val cashTx = CashTransaction(
            id = "tx-1",
            shopId = shopId,
            partyId = farmerId,
            transactionType = TransactionType.UDHAR_GIVEN,
            amountPaisa = 5_000_000L,
            paymentMode = PaymentMode.CASH,
            transactionDate = 1000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        cashRepo.recordTransaction(cashTx)

        partyRepo.getPartyBalanceStream(farmerId).test {
            val updatedBalance = awaitItem()
            assertNotNull(updatedBalance)
            assertEquals(-4_509_000L, updatedBalance.balancePaisa)
        }

        // 5. Verify Sync Engine
        val syncSummary = syncEngine.getPendingSyncSummary()
        assertEquals(1, syncSummary.pendingPartiesCount)
        assertEquals(1, syncSummary.pendingDealsCount)
        assertEquals(1, syncSummary.pendingTransactionsCount)

        syncEngine.markAllBatchSynced(listOf(farmerId), listOf("deal-1"), listOf("tx-1"))
        val cleanSyncSummary = syncEngine.getPendingSyncSummary()
        assertEquals(0, cleanSyncSummary.pendingPartiesCount)
        assertEquals(0, cleanSyncSummary.pendingDealsCount)
        assertEquals(0, cleanSyncSummary.pendingTransactionsCount)
    }
}
