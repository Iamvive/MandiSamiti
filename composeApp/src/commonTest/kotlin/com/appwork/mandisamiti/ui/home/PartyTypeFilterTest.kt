package com.appwork.mandisamiti.ui.home

import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import kotlin.test.Test
import kotlin.test.assertEquals

class PartyTypeFilterTest {
    private fun p(id: String, name: String, type: PartyType, bal: Long) = PartyWithBalance(
        Party(id = id, shopId = "s", name = name, partyType = type, createdAt = 0, updatedAt = 0),
        bal
    )

    private val all = listOf(
        p("1", "Ramesh", PartyType.FARMER, -500),
        p("2", "Suresh", PartyType.FARMER, 300),
        p("3", "Mohan", PartyType.BUYER, 900),
        p("4", "Mukesh", PartyType.BUYER, -100)
    )

    private fun ids(type: PartyTypeFilter, bal: PartyFilter = PartyFilter.ALL, q: String = "") =
        filterList(all, q, bal, type).map { it.party.id }

    @Test fun kisaanOnly() = assertEquals(listOf("1", "2"), ids(PartyTypeFilter.KISAAN))
    @Test fun vyapariOnly() = assertEquals(listOf("3", "4"), ids(PartyTypeFilter.VYAPARI))
    @Test fun allKeepsEveryone() = assertEquals(listOf("1", "2", "3", "4"), ids(PartyTypeFilter.ALL))
    @Test fun typeAndPayable() = assertEquals(listOf("4"), ids(PartyTypeFilter.VYAPARI, PartyFilter.PAYABLE))
    @Test fun typeAndReceivable() = assertEquals(listOf("2"), ids(PartyTypeFilter.KISAAN, PartyFilter.RECEIVABLE))
    @Test fun searchAppliesAfterType() = assertEquals(listOf("3"), ids(PartyTypeFilter.VYAPARI, q = "Mohan"))
    @Test fun noMatchIsEmpty() = assertEquals(emptyList(), ids(PartyTypeFilter.KISAAN, q = "Mohan"))
}
