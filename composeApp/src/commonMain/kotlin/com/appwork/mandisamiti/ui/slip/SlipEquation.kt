package com.appwork.mandisamiti.ui.slip

import com.appwork.mandisamiti.domain.math.MandiMathEngine

private fun kgText(grams: Long): String {
    val whole = grams / 1000
    val frac = (grams % 1000).toString().padStart(3, '0').trimEnd('0')
    return if (frac.isEmpty()) "$whole" else "$whole.$frac"
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
        "${MandiMathEngine.gramsToQuintalsInputString(totalGrams)} क्विंटल × ₹${MandiMathEngine.paisaToRupeesString(ratePaisaPerQuintal)}"
}
