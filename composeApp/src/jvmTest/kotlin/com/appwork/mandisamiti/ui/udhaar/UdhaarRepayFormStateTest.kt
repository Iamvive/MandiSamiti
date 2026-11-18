package com.appwork.mandisamiti.ui.udhaar

import com.appwork.mandisamiti.domain.usecase.UdhaarOverpaymentException
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
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

    @Test
    fun isSameOrAfterDayInKolkata() {
        val kolkata = TimeZone.of("Asia/Kolkata")
        val loanDateMs = LocalDateTime(2026, 11, 12, 10, 0).toInstant(kolkata).toEpochMilliseconds()

        // 11 Nov (one day before): invalid
        val pickedBeforeMs = LocalDate(2026, 11, 11).atStartOfDayIn(kolkata).toEpochMilliseconds()
        assertFalse(isSameOrAfterDay(pickedBeforeMs, loanDateMs, kolkata), "Day before loan must be invalid in Kolkata")

        // 12 Nov (same day): valid
        val pickedSameMs = LocalDate(2026, 11, 12).atStartOfDayIn(kolkata).toEpochMilliseconds()
        assertTrue(isSameOrAfterDay(pickedSameMs, loanDateMs, kolkata), "Same day as loan must be valid in Kolkata")

        // 13 Nov (next day): valid
        val pickedAfterMs = LocalDate(2026, 11, 13).atStartOfDayIn(kolkata).toEpochMilliseconds()
        assertTrue(isSameOrAfterDay(pickedAfterMs, loanDateMs, kolkata), "Day after loan must be valid in Kolkata")
    }

    @Test
    fun isSameOrAfterDayInLosAngeles() {
        val la = TimeZone.of("America/Los_Angeles")
        val loanDateMs = LocalDateTime(2026, 11, 12, 10, 0).toInstant(la).toEpochMilliseconds()

        // 11 Nov evening (e.g. 20:00 local time = 12 Nov 04:00 UTC): local day is 11 Nov, must be invalid
        val pickedBeforeEveningMs = LocalDateTime(2026, 11, 11, 20, 0).toInstant(la).toEpochMilliseconds()
        assertFalse(isSameOrAfterDay(pickedBeforeEveningMs, loanDateMs, la), "Day before loan (evening local) must be invalid in LA")

        // 11 Nov start of day: invalid
        val pickedBeforeMs = LocalDate(2026, 11, 11).atStartOfDayIn(la).toEpochMilliseconds()
        assertFalse(isSameOrAfterDay(pickedBeforeMs, loanDateMs, la), "Day before loan must be invalid in LA")

        // 12 Nov (same day): valid
        val pickedSameMs = LocalDate(2026, 11, 12).atStartOfDayIn(la).toEpochMilliseconds()
        assertTrue(isSameOrAfterDay(pickedSameMs, loanDateMs, la), "Same day as loan must be valid in LA")

        // 13 Nov (next day): valid
        val pickedAfterMs = LocalDate(2026, 11, 13).atStartOfDayIn(la).toEpochMilliseconds()
        assertTrue(isSameOrAfterDay(pickedAfterMs, loanDateMs, la), "Day after loan must be valid in LA")
    }

    @Test
    fun isSameOrAfterDayLoanAt18LocalPickedSameDayValid() {
        val kolkata = TimeZone.of("Asia/Kolkata")
        val loanDateMs = LocalDateTime(2026, 11, 12, 18, 0).toInstant(kolkata).toEpochMilliseconds()
        val pickedSameDayMs = LocalDate(2026, 11, 12).atStartOfDayIn(kolkata).toEpochMilliseconds()

        // Raw timestamp of picked start-of-day is earlier than 18:00
        assertTrue(pickedSameDayMs < loanDateMs)
        // But calendar day is identical, so it must be valid
        assertTrue(isSameOrAfterDay(pickedSameDayMs, loanDateMs, kolkata), "Same calendar day must be valid even if loan was recorded later in the day")
    }

    @Test
    fun normalisePickerDateRoundTrip() {
        // Material date picker returns midnight UTC of 12 Nov 2026
        val utc12NovMs = LocalDate(2026, 11, 12).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

        // In Asia/Kolkata: normalised local day is 12 Nov
        val kolkata = TimeZone.of("Asia/Kolkata")
        val kolkataMs = normalisePickerDate(utc12NovMs, kolkata)
        val kolkataDate = Instant.fromEpochMilliseconds(kolkataMs).toLocalDateTime(kolkata).date
        assertEquals(LocalDate(2026, 11, 12), kolkataDate)

        // In America/Los_Angeles: normalised local day is 12 Nov
        val la = TimeZone.of("America/Los_Angeles")
        val laMs = normalisePickerDate(utc12NovMs, la)
        val laDate = Instant.fromEpochMilliseconds(laMs).toLocalDateTime(la).date
        assertEquals(LocalDate(2026, 11, 12), laDate)
    }

    @Test
    fun updateTotalDuePreservesTypedAmountAndDate() {
        val state = UdhaarRepayFormState(
            totalDuePaisa = 5000_00L,
            todayMs = todayMs,
            minDateMs = loanDateMs,
        )
        state.amountRupees = "3500"
        val customDate = loanDateMs + 86400000L
        state.dateMs = customDate

        state.updateTotalDue(6000_00L)
        assertEquals("3500", state.amountRupees, "Typed amount must be preserved across updateTotalDue")
        assertEquals(customDate, state.dateMs, "Picked date must be preserved across updateTotalDue")
        assertEquals(6000_00L, state.totalDuePaisa)
        assertTrue(state.isValid)
    }

    @Test
    fun typedAmountAboveNewDueShowsMessageAndKeepsText() {
        val state = UdhaarRepayFormState(
            totalDuePaisa = 5000_00L,
            todayMs = todayMs,
            minDateMs = loanDateMs,
        )
        state.amountRupees = "4500"
        assertTrue(state.isValid)

        // Stream emits lower due: 4000
        state.updateTotalDue(4000_00L)
        assertEquals("4500", state.amountRupees, "Typed text must be preserved when due drops below it")
        assertFalse(state.isValid)
        assertEquals("Only ₹4,000 is due", state.validationReason(isEnglish = true))
        assertEquals("बाकी सिर्फ ₹4,000 है", state.validationReason(isEnglish = false))
    }

    @Test
    fun minDateMsUpdateRevalidatesDate() {
        val state = UdhaarRepayFormState(
            totalDuePaisa = 5000_00L,
            todayMs = todayMs,
            minDateMs = loanDateMs,
        )
        state.amountRupees = "1000"
        state.dateMs = loanDateMs
        assertTrue(state.isValid)

        // minDateMs increases to tomorrow
        state.minDateMs = loanDateMs + 86400000L
        assertFalse(state.isValid)
        assertEquals("Date cannot be before the loan date", state.validationReason(isEnglish = true))
        assertEquals("तारीख उधार की तारीख से पहले नहीं हो सकती", state.validationReason(isEnglish = false))
    }
}
