package com.appwork.mandisamiti.domain.buy

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

    fun rupeesToPaisa(rupees: Long): Long {
        return rupees * 100L
    }
}
