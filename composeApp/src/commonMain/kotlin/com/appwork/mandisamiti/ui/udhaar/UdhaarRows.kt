package com.appwork.mandisamiti.ui.udhaar

import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.Party

data class UdhaarPersonRow(
    val partyId: String,
    val name: String,
    val village: String?,
    val principalDuePaisa: Long,
    val interestDuePaisa: Long,
    val interestOn: Boolean,
)

fun buildUdhaarRows(
    direction: LendDirection,
    parties: List<Party>,
    loans: List<LendEntry>,
    settlements: List<LendSettlement>,
    nowMs: Long,
): List<UdhaarPersonRow> {
    val partyMap = parties.associateBy { it.id }
    val nonVoidSettlements = settlements.filter { !it.isVoid }

    val partyLoans = loans.filter {
        it.direction == direction &&
        !it.isVoid &&
        it.lendType == LendType.MONEY
    }.groupBy { it.partyId }

    val rows = mutableListOf<UdhaarPersonRow>()

    for ((partyId, partyLoanList) in partyLoans) {
        var totalPrincipalDue = 0L
        var totalInterestDue = 0L
        var interestOn = false

        for (loan in partyLoanList) {
            val loanSettlements = nonVoidSettlements.filter { it.lendId == loan.id }
            val reducedPaisa = loanSettlements.sumOf { s ->
                if (s.principalPaisa == 0L && s.interestPaisa == 0L) s.amountPaisa else s.principalPaisa
            }
            val outstandingPaisa = (loan.amountPaisa - reducedPaisa).coerceAtLeast(0L)

            if (outstandingPaisa > 0L) {
                totalPrincipalDue += outstandingPaisa
                if (loan.monthlyRatePercent > 0.0) {
                    interestOn = true
                }
                val interest = RuralInterestEngine.interestDue(
                    principalPaisa = loan.amountPaisa,
                    monthlyRatePercent = loan.monthlyRatePercent,
                    lentAtMs = loan.entryDate,
                    settlements = loanSettlements,
                    nowMs = nowMs,
                )
                totalInterestDue += interest
            }
        }

        if (totalPrincipalDue + totalInterestDue > 0L) {
            val party = partyMap[partyId]
            val name = party?.name?.ifBlank { "अज्ञात" } ?: "अज्ञात"
            val village = party?.village
            rows.add(
                UdhaarPersonRow(
                    partyId = partyId,
                    name = name,
                    village = village,
                    principalDuePaisa = totalPrincipalDue,
                    interestDuePaisa = totalInterestDue,
                    interestOn = interestOn,
                )
            )
        }
    }

    return rows.sortedWith(
        compareByDescending<UdhaarPersonRow> { it.principalDuePaisa + it.interestDuePaisa }
            .thenBy { it.name }
    )
}
