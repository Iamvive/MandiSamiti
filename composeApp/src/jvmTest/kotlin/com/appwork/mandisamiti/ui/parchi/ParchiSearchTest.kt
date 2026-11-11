package com.appwork.mandisamiti.ui.parchi

import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private data class Slip(val id: String, val partyId: String, val due: Long, val date: Long)

private fun party(id: String, name: String, phone: String? = null, village: String? = null) = Party(
    id = id, shopId = "s", name = name, phone = phone, village = village,
    partyType = PartyType.FARMER, createdAt = 0L, updatedAt = 0L
)

private fun search(q: String, slips: List<Slip>, parties: List<Party>) =
    filterSlipsByParty(q, slips, parties, { it.partyId }, { it.due }, { it.date })

class ParchiSearchTest {
    private val ramesh = party("p1", "Ramesh", "9876543210", "Nagla Mohar Singh")
    private val ramu = party("p2", "Ramu", "9123456780", "Demo village")
    private val slips = listOf(
        Slip("a", "p1", 1_000_00, 3),
        Slip("b", "p1", 0, 2),
        Slip("c", "p1", 500_00, 5),
        Slip("d", "p2", 700_00, 4)
    )

    @Test fun blankQueryReturnsNothing() =
        assertTrue(search("  ", slips, listOf(ramesh, ramu)).isEmpty())

    @Test fun nameMatchIsCaseInsensitiveAndTotalsUnpaidOnly() {
        val g = search("ramesh", slips, listOf(ramesh, ramu)).single()
        assertEquals(150_000L, g.totalDuePaisa)
        assertEquals(listOf("c", "a"), g.unpaid.map { it.id })
        assertEquals(listOf("b"), g.paid.map { it.id })
    }

    @Test fun sharedPrefixReturnsOneGroupPerParty() {
        val groups = search("ram", slips, listOf(ramesh, ramu))
        assertEquals(listOf("Ramesh", "Ramu"), groups.map { it.party.name })
    }

    @Test fun villageMatches() =
        assertEquals("Ramu", search("demo", slips, listOf(ramesh, ramu)).single().party.name)

    @Test fun partialPhoneMatches() =
        assertEquals("Ramesh", search("98765", slips, listOf(ramesh, ramu)).single().party.name)

    @Test fun plus91AndSpacesAreIgnored() =
        assertEquals("Ramesh", search("+91 98765 43210", slips, listOf(ramesh, ramu)).single().party.name)

    @Test fun fewerThanThreeDigitsMatchesNothing() =
        assertTrue(search("98", slips, listOf(ramesh, ramu)).isEmpty())

    @Test fun partyWithNoSlipsIsOmitted() =
        assertTrue(search("ramesh", emptyList(), listOf(ramesh)).isEmpty())
}
