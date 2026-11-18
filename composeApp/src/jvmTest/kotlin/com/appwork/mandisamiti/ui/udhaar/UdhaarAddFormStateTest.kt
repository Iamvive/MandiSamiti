package com.appwork.mandisamiti.ui.udhaar

import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.ui.lend.LendPartyResolver
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

class UdhaarAddFormStateTest {

    private val todayMs = 1700000000000L

    @Test
    fun invalidUntilPhoneHas10DigitsNameIsNonBlankAndAmountIsPositiveWholeNumber() {
        val state = UdhaarAddFormState(direction = LendDirection.GIVEN, todayMs = todayMs)

        // Initially empty
        assertFalse(state.isValid, "Empty form must be invalid")
        assertNotNull(state.validationReason())

        // Phone < 10 digits
        state.updatePhone("987654321")
        state.name = "Ramesh Kumar"
        state.amountRupees = "5000"
        assertFalse(state.isValid, "Phone with 9 digits must be invalid")

        // Valid phone, but blank name
        state.updatePhone("9876543210")
        state.name = "   "
        assertFalse(state.isValid, "Blank name must be invalid")

        // Valid phone and name, but amount is blank
        state.name = "Ramesh Kumar"
        state.amountRupees = ""
        assertFalse(state.isValid, "Blank amount must be invalid")

        // Amount is zero
        state.amountRupees = "0"
        assertFalse(state.isValid, "Zero amount must be invalid")

        // Amount is negative
        state.amountRupees = "-500"
        assertFalse(state.isValid, "Negative amount must be invalid")

        // Amount is decimal (not whole number)
        state.amountRupees = "500.50"
        assertFalse(state.isValid, "Decimal amount must be invalid")

        // Amount has non-digits
        state.amountRupees = "500abc"
        assertFalse(state.isValid, "Non-digit amount must be invalid")

        // Valid positive whole number
        state.amountRupees = "5000"
        assertTrue(state.isValid, "Valid phone, name, and whole amount must make form valid")
        assertNull(state.validationReason())
    }

    @Test
    fun edgeCase1_updatePhoneStripsNonDigitsAndTruncatesTo10() {
        val state = UdhaarAddFormState(direction = LendDirection.GIVEN, todayMs = todayMs)
        state.updatePhone("98-76 543210x")
        assertEquals("9876543210", state.phone)

        // Truncates to max 10 digits
        state.updatePhone("9876543210123")
        assertEquals("9876543210", state.phone)
    }

    @Test
    fun edgeCase4_amountWholeRupeesCapAndLeadingZeros() {
        val state = UdhaarAddFormState(direction = LendDirection.GIVEN, todayMs = todayMs)
        state.updatePhone("9876543210")
        state.name = "Ramesh Kumar"

        // Leading zeros fine: "007" -> 7 rupees -> 700 paisa
        state.amountRupees = "007"
        assertTrue(state.isValid)
        val form007 = state.toForm(shopId = "shop1", partyId = "party1")
        assertEquals(700L, form007.amountPaisa)

        // Max cap: 99,999,999 is valid
        state.amountRupees = "99999999"
        assertTrue(state.isValid)
        val formMax = state.toForm(shopId = "shop1", partyId = "party1")
        assertEquals(99_999_999_00L, formMax.amountPaisa)

        // Over cap: 100,000,000 is invalid
        state.amountRupees = "100000000"
        assertFalse(state.isValid, "Amount exceeding 9,99,99,999 must be invalid")
        assertEquals("राशि ₹9,99,99,999 से अधिक नहीं हो सकती", state.validationReason(isEnglish = false))
        assertEquals("Amount cannot exceed ₹9,99,99,999", state.validationReason(isEnglish = true))
    }

    @Test
    fun edgeCase5_futureDateIsInvalidAndSameOrPastDayAllowed() {
        val state = UdhaarAddFormState(direction = LendDirection.GIVEN, todayMs = todayMs)
        state.updatePhone("9876543210")
        state.name = "Ramesh Kumar"
        state.amountRupees = "5000"
        state.dateMs = todayMs
        assertTrue(state.isValid, "Today's date should be valid")

        state.dateMs = todayMs - 86400000L
        assertTrue(state.isValid, "Past date should be valid")

        state.dateMs = todayMs + 86400000L * 2
        assertFalse(state.isValid, "Future date must be invalid")
        assertEquals("भविष्य की तारीख अमान्य है", state.validationReason(isEnglish = false))
    }

