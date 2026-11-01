package com.appwork.mandisamiti.ui.buy

fun buyEntryActions(
    formState: BuyEntryFormState,
    onSaveTriggered: () -> Unit,
    onPartySelected: ((String) -> Unit)? = null
): BuyEntryActions = object : BuyEntryActions {
    override fun onPhoneChange(value: String) {
        formState.updatePhone(value)
    }

    override fun onNameChange(value: String) {
        formState.farmerName = value
    }

    override fun onVillageChange(value: String) {
        formState.village = value
    }

    override fun onPickParty(id: String) {
        formState.selectedFarmerId = id
        onPartySelected?.invoke(id)
    }

    override fun onLotChange(id: String, field: BuyLotField, value: String) {
        val lot = formState.lots.firstOrNull { it.id == id } ?: return
        when (field) {
            BuyLotField.Crop -> lot.cropName = value
            BuyLotField.Bags -> lot.bags = value.filter { it.isDigit() }.take(4)
            BuyLotField.WeightPerBag -> lot.weightPerBagKg = value.filter { it.isDigit() }.take(3)
            BuyLotField.ExtraWeight -> lot.extraWeightKg = value.take(5)
            BuyLotField.Rate -> lot.ratePerQuintal = value.filter { it.isDigit() }.take(6)
        }
    }

    override fun onAddLot() {
        formState.addLot()
    }

    override fun onRemoveLot(id: String) {
        val index = formState.lots.indexOfFirst { it.id == id }
        if (index >= 0) formState.removeLot(index)
    }

    override fun onCommissionChange(value: String) {
        formState.commissionPercent = value
    }

    override fun onLabourChange(value: String) {
        formState.labourPerBag = value
    }

    override fun onAdvanceChange(value: String) {
        formState.advancePaid = value
    }

    override fun onLoanCutChange(value: String) {
        formState.loanCut = value.filter { it.isDigit() }.take(9)
    }

    override fun onDepositUseChange(value: String) {
        // Deposit is not used on Buy slip per Vivek decision 2026-11-01
    }

    override fun onConfirmRateOutlier() {
        formState.showRateOutlierConfirmation = false
    }

    override fun onSave() {
        onSaveTriggered()
    }
}
