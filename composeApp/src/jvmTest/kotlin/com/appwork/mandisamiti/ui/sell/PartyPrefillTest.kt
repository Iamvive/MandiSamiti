package com.appwork.mandisamiti.ui.sell

import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.ui.buy.BuyEntryFormState
import com.appwork.mandisamiti.ui.buy.BuyPartyResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PartyPrefillTest {
    private val farmer = Party(
        id = "f1",
        shopId = "shop",
        name = "राम किसान",
        phone = "9876543210",
        village = "उज्जैन",
        partyType = PartyType.FARMER,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private val buyer = Party(
        id = "b1",
        shopId = "shop",
        name = "श्याम ट्रेडर्स",
        phone = "9123456780",
        village = "देवास",
        partyType = PartyType.BUYER,
        gstin = "23AABCS1429B1Z5",
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun kisaanPrefillSetsMobileBasicFieldsAndStartsClean() {
        val form = BuyEntryFormState()
        BuyPartyResolver.pick(form, farmer)

        assertEquals("f1", form.selectedFarmerId)
        assertEquals("राम किसान", form.farmerName)
        assertEquals("9876543210", form.phone)
        assertEquals("उज्जैन", form.village)
        assertFalse(form.hasUnsavedChanges(prefilledParty = farmer))

        form.farmerName = "राम प्रसाद"
        assertTrue(form.hasUnsavedChanges(prefilledParty = farmer))
    }

    @Test
    fun vyapariPrefillSetsMobileBasicFieldsMerchantKindGstinAndStateCode() {
        val form = SellEntryFormState()
        SellPartyResolver.pick(form, buyer)

        assertEquals("b1", form.selectedBuyerId)
        assertEquals("श्याम ट्रेडर्स", form.buyerName)
        assertEquals("9123456780", form.phone)
        assertEquals("देवास", form.village)
        assertEquals("23AABCS1429B1Z5", form.gstin)
        assertEquals("23", form.stateCode)
        assertEquals(SellPartyKind.MERCHANT, form.partyKind)
        assertFalse(form.hasUnsavedChanges(prefilledParty = buyer))

        form.buyerName = "श्याम एंड सन्स"
        assertTrue(form.hasUnsavedChanges(prefilledParty = buyer))
    }
}
