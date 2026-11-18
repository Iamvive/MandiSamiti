package com.appwork.mandisamiti.ui.ledger

import com.appwork.mandisamiti.data.sync.NoOpSyncScheduler
import com.appwork.mandisamiti.data.sync.SyncScheduler
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.usecase.LendException
import com.appwork.mandisamiti.domain.usecase.LendTransactionUseCase
import com.appwork.mandisamiti.domain.math.InterestCalculation
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.domain.repository.BuyRepository
import com.appwork.mandisamiti.domain.repository.CashTransactionRepository
import com.appwork.mandisamiti.domain.repository.DealRepository
import com.appwork.mandisamiti.domain.repository.LendRepository
import com.appwork.mandisamiti.domain.repository.PartyRepository
import com.appwork.mandisamiti.domain.repository.SellRepository
import com.appwork.mandisamiti.domain.repository.ShopProfileRepository
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

sealed interface LedgerItem {
    val timestamp: Long
    val tag: com.appwork.mandisamiti.domain.ledger.LedgerTag? get() = null

    data class BuyItem(
        val buy: BuyEntry,
        override val timestamp: Long = buy.entryDate
    ) : LedgerItem {
        override val tag: com.appwork.mandisamiti.domain.ledger.LedgerTag get() = com.appwork.mandisamiti.domain.ledger.LedgerTag.Slip("BUY", buy.id)
    }

    data class SellItem(
        val sell: SellEntry,
        override val timestamp: Long = sell.entryDate
    ) : LedgerItem {
        override val tag: com.appwork.mandisamiti.domain.ledger.LedgerTag get() = com.appwork.mandisamiti.domain.ledger.LedgerTag.Slip("SELL", sell.id)
    }

    data class LendItem(
        val lend: LendEntry,
        override val timestamp: Long = lend.createdAt
    ) : LedgerItem {
        override val tag: com.appwork.mandisamiti.domain.ledger.LedgerTag get() = com.appwork.mandisamiti.domain.ledger.LedgerTag.Udhaar
    }

    data class DealItem(
        val deal: Deal,
        override val timestamp: Long = deal.dealDate
    ) : LedgerItem {
        override val tag: com.appwork.mandisamiti.domain.ledger.LedgerTag get() = com.appwork.mandisamiti.domain.ledger.LedgerTag.Slip("DEAL", deal.id)
    }

    data class CashItem(
        val transaction: CashTransaction,
        override val timestamp: Long = transaction.transactionDate
    ) : LedgerItem {
        /** Slip, loan, or null for plain cash. */
        override val tag: com.appwork.mandisamiti.domain.ledger.LedgerTag? get() = com.appwork.mandisamiti.domain.ledger.ledgerTagFor(transaction)
    }
}

data class OpenLoanInfo(
    val lend: LendEntry,
    val outstandingPaisa: Long,
    val interestDuePaisa: Long = 0L,
    val monthlyRatePercent: Double = 0.0,
    val settlements: List<LendSettlement> = emptyList()
)

data class PartyLedgerUiState(
    val party: Party? = null,
    val isSoundEnabled: Boolean = true,
    val slipBalancePaisa: Long = 0L,
    val udhaarGivenPaisa: Long = 0L,
    val udhaarTakenPaisa: Long = 0L,
    val udhaarNetPaisa: Long = 0L,
    val udhaarGivenTotalPaisa: Long = 0L,
    val udhaarGivenInterestPaisa: Long = 0L,
    val udhaarTakenTotalPaisa: Long = 0L,
    val udhaarTakenInterestPaisa: Long = 0L,
    val ledgerItems: List<LedgerItem> = emptyList(),
    val openLoans: List<OpenLoanInfo> = emptyList(),
    val loansInfoById: Map<String, OpenLoanInfo> = emptyMap(),
    val isCashDialogOpen: Boolean = false,
    val isInterestDialogOpen: Boolean = false,
    val calculatedInterest: InterestCalculation? = null,
    val repaymentEditPreview: com.appwork.mandisamiti.domain.math.RepaymentEditPlan? = null,
    val repaymentEditError: String? = null,
    val repaymentHistory: List<com.appwork.mandisamiti.domain.model.LendSettlement> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null
)

