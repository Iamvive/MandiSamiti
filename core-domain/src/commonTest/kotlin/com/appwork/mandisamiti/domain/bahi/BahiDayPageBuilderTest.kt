package com.appwork.mandisamiti.domain.bahi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BahiDayPageBuilderTest {
    private fun entry(
        id: String,
        kind: BahiEntryKind,
        rupees: Long,
        mode: BahiPaymentMode = BahiPaymentMode.CASH,
        void: Boolean = false,
        sameDayAdvance: Boolean = false,
        savedAfterClose: Boolean = false,
    ) = BahiDayEntry(
        id, kind, id, rupees * 100L, mode,
        isVoid = void,
        isSameDayAdvance = sameDayAdvance,
        savedAfterClose = savedAfterClose,
    )

    @Test fun balancedDay() {
        val page = BahiDayPageBuilder.build(1, 10_000_00, 8_000_00, listOf(entry("expense", BahiEntryKind.EXPENSE, 2_000)))
        assertTrue(page.isBalanced)
        assertEquals(10_000_00, page.leftTotalPaisa)
    }

    @Test fun cashOnlyDay() {
        val page = BahiDayPageBuilder.build(1, 10_000_00, 11_000_00, listOf(entry("buyer", BahiEntryKind.BUYER_PAYMENT, 1_000)))
        assertTrue(page.isBalanced)
        assertEquals(2, page.left.size)
        assertEquals(1, page.right.size)
    }

    @Test fun onlineInAndOutPairAppearsOnBothSides() {
        val entries = listOf(
            entry("in", BahiEntryKind.BUYER_PAYMENT, 2_000, BahiPaymentMode.RTGS),
            entry("out", BahiEntryKind.FARMER_PAYMENT, 1_000, BahiPaymentMode.NEFT),
        )
        val page = BahiDayPageBuilder.build(1, 10_000_00, 10_000_00, entries)
        assertTrue(page.isBalanced)
        assertEquals(2, page.left.count { it.paymentMode != BahiPaymentMode.CASH })
        assertEquals(2, page.right.count { it.paymentMode != BahiPaymentMode.CASH })
    }

    @Test fun sameDayAdvanceIsStruckOffOnRight() {
        val page = BahiDayPageBuilder.build(1, 10_000_00, 9_000_00, listOf(entry("advance", BahiEntryKind.ADVANCE_PAID, 1_000, sameDayAdvance = true)))
        assertTrue(page.right.first { it.id == "advance" }.isStruckThrough)
    }

    @Test fun earlierAdvanceIsCarriedIntoPurchaseDayLeft() {
        val page = BahiDayPageBuilder.build(2, 9_000_00, 10_000_00, listOf(entry("advance-link", BahiEntryKind.ADVANCE_CARRIED_IN, 1_000)))
        assertEquals(BahiEntryKind.ADVANCE_CARRIED_IN, page.left.last().kind)
        assertTrue(page.isBalanced)
    }

    @Test fun voidedEntryIsListedButExcluded() {
        val page = BahiDayPageBuilder.build(1, 10_000_00, 10_000_00, listOf(entry("void", BahiEntryKind.EXPENSE, 5_000, void = true)))
        assertTrue(page.right.first { it.id == "void" }.isStruckThrough)
        assertTrue(page.isBalanced)
    }

    @Test fun mismatchedDayCanCloseWithDifferenceExpense() {
        val page = BahiDayPageBuilder.build(1, 10_000_00, 9_500_00, emptyList(), closeMismatch = true)
        assertTrue(page.isBalanced)
        assertEquals(500_00, page.right.first { it.kind == BahiEntryKind.DAY_CLOSE_DIFF }.amountPaisa)
    }

    @Test fun editAfterCloseMarksDayAndCarriesToNextOpening() {
        val edited = BahiDayPageBuilder.build(1, 10_000_00, 9_000_00, listOf(entry("late", BahiEntryKind.EXPENSE, 1_000, savedAfterClose = true)))
        val next = BahiDayPageBuilder.build(2, edited.right.first { it.kind == BahiEntryKind.CLOSING_CASH }.amountPaisa, 9_000_00, emptyList())
        assertTrue(edited.changedAfterClosing)
        assertEquals(9_000_00, next.left.first().amountPaisa)
        assertFalse(next.changedAfterClosing)
    }
}
