package com.appwork.mandisamiti.ui.deal

import com.appwork.mandisamiti.domain.math.DeductionsInput
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.Commodity
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.DealStatus
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.repository.DealRepository
import com.appwork.mandisamiti.domain.repository.PartyRepository
import com.appwork.mandisamiti.domain.repository.ShopProfileRepository
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import com.appwork.mandisamiti.ui.components.KeypadAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

enum class ActiveInputField {
    BAGS_COUNT,
    GROSS_WEIGHT,
    TARE_WEIGHT,
    RATE_PER_QUINTAL,
    LABOUR_CHARGES,
    COMMISSION_PERCENT
}

data class DealEntryUiState(
    val dealId: String? = null,
    val shopId: String = "shop-1",
    val isEditMode: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val selectedFarmer: Party? = null,
    val selectedBuyer: Party? = null,
    val selectedCommodity: Commodity? = null,
    val availableFarmers: List<Party> = emptyList(),
    val availableBuyers: List<Party> = emptyList(),
    val availableCommodities: List<Commodity> = emptyList(),

    // Field strings for calculator keypad
    val bagsCountText: String = "",
    val grossWeightText: String = "", // in Quintals, e.g. "18.40"
    val tareWeightText: String = "0.35",  // in Quintals, e.g. "0.35"
    val ratePerQuintalText: String = "", // in Rs/Quintal, e.g. "2450"
    val labourChargesText: String = "150", // in Rs
    val commissionPercentText: String = "1.5", // %

    val activeField: ActiveInputField = ActiveInputField.GROSS_WEIGHT,
    val receiptPhotoUri: String? = null,

    // Real-time calculated preview fields
    val netWeightQuintals: String = "0.00",
    val grossAmountPaisa: Long = 0L,
    val netFarmerPayablePaisa: Long = 0L,
    val netBuyerReceivablePaisa: Long = 0L,
    val isSettledStage: Boolean = false, // false = Stage 1 arrival, true = Stage 2 rate entry
    val isSaving: Boolean = false,
    val error: String? = null
)

sealed interface DealEntryEvent {
    data class DealSavedSuccess(val deal: Deal, val ttsSpeechText: String) : DealEntryEvent
    data class Error(val message: String) : DealEntryEvent
}

