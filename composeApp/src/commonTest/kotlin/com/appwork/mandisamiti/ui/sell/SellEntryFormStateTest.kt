package com.appwork.mandisamiti.ui.sell

import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.model.TradeSettings
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
        state.updatePhone("9876543210")
        state.village = "सीहोर"
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
        // Tulai is same as labour: no separate tulak
        assertEquals(0, preview.labourPaisa)
        // No Mandi tax or Cess on normal
        assertEquals(0, preview.mandiShulkPaisa)
        assertEquals(0, preview.krishakCessPaisa)
        assertEquals(0, preview.tulakPaisa)
        // Total receivable = Rs 13,260 (1,326,000 paisa)
        assertEquals(1_326_000, preview.totalReceivablePaisa)

        assertTrue(state.canSave())
        assertNull(state.validationReason())
    }

    @Test
    fun normalMerchantMultiLotCalculation() {
        val state = SellEntryFormState()
        state.buyerName = "Shree Agro Traders"
        state.updatePhone("9876543210")
        state.village = "सीहोर"
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
        // Tulai is same as labour: no separate tulak
        assertEquals(0, preview.labourPaisa)
        // No Mandi tax on normal
        assertEquals(0, preview.mandiShulkPaisa)
        assertEquals(0, preview.tulakPaisa)
        // Total = 40,000 + 600 = Rs 40,600 (4,060,000 paisa)
        assertEquals(4_060_000, preview.totalReceivablePaisa)

        assertTrue(state.canSave())
    }

    @Test
    fun billOfSupplyMerchantRequiresGstinAndStateCode() {
        val state = SellEntryFormState()
        state.buyerName = "Kisan Seeds Corp"
        state.updatePhone("9876543210")
        state.village = "सीहोर"
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
        // Tulai is same as labour: no separate tulak
        assertEquals(0, preview.labourPaisa)
        assertEquals(0, preview.tulakPaisa)
        // Commission (arat) 1.5% default: 26,000 * 1.5% = 390 (39,000 paisa)
        // Total = 26,000 + 390 (arat) + 390 (shulk) = Rs 26,780 (2,678,000 paisa)
        assertEquals(2_678_000, preview.totalReceivablePaisa)
    }

    @Test
    fun emptyFieldValidationGating() {
        val state = SellEntryFormState()
        // Empty buyer name
        assertFalse(state.canSave())
        assertEquals("ग्राहक का नाम दर्ज करें", state.validationReason())

        state.buyerName = "Sita Ram"
        // Empty phone
        assertFalse(state.canSave())
        assertEquals("मोबाइल नंबर दर्ज करें", state.validationReason())

        state.updatePhone("98765")
        assertFalse(state.canSave())
        assertTrue(state.validationReason()?.contains("10 अंकों") == true)

        state.updatePhone("9876543210")
        // Empty village
        assertFalse(state.canSave())
        assertEquals("गाँव / शहर का नाम दर्ज करें", state.validationReason())

        state.village = "सीहोर"
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

    @Test
    fun merchantTraderLabourCalculatedAndRoundsToWholeRupee() {
        val settings = TradeSettings(
            sellLabourPaisaPerQuintal = 500L // ₹5 per quintal
        )
        val state = SellEntryFormState(settings)
        state.buyerName = "Apex Grains"
        state.updatePhone("9876543210")
        state.village = "सीहोर"
        state.setParty(SellPartyKind.MERCHANT)
        state.commissionPercent = "1.0" // 1%

        // 10 bags x 50 kg + 21 kg = 521 kg = 5.21 quintals
        state.lots[0].cropName = "गेहूं"
        state.lots[0].bags = "10"
        state.lots[0].weightPerBagKg = "50"
        state.lots[0].extraWeightKg = "21"
        state.lots[0].ratePerQuintal = "2000"

        val preview = state.aggregatePreview()
        // Total weight = 521,000 grams = 5.21 qtl
        assertEquals(521_000L, preview.totalWeightGrams)
        // Gross = 5.21 * 2000 = 10,420 Rs = 1,042,000 paisa
        assertEquals(1_042_000L, preview.grossPaisa)
        // Arat 1% = 10,420 paisa
        assertEquals(10_420L, preview.commissionPaisa)
        // No Mandi Shulk on normal mode
        assertEquals(0L, preview.mandiShulkPaisa)
        // Tulai is same as labour: tulak = 0
        assertEquals(0L, preview.tulakPaisa)
        // Trader labour nominal = 5.21 * 5 = 26.05 Rs = 2,605 paisa
        // Raw total = 1,042,000 + 10,420 + 2,605 = 1,055,025 paisa
        // Rounded to nearest rupee: 1,055,000 paisa
        assertEquals(1_055_000L, preview.totalReceivablePaisa)
        assertEquals(0L, preview.totalReceivablePaisa % 100L)
        // Labour absorbs rounding: 2,605 + (1,055,000 - 1,055,025) = 2,605 - 25 = 2,580 paisa
        assertEquals(2_580L, preview.labourPaisa)
        assertEquals(preview.grossPaisa + preview.commissionPaisa + preview.tulakPaisa + preview.mandiShulkPaisa + preview.labourPaisa, preview.totalReceivablePaisa)
    }

    @Test
    fun customerAlsoPaysPerQuintalLabour() {
        val settings = TradeSettings(
            sellLabourPaisaPerQuintal = 500L
        )
        val state = SellEntryFormState(settings)
        state.buyerName = "Retail Customer"
        state.updatePhone("9876543210")
        state.village = "सीहोर"
        state.setParty(SellPartyKind.CUSTOMER)
        state.lots[0].bags = "10"
        state.lots[0].weightPerBagKg = "50"
        state.lots[0].ratePerQuintal = "2000"

        val preview = state.aggregatePreview()
        // 5 qtl x Rs 5 = Rs 25 labour; rounding paise also go into labour
        assertEquals(true, preview.labourPaisa in 2_450L..2_550L)
        assertEquals(0L, preview.totalReceivablePaisa % 100L)
    }

    @Test
    fun savedSellKeepsExtraWeightAndLabourTotalAndReopensSame() {
        val settings = TradeSettings(sellLabourPaisaPerQuintal = 500L)
        val state = SellEntryFormState(settings)
        state.buyerName = "Retail Customer"
        state.updatePhone("9876543210")
        state.village = "सीहोर"
        state.setParty(SellPartyKind.CUSTOMER)
        state.lots[0].cropName = "गेहूं"
        state.lots[0].bags = "10"
        state.lots[0].weightPerBagKg = "50"
        state.lots[0].extraWeightKg = "21"
        state.lots[0].ratePerQuintal = "2000"

        val preview = state.aggregatePreview()
        val entry = state.toSellEntry(shopId = "shop-1")
        assertEquals(521_000L, entry.totalWeightGrams) // extra weight is billed and stored
        assertEquals(preview.totalReceivablePaisa, entry.totalReceivablePaisa) // labour is inside the saved total
        assertEquals(true, entry.remarks!!.contains("21000")) // extra weight kept per lot

        val reopened = SellEntryFormState(settings)
        reopened.populateFrom(entry)
        assertEquals(entry.totalReceivablePaisa, reopened.aggregatePreview().totalReceivablePaisa)
        assertEquals(521_000L, reopened.aggregatePreview().totalWeightGrams)
    }

    @Test
    fun customDecimalLabourPerQuintalRoundsReceivableToWholeRupee() {
        val state = SellEntryFormState() // no default settings
        state.buyerName = "Mohan Traders"
        state.village = "सीहोर"
        state.setParty(SellPartyKind.MERCHANT)
        state.commissionPercent = "1.5"
        state.labourPerQuintal = "5.50" // Decimal labour input

        // 10 bags x 50 kg + 15 kg extra = 515 kg = 5.15 qtl at ₹2,200/qtl
        state.lots[0].bags = "10"
        state.lots[0].weightPerBagKg = "50"
        state.lots[0].extraWeightKg = "15"
        state.lots[0].ratePerQuintal = "2200"

        val preview = state.aggregatePreview()
        assertEquals(515_000L, preview.totalWeightGrams)
        // Must be an exact whole rupee:
        assertEquals(0L, preview.totalReceivablePaisa % 100L)
        assertEquals(0L, preview.tulakPaisa, "Tulai is same as labour; tulak must be 0")
        // Labour component absorbs rounding:
        assertEquals(
            preview.totalReceivablePaisa,
            preview.grossPaisa + preview.commissionPaisa + preview.tulakPaisa + preview.mandiShulkPaisa + preview.labourPaisa
        )
    }
}
