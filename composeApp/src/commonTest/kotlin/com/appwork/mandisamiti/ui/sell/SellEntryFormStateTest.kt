package com.appwork.mandisamiti.ui.sell

import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.SellPartyKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SellEntryFormStateTest {

    @Test
    fun customerCannotSelectBillOfSupply() {
        val state = SellEntryFormState()
        state.setParty(SellPartyKind.CUSTOMER)
        state.setMode(BillMode.BILL_OF_SUPPLY)
        assertEquals(BillMode.NORMAL, state.billMode, "Customer must never be allowed to select Bill of Supply")

        // If Merchant selects BOS, then party changes to Customer, BOS resets to NORMAL
        state.setParty(SellPartyKind.MERCHANT)
        state.setMode(BillMode.BILL_OF_SUPPLY)
        assertEquals(BillMode.BILL_OF_SUPPLY, state.billMode)
        state.setParty(SellPartyKind.CUSTOMER)
        assertEquals(BillMode.NORMAL, state.billMode, "Switching to Customer must reset mode to NORMAL")
    }

    @Test
    fun normalCustomerSingleLotCalculation() {
        val state = SellEntryFormState()
        state.buyerName = "Ramesh Kumar"
        state.setParty(SellPartyKind.CUSTOMER)
        state.commissionPercent = "2"
        state.labourPerBag = "5"

        val lot = state.lots[0]
        lot.cropName = "गेहूं"
        lot.bags = "10"
        lot.weightPerBagKg = "50"
        lot.ratePerQuintal = "2600"

        val preview = state.aggregatePreview()
        // 10 bags * 50kg = 500kg = 5.0 Qtl @ 2600 = Rs 13,000 (1,300,000 paisa)
        assertEquals(1_300_000, preview.grossPaisa)
        // Commission 2% = Rs 260 (26,000 paisa)
        assertEquals(26_000, preview.commissionPaisa)
        // Per sheet parity: Cash Memo has no separate labour; tulak absorbs whole-rupee cut
        // 5.0 Qtl * Rs 1 nominal = Rs 5. Total unrounded = 13,000 + 260 + 5 = 13,265
        assertEquals(0, preview.labourPaisa)
        // No Mandi tax or Cess on normal
        assertEquals(0, preview.mandiShulkPaisa)
        assertEquals(0, preview.krishakCessPaisa)
        // Tulak absorbs difference: 13,265 - 13,000 - 260 = 5 Rs = 500 paisa
        assertEquals(500, preview.tulakPaisa)
        // Total receivable = Rs 13,265 (1,326,500 paisa)
        assertEquals(1_326_500, preview.totalReceivablePaisa)

        assertTrue(state.canSave())
        assertNull(state.validationReason())
    }

    @Test
    fun normalMerchantMultiLotCalculation() {
        val state = SellEntryFormState()
        state.buyerName = "Shree Agro Traders"
        state.setParty(SellPartyKind.MERCHANT)
        state.setMode(BillMode.NORMAL)
        state.commissionPercent = "1.5"
        state.labourPerBag = "4"

        // Lot 1: 20 bags Lokwan @ 2500
        state.lots[0].cropName = "गेहूं लोकवान"
        state.lots[0].bags = "20"
        state.lots[0].weightPerBagKg = "50"
        state.lots[0].ratePerQuintal = "2500" // Gross = Rs 25,000

        // Lot 2: 10 bags Sharbati @ 3000
        state.addLot("गेहूं शरबती")
        state.lots[1].bags = "10"
        state.lots[1].weightPerBagKg = "50"
        state.lots[1].ratePerQuintal = "3000" // Gross = Rs 15,000

        val preview = state.aggregatePreview()
        assertEquals(30, preview.totalBags)
        assertEquals(4_000_000, preview.grossPaisa) // Rs 40,000
        // Commission 1.5% of 40,000 = Rs 600 (60,000 paisa)
        assertEquals(60_000, preview.commissionPaisa)
        // Per sheet parity: Cash Memo has no separate labour; tulak absorbs rounding
        assertEquals(0, preview.labourPaisa)
        // No Mandi tax on normal
        assertEquals(0, preview.mandiShulkPaisa)
        // 30 bags x 50kg = 15.0 Qtl * Rs 1 nominal = Rs 15 (1,500 paisa)
        assertEquals(1_500, preview.tulakPaisa)
        // Total = 40,000 + 600 + 15 = Rs 40,615 (4,061,500 paisa)
        assertEquals(4_061_500, preview.totalReceivablePaisa)

        assertTrue(state.canSave())
    }

    @Test
    fun billOfSupplyMerchantRequiresGstinAndStateCode() {
        val state = SellEntryFormState()
        state.buyerName = "Kisan Seeds Corp"
        state.setParty(SellPartyKind.MERCHANT)
        state.setMode(BillMode.BILL_OF_SUPPLY)

        state.lots[0].bags = "20"
        state.lots[0].weightPerBagKg = "50"
        state.lots[0].ratePerQuintal = "2600"

        // Without GSTIN, save must be blocked
        assertFalse(state.canSave())
        assertEquals("व्यापारी GSTIN दर्ज करें", state.validationReason())

        // With invalid length GSTIN
        state.gstin = "12345"
        assertFalse(state.canSave())
        assertTrue(state.validationReason()?.contains("15") == true)

        // Valid GSTIN & State Code
        state.gstin = "23AAAAA0000A1Z5"
        state.stateCode = "23"
        assertTrue(state.canSave())

        val preview = state.aggregatePreview()
        // 20 bags x 50kg = 10 Qtl @ 2600 = Rs 26,000 (2,600,000 paisa)
        assertEquals(2_600_000, preview.grossPaisa)
        // Mandi Shulk 1.5% of 26,000 = Rs 390 (39,000 paisa)
        assertEquals(39_000, preview.mandiShulkPaisa)
        // Krishak Kalyan Cess 0% on BOS
        assertEquals(0, preview.krishakCessPaisa)
        // No labour on BOS per sheet parity
        assertEquals(0, preview.labourPaisa)
        // Tulak default Rs 1/qtl = 10 * 1 = Rs 10 (1,000 paisa)
        assertEquals(1_000, preview.tulakPaisa)
        // Commission (arat) 1.5% default: 26,000 * 1.5% = 390 (39,000 paisa)
        // Total = 26,000 + 390 (arat) + 390 (shulk) + 10 (tulak) = Rs 26,790 (2,679,000 paisa)
        assertEquals(2_679_000, preview.totalReceivablePaisa)
    }

    @Test
    fun emptyFieldValidationGating() {
        val state = SellEntryFormState()
        // Empty buyer name
        assertFalse(state.canSave())
        assertEquals("ग्राहक का नाम दर्ज करें", state.validationReason())

        state.buyerName = "Sita Ram"
        // Empty bags
        assertFalse(state.canSave())
        assertEquals("फसल #1: बोरी संख्या दर्ज करें", state.validationReason())

        state.lots[0].bags = "10"
        // Empty rate
        assertFalse(state.canSave())
        assertEquals("फसल #1: भाव दर्ज करें", state.validationReason())

        state.lots[0].ratePerQuintal = "2500"
        assertTrue(state.canSave())
        assertNull(state.validationReason())
    }

    @Test
    fun toSellEntryRoundTripWithMultiLots() {
        val state = SellEntryFormState()
        state.buyerName = "Apex Grains"
        state.setParty(SellPartyKind.MERCHANT)
        state.setMode(BillMode.BILL_OF_SUPPLY)
        state.gstin = "23AAAAA0000A1Z5"
        state.stateCode = "23"

        state.lots[0].cropName = "चना"
        state.lots[0].bags = "15"
        state.lots[0].ratePerQuintal = "5200"

        state.addLot("सरसों")
        state.lots[1].bags = "25"
        state.lots[1].ratePerQuintal = "5600"

        val entry = state.toSellEntry(shopId = "shop-test")
        assertEquals(SellPartyKind.MERCHANT, entry.partyKind)
        assertEquals(BillMode.BILL_OF_SUPPLY, entry.billMode)
        assertEquals("23AAAAA0000A1Z5", entry.gstin)
        assertEquals(0L, entry.receivedPaisa, "Prapt Rashi must be 0")
        assertNotNull(entry.remarks, "Remarks must store JSON serialized multi-lots")

        // Test restore
        val restored = SellEntryFormState()
        restored.populateFrom(entry)
        assertEquals("Apex Grains", restored.buyerName)
        assertEquals(SellPartyKind.MERCHANT, restored.partyKind)
        assertEquals(BillMode.BILL_OF_SUPPLY, restored.billMode)
        assertEquals("23AAAAA0000A1Z5", restored.gstin)
        assertEquals(2, restored.lots.size)
        assertEquals("चना", restored.lots[0].cropName)
        assertEquals("15", restored.lots[0].bags)
        assertEquals("सरसों", restored.lots[1].cropName)
        assertEquals("25", restored.lots[1].bags)
    }
}
