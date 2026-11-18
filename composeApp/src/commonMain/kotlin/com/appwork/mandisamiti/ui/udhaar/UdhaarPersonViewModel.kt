package com.appwork.mandisamiti.ui.udhaar

import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.math.RepaymentEditPlan
import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.repository.LendRepository
import com.appwork.mandisamiti.domain.repository.PartyRepository
import com.appwork.mandisamiti.domain.usecase.LendException
import com.appwork.mandisamiti.domain.usecase.LendTransactionUseCase
import com.appwork.mandisamiti.domain.usecase.UdhaarDue
import com.appwork.mandisamiti.domain.usecase.UdhaarUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class UdhaarLoanWithSettlements(
    val loan: LendEntry,
    val principalDuePaisa: Long,
    val interestDuePaisa: Long,
    val settlements: List<LendSettlement>,
) {
    val totalDuePaisa: Long get() = principalDuePaisa + interestDuePaisa
    val isSettled: Boolean get() = principalDuePaisa == 0L && interestDuePaisa == 0L
}

data class UdhaarPersonUiState(
    val partyId: String,
    val direction: LendDirection,
    val partyName: String?,
    val village: String?,
    val phone: String?,
    val principalDuePaisa: Long,
    val interestDuePaisa: Long,
    val interestOn: Boolean,
    val loans: List<UdhaarLoanWithSettlements>,
    val openLoans: List<LendEntry>,
    val minDateMs: Long?,
    val repaymentEditSettlement: LendSettlement? = null,
    val repaymentEditPreview: RepaymentEditPlan? = null,
    val repaymentEditError: String? = null,
    val repaymentHistory: List<LendSettlement>? = null,
) {
    val totalDuePaisa: Long get() = principalDuePaisa + interestDuePaisa
    val isSettled: Boolean get() = principalDuePaisa == 0L && interestDuePaisa == 0L
}

