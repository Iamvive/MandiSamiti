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

    /** Sheet rule: extra loose weight above 5 kg is billed as one more bag for labour. */
    private const val EXTRA_BAG_THRESHOLD_GRAMS = 5_000L
    private const val PAISA_PER_RUPEE = 100L
    private const val ROUND_TO_PAISA = 1_000L // nearest Rs 10

    private fun floorToRupee(paisa: Long): Long = paisa.floorDiv(PAISA_PER_RUPEE) * PAISA_PER_RUPEE

    private fun roundToNearestTenRupees(paisa: Long): Long =
        (paisa + ROUND_TO_PAISA / 2).floorDiv(ROUND_TO_PAISA) * ROUND_TO_PAISA

    /** Farmer slip as on the shop sheet: gross cut to the rupee, final rounded to Rs 10, commission absorbs the rounding. */
    fun calculateBuy(input: BuyMathInput): BuyMathResult {
        val grams = totalWeightGrams(input.bags, input.weightPerBagGrams, input.extraWeightGrams)
        val gross = floorToRupee(grams * input.ratePaisaPerQuintal / MandiMathEngine.GRAMS_PER_QUINTAL)
        val billedBags = input.bags + if (input.extraWeightGrams > EXTRA_BAG_THRESHOLD_GRAMS) 1 else 0
        val labour = billedBags * input.labourPerBagPaisa
        val final = roundToNearestTenRupees(gross - labour - MandiMathEngine.percentageOf(gross, input.commissionBps))
        val commission = gross - labour - final
        return BuyMathResult(grams, gross, commission, labour, final - input.advancePaidPaisa)
    }

    fun calculateSell(input: SellMathInput): SellMathResult {
        val grams = totalWeightGrams(input.bags, input.weightPerBagGrams, 0L)
        val gross = MandiMathEngine.grossAmountPaisa(grams, input.ratePaisaPerQuintal)
        val bos = input.billMode == BillMode.BILL_OF_SUPPLY
        val arat = MandiMathEngine.percentageOf(gross, input.aratBps)
        val shulk = if (bos) MandiMathEngine.percentageOf(gross, MANDI_SHULK_BPS) else 0L
        if (bos) {
            val tulak = input.tulakPaisaOverride ?: defaultTulakPaisa(grams)
            val total = gross + arat + tulak + shulk
            return SellMathResult(grams, gross, arat, tulak, shulk, total, total - input.receivedPaisa)
        }
        // Cash Memo: final cut to the whole rupee, tulak absorbs the difference.
        val total = floorToRupee(gross + arat + defaultTulakPaisa(grams))
        val tulak = total - gross - arat
        return SellMathResult(grams, gross, arat, tulak, shulk, total, total - input.receivedPaisa)
    }
}
