package com.appwork.mandisamiti.domain.math

import kotlin.math.round

data class DeductionsInput(
    val farmerCommissionPaisa: Long = 0L,
    val buyerCommissionPaisa: Long = 0L,
    val labourChargePaisa: Long = 0L,
    val weighingChargePaisa: Long = 0L,
    val otherDeductionsPaisa: Long = 0L
)

data class SettlementCalculation(
    val grossWeightGrams: Long,
    val cutWeightGrams: Long,
    val netWeightGrams: Long,
    val grossAmountPaisa: Long,
    val farmerCommissionPaisa: Long,
    val buyerCommissionPaisa: Long,
    val labourChargePaisa: Long,
    val weighingChargePaisa: Long,
    val otherDeductionsPaisa: Long,
    val totalFarmerDeductionsPaisa: Long,
    val netFarmerPayablePaisa: Long,
    val netBuyerReceivablePaisa: Long
)

object MandiMathEngine {

    const val GRAMS_PER_KG = 1_000L
    const val GRAMS_PER_QUINTAL = 100_000L // 100 kg * 1,000 g
    const val PAISA_PER_RUPEE = 100L

    /**
     * Converts weight in grams to quintals for display (e.g. 1,840,000g -> 18.40 qtl).
     */
    fun gramsToQuintals(grams: Long): Double {
        return grams.toDouble() / GRAMS_PER_QUINTAL
    }

    /**
     * Converts quintals to exact integer grams safely with rounding.
     */
    fun quintalsToGrams(quintals: Double): Long {
        return round(quintals * GRAMS_PER_QUINTAL).toLong()
    }

    /**
     * Parses a quintals string (e.g. "18.40" or "18.4") into exact integer grams.
     */
    fun parseQuintalsStringToGrams(input: String): Long {
        val clean = input.trim()
        if (clean.isEmpty()) return 0L
        val parts = clean.split(".")
        val wholeQuintals = parts[0].toLongOrNull() ?: 0L
        val fractionalGrams = if (parts.size > 1) {
            val frac = parts[1].padEnd(5, '0').take(5)
            frac.toLongOrNull() ?: 0L
        } else {
            0L
        }
        return (wholeQuintals * GRAMS_PER_QUINTAL) + fractionalGrams
    }

    /**
     * Calculates the complete mandi settlement using exact integer math without float rounding loss.
     */
    fun calculateSettlement(
        grossWeightGrams: Long,
        cutWeightGrams: Long,
        ratePaisaPerQuintal: Long,
        deductions: DeductionsInput
    ): SettlementCalculation {
        val netWeightGrams = (grossWeightGrams - cutWeightGrams).coerceAtLeast(0L)

        // Gross Amount = (netWeightGrams * ratePaisaPerQuintal) / GRAMS_PER_QUINTAL
        val grossAmountPaisa = (netWeightGrams * ratePaisaPerQuintal) / GRAMS_PER_QUINTAL

        val totalFarmerDeductions = deductions.farmerCommissionPaisa +
                deductions.labourChargePaisa +
                deductions.weighingChargePaisa +
                deductions.otherDeductionsPaisa

        val netFarmerPayable = (grossAmountPaisa - totalFarmerDeductions).coerceAtLeast(0L)
        val netBuyerReceivable = grossAmountPaisa + deductions.buyerCommissionPaisa

        return SettlementCalculation(
            grossWeightGrams = grossWeightGrams,
            cutWeightGrams = cutWeightGrams,
            netWeightGrams = netWeightGrams,
            grossAmountPaisa = grossAmountPaisa,
            farmerCommissionPaisa = deductions.farmerCommissionPaisa,
            buyerCommissionPaisa = deductions.buyerCommissionPaisa,
            labourChargePaisa = deductions.labourChargePaisa,
            weighingChargePaisa = deductions.weighingChargePaisa,
            otherDeductionsPaisa = deductions.otherDeductionsPaisa,
            totalFarmerDeductionsPaisa = totalFarmerDeductions,
            netFarmerPayablePaisa = netFarmerPayable,
            netBuyerReceivablePaisa = netBuyerReceivable
        )
    }

    /**
     * Formats paisa to standard Indian number format (e.g. 9509022 -> "95,090.22").
     */
    fun paisaToRupeesString(paisa: Long): String {
        val isNegative = paisa < 0
        val absPaisa = if (isNegative) -paisa else paisa
        val rupees = absPaisa / PAISA_PER_RUPEE
        val remainingPaisa = absPaisa % PAISA_PER_RUPEE

        val formattedRupees = formatIndianGrouping(rupees)
        val formattedPaisa = if (remainingPaisa < 10) "0$remainingPaisa" else "$remainingPaisa"

        val result = if (remainingPaisa > 0) "$formattedRupees.$formattedPaisa" else formattedRupees
        return if (isNegative) "-$result" else result
    }

    private fun formatIndianGrouping(number: Long): String {
        val str = number.toString()
        if (str.length <= 3) return str

        val last3 = str.substring(str.length - 3)
        var remaining = str.substring(0, str.length - 3)
        val parts = mutableListOf<String>()

        while (remaining.length > 2) {
            parts.add(0, remaining.substring(remaining.length - 2))
            remaining = remaining.substring(0, remaining.length - 2)
        }
        if (remaining.isNotEmpty()) {
            parts.add(0, remaining)
        }
        return parts.joinToString(",") + ",$last3"
    }
}
