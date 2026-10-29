package com.appwork.mandisamiti.ui.dashboard

import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.ExpenseEntry
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.SellEntry

data class DashboardKpis(
    val totalBuyPaisa: Long,
    val totalSellPaisa: Long,
    val totalLendPaisa: Long,
    val totalExpensePaisa: Long,
    val profitLossPaisa: Long
)

fun computeKpis(
    buys: List<BuyEntry>,
    sells: List<SellEntry>,
    lends: List<LendEntry>,
    expenses: List<ExpenseEntry>,
    settlements: List<LendSettlement> = emptyList(),
): DashboardKpis {
    val nonVoidBuys = buys.filter { !it.isVoid }
    val nonVoidSells = sells.filter { !it.isVoid }
    val nonVoidLends = lends.filter { !it.isVoid && it.lendType == LendType.MONEY }
    val nonVoidExpenses = expenses.filter { !it.isVoid }

    val nonVoidLendIds = nonVoidLends.map { it.id }.toSet()
    val nonVoidSettlements = settlements.filter { !it.isVoid && it.lendId in nonVoidLendIds }
    val settledPaisa = nonVoidSettlements.sumOf { it.amountPaisa }

    val totalBuy = nonVoidBuys.sumOf { it.grossPaisa }
    val totalSell = nonVoidSells.sumOf { it.grossPaisa }
    val totalLend = (nonVoidLends.sumOf { it.amountPaisa } - settledPaisa).coerceAtLeast(0L)
    val totalExpense = nonVoidExpenses.sumOf { it.amountPaisa }

    val profitLoss = totalSell - totalBuy - totalExpense

    return DashboardKpis(
        totalBuyPaisa = totalBuy,
        totalSellPaisa = totalSell,
        totalLendPaisa = totalLend,
        totalExpensePaisa = totalExpense,
        profitLossPaisa = profitLoss
    )
}
