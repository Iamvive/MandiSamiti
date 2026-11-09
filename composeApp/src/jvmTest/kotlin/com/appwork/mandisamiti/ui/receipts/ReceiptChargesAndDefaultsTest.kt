package com.appwork.mandisamiti.ui.receipts

import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.TradeSettings
import com.appwork.mandisamiti.ui.buy.BuyEntryFormState
import com.appwork.mandisamiti.ui.sell.SellEntryFormState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ReceiptChargesAndDefaultsTest {

    @Test
    fun buyForm_seedsFromSavedShopTradeSettings() {
        val settings = TradeSettings(
            farmerCommissionBps = 150L,   // 1.5%
            buyerCommissionBps = 200L,    // 2.0%
            defaultLabourPaisa = 15000L   // ₹150
        )

        val form = BuyEntryFormState(settings)

        assertEquals("1.50", form.commissionPercent)
        assertEquals("150", form.labourPerBag)
    }

    @Test
    fun sellForm_seedsFromSavedShopTradeSettings() {
        val settings = TradeSettings(
            farmerCommissionBps = 150L,
            buyerCommissionBps = 300L,    // merchant 3.0%
            customerCommissionBps = 250L, // customer 2.5%, the default Sell party
            defaultLabourPaisa = 18000L   // ₹180
        )

        val form = SellEntryFormState(settings)

        assertEquals("2.50", form.commissionPercent)
        assertEquals("180", form.labourPerBag)
    }

    @Test
    fun receiptOverride_doesNotMutateShopDefaultSettings() {
        val shopDefaults = TradeSettings(
            farmerCommissionBps = 150L,
            buyerCommissionBps = 150L,
            defaultLabourPaisa = 15000L
        )

        val form1 = BuyEntryFormState(shopDefaults)
        // User overrides the rate on receipt 1
        form1.commissionPercent = "3.0"
        form1.labourPerBag = "200"

        // Next receipt is created with shop defaults
        val form2 = BuyEntryFormState(shopDefaults)

        assertEquals("1.50", form2.commissionPercent)
        assertEquals("150", form2.labourPerBag)

        // Shop defaults remain completely unchanged
        assertEquals(150L, shopDefaults.farmerCommissionBps)
        assertEquals(15000L, shopDefaults.defaultLabourPaisa)
    }

    @Test
    fun grossWeight_appliedInBuyNetWeightCalculation() {
        val form = BuyEntryFormState()
        form.bags = "10"
        form.weightPerBagKg = "50" // 500 kg = 5.0 quintals
        form.ratePerQuintal = "2000"

        val preview = form.aggregatePreview()

        // Gross weight 500 kg
        assertEquals(500_000L, preview.totalWeightGrams)
        // 5.0 qtl @ ₹2,000 = ₹10,000 (1,000,000 paisa)
        assertEquals(1_000_000L, preview.totalGrossPaisa)
    }
}