class UdhaarPersonViewModel(
    val partyId: String,
    val direction: LendDirection,
    private val shopId: String?,
    private val lendRepository: LendRepository,
    private val partyRepository: PartyRepository,
    val udhaarUseCase: UdhaarUseCase? = null,
    coroutineScope: CoroutineScope? = null,
) {
    private val scope = coroutineScope ?: CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _uiState = MutableStateFlow<UdhaarPersonUiState?>(null)
    val uiState: StateFlow<UdhaarPersonUiState?> = _uiState.asStateFlow()

    private val lendUseCase = LendTransactionUseCase(
        lendRepository,
        { IdGenerator.newId() },
        { Clock.System.now().toEpochMilliseconds() }
    )
    private var previewJob: Job? = null

    init {
        observeData()
    }

    private fun observeData() {
        val effectiveShopId = shopId ?: ""
        val lendsFlow = lendRepository.getLendEntriesStream(effectiveShopId)
        val settlementsFlow = lendRepository.getSettlementsStream(effectiveShopId)
        val partiesFlow = partyRepository.getPartiesStream(effectiveShopId)

        combine(lendsFlow, settlementsFlow, partiesFlow) { lends, settlements, parties ->
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val party = parties.find { it.id == partyId }
                ?: partyRepository.getPartyById(partyId)

            val personLoans = lends.filter {
                it.partyId == partyId &&
                    it.direction == direction &&
                    !it.isVoid &&
                    it.lendType == LendType.MONEY
            }

            val nonVoidSettlements = settlements.filter { !it.isVoid }

            var totalPrincipalDue = 0L
            var totalInterestDue = 0L
            var interestOn = false
            val openLoansList = mutableListOf<LendEntry>()
            val loansWithSettlements = mutableListOf<UdhaarLoanWithSettlements>()

            val sortedLoans = personLoans.sortedByDescending { it.entryDate }

            for (loan in sortedLoans) {
                val allLoanSettlements = settlements.filter { it.lendId == loan.id }.sortedByDescending { it.settledAt }
                val nonVoidLoanSettlements = nonVoidSettlements.filter { it.lendId == loan.id }

                val reducedPaisa = nonVoidLoanSettlements.sumOf { s ->
                    if (s.principalPaisa == 0L && s.interestPaisa == 0L) s.amountPaisa else s.principalPaisa
                }
                val outstandingPaisa = (loan.amountPaisa - reducedPaisa).coerceAtLeast(0L)
                val interest = if (outstandingPaisa > 0L) {
                    RuralInterestEngine.interestDue(
                        principalPaisa = loan.amountPaisa,
                        monthlyRatePercent = loan.monthlyRatePercent,
                        lentAtMs = loan.entryDate,
                        settlements = nonVoidLoanSettlements,
                        nowMs = nowMs,
                    )
                } else 0L

                if (outstandingPaisa > 0L || interest > 0L) {
                    totalPrincipalDue += outstandingPaisa
                    totalInterestDue += interest
                    if (loan.monthlyRatePercent > 0.0) interestOn = true
                    openLoansList.add(loan)
                }

                loansWithSettlements.add(
                    UdhaarLoanWithSettlements(
                        loan = loan,
                        principalDuePaisa = outstandingPaisa,
                        interestDuePaisa = interest,
                        settlements = allLoanSettlements,
                    )
                )
            }

            val minDateMs = openLoansList.maxOfOrNull { it.entryDate }
            val current = _uiState.value

            UdhaarPersonUiState(
                partyId = partyId,
                direction = direction,
                partyName = party?.name?.takeIf { it.isNotBlank() },
                village = party?.village?.takeIf { it.isNotBlank() },
                phone = party?.phone?.takeIf { it.isNotBlank() },
                principalDuePaisa = totalPrincipalDue,
                interestDuePaisa = totalInterestDue,
                interestOn = interestOn,
                loans = loansWithSettlements,
                openLoans = openLoansList,
                minDateMs = minDateMs,
                repaymentEditSettlement = current?.repaymentEditSettlement,
                repaymentEditPreview = current?.repaymentEditPreview,
                repaymentEditError = current?.repaymentEditError,
                repaymentHistory = current?.repaymentHistory,
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(scope)
    }

    suspend fun computeDueAt(dateMs: Long): UdhaarDue {
        val effectiveShopId = shopId ?: return UdhaarDue(0L, 0L)
        val useCase = udhaarUseCase ?: return UdhaarDue(0L, 0L)
        return useCase.totalDue(effectiveShopId, partyId, direction, dateMs)
    }

    suspend fun repay(paymentPaisa: Long, paidAt: Long): Result<Unit> {
        val effectiveShopId = shopId ?: return Result.failure(IllegalStateException("No shop selected"))
        val useCase = udhaarUseCase ?: return Result.failure(IllegalStateException("UdhaarUseCase not available"))
        return try {
            useCase.repay(
                shopId = effectiveShopId,
                partyId = partyId,
                direction = direction,
                paymentPaisa = paymentPaisa,
                paidAt = paidAt,
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun startRepaymentEdit(settlement: LendSettlement) {
        _uiState.value = _uiState.value?.copy(
            repaymentEditSettlement = settlement,
            repaymentEditPreview = null,
            repaymentEditError = null
        )
    }

    fun previewRepaymentEdit(settlement: LendSettlement, amountPaisa: Long, settledAt: Long) {
        previewJob?.cancel()
        previewJob = scope.launch {
            try {
                val loan = lendRepository.getLendEntryById(settlement.lendId) ?: return@launch
                val plan = lendUseCase.previewRepaymentEdit(
                    settlement.lendId,
                    settlement.id,
                    amountPaisa,
                    settledAt,
                    loan.monthlyRatePercent
                )
                _uiState.value = _uiState.value?.copy(repaymentEditPreview = plan, repaymentEditError = null)
            } catch (e: LendException.OverRepayment) {
                _uiState.value = _uiState.value?.copy(repaymentEditPreview = null, repaymentEditError = "OVER")
            } catch (e: IllegalArgumentException) {
                _uiState.value = _uiState.value?.copy(repaymentEditPreview = null, repaymentEditError = "INVALID")
            }
        }
    }

    suspend fun editRepayment(settlement: LendSettlement, amountPaisa: Long, settledAt: Long): Result<Unit> {
        return try {
            val loan = lendRepository.getLendEntryById(settlement.lendId) ?: return Result.failure(IllegalArgumentException("Loan not found"))
            lendUseCase.editRepayment(settlement.id, amountPaisa, settledAt, loan.monthlyRatePercent)
            clearRepaymentEdit()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearRepaymentEdit() {
        previewJob?.cancel()
        _uiState.value = _uiState.value?.copy(
            repaymentEditSettlement = null,
            repaymentEditPreview = null,
            repaymentEditError = null,
            repaymentHistory = null
        )
    }

    fun loadRepaymentHistory(settlementId: String) {
        scope.launch {
            val history = lendRepository.getSettlementHistory(settlementId)
            _uiState.value = _uiState.value?.copy(repaymentHistory = history)
        }
    }

    fun clearRepaymentHistory() {
        _uiState.value = _uiState.value?.copy(repaymentHistory = null)
    }
}
