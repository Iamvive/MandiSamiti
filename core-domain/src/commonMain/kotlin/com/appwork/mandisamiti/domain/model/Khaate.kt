package com.appwork.mandisamiti.domain.model

import com.appwork.mandisamiti.domain.math.MandiMathEngine

/** Day-end spare crop. No party, no commission, no labour, no Galla effect. */
object Khaate {
    const val PARTY_NAME = "खाते"
    fun partyId(shopId: String): String = "khaate-$shopId"

    /** grams / 100_000 = quintal. Half-up to the paisa, same as normal entries. */
    fun asalPaisa(totalWeightGrams: Long, ratePaisaPerQuintal: Long): Long =
        MandiMathEngine.grossAmountPaisa(totalWeightGrams, ratePaisaPerQuintal)
}
