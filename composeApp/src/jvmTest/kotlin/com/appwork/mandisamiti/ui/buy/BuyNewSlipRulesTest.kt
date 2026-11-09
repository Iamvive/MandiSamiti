package com.appwork.mandisamiti.ui.buy

import com.appwork.mandisamiti.domain.model.TradeSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Buy rules Vivek set on 2026-10-31 (review, "Answered by Vivek"). Each limit is tested at and past its edge. */
class BuyNewSlipRulesTest {
    private fun valid() = BuyEntryFormState().apply {
        farmerName = "TEST Ramesh"
        updatePhone("9876543210")
        village = "TEST गाँव"
        bags = "10"; weightPerBagKg = "50"; ratePerQuintal = "2500"
    }

    @Test
    fun aFullyFilledFormCanSave() = assertTrue(valid().canSave())

    @Test
    fun phoneIsRequired() {
        val f = valid().apply { phone = "" }
        assertFalse(f.canSave())
        assertEquals("मोबाइल नंबर दर्ज करें", f.validationReason())
    }

    @Test
    fun villageIsRequired() {
        val f = valid().apply { village = "" }
        assertFalse(f.canSave())
        assertTrue(f.validationReason()?.contains("गाँव") == true)
    }

    @Test
    fun bagsAllowFourDigitsNotFive() {
        assertTrue(valid().apply { bags = "9999" }.canSave())
        assertFalse(valid().apply { bags = "10000" }.canSave())
    }

    @Test
    fun bagWeightIsWholeKgUpTo999() {
        assertTrue(valid().apply { weightPerBagKg = "999" }.canSave())
        assertFalse(valid().apply { weightPerBagKg = "1000" }.canSave())
        assertFalse(valid().apply { weightPerBagKg = "50.5" }.canSave())
    }

    @Test
    fun extraWeightMayHaveDecimalsAndStaysBelowBagWeight() {
        assertTrue(valid().apply { extraWeightKg = "2.5" }.canSave())
        assertTrue(valid().apply { extraWeightKg = "49.9" }.canSave())
        assertFalse(valid().apply { extraWeightKg = "50" }.canSave())
        assertFalse(valid().apply { extraWeightKg = "60" }.canSave())
    }

    @Test
    fun rateIsWholeRupeesUpToOneLakh() {
        assertTrue(valid().apply { ratePerQuintal = "100000" }.canSave())
        assertFalse(valid().apply { ratePerQuintal = "100001" }.canSave())
        assertFalse(valid().apply { ratePerQuintal = "2500.50" }.canSave())
    }

    @Test
    fun newFormSeedsFromSettingsAndUsesEnteredWeight() {
        val settings = TradeSettings(farmerCommissionBps = 150L, buyerCommissionBps = 200L, defaultLabourPaisa = 15000L)
        val f = BuyEntryFormState(settings)
        assertEquals("1.50", f.commissionPercent)
        assertEquals("150", f.labourPerBag)

        val preview = valid().preview()
        assertEquals(preview.grossPaisa, valid().also { it.applySettings(settings) }.preview().grossPaisa)
    }

    @Test
    fun editingAnOldSlipWithDecimalStoredWeightStillPassesTheWholeKgRule() {
        val entry = valid().toBuyEntry("shop")
        val f = BuyEntryFormState().also { it.populateFrom(entry); it.village = "TEST गाँव" }
        assertEquals("50", f.weightPerBagKg.removeSuffix(".0"))
        assertTrue(f.canSave())
    }
}
