package com.appwork.mandisamiti.ui.udhaar

import com.appwork.mandisamiti.domain.usecase.UdhaarOverpaymentException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UdhaarRepayFormStateTest {

    private val todayMs = 1700000000000L
    private val loanDateMs = 1699000000000L

    @Test
    fun blankOrZeroOrNonNumericAmountIsInvalid() {
        val state = UdhaarRepayFormState(
            totalDuePaisa = 5000_00L,
            todayMs = todayMs,
            minDateMs = loanDateMs,
        )

        // Blank
        state.amountRupees = ""
        assertFalse(state.isValid)
        assertNotNull(state.validationReason(isEnglish = true))
        assertNotNull(state.validationReason(isEnglish = false))

        // Zero
        state.amountRupees = "0"
        assertFalse(state.isValid)
        assertEquals("Amount must be greater than 0", state.validationReason(isEnglish = true))
        assertEquals("राशि 0 से अधिक होनी चाहिए", state.validationReason(isEnglish = false))

        // Negative
        state.amountRupees = "-100"
        assertFalse(state.isValid)

        // Non-numeric
        state.amountRupees = "abc"
        assertFalse(state.isValid)
        assertEquals("Enter valid amount", state.validationReason(isEnglish = true))
        assertEquals("मान्य राशि दर्ज करें", state.validationReason(isEnglish = false))
    }

    @Test
    fun amountAboveDueIsInvalidAndExactDueIsValid() {
        val state = UdhaarRepayFormState(
            totalDuePaisa = 5000_00L,
            todayMs = todayMs,
            minDateMs = loanDateMs,
        )

        // Above due: 5001 > 5000
        state.amountRupees = "5001"
        assertFalse(state.isValid)
        assertEquals("Only ₹5,000 is due", state.validationReason(isEnglish = true))
        assertEquals("बाकी सिर्फ ₹5,000 है", state.validationReason(isEnglish = false))

        // Exactly due: 5000 == 5000
        state.amountRupees = "5000"
        assertTrue(state.isValid)
        assertNull(state.validationReason())
        assertEquals(5000_00L, state.paymentPaisa)

        // Partial due: 2500 < 5000
        state.amountRupees = "2500"
        assertTrue(state.isValid)
        assertNull(state.validationReason())
        assertEquals(2500_00L, state.paymentPaisa)
    }

    @Test
    fun decimalsUpToTwoPlacesSupportedForOddInterestDue() {
        val state = UdhaarRepayFormState(
            totalDuePaisa = 1250_75L,
            todayMs = todayMs,
            minDateMs = loanDateMs,
        )

        state.amountRupees = "1250.75"
        assertTrue(state.isValid)
        assertEquals(1250_75L, state.paymentPaisa)

        // Over by 1 paisa
        state.amountRupees = "1250.76"
        assertFalse(state.isValid)
        assertEquals("Only ₹1,250.75 is due", state.validationReason(isEnglish = true))
        assertEquals("बाकी सिर्फ ₹1,250.75 है", state.validationReason(isEnglish = false))
    }

    @Test
    fun futureDateIsInvalid() {
        val state = UdhaarRepayFormState(
            totalDuePaisa = 5000_00L,
            todayMs = todayMs,
            minDateMs = loanDateMs,
        )
        state.amountRupees = "1000"
        state.dateMs = todayMs + 86400000L * 2

        assertFalse(state.isValid)
        assertEquals("Date cannot be in the future", state.validationReason(isEnglish = true))
        assertEquals("भविष्य की तारीख अमान्य है", state.validationReason(isEnglish = false))
    }

    @Test
    fun dateEarlierThanMinDateIsInvalid() {
        val state = UdhaarRepayFormState(
            totalDuePaisa = 5000_00L,
            todayMs = todayMs,
            minDateMs = loanDateMs,
        )
        state.amountRupees = "1000"
        state.dateMs = loanDateMs - 86400000L

        assertFalse(state.isValid)
        assertEquals("Date cannot be before the loan date", state.validationReason(isEnglish = true))
        assertEquals("तारीख उधार की तारीख से पहले नहीं हो सकती", state.validationReason(isEnglish = false))

        // Date equal to minDateMs is valid
        state.dateMs = loanDateMs
        assertTrue(state.isValid)
        assertNull(state.validationReason())
    }

    @Test
    fun onOverpaymentSetsMessageWithDueAmount() {
        val state = UdhaarRepayFormState(
            totalDuePaisa = 5000_00L,
            todayMs = todayMs,
            minDateMs = loanDateMs,
        )
        state.amountRupees = "4000"
        assertTrue(state.isValid)

        // Race condition: server reported that actual due was only 3500_00L
        state.onOverpayment(UdhaarOverpaymentException(3500_00L))
        assertEquals(3500_00L, state.totalDuePaisa)
        assertFalse(state.isValid)
        assertEquals("Only ₹3,500 is due", state.validationReason(isEnglish = true))
        assertEquals("बाकी सिर्फ ₹3,500 है", state.validationReason(isEnglish = false))
    }

    @Test
    fun dateChangeUpdatesDueAndRevalidatesAmount() {
        val state = UdhaarRepayFormState(
            totalDuePaisa = 5100_00L,
            todayMs = todayMs,
            minDateMs = loanDateMs,
        )
        state.amountRupees = "5050"
        assertTrue(state.isValid)

        // When date is changed to an earlier date with lower interest (total due drops to 5000_00L)
        state.updateTotalDue(5000_00L)
        assertFalse(state.isValid)
        assertEquals("Only ₹5,000 is due", state.validationReason(isEnglish = true))
        assertEquals("बाकी सिर्फ ₹5,000 है", state.validationReason(isEnglish = false))
    }
}
