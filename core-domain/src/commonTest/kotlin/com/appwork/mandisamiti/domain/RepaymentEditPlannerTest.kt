package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.math.RepaymentEditPlanner
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.usecase.LendException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RepaymentEditPlannerTest {
    private val day = 86_400_000L
    private val lend = LendEntry(
        id = "l1", shopId = "shop", partyId = "p", lendType = LendType.MONEY,
        entryDate = 0L, amountPaisa = 5_000_000L, createdAt = 0L, updatedAt = 0L,
    )

    private fun pay(id: String, days: Int, amount: Long, interest: Long, principal: Long) = LendSettlement(
        id = id, shopId = "shop", lendId = "l1", settledAt = days * day, amountPaisa = amount,
        createdAt = days * day, updatedAt = days * day, principalPaisa = principal,
        interestPaisa = interest, calculatedInterestPaisa = interest,
    )

    private val live = listOf(
        pay("a", 30, 1_000_000L, 100_000L, 900_000L),
        pay("b", 60, 500_000L, 82_000L, 418_000L),
        pay("c", 90, 500_000L, 73_640L, 426_360L),
    )

    @Test
    fun editMiddlePaymentRecalculatesLaterOnes() {
        val plan = RepaymentEditPlanner.plan(lend, live, "b", 550_000L, 60 * day, 2.0, now = 999L)
        assertEquals(50_000L, plan.gallaDeltaPaisa)
        assertEquals(listOf("b", "c"), plan.changedSettlements.map { it.id })
        val b = plan.changedSettlements[0]
        assertEquals(550_000L, b.amountPaisa)
        assertEquals(82_000L, b.interestPaisa)
        assertEquals(468_000L, b.principalPaisa)
        val c = plan.changedSettlements[1]
        assertEquals(500_000L, c.amountPaisa)
        assertEquals(72_640L, c.interestPaisa)
        assertEquals(427_360L, c.principalPaisa)
        assertEquals(2L, b.revision)
        assertEquals(999L, b.updatedAt)
    }

    @Test
    fun noChangeGivesEmptyPlan() {
        val plan = RepaymentEditPlanner.plan(lend, live, "b", 500_000L, 60 * day, 2.0, now = 999L)
        assertEquals(0L, plan.gallaDeltaPaisa)
        assertEquals(emptyList(), plan.changes)
    }

    @Test
    fun paymentThatTookNoInterestStaysAllPrincipal() {
        val noInterest = live.map { if (it.id == "c") it.copy(interestPaisa = 0L, calculatedInterestPaisa = 0L, principalPaisa = 500_000L) else it }
        val plan = RepaymentEditPlanner.plan(lend, noInterest, "b", 550_000L, 60 * day, 2.0, now = 1L)
        val c = plan.changedSettlements.singleOrNull { it.id == "c" }
        assertEquals(null, c) // unchanged: still 0 interest, 500,000 principal
    }

    @Test
    fun editThatWouldOverRepayIsRejected() {
        val single = listOf(pay("a", 30, 1_000_000L, 100_000L, 900_000L))
        assertFailsWith<LendException.OverRepayment> {
            RepaymentEditPlanner.plan(lend, single, "a", 6_000_000L, 30 * day, 2.0, now = 1L)
        }
    }

    @Test
    fun dateBeforeLoanIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            RepaymentEditPlanner.plan(lend.copy(entryDate = 10 * day), live, "a", 1_000_000L, 5 * day, 2.0, now = 1L)
        }
    }
}
