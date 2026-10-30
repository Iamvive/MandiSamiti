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

    data class BuyItem(
        val buy: BuyEntry,
        override val timestamp: Long = buy.entryDate
    ) : LedgerItem

    data class SellItem(
        val sell: SellEntry,
        override val timestamp: Long = sell.entryDate
    ) : LedgerItem

    data class LendItem(
        val lend: LendEntry,
        override val timestamp: Long = lend.createdAt
    ) : LedgerItem

    data class DealItem(
        val deal: Deal,
        override val timestamp: Long = deal.dealDate
    ) : LedgerItem

    data class CashItem(
        val transaction: CashTransaction,
        override val timestamp: Long = transaction.transactionDate
    ) : LedgerItem
}

data class OpenLoanInfo(
    val lend: LendEntry,
    val outstandingPaisa: Long,
    val interestDuePaisa: Long = 0L,
    val monthlyRatePercent: Double = 0.0
)

data class PartyLedgerUiState(
    val party: Party? = null,
    val isSoundEnabled: Boolean = true,
    val defaultMonthlyRatePercent: Double = 1.5,
    val balancePaisa: Long = 0L,
    val ledgerItems: List<LedgerItem> = emptyList(),
    val openLoans: List<OpenLoanInfo> = emptyList(),
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
    private val buyRepository: BuyRepository? = null,
    private val sellRepository: SellRepository? = null,
    private val lendRepository: LendRepository? = null,
    private val syncScheduler: SyncScheduler = NoOpSyncScheduler(),
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
                    _uiState.value = _uiState.value.copy(isSoundEnabled = profile.isSoundEnabled, defaultMonthlyRatePercent = profile.defaultMonthlyInterestRate)
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
                    _uiState.value = _uiState.value.copy(balancePaisa = balance.balancePaisa)
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

            val openLends = partyLends.filter { !it.isVoid && it.status != com.appwork.mandisamiti.domain.model.LendStatus.SETTLED }.mapNotNull { lend ->
                val nonVoidSettlements = settlementsList.filter { it.lendId == lend.id && !it.isVoid }
                val repaidPaisa = nonVoidSettlements.sumOf { it.amountPaisa }
                val isMoney = lend.lendType == com.appwork.mandisamiti.domain.model.LendType.MONEY
                // Interest rows are part of amountPaisa but never reduce the loan; only principal does (D4.4).
                val reducedPaisa = if (isMoney) nonVoidSettlements.sumOf { it.principalPaisa } else repaidPaisa
                val outstandingPaisa = (lend.amountPaisa - reducedPaisa).coerceAtLeast(0L)
                val repaidGrams = nonVoidSettlements.sumOf { it.weightGrams }
                val outstandingGrams = (lend.weightGrams - repaidGrams).coerceAtLeast(0L)

                val isOpen = if (lend.lendType == com.appwork.mandisamiti.domain.model.LendType.MONEY) {
                    outstandingPaisa > 0L
                } else {
                    outstandingGrams > 0L
                }

                if (isOpen) {
                    val rate = currentMonthlyRate()
                    val interestDue = if (isMoney) {
                        RuralInterestEngine.interestDue(lend.amountPaisa, rate, lend.entryDate, nonVoidSettlements, Clock.System.now().toEpochMilliseconds())
                    } else 0L
                    OpenLoanInfo(lend = lend, outstandingPaisa = outstandingPaisa, interestDuePaisa = interestDue, monthlyRatePercent = rate)
                } else {
                    null
                }
            }

            Triple(buysList, sellsList, partyLends) to openLends
        }

        combine(
            cashRepository.getTransactionsByPartyStream(partyId),
            dealRepository.getDealsByShopStream(shopId),
            tradeItemsFlow
        ) { cashList, dealsList, (trades, openLends) ->
            val (buysList, sellsList, partyLends) = trades
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

            allItems.sortedByDescending { it.timestamp } to openLends
        }.onEach { (sortedItems, openLends) ->
            _uiState.value = _uiState.value.copy(ledgerItems = sortedItems, openLoans = openLends)
        }.launchIn(viewModelScope)
    }

    private fun currentMonthlyRate(): Double =
        _uiState.value.party?.monthlyInterestRate ?: _uiState.value.defaultMonthlyRatePercent

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
                monthlyRatePercent = currentMonthlyRate(),
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

    fun recordCalculatedInterest(interestResult: InterestCalculation) {
        val now = Clock.System.now().toEpochMilliseconds()

        val tx = CashTransaction(
            id = IdGenerator.newId(),
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
            syncScheduler.scheduleOneTimeSync()

            val partyName = _uiState.value.party?.name ?: partyRepository.getPartyById(partyId)?.name ?: "खाता"
            val formattedAmount = MandiMathEngine.paisaToRupeesString(interestResult.accruedInterestPaisa)
            val speechText = "$partyName, ₹$formattedAmount ब्याज खाते में जोड़ा गया।"

            ttsManager.speak(speechText, _uiState.value.isSoundEnabled)
            _uiState.value = _uiState.value.copy(isSaving = false, isInterestDialogOpen = false)
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
