package com.appwork.mandisamiti.ui.forms

import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.validation.MandiValidators
import com.appwork.mandisamiti.ui.buy.BuyEntryFormState
import com.appwork.mandisamiti.ui.sell.SellEntryFormState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormSanityAndFormattingF06Test {

    @Test
    fun testIndianNumberGroupingAndNegativeCurrencyFormatting() {
        // Indian grouping test
        assertEquals("10,92,500", MandiMathEngine.formatIndianGrouping(1092500L))
        assertEquals("2,000", MandiMathEngine.formatIndianGrouping(2000L))
        assertEquals("50", MandiMathEngine.formatIndianGrouping(50L))
        assertEquals("1,00,00,000", MandiMathEngine.formatIndianGrouping(10000000L))

        // Currency format test: Positive, Zero, Negative (-₹X, never ₹-X)
        assertEquals("₹10,92,500", MandiMathEngine.formatRupees(109250000L))
        assertEquals("₹0", MandiMathEngine.formatRupees(0L))
        assertEquals("-₹54,62,500", MandiMathEngine.formatRupees(-546250000L))
        assertEquals("-₹12,200", MandiMathEngine.formatRupees(-1220000L))
    }

    @Test
    fun testVillageSanitizationStripsDigitsAndStrayInput() {
        // PM repro had village "Khedae10n2800"
        assertEquals("Khedae", MandiValidators.sanitizeVillage("Khedae10n2800"))
        assertEquals("इंदौर", MandiValidators.sanitizeVillage("इंदौर 452001"))
        assertEquals("खेड़ा", MandiValidators.sanitizeVillage("खेड़ा123"))
    }

    @Test
    fun testBuyFormSanityLimits() {
        val form = BuyEntryFormState().apply {
            farmerName = "राधे श्याम"
            village = "खेड़ा"
            lots[0].bags = "50"
            lots[0].weightPerBagKg = "50"
            lots[0].ratePerQuintal = "2500"
        }

        // Normal values have no sanity warning
        assertFalse(form.hasSanityWarning())

        // 1. Bags > 2,000 triggers sanity warning
        form.lots[0].bags = "2500"
        assertTrue(form.hasSanityWarning())
        assertTrue(form.sanityWarningMessage()?.contains("2,000") == true)
        form.lots[0].bags = "50"

        // 2. Weight per bag outside 20-120 kg triggers sanity warning
        form.lots[0].weightPerBagKg = "15" // Under 20 kg
        assertTrue(form.hasSanityWarning())
        assertTrue(form.sanityWarningMessage()?.contains("20-120") == true)

        form.lots[0].weightPerBagKg = "130" // Over 120 kg
        assertTrue(form.hasSanityWarning())
        assertTrue(form.sanityWarningMessage()?.contains("20-120") == true)
        form.lots[0].weightPerBagKg = "50"

        // 3. Rate outside commodity band (₹500 - ₹20,000)
        form.lots[0].ratePerQuintal = "300" // Under ₹500
        assertTrue(form.hasSanityWarning())
        assertTrue(form.sanityWarningMessage()?.contains("500") == true)

        form.lots[0].ratePerQuintal = "25000" // Over ₹20,000
        assertTrue(form.hasSanityWarning())
        assertTrue(form.sanityWarningMessage()?.contains("20,000") == true)
    }

    @Test
    fun testSellFormSanityLimits() {
        val form = SellEntryFormState().apply {
            buyerName = "महावीर ट्रेडर्स"
            lots[0].bags = "20"
            lots[0].weightPerBagKg = "50"
            lots[0].ratePerQuintal = "3000"
        }

        assertFalse(form.hasSanityWarning())

        // Outlier bags
        form.lots[0].bags = "3000"
        assertTrue(form.hasSanityWarning())
        assertTrue(form.sanityWarningMessage()?.contains("2,000") == true)
        form.lots[0].bags = "20"

        // Outlier weight
        form.lots[0].weightPerBagKg = "10"
        assertTrue(form.hasSanityWarning())
        assertTrue(form.sanityWarningMessage()?.contains("20-120") == true)
    }
}
