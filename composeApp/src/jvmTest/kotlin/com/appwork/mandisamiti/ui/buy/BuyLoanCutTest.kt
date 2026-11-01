package com.appwork.mandisamiti.ui.buy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Loan cut on a Buy slip (Vivek's Option 1, offset): interest first, then principal, reduces the payout. */
class BuyLoanCutTest {
    // 10 bags x 50 kg x 2500/q = 12,500 gross; labour 100, commission rounds net to 12,310 (see BuyNewSlipRulesTest math)
    private fun form() = BuyEntryFormState().apply {
        farmerName = "TEST Ramesh"
        updatePhone("9876543210")
        village = "TEST गाँव"
        bags = "10"; weightPerBagKg = "50"; ratePerQuintal = "2500"
        setLoan(lendId = "L1", principalPaisa = 500_000L, interestDuePaisa = 30_000L)
    }

    @Test
    fun noCutLeavesPayoutUnchanged() {
        val f = form()
        assertEquals(f.aggregatePreview().netPayablePaisa, f.aggregatePreview().payoutPaisa)
        assertNull(f.buildLoanCut("shop", 1L))
    }

    @Test
    fun cutPaysInterestFirstThenPrincipal() {
        val f = form().apply { loanCut = "3000" }
        val split = f.loanCutSplit()
        assertEquals(30_000L, split.interestPaisa)
        assertEquals(270_000L, split.principalPaisa)
        val s = f.buildLoanCut("shop", 5L)!!
        assertEquals("L1", s.lendId)
        assertEquals(300_000L, s.amountPaisa)
        assertEquals(30_000L, s.interestPaisa)
        assertEquals(270_000L, s.principalPaisa)
    }

    @Test
    fun cutAndAdvanceBothReducePayoutAndPaid() {
        val f = form().apply { loanCut = "3000"; advancePaid = "2000" }
        val agg = f.aggregatePreview()
        assertEquals(agg.netPayablePaisa - 200_000L - 300_000L, agg.payoutPaisa)
        assertEquals(500_000L, f.toBuyEntry("shop").paidPaisa)
    }

    @Test
    fun cutCannotExceedWhatIsOwed() {
        val f = form().apply { loanCut = "5301" }
        assertFalse(f.canSave())
        assertTrue(f.loanCutError() != null)
        assertTrue(form().apply { loanCut = "5300" }.canSave())
    }

    @Test
    fun cutPlusAdvanceCannotExceedNetPayable() {
        val f = form().apply { loanCut = "5000"; advancePaid = "9000" }
        assertFalse(f.canSave())
    }
}
