package com.appwork.mandisamiti.ui.home

import app.cash.turbine.test
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.ShopProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @Test
    fun testHomeViewModelSearchAndFilter() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler) // repos and VM share the test scheduler: no real threads
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)

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
            shopId = shopId,
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

    /** BUG-1/BUG-2: the khata list follows the session shop id, not whichever profile row comes first. */
    @Test
    fun partiesComeFromSessionShopNotFromFirstProfileRow() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)

        // A leftover demo profile is the only profile row; the session belongs to the server shop.
        shopRepo.saveShopProfile(
            ShopProfile(
                id = "legacy-demo", shopName = "Demo", ownerName = "O", mandiName = "M",
                phoneNumber = "9", pinHash = "", createdAt = 1L, updatedAt = 1L
            )
        )
        partyRepo.saveParty(
            Party(id = "p-legacy", shopId = "legacy-demo", name = "पुराना", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L)
        )
        partyRepo.saveParty(
            Party(id = "p-server", shopId = "srv-shop", name = "रामवीर", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L)
        )

        val viewModel = HomeViewModel(
            shopId = "srv-shop",
            shopProfileRepository = shopRepo,
            partyRepository = partyRepo,
            viewModelScope = backgroundScope
        )

        val state = viewModel.uiState.first { !it.isLoading && it.allParties.isNotEmpty() }
        assertEquals(listOf("p-server"), state.allParties.map { it.party.id })
    }

    @Test
    fun createPartySavesAndReturnsNewParty() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)

        val viewModel = HomeViewModel(
            shopId = "srv-shop",
            shopProfileRepository = shopRepo,
            partyRepository = partyRepo,
            viewModelScope = backgroundScope
        )

        val createdParty = viewModel.createParty(
            name = "सुरेश कुमार",
            village = "भरतपुर",
            phoneNumber = "9898989898",
            partyType = PartyType.FARMER
        )

        assertNotNull(createdParty)
        assertEquals("सुरेश कुमार", createdParty.name)
        assertEquals("भरतपुर", createdParty.village)
        assertEquals("9898989898", createdParty.phone)
        assertEquals(PartyType.FARMER, createdParty.partyType)
        assertEquals("srv-shop", createdParty.shopId)

        val inDb = partyRepo.getPartyById(createdParty.id)
        assertNotNull(inDb)
        assertEquals("सुरेश कुमार", inDb.name)
    }

    @Test
    fun createPartyAsksForASync() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        var writes = 0

        val viewModel = HomeViewModel(
            shopId = "srv-shop",
            shopProfileRepository = shopRepo,
            partyRepository = partyRepo,
            viewModelScope = backgroundScope,
            onLocalWrite = { writes++ }
        )

        viewModel.createParty("रामवीर", null, null, PartyType.FARMER)

        assertEquals(1, writes)
    }

    @Test
    fun createPartyRefusesAnInvalidPhoneAndSavesNothing() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val viewModel = HomeViewModel(
            shopId = "srv-shop",
            shopProfileRepository = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher),
            partyRepository = partyRepo,
            viewModelScope = backgroundScope
        )

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            viewModel.createParty("रामवीर", null, "98765432101234567", PartyType.FARMER)
        }
        assertEquals(0, database.appDatabaseQueries.getAllParties("srv-shop").executeAsList().size)
    }

    @Test
    fun testLanguageSettingPersistence() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val tradeRepo = com.appwork.mandisamiti.data.repository.OfflineFirstTradeSettingsRepository(database, ioDispatcher = ioDispatcher)
        var syncTriggered = 0

        val viewModel = HomeViewModel(
            shopId = "shop-1",
            shopProfileRepository = shopRepo,
            partyRepository = partyRepo,
            tradeSettingsRepository = tradeRepo,
            viewModelScope = backgroundScope,
            onLocalWrite = { syncTriggered++ }
        )

        // Initial language is Hindi (false)
        assertEquals(false, viewModel.uiState.value.isEnglish)

        // Change language to English
        viewModel.setLanguage(true)
        testScheduler.runCurrent()
        assertEquals(true, viewModel.uiState.value.isEnglish)
        assertEquals(1, syncTriggered)

        // Verify persisted to DB and reloadable
        val persistedSettings = tradeRepo.getTradeSettings()
        assertEquals(true, persistedSettings.isEnglish)

        // Fresh ViewModel reloads English from repository
        val viewModel2 = HomeViewModel(
            shopId = "shop-1",
            shopProfileRepository = shopRepo,
            partyRepository = partyRepo,
            tradeSettingsRepository = tradeRepo,
            viewModelScope = backgroundScope
        )

        viewModel2.uiState.first { it.isEnglish }
        assertEquals(true, viewModel2.uiState.value.isEnglish)
    }

    @Test
    fun testHomeViewModelObservesKpisAndStockLines() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val buyRepo = com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository(database, ioDispatcher = ioDispatcher)
        val sellRepo = com.appwork.mandisamiti.data.repository.OfflineFirstSellRepository(database, ioDispatcher = ioDispatcher)
        val lendRepo = com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository(database, ioDispatcher = ioDispatcher)
        val expenseRepo = com.appwork.mandisamiti.data.repository.OfflineFirstExpenseRepository(database, ioDispatcher = ioDispatcher)
        val stockUseCase = com.appwork.mandisamiti.domain.usecase.StockInventoryUseCase(expenseRepo)

        val shopId = "test-shop"
        val viewModel = HomeViewModel(
            shopId = shopId,
            shopProfileRepository = shopRepo,
            partyRepository = partyRepo,
            buyRepository = buyRepo,
            sellRepository = sellRepo,
            lendRepository = lendRepo,
            expenseRepository = expenseRepo,
            stockUseCase = stockUseCase,
            viewModelScope = backgroundScope
        )

        // Save a buy entry: 10 quintals of Wheat
        buyRepo.saveBuyEntry(
            com.appwork.mandisamiti.domain.model.BuyEntry(
                id = "b-1",
                shopId = shopId,
                farmerId = "f-1",
                commodityId = "Wheat",
                entryDate = 1730000000000L,
                bagsCount = 20L,
                weightPerBagGrams = 50_000L,
                extraWeightGrams = 0L,
                totalWeightGrams = 1_000_000L,
                ratePaisaPerQuintal = 250_000L,
                commissionBps = 0L,
                labourPerBagPaisa = 0L,
                advancePaidPaisa = 0L,
                grossPaisa = 25_000_00L,
                commissionPaisa = 0L,
                labourPaisa = 0L,
                netPayablePaisa = 25_000_00L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        // Save a sell entry: 5 quintals of Wheat
        sellRepo.saveSellEntry(
            com.appwork.mandisamiti.domain.model.SellEntry(
                id = "s-1",
                shopId = shopId,
                buyerId = "m-1",
                commodityId = "Wheat",
                entryDate = 1730000000000L,
                billMode = com.appwork.mandisamiti.domain.model.BillMode.NORMAL,
                partyKind = com.appwork.mandisamiti.domain.model.SellPartyKind.CUSTOMER,
                bagsCount = 10L,
                weightPerBagGrams = 50_000L,
                totalWeightGrams = 500_000L,
                ratePaisaPerQuintal = 300_000L,
                aratBps = 0L,
                tulakPaisa = 0L,
                grossPaisa = 15_000_00L,
                aratPaisa = 0L,
                mandiShulkPaisa = 0L,
                totalReceivablePaisa = 15_000_00L,
                receivedPaisa = 0L,
                balanceDuePaisa = 15_000_00L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val stateWithKpis = viewModel.uiState.first { it.kpis.totalBuyPaisa > 0 && it.kpis.totalSellPaisa > 0 }
        assertEquals(25_000_00L, stateWithKpis.kpis.totalBuyPaisa)
        assertEquals(15_000_00L, stateWithKpis.kpis.totalSellPaisa)
        assertEquals(-10_000_00L, stateWithKpis.kpis.profitLossPaisa)

        // Verify stock line for Wheat: bought 10 qtl (1M g), sold 5 qtl (500k g), onHand 5 qtl (500k g)
        val wheatLine = stateWithKpis.stockLines.firstOrNull { it.commodityId == "Wheat" }
        assertNotNull(wheatLine)
        assertEquals(1_000_000L, wheatLine.boughtGrams)
        assertEquals(500_000L, wheatLine.soldGrams)
        assertEquals(500_000L, wheatLine.onHandGrams)
    }

    @Test
    fun testLendEntryReflectedInReceivablesAndHonestSubtitleWhenUnlinked() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val lendRepo = com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository(database, ioDispatcher = ioDispatcher)

        val shopId = "test-shop"
        val viewModel = HomeViewModel(
            shopId = shopId,
            shopProfileRepository = shopRepo,
            partyRepository = partyRepo,
            lendRepository = lendRepo,
            viewModelScope = backgroundScope
        )

        // Save 1 money lend entry for ₹10,000 without any registered party in partyRepo
        val lendAmountPaisa = 1_000_000L // ₹10,000
        lendRepo.saveLendEntry(
            com.appwork.mandisamiti.domain.model.LendEntry(
                id = "lend-1",
                shopId = shopId,
                partyId = "Kailash Farmer", // Unlinked name
                lendType = com.appwork.mandisamiti.domain.model.LendType.MONEY,
                entryDate = 1730000000000L,
                amountPaisa = lendAmountPaisa,
                status = com.appwork.mandisamiti.domain.model.LendStatus.OPEN,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val state = viewModel.uiState.first { it.kpis.totalLendPaisa == lendAmountPaisa }
        assertEquals(lendAmountPaisa, state.kpis.totalLendPaisa)
        assertEquals(lendAmountPaisa, state.totalMarketReceivablePaisa)
        assertEquals(lendAmountPaisa, state.unlinkedLendAmountPaisa)
        assertEquals(1, state.unlinkedLendCount)
    }
}