    @Test
    fun edgeCase6_interestOffYieldsZeroEvenWithJunkRate() {
        val state = UdhaarAddFormState(direction = LendDirection.GIVEN, todayMs = todayMs)
        state.updatePhone("9876543210")
        state.name = "Ramesh Kumar"
        state.amountRupees = "5000"
        state.interestOn = false
        state.rateInput = "junk_non_numeric"

        assertTrue(state.isValid, "Form is valid when interest is off even if rateInput has junk")
        val form = state.toForm(shopId = "shop1", partyId = "party1")
        assertEquals(0.0, form.monthlyRatePercent, 0.0001)
    }

    @Test
    fun edgeCase7_interestOnWithValidAndInvalidRates() {
        val state = UdhaarAddFormState(direction = LendDirection.GIVEN, todayMs = todayMs)
        state.updatePhone("9876543210")
        state.name = "Ramesh Kumar"
        state.amountRupees = "5000"
        state.interestOn = true

        state.rateInput = "0"
        assertFalse(state.isValid, "Zero rate must be invalid")

        state.rateInput = "0.0"
        assertFalse(state.isValid, "0.0 rate must be invalid")

        state.rateInput = "-1.5"
        assertFalse(state.isValid, "Negative rate must be invalid")

        state.rateInput = "100.1"
        assertFalse(state.isValid, "Rate > 100 must be invalid")

        state.rateInput = "abc"
        assertFalse(state.isValid, "Non-numeric rate must be invalid")

        state.rateInput = ""
        assertFalse(state.isValid, "Empty rate must be invalid")

        state.rateInput = "2,5"
        assertTrue(state.isValid, "Rate with decimal comma must be valid")
        val formComma = state.toForm(shopId = "shop1", partyId = "party1")
        assertEquals(2.5, formComma.monthlyRatePercent, 0.0001)

        state.rateInput = "2.5"
        assertTrue(state.isValid, "Positive rate <= 100 must be valid")
        val formDot = state.toForm(shopId = "shop1", partyId = "party1")
        assertEquals(2.5, formDot.monthlyRatePercent, 0.0001)
    }

    @Test
    fun edgeCase8_forTakenFormGiveToOtherIsIgnoredAndReceiverFieldsAreNull() {
        val state = UdhaarAddFormState(direction = LendDirection.TAKEN, todayMs = todayMs)
        state.updatePhone("9876543210")
        state.name = "Ramesh Kumar"
        state.amountRupees = "5000"
        state.giveToOther = true
        state.receiverName = "" // blank receiver name would be invalid on GIVEN, but ignored on TAKEN
        state.receiverPhone = "9876543211"

        assertTrue(state.isValid, "On TAKEN form, giveToOther is ignored and blank receiverName is not invalid")

        val form = state.toForm(shopId = "shop1", partyId = "party1")
        assertNull(form.receiverName, "On TAKEN form, receiverName in form must be null")
        assertNull(form.receiverPhone, "On TAKEN form, receiverPhone in form must be null")
    }

    @Test
    fun edgeCase9_receiverNameAndReceiverPhoneValidationOnGiven() {
        val state = UdhaarAddFormState(direction = LendDirection.GIVEN, todayMs = todayMs)
        state.updatePhone("9876543210")
        state.name = "Ramesh Kumar"
        state.amountRupees = "5000"
        state.giveToOther = true

        // Blank name is invalid
        state.receiverName = "   "
        assertFalse(state.isValid, "Blank receiver name must be invalid")

        // Name valid, phone blank -> valid and phone becomes null in form
        state.receiverName = "Suresh"
        state.receiverPhone = ""
        assertTrue(state.isValid)
        val formBlankPhone = state.toForm(shopId = "shop1", partyId = "party1")
        assertEquals("Suresh", formBlankPhone.receiverName)
        assertNull(formBlankPhone.receiverPhone)

        // Non-blank phone with < 10 digits -> invalid
        state.updateReceiverPhone("987654")
        assertFalse(state.isValid, "Receiver phone with < 10 digits must be invalid")

        // Non-blank phone with 10 digits -> valid
        state.updateReceiverPhone("9876543211")
        assertTrue(state.isValid)
        val formWithPhone = state.toForm(shopId = "shop1", partyId = "party1")
        assertEquals("Suresh", formWithPhone.receiverName)
        assertEquals("9876543211", formWithPhone.receiverPhone)
    }

