package com.appwork.mandisamiti.ui.ledger

import com.appwork.mandisamiti.domain.math.InterestCalculation
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.repository.CashTransactionRepository
import com.appwork.mandisamiti.domain.repository.DealRepository
import com.appwork.mandisamiti.domain.repository.PartyRepository
import com.appwork.mandisamiti.domain.repository.ShopProfileRepository
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

sealed interface LedgerItem {
    val timestamp: Long

    data class DealItem(
        val deal: Deal,
        override val timestamp: Long = deal.dealDate
    ) : LedgerItem

    data class CashItem(
        val transaction: CashTransaction,
        override val timestamp: Long = transaction.transactionDate
    ) : LedgerItem
}

data class PartyLedgerUiState(
    val party: Party? = null,
    val isSoundEnabled: Boolean = true,
    val balancePaisa: Long = 0L,
    val ledgerItems: List<LedgerItem> = emptyList(),
    val isCashDialogOpen: Boolean = false,
    val isInterestDialogOpen: Boolean = false,
    val calculatedInterest: InterestCalculation? = null,
    val isSaving: Boolean = false,
    val error: String? = null
)

sealed interface PartyLedgerEvent {
    data class TransactionRecorded(val speechText: String) : PartyLedgerEvent
    data class Error(val message: String) : PartyLedgerEvent
}

class PartyLedgerViewModel(
    private val shopId: String,
    private val partyId: String,
    private val partyRepository: PartyRepository,
    private val cashRepository: CashTransactionRepository,
    private val dealRepository: DealRepository,
    private val shopProfileRepository: ShopProfileRepository,
    private val ttsManager: SoundboxTtsManager,
    private val viewModelScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    private val _uiState = MutableStateFlow(PartyLedgerUiState())
    val uiState: StateFlow<PartyLedgerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PartyLedgerEvent>()
    val events: SharedFlow<PartyLedgerEvent> = _events.asSharedFlow()

    init {
        loadPartyData()
    }

    private fun loadPartyData() {
        shopProfileRepository.getShopProfileStream()
            .onEach { profile ->
                if (profile != null) {
                    _uiState.value = _uiState.value.copy(isSoundEnabled = profile.isSoundEnabled)
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val party = partyRepository.getPartyById(partyId)
            _uiState.value = _uiState.value.copy(party = party)
        }

        partyRepository.getPartyBalanceStream(partyId)
            .onEach { balance ->
                if (balance != null) {
                    _uiState.value = _uiState.value.copy(balancePaisa = balance.balancePaisa)
                }
            }
            .launchIn(viewModelScope)

        combine(
            cashRepository.getTransactionsByPartyStream(partyId),
            dealRepository.getDealsByShopStream(shopId)
        ) { cashList, dealsList ->
            val partyDeals = dealsList.filter { it.farmerId == partyId || it.buyerId == partyId }
            val allItems = mutableListOf<LedgerItem>()
            cashList.forEach { allItems.add(LedgerItem.CashItem(it)) }
            partyDeals.forEach { allItems.add(LedgerItem.DealItem(it)) }

            allItems.sortedByDescending { it.timestamp }
        }.onEach { sortedItems ->
            _uiState.value = _uiState.value.copy(ledgerItems = sortedItems)
        }.launchIn(viewModelScope)
    }

    fun openCashDialog() {
        _uiState.value = _uiState.value.copy(isCashDialogOpen = true)
    }

    fun closeCashDialog() {
        _uiState.value = _uiState.value.copy(isCashDialogOpen = false)
    }

    fun openInterestDialog() {
        val principal = _uiState.value.balancePaisa
        if (principal > 0L) {
            val interest = RuralInterestEngine.calculateAccruedInterestByDays(
                principalPaisa = principal,
                monthlyRatePercent = _uiState.value.party?.monthlyInterestRate ?: 1.5,
                elapsedDays = 30
            )
            _uiState.value = _uiState.value.copy(
                isInterestDialogOpen = true,
                calculatedInterest = interest
            )
        } else {
            _uiState.value = _uiState.value.copy(isInterestDialogOpen = true)
        }
    }

    fun closeInterestDialog() {
        _uiState.value = _uiState.value.copy(isInterestDialogOpen = false)
    }

    fun recordCashEntry(
        transactionType: TransactionType,
        amountRs: Long,
        paymentMode: PaymentMode = PaymentMode.CASH,
        remarks: String? = null
    ) {
        if (amountRs <= 0L) return

        val now = Clock.System.now().toEpochMilliseconds()
        val amountPaisa = amountRs * 100L

        val tx = CashTransaction(
            id = "tx_${now}_${(1000..9999).random()}",
            shopId = shopId,
            partyId = partyId,
            transactionType = transactionType,
            amountPaisa = amountPaisa,
            paymentMode = paymentMode,
            transactionDate = now,
            remarks = remarks,
            createdAt = now,
            updatedAt = now
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            cashRepository.recordTransaction(tx)

            val partyName = _uiState.value.party?.name ?: partyRepository.getPartyById(partyId)?.name ?: "खाता"
            val typeHindi = when (transactionType) {
                TransactionType.UDHAR_GIVEN -> "उधार दिया गया"
                TransactionType.JAMA_RECEIVED -> "जमा प्राप्त हुआ"
                TransactionType.INTEREST_ADDED -> "ब्याज जोड़ा गया"
                TransactionType.DISCOUNT_GIVEN -> "छूट / समझौता दिया गया"
            }
            val formattedAmount = MandiMathEngine.paisaToRupeesString(amountPaisa)
            val speechText = "$partyName, ₹$formattedAmount $typeHindi।"

            ttsManager.speak(speechText, _uiState.value.isSoundEnabled)
            _uiState.value = _uiState.value.copy(isSaving = false, isCashDialogOpen = false)
            _events.emit(PartyLedgerEvent.TransactionRecorded(speechText))
        }
    }

    fun recordCalculatedInterest(interestResult: InterestCalculation) {
        val now = Clock.System.now().toEpochMilliseconds()

        val tx = CashTransaction(
            id = "tx_int_${now}_${(1000..9999).random()}",
            shopId = shopId,
            partyId = partyId,
            transactionType = TransactionType.INTEREST_ADDED,
            amountPaisa = interestResult.accruedInterestPaisa,
            paymentMode = PaymentMode.CASH,
            transactionDate = now,
            remarks = "देसी ब्याज (${interestResult.monthlyRatePercent}% / माह, ${interestResult.daysElapsed} दिन)",
            createdAt = now,
            updatedAt = now
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            cashRepository.recordTransaction(tx)

            val partyName = _uiState.value.party?.name ?: partyRepository.getPartyById(partyId)?.name ?: "खाता"
            val formattedAmount = MandiMathEngine.paisaToRupeesString(interestResult.accruedInterestPaisa)
            val speechText = "$partyName, ₹$formattedAmount ब्याज खाते में जोड़ा गया।"

            ttsManager.speak(speechText, _uiState.value.isSoundEnabled)
            _uiState.value = _uiState.value.copy(isSaving = false, isInterestDialogOpen = false)
            _events.emit(PartyLedgerEvent.TransactionRecorded(speechText))
        }
    }
}
