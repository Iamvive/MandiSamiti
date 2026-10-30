package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PartyTaxFieldsTest {
    @Test
    fun buyerTaxFieldsRoundTripWhileFarmerFieldsRemainNull() = runTest {
        val repository = OfflineFirstPartyRepository(createTestDatabase())
        repository.saveParty(
            Party(
                id = "buyer-tax",
                shopId = "shop",
                name = "Buyer Tax",
                partyType = PartyType.BUYER,
                createdAt = 1L,
                updatedAt = 1L,
                gstin = "29ABCDE1234F1Z5",
                mandiLicense = "LIC-001",
            )
        )
        repository.saveParty(
            Party(
                id = "farmer-tax",
                shopId = "shop",
                name = "Farmer Tax",
                partyType = PartyType.FARMER,
                createdAt = 1L,
                updatedAt = 1L,
            )
        )

        val buyer = repository.getPartyById("buyer-tax")!!
        assertEquals("29ABCDE1234F1Z5", buyer.gstin)
        assertEquals("LIC-001", buyer.mandiLicense)

        val farmer = repository.getPartyById("farmer-tax")!!
        assertNull(farmer.gstin)
        assertNull(farmer.mandiLicense)
    }
}
