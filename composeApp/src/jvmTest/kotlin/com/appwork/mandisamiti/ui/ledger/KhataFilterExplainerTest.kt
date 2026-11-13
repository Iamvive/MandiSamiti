package com.appwork.mandisamiti.ui.ledger

import com.appwork.mandisamiti.ui.home.PartyFilter
import com.appwork.mandisamiti.ui.home.PartyTypeFilter
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KhataFilterExplainerTest {
    private fun show(
        empty: Boolean = true,
        loading: Boolean = false,
        status: PartyFilter = PartyFilter.ALL,
        type: PartyTypeFilter = PartyTypeFilter.ALL
    ) = shouldShowKhataFilterExplainer(empty, loading, status, type)

    @Test
    fun emptyWithNoFiltersShowsPlainText() = assertFalse(show())

    @Test
    fun emptyWithStatusFilterShowsCard() = assertTrue(show(status = PartyFilter.RECEIVABLE))

    @Test
    fun emptyWithTypeFilterShowsCard() = assertTrue(show(type = PartyTypeFilter.KISAAN))

    @Test
    fun emptyWithBothFiltersShowsCard() =
        assertTrue(show(status = PartyFilter.PAYABLE, type = PartyTypeFilter.VYAPARI))

    @Test
    fun nonEmptyListNeverShowsCard() =
        assertFalse(show(empty = false, status = PartyFilter.RECEIVABLE, type = PartyTypeFilter.KISAAN))

    @Test
    fun loadingNeverShowsCard() = assertFalse(show(loading = true, status = PartyFilter.PAYABLE))

    @Test
    fun filterActiveHelper() {
        assertFalse(isKhataFilterActive(PartyFilter.ALL, PartyTypeFilter.ALL))
        assertTrue(isKhataFilterActive(PartyFilter.PAYABLE, PartyTypeFilter.ALL))
        assertTrue(isKhataFilterActive(PartyFilter.ALL, PartyTypeFilter.VYAPARI))
    }
}
