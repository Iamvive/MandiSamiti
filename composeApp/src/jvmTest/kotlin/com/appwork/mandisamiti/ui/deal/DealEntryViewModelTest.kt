package com.appwork.mandisamiti.ui.deal

import app.cash.turbine.test
import com.appwork.mandisamiti.data.repository.OfflineFirstDealRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.DealStatus
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import com.appwork.mandisamiti.ui.components.KeypadAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DealEntryViewModelTest {

    @Test
    fun testTwoStageDealCalculationAndKeypadEntry() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler) // repos and VM share the test scheduler: no real threads
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        val ttsManager = SoundboxTtsManager()

        val shopId = "shop-1"
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
            id = "farmer-1",
            shopId = shopId,
            name = "रामवीर सिंह",
            village = "राया",
            partyType = PartyType.FARMER,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        partyRepo.saveParty(farmer)

        val buyer = Party(
            id = "buyer-1",
            shopId = shopId,
            name = "अग्रवाल ट्रेडर्स",
            village = "मथुरा",
            partyType = PartyType.BUYER,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        partyRepo.saveParty(buyer)

        val viewModel = DealEntryViewModel(
            shopId = shopId,
            existingDealId = null,
            dealRepository = dealRepo,
            partyRepository = partyRepo,
            shopProfileRepository = shopRepo,
            ttsManager = ttsManager,
            viewModelScope = backgroundScope
        )

        // Select farmer & buyer
        viewModel.onSelectFarmer(farmer)
        viewModel.onSelectBuyer(buyer)

        // Enter Gross Weight: 18.40 Quintals via keypad
        viewModel.onFocusField(ActiveInputField.GROSS_WEIGHT)
        viewModel.onKeypadAction(KeypadAction.DIGIT_1)
        viewModel.onKeypadAction(KeypadAction.DIGIT_8)
        viewModel.onKeypadAction(KeypadAction.DECIMAL)
        viewModel.onKeypadAction(KeypadAction.DIGIT_4)
        viewModel.onKeypadAction(KeypadAction.DIGIT_0)

        // Enter Bags Count: 35
        viewModel.onFocusField(ActiveInputField.BAGS_COUNT)
        viewModel.onKeypadAction(KeypadAction.DIGIT_3)
        viewModel.onKeypadAction(KeypadAction.DIGIT_5)

        // Switch to Stage 2 (Settlement)
        viewModel.toggleSettlementStage(true)

        // Enter Rate: 2450 Rs/Quintal
        viewModel.onFocusField(ActiveInputField.RATE_PER_QUINTAL)
        viewModel.onKeypadAction(KeypadAction.DIGIT_2)
        viewModel.onKeypadAction(KeypadAction.DIGIT_4)
        viewModel.onKeypadAction(KeypadAction.DIGIT_5)
        viewModel.onKeypadAction(KeypadAction.DIGIT_0)

        val state = viewModel.uiState.value
        assertEquals("18.05", state.netWeightQuintals) // 18.40 - 0.35 = 18.05 Q
        assertTrue(state.netFarmerPayablePaisa > 0L)

        // Save Deal & verify Event
        viewModel.events.test {
            viewModel.saveDeal()
            val event = awaitItem()
            assertTrue(event is DealEntryEvent.DealSavedSuccess)
            assertEquals(DealStatus.SETTLED, (event as DealEntryEvent.DealSavedSuccess).deal.dealStatus)
            assertTrue((event as DealEntryEvent.DealSavedSuccess).ttsSpeechText.contains("रामवीर सिंह"))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
