package com.appwork.mandisamiti.ui.home

import app.cash.turbine.test
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
import com.appwork.mandisamiti.ui.deal.DealEntryViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @Test
    fun testHomeViewModelSearchAndFilter() = runTest {
        val database = createTestDatabase()
        val shopRepo = OfflineFirstShopProfileRepository(database)
        val partyRepo = OfflineFirstPartyRepository(database)

        val shopId = "shop-1"
        shopRepo.saveShopProfile(
            ShopProfile(
                id = shopId,
                shopName = "श्री गणेश ट्रेडिंग",
                ownerName = "लाला जी",
                mandiName = "मथुरा मंडी",
                phoneNumber = "9837000000",
                pinHash = "1234",
                isSoundEnabled = true,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        partyRepo.saveParty(
            Party(
                id = "p-1",
                shopId = shopId,
                name = "रामवीर सिंह",
                village = "राया",
                partyType = PartyType.FARMER,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        partyRepo.saveParty(
            Party(
                id = "p-2",
                shopId = shopId,
                name = "महेन्द्र प्रधान",
                village = "गोवर्धन",
                partyType = PartyType.FARMER,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val viewModel = HomeViewModel(
            shopProfileRepository = shopRepo,
            partyRepository = partyRepo,
            viewModelScope = backgroundScope
        )

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.allParties.size < 2 || state.shopProfile == null) {
                state = awaitItem()
            }
            assertNotNull(state.shopProfile)
            assertEquals("श्री गणेश ट्रेडिंग", state.shopProfile?.shopName)
            assertEquals(2, state.allParties.size)

            // Test Search by village
            viewModel.onSearchQueryChanged("गोवर्धन")
            val filteredState = awaitItem()
            assertEquals(1, filteredState.filteredParties.size)
            assertEquals("महेन्द्र प्रधान", filteredState.filteredParties[0].party.name)

            // Clear search
            viewModel.onSearchQueryChanged("")
            val resetState = awaitItem()
            assertEquals(2, resetState.filteredParties.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testAddNewPartyFromHomeViewModel() = runTest {
        val database = createTestDatabase()
        val shopRepo = OfflineFirstShopProfileRepository(database)
        val partyRepo = OfflineFirstPartyRepository(database)

        val shopId = "shop-add-party"
        shopRepo.saveShopProfile(
            ShopProfile(
                id = shopId,
                shopName = "कृष्णा आढ़त",
                ownerName = "राधे श्याम",
                mandiName = "कोसी कलां",
                phoneNumber = "9837000000",
                pinHash = "1234",
                isSoundEnabled = true,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val viewModel = HomeViewModel(
            shopProfileRepository = shopRepo,
            partyRepository = partyRepo,
            viewModelScope = backgroundScope
        )

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.shopProfile == null) {
                state = awaitItem()
            }

            viewModel.addNewParty(
                name = "कमलेश गुर्जर",
                phone = "9837112233",
                village = "बरसाना",
                partyType = PartyType.FARMER,
                monthlyInterestRate = 1.5
            )

            while (state.allParties.none { it.party.name == "कमलेश गुर्जर" }) {
                state = awaitItem()
            }
            val added = state.allParties.first { it.party.name == "कमलेश गुर्जर" }
            assertEquals("कमलेश गुर्जर", added.party.name)
            assertEquals("9837112233", added.party.phone)
            assertEquals("बरसाना", added.party.village)
            assertEquals(PartyType.FARMER, added.party.partyType)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testRealtimeBalanceUpdateOnNewDealAndCashTxn() = runTest {
        val database = createTestDatabase()
        val shopRepo = OfflineFirstShopProfileRepository(database)
        val partyRepo = OfflineFirstPartyRepository(database)
        val dealRepo = OfflineFirstDealRepository(database)
        val cashRepo = OfflineFirstCashTransactionRepository(database)

        val shopId = "shop-live-update"
        val farmerId = "farmer-live-1"
        val buyerId = "buyer-live-1"

        shopRepo.saveShopProfile(
            ShopProfile(
                id = shopId,
                shopName = "श्री गणेश ट्रेडिंग",
                ownerName = "लाला जी",
                mandiName = "मथुरा मंडी",
                phoneNumber = "9837000000",
                pinHash = "1234",
                isSoundEnabled = true,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        partyRepo.saveParty(
            Party(
                id = farmerId,
                shopId = shopId,
                name = "रामेश्वर किसान",
                village = "राया",
                partyType = PartyType.FARMER,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        partyRepo.saveParty(
            Party(
                id = buyerId,
                shopId = shopId,
                name = "अग्रवाल ट्रेडर्स",
                village = "मथुरा",
                partyType = PartyType.BUYER,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val viewModel = HomeViewModel(
            shopProfileRepository = shopRepo,
            partyRepository = partyRepo,
            viewModelScope = backgroundScope
        )

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.allParties.size < 2) {
                state = awaitItem()
            }
            assertEquals(0L, state.totalMarketReceivablePaisa)
            assertEquals(0L, state.totalFarmerPayablePaisa)

            // Add a settled deal: Farmer payable ₹44,222.50 (4,422,250 paisa), Buyer receivable ₹45,500.00 (4,550,000 paisa)
            dealRepo.saveDeal(
                Deal(
                    id = "deal-live-1",
                    shopId = shopId,
                    farmerId = farmerId,
                    buyerId = buyerId,
                    commodityId = "comm_wheat",
                    dealStatus = DealStatus.SETTLED,
                    dealDate = 2000L,
                    bagsCount = 35,
                    grossWeightGrams = 1_840_000L,
                    cutWeightGrams = 35_000L,
                    netWeightGrams = 1_805_000L,
                    ratePaisaPerUnit = 245_000L,
                    grossAmountPaisa = 4_422_250L,
                    farmerCommissionPaisa = 66_333L,
                    labourChargePaisa = 15_000L,
                    netFarmerPayablePaisa = 4_340_917L,
                    buyerCommissionPaisa = 88_445L,
                    netBuyerReceivablePaisa = 4_554_917L,
                    createdAt = 2000L,
                    updatedAt = 2000L
                )
            )

            // Verify HomeViewModel UI state automatically updates balances without manual reload
            while (state.totalMarketReceivablePaisa == 0L || state.totalFarmerPayablePaisa == 0L) {
                state = awaitItem()
            }
            assertEquals(4_554_917L, state.totalMarketReceivablePaisa)
            assertEquals(4_340_917L, state.totalFarmerPayablePaisa)

            // Now add cash payment to farmer: Shop gives payment / Jama
            cashRepo.recordTransaction(
                CashTransaction(
                    id = "cash-live-1",
                    shopId = shopId,
                    partyId = farmerId,
                    dealId = "deal-live-1",
                    transactionType = TransactionType.JAMA_RECEIVED, // reducing shop liability / farmer jama
                    amountPaisa = 1_000_000L,
                    paymentMode = PaymentMode.CASH,
                    transactionDate = 3000L,
                    createdAt = 3000L,
                    updatedAt = 3000L
                )
            )

            // Balance updates reactively
            while (state.totalFarmerPayablePaisa != (4_340_917L + 1_000_000L)) {
                state = awaitItem()
            }
            assertEquals(5_340_917L, state.totalFarmerPayablePaisa)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

