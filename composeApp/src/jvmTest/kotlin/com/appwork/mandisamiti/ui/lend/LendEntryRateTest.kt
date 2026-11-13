package com.appwork.mandisamiti.ui.lend

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class LendEntryRateTest {

    @Test
    fun parsedRate_acceptsDotAndComma() {
        val form = LendEntryFormState()
        assertEquals(2.0, form.parsedRate)
        form.rateInput = "1,5"
        assertEquals(1.5, form.parsedRate)
    }

    @Test
    fun parsedRate_rejectsInvalidAndOutOfRange() {
        val form = LendEntryFormState()
        for (bad in listOf("", "0", "abc", "101")) {
            form.rateInput = bad
            assertNull(form.parsedRate, "expected null for '$bad'")
        }
    }

    @Test
    fun invalidRate_blocksSaveForMoneyLoan() {
        val form = LendEntryFormState().apply {
            updatePhone("9876543210")
            partyName = "सुरेश"
            village = "खेडा"
            amountRupees = "5000"
            rateInput = "0"
        }
        assertFalse(form.canSave())
    }

    @Test
    fun reset_restoresDefaultRate() {
        val form = LendEntryFormState().apply { rateInput = "3" }
        form.reset()
        assertEquals("2", form.rateInput)
    }

    @Test
    fun newLoanFormStartsAtTwoPercentAndAcceptsOverride() {
        val form = LendEntryFormState()
        assertEquals("2", form.rateInput)
        assertEquals(2.0, form.parsedRate)
        form.rateInput = "1.5"
        assertEquals(1.5, form.parsedRate)
        form.rateInput = "0"
        assertNull(form.parsedRate)
        form.reset()
        assertEquals("2", form.rateInput)
    }
}
