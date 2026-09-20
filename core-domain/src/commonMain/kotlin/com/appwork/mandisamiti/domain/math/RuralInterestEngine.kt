package com.appwork.mandisamiti.domain.math

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
        val interestDouble = principalPaisa * (monthlyRatePercent / 100.0) * months
        val accruedInterestPaisa = interestDouble.toLong()
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
