package com.appwork.mandisamiti.ui.components

import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PartyLookupTest {
    private fun party(id: String, name: String, phone: String?, village: String? = null) = Party(
        id = id, shopId = "s", name = name, phone = phone, village = village,
        partyType = PartyType.values().first(), createdAt = 0, updatedAt = 0
    )

    private val parties = listOf(
        party("1", "Ramesh Kumar", "9876543210", "Sikar"),
        party("2", "Suresh", "9871111111", "Jaipur"),
        party("3", "रमेश यादव", null, "Alwar")
    )

    @Test
    fun nameQueryMatchesNameCaseInsensitive() {
        assertEquals(listOf("1"), PartyLookup.matches(parties, "ramesh").map { it.id })
    }

    @Test
    fun hindiNameQueryMatches() {
        assertEquals(listOf("3"), PartyLookup.matches(parties, "रमेश").map { it.id })
    }

    @Test
    fun villageQueryMatches() {
        assertEquals(listOf("2"), PartyLookup.matches(parties, "jaip").map { it.id })
    }

    @Test
    fun digitPrefixMatchesPhone() {
        assertEquals(listOf("1", "2"), PartyLookup.matches(parties, "987").map { it.id }.sorted())
    }

    @Test
    fun oneCharacterQueryMatchesNothing() {
        assertEquals(emptyList(), PartyLookup.matches(parties, "r"))
    }

    @Test
    fun numberModeKeepsOnlyTenDigitsAndEmitsPhone() {
        val r = PartyLookup.onInput("98a76-54321012", textMode = false)
        assertEquals("9876543210", r.query)
        assertEquals("9876543210", r.phoneToEmit)
    }

    @Test
    fun textModeKeepsRawTextAndNeverEmitsPhone() {
        val r = PartyLookup.onInput("रमेश 12", textMode = true)
        assertEquals("रमेश 12", r.query)
        assertNull(r.phoneToEmit)
    }
}
