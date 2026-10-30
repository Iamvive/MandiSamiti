package com.appwork.mandisamiti.ui.buy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuyEntryFormStateTest {
    @Test
    fun previewMatchesTheBuyWorkedExample() {
        val f = BuyEntryFormState().apply {
            farmerName = "रमेश किसान"
            bags = "10"; weightPerBagKg = "50"; extraWeightKg = "5"; ratePerQuintal = "2500"
            commissionPercent = "2"; labourPerBag = "5"; advancePaid = "1000"
        }
        assertEquals(1_132_250, f.preview().netPayablePaisa)
    }

    @Test
    fun blankFieldsPreviewAsZeroNotCrash() {
        assertEquals(0, BuyEntryFormState().preview().grossPaisa)
    }

    @Test
    fun multiLotCalculationAggregatesCorrectly() {
        val f = BuyEntryFormState().apply {
            farmerName = "सुनील कुमार"
            village = "इंदौर"
            commissionPercent = "2" // 2%
            labourPerBag = "5" // ₹5/bag
            advancePaid = "0"

            // Lot 1: गेहूं शरबती: 10 bags, 50kg, rate 2800 -> 5 Qtl @ 2800 = 14,000
            lots[0].cropName = "गेहूं शरबती"
            lots[0].bags = "10"
            lots[0].weightPerBagKg = "50"
            lots[0].ratePerQuintal = "2800"

            // Lot 2: गेहूं लोकवान: 20 bags, 50kg, rate 2450 -> 10 Qtl @ 2450 = 24,500
            addLot("गेहूं लोकवान")
            lots[1].bags = "20"
            lots[1].weightPerBagKg = "50"
            lots[1].ratePerQuintal = "2450"
        }

        val agg = f.aggregatePreview()
        // Total bags: 10 + 20 = 30
        assertEquals(30L, agg.totalBags)
        // Total weight: 500kg + 1000kg = 1500kg = 1,500,000 grams = 15 Quintals
        assertEquals(1_500_000L, agg.totalWeightGrams)
        // Total gross: 14,000 + 24,500 = 38,500 = 3,850,000 paisa
        assertEquals(3_850_000L, agg.totalGrossPaisa)
        // Commission (2% on 38,500) = 770 = 77,000 paisa
        assertEquals(77_000L, agg.commissionPaisa)
        // Labour (30 bags * 5) = 150 = 15,000 paisa
        assertEquals(15_000L, agg.labourPaisa)
        // Net payable = 38,500 - 770 - 150 - 0 = 37,580 = 3,758,000 paisa
        assertEquals(3_758_000L, agg.netPayablePaisa)
    }

    @Test
    fun appLevelValidationChecks() {
        val f = BuyEntryFormState()

        // Empty farmer name blocks saving
        assertFalse(f.canSave())
        assertEquals("किसान का नाम दर्ज करें", f.validationReason())

        f.farmerName = "रामू"
        // Active lot with empty bags blocks saving
        assertFalse(f.canSave())
        assertTrue(f.validationReason()?.contains("बोरी") == true)

        f.bags = "10"
        f.weightPerBagKg = "50"
        f.ratePerQuintal = "2500"

        // Valid form can save with optional phone and optional advance blank
        assertTrue(f.canSave())

        // Invalid phone (< 10 digits) blocks saving
        f.updatePhone("98765")
        assertFalse(f.canSave())
        assertTrue(f.validationReason()?.contains("10 अंकों") == true)

        // Valid 10-digit Indian phone allows saving
        f.updatePhone("9876543210")
        assertTrue(f.canSave())
    }

    @Test
    fun toBuyEntryConstructsValidEntityAndPopulatesBack() {
        val f = BuyEntryFormState().apply {
            farmerName = "राधे श्याम"
            selectedFarmerId = "party-123"
            phone = "9876543210"
            village = "खेड़ा"
            bags = "10"
            weightPerBagKg = "50"
            extraWeightKg = "5"
            ratePerQuintal = "2500"
            commissionPercent = "2"
            labourPerBag = "5"
            advancePaid = "1000"
        }
        val entry = f.toBuyEntry(shopId = "shop-123")
        assertEquals("shop-123", entry.shopId)
        assertEquals("party-123", entry.farmerId)
        assertEquals("9876543210", entry.vehicleNumber)
        assertEquals(10L, entry.bagsCount)
        assertEquals(50_000L, entry.weightPerBagGrams)
        assertEquals(5_000L, entry.extraWeightGrams)
        assertEquals(505_000L, entry.totalWeightGrams)
        assertEquals(250_000L, entry.ratePaisaPerQuintal)
        assertEquals(200L, entry.commissionBps)
        assertEquals(500L, entry.labourPerBagPaisa)
        assertEquals(100_000L, entry.advancePaidPaisa)
        assertEquals(1_262_500L, entry.grossPaisa)
        assertEquals(25_250L, entry.commissionPaisa)
        assertEquals(5_000L, entry.labourPaisa)
        assertEquals(1_132_250L, entry.netPayablePaisa)

        val restoredForm = BuyEntryFormState()
        restoredForm.populateFrom(entry)
        assertEquals("party-123", restoredForm.farmerName)
        assertEquals("party-123", restoredForm.selectedFarmerId)
        assertEquals("9876543210", restoredForm.phone)
        assertEquals("10", restoredForm.bags)
        assertEquals("50.0", restoredForm.weightPerBagKg)
        assertEquals("5.0", restoredForm.extraWeightKg)
        assertEquals("2500", restoredForm.ratePerQuintal)
        assertEquals("2", restoredForm.commissionPercent)
        assertEquals("5", restoredForm.labourPerBag)
        assertEquals("1000", restoredForm.advancePaid)
        assertEquals("खेड़ा", restoredForm.village)
        assertEquals("खेड़ा", extractVillage(entry.remarks))
    }

    @Test
    fun extractVillageHandlesVariousRemarksFormats() {
        assertEquals("विशनपुर", extractVillage("गाँव: विशनपुर | DATA: []"))
        assertEquals("विशनपुर", extractVillage("गाँव: विशनपुर"))
        assertEquals(null, extractVillage("DATA: []"))
        assertEquals(null, extractVillage(""))
        assertEquals(null, extractVillage(null))
    }
}
