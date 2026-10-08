package com.appwork.mandisamiti.data.auth

import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstDealRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.DealStatus
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.TransactionType
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalDataWiperTest {
    @Test
    fun wipeAllEmptiesEveryTable() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val db = createTestDatabase()
        val q = db.appDatabaseQueries
        OfflineFirstShopProfileRepository(db, io).saveShopProfile(
            ShopProfile(
                id = "shop-1", shopName = "S", ownerName = "O", mandiName = "M",
                phoneNumber = "9837000000", pinHash = "1234", createdAt = 1L, updatedAt = 1L
            )
        )
        OfflineFirstPartyRepository(db, io).saveParty(
            Party(
                id = "farmer-1", shopId = "shop-1", name = "R", village = "V",
                partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L
            )
        )
        q.insertCommodity("comm-1", "shop-1", "गेहूं", "Wheat", "QUINTAL", 1, 1L, 1L, 0, 0)
        OfflineFirstDealRepository(db, io).saveDeal(
            Deal(
                id = "deal-1", shopId = "shop-1", farmerId = "farmer-1", buyerId = "buyer-1",
                commodityId = "comm-1", dealStatus = DealStatus.SETTLED, dealDate = 1L,
                bagsCount = 1, grossWeightGrams = 1L, netWeightGrams = 1L,
                grossAmountPaisa = 1L, netFarmerPayablePaisa = 1L, netBuyerReceivablePaisa = 1L,
                createdAt = 1L, updatedAt = 1L
            )
        )
        OfflineFirstCashTransactionRepository(db, io).recordTransaction(
            CashTransaction(
                id = "tx-1", shopId = "shop-1", partyId = "farmer-1",
                transactionType = TransactionType.UDHAR_GIVEN, amountPaisa = 1L,
                paymentMode = PaymentMode.CASH, transactionDate = 1L, createdAt = 1L, updatedAt = 1L
            )
        )
        assertEquals(1, q.getRevisionsForEntry("deal-1").executeAsList().size)

        LocalDataWiper(db).wipeAll()

        assertNull(q.getShopProfile().executeAsOneOrNull())
        assertEquals(0, q.getAllParties("shop-1").executeAsList().size)
        assertEquals(0, q.getAllCommodities("shop-1").executeAsList().size)
        assertEquals(0, q.getDealsByShop("shop-1").executeAsList().size)
        assertEquals(0, q.getCashTransactionsByShop("shop-1").executeAsList().size)
        assertEquals(0, q.getRevisionsForEntry("deal-1").executeAsList().size)
        assertEquals(0, q.getPendingSyncRevisions().executeAsList().size)
    }

    @Test
    fun inMemorySessionStoreLifecycle() {
        val store = InMemorySessionStore()
        assertNull(store.current())
        store.save(Session("shop-1", "a1", "r1"))
        assertEquals(Session("shop-1", "a1", "r1"), store.current())
        store.updateTokens("a2", "r2")
        assertEquals(Session("shop-1", "a2", "r2"), store.current())
        store.clear()
        assertNull(store.current())
    }
}
