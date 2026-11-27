package com.appwork.mandisamiti.domain.usecase

import com.appwork.mandisamiti.domain.math.AllocationResult
import com.appwork.mandisamiti.domain.math.LoanPayment
import com.appwork.mandisamiti.domain.math.OpenLoan
import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import com.appwork.mandisamiti.domain.math.UdhaarAllocator
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendKind
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.domain.model.LendStatus
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.repository.LendRepository
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class UdhaarOverpaymentException(val totalDuePaisa: Long) : IllegalArgumentException("Payment is above the amount due")

data class UdhaarDue(
    val principalPaisa: Long,
    val interestPaisa: Long,
) {
    val totalPaisa: Long get() = principalPaisa + interestPaisa
    val totalDuePaisa: Long get() = totalPaisa
}

data class UdhaarLoanForm(
    val shopId: String,
    val partyId: String,
    val direction: LendDirection,
    val amountPaisa: Long,
    val date: Long,
    val kind: LendKind = LendKind.LOAN,
    val monthlyRatePercent: Double = 0.0,
    val receiverName: String? = null,
    val receiverPhone: String? = null,
    val notes: String? = null,
    val paymentMode: PaymentMode = PaymentMode.CASH,
)

enum class CashSide { GAVE, GOT }

sealed interface PlainCashResult {
    data class Repaid(val parts: List<LoanPayment>) : PlainCashResult
    data class NewLoan(val loan: LendEntry) : PlainCashResult
}

