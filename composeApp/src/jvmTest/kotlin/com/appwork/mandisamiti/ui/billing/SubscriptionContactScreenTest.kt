package com.appwork.mandisamiti.ui.billing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionContactScreenTest {

    @Test
    fun testContactScreenDisplaysExhaustedUsage() {
        val state = SubscriptionState(totalCreated = 70, limit = 70, shopName = "Chaudhary Trading Co.")
        assertEquals("All 70 bills created for this month - Limit Reached", state.exhaustedMessage)
        assertTrue(state.isExhausted)
        assertEquals("Chaudhary Trading Co.", state.shopName)
    }

    @Test
    fun testContactScreenDisplaysActiveUsage() {
        val state = SubscriptionState(totalCreated = 45, limit = 50, shopName = "Kisan Store")
        assertEquals("45 of 50 bills created this month", state.activeMessage)
        assertFalse(state.isExhausted)
    }

    @Test
    fun testContactScreenPlanDetails() {
        val chhotaPlan = BillingPlan.VyapariChhota
        val unlimitedPlan = BillingPlan.VyapariUnlimited

        assertEquals("CHHOTA", chhotaPlan.id)
        assertEquals("Vyapari Chhota Plan", chhotaPlan.title)
        assertEquals("Up to 150 bills/month, Rokad cashbook, 1 device", chhotaPlan.description)
        assertTrue(chhotaPlan.features.contains("Up to 150 bills/month"))
        assertTrue(chhotaPlan.features.contains("Rokad cashbook"))

        assertEquals("PRO", unlimitedPlan.id)
        assertEquals("Vyapari Unlimited Pro", unlimitedPlan.title)
        assertEquals("Unlimited bills, multi-phone sync, priority support", unlimitedPlan.description)
        assertTrue(unlimitedPlan.isHighlighted)
        assertTrue(unlimitedPlan.features.contains("Unlimited bills"))
        assertTrue(unlimitedPlan.features.contains("Multi-phone sync"))
        assertTrue(unlimitedPlan.features.contains("Priority support"))
    }

    @Test
    fun testContactSupportHandlerUriBuilders() {
        val phoneUri = ContactSupportHandler.buildDialerUri(MANDI_SUPPORT_PHONE)
        assertEquals("tel:+919368299074", phoneUri)

        val whatsAppUri = ContactSupportHandler.buildWhatsAppUri(
            MANDI_SUPPORT_WHATSAPP,
            DEFAULT_WHATSAPP_UPGRADE_MESSAGE
        )
        assertEquals(
            "https://wa.me/919368299074?text=Hello%20MandiSamiti%20Support,%20I%20want%20to%20upgrade%20my%20shop%20account.",
            whatsAppUri
        )
    }

    @Test
    fun testContactSupportHandlerCallbackDelegation() {
        var dialedNumber: String? = null
        var whatsAppNumber: String? = null
        var whatsAppMessage: String? = null

        val handler = object : ContactSupportHandler {
            override fun dialPhone(phoneNumber: String) {
                dialedNumber = phoneNumber
            }

            override fun openWhatsApp(phoneNumber: String, message: String) {
                whatsAppNumber = phoneNumber
                whatsAppMessage = message
            }
        }

        handler.dialPhone(MANDI_SUPPORT_PHONE)
        assertEquals(MANDI_SUPPORT_PHONE, dialedNumber)

        handler.openWhatsApp(MANDI_SUPPORT_WHATSAPP, DEFAULT_WHATSAPP_UPGRADE_MESSAGE)
        assertEquals(MANDI_SUPPORT_WHATSAPP, whatsAppNumber)
        assertEquals(DEFAULT_WHATSAPP_UPGRADE_MESSAGE, whatsAppMessage)
    }

    @Test
    fun testReassuranceCopy() {
        assertEquals(
            "All existing data, ledgers, and slips remain permanently saved and readable.",
            REASSURANCE_FOOTER_TEXT
        )
        assertEquals(
            "सभी पुराना डेटा, बही-खाते और पर्चियां हमेशा सुरक्षित और पढ़ने के लिए उपलब्ध रहेंगी।",
            REASSURANCE_FOOTER_TEXT_HI
        )
    }
}
