package com.appwork.mandisamiti.ui.register

import app.cash.turbine.test
import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DailyRegisterViewModelTest {

    @Test
    fun testDailyRegisterCashDrawerReconciliation() = runTest {
        val database = createTestDatabase()
        val shopRepo = OfflineFirstShopProfileRepository(database)
        val partyRepo = OfflineFirstPartyRepository(database)
        val cashRepo = OfflineFirstCashTransactionRepository(database)
        val ttsManager = SoundboxTtsManager()

        val shopId = "shop-1"
        val partyId = "farmer-1"

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

        val farmer = Party(
            id = partyId,
            shopId = shopId,
            name = "रामवीर सिंह",
            village = "राया",
            partyType = PartyType.FARMER,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        partyRepo.saveParty(farmer)

        val viewModel = DailyRegisterViewModel(
            shopId = shopId,
            cashRepository = cashRepo,
            partyRepository = partyRepo,
            shopProfileRepository = shopRepo,
            ttsManager = ttsManager,
            viewModelScope = backgroundScope
        )

        // 1. Record Cash Received (JAMA): ₹20,000
        viewModel.recordDailyEntry(
            partyId = partyId,
            transactionType = TransactionType.JAMA_RECEIVED,
            amountRs = 20000L,
            remarks = "बिक्री जमा"
        )

        // 2. Record Cash Advance (UDHAR): ₹5,000
        viewModel.recordDailyEntry(
            partyId = partyId,
            transactionType = TransactionType.UDHAR_GIVEN,
            amountRs = 5000L,
            remarks = "खाद पेशगी"
        )

        // Check cash register summary via uiState
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.todayTransactions.size < 2) {
                state = awaitItem()
            }
            assertEquals(2000000L, state.todayCashInPaisa) // ₹20,000
            assertEquals(500000L, state.todayCashOutPaisa)  // ₹5,000
            assertEquals(1500000L, state.inHandCashDrawerPaisa) // ₹15,000 net in drawer
            cancelAndIgnoreRemainingEvents()
        }
    }
}
