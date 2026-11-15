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
    val extraWeightGrams: Long = 0L,
    /** Trader (Vyapaari) labour rate per quintal; 0 = no labour line. */
    val traderLabourPaisaPerQuintal: Long = 0L,
)

data class SellMathResult(
    val totalWeightGrams: Long,
    val grossPaisa: Long,
    val aratPaisa: Long,
    val tulakPaisa: Long,
    val mandiShulkPaisa: Long,
    val totalReceivablePaisa: Long,
    val balanceDuePaisa: Long,
    /** Trader labour, includes the paise that round the total to a whole rupee. 0 when no rate is set. */
    val labourPaisa: Long = 0L,
)

object BuySellMath {
    const val MANDI_SHULK_BPS = 150L
    private const val TULAK_PAISA_PER_QUINTAL = 100L

    fun totalWeightGrams(bags: Long, weightPerBagGrams: Long, extraWeightGrams: Long): Long =
        bags * weightPerBagGrams + extraWeightGrams

    fun defaultTulakPaisa(totalWeightGrams: Long): Long =
        MandiMathEngine.grossAmountPaisa(totalWeightGrams, TULAK_PAISA_PER_QUINTAL)

    /** Sheet rule: extra loose weight above 20 kg is billed as one more bag for labour (Buy only). */
    private const val EXTRA_BAG_THRESHOLD_GRAMS = 20_000L
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
        val grams = totalWeightGrams(input.bags, input.weightPerBagGrams, input.extraWeightGrams)
        val gross = MandiMathEngine.grossAmountPaisa(grams, input.ratePaisaPerQuintal)
        val bos = input.billMode == BillMode.BILL_OF_SUPPLY
        val arat = MandiMathEngine.percentageOf(gross, input.aratBps)
        val shulk = if (bos) MandiMathEngine.percentageOf(gross, MANDI_SHULK_BPS) else 0L
        if (input.traderLabourPaisaPerQuintal > 0L) {
            // Trader sale: tulak stays at its default, labour absorbs the rounding to a whole rupee.
            val tulak = if (bos) input.tulakPaisaOverride ?: defaultTulakPaisa(grams) else defaultTulakPaisa(grams)
            val labour = MandiMathEngine.grossAmountPaisa(grams, input.traderLabourPaisaPerQuintal)
            val raw = gross + arat + tulak + shulk + labour
            val total = (raw + PAISA_PER_RUPEE / 2).floorDiv(PAISA_PER_RUPEE) * PAISA_PER_RUPEE
            return SellMathResult(grams, gross, arat, tulak, shulk, total, total - input.receivedPaisa, labour + (total - raw))
        }
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
