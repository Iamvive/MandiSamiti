package com.appwork.mandisamiti.ui.dashboard

import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.ExpenseCategory
import com.appwork.mandisamiti.domain.model.ExpenseEntry
import com.appwork.mandisamiti.domain.model.ExpensePaymentMode
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendStatus
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import kotlin.test.Test
import kotlin.test.assertEquals

class DashboardKpisTest {

    private fun buy(gross: Long, void: Boolean = false) = BuyEntry(
        id = "b1", shopId = "s1", farmerId = "f1", commodityId = "c1",
        entryDate = 1000L, bagsCount = 10L, weightPerBagGrams = 50_000L, extraWeightGrams = 0L,
        totalWeightGrams = 500_000L, ratePaisaPerQuintal = 2000L, commissionBps = 0L,
        labourPerBagPaisa = 0L, advancePaidPaisa = 0L, grossPaisa = gross, commissionPaisa = 0L,
        labourPaisa = 0L, netPayablePaisa = gross, createdAt = 1000L, updatedAt = 1000L,
        isVoid = void
    )

    private fun sell(gross: Long, total: Long, void: Boolean = false) = SellEntry(
        id = "s1", shopId = "s1", buyerId = "b1", commodityId = "c1",
        entryDate = 1000L, billMode = BillMode.NORMAL, partyKind = SellPartyKind.CUSTOMER,
        bagsCount = 10L, weightPerBagGrams = 50_000L, totalWeightGrams = 500_000L,
        ratePaisaPerQuintal = 2000L, aratBps = 0L, tulakPaisa = 0L, grossPaisa = gross,
        aratPaisa = 0L, mandiShulkPaisa = 0L, totalReceivablePaisa = total, receivedPaisa = total,
        balanceDuePaisa = 0L, createdAt = 1000L, updatedAt = 1000L, isVoid = void
    )

    private fun expense(amount: Long, void: Boolean = false) = ExpenseEntry(
        id = "e1", shopId = "s1", entryDate = 1000L, category = ExpenseCategory.HAMMALI,
        amountPaisa = amount, paymentMode = ExpensePaymentMode.CASH, createdAt = 1000L,
        updatedAt = 1000L, isVoid = void
    )

    @Test
    fun voidRowsAreExcluded_andProfitUsesGrossOnly() {
        val k = computeKpis(
            buys = listOf(buy(gross = 1_000_000), buy(gross = 500_000, void = true)),
            sells = listOf(sell(gross = 1_300_000, total = 1_340_000)),
            lends = emptyList(),
            expenses = listOf(expense(50_000), expense(70_000, void = true)),
        )
        assertEquals(1_000_000, k.totalBuyPaisa)
        assertEquals(1_300_000, k.totalSellPaisa)
        assertEquals(50_000, k.totalExpensePaisa)
        assertEquals(250_000, k.profitLossPaisa)
    }

    @Test
    fun totalOutstandingKpi_subtractsNonVoidSettlements_andIgnoresVoidLends() {
        val lend1 = LendEntry(
            id = "l1", shopId = "s1", partyId = "p1", lendType = LendType.MONEY,
            entryDate = 1000L, amountPaisa = 1_000_000L, createdAt = 1000L, updatedAt = 1000L
        )
        val lend2Void = LendEntry(
            id = "l2", shopId = "s1", partyId = "p2", lendType = LendType.MONEY,
            entryDate = 1000L, amountPaisa = 500_000L, createdAt = 1000L, updatedAt = 1000L,
            isVoid = true
        )
        val s1 = com.appwork.mandisamiti.domain.model.LendSettlement(
            id = "s1", shopId = "s1", lendId = "l1", settledAt = 1000L,
            amountPaisa = 400_000L, createdAt = 1000L, updatedAt = 1000L
        )
        val s2Void = com.appwork.mandisamiti.domain.model.LendSettlement(
            id = "s2", shopId = "s1", lendId = "l1", settledAt = 1000L,
            amountPaisa = 200_000L, createdAt = 1000L, updatedAt = 1000L,
            isVoid = true
        )

        val k = computeKpis(
            buys = emptyList(),
            sells = emptyList(),
            lends = listOf(lend1, lend2Void),
            expenses = emptyList(),
            settlements = listOf(s1, s2Void)
        )
        // 1,000,000 (l1) - 400,000 (s1) = 600,000 paisa
        assertEquals(600_000L, k.totalLendPaisa)
    }
}
