package com.appwork.mandisamiti.ui.register

import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.repository.CashTransactionRepository
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

data class TransactionWithParty(
    val transaction: CashTransaction,
    val partyName: String,
    val village: String?
)

data class DailyRegisterUiState(
    val shopProfile: ShopProfile? = null,
    val isSoundEnabled: Boolean = true,
    val todayCashInPaisa: Long = 0L,
    val todayCashOutPaisa: Long = 0L,
    val inHandCashDrawerPaisa: Long = 0L,
    val todayTransactions: List<TransactionWithParty> = emptyList(),
    val availableParties: List<Party> = emptyList(),
    val isAddEntryDialogOpen: Boolean = false,
    val isSaving: Boolean = false
)

sealed interface DailyRegisterEvent {
    data class EntryRecorded(val speechText: String) : DailyRegisterEvent
}

class DailyRegisterViewModel(
    private val shopId: String,
    private val cashRepository: CashTransactionRepository,
    private val partyRepository: PartyRepository,
    private val shopProfileRepository: ShopProfileRepository,
    private val ttsManager: SoundboxTtsManager,
    private val viewModelScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    private val _uiState = MutableStateFlow(DailyRegisterUiState())
    val uiState: StateFlow<DailyRegisterUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DailyRegisterEvent>()
    val events: SharedFlow<DailyRegisterEvent> = _events.asSharedFlow()

    init {
        loadDailyData()
    }

    private fun loadDailyData() {
        shopProfileRepository.getShopProfileStream()
            .onEach { profile ->
                if (profile != null) {
                    _uiState.value = _uiState.value.copy(
                        shopProfile = profile,
                        isSoundEnabled = profile.isSoundEnabled
                    )
                }
            }
            .launchIn(viewModelScope)

        partyRepository.getPartiesStream(shopId)
            .onEach { parties ->
                _uiState.value = _uiState.value.copy(availableParties = parties)
            }
            .launchIn(viewModelScope)

        // Observe cash transactions and calculate daily cash register summary
        cashRepository.getTransactionsByShopStream(shopId)
            .onEach { allTx ->
                val partyMap = _uiState.value.availableParties.associateBy { it.id }

                var cashIn = 0L
                var cashOut = 0L

                val txWithParties = allTx.map { tx ->
                    if (tx.transactionType == TransactionType.JAMA_RECEIVED) {
                        cashIn += tx.amountPaisa
                    } else if (tx.transactionType == TransactionType.UDHAR_GIVEN) {
                        cashOut += tx.amountPaisa
                    }

                    val party = partyMap[tx.partyId]
                    TransactionWithParty(
                        transaction = tx,
                        partyName = party?.name ?: "खाता",
                        village = party?.village
                    )
                }

                _uiState.value = _uiState.value.copy(
                    todayCashInPaisa = cashIn,
                    todayCashOutPaisa = cashOut,
                    inHandCashDrawerPaisa = cashIn - cashOut,
                    todayTransactions = txWithParties.sortedByDescending { it.transaction.transactionDate }
                )
            }
            .launchIn(viewModelScope)
    }

    fun openAddEntryDialog() {
        _uiState.value = _uiState.value.copy(isAddEntryDialogOpen = true)
    }

    fun closeAddEntryDialog() {
        _uiState.value = _uiState.value.copy(isAddEntryDialogOpen = false)
    }

    fun recordDailyEntry(
        partyId: String,
        transactionType: TransactionType,
        amountRs: Long,
        paymentMode: PaymentMode = PaymentMode.CASH,
        remarks: String? = null
    ) {
        if (amountRs <= 0L) return

        val now = Clock.System.now().toEpochMilliseconds()
        val amountPaisa = amountRs * 100L

        val tx = CashTransaction(
            id = "tx_daily_${now}_${(1000..9999).random()}",
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

            val party = partyRepository.getPartyById(partyId)
            val partyName = party?.name ?: "खाता"
            val typeHindi = if (transactionType == TransactionType.JAMA_RECEIVED) "जमा मिला" else "उधार दिया"
            val speechText = "$partyName, ₹${MandiMathEngine.paisaToRupeesString(amountPaisa)} $typeHindi, गल्ला हिसाब में दर्ज।"

            ttsManager.speak(speechText, _uiState.value.isSoundEnabled)
            _uiState.value = _uiState.value.copy(isSaving = false, isAddEntryDialogOpen = false)
            _events.emit(DailyRegisterEvent.EntryRecorded(speechText))
        }
    }
}
