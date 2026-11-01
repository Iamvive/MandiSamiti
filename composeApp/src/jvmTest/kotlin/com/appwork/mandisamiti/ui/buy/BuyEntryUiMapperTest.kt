package com.appwork.mandisamiti.ui.buy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** FormState to BuyEntryUiState: what the A3 screen renders. */
class BuyEntryUiMapperTest {
    private fun form() = BuyEntryFormState().apply {
        farmerName = "TEST Ramesh"
        updatePhone("9876543210")
        village = "TEST गाँव"
        bags = "10"; weightPerBagKg = "50"; ratePerQuintal = "2500"
        setLoan(lendId = "L1", principalPaisa = 500_000L, interestDuePaisa = 30_000L)
    }

    @Test
    fun partyAndSlipAreMapped() {
        val f = form().apply { selectedFarmerId = "P1" }
        val ui = f.toUiState()
        assertEquals("9876543210", ui.party.phone)
        assertEquals("TEST Ramesh", ui.party.name)
        assertEquals("P1", ui.party.selectedPartyId)
        val agg = f.aggregatePreview()
        assertEquals(agg.netPayablePaisa, ui.slip.netPayablePaisa)
        assertEquals(agg.labourPaisa, ui.slip.labourPaisa)
        assertEquals(1, ui.lots.size)
        assertEquals(agg.totalGrossPaisa, ui.lots[0].grossPaisa)
        assertEquals("10", ui.lots[0].bags)
    }

    @Test
    fun loanCutSplitsAndLowersPayout() {
        val f = form().apply { loanCut = "3000" }
        val ui = f.toUiState()
        assertEquals(500_000L, ui.loanDeposit.loanPrincipalPaisa)
        assertEquals(30_000L, ui.loanDeposit.loanInterestDuePaisa)
        assertEquals("3000", ui.loanDeposit.loanCutText)
        assertEquals(30_000L, ui.loanDeposit.cutToInterestPaisa)
        assertEquals(270_000L, ui.loanDeposit.cutToPrincipalPaisa)
        assertEquals(f.aggregatePreview().payoutPaisa, ui.payoutPaisa)
        assertTrue(ui.loanDeposit.hasAnything)
        assertTrue(ui.canSave)
        assertNull(ui.saveBlockedReason)
    }

    @Test
    fun overCutBlocksSaveWithHindiError() {
        val ui = form().apply { loanCut = "9000" }.toUiState()
        assertNotNull(ui.loanDeposit.loanCutError)
        assertFalse(ui.canSave)
        assertEquals(ui.loanDeposit.loanCutError, ui.saveBlockedReason)
    }

    @Test
    fun emptyFormShowsNoFieldErrorsButCannotSave() {
        val ui = BuyEntryFormState().toUiState()
        assertNull(ui.party.nameError)
        assertNull(ui.lots[0].bagsError)
        assertNull(ui.lots[0].rateError)
        assertFalse(ui.canSave)
        assertFalse(ui.loanDeposit.hasAnything)
    }

    @Test
    fun badBagsShowsLotError() {
        val f = form().apply { bags = "0" }
        assertNotNull(f.toUiState().lots[0].bagsError)
    }
}
