package com.appwork.mandisamiti.ui.buy

import com.appwork.mandisamiti.domain.math.MandiMathEngine

/** Field errors show only once the field has text; [BuyEntryUiState.saveBlockedReason] covers what is still missing. */
private fun String.errorIfFilled(error: String?): String? = if (isBlank()) null else error

/**
 * Maps the form to the contract the Buy screen renders (A3).
 * Not in the form yet, so left at defaults: party matches and the create/rename/phone-prompt flags,
 * [BuyEntryUiState.isSaving], [BuyEntryUiState.saveError], the rate outlier warning text,
 * and deposit use (the save does not support it).
 */
fun BuyEntryFormState.toUiState(): BuyEntryUiState {
    val agg = aggregatePreview()
    val lotErrors = validateLots()
    val split = loanCutSplit()
    val cutError = loanCutError()
    return BuyEntryUiState(
        party = BuyPartyState(
            phone = phone,
            phoneError = phone.errorIfFilled(validatePhone().errorOrNull),
            name = farmerName,
            nameError = farmerName.errorIfFilled(validateFarmerName().errorOrNull),
            village = village,
            villageError = village.errorIfFilled(validateVillage().errorOrNull),
            selectedPartyId = selectedFarmerId,
        ),
        lots = lots.mapIndexed { i, lot ->
            val v = lotErrors[i]
            val calc = agg.lotsCalculations[i].second
            BuyLotUiState(
                id = lot.id,
                cropName = lot.cropName,
                bags = lot.bags,
                weightPerBagKg = lot.weightPerBagKg,
                extraWeightKg = lot.extraWeightKg,
                ratePerQuintal = lot.ratePerQuintal,
                bagsError = lot.bags.errorIfFilled(v.bagsError),
                weightError = lot.weightPerBagKg.errorIfFilled(v.weightError),
                extraError = lot.extraWeightKg.errorIfFilled(v.extraError),
                rateError = lot.ratePerQuintal.errorIfFilled(v.rateError),
                netWeightGrams = calc.totalWeightGrams,
                grossPaisa = calc.grossPaisa,
            )
        },
        rates = BuyRatesState(commissionPercent = commissionPercent, labourPerBag = labourPerBag),
        slip = BuySlipState(
            totalBags = agg.totalBags,
            totalWeightGrams = agg.totalWeightGrams,
            grossPaisa = agg.totalGrossPaisa,
            labourPaisa = agg.labourPaisa,
            commissionPaisa = agg.commissionPaisa,
            netPayablePaisa = agg.netPayablePaisa,
        ),
        loanDeposit = LoanDepositState(
            loanPrincipalPaisa = loanPrincipalPaisa,
            loanInterestDuePaisa = loanInterestDuePaisa,
            loanCutText = loanCut,
            loanCutError = cutError,
            cutToInterestPaisa = split.interestPaisa,
            cutToPrincipalPaisa = split.principalPaisa,
        ),
        advanceText = advancePaid,
        payoutPaisa = agg.payoutPaisa,
        saveBlockedReason = validationReason(),
        canSave = canSave(),
    )
}
