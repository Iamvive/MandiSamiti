package com.appwork.mandisamiti.domain.usecase

import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import com.appwork.mandisamiti.domain.model.*
import com.appwork.mandisamiti.domain.repository.LendRepository

data class LendForm(
    val shopId: String, val partyId: String, val lendType: LendType,
    val amountPaisa: Long = 0L, val commodityId: String? = null, val bags: Long = 0L, val weightGrams: Long = 0L,
    val expectedReturnDate: Long? = null, val notes: String? = null,
)

sealed class LendException(message: String) : IllegalArgumentException(message) {
    data class OverRepayment(val requestedPaisa: Long, val outstandingPaisa: Long) :
        LendException("Repayment of $requestedPaisa paisa exceeds outstanding $outstandingPaisa paisa")

    data class OverRepaymentWeight(val requestedGrams: Long, val outstandingGrams: Long) :
        LendException("Repayment of $requestedGrams g exceeds outstanding $outstandingGrams g")
}

class LendTransactionUseCase(
    private val repo: LendRepository,
    private val newId: () -> String,
    private val nowMillis: () -> Long,
) {
    suspend fun lend(form: LendForm): LendEntry {
        when (form.lendType) {
            LendType.MONEY -> require(form.amountPaisa > 0) { "Amount is required" }
            LendType.GRAIN -> require(form.weightGrams > 0 && form.commodityId != null) { "Grain and weight are required" }
        }
        val now = nowMillis()
        val entry = LendEntry(
            id = newId(), shopId = form.shopId, partyId = form.partyId, lendType = form.lendType, entryDate = now,
            amountPaisa = form.amountPaisa, commodityId = form.commodityId, bagsCount = form.bags,
            weightGrams = form.weightGrams, expectedReturnDate = form.expectedReturnDate, notes = form.notes,
            createdAt = now, updatedAt = now,
        )
        repo.saveLendEntry(entry)
        return entry
    }

    /** Cap check, settlement, JAMA_RECEIVED cash row and lend status all happen inside one repo transaction. */
    suspend fun repay(lendId: String, amountPaisa: Long, weightGrams: Long, bags: Long, note: String? = null, cashPartyId: String? = null, paymentMode: PaymentMode = PaymentMode.CASH): LendSettlement {
        val lend = repo.getLendEntryById(lendId) ?: throw IllegalArgumentException("Lend $lendId does not exist")
        require(!lend.isVoid) { "Lend $lendId is void" }
        require(amountPaisa > 0 || weightGrams > 0) { "Nothing to settle" }

        val now = nowMillis()
        val s = LendSettlement(newId(), lend.shopId, lendId, now, amountPaisa, weightGrams, bags, now, now)
        val cash = if (amountPaisa > 0L) {
            val noteText = note?.trim().orEmpty()
            val rem = if (noteText.isBlank()) "उधार वापसी जमा" else "उधार वापसी जमा: $noteText"
            CashTransaction(
                id = newId(), shopId = lend.shopId, partyId = cashPartyId ?: lend.partyId,
                transactionType = TransactionType.JAMA_RECEIVED, amountPaisa = amountPaisa,
                paymentMode = paymentMode, transactionDate = now,
                remarks = "$rem [SETTLE:${s.id}:$lendId]", createdAt = now, updatedAt = now,
            )
        } else null
        repo.settle(s, listOfNotNull(cash))
        return s
    }

    /** Deposit of [totalPaisa] against a money loan with no interest taken. See [repayWithInterest]. */
    suspend fun repayWithOverflow(lendId: String, totalPaisa: Long, note: String? = null, cashPartyId: String? = null): DepositSplit =
        repayWithInterest(lendId, totalPaisa, 0L, 0L, note, cashPartyId, PaymentMode.CASH)

    /**
     * D4.4/D4.5/D4.7: a payment of [paymentPaisa] clears [interestPaisa] first (the trader may have adjusted it away
     * from [calculatedInterestPaisa]), the rest reduces principal up to the loan's outstanding, and any excess becomes
     * a general jama. One settlement, cash rows in order (principal, interest, general), one repo transaction.
     */
    suspend fun repayWithInterest(
        lendId: String, paymentPaisa: Long, interestPaisa: Long, calculatedInterestPaisa: Long,
        note: String? = null, cashPartyId: String? = null, paymentMode: PaymentMode = PaymentMode.CASH,
    ): DepositSplit {
        val lend = repo.getLendEntryById(lendId) ?: throw IllegalArgumentException("Lend $lendId does not exist")
        require(!lend.isVoid) { "Lend $lendId is void" }
        require(lend.lendType == LendType.MONEY) { "Only money loans take a cash deposit" }
        require(paymentPaisa > 0) { "Nothing to settle" }

        val outstanding = (lend.amountPaisa - repo.getSettlements(lendId).filter { !it.isVoid }.sumOf { it.principalPaisa }).coerceAtLeast(0L)
        val interestSplit = RuralInterestEngine.splitRepayment(paymentPaisa, interestPaisa)
        val principalSplit = splitDeposit(interestSplit.principalPaisa, outstanding)
        require(principalSplit.loanPaisa > 0 || interestSplit.interestPaisa > 0) { "Nothing to settle" }

        val now = nowMillis()
        val partyId = cashPartyId ?: lend.partyId
        val noteText = note?.trim().orEmpty()
        val s = LendSettlement(
            id = newId(), shopId = lend.shopId, lendId = lendId, settledAt = now,
            amountPaisa = interestSplit.interestPaisa + principalSplit.loanPaisa, createdAt = now, updatedAt = now,
            principalPaisa = principalSplit.loanPaisa, interestPaisa = interestSplit.interestPaisa,
            calculatedInterestPaisa = calculatedInterestPaisa,
        )
        fun row(type: TransactionType, paisa: Long, remarks: String) = CashTransaction(
            id = newId(), shopId = lend.shopId, partyId = partyId, transactionType = type,
            amountPaisa = paisa, paymentMode = paymentMode, transactionDate = now, remarks = remarks,
            createdAt = now, updatedAt = now,
        )
        val loanRem = if (noteText.isBlank()) "उधार वापसी जमा" else "उधार वापसी जमा: $noteText"
        val rows = buildList {
            if (principalSplit.loanPaisa > 0) add(row(TransactionType.JAMA_RECEIVED, principalSplit.loanPaisa, "$loanRem [SETTLE:${s.id}:$lendId]"))
            if (interestSplit.interestPaisa > 0) add(row(TransactionType.INTEREST_RECEIVED, interestSplit.interestPaisa, "ब्याज जमा [SETTLE:${s.id}:$lendId]"))
            if (principalSplit.generalPaisa > 0) add(row(TransactionType.JAMA_RECEIVED, principalSplit.generalPaisa, noteText.ifBlank { "सामान्य जमा" }))
        }
        repo.settle(s, rows)
        return DepositSplit(principalSplit.loanPaisa, principalSplit.generalPaisa, interestSplit.interestPaisa)
    }
}

data class DepositSplit(val loanPaisa: Long, val generalPaisa: Long, val interestPaisa: Long = 0L)

/** Splits a deposit between a loan's outstanding balance and a general jama. Never over-credits the loan. */
fun splitDeposit(totalPaisa: Long, outstandingPaisa: Long): DepositSplit {
    val loan = minOf(totalPaisa, outstandingPaisa.coerceAtLeast(0L))
    return DepositSplit(loan, totalPaisa - loan)
}
