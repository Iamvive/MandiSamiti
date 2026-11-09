package com.appwork.mandisamiti.ui.sell

import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SellEntryDirectionBTest {

    @Test
    fun phoneAndVillageAreRequiredToSave() {
        val form = SellEntryFormState()
        form.buyerName = "अमित ट्रेडर्स"
        form.lots[0].bags = "10"
        form.lots[0].weightPerBagKg = "50"
        form.lots[0].ratePerQuintal = "2500"

        // Without phone, save is blocked
        assertFalse(form.canSave())
        assertEquals("मोबाइल नंबर दर्ज करें", form.validationReason())

        // Incomplete phone blocks save
        form.updatePhone("98765")
        assertFalse(form.canSave())
        assertTrue(form.validationReason()?.contains("10 अंकों") == true)

        // With valid 10 digits but no village, save is blocked
        form.updatePhone("9876543210")
        assertFalse(form.canSave())
        assertEquals("गाँव / शहर का नाम दर्ज करें", form.validationReason())

        // With village entered, save is enabled
        form.village = "सीहोर"
        assertTrue(form.canSave())
        assertNull(form.validationReason())
    }

    @Test
    fun extraWeightMustBeLessThanBagWeight() {
        val form = SellEntryFormState()
        form.buyerName = "सुरेश कुमार"
        form.updatePhone("9876543210")
        form.village = "आष्टा"
        val lot = form.lots[0]
        lot.bags = "10"
        lot.weightPerBagKg = "50"
        lot.ratePerQuintal = "2400"

        // Valid extra weight < 50kg
        lot.extraWeightKg = "4.5"
        assertTrue(lot.validate().isValid)
        assertTrue(form.canSave())

        // Extra weight >= bag weight is invalid
        lot.extraWeightKg = "50"
        assertFalse(lot.validate().isValid)
        assertEquals("अतिरिक्त वजन बोरी के वजन से कम होना चाहिए", lot.validate().extraError)
        assertFalse(form.canSave())

        lot.extraWeightKg = "55"
        assertFalse(lot.validate().isValid)
        assertFalse(form.canSave())
    }

    @Test
    fun newSlipsCalculateGrossWeightAndPopulateFromRemarks() {
        val form = SellEntryFormState()

        form.buyerName = "राजेन्द्र"
        form.updatePhone("9876543210")
        form.village = "सीहोर"
        form.lots[0].bags = "20"
        form.lots[0].weightPerBagKg = "50"
        form.lots[0].ratePerQuintal = "3000"

        // Weight = 20 * 50 = 1000 kg (10.00 Qtl) = 1,000,000 grams
        val preview = form.aggregatePreview()
        assertEquals(1_000_000L, preview.totalWeightGrams)
        assertEquals(3_000_000L, preview.grossPaisa)

        val oldEntry = SellEntry(
            id = "sell-old-1",
            shopId = "shop-1",
            buyerId = "राजेन्द्र",
            commodityId = "गेहूं",
            entryDate = 1700000000000L,
            billMode = BillMode.NORMAL,
            partyKind = SellPartyKind.CUSTOMER,
            bagsCount = 20,
            weightPerBagGrams = 50_000L,
            totalWeightGrams = 1_000_000L,
            ratePaisaPerQuintal = 300_000L,
            aratBps = 150L,
            tulakPaisa = 1000L,
            grossPaisa = 3_000_000L,
            aratPaisa = 45_000L,
            mandiShulkPaisa = 0L,
            totalReceivablePaisa = 3_046_000L,
            receivedPaisa = 0L,
            balanceDuePaisa = 3_046_000L,
            vehicleNumber = "9876543210",
            remarks = "गाँव: सीहोर | पुराना हिसाब",
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L
        )

        val restoredForm = SellEntryFormState()
        restoredForm.populateFrom(oldEntry)
        assertEquals("9876543210", restoredForm.phone)
        assertEquals("सीहोर", restoredForm.village)
    }

    @Test
    fun advanceReceivedReducesBalanceDuePaisa() {
        val form = SellEntryFormState()
        form.buyerName = "गोपाल ट्रेडर्स"
        form.updatePhone("9876543210")
        form.village = "भोपाल"
        form.commissionPercent = "1.5"
        form.labourPerBag = "0"

        form.lots[0].bags = "10"
        form.lots[0].weightPerBagKg = "50"
        form.lots[0].ratePerQuintal = "2000"
        // 10 bags * 50kg = 5 Qtl @ 2000 = Rs 10,000 (1,000,000 paisa)
        // Commission 1.5% = Rs 150 (15,000 paisa)
        // Nominal tulak 5 Qtl * 1 = Rs 5 (500 paisa)
        // Total receivable = Rs 10,155 (1,015,500 paisa)

        val previewBefore = form.aggregatePreview()
        assertEquals(1_015_500L, previewBefore.totalReceivablePaisa)
        assertEquals(0L, previewBefore.advanceReceivedPaisa)
        assertEquals(1_015_500L, previewBefore.balanceDuePaisa)

        // Advance received Rs 3,000 (300,000 paisa)
        form.advanceReceived = "3000"
        val previewAfter = form.aggregatePreview()
        assertEquals(1_015_500L, previewAfter.totalReceivablePaisa)
        assertEquals(300_000L, previewAfter.advanceReceivedPaisa)
        assertEquals(715_500L, previewAfter.balanceDuePaisa) // Rs 7,155 remaining receivable
    }

    @Test
    fun loanAndDepositDoNotAlterTotalReceivableOrBalanceDue() {
        val form = SellEntryFormState()
        form.buyerName = "विकास"
        form.updatePhone("9876543210")
        form.village = "उज्जैन"
        form.commissionPercent = "0"
        form.labourPerBag = "0"

        form.lots[0].bags = "10"
        form.lots[0].weightPerBagKg = "50"
        form.lots[0].ratePerQuintal = "2000" // 5 Qtl @ 2000 = Rs 10,000

        // Set party open loan debt and deposit
        form.setLoan(debtPaisa = 525_000L) // Rs 5,250 total debt
        form.setDeposit(paisa = 150_000L) // Rs 1,500 deposit

        assertEquals(525_000L, form.loanDebtPaisa)
        assertEquals(150_000L, form.depositPaisa)

        val preview = form.aggregatePreview()
        // Total receivable and balance due remain strictly commodity bill arithmetic
        // Nominal tulak = 5 * 100 = 500 paisa -> total = 1,000,500 paisa
        assertEquals(1_000_500L, preview.totalReceivablePaisa)
        assertEquals(1_000_500L, preview.balanceDuePaisa)
    }

    @Test
    fun lotFieldLimitsEnforced() {
        val lot = SellLotItemState()
        lot.bags = "10"
        lot.weightPerBagKg = "50"
        lot.ratePerQuintal = "2500"
        assertTrue(lot.validate().isValid)

        // Bags > 9,999
        lot.bags = "10000"
        assertFalse(lot.validate().isValid)
        assertTrue(lot.validate().bagsError?.contains("अधिकतम") == true)
        lot.bags = "9999"
        assertTrue(lot.validate().isValid)

        // Weight per bag > 999 kg or fractional kg
        lot.weightPerBagKg = "1000"
        assertFalse(lot.validate().isValid)
        assertTrue(lot.validate().weightError?.contains("अधिकतम") == true)

        lot.weightPerBagKg = "50.5" // Fractional weight not allowed per sheet rules
        assertFalse(lot.validate().isValid)
        assertTrue(lot.validate().weightError?.contains("पूरे किलो") == true)
        lot.weightPerBagKg = "999"
        assertTrue(lot.validate().isValid)

        // Rate > 1,00,000 or fractional rupee
        lot.ratePerQuintal = "100001"
        assertFalse(lot.validate().isValid)
        assertTrue(lot.validate().rateError?.contains("अधिकतम") == true)

        lot.ratePerQuintal = "2500.50"
        assertFalse(lot.validate().isValid)
        assertTrue(lot.validate().rateError?.contains("पूरे रुपये") == true)
        lot.ratePerQuintal = "100000"
        assertTrue(lot.validate().isValid)
    }
}