sealed interface PartyLedgerEvent {
    data class TransactionRecorded(val speechText: String) : PartyLedgerEvent
    data class Error(val message: String) : PartyLedgerEvent
}

class PartyLedgerViewModel(
    val shopId: String,
    val partyId: String,
    val partyRepository: PartyRepository,
    val cashRepository: CashTransactionRepository,
    val dealRepository: DealRepository,
    val shopProfileRepository: ShopProfileRepository,
    val ttsManager: SoundboxTtsManager,
    val buyRepository: BuyRepository? = null,
    val sellRepository: SellRepository? = null,
    val lendRepository: LendRepository? = null,
    val udhaarUseCase: com.appwork.mandisamiti.domain.usecase.UdhaarUseCase? = null,
    val syncScheduler: SyncScheduler = NoOpSyncScheduler(),
    private val viewModelScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    fun onPlainCash(
        side: com.appwork.mandisamiti.domain.usecase.CashSide,
        amountPaisa: Long,
        dateMs: Long = Clock.System.now().toEpochMilliseconds(),
        note: String? = null,
    ) {
        val useCase = udhaarUseCase ?: return
        if (amountPaisa <= 0L) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                useCase.plainCash(
                    shopId = shopId,
                    partyId = partyId,
                    side = side,
                    amountPaisa = amountPaisa,
                    date = dateMs
                )
                val party = _uiState.value.party ?: partyRepository.getPartyById(partyId)
                val partyName = party?.name ?: ""
                val amountRs = amountPaisa / 100L
                val speech = when (side) {
                    com.appwork.mandisamiti.domain.usecase.CashSide.GAVE -> "${partyName} को ₹${MandiMathEngine.formatIndianGrouping(amountRs)} दिए"
                    com.appwork.mandisamiti.domain.usecase.CashSide.GOT -> "${partyName} से ₹${MandiMathEngine.formatIndianGrouping(amountRs)} प्राप्त हुए"
                }
                ttsManager.speak(speech, _uiState.value.isSoundEnabled)
                _uiState.value = _uiState.value.copy(isSaving = false)
                _events.emit(PartyLedgerEvent.TransactionRecorded(speech))
                syncScheduler.scheduleOneTimeSync()
            } catch (e: com.appwork.mandisamiti.domain.usecase.UdhaarOverpaymentException) {
                val dueRs = e.totalDuePaisa / 100L
                val msg = "बाकी सिर्फ ₹${MandiMathEngine.formatIndianGrouping(dueRs)} है"
                _uiState.value = _uiState.value.copy(isSaving = false, error = msg)
                _events.emit(PartyLedgerEvent.Error(msg))
            } catch (e: Exception) {
                val msg = e.message ?: "त्रुटि हुई"
                _uiState.value = _uiState.value.copy(isSaving = false, error = msg)
                _events.emit(PartyLedgerEvent.Error(msg))
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private val _uiState = MutableStateFlow(PartyLedgerUiState())
    val uiState: StateFlow<PartyLedgerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PartyLedgerEvent>()
    val events: SharedFlow<PartyLedgerEvent> = _events.asSharedFlow()

    init {
        loadPartyData()
    }

    fun loadPartyData() {
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
            observeLedgerStream(party?.name)
        }

        partyRepository.getPartyBalanceStream(partyId)
            .onEach { balance ->
                if (balance != null) {
                    _uiState.value = _uiState.value.copy(
                        slipBalancePaisa = balance.slipBalancePaisa,
                        udhaarGivenPaisa = balance.udhaarGivenPaisa,
                        udhaarTakenPaisa = balance.udhaarTakenPaisa,
                        udhaarNetPaisa = balance.udhaarNetPaisa,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeLedgerStream(partyName: String?) {
        val buysFlow = buyRepository?.getBuyEntriesStream(shopId) ?: flowOf(emptyList())
        val sellsFlow = sellRepository?.getSellEntriesStream(shopId) ?: flowOf(emptyList())
        val lendsFlow = lendRepository?.getLendEntriesStream(shopId) ?: flowOf(emptyList())
        val settlementsFlow = lendRepository?.getSettlementsStream(shopId) ?: flowOf(emptyList())

        val tradeItemsFlow = combine(buysFlow, sellsFlow, lendsFlow, settlementsFlow) { buysList, sellsList, lendsList, settlementsList ->
            val partyLends = lendsList.filter { it.partyId == partyId || (partyName != null && it.partyId.equals(partyName, ignoreCase = true)) }

            val allLoansInfo = partyLends.filter { !it.isVoid }.associate { lend ->
                val nonVoidSettlements = settlementsList.filter { it.lendId == lend.id && !it.isVoid }
                val repaidPaisa = nonVoidSettlements.sumOf { it.amountPaisa }
                val isMoney = lend.lendType == com.appwork.mandisamiti.domain.model.LendType.MONEY
                val reducedPaisa = if (isMoney) nonVoidSettlements.sumOf { it.principalPaisa } else repaidPaisa
                val outstandingPaisa = (lend.amountPaisa - reducedPaisa).coerceAtLeast(0L)
                val interestDue = if (isMoney && outstandingPaisa > 0L) {
                    RuralInterestEngine.interestDue(lend.amountPaisa, lend.monthlyRatePercent, lend.entryDate, nonVoidSettlements, Clock.System.now().toEpochMilliseconds())
                } else 0L
                lend.id to OpenLoanInfo(
                    lend = lend,
                    outstandingPaisa = outstandingPaisa,
                    interestDuePaisa = interestDue,
                    monthlyRatePercent = lend.monthlyRatePercent,
                    settlements = nonVoidSettlements
                )
            }

            val openLends = partyLends.filter { !it.isVoid && it.status != com.appwork.mandisamiti.domain.model.LendStatus.SETTLED }.mapNotNull { lend ->
                val info = allLoansInfo[lend.id] ?: return@mapNotNull null
                val isOpen = if (lend.lendType == com.appwork.mandisamiti.domain.model.LendType.MONEY) {
                    info.outstandingPaisa > 0L
                } else {
                    val nonVoidSettlements = info.settlements
                    val repaidGrams = nonVoidSettlements.sumOf { it.weightGrams }
                    val outstandingGrams = (lend.weightGrams - repaidGrams).coerceAtLeast(0L)
                    outstandingGrams > 0L
                }
                if (isOpen) info else null
            }

            Triple(buysList, sellsList, partyLends) to (openLends to allLoansInfo)
        }

        combine(
            cashRepository.getTransactionsByPartyStream(partyId),
            dealRepository.getDealsByShopStream(shopId),
            tradeItemsFlow
        ) { cashList, dealsList, (trades, loansPair) ->
            val (buysList, sellsList, partyLends) = trades
            val (openLends, allLoansInfo) = loansPair
            val allItems = mutableListOf<LedgerItem>()

            // 1. Cash transactions
            cashList.forEach { allItems.add(LedgerItem.CashItem(it)) }

            // 2. Buy entries (farmer purchases)
            buysList.filter { it.farmerId == partyId || (partyName != null && it.farmerId.equals(partyName, ignoreCase = true)) }
                .forEach { allItems.add(LedgerItem.BuyItem(it)) }

            // 3. Sell entries (buyer sales)
            sellsList.filter { it.buyerId == partyId || (partyName != null && it.buyerId.equals(partyName, ignoreCase = true)) }
                .forEach { allItems.add(LedgerItem.SellItem(it)) }

            // 4. Lend entries (advances / loans)
            partyLends.forEach { allItems.add(LedgerItem.LendItem(it)) }

            // 5. Legacy deals
            dealsList.filter { it.farmerId == partyId || it.buyerId == partyId || (partyName != null && (it.farmerId.equals(partyName, ignoreCase = true) || it.buyerId.equals(partyName, ignoreCase = true))) }
                .forEach { allItems.add(LedgerItem.DealItem(it)) }

            allItems.sortedByDescending { it.timestamp } to (openLends to allLoansInfo)
        }.onEach { (sortedItems, loansPair) ->
            val (openLends, allLoansInfo) = loansPair
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val givenDue = udhaarUseCase?.totalDue(shopId, partyId, com.appwork.mandisamiti.domain.model.LendDirection.GIVEN, nowMs)
            val takenDue = udhaarUseCase?.totalDue(shopId, partyId, com.appwork.mandisamiti.domain.model.LendDirection.TAKEN, nowMs)

            val givenLoans = allLoansInfo.values.filter { it.lend.direction == com.appwork.mandisamiti.domain.model.LendDirection.GIVEN && it.lend.lendType == com.appwork.mandisamiti.domain.model.LendType.MONEY }
            val takenLoans = allLoansInfo.values.filter { it.lend.direction == com.appwork.mandisamiti.domain.model.LendDirection.TAKEN && it.lend.lendType == com.appwork.mandisamiti.domain.model.LendType.MONEY }

            val fallbackGivenPrincipal = givenLoans.sumOf { it.outstandingPaisa }
            val fallbackGivenInterest = givenLoans.sumOf { it.interestDuePaisa }
            val fallbackTakenPrincipal = takenLoans.sumOf { it.outstandingPaisa }
            val fallbackTakenInterest = takenLoans.sumOf { it.interestDuePaisa }

            _uiState.value = _uiState.value.copy(
                ledgerItems = sortedItems,
                openLoans = openLends,
                loansInfoById = allLoansInfo,
                udhaarGivenTotalPaisa = givenDue?.totalDuePaisa ?: (fallbackGivenPrincipal + fallbackGivenInterest),
                udhaarGivenInterestPaisa = givenDue?.interestPaisa ?: fallbackGivenInterest,
                udhaarTakenTotalPaisa = takenDue?.totalDuePaisa ?: (fallbackTakenPrincipal + fallbackTakenInterest),
                udhaarTakenInterestPaisa = takenDue?.interestPaisa ?: fallbackTakenInterest,
            )
        }.launchIn(viewModelScope)
    }

    fun applyCashEntryToLoan(lendId: String, amountRs: Long, note: String? = null) =
        repayLoan(lendId, amountRs * 100L, interestPaisa = 0L, calculatedInterestPaisa = 0L, note = note)

    /** Deposit larger than the loan: outstanding goes to the loan, the rest is a general jama, one transaction. */
    fun applyDepositSplitAcrossLoan(lendId: String, amountRs: Long, note: String? = null) =
        repayLoan(lendId, amountRs * 100L, interestPaisa = 0L, calculatedInterestPaisa = 0L, note = note)

    /**
     * D4.4/D4.5: [paymentPaisa] clears [interestPaisa] first (as adjusted by the trader; [calculatedInterestPaisa] is
     * what the engine computed), then principal, and any excess is a general jama.
     */
    fun repayLoan(
        lendId: String,
        paymentPaisa: Long,
        interestPaisa: Long,
        calculatedInterestPaisa: Long,
        note: String? = null,
        paymentMode: PaymentMode = PaymentMode.CASH
    ) {
        viewModelScope.launch {
            val repo = lendRepository ?: return@launch
            val useCase = LendTransactionUseCase(repo, { IdGenerator.newId() }, { Clock.System.now().toEpochMilliseconds() })
            val split = try {
                useCase.repayWithInterest(lendId, paymentPaisa, interestPaisa, calculatedInterestPaisa, note, cashPartyId = partyId, paymentMode = paymentMode)
            } catch (e: IllegalArgumentException) {
                _events.emit(PartyLedgerEvent.Error("राशि दर्ज नहीं हो सकी"))
                return@launch
            }
            syncScheduler.scheduleOneTimeSync()
            val parts = buildList {
                if (split.interestPaisa > 0) add("₹${MandiMathEngine.paisaToRupeesString(split.interestPaisa)} ब्याज")
                if (split.loanPaisa > 0) add("₹${MandiMathEngine.paisaToRupeesString(split.loanPaisa)} उधार में")
                if (split.generalPaisa > 0) add("₹${MandiMathEngine.paisaToRupeesString(split.generalPaisa)} सामान्य जमा")
            }
            _events.emit(PartyLedgerEvent.TransactionRecorded(parts.joinToString(", ") + " दर्ज किया गया"))
        }
    }

    private fun lendUseCase(): LendTransactionUseCase? {
        val repo = lendRepository ?: return null
        return LendTransactionUseCase(repo, { IdGenerator.newId() }, { Clock.System.now().toEpochMilliseconds() })
    }

    private var previewJob: Job? = null

    /** Fills [PartyLedgerUiState.repaymentEditPreview] or [PartyLedgerUiState.repaymentEditError]. Saves nothing. */
    fun previewRepaymentEdit(lendId: String, settlementId: String, amountPaisa: Long, settledAt: Long, ratePercent: Double) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            val useCase = lendUseCase() ?: return@launch
            try {
                val plan = useCase.previewRepaymentEdit(lendId, settlementId, amountPaisa, settledAt, ratePercent)
                _uiState.value = _uiState.value.copy(repaymentEditPreview = plan, repaymentEditError = null)
            } catch (e: com.appwork.mandisamiti.domain.usecase.LendException.OverRepayment) {
                _uiState.value = _uiState.value.copy(repaymentEditPreview = null, repaymentEditError = "OVER")
            } catch (e: IllegalArgumentException) {
                _uiState.value = _uiState.value.copy(repaymentEditPreview = null, repaymentEditError = "INVALID")
            }
        }
    }

    fun editRepayment(settlementId: String, amountPaisa: Long, settledAt: Long, ratePercent: Double) {
        viewModelScope.launch {
            val useCase = lendUseCase() ?: return@launch
            try {
                useCase.editRepayment(settlementId, amountPaisa, settledAt, ratePercent)
            } catch (e: IllegalArgumentException) {
                _events.emit(PartyLedgerEvent.Error("वापसी बदली नहीं जा सकी"))
                return@launch
            }
            syncScheduler.scheduleOneTimeSync()
            clearRepaymentEdit()
            _events.emit(PartyLedgerEvent.TransactionRecorded("वापसी सुधारी गई"))
        }
    }

    fun loadRepaymentHistory(settlementId: String) {
        viewModelScope.launch {
            val repo = lendRepository ?: return@launch
            _uiState.value = _uiState.value.copy(repaymentHistory = repo.getSettlementHistory(settlementId))
        }
    }

    fun clearRepaymentEdit() {
        previewJob?.cancel()
        _uiState.value = _uiState.value.copy(repaymentEditPreview = null, repaymentEditError = null, repaymentHistory = emptyList())
    }

    /**
     * Settle Buy slip with full or partial cash payment.
     * Updates BuyEntry.paidPaisa and records a linked CashTransaction that moves Galla cash.
     */
    fun settleBuySlip(
        buyId: String,
        amountPaisa: Long,
        note: String? = null
    ) {
        if (amountPaisa <= 0L) return
        viewModelScope.launch {
            val buyRepo = buyRepository ?: return@launch
            val currentBuy = buyRepo.getBuyEntryById(buyId) ?: return@launch
            if (currentBuy.isVoid) return@launch

            val newPaidPaisa = currentBuy.paidPaisa + amountPaisa
            buyRepo.editBuyEntry(currentBuy.copy(paidPaisa = newPaidPaisa))

            val now = Clock.System.now().toEpochMilliseconds()
            val slipRef = currentBuy.slipNo?.let { "#$it" } ?: buyId.takeLast(5)
            val tx = CashTransaction(
                id = IdGenerator.newId(),
                shopId = shopId,
                partyId = partyId,
                transactionType = TransactionType.UDHAR_GIVEN, // Farmer payout / cash out from galla
                amountPaisa = amountPaisa,
                paymentMode = PaymentMode.CASH,
                transactionDate = now,
                remarks = "खरीद पर्ची $slipRef भुगतान${if (!note.isNullOrBlank()) ": $note" else ""}",
                createdAt = now,
                updatedAt = now
            )
            cashRepository.recordTransaction(tx)
            syncScheduler.scheduleOneTimeSync()

            val speechText = "पर्ची $slipRef, ₹${MandiMathEngine.paisaToRupeesString(amountPaisa)} भुगतान किया गया।"
            ttsManager.speak(speechText, _uiState.value.isSoundEnabled)
            _events.emit(PartyLedgerEvent.TransactionRecorded(speechText))
        }
    }

    /**
     * Settle Sell slip with full or partial cash receipt.
     * Updates SellEntry.receivedPaisa and records a linked CashTransaction that moves Galla cash.
     */
    fun settleSellSlip(
        sellId: String,
        amountPaisa: Long,
        note: String? = null
    ) {
        if (amountPaisa <= 0L) return
        viewModelScope.launch {
            val sellRepo = sellRepository ?: return@launch
            val currentSell = sellRepo.getSellEntryById(sellId) ?: return@launch
            if (currentSell.isVoid) return@launch

            val newReceivedPaisa = currentSell.receivedPaisa + amountPaisa
            val newBalanceDue = (currentSell.totalReceivablePaisa - newReceivedPaisa).coerceAtLeast(0L)
            sellRepo.editSellEntry(currentSell.copy(
                receivedPaisa = newReceivedPaisa,
                balanceDuePaisa = newBalanceDue
            ))

            val now = Clock.System.now().toEpochMilliseconds()
            val slipRef = currentSell.slipNo?.let { "#$it" } ?: sellId.takeLast(5)
            val tx = CashTransaction(
                id = IdGenerator.newId(),
                shopId = shopId,
                partyId = partyId,
                transactionType = TransactionType.JAMA_RECEIVED, // Cash receipt into galla
                amountPaisa = amountPaisa,
                paymentMode = PaymentMode.CASH,
                transactionDate = now,
                remarks = "बिक्री पर्ची $slipRef वसूली${if (!note.isNullOrBlank()) ": $note" else ""}",
                createdAt = now,
                updatedAt = now
            )
            cashRepository.recordTransaction(tx)
            syncScheduler.scheduleOneTimeSync()

            val speechText = "बिक्री पर्ची $slipRef, ₹${MandiMathEngine.paisaToRupeesString(amountPaisa)} जमा प्राप्त।"
            ttsManager.speak(speechText, _uiState.value.isSoundEnabled)
            _events.emit(PartyLedgerEvent.TransactionRecorded(speechText))
        }
    }

    fun openCashDialog() {
        _uiState.value = _uiState.value.copy(isCashDialogOpen = true)
    }

    fun closeCashDialog() {
        _uiState.value = _uiState.value.copy(isCashDialogOpen = false)
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
            syncScheduler.scheduleOneTimeSync()

            val partyName = _uiState.value.party?.name ?: partyRepository.getPartyById(partyId)?.name ?: "खाता"
            val typeHindi = when (transactionType) {
                TransactionType.UDHAR_GIVEN -> "उधार दिया गया"
                TransactionType.JAMA_RECEIVED -> "जमा प्राप्त हुआ"
                TransactionType.INTEREST_ADDED -> "ब्याज जोड़ा गया"
                TransactionType.INTEREST_RECEIVED -> "ब्याज मिला"
                TransactionType.DISCOUNT_GIVEN -> "छूट / समझौता दिया गया"
            }
            val formattedAmount = MandiMathEngine.paisaToRupeesString(amountPaisa)
            val speechText = "$partyName, ₹$formattedAmount $typeHindi।"

            ttsManager.speak(speechText, _uiState.value.isSoundEnabled)
            _uiState.value = _uiState.value.copy(isSaving = false, isCashDialogOpen = false)
            _events.emit(PartyLedgerEvent.TransactionRecorded(speechText))
        }
    }

    fun editCash(item: LedgerItem.CashItem, amountPaisa: Long, remarks: String?) {
        viewModelScope.launch {
            val t = item.transaction
            cashRepository.editTransaction(t.id, amountPaisa, t.paymentMode, t.transactionDate, remarks)
            syncScheduler.scheduleOneTimeSync()
            _events.emit(PartyLedgerEvent.TransactionRecorded("प्रविष्टि सुधारी गई"))
        }
    }

    fun editLend(item: LedgerItem.LendItem, amountPaisa: Long, notes: String?) {
        viewModelScope.launch {
            val msg = try {
                lendRepository?.editLendEntry(item.lend.id, amountPaisa, notes)
                syncScheduler.scheduleOneTimeSync()
                "प्रविष्टि सुधारी गई"
            } catch (e: IllegalArgumentException) {
                "वापसी हो चुकी है, रकम नहीं बदल सकते"
            }
            _events.emit(PartyLedgerEvent.TransactionRecorded(msg))
        }
    }

    fun voidEntry(item: LedgerItem, reason: VoidReason) {
        viewModelScope.launch {
            when (item) {
                is LedgerItem.DealItem -> dealRepository.voidDeal(item.deal.id, reason)
                is LedgerItem.CashItem -> cashRepository.voidTransaction(item.transaction.id, reason)
                is LedgerItem.BuyItem -> buyRepository?.voidBuyEntry(item.buy.id, reason)
                is LedgerItem.SellItem -> sellRepository?.voidSellEntry(item.sell.id, reason)
                is LedgerItem.LendItem -> lendRepository?.voidLendEntry(item.lend.id, reason)
            }
            syncScheduler.scheduleOneTimeSync()
            val speech = "प्रविष्टि रद्द की गई: ${reason.labelHi}"
            ttsManager.speak(speech, _uiState.value.isSoundEnabled)
            _events.emit(PartyLedgerEvent.TransactionRecorded(speech))
        }
    }

    fun updatePartyDetails(
        name: String,
        village: String?,
        phone: String?,
        gstin: String?,
        mandiLicense: String?
    ) {
        val current = _uiState.value.party ?: return
        val cleanPhone = phone?.filter { it.isDigit() }?.takeLast(10)?.ifBlank { null }
        val now = Clock.System.now().toEpochMilliseconds()
        val updated = current.copy(
            name = name.trim().ifBlank { current.name },
            village = village?.trim()?.ifBlank { null },
            phone = cleanPhone ?: current.phone,
            gstin = if (current.partyType == PartyType.BUYER) gstin?.trim()?.uppercase()?.ifBlank { null } else null,
            mandiLicense = if (current.partyType == PartyType.BUYER) mandiLicense?.trim()?.ifBlank { null } else null,
            updatedAt = now,
            syncStatus = 0
        )
        viewModelScope.launch {
            partyRepository.saveParty(updated)
            syncScheduler.scheduleOneTimeSync()
            _uiState.value = _uiState.value.copy(party = updated)
        }
    }
}
