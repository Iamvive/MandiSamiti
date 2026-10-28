package com.appwork.mandisamiti.ui.register

import com.appwork.mandisamiti.domain.cash.CashDrawer
import com.appwork.mandisamiti.domain.id.IdGenerator
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
import kotlinx.datetime.TimeZone

data class TransactionWithParty(
    val transaction: CashTransaction,
    val partyName: String,
    val village: String?
)

data class DailyRegisterUiState(
    val shopProfile: ShopProfile? = null,
    val isSoundEnabled: Boolean = true,
    val openingCashPaisa: Long = 0L,
    val todayCashInPaisa: Long = 0L,
    val todayCashOutPaisa: Long = 0L,
    val inHandCashDrawerPaisa: Long = 0L,
    val todayTransactions: List<TransactionWithParty> = emptyList(),
    val availableParties: List<Party> = emptyList(),
    val isAddEntryDialogOpen: Boolean = false,
    val isDayClosingSummaryOpen: Boolean = false,
    val isSaving: Boolean = false
)

sealed interface DailyRegisterEvent {
    data class EntryRecorded(val speechText: String) : DailyRegisterEvent
}

enum class CashReconciliationStatus { MATCH, SURPLUS, SHORTAGE }

data class CashReconciliation(
    val expectedPaisa: Long,
    val countedPaisa: Long,
    val differencePaisa: Long,
    val status: CashReconciliationStatus
)