class DealEntryViewModel(
    private val shopId: String,
    private val existingDealId: String? = null,
    private val dealRepository: DealRepository,
    private val partyRepository: PartyRepository,
    private val shopProfileRepository: ShopProfileRepository,
    private val ttsManager: SoundboxTtsManager,
    private val viewModelScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    private val _uiState = MutableStateFlow(DealEntryUiState(dealId = existingDealId, shopId = shopId, isEditMode = existingDealId != null))
    val uiState: StateFlow<DealEntryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DealEntryEvent>()
    val events: SharedFlow<DealEntryEvent> = _events.asSharedFlow()

    init {
        loadShopProfileAndParties()
        if (existingDealId != null) {
            loadExistingDeal(existingDealId)
        }
    }

    private fun loadShopProfileAndParties() {
        shopProfileRepository.getShopProfileStream()
            .onEach { profile ->
                if (profile != null) {
                    _uiState.value = _uiState.value.copy(isSoundEnabled = profile.isSoundEnabled)
                }
            }
            .launchIn(viewModelScope)

        partyRepository.getPartiesStream(shopId)
            .onEach { parties ->
                val farmers = parties.filter { it.partyType == PartyType.FARMER }
                val buyers = parties.filter { it.partyType == PartyType.BUYER }
                _uiState.value = _uiState.value.copy(
                    availableFarmers = farmers,
                    availableBuyers = buyers,
                    selectedFarmer = _uiState.value.selectedFarmer ?: farmers.firstOrNull()
                )
            }
            .launchIn(viewModelScope)
    }

    private fun loadExistingDeal(dealId: String) {
        viewModelScope.launch {
            val deal = dealRepository.getDealById(dealId)
            if (deal != null) {
                val farmer = partyRepository.getPartyById(deal.farmerId)
                val buyer = deal.buyerId?.let { partyRepository.getPartyById(it) }

                val grossQ = MandiMathEngine.gramsToQuintals(deal.grossWeightGrams).toString()
                val tareQ = MandiMathEngine.gramsToQuintals(deal.cutWeightGrams).toString()
                val rate = deal.ratePaisaPerUnit?.let { (it / 100L).toString() } ?: ""

                _uiState.value = _uiState.value.copy(
                    dealId = deal.id,
                    isEditMode = true,
                    selectedFarmer = farmer,
                    selectedBuyer = buyer,
                    bagsCountText = deal.bagsCount.toString(),
                    grossWeightText = grossQ,
                    tareWeightText = tareQ,
                    ratePerQuintalText = rate,
                    receiptPhotoUri = deal.receiptPhotoUri,
                    isSettledStage = deal.dealStatus == DealStatus.SETTLED
                )
                recalculate()
            }
        }
    }

    fun onSelectFarmer(farmer: Party) {
        _uiState.value = _uiState.value.copy(selectedFarmer = farmer)
    }

    fun onSelectBuyer(buyer: Party?) {
        _uiState.value = _uiState.value.copy(selectedBuyer = buyer)
        recalculate()
    }

    fun onSelectCommodity(commodity: Commodity) {
        _uiState.value = _uiState.value.copy(selectedCommodity = commodity)
    }

    fun onFocusField(field: ActiveInputField) {
        _uiState.value = _uiState.value.copy(activeField = field)
    }

    fun onReceiptPhotoCaptured(uri: String) {
        _uiState.value = _uiState.value.copy(receiptPhotoUri = uri)
    }

    fun onKeypadAction(action: KeypadAction) {
        when (action) {
            KeypadAction.CLEAR -> onKeypadClear()
            KeypadAction.BACKSPACE -> onKeypadBackspace()
            KeypadAction.NEXT -> moveToNextField()
            KeypadAction.SUBMIT -> saveDeal()
            KeypadAction.DIGIT_0 -> appendKey("0")
            KeypadAction.DIGIT_1 -> appendKey("1")
            KeypadAction.DIGIT_2 -> appendKey("2")
            KeypadAction.DIGIT_3 -> appendKey("3")
            KeypadAction.DIGIT_4 -> appendKey("4")
            KeypadAction.DIGIT_5 -> appendKey("5")
            KeypadAction.DIGIT_6 -> appendKey("6")
            KeypadAction.DIGIT_7 -> appendKey("7")
            KeypadAction.DIGIT_8 -> appendKey("8")
            KeypadAction.DIGIT_9 -> appendKey("9")
            KeypadAction.DOUBLE_ZERO -> appendKey("00")
            KeypadAction.DECIMAL -> appendKey(".")
        }
    }

    private fun moveToNextField() {
        val next = when (_uiState.value.activeField) {
            ActiveInputField.BAGS_COUNT -> ActiveInputField.GROSS_WEIGHT
            ActiveInputField.GROSS_WEIGHT -> ActiveInputField.TARE_WEIGHT
            ActiveInputField.TARE_WEIGHT -> if (_uiState.value.isSettledStage) ActiveInputField.RATE_PER_QUINTAL else ActiveInputField.BAGS_COUNT
            ActiveInputField.RATE_PER_QUINTAL -> ActiveInputField.LABOUR_CHARGES
            ActiveInputField.LABOUR_CHARGES -> ActiveInputField.COMMISSION_PERCENT
            ActiveInputField.COMMISSION_PERCENT -> ActiveInputField.RATE_PER_QUINTAL
        }
        _uiState.value = _uiState.value.copy(activeField = next)
    }

    private fun appendKey(key: String) {
        val current = _uiState.value
        val updatedText = when (current.activeField) {
            ActiveInputField.BAGS_COUNT -> applyKeypad(current.bagsCountText, key, allowDecimal = false)
            ActiveInputField.GROSS_WEIGHT -> applyKeypad(current.grossWeightText, key, allowDecimal = true)
            ActiveInputField.TARE_WEIGHT -> applyKeypad(current.tareWeightText, key, allowDecimal = true)
            ActiveInputField.RATE_PER_QUINTAL -> applyKeypad(current.ratePerQuintalText, key, allowDecimal = false)
            ActiveInputField.LABOUR_CHARGES -> applyKeypad(current.labourChargesText, key, allowDecimal = false)
            ActiveInputField.COMMISSION_PERCENT -> applyKeypad(current.commissionPercentText, key, allowDecimal = true)
        }

        _uiState.value = when (current.activeField) {
            ActiveInputField.BAGS_COUNT -> current.copy(bagsCountText = updatedText)
            ActiveInputField.GROSS_WEIGHT -> current.copy(grossWeightText = updatedText)
            ActiveInputField.TARE_WEIGHT -> current.copy(tareWeightText = updatedText)
            ActiveInputField.RATE_PER_QUINTAL -> current.copy(ratePerQuintalText = updatedText)
            ActiveInputField.LABOUR_CHARGES -> current.copy(labourChargesText = updatedText)
            ActiveInputField.COMMISSION_PERCENT -> current.copy(commissionPercentText = updatedText)
        }
        recalculate()
    }

    fun onKeypadBackspace() {
        val current = _uiState.value
        val updatedText = when (current.activeField) {
            ActiveInputField.BAGS_COUNT -> current.bagsCountText.dropLast(1)
            ActiveInputField.GROSS_WEIGHT -> current.grossWeightText.dropLast(1)
            ActiveInputField.TARE_WEIGHT -> current.tareWeightText.dropLast(1)
            ActiveInputField.RATE_PER_QUINTAL -> current.ratePerQuintalText.dropLast(1)
            ActiveInputField.LABOUR_CHARGES -> current.labourChargesText.dropLast(1)
            ActiveInputField.COMMISSION_PERCENT -> current.commissionPercentText.dropLast(1)
        }

        _uiState.value = when (current.activeField) {
            ActiveInputField.BAGS_COUNT -> current.copy(bagsCountText = updatedText)
            ActiveInputField.GROSS_WEIGHT -> current.copy(grossWeightText = updatedText)
            ActiveInputField.TARE_WEIGHT -> current.copy(tareWeightText = updatedText)
            ActiveInputField.RATE_PER_QUINTAL -> current.copy(ratePerQuintalText = updatedText)
            ActiveInputField.LABOUR_CHARGES -> current.copy(labourChargesText = updatedText)
            ActiveInputField.COMMISSION_PERCENT -> current.copy(commissionPercentText = updatedText)
        }
        recalculate()
    }

    fun onKeypadClear() {
        val current = _uiState.value
        _uiState.value = when (current.activeField) {
            ActiveInputField.BAGS_COUNT -> current.copy(bagsCountText = "")
            ActiveInputField.GROSS_WEIGHT -> current.copy(grossWeightText = "")
            ActiveInputField.TARE_WEIGHT -> current.copy(tareWeightText = "")
            ActiveInputField.RATE_PER_QUINTAL -> current.copy(ratePerQuintalText = "")
            ActiveInputField.LABOUR_CHARGES -> current.copy(labourChargesText = "")
            ActiveInputField.COMMISSION_PERCENT -> current.copy(commissionPercentText = "")
        }
        recalculate()
    }

    fun toggleSettlementStage(isSettled: Boolean) {
        _uiState.value = _uiState.value.copy(isSettledStage = isSettled)
        recalculate()
    }

    private fun applyKeypad(current: String, key: String, allowDecimal: Boolean): String {
        return when (key) {
            "." -> if (allowDecimal && !current.contains(".")) (if (current.isEmpty()) "0." else "$current.") else current
            "00" -> if (current.isNotEmpty() && current != "0") "$current" + "00" else current
            else -> if (current == "0" && key != ".") key else current + key
        }
    }

    private fun recalculate() {
        val state = _uiState.value
        val grossGrams = MandiMathEngine.parseQuintalsStringToGrams(state.grossWeightText)
        val tareGrams = MandiMathEngine.parseQuintalsStringToGrams(state.tareWeightText)
        val netGrams = (grossGrams - tareGrams).coerceAtLeast(0L)

        val netQuintalsStr = (netGrams.toDouble() / MandiMathEngine.GRAMS_PER_QUINTAL).let {
            val rounded = ((it * 100).toLong() / 100.0).toString()
            rounded
        }

        val rateRs = state.ratePerQuintalText.toLongOrNull() ?: 0L
        val ratePaisa = rateRs * 100L
        val labourRs = state.labourChargesText.toLongOrNull() ?: 0L
        val labourPaisa = labourRs * 100L
        val commPercent = state.commissionPercentText.toDoubleOrNull() ?: 1.5

        if (ratePaisa > 0L && netGrams > 0L) {
            val grossAmount = (netGrams * ratePaisa) / MandiMathEngine.GRAMS_PER_QUINTAL
            val farmerComm = (grossAmount * (commPercent / 100.0)).toLong()
            val calc = MandiMathEngine.calculateSettlement(
                grossWeightGrams = grossGrams,
                cutWeightGrams = tareGrams,
                ratePaisaPerQuintal = ratePaisa,
                deductions = DeductionsInput(
                    farmerCommissionPaisa = farmerComm,
                    labourChargePaisa = labourPaisa
                )
            )
            _uiState.value = _uiState.value.copy(
                netWeightQuintals = netQuintalsStr,
                grossAmountPaisa = calc.grossAmountPaisa,
                netFarmerPayablePaisa = calc.netFarmerPayablePaisa,
                netBuyerReceivablePaisa = calc.netBuyerReceivablePaisa
            )
        } else {
            _uiState.value = _uiState.value.copy(
                netWeightQuintals = netQuintalsStr,
                grossAmountPaisa = 0L,
                netFarmerPayablePaisa = 0L,
                netBuyerReceivablePaisa = 0L
            )
        }
    }

    fun saveDeal() {
        val state = _uiState.value
        val farmer = state.selectedFarmer
        if (farmer == null) {
            _uiState.value = state.copy(error = "कृपया किसान का चयन करें")
            return
        }

        val grossGrams = MandiMathEngine.parseQuintalsStringToGrams(state.grossWeightText)
        val tareGrams = MandiMathEngine.parseQuintalsStringToGrams(state.tareWeightText)
        val netGrams = (grossGrams - tareGrams).coerceAtLeast(0L)
        val bags = state.bagsCountText.toIntOrNull() ?: 0

        if (grossGrams <= 0L) {
            _uiState.value = state.copy(error = "कृपया कुल वजन (Gross Weight) दर्ज करें")
            return
        }

        val isSettled = state.isSettledStage && state.ratePerQuintalText.isNotEmpty()
        val rateRs = state.ratePerQuintalText.toLongOrNull()
        val ratePaisa = rateRs?.let { it * 100L }
        val labourRs = state.labourChargesText.toLongOrNull() ?: 0L
        val labourPaisa = labourRs * 100L
        val commPercent = state.commissionPercentText.toDoubleOrNull() ?: 1.5

        val now = Clock.System.now().toEpochMilliseconds()
        val dealId = state.dealId ?: "deal_${now}_${(1000..9999).random()}"

        val deal = Deal(
            id = dealId,
            shopId = state.shopId,
            farmerId = farmer.id,
            buyerId = state.selectedBuyer?.id,
            commodityId = state.selectedCommodity?.id ?: "comm_wheat",
            dealStatus = if (isSettled) DealStatus.SETTLED else DealStatus.PENDING_SETTLEMENT,
            dealDate = now,
            bagsCount = bags,
            grossWeightGrams = grossGrams,
            cutWeightGrams = tareGrams,
            netWeightGrams = netGrams,
            ratePaisaPerUnit = ratePaisa,
            grossAmountPaisa = state.grossAmountPaisa,
            farmerCommissionPaisa = (state.grossAmountPaisa * (commPercent / 100.0)).toLong(),
            labourChargePaisa = labourPaisa,
            netFarmerPayablePaisa = state.netFarmerPayablePaisa,
            netBuyerReceivablePaisa = state.netBuyerReceivablePaisa,
            receiptPhotoUri = state.receiptPhotoUri,
            createdAt = now,
            updatedAt = now
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            dealRepository.saveDeal(deal)

            // Hindi Soundbox Voice Announcement Text
            val hindiVoiceText = if (isSettled) {
                val farmerName = farmer.name
                val bagsStr = if (bags > 0) "$bags बोरी " else ""
                val amountRs = MandiMathEngine.paisaToRupeesString(state.netFarmerPayablePaisa)
                "$farmerName, $bagsStr, ₹$amountRs पक्के हिसाब में दर्ज हुए।"
            } else {
                val farmerName = farmer.name
                val quintalsStr = state.netWeightQuintals
                "$farmerName, $quintalsStr कुंतल आवक दर्ज हुई।"
            }

            ttsManager.speak(hindiVoiceText, state.isSoundEnabled)
            _uiState.value = _uiState.value.copy(isSaving = false)
            _events.emit(DealEntryEvent.DealSavedSuccess(deal, hindiVoiceText))
        }
    }
}
