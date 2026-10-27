package com.appwork.mandisamiti.ui.sell

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.appwork.mandisamiti.domain.math.BuySellMath
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.math.SellMathInput
import com.appwork.mandisamiti.domain.math.SellMathResult
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.SellPartyKind

class SellEntryFormState {
    var billMode by mutableStateOf(BillMode.NORMAL)
    var partyKind by mutableStateOf(SellPartyKind.CUSTOMER)
    var gstin by mutableStateOf("")
    var bags by mutableStateOf("")
    var weightPerBagKg by mutableStateOf("")
    var ratePerQuintal by mutableStateOf("")
    var aratPercent by mutableStateOf("")
    var tulak by mutableStateOf("")
    var received by mutableStateOf("")

    fun preview(): SellMathResult {
        val totalWeightGrams = BuySellMath.totalWeightGrams(
            bags = bags.toLongOrNull() ?: 0L,
            weightPerBagGrams = MandiMathEngine.parseKgToGrams(weightPerBagKg.ifBlank { "0" }),
            extraWeightGrams = 0L
        )
        val tulakOverride = if (tulak.isNotBlank()) {
            MandiMathEngine.parseRupeesToPaisa(tulak)
        } else {
            null
        }

        return BuySellMath.calculateSell(
            SellMathInput(
                bags = bags.toLongOrNull() ?: 0L,
                weightPerBagGrams = MandiMathEngine.parseKgToGrams(weightPerBagKg.ifBlank { "0" }),
                ratePaisaPerQuintal = MandiMathEngine.parseRupeesToPaisa(ratePerQuintal.ifBlank { "0" }),
                billMode = billMode,
                aratBps = MandiMathEngine.parsePercentToBasisPoints(aratPercent.ifBlank { "0" }),
                tulakPaisaOverride = tulakOverride,
                receivedPaisa = MandiMathEngine.parseRupeesToPaisa(received.ifBlank { "0" })
            )
        )
    }
}
