package com.appwork.mandisamiti.ui.buy

/**
 * Contract between the Buy entry logic (Claude, C6) and the Buy entry screen (Antigravity, A3).
 * The screen renders only this and calls [BuyEntryActions]. All money is paisa, all weight is grams.
 * Errors are Hindi strings ready to show; null means the field is fine.
 */
data class BuyEntryUiState(
    val party: BuyPartyState = BuyPartyState(),
    val lots: List<BuyLotUiState> = emptyList(),
    val rates: BuyRatesState = BuyRatesState(),
    val slip: BuySlipState = BuySlipState(),
    val loanDeposit: LoanDepositState = LoanDepositState(),
    /** Advance is entered after the slip calculation (Vivek, 2026-10-31). */
    val advanceText: String = "",
    val advanceError: String? = null,
    /** Net payable of the commodity, then what is actually paid out after advance and loan/deposit. */
    val payoutPaisa: Long = 0L,
    /** Why Save is off, or null when [canSave]. */
    val saveBlockedReason: String? = null,
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
    /** Set when the save failed. Nothing was written (one transaction). */
    val saveError: String? = null,
)

/** Phone is the primary field. A name or phone match fills the rest. */
data class BuyPartyState(
    val phone: String = "",
    val phoneError: String? = null,
    val name: String = "",
    val nameError: String? = null,
    val village: String = "",
    val villageError: String? = null,
    val matches: List<PartyMatch> = emptyList(),
    val selectedPartyId: String? = null,
    /** True when a picked party has no number: ask for it before Save and store it on the party. */
    val needsPhonePrompt: Boolean = false,
    /** True when the name was edited on an existing party: Save renames it everywhere, older slips included. */
    val willRenameParty: Boolean = false,
    /** True when the phone is new: Save creates the party once. */
    val willCreateParty: Boolean = false,
)

data class PartyMatch(
    val id: String,
    val name: String,
    val phone: String?,
    val village: String?,
)

/** One crop line: bags, weight per bag and extra weight share a row, rate sits below. */
data class BuyLotUiState(
    val id: String,
    val cropName: String,
    val bags: String,
    val weightPerBagKg: String,
    val extraWeightKg: String,
    val ratePerQuintal: String,
    val bagsError: String? = null,
    val weightError: String? = null,
    val extraError: String? = null,
    val rateError: String? = null,
    val netWeightGrams: Long = 0L,
    val grossPaisa: Long = 0L,
)

/** Always-visible editable fields above the slip, filled from Settings. */
data class BuyRatesState(
    val commissionPercent: String = "",
    val labourPerBag: String = "",
)

/** The हिसाब पर्ची. Labour sits above commission. Tare is not part of a new slip. */
data class BuySlipState(
    val totalBags: Long = 0L,
    val totalWeightGrams: Long = 0L,
    val grossPaisa: Long = 0L,
    val labourPaisa: Long = 0L,
    val commissionPaisa: Long = 0L,
    val netPayablePaisa: Long = 0L,
    val rateOutlierWarning: String? = null,
)

/**
 * The person's existing loan (we lent) or deposit (they left with us), shown after the slip.
 * A loan cut reduces the payout, interest first and then principal, as in RepayLoanDialog.
 * Deposit is not used on the Buy slip (Vivek, 2026-11-01: the slip covers the current transaction only); [depositUseText] stays unused.
 */
data class LoanDepositState(
    val loanPrincipalPaisa: Long = 0L,
    val loanInterestDuePaisa: Long = 0L,
    val depositPaisa: Long = 0L,
    val loanCutText: String = "",
    val loanCutError: String? = null,
    /** How the entered cut splits, so the screen can show it before Save. */
    val cutToInterestPaisa: Long = 0L,
    val cutToPrincipalPaisa: Long = 0L,
    val depositUseText: String = "",
    val depositUseError: String? = null,
) {
    val hasAnything: Boolean get() = loanPrincipalPaisa > 0L || loanInterestDuePaisa > 0L || depositPaisa > 0L
}

/** Everything the screen can ask for. The screen holds no business rules. */
interface BuyEntryActions {
    fun onPhoneChange(value: String)
    fun onNameChange(value: String)
    fun onVillageChange(value: String)
    fun onPickParty(id: String)
    fun onLotChange(id: String, field: BuyLotField, value: String)
    fun onAddLot()
    fun onRemoveLot(id: String)
    fun onCommissionChange(value: String)
    fun onLabourChange(value: String)
    fun onAdvanceChange(value: String)
    fun onLoanCutChange(value: String)
    fun onDepositUseChange(value: String)
    fun onConfirmRateOutlier()
    fun onSave()
}

enum class BuyLotField { Crop, Bags, WeightPerBag, ExtraWeight, Rate }
