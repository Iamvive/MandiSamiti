package com.appwork.mandisamiti.domain.math

import com.appwork.mandisamiti.domain.model.BillMode

data class BuyMathInput(
    val bags: Long,
    val weightPerBagGrams: Long,
    val extraWeightGrams: Long,
    val ratePaisaPerQuintal: Long,
    val commissionBps: Long,
    val labourPerBagPaisa: Long,
    val advancePaidPaisa: Long,
)

data class BuyMathResult(
    val totalWeightGrams: Long,
    val grossPaisa: Long,
    val commissionPaisa: Long,
    val labourPaisa: Long,
    /** May be negative when the advance exceeds the amount due; the UI must flag that, not hide it. */
    val netPayablePaisa: Long,
)

data class SellMathInput(
    val bags: Long,
    val weightPerBagGrams: Long,
    val ratePaisaPerQuintal: Long,
    val billMode: BillMode,
    val aratBps: Long,
    /** null = use the Rs 1 per quintal default. */
    val tulakPaisaOverride: Long?,
    val receivedPaisa: Long,
)

data class SellMathResult(
    val totalWeightGrams: Long,
    val grossPaisa: Long,
    val aratPaisa: Long,
    val tulakPaisa: Long,
    val mandiShulkPaisa: Long,
    val totalReceivablePaisa: Long,
    val balanceDuePaisa: Long,
)

object BuySellMath {
    const val MANDI_SHULK_BPS = 150L
    private const val TULAK_PAISA_PER_QUINTAL = 100L

    fun totalWeightGrams(bags: Long, weightPerBagGrams: Long, extraWeightGrams: Long): Long =
        bags * weightPerBagGrams + extraWeightGrams

    fun defaultTulakPaisa(totalWeightGrams: Long): Long =
        MandiMathEngine.grossAmountPaisa(totalWeightGrams, TULAK_PAISA_PER_QUINTAL)

    fun calculateBuy(input: BuyMathInput): BuyMathResult {
        val grams = totalWeightGrams(input.bags, input.weightPerBagGrams, input.extraWeightGrams)
        val gross = MandiMathEngine.grossAmountPaisa(grams, input.ratePaisaPerQuintal)
        val commission = MandiMathEngine.percentageOf(gross, input.commissionBps)
        val labour = input.bags * input.labourPerBagPaisa
        return BuyMathResult(grams, gross, commission, labour, gross - commission - labour - input.advancePaidPaisa)
    }

    fun calculateSell(input: SellMathInput): SellMathResult {
        val grams = totalWeightGrams(input.bags, input.weightPerBagGrams, 0L)
        val gross = MandiMathEngine.grossAmountPaisa(grams, input.ratePaisaPerQuintal)
        val bos = input.billMode == BillMode.BILL_OF_SUPPLY
        val arat = if (bos) MandiMathEngine.percentageOf(gross, input.aratBps) else 0L
        val tulak = if (bos) input.tulakPaisaOverride ?: defaultTulakPaisa(grams) else 0L
        val shulk = if (bos) MandiMathEngine.percentageOf(gross, MANDI_SHULK_BPS) else 0L
        val total = gross + arat + tulak + shulk
        return SellMathResult(grams, gross, arat, tulak, shulk, total, total - input.receivedPaisa)
    }
}
