package com.appwork.mandisamiti.ui.register

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GaddiCounterTest {

    @Test
    fun replaceThenAppendDigits() {
        var draft = GaddiDraft(selected = 500, field = GaddiField.LOOSE)
        // First digit replaces initial 0
        draft = draft.digit(5)
        assertEquals(5, draft.counts[500]?.loose)

        // Subsequent digits append
        draft = draft.digit(2)
        assertEquals(52, draft.counts[500]?.loose)

        draft = draft.digit(0)
        assertEquals(520, draft.counts[500]?.loose)
    }

    @Test
    fun bundleAndLooseCaps() {
        var draft = GaddiDraft(selected = 500, field = GaddiField.BUNDLES)
        draft = draft.digit(9).digit(9).digit(9)
        assertEquals(999, draft.counts[500]?.bundles)
        // Attempting to exceed 999 is ignored
        draft = draft.digit(1)
        assertEquals(999, draft.counts[500]?.bundles)

        // Loose cap is 9999
        draft = draft.focus(GaddiField.LOOSE)
        draft = draft.digit(9).digit(9).digit(9).digit(9)
        assertEquals(9999, draft.counts[500]?.loose)
        // Exceeding 9999 is ignored
        draft = draft.digit(5)
        assertEquals(9999, draft.counts[500]?.loose)
    }

    @Test
    fun backspaceOnUntouchedDoesNothing() {
        val draft = GaddiDraft(selected = 500)
        val afterBackspace = draft.backspace()
        assertEquals(draft, afterBackspace)
    }

    @Test
    fun loose137Stays137AndIsNotConvertedToBundles() {
        var draft = GaddiDraft(selected = 500, field = GaddiField.LOOSE)
        draft = draft.digit(1).digit(3).digit(7)
        val count = draft.counts[500]
        assertEquals(0, count?.bundles)
        assertEquals(137, count?.loose)
        assertEquals(137, count?.pieces)
        assertEquals(137 * 500L, draft.totalRupees)
    }

    @Test
    fun zeroGivesTotalZeroAndMarksCounted() {
        var draft = GaddiDraft(selected = 500)
        draft = draft.zero()
        assertEquals(0, draft.counts[500]?.bundles)
        assertEquals(0, draft.counts[500]?.loose)
        assertEquals(0L, draft.totalRupees)
    }

    @Test
    fun emptyDraftGivesNullTotal() {
        val draft = GaddiDraft()
        assertNull(draft.totalRupees)
    }

    @Test
    fun endStopsForNextAndPrevious() {
        // First denomination is 2000; previous should do nothing
        val atStart = GaddiDraft(selected = 2000)
        assertEquals(2000, atStart.previous().selected)
        assertEquals(500, atStart.next().selected)

        // Last denomination is 1; next should do nothing
        val atEnd = GaddiDraft(selected = 1)
        assertEquals(1, atEnd.next().selected)
        assertEquals(2, atEnd.previous().selected)
    }

    @Test
    fun twoGaddiPlus37Of500Equals118500() {
        var draft = GaddiDraft(selected = 500, field = GaddiField.BUNDLES)
        draft = draft.digit(2)
        draft = draft.focus(GaddiField.LOOSE)
        draft = draft.digit(3).digit(7)

        val count = draft.counts[500]
        assertEquals(2, count?.bundles)
        assertEquals(37, count?.loose)
        assertEquals(237, count?.pieces)
        // 237 * 500 = 118,500
        assertEquals(118_500L, draft.totalRupees)
    }

    @Test
    fun coinHasNoBundles() {
        var draft = GaddiDraft(selected = 5)
        draft = draft.focus(GaddiField.BUNDLES) // Should remain in LOOSE
        assertEquals(GaddiField.LOOSE, draft.field)

        draft = draft.digit(4).digit(0)
        val count = draft.counts[5]
        assertEquals(0, count?.bundles)
        assertEquals(40, count?.loose)
        assertEquals(200L, draft.totalRupees)
    }

    @Test
    fun addOneIncrementsActiveField() {
        var draft = GaddiDraft(selected = 100, field = GaddiField.BUNDLES)
        draft = draft.addOne().addOne()
        assertEquals(2, draft.counts[100]?.bundles)

        draft = draft.focus(GaddiField.LOOSE).addOne()
        assertEquals(1, draft.counts[100]?.loose)
        assertEquals(201, draft.counts[100]?.pieces)
    }
}
