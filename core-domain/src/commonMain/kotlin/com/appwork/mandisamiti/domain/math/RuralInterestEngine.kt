package com.appwork.mandisamiti.domain.math

import kotlin.math.roundToLong

/** How one repayment divides: interest is cleared first, whatever is left reduces principal. */
data class RepaymentSplit(val interestPaisa: Long, val principalPaisa: Long)

data class InterestCalculation(
    val principalPaisa: Long,
    val monthlyRatePercent: Double,
    val daysElapsed: Int,
    val approxMonthsElapsed: Double,
    val accruedInterestPaisa: Long,
    val totalPayablePaisa: Long
)

object RuralInterestEngine {

    const val DAYS_IN_MONTH = 30.0

    /**
     * Calculates simple monthly interest based on days elapsed (e.g. ₹1.50 per ₹100 per month).
     * Formula: Interest = Principal * (Rate / 100) * (Days / 30)
     */
    fun calculateAccruedInterestByDays(
        principalPaisa: Long,
        monthlyRatePercent: Double,
        elapsedDays: Int
    ): InterestCalculation {
        if (principalPaisa <= 0 || monthlyRatePercent <= 0 || elapsedDays <= 0) {
            return InterestCalculation(
                principalPaisa = principalPaisa,
                monthlyRatePercent = monthlyRatePercent,
                daysElapsed = elapsedDays.coerceAtLeast(0),
                approxMonthsElapsed = 0.0,
                accruedInterestPaisa = 0L,
                totalPayablePaisa = principalPaisa
            )
        }

        val months = elapsedDays / DAYS_IN_MONTH
        // Money stays in Long: the rate is taken in hundredths of a percent (1.5% -> 150) and the division floors.
        val rateHundredths = (monthlyRatePercent * 100.0).roundToLong()
        val accruedInterestPaisa = principalPaisa * rateHundredths * elapsedDays / (10_000L * DAYS_IN_MONTH.toLong())
        val totalPayablePaisa = principalPaisa + accruedInterestPaisa

        return InterestCalculation(
            principalPaisa = principalPaisa,
            monthlyRatePercent = monthlyRatePercent,
            daysElapsed = elapsedDays,
            approxMonthsElapsed = months,
            accruedInterestPaisa = accruedInterestPaisa,
            totalPayablePaisa = totalPayablePaisa
        )
    }

    /** D4.4: a part repayment pays interest first, then principal. [interestDuePaisa] is the agreed amount. */
    fun splitRepayment(paymentPaisa: Long, interestDuePaisa: Long): RepaymentSplit {
        val interest = minOf(paymentPaisa.coerceAtLeast(0L), interestDuePaisa.coerceAtLeast(0L))
        return RepaymentSplit(interest, paymentPaisa.coerceAtLeast(0L) - interest)
    }

    /**
     * Calculates interest between two epoch timestamps in milliseconds.
     */
    fun calculateAccruedInterestBetweenEpochs(
        principalPaisa: Long,
        monthlyRatePercent: Double,
        startEpochMs: Long,
        endEpochMs: Long
    ): InterestCalculation {
        val diffMs = (endEpochMs - startEpochMs).coerceAtLeast(0L)
        val elapsedDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
        return calculateAccruedInterestByDays(
            principalPaisa = principalPaisa,
            monthlyRatePercent = monthlyRatePercent,
            elapsedDays = elapsedDays
        )
    }
}
