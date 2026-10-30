package com.appwork.mandisamiti.ui.forms

import com.appwork.mandisamiti.ui.buy.BuyEntryFormState
import com.appwork.mandisamiti.ui.sell.SellEntryFormState
import kotlin.test.Test
import kotlin.test.assertEquals

class PartySelectionIdTest {
    @Test
    fun buyUsesSelectedPartyIdInsteadOfDisplayName() {
        val form = BuyEntryFormState().apply {
            farmerName = "Same Name"
            selectedFarmerId = "farmer-village-a"
            bags = "1"
            weightPerBagKg = "50"
            ratePerQuintal = "2000"
        }

        assertEquals("farmer-village-a", form.toBuyEntry("shop").farmerId)
    }

    @Test
    fun sellUsesSelectedPartyIdInsteadOfDisplayName() {
        val form = SellEntryFormState().apply {
            buyerName = "Same Name"
            selectedBuyerId = "buyer-village-b"
            lots[0].bags = "1"
            lots[0].weightPerBagKg = "50"
            lots[0].ratePerQuintal = "2000"
        }

        assertEquals("buyer-village-b", form.toSellEntry(shopId = "shop").buyerId)
    }
}
