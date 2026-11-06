package com.appwork.mandisamiti.ui.lend

import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LendEntryDirectionBTest {

    @Test
    fun phoneIsRequiredToSave() {
        val form = LendEntryFormState().apply {
            partyName = "सुरेश कुमार"
            village = "खेडा"
            amountRupees = "5000"
        }
        assertFalse(form.canSave())
        assertEquals("10 अंकों का नंबर डालें", form.validationReason(isEnglish = false))
        assertEquals("Add number to save", form.validationReason(isEnglish = true))

        form.updatePhone("12345")
        assertFalse(form.canSave())
        assertEquals("मान्य 10 अंकों का मोबाइल नंबर दर्ज करें", form.validationReason(isEnglish = false))
        assertEquals("Enter valid 10-digit number", form.validationReason(isEnglish = true))

        form.updatePhone("9876543210")
        assertTrue(form.canSave())
    }

    @Test
    fun nameAndVillageAreRequiredToSave() {
        val form = LendEntryFormState().apply {
            updatePhone("9876543210")
            amountRupees = "2500"
        }
        assertFalse(form.canSave())
        assertEquals("नाम और गांव डालने पर सुरक्षित होगा", form.validationReason(isEnglish = false))
        assertEquals("Add name and village to save", form.validationReason(isEnglish = true))

        form.partyName = "रमेश"
        assertFalse(form.canSave())
        assertEquals("नाम और गांव डालने पर सुरक्षित होगा", form.validationReason(isEnglish = false))

        form.village = "रामपुर"
        assertTrue(form.canSave())
    }

    @Test
    fun moneyLoanRequiresPositiveAmount() {
        val form = LendEntryFormState().apply {
            updatePhone("9876543210")
            partyName = "दिनेश"
            village = "इन्दौर"
            lendType = LendType.MONEY
            amountRupees = ""
        }
        assertFalse(form.canSave())
        assertEquals("उधार राशि (रु) दर्ज करें", form.validationReason(isEnglish = false))
        assertEquals("Enter loan amount (₹)", form.validationReason(isEnglish = true))

        form.amountRupees = "0"
        assertFalse(form.canSave())

        form.amountRupees = "1000"
        assertTrue(form.canSave())
    }

    @Test
    fun grainLoanRequiresCommodityAndWeight() {
        val form = LendEntryFormState().apply {
            updatePhone("9876543210")
            partyName = "दिनेश"
            village = "इन्दौर"
            lendType = LendType.GRAIN
        }
        assertFalse(form.canSave())
        assertEquals("जिंस और कुल वजन दर्ज करें", form.validationReason(isEnglish = false))
        assertEquals("Enter crop and weight", form.validationReason(isEnglish = true))

        form.commodityName = "गेहूं"
        assertFalse(form.canSave())

        form.weightKg = "50"
        form.bagsCount = "1"
        assertTrue(form.canSave())
    }

    @Test
    fun pickPartyFillsPhoneNameAndVillage() {
        val form = LendEntryFormState()
        val party = Party(
            id = "p-101",
            shopId = "shop-1",
            name = "पवन कुमार",
            phone = "6398207680",
            village = "बलरई",
            partyType = PartyType.FARMER,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        LendPartyResolver.pick(form, party)

        assertEquals("p-101", form.selectedPartyId)
        assertEquals("पवन कुमार", form.partyName)
        assertEquals("6398207680", form.phone)
        assertEquals("बलरई", form.village)
        assertFalse(LendPartyResolver.needsPhonePrompt(form))
    }

    @Test
    fun oldPartyWithoutPhonePromptsForNumber() {
        val form = LendEntryFormState()
        val oldParty = Party(
            id = "p-old",
            shopId = "shop-1",
            name = "पुराना किसान",
            phone = null,
            village = "गांव",
            partyType = PartyType.FARMER,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        LendPartyResolver.pick(form, oldParty)
        assertEquals("p-old", form.selectedPartyId)
        assertEquals("", form.phone)
        assertTrue(LendPartyResolver.needsPhonePrompt(form))
        assertFalse(form.canSave())
    }

    @Test
    fun partyToSaveCreatesNewPartyWithFarmerType() {
        val form = LendEntryFormState().apply {
            updatePhone("9876543210")
            partyName = "नया कर्जदार"
            village = "सोनकच्छ 123"
        }

        val toSave = LendPartyResolver.partyToSave(form, "shop-test", existing = null, now = 2000L, newId = "new-p")
        assertNotNull(toSave)
        assertEquals("new-p", toSave.id)
        assertEquals("shop-test", toSave.shopId)
        assertEquals("नया कर्जदार", toSave.name)
        assertEquals("9876543210", toSave.phone)
        assertEquals("सोनकच्छ", toSave.village) // sanitized village removes digits
        assertEquals(PartyType.FARMER, toSave.partyType)
    }

    @Test
    fun partyToSaveReturnsNullIfNoChanges() {
        val existing = Party(
            id = "p-existing",
            shopId = "shop-test",
            name = "सुरेश",
            phone = "9826012345",
            village = "देवास",
            partyType = PartyType.FARMER,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val form = LendEntryFormState().apply {
            updatePhone("9826012345")
            partyName = "सुरेश"
            village = "देवास"
        }

        val toSave = LendPartyResolver.partyToSave(form, "shop-test", existing = existing, now = 2000L)
        assertNull(toSave)
    }

    @Test
    fun toLendEntryBuildsCorrectModel() {
        val form = LendEntryFormState().apply {
            updatePhone("9876543210")
            partyName = "सुरेश"
            village = "देवास"
            lendType = LendType.MONEY
            amountRupees = "7500"
            expectedReturnMillis = 50000L
            notes = "बीज के लिए"
        }

        val entry = form.toLendEntry("shop-1", "party-123", now = 10000L, id = "lend-99")
        assertEquals("lend-99", entry.id)
        assertEquals("shop-1", entry.shopId)
        assertEquals("party-123", entry.partyId)
        assertEquals(LendType.MONEY, entry.lendType)
        assertEquals(750000L, entry.amountPaisa)
        assertEquals(50000L, entry.expectedReturnDate)
        assertEquals("बीज के लिए", entry.notes)
    }
}
