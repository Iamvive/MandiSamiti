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
            defaultLabourPaisa = 15000L,  // ₹150
            defaultTareGrams = 35000L     // 0.35 qtl
        )

        val form = BuyEntryFormState(settings)

        assertEquals("1.50", form.commissionPercent)
        assertEquals("150", form.labourPerBag)
        assertEquals("0.35", form.tareQuintals)
    }

    @Test
    fun sellForm_seedsFromSavedShopTradeSettings() {
        val settings = TradeSettings(
            farmerCommissionBps = 150L,
            buyerCommissionBps = 250L,    // 2.5%
            defaultLabourPaisa = 18000L,  // ₹180
            defaultTareGrams = 20000L     // 0.2 qtl
        )

        val form = SellEntryFormState(settings)

        assertEquals("2.50", form.commissionPercent)
        assertEquals("180", form.labourPerBag)
        assertEquals("0.2", form.tareQuintals)
    }

    @Test
    fun receiptOverride_doesNotMutateShopDefaultSettings() {
        val shopDefaults = TradeSettings(
            farmerCommissionBps = 150L,
            buyerCommissionBps = 150L,
            defaultLabourPaisa = 15000L,
            defaultTareGrams = 35000L
        )

        val form1 = BuyEntryFormState(shopDefaults)
        // User overrides the rate on receipt 1
        form1.commissionPercent = "3.0"
        form1.labourPerBag = "200"
        form1.tareQuintals = "0.50"

        // Next receipt is created with shop defaults
        val form2 = BuyEntryFormState(shopDefaults)

        assertEquals("1.50", form2.commissionPercent)
        assertEquals("150", form2.labourPerBag)
        assertEquals("0.35", form2.tareQuintals)

        // Shop defaults remain completely unchanged
        assertEquals(150L, shopDefaults.farmerCommissionBps)
        assertEquals(15000L, shopDefaults.defaultLabourPaisa)
        assertEquals(35000L, shopDefaults.defaultTareGrams)
    }

    @Test
    fun tareWeight_appliedInBuyNetWeightCalculation() {
        val form = BuyEntryFormState()
        form.bags = "10"
        form.weightPerBagKg = "50" // 500 kg = 5.0 quintals
        form.ratePerQuintal = "2000"
        form.tareQuintals = "0.5" // 50 kg tare

        val preview = form.aggregatePreview()

        // Gross weight 500 kg - tare 50 kg = 450 kg (4.5 quintals)
        assertEquals(450_000L, preview.totalWeightGrams)
        // 4.5 qtl @ ₹2,000 = ₹9,000 (900,000 paisa)
        assertEquals(900_000L, preview.totalGrossPaisa)
    }
}
