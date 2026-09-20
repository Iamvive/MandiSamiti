package com.appwork.mandisamiti.ui.home

import app.cash.turbine.test
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.ShopProfile
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
}
