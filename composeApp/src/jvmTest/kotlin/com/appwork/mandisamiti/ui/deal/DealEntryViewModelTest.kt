package com.appwork.mandisamiti.ui.deal

import app.cash.turbine.test
import com.appwork.mandisamiti.data.repository.OfflineFirstDealRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.DealStatus
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import com.appwork.mandisamiti.ui.components.KeypadAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

    private suspend fun seedShopAndParties(shopRepo: OfflineFirstShopProfileRepository, partyRepo: OfflineFirstPartyRepository) {
        shopRepo.saveShopProfile(
            ShopProfile(
                id = "shop-1", shopName = "श्री गणेश ट्रेडिंग", ownerName = "लाला जी", mandiName = "मथुरा मंडी",
                phoneNumber = "9837000000", pinHash = "1234", createdAt = 1000L, updatedAt = 1000L
            )
        )
        partyRepo.saveParty(Party(id = "farmer-1", shopId = "shop-1", name = "रामवीर सिंह", village = "राया", partyType = PartyType.FARMER, createdAt = 1000L, updatedAt = 1000L))
        partyRepo.saveParty(Party(id = "buyer-1", shopId = "shop-1", name = "अग्रवाल ट्रेडर्स", village = "मथुरा", partyType = PartyType.BUYER, createdAt = 1000L, updatedAt = 1000L))
    }

    private fun existingDeal() = Deal(
        id = "deal-old", shopId = "shop-1", farmerId = "farmer-1", buyerId = "buyer-1", commodityId = "comm_wheat",
        dealStatus = DealStatus.SETTLED, dealDate = 1_000L, bagsCount = 35,
        grossWeightGrams = 1_840_000L, cutWeightGrams = 35_000L, netWeightGrams = 1_805_000L,
        ratePaisaPerUnit = 227_550L, labourChargePaisa = 15_050L, farmerCommissionBps = 150L,
        createdAt = 1_000L, updatedAt = 1_000L
    )

    @Test
    fun editReloadKeepsPaisaInRateLabourAndCommission() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        seedShopAndParties(shopRepo, partyRepo)
        dealRepo.saveDeal(existingDeal())

        val viewModel = DealEntryViewModel(
            shopId = "shop-1", existingDealId = "deal-old", dealRepository = dealRepo,
            partyRepository = partyRepo, shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(), viewModelScope = backgroundScope
        )
        val state = viewModel.uiState.first { it.isEditMode && it.selectedFarmer != null && it.ratePerQuintalText.isNotEmpty() }

        assertEquals("2275.50", state.ratePerQuintalText)
        assertEquals("150.50", state.labourChargesText)
        assertEquals("1.50", state.commissionPercentText)
        assertEquals("18.4", state.grossWeightText)
    }

    @Test
    fun savingAnEditKeepsTheOriginalDealDate() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        seedShopAndParties(shopRepo, partyRepo)
        dealRepo.saveDeal(existingDeal())

        val viewModel = DealEntryViewModel(
            shopId = "shop-1", existingDealId = "deal-old", dealRepository = dealRepo,
            partyRepository = partyRepo, shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(), viewModelScope = backgroundScope
        )
        viewModel.uiState.first { it.isEditMode && it.selectedFarmer != null && it.ratePerQuintalText.isNotEmpty() }

        viewModel.events.test {
            viewModel.saveDeal()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val saved = dealRepo.getDealById("deal-old")!!
        assertEquals(1_000L, saved.dealDate)
        assertEquals(1_000L, saved.createdAt)
        assertEquals(2, saved.revision)
        assertEquals(227_550L, saved.ratePaisaPerUnit)
        assertEquals(1, dealRepo.getDealsByShopStream("shop-1").first().size) // edited, not duplicated
    }

    @Test
    fun newDealCommissionIsExactAndUsesUuid() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        seedShopAndParties(shopRepo, partyRepo)

        val viewModel = DealEntryViewModel(
            shopId = "shop-1", existingDealId = null, dealRepository = dealRepo,
            partyRepository = partyRepo, shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(), viewModelScope = backgroundScope
        )
        viewModel.uiState.first { it.selectedFarmer != null }

        viewModel.onFocusField(ActiveInputField.GROSS_WEIGHT)
        listOf(KeypadAction.DIGIT_1, KeypadAction.DIGIT_8, KeypadAction.DECIMAL, KeypadAction.DIGIT_4)
            .forEach(viewModel::onKeypadAction)
        viewModel.toggleSettlementStage(true)
        viewModel.onFocusField(ActiveInputField.RATE_PER_QUINTAL)
        listOf(KeypadAction.DIGIT_5, KeypadAction.DIGIT_4, KeypadAction.DIGIT_2, KeypadAction.DIGIT_0)
            .forEach(viewModel::onKeypadAction)

        // net 18.05 qtl x ₹5,420 = ₹97,831.00; 1.5% = ₹1,467.465 -> ₹1,467.47; labour ₹150
        val state = viewModel.uiState.value
        assertEquals(9_783_100L, state.grossAmountPaisa)
        assertEquals(9_783_100L - 146_747L - 15_000L, state.netFarmerPayablePaisa)

        viewModel.events.test {
            viewModel.saveDeal()
            val deal = (awaitItem() as DealEntryEvent.DealSavedSuccess).deal
            assertEquals(146_747L, deal.farmerCommissionPaisa)
            assertEquals(150L, deal.farmerCommissionBps)
            assertEquals(36, deal.id.length) // UUID, not "deal_<time>_<rand>"
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun doubleTapSaveCreatesExactlyOneDeal() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        seedShopAndParties(shopRepo, partyRepo)

        val viewModel = DealEntryViewModel(
            shopId = "shop-1", existingDealId = null, dealRepository = dealRepo,
            partyRepository = partyRepo, shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(), viewModelScope = backgroundScope
        )
        viewModel.uiState.first { it.selectedFarmer != null }
        viewModel.onFocusField(ActiveInputField.GROSS_WEIGHT)
        listOf(KeypadAction.DIGIT_1, KeypadAction.DIGIT_8).forEach(viewModel::onKeypadAction)

        viewModel.saveDeal()
        viewModel.saveDeal()
        testScheduler.runCurrent() // runs the queued backgroundScope work (advanceUntilIdle skips background tasks)

        assertEquals(1, dealRepo.getDealsByShopStream("shop-1").first().size)
    }

    @Test
    fun saveInEditModeBeforeDealLoadsDoesNotCreateANewDeal() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        seedShopAndParties(shopRepo, partyRepo)
        dealRepo.saveDeal(existingDeal())
        val farmer = partyRepo.getPartyById("farmer-1")!!

        val viewModel = DealEntryViewModel(
            shopId = "shop-1", existingDealId = "deal-old", dealRepository = dealRepo,
            partyRepository = partyRepo, shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(), viewModelScope = backgroundScope
        )
        // The load coroutine is queued but has not run: originalDeal is still null.
        viewModel.onSelectFarmer(farmer)
        viewModel.onFocusField(ActiveInputField.GROSS_WEIGHT)
        listOf(KeypadAction.DIGIT_1, KeypadAction.DIGIT_8).forEach(viewModel::onKeypadAction)
        viewModel.saveDeal()
        testScheduler.runCurrent() // runs the queued backgroundScope work (advanceUntilIdle skips background tasks)

        val deals = dealRepo.getDealsByShopStream("shop-1").first()
        assertEquals(listOf("deal-old"), deals.map { it.id })
        assertEquals(1, deals.single().revision)
    }

    @Test
    fun editSaveKeepsFieldsTheScreenDoesNotHold() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        seedShopAndParties(shopRepo, partyRepo)
        dealRepo.saveDeal(
            existingDeal().copy(
                remarks = "गीला माल", voiceNoteUri = "voice://1", buyerCommissionPaisa = 7_000L,
                weighingChargePaisa = 5_000L, otherDeductionsPaisa = 3_000L
            )
        )

        val viewModel = DealEntryViewModel(
            shopId = "shop-1", existingDealId = "deal-old", dealRepository = dealRepo,
            partyRepository = partyRepo, shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(), viewModelScope = backgroundScope
        )
        viewModel.uiState.first { it.isEditMode && it.selectedFarmer != null && it.ratePerQuintalText.isNotEmpty() }

        viewModel.events.test {
            viewModel.saveDeal()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val saved = dealRepo.getDealById("deal-old")!!
        assertEquals("गीला माल", saved.remarks)
        assertEquals("voice://1", saved.voiceNoteUri)
        assertEquals(7_000L, saved.buyerCommissionPaisa)
        assertEquals(5_000L, saved.weighingChargePaisa)
        assertEquals(3_000L, saved.otherDeductionsPaisa)
    }

    @Test
    fun keypadCapsDecimalsPerFieldAndTotalLength() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        seedShopAndParties(shopRepo, partyRepo)
        val viewModel = DealEntryViewModel(
            shopId = "shop-1", existingDealId = null, dealRepository = dealRepo,
            partyRepository = partyRepo, shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(), viewModelScope = backgroundScope
        )
        viewModel.uiState.first { it.selectedFarmer != null }

        fun type(field: ActiveInputField, vararg keys: KeypadAction) {
            viewModel.onFocusField(field)
            viewModel.onKeypadClear()
            keys.forEach(viewModel::onKeypadAction)
        }
        val d = KeypadAction.DECIMAL
        val k1 = KeypadAction.DIGIT_1
        val k5 = KeypadAction.DIGIT_5

        type(ActiveInputField.COMMISSION_PERCENT, k1, d, k5, k5, k5)
        assertEquals("1.55", viewModel.uiState.value.commissionPercentText)

        type(ActiveInputField.GROSS_WEIGHT, k1, d, k5, k5, k5, k5, k5, k5, k5)
        assertEquals("1.55555", viewModel.uiState.value.grossWeightText) // 5 decimals max for weight

        type(ActiveInputField.BAGS_COUNT, k1, d, k5)
        assertEquals("15", viewModel.uiState.value.bagsCountText) // bags take no decimal point

        type(ActiveInputField.RATE_PER_QUINTAL, k1, k1, k1, k1, k1, k1, k1, k1, k1, k1, k1)
        assertEquals("111111111", viewModel.uiState.value.ratePerQuintalText) // 9 characters max
    }
}
