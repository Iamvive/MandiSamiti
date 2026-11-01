package com.appwork.mandisamiti.ui.buy

object BuyPaymentCalculations {
    fun calculateFinalPayable(netPayablePaisa: Long, advanceOrCutPaisa: Long): Long {
        return (netPayablePaisa - advanceOrCutPaisa).coerceAtLeast(0L)
    }

    fun calculateRemainingLeft(finalPayablePaisa: Long, amountToPayPaisa: Long): Long {
        return (finalPayablePaisa - amountToPayPaisa).coerceAtLeast(0L)
    }

    fun calculateLoanCut(paidPaisa: Long, advancePaidPaisa: Long): Long {
        return (paidPaisa - advancePaidPaisa).coerceAtLeast(0L)
    }

    fun calculateLeft(netPayablePaisa: Long, paidPaisa: Long): Long {
        return (netPayablePaisa - paidPaisa).coerceAtLeast(0L)
    }

    fun calculatePostDeductionPayable(netPayablePaisa: Long, advancePaisa: Long, loanCutPaisa: Long): Long {
        return (netPayablePaisa - advancePaisa - loanCutPaisa).coerceAtLeast(0L)
    }

    fun calculateCashPayment(totalPaidPaisa: Long, advancePaisa: Long, loanCutPaisa: Long): Long {
        return (totalPaidPaisa - advancePaisa - loanCutPaisa).coerceAtLeast(0L)
    }

    fun rupeesToPaisa(rupees: Long): Long {
        return rupees * 100L
    }

    fun isGreaterThanZero(paisa: Long): Boolean {
        return paisa > 0L
    }

    fun calculateTotalCut(interestCutPaisa: Long, principalCutPaisa: Long): Long {
        return interestCutPaisa + principalCutPaisa
    }

    fun hasLoanBalance(principalPaisa: Long, interestPaisa: Long): Boolean {
        return principalPaisa > 0L || interestPaisa > 0L
    }

    fun calculateTotalDebt(principalPaisa: Long, interestPaisa: Long): Long {
        return principalPaisa + interestPaisa
    }

    fun calculateRemainingPrincipal(principalPaisa: Long, principalCutPaisa: Long): Long {
        return (principalPaisa - principalCutPaisa).coerceAtLeast(0L)
    }
}
