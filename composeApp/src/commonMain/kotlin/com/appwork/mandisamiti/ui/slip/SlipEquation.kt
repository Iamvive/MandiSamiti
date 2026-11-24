package com.appwork.mandisamiti.ui.slip

import com.appwork.mandisamiti.domain.math.MandiMathEngine

private fun kgText(grams: Long): String {
    val whole = grams / 1000
    val frac = (grams % 1000).toString().padStart(3, '0').trimEnd('0')
    return if (frac.isEmpty()) "$whole" else "$whole.$frac"
}

/** Quintal quantity for slips, always 2 decimals (12.50), rounded half-up from grams. */
fun slipQuintalText(grams: Long): String {
    val hundredths = (grams + 500L) / 1000L
    return "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}"
}

/** Slip line: bori x wt per bori + extra = qtl x rate. Weights in kg. */
fun slipEquation(
    bags: Long,
    weightPerBagGrams: Long,
    totalGrams: Long,
    ratePaisaPerQuintal: Long,
): String {
    val extra = (totalGrams - bags * weightPerBagGrams).coerceAtLeast(0L)
    return "$bags × ${kgText(weightPerBagGrams)} + ${kgText(extra)} = " +
        "${slipQuintalText(totalGrams)} क्विंटल × ₹${MandiMathEngine.paisaToRupeesString(ratePaisaPerQuintal)}"
}
