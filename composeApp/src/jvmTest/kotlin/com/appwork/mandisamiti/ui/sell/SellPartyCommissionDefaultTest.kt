package com.appwork.mandisamiti.ui.sell

import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.model.TradeSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class SellPartyCommissionDefaultTest {
    private val settings = TradeSettings(buyerCommissionBps = 150L, customerCommissionBps = 100L)

    @Test
    fun customerOpensAtCustomerRateAndMerchantSwitchesToMerchantRate() {
        val f = SellEntryFormState(settings)
        assertEquals("1", f.commissionPercent)
        f.setParty(SellPartyKind.MERCHANT)
        assertEquals("1.50", f.commissionPercent)
        f.setParty(SellPartyKind.CUSTOMER)
        assertEquals("1", f.commissionPercent)
    }

    @Test
    fun switchingPartyKeepsACommissionTheOwnerTypedByHand() {
        val f = SellEntryFormState(settings)
        f.commissionPercent = "2.25"
        f.setParty(SellPartyKind.MERCHANT)
        assertEquals("2.25", f.commissionPercent)
    }
}
