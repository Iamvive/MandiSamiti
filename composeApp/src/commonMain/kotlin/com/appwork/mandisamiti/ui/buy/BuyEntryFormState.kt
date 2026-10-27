package com.appwork.mandisamiti.ui.buy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.appwork.mandisamiti.domain.math.BuyMathInput
import com.appwork.mandisamiti.domain.math.BuyMathResult
import com.appwork.mandisamiti.domain.math.BuySellMath
import com.appwork.mandisamiti.domain.math.MandiMathEngine

class BuyEntryFormState {
    var phone by mutableStateOf("")
    var bags by mutableStateOf("")
    var weightPerBagKg by mutableStateOf("")
    var extraWeightKg by mutableStateOf("")
    var ratePerQuintal by mutableStateOf("")
    var commissionPercent by mutableStateOf("")
    var labourPerBag by mutableStateOf("")
    var advancePaid by mutableStateOf("")

    fun preview(): BuyMathResult = BuySellMath.calculateBuy(
        BuyMathInput(
            bags = bags.toLongOrNull() ?: 0L,
            weightPerBagGrams = MandiMathEngine.parseKgToGrams(weightPerBagKg.ifBlank { "0" }),
            extraWeightGrams = MandiMathEngine.parseKgToGrams(extraWeightKg.ifBlank { "0" }),
            ratePaisaPerQuintal = MandiMathEngine.parseRupeesToPaisa(ratePerQuintal.ifBlank { "0" }),
            commissionBps = MandiMathEngine.parsePercentToBasisPoints(commissionPercent.ifBlank { "0" }),
            labourPerBagPaisa = MandiMathEngine.parseRupeesToPaisa(labourPerBag.ifBlank { "0" }),
            advancePaidPaisa = MandiMathEngine.parseRupeesToPaisa(advancePaid.ifBlank { "0" }),
        )
    )
}