class DailyRegisterViewModel(
    private val shopId: String,
    private val cashRepository: CashTransactionRepository,
    private val partyRepository: PartyRepository,
    private val shopProfileRepository: ShopProfileRepository,
    private val ttsManager: SoundboxTtsManager,
    private val viewModelScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    private val onLocalWrite: () -> Unit = {}
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

        // Combined so a party list that loads after the cash list still renames the rows.
        combine(
            partyRepository.getPartiesStream(shopId),
            cashRepository.getTransactionsByShopStream(shopId)
        ) { parties, allTx -> parties to allTx }
            .onEach { (parties, allTx) ->
                val window = CashDrawer.dayWindow(clock.now(), timeZone)
                val summary = CashDrawer.summarize(allTx, window)
                val partyMap = parties.associateBy { it.id }
                val todays = allTx
                    .filter { it.transactionDate >= window.startMs && it.transactionDate < window.endMs }
                    .map { tx ->
                        val party = partyMap[tx.partyId]
                        TransactionWithParty(transaction = tx, partyName = party?.name ?: "खाता", village = party?.village)
                    }
                _uiState.value = _uiState.value.copy(
                    availableParties = parties,
                    openingCashPaisa = summary.openingPaisa,
                    todayCashInPaisa = summary.cashInPaisa,
                    todayCashOutPaisa = summary.cashOutPaisa,
                    inHandCashDrawerPaisa = summary.closingPaisa,
                    todayTransactions = todays.sortedByDescending { it.transaction.transactionDate }
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

    fun openDayClosingSummary() {
        _uiState.value = _uiState.value.copy(isDayClosingSummaryOpen = true)
    }

    fun closeDayClosingSummary() {
        _uiState.value = _uiState.value.copy(isDayClosingSummaryOpen = false)
    }

    fun reconcilePhysicalCash(countedRs: Long): CashReconciliation {
        val expected = _uiState.value.inHandCashDrawerPaisa
        val countedPaisa = countedRs * 100L
        val diff = countedPaisa - expected
        val status = when {
            diff == 0L -> CashReconciliationStatus.MATCH
            diff > 0L -> CashReconciliationStatus.SURPLUS
            else -> CashReconciliationStatus.SHORTAGE
        }
        return CashReconciliation(
            expectedPaisa = expected,
            countedPaisa = countedPaisa,
            differencePaisa = diff,
            status = status
        )
    }

    fun generateDayClosingReportText(physicalCountRs: Long? = null): String {
        val profile = _uiState.value.shopProfile
        val shopName = profile?.shopName ?: "मंडी आढ़त"
        val mandiLocation = profile?.mandiName ?: "मंडी समिति"
        val opening = MandiMathEngine.paisaToRupeesString(_uiState.value.openingCashPaisa)
        val cashIn = MandiMathEngine.paisaToRupeesString(_uiState.value.todayCashInPaisa)
        val cashOut = MandiMathEngine.paisaToRupeesString(_uiState.value.todayCashOutPaisa)
        val closing = MandiMathEngine.paisaToRupeesString(_uiState.value.inHandCashDrawerPaisa)
        val txnCount = _uiState.value.todayTransactions.size

        val dateStr = clock.now().toString().take(10)

        val reconciliationPart = if (physicalCountRs != null) {
            val recon = reconcilePhysicalCash(physicalCountRs)
            val countedStr = MandiMathEngine.paisaToRupeesString(recon.countedPaisa)
            val diffStr = MandiMathEngine.paisaToRupeesString(if (recon.differencePaisa >= 0) recon.differencePaisa else -recon.differencePaisa)
            when (recon.status) {
                CashReconciliationStatus.MATCH -> "\n🔍 *गल्ला मिलान:* ₹$countedStr (बिल्कुल बराबर)"
                CashReconciliationStatus.SURPLUS -> "\n🔍 *गल्ला मिलान:* ₹$countedStr (फालतू: +₹$diffStr)"
                CashReconciliationStatus.SHORTAGE -> "\n🔍 *गल्ला मिलान:* ₹$countedStr (कमी: -₹$diffStr)"
            }
        } else ""

        return """
            🌾 *दैनिक गल्ला रोकड़ बही — रोज़नामा*
            🏪 *दुकान:* $shopName ($mandiLocation)
            📅 *दिनांक:* $dateStr
            ━━━━━━━━━━━━━━━━━━
            💵 *पिछला शेष (Opening):* ₹$opening
            🟢 *आज की कुल आवक (Cash In):* ₹$cashIn
            🔴 *आज की कुल निकासी (Cash Out):* ₹$cashOut
            ━━━━━━━━━━━━━━━━━━
            💰 *गल्ले में शुद्ध नकदी (Closing In Hand):* ₹$closing$reconciliationPart
            📝 *कुल लेन-देन संख्या:* $txnCount प्रविष्टियाँ
            ━━━━━━━━━━━━━━━━━━
            _मंडी समिति डिजिटल बहीखाता_
        """.trimIndent()
    }

    fun announceDayClosingSummary(): String {
        val closing = MandiMathEngine.paisaToRupeesString(_uiState.value.inHandCashDrawerPaisa)
        val speech = "आज का दिन समाप्त। गल्ले में कुल नकदी ₹$closing है।"
        ttsManager.speak(speech, _uiState.value.isSoundEnabled)
        return speech
    }

    fun recordDailyEntry(
        partyId: String,
        transactionType: TransactionType,
        amountRs: Long,
        paymentMode: PaymentMode = PaymentMode.CASH,
        remarks: String? = null
    ) {
        if (amountRs <= 0L) return

        val now = clock.now().toEpochMilliseconds()
        val amountPaisa = amountRs * 100L

        val tx = CashTransaction(
            id = IdGenerator.newId(),
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
            onLocalWrite()

            val party = partyRepository.getPartyById(partyId)
            val partyName = party?.name ?: "खाता"
            val typeHindi = if (transactionType == TransactionType.JAMA_RECEIVED) "जमा मिला" else "उधार दिया"
            val speechText = "$partyName, ₹${MandiMathEngine.paisaToRupeesString(amountPaisa)} $typeHindi, गल्ला हिसाब में दर्ज।"

            ttsManager.speak(speechText, _uiState.value.isSoundEnabled)
            _uiState.value = _uiState.value.copy(isSaving = false, isAddEntryDialogOpen = false)
            _events.emit(DailyRegisterEvent.EntryRecorded(speechText))
        }
    }

    fun setOrAdjustOpeningBalance(targetOpeningRs: Long, remarks: String? = null) {
        val targetPaisa = targetOpeningRs * 100L
        val currentOpening = _uiState.value.openingCashPaisa
        val deltaPaisa = targetPaisa - currentOpening
        if (deltaPaisa == 0L) return

        val window = CashDrawer.dayWindow(clock.now(), timeZone)
        val txDate = window.startMs - 1000L
        val now = clock.now().toEpochMilliseconds()
        val isPositive = deltaPaisa > 0L
        val amountPaisa = if (isPositive) deltaPaisa else -deltaPaisa
        val type = if (isPositive) TransactionType.JAMA_RECEIVED else TransactionType.UDHAR_GIVEN
        val defaultRemarks = if (isPositive) {
            remarks ?: "प्रारंभिक रोकड़ शेष (Opening Float)"
        } else {
            remarks ?: "प्रारंभिक रोकड़ समायोजन (Opening Adjustment)"
        }

        val tx = CashTransaction(
            id = IdGenerator.newId(),
            shopId = shopId,
            partyId = _uiState.value.availableParties.firstOrNull()?.id ?: "GALLA_DRAWER",
            transactionType = type,
            amountPaisa = amountPaisa,
            paymentMode = PaymentMode.CASH,
            transactionDate = txDate,
            remarks = defaultRemarks,
            createdAt = now,
            updatedAt = now
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            cashRepository.recordTransaction(tx)
            onLocalWrite()
            val speech = "प्रारंभिक रोकड़ शेष ₹$targetOpeningRs निर्धारित किया गया।"
            ttsManager.speak(speech, _uiState.value.isSoundEnabled)
            _uiState.value = _uiState.value.copy(isSaving = false)
            _events.emit(DailyRegisterEvent.EntryRecorded(speech))
        }
    }

    fun recordCashWithdrawalHome(amountRs: Long, remarks: String? = null) {
        if (amountRs <= 0L) return
        val now = clock.now().toEpochMilliseconds()
        val amountPaisa = amountRs * 100L

        val tx = CashTransaction(
            id = IdGenerator.newId(),
            shopId = shopId,
            partyId = _uiState.value.availableParties.firstOrNull()?.id ?: "GALLA_DRAWER",
            transactionType = TransactionType.UDHAR_GIVEN,
            amountPaisa = amountPaisa,
            paymentMode = PaymentMode.CASH,
            transactionDate = now,
            remarks = remarks ?: "घर ले गए / गल्ला निकासी (Owner Cash Home)",
            createdAt = now,
            updatedAt = now
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            cashRepository.recordTransaction(tx)
            onLocalWrite()
            val speech = "गल्ले से ₹$amountRs घर निकासी दर्ज।"
            ttsManager.speak(speech, _uiState.value.isSoundEnabled)
            _uiState.value = _uiState.value.copy(isSaving = false)
            _events.emit(DailyRegisterEvent.EntryRecorded(speech))
        }
    }

    fun recordDiscrepancyAdjustment(differencePaisa: Long) {
        if (differencePaisa == 0L) return
        val now = clock.now().toEpochMilliseconds()
        val isSurplus = differencePaisa > 0L
        val amountPaisa = if (isSurplus) differencePaisa else -differencePaisa
        val type = if (isSurplus) TransactionType.JAMA_RECEIVED else TransactionType.UDHAR_GIVEN
        val remarks = if (isSurplus) "गल्ला मिलान बचत (Cash Surplus Adjustment)" else "गल्ला मिलान कमी (Cash Shortage Adjustment)"

        val tx = CashTransaction(
            id = IdGenerator.newId(),
            shopId = shopId,
            partyId = _uiState.value.availableParties.firstOrNull()?.id ?: "GALLA_DRAWER",
            transactionType = type,
            amountPaisa = amountPaisa,
            paymentMode = PaymentMode.CASH,
            transactionDate = now,
            remarks = remarks,
            createdAt = now,
            updatedAt = now
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            cashRepository.recordTransaction(tx)
            onLocalWrite()
            val diffRs = MandiMathEngine.paisaToRupeesString(amountPaisa)
            val speech = if (isSurplus) "गल्ला बचत ₹$diffRs समायोजित की गई।" else "गल्ला कमी ₹$diffRs समायोजित की गई।"
            ttsManager.speak(speech, _uiState.value.isSoundEnabled)
            _uiState.value = _uiState.value.copy(isSaving = false)
            _events.emit(DailyRegisterEvent.EntryRecorded(speech))
        }
    }
}
