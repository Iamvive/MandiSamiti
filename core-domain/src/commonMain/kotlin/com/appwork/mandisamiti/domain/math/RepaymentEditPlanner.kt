package com.appwork.mandisamiti.domain.math

import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.domain.usecase.LendException

/** One payment that changes because of an edit, with its old and new values. */
data class SplitChange(
    val settlementId: String,
    val oldSettledAt: Long,
    val newSettledAt: Long,
    val oldAmountPaisa: Long,
    val newAmountPaisa: Long,
    val oldInterestPaisa: Long,
    val newInterestPaisa: Long,
    val oldPrincipalPaisa: Long,
    val newPrincipalPaisa: Long,
)

data class RepaymentEditPlan(
    /** Only the payments that differ from before, with revision + 1, updatedAt = now, syncStatus = 0. */
    val changedSettlements: List<LendSettlement>,
    val changes: List<SplitChange>,
    /** New amount of the edited payment minus its old amount: how far the Galla cash moves. */
    val gallaDeltaPaisa: Long,
)

/** Legacy rows with 0 principal and 0 interest were all principal. */
fun LendSettlement.effectivePrincipalPaisa(): Long =
    if (principalPaisa == 0L && interestPaisa == 0L) amountPaisa else principalPaisa

object RepaymentEditPlanner {

    fun plan(
        lend: LendEntry,
        live: List<LendSettlement>,
        editedId: String,
        newAmountPaisa: Long,
        newSettledAt: Long,
        monthlyRatePercent: Double,
        now: Long,
    ): RepaymentEditPlan {
        val edited = live.firstOrNull { it.id == editedId }
            ?: throw IllegalArgumentException("Payment $editedId does not exist")
        require(newAmountPaisa > 0) { "Amount must be positive" }
        require(newSettledAt >= lend.entryDate) { "Payment cannot be dated before the loan" }

        val moved = live
            .map { if (it.id == editedId) it.copy(amountPaisa = newAmountPaisa, settledAt = newSettledAt) else it }
            .sortedWith(compareBy({ it.settledAt }, { it.createdAt }))

        var outstanding = lend.amountPaisa
        var from = lend.entryDate
        var accrued = 0L
        var interestPaid = 0L
        val replayed = ArrayList<LendSettlement>(moved.size)
        for (s in moved) {
            accrued += RuralInterestEngine
                .calculateAccruedInterestBetweenEpochs(outstanding, monthlyRatePercent, from, s.settledAt)
                .accruedInterestPaisa
            val due = (accrued - interestPaid).coerceAtLeast(0L)
            // A payment that never looked at interest (both 0) stays all principal.
            val takesInterest = s.interestPaisa > 0L || s.calculatedInterestPaisa > 0L
            val interest = if (takesInterest) minOf(s.amountPaisa, due) else 0L
            val principal = s.amountPaisa - interest
            if (principal > outstanding) throw LendException.OverRepayment(principal, outstanding)
            replayed += s.copy(
                interestPaisa = interest,
                principalPaisa = principal,
                calculatedInterestPaisa = if (takesInterest) due else 0L,
            )
            interestPaid += interest
            outstanding -= principal
            from = s.settledAt
        }

        val before = live.associateBy { it.id }
        val changes = ArrayList<SplitChange>()
        val changed = ArrayList<LendSettlement>()
        for (n in replayed) {
            val o = before.getValue(n.id)
            val oldPrincipal = o.effectivePrincipalPaisa()
            val same = o.settledAt == n.settledAt && o.amountPaisa == n.amountPaisa &&
                o.interestPaisa == n.interestPaisa && oldPrincipal == n.principalPaisa
            if (same) continue
            changes += SplitChange(
                settlementId = n.id,
                oldSettledAt = o.settledAt, newSettledAt = n.settledAt,
                oldAmountPaisa = o.amountPaisa, newAmountPaisa = n.amountPaisa,
                oldInterestPaisa = o.interestPaisa, newInterestPaisa = n.interestPaisa,
                oldPrincipalPaisa = oldPrincipal, newPrincipalPaisa = n.principalPaisa,
            )
            changed += n.copy(revision = o.revision + 1, updatedAt = now, syncStatus = 0L)
        }
        return RepaymentEditPlan(changed, changes, newAmountPaisa - edited.amountPaisa)
    }
}
