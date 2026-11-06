package com.appwork.mandisamiti.ui.buy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BuyEntryLoanSectionTest {

    @Test
    fun loanSectionRendersStateFromMapper() {
        val form = BuyEntryFormState().apply {
            farmerName = "TEST Farmer"
            updatePhone("9876543210")
            village = "TEST Village"
            bags = "20"
            weightPerBagKg = "50"
            ratePerQuintal = "2400"
            setLoan(lendId = "LEND_1", principalPaisa = 400_000L, interestDuePaisa = 25_000L)
            loanCut = "1500"
        }

        val state = form.toUiState()

        // LoanDepositState verification
        assertTrue(state.loanDeposit.hasAnything)
        assertEquals(400_000L, state.loanDeposit.loanPrincipalPaisa)
        assertEquals(25_000L, state.loanDeposit.loanInterestDuePaisa)
        assertEquals(0L, state.loanDeposit.depositPaisa)
        assertEquals("1500", state.loanDeposit.loanCutText)
        assertNull(state.loanDeposit.loanCutError)

        // Split: interest first (25,000 paisa = Rs 250), then principal (125,000 paisa = Rs 1,250)
        assertEquals(25_000L, state.loanDeposit.cutToInterestPaisa)
        assertEquals(125_000L, state.loanDeposit.cutToPrincipalPaisa)

        // Payout reduction: net is 23,630 (24,000 gross - 200 labour - 170 comm). Loan cut Rs 1,500 reduces payout to Rs 22,130
        assertEquals(23_630_00L, state.slip.netPayablePaisa)
        assertEquals(22_130_00L, state.payoutPaisa)
    }

    @Test
    fun loanSectionReportsErrorWhenCutExceedsDebt() {
        val form = BuyEntryFormState().apply {
            farmerName = "TEST Farmer"
            updatePhone("9876543210")
            village = "TEST Village"
            bags = "20"
            weightPerBagKg = "50"
            ratePerQuintal = "2400"
            setLoan(lendId = "LEND_1", principalPaisa = 100_000L, interestDuePaisa = 10_000L) // Total Rs 1,100
            loanCut = "1200" // Exceeds Rs 1,100
        }

        val state = form.toUiState()
        assertNotNull(state.loanDeposit.loanCutError)
        assertEquals("कटौती बकाया उधार से अधिक है", state.loanDeposit.loanCutError)
        assertFalse(state.canSave)
    }

    @Test
    fun depositPaisaIsReadOnlyAndDepositTextIsUnused() {
        val state = LoanDepositState(
            loanPrincipalPaisa = 0L,
            loanInterestDuePaisa = 0L,
            depositPaisa = 50_000L, // Rs 500 deposit
            loanCutText = "",
            depositUseText = ""
        )

        assertTrue(state.hasAnything)
        assertEquals(50_000L, state.depositPaisa)
        assertEquals("", state.depositUseText)
        assertNull(state.depositUseError)
    }
}
