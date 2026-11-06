package com.appwork.mandisamiti.ui.components

import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PartyNameCheckTest {
    private fun party(id: String, name: String) =
        Party(id = id, shopId = "s", name = name, village = null, partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L)

    private val parties = listOf(party("p1", "TEST Ramesh"), party("p2", "TEST Suresh"))

    @Test fun selectedPartyNeedsNoCheck() {
        assertIs<TypedPartyCheck.Selected>(checkTypedParty("anything", "p1", parties))
    }

    @Test fun partialNameNeedsConfirmAndOffersSimilar() {
        val r = checkTypedParty("TEST R", null, parties)
        assertIs<TypedPartyCheck.NeedsConfirm>(r)
        assertEquals(listOf("TEST Ramesh"), r.similar.map { it.name })
    }

    @Test fun exactNameResolvesToExistingPartyIgnoringCaseAndSpaces() {
        val r = checkTypedParty("  test ramesh ", null, parties)
        assertIs<TypedPartyCheck.ExactMatch>(r)
        assertEquals("p1", r.party.id)
    }

    @Test fun unknownNameNeedsConfirmWithNoSimilar() {
        val r = checkTypedParty("TEST Nobody", null, parties)
        assertIs<TypedPartyCheck.NeedsConfirm>(r)
        assertEquals(emptyList(), r.similar)
    }

    @Test fun hindiNameMatchesExactly() {
        val r = checkTypedParty("रामवीर सिंह", null, listOf(party("h1", "रामवीर सिंह")))
        assertIs<TypedPartyCheck.ExactMatch>(r)
    }

    @Test fun blankIsBlank() {
        assertIs<TypedPartyCheck.Blank>(checkTypedParty("   ", null, parties))
    }

    @Test fun twoPartiesWithSameNameAreNotAutoPicked() {
        val dup = listOf(party("a", "TEST Ramesh"), party("b", "TEST Ramesh"))
        assertIs<TypedPartyCheck.NeedsConfirm>(checkTypedParty("TEST Ramesh", null, dup))
    }

    @Test fun typedPhoneResolvesToThatParty() {
        val withPhone = listOf(party("p1", "TEST Ramesh").copy(phone = "9000000001"))
        val r = checkTypedParty("9000000001", null, withPhone)
        assertIs<TypedPartyCheck.ExactMatch>(r)
        assertEquals("p1", r.party.id)
    }
}
