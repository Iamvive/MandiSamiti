package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.math.*
import kotlin.test.*

class UdhaarAllocatorTest {
    private fun loan(id: String, date: Long, principal: Long, interest: Long = 0L) = OpenLoan(id, date, principal, interest)

    @Test fun oldestLoanFirstInterestBeforePrincipal() {
        // a (older): Rs 1000 principal + Rs 50 interest. b (newer): Rs 500 principal. Pay Rs 1100.
        val r = UdhaarAllocator.allocate(listOf(loan("b", 2, 50_000), loan("a", 1, 100_000, 5_000)), 110_000)
        assertEquals(
            AllocationResult.Ok(listOf(LoanPayment("a", 100_000, 5_000), LoanPayment("b", 5_000, 0))), r,
        )
    }

    @Test fun smallPaymentClearsInterestOnly() {
        val r = UdhaarAllocator.allocate(listOf(loan("a", 1, 100_000, 5_000)), 3_000)
        assertEquals(AllocationResult.Ok(listOf(LoanPayment("a", 0, 3_000))), r)
    }

    @Test fun overpaymentIsBlockedWithTotalDue() {
        val r = UdhaarAllocator.allocate(listOf(loan("a", 1, 100_000, 5_000)), 105_001)
        assertEquals(AllocationResult.Over(105_000), r)
    }

    @Test fun exactPayoffIsAllowed() {
        val r = UdhaarAllocator.allocate(listOf(loan("a", 1, 100_000, 5_000)), 105_000)
        assertEquals(AllocationResult.Ok(listOf(LoanPayment("a", 100_000, 5_000))), r)
    }

    @Test fun paidLoansAreSkipped() {
        val r = UdhaarAllocator.allocate(listOf(loan("a", 1, 0, 0), loan("b", 2, 10_000)), 4_000)
        assertEquals(AllocationResult.Ok(listOf(LoanPayment("b", 4_000, 0))), r)
    }

    @Test fun zeroPaymentIsRejected() {
        assertFailsWith<IllegalArgumentException> { UdhaarAllocator.allocate(listOf(loan("a", 1, 100)), 0) }
    }
}
