package com.appwork.mandisamiti.domain.math

import kotlin.math.abs

object MandiCurrencyFormatter {

    private const val PAISA_PER_RUPEE = 100L
    private const val RUPEES_PER_CRORE = 10_000_000L
    private const val RUPEES_PER_LAKH = 100_000L

    /**
     * Formats [paisa] into a compact Indian currency string:
     * - Below ₹1,00,000: Full comma-formatted amount (e.g. "₹99,999")
     * - ₹1,00,000 to ₹99,99,999: "₹X.XX L" (English) or "₹X.XX लाख" (Hindi)
     * - ₹1,00,00,000+: "₹X.XX Cr" (English) or "₹X.XX करोड़" (Hindi)
     */
    fun formatCompactRupees(paisa: Long, isEnglish: Boolean = false): String {
        val isNegative = paisa < 0
        val absPaisa = abs(paisa)
        val absRupees = absPaisa / PAISA_PER_RUPEE
        val sign = if (isNegative) "-" else ""

        return when {
            absRupees >= RUPEES_PER_CRORE -> {
                val croreInt = absRupees / RUPEES_PER_CRORE
                val croreDec = ((absRupees % RUPEES_PER_CRORE) / 100_000L).toString().padStart(2, '0')
                val unit = if (isEnglish) "Cr" else "करोड़"
                "$sign₹$croreInt.$croreDec $unit"
            }
            absRupees >= RUPEES_PER_LAKH -> {
                val lakhInt = absRupees / RUPEES_PER_LAKH
                val lakhDec = ((absRupees % RUPEES_PER_LAKH) / 1_000L).toString().padStart(2, '0')
                val unit = if (isEnglish) "L" else "लाख"
                "$sign₹$lakhInt.$lakhDec $unit"
            }
            else -> {
                "$sign₹${MandiMathEngine.paisaToRupeesString(absPaisa)}"
            }
        }
    }
}