    @Test
    fun fix2_partyToSaveRenamesExistingPartyOnlyWhenNameDiffersPreservingVillage() {
        val existing = Party(
            id = "p1",
            shopId = "shop1",
            name = "Old Farmer",
            phone = "9876543210",
            village = "Rampur",
            partyType = PartyType.FARMER,
            createdAt = 1000L,
            updatedAt = 1000L,
        )

        // Same name: returns null (no redundant write)
        val noChange = LendPartyResolver.partyToSave(
            name = "Old Farmer",
            phone = "9876543210",
            village = existing.village,
            shopId = "shop1",
            existing = existing,
            now = 2000L,
        )
        assertNull(noChange, "When name is unchanged, partyToSave must return null")

        // Changed name: returns existing party updated with new name and preserved village
        val changed = LendPartyResolver.partyToSave(
            name = "New Farmer Name",
            phone = "9876543210",
            village = existing.village,
            shopId = "shop1",
            existing = existing,
            now = 2000L,
        )
        assertNotNull(changed)
        assertEquals("p1", changed.id)
        assertEquals("New Farmer Name", changed.name)
        assertEquals("Rampur", changed.village)
        assertEquals(2000L, changed.updatedAt)
        assertEquals(0, changed.syncStatus)

        // Notice condition (willRename)
        val willRenameSame = existing.name != "Old Farmer".trim()
        assertFalse(willRenameSame)
        val willRenameDiff = existing.name != "New Farmer Name".trim()
        assertTrue(willRenameDiff)
    }

    @Test
    fun fix3_interestRateValidationMessageSaysAbove0UpTo100() {
        val state = UdhaarAddFormState(direction = LendDirection.GIVEN, todayMs = todayMs)
        state.updatePhone("9876543210")
        state.name = "Ramesh Kumar"
        state.amountRupees = "5000"
        state.interestOn = true
        state.rateInput = "0"

        assertFalse(state.isValid)
        assertEquals("Enter valid interest rate (above 0 up to 100)", state.validationReason(isEnglish = true))
        assertEquals("मान्य ब्याज दर दर्ज करें (0 से अधिक और 100 तक)", state.validationReason(isEnglish = false))
    }

    @Test
    fun isSameOrPastDayInKolkataAndLosAngeles() {
        val kolkata = TimeZone.of("Asia/Kolkata")
        val todayKolkataMs = LocalDateTime(2026, 11, 12, 12, 0).toInstant(kolkata).toEpochMilliseconds()

        // Yesterday: valid
        val yesterdayKolkataMs = LocalDate(2026, 11, 11).atStartOfDayIn(kolkata).toEpochMilliseconds()
        assertTrue(isSameOrPastDay(yesterdayKolkataMs, todayKolkataMs, kolkata), "Yesterday must be valid in Kolkata")

        // Today: valid
        val todayStartKolkataMs = LocalDate(2026, 11, 12).atStartOfDayIn(kolkata).toEpochMilliseconds()
        assertTrue(isSameOrPastDay(todayStartKolkataMs, todayKolkataMs, kolkata), "Today must be valid in Kolkata")

        // Tomorrow: invalid
        val tomorrowKolkataMs = LocalDate(2026, 11, 13).atStartOfDayIn(kolkata).toEpochMilliseconds()
        assertFalse(isSameOrPastDay(tomorrowKolkataMs, todayKolkataMs, kolkata), "Tomorrow must be invalid in Kolkata")

        // In a positive offset timezone like Asia/Tokyo (UTC+9) where tomorrow morning is today in UTC:
        val tokyo = TimeZone.of("Asia/Tokyo")
        val todayTokyoMs = LocalDateTime(2026, 11, 12, 12, 0).toInstant(tokyo).toEpochMilliseconds()
        // Tomorrow at 02:00 in Tokyo is 12 Nov 17:00 in UTC. Must be INVALID because local day is 13 Nov.
        val tomorrowEarlyTokyoMs = LocalDateTime(2026, 11, 13, 2, 0).toInstant(tokyo).toEpochMilliseconds()
        assertFalse(isSameOrPastDay(tomorrowEarlyTokyoMs, todayTokyoMs, tokyo), "Tomorrow early morning in Tokyo must be invalid even if its UTC date is today")

        // In America/Los_Angeles (UTC-8)
        val la = TimeZone.of("America/Los_Angeles")
        val todayLaMs = LocalDateTime(2026, 11, 12, 12, 0).toInstant(la).toEpochMilliseconds()

        val yesterdayLaMs = LocalDate(2026, 11, 11).atStartOfDayIn(la).toEpochMilliseconds()
        assertTrue(isSameOrPastDay(yesterdayLaMs, todayLaMs, la), "Yesterday must be valid in LA")

        val todayLaLateMs = LocalDateTime(2026, 11, 12, 23, 0).toInstant(la).toEpochMilliseconds()
        assertTrue(isSameOrPastDay(todayLaLateMs, todayLaMs, la), "Today late evening must be valid in LA")

        val tomorrowLaMs = LocalDate(2026, 11, 13).atStartOfDayIn(la).toEpochMilliseconds()
        assertFalse(isSameOrPastDay(tomorrowLaMs, todayLaMs, la), "Tomorrow must be invalid in LA")
    }
}
