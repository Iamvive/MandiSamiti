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
}
