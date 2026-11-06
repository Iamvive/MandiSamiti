package com.appwork.mandisamiti.ui.buy

import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Phone-first lookup, missing-number prompt and rename rules (review, "Answered by Vivek"). */
class BuyPartyResolverTest {
    private fun party(id: String, name: String, phone: String?, village: String? = "TEST गाँव") =
        Party(id, "shop", name, phone, village, PartyType.FARMER, createdAt = 1, updatedAt = 1)

    private val ramesh = party("p1", "TEST रमेश", "9876543210")
    private val old = party("p2", "TEST सुरेश", null, null)
    private val all = listOf(ramesh, old)

    @Test
    fun phonePrefixFindsParty() {
        assertEquals(listOf("p1"), BuyPartyResolver.matches(all, "98765", "").map { it.id })
    }

    @Test
    fun shortPhoneDoesNotMatchAnything() {
        assertTrue(BuyPartyResolver.matches(all, "98", "").isEmpty())
    }

    @Test
    fun nameFindsOldPartyWithNoNumber() {
        assertEquals(listOf("p2"), BuyPartyResolver.matches(all, "", "सुरेश").map { it.id })
    }

    @Test
    fun pickingPartyWithNumberFillsEverything() {
        val f = BuyEntryFormState()
        BuyPartyResolver.pick(f, ramesh)
        assertEquals("TEST रमेश", f.farmerName)
        assertEquals("9876543210", f.phone)
        assertEquals("TEST गाँव", f.village)
        assertEquals("p1", f.selectedFarmerId)
        assertTrue(!BuyPartyResolver.needsPhonePrompt(f))
    }

    @Test
    fun pickingPartyWithoutNumberAsksForIt() {
        val f = BuyEntryFormState()
        BuyPartyResolver.pick(f, old)
        assertTrue(BuyPartyResolver.needsPhonePrompt(f))
        f.updatePhone("9000000001")
        assertTrue(!BuyPartyResolver.needsPhonePrompt(f))
    }

    @Test
    fun newPhoneCreatesPartyOnce() {
        val f = BuyEntryFormState().apply { farmerName = "TEST नया"; updatePhone("9111111111"); village = "TEST गाँव" }
        val toSave = BuyPartyResolver.partyToSave(f, "shop", null, now = 5, newId = "new1")!!
        assertEquals("new1", toSave.id)
        assertEquals("9111111111", toSave.phone)
        assertEquals("TEST नया", toSave.name)
    }

    @Test
    fun unchangedPartyIsNotWritten() {
        val f = BuyEntryFormState()
        BuyPartyResolver.pick(f, ramesh)
        assertNull(BuyPartyResolver.partyToSave(f, "shop", ramesh, now = 5))
    }

    @Test
    fun editedNameRenamesTheSameParty() {
        val f = BuyEntryFormState()
        BuyPartyResolver.pick(f, ramesh)
        f.farmerName = "TEST रमेश कुमार"
        assertTrue(BuyPartyResolver.willRename(f, ramesh))
        val toSave = BuyPartyResolver.partyToSave(f, "shop", ramesh, now = 5)!!
        assertEquals("p1", toSave.id)
        assertEquals("TEST रमेश कुमार", toSave.name)
        assertEquals(5L, toSave.updatedAt)
        assertEquals(0, toSave.syncStatus)
    }

    @Test
    fun enteredPhoneIsStoredOnOldParty() {
        val f = BuyEntryFormState()
        BuyPartyResolver.pick(f, old)
        f.updatePhone("9000000001"); f.village = "TEST गाँव"
        val toSave = BuyPartyResolver.partyToSave(f, "shop", old, now = 5)!!
        assertEquals("p2", toSave.id)
        assertEquals("9000000001", toSave.phone)
        assertEquals("TEST गाँव", toSave.village)
        assertNotEquals(old, toSave)
    }

    @Test
    fun existingIsFoundByPickedIdOrExactPhone() {
        assertEquals("p1", BuyPartyResolver.existing(all, "p1", "")?.id)
        assertEquals("p1", BuyPartyResolver.existing(all, null, "9876543210")?.id)
        assertEquals(null, BuyPartyResolver.existing(all, null, "98765"))
        assertEquals(null, BuyPartyResolver.existing(all, null, ""))
    }
}
