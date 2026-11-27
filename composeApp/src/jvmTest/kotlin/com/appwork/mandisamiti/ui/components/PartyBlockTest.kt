package com.appwork.mandisamiti.ui.components

import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PartyBlockTest {
    private fun party(id: String, name: String, phone: String?, village: String? = "TEST गाँव") =
        Party(id, "shop", name, phone, village, PartyType.FARMER, createdAt = 1, updatedAt = 1)

    private val ramesh = party("p1", "TEST रमेश", "9876543210")
    private val noPhone = party("p2", "TEST सुरेश", null)
    private val all = listOf(ramesh, noPhone)

    @Test
    fun emptyFormShowsOnlyLookup() {
        assertEquals(PartyBlockState.Lookup, partyBlockState(all, null, "", false))
        assertEquals(PartyBlockState.Lookup, partyBlockState(all, null, "98765", false))
    }

    @Test
    fun matchingNumberOrPickShowsSaved() {
        assertEquals(PartyBlockState.Saved(ramesh), partyBlockState(all, null, "9876543210", false))
        assertEquals(PartyBlockState.Saved(noPhone), partyBlockState(all, "p2", "", false))
    }

    @Test
    fun unknownNumberOrNewRowShowsNewFields() {
        assertEquals(PartyBlockState.New, partyBlockState(all, null, "9111111111", false))
        assertEquals(PartyBlockState.New, partyBlockState(all, null, "", true))
    }

    @Test
    fun changingNumberMakesSelectionStale() {
        assertTrue(isSelectionStale(all, "p1", "987654321"))
        assertFalse(isSelectionStale(all, "p1", "9876543210"))
    }

    @Test
    fun addingMissingNumberKeepsSelection() {
        assertFalse(isSelectionStale(all, "p2", "9000000001"))
        assertFalse(isSelectionStale(all, null, "9000000001"))
    }

    @Test
    fun sameNameAndVillageOnNewNumberIsCandidate() {
        assertEquals(ramesh, samePersonCandidate(all, null, "9111111111", " test रमेश ", "test गाँव"))
    }

    @Test
    fun noCandidateWhenPickedKnownNumberOrDifferentVillage() {
        assertNull(samePersonCandidate(all, "p1", "9876543210", "TEST रमेश", "TEST गाँव"))
        assertNull(samePersonCandidate(all, null, "9876543210", "TEST रमेश", "TEST गाँव"))
        assertNull(samePersonCandidate(all, null, "9111111111", "TEST रमेश", "दूसरा"))
        assertNull(samePersonCandidate(all, null, "9111111111", "", ""))
    }
}