class UdhaarUseCase(
    private val repo: LendRepository,
    private val newId: () -> String,
    private val nowMillis: () -> Long,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    /** Returns true when [dateMs] falls on a calendar day strictly before [nowMs] in [tz]. */
    fun isOlderThanToday(
        dateMs: Long,
        nowMs: Long = nowMillis(),
        tz: TimeZone = timeZone,
    ): Boolean {
        val entryDate = Instant.fromEpochMilliseconds(dateMs).toLocalDateTime(tz).date
        val todayDate = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date
        return entryDate < todayDate
    }

    suspend fun createLoan(form: UdhaarLoanForm): LendEntry {
        require(form.amountPaisa > 0L) { "Amount must be above zero" }
        require(form.monthlyRatePercent >= 0.0) { "Interest rate cannot be negative" }
        val now = nowMillis()
        val cleanNotes = form.notes?.trim()?.takeIf { it.isNotEmpty() }
        val loan = LendEntry(
            id = newId(),
            shopId = form.shopId,
            partyId = form.partyId,
            lendType = LendType.MONEY,
            entryDate = form.date,
            amountPaisa = form.amountPaisa,
            notes = cleanNotes,
            status = LendStatus.OPEN,
            createdAt = now,
            updatedAt = now,
            monthlyRatePercent = form.monthlyRatePercent,
            direction = form.direction,
            kind = form.kind,
            receiverName = form.receiverName?.trim()?.takeIf { form.direction == LendDirection.GIVEN && it.isNotEmpty() },
            receiverPhone = form.receiverPhone?.trim()?.takeIf { form.direction == LendDirection.GIVEN && it.isNotEmpty() },
        )
        val transactionType = when (form.direction) {
            LendDirection.GIVEN -> TransactionType.UDHAR_GIVEN
            LendDirection.TAKEN -> TransactionType.JAMA_RECEIVED
        }
        val effectivePaymentMode = if (
            form.paymentMode == PaymentMode.BOOK_ENTRY ||
            isOlderThanToday(form.date, now, timeZone)
        ) {
            PaymentMode.BOOK_ENTRY
        } else {
            form.paymentMode
        }
        val cashRemarks = if (cleanNotes != null) "$cleanNotes [LEND:${loan.id}]" else "उधार [LEND:${loan.id}]"
        val cash = CashTransaction(
            id = newId(),
            shopId = form.shopId,
            partyId = form.partyId,
            transactionType = transactionType,
            amountPaisa = form.amountPaisa,
            paymentMode = effectivePaymentMode,
            transactionDate = form.date,
            remarks = cashRemarks,
            createdAt = now,
            updatedAt = now,
            sourceEntryId = null,
            lendId = loan.id,
        )
        repo.saveLoanWithCash(loan, cash)
        return loan
    }

    suspend fun totalDue(
        shopId: String,
        partyId: String,
        direction: LendDirection,
        atMs: Long,
    ): UdhaarDue {
        val loans = openLoans(shopId, partyId, direction, atMs)
        return UdhaarDue(
            principalPaisa = loans.sumOf { it.outstandingPrincipalPaisa },
            interestPaisa = loans.sumOf { it.interestDuePaisa },
        )
    }

    suspend fun repay(
        shopId: String,
        partyId: String,
        direction: LendDirection,
        paymentPaisa: Long,
        paidAt: Long,
        mode: PaymentMode = PaymentMode.CASH,
        remarks: String? = null,
    ): List<LoanPayment> {
        require(paymentPaisa > 0L) { "Payment must be above zero" }
        val loans = openLoans(shopId, partyId, direction, paidAt)
        val allocation = UdhaarAllocator.allocate(loans, paymentPaisa)
        if (allocation is AllocationResult.Over) throw UdhaarOverpaymentException(allocation.totalDuePaisa)
        allocation as AllocationResult.Ok

        val items = allocation.parts.map { part ->
            val loan = repo.getLendEntryById(part.lendId) ?: throw IllegalArgumentException("Loan ${part.lendId} does not exist")
            val settlementId = newId()
            val amount = part.principalPaisa + part.interestPaisa
            val settlement = LendSettlement(
                id = settlementId,
                shopId = loan.shopId,
                lendId = loan.id,
                settledAt = paidAt,
                amountPaisa = amount,
                createdAt = nowMillis(),
                updatedAt = nowMillis(),
                principalPaisa = part.principalPaisa,
                interestPaisa = part.interestPaisa,
                calculatedInterestPaisa = part.interestPaisa,
            )
            val rows = buildList {
                if (part.principalPaisa > 0L) add(
                    cashRow(loan, settlementId, part.principalPaisa, paidAt, mode, remarks, principalType(direction))
                )
                if (part.interestPaisa > 0L) add(
                    cashRow(loan, settlementId, part.interestPaisa, paidAt, mode, remarks, interestType(direction))
                )
            }
            settlement to rows
        }
        repo.settleAll(items)
        return allocation.parts
    }

    suspend fun plainCash(
        shopId: String,
        partyId: String,
        side: CashSide,
        amountPaisa: Long,
        date: Long,
        note: String? = null,
    ): PlainCashResult {
        require(amountPaisa > 0L) { "Amount must be above zero" }
        val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }
        val opposite = if (side == CashSide.GAVE) LendDirection.TAKEN else LendDirection.GIVEN
        val loans = openLoans(shopId, partyId, opposite, date)
        if (loans.any { it.outstandingPrincipalPaisa + it.interestDuePaisa > 0L }) {
            return PlainCashResult.Repaid(repay(shopId, partyId, opposite, amountPaisa, date, remarks = cleanNote))
        }
        val direction = if (side == CashSide.GAVE) LendDirection.GIVEN else LendDirection.TAKEN
        return PlainCashResult.NewLoan(createLoan(UdhaarLoanForm(
            shopId = shopId,
            partyId = partyId,
            direction = direction,
            amountPaisa = amountPaisa,
            date = date,
            kind = LendKind.CASH,
            notes = cleanNote,
        )))
    }

    private suspend fun openLoans(shopId: String, partyId: String, direction: LendDirection, nowMs: Long): List<OpenLoan> =
        repo.getLoansForParty(shopId, partyId, direction).mapNotNull { loan ->
            if (loan.isVoid || loan.lendType != LendType.MONEY) return@mapNotNull null
            val settlements = repo.getSettlements(loan.id).filter { !it.isVoid }
            val principal = (loan.amountPaisa - settlements.sumOf { effectivePrincipal(it) }).coerceAtLeast(0L)
            val interest = RuralInterestEngine.interestDue(
                principalPaisa = loan.amountPaisa,
                monthlyRatePercent = loan.monthlyRatePercent,
                lentAtMs = loan.entryDate,
                settlements = settlements,
                nowMs = nowMs,
            )
            OpenLoan(loan.id, loan.entryDate, principal, interest)
        }

    private fun effectivePrincipal(settlement: LendSettlement): Long =
        if (settlement.principalPaisa == 0L && settlement.interestPaisa == 0L) settlement.amountPaisa else settlement.principalPaisa

    private fun principalType(direction: LendDirection) = when (direction) {
        LendDirection.GIVEN -> TransactionType.JAMA_RECEIVED
        LendDirection.TAKEN -> TransactionType.UDHAR_GIVEN
    }

    private fun interestType(direction: LendDirection) = when (direction) {
        LendDirection.GIVEN -> TransactionType.INTEREST_RECEIVED
        LendDirection.TAKEN -> TransactionType.UDHAR_GIVEN
    }

    private fun cashRow(
        loan: LendEntry,
        settlementId: String,
        amountPaisa: Long,
        date: Long,
        mode: PaymentMode,
        note: String?,
        type: TransactionType,
    ) = CashTransaction(
        id = newId(),
        shopId = loan.shopId,
        partyId = loan.partyId,
        transactionType = type,
        amountPaisa = amountPaisa,
        paymentMode = mode,
        transactionDate = date,
        remarks = "${note?.trim()?.takeIf { it.isNotEmpty() } ?: "उधार वापसी"} [SETTLE:$settlementId:${loan.id}]",
        createdAt = nowMillis(),
        updatedAt = nowMillis(),
        sourceEntryId = null,
        lendId = loan.id,
    )
}
