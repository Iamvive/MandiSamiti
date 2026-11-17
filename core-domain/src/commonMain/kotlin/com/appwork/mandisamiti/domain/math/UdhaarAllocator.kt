package com.appwork.mandisamiti.domain.math

data class OpenLoan(
    val lendId: String,
    val entryDate: Long,
    val outstandingPrincipalPaisa: Long,
    val interestDuePaisa: Long,
)

data class LoanPayment(val lendId: String, val principalPaisa: Long, val interestPaisa: Long)

sealed interface AllocationResult {
    data class Ok(val parts: List<LoanPayment>) : AllocationResult
    data class Over(val totalDuePaisa: Long) : AllocationResult
}

/** Oldest loan first; inside one loan, interest before principal. Overpayment is reported, never absorbed. */
object UdhaarAllocator {
    fun allocate(loans: List<OpenLoan>, paymentPaisa: Long): AllocationResult {
        require(paymentPaisa > 0) { "Payment must be above zero" }
        val live = loans.filter { it.outstandingPrincipalPaisa > 0 || it.interestDuePaisa > 0 }
            .sortedBy { it.entryDate }
        val totalDue = live.sumOf { it.outstandingPrincipalPaisa + it.interestDuePaisa }
        if (paymentPaisa > totalDue) return AllocationResult.Over(totalDue)
        var left = paymentPaisa
        val parts = mutableListOf<LoanPayment>()
        for (loan in live) {
            if (left == 0L) break
            val interest = minOf(left, loan.interestDuePaisa)
            left -= interest
            val principal = minOf(left, loan.outstandingPrincipalPaisa)
            left -= principal
            if (interest + principal > 0) parts += LoanPayment(loan.lendId, principal, interest)
        }
        return AllocationResult.Ok(parts)
    }
}
