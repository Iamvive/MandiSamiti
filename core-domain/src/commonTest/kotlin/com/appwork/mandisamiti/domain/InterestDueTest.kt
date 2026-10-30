package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import com.appwork.mandisamiti.domain.model.LendSettlement
import kotlin.test.Test
import kotlin.test.assertEquals

class InterestDueTest {
    private val day = 24L * 60 * 60 * 1000
    private val loan = 10_000_00L

    private fun settlement(
        dayIndex: Long,
        principal: Long,
        interest: Long = 0L,
        void: Boolean = false
    ) = LendSettlement(
        id = "s$dayIndex", shopId = "shop", lendId = "l1", settledAt = dayIndex * day,
        amountPaisa = principal + interest, createdAt = 0L, updatedAt = 0L,
        isVoid = void, principalPaisa = principal, interestPaisa = interest
    )

    private fun due(settlements: List<LendSettlement>, days: Long) =
        RuralInterestEngine.interestDue(loan, 2.0, 0L, settlements, days * day)

    @Test
    fun thirtyDaysAtTwoPercentIsTwoHundredRupees() {
        assertEquals(200_00L, due(emptyList(), 30))
    }

    @Test
    fun partPrincipalRepaidHalvesNextMonthsInterest() {
        // Day 30: pay the 200 interest and 5,000 principal. Next 30 days accrue on 5,000 only.
        assertEquals(100_00L, due(listOf(settlement(30, 5_000_00L, 200_00L)), 60))
    }

    @Test
    fun unpaidInterestDoesNotCompound() {
        // Nothing paid: two months is exactly twice one month, not 200 + 2% of 10,200.
        assertEquals(400_00L, due(emptyList(), 60))
        // Principal-only payment on day 30 leaves the 200 interest unpaid and earning nothing.
        assertEquals(200_00L + 100_00L, due(listOf(settlement(30, 5_000_00L, 0L)), 60))
    }

    @Test
    fun voidedSettlementIsIgnored() {
        assertEquals(200_00L, due(listOf(settlement(15, 5_000_00L, 100_00L, void = true)), 30))
    }

    @Test
    fun legacyRowWithoutSplitCountsAsAllPrincipal() {
        val legacy = LendSettlement(
            id = "old", shopId = "shop", lendId = "l1", settledAt = 30 * day,
            amountPaisa = 5_000_00L, createdAt = 0L, updatedAt = 0L
        )
        assertEquals(300_00L, due(listOf(legacy), 60))
    }

    @Test
    fun neverNegativeWhenOverpaid() {
        assertEquals(0L, due(listOf(settlement(30, 0L, 500_00L)), 30))
    }
}
