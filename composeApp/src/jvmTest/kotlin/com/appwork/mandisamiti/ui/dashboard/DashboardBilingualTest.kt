package com.appwork.mandisamiti.ui.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashboardBilingualTest {

    private val devanagariRegex = Regex("[\\u0900-\\u097F]")

    @Test
    fun englishDashboardStringsContainZeroDevanagari() {
        val englishStrings = listOf(
            DashboardStrings.kpiPurchases(isEnglish = true),
            DashboardStrings.kpiSales(isEnglish = true),
            DashboardStrings.kpiOutstanding(isEnglish = true),
            DashboardStrings.kpiExpenses(isEnglish = true),
            DashboardStrings.profitLossNetLoss(isEnglish = true),
            DashboardStrings.profitLossNetProfit(isEnglish = true),
            DashboardStrings.godownTitle(isEnglish = true),
            DashboardStrings.godownViewAll(isEnglish = true),
            DashboardStrings.godownEmpty(isEnglish = true),
            DashboardStrings.godownOversold(isEnglish = true),
            DashboardStrings.quickActionBuy(isEnglish = true),
            DashboardStrings.quickActionBuySub(isEnglish = true),
            DashboardStrings.quickActionSell(isEnglish = true),
            DashboardStrings.quickActionSellSub(isEnglish = true),
            DashboardStrings.quickActionLend(isEnglish = true),
            DashboardStrings.quickActionLendSub(isEnglish = true),
            DashboardStrings.quickActionExpense(isEnglish = true),
            DashboardStrings.quickActionExpenseSub(isEnglish = true),
            DashboardStrings.soundVoiceOn(isEnglish = true),
            DashboardStrings.soundMute(isEnglish = true),
            DashboardStrings.metricReceivableTitle(isEnglish = true),
            DashboardStrings.metricPayableTitle(isEnglish = true),
            DashboardStrings.activeAccountsHeader(isEnglish = true),
            DashboardStrings.viewAll(isEnglish = true),
            DashboardStrings.noAccountsEmpty(isEnglish = true)
        )

        for (str in englishStrings) {
            assertFalse(
                devanagariRegex.containsMatchIn(str),
                "Expected 0 Devanagari characters in English string '$str'"
            )
        }
    }

    @Test
    fun hindiDashboardStringsContainDevanagari() {
        val hindiStrings = listOf(
            DashboardStrings.kpiPurchases(isEnglish = false),
            DashboardStrings.kpiSales(isEnglish = false),
            DashboardStrings.kpiOutstanding(isEnglish = false),
            DashboardStrings.kpiExpenses(isEnglish = false),
            DashboardStrings.profitLossNetLoss(isEnglish = false),
            DashboardStrings.profitLossNetProfit(isEnglish = false),
            DashboardStrings.godownTitle(isEnglish = false),
            DashboardStrings.godownViewAll(isEnglish = false),
            DashboardStrings.godownEmpty(isEnglish = false),
            DashboardStrings.godownOversold(isEnglish = false),
            DashboardStrings.quickActionBuy(isEnglish = false),
            DashboardStrings.quickActionBuySub(isEnglish = false),
            DashboardStrings.quickActionSell(isEnglish = false),
            DashboardStrings.quickActionSellSub(isEnglish = false),
            DashboardStrings.quickActionLend(isEnglish = false),
            DashboardStrings.quickActionLendSub(isEnglish = false),
            DashboardStrings.quickActionExpense(isEnglish = false),
            DashboardStrings.quickActionExpenseSub(isEnglish = false),
            DashboardStrings.soundVoiceOn(isEnglish = false),
            DashboardStrings.soundMute(isEnglish = false),
            DashboardStrings.metricReceivableTitle(isEnglish = false),
            DashboardStrings.metricPayableTitle(isEnglish = false),
            DashboardStrings.activeAccountsHeader(isEnglish = false),
            DashboardStrings.viewAll(isEnglish = false),
            DashboardStrings.noAccountsEmpty(isEnglish = false)
        )

        for (str in hindiStrings) {
            assertTrue(
                devanagariRegex.containsMatchIn(str),
                "Expected Devanagari characters in Hindi string '$str'"
            )
        }
    }

    @Test
    fun conversationalEnglishSubLabels() {
        assertEquals("From Farmer", DashboardStrings.quickActionBuySub(isEnglish = true))
        assertEquals("To Buyer", DashboardStrings.quickActionSellSub(isEnglish = true))
    }
}
