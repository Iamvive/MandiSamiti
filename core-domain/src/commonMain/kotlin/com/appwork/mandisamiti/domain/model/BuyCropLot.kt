package com.appwork.mandisamiti.domain.model

import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import kotlinx.serialization.Serializable

@Serializable
data class BuyCropLot(
    val id: String = IdGenerator.newId(),
    val cropName: String = "गेहूं",
    val bags: Long = 0L,
    val weightPerBagGrams: Long = 50_000L,
    val extraWeightGrams: Long = 0L,
    val totalWeightGrams: Long = 0L,
    val ratePaisaPerQuintal: Long = 0L,
    val grossPaisa: Long = 0L,
) {
    companion object {
        fun create(
            cropName: String,
            bags: Long,
            weightPerBagGrams: Long,
            extraWeightGrams: Long,
            ratePaisaPerQuintal: Long,
            id: String = IdGenerator.newId()
        ): BuyCropLot {
            val totalWeight = (bags * weightPerBagGrams) + extraWeightGrams
            val gross = MandiMathEngine.grossAmountPaisa(totalWeight, ratePaisaPerQuintal)
            return BuyCropLot(
                id = id,
                cropName = cropName.ifBlank { "गेहूं" },
                bags = bags,
                weightPerBagGrams = weightPerBagGrams,
                extraWeightGrams = extraWeightGrams,
                totalWeightGrams = totalWeight,
                ratePaisaPerQuintal = ratePaisaPerQuintal,
                grossPaisa = gross
            )
        }
    }
}
