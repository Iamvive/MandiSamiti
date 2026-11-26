package com.appwork.mandisamiti.analytics

import com.appwork.mandisamiti.Screen
import com.appwork.mandisamiti.data.analytics.Analytics
import com.appwork.mandisamiti.data.analytics.AnalyticsEventName
import com.appwork.mandisamiti.data.analytics.AnalyticsScreen
import com.appwork.mandisamiti.ui.navigation.NavigationTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun Screen.toAnalyticsScreen(homeTab: NavigationTab? = null): AnalyticsScreen = when (this) {
    is Screen.Register -> AnalyticsScreen.REGISTER
    is Screen.FirstSync -> AnalyticsScreen.FIRST_SYNC
    is Screen.Home -> when (homeTab) {
        NavigationTab.DASHBOARD -> AnalyticsScreen.HOME_DASHBOARD
        NavigationTab.PARCHI -> AnalyticsScreen.HOME_PARCHI
        NavigationTab.GALLA -> AnalyticsScreen.HOME_GALLA
        NavigationTab.GODOWN -> AnalyticsScreen.HOME_GODOWN
        NavigationTab.SETTINGS -> AnalyticsScreen.HOME_SETTINGS
        null -> AnalyticsScreen.HOME
    }
    is Screen.DealEntry -> AnalyticsScreen.DEAL_ENTRY
    is Screen.PartyLedger -> AnalyticsScreen.PARTY_LEDGER
    is Screen.ReceiptPreview -> AnalyticsScreen.RECEIPT_PREVIEW
    is Screen.DailyRegister -> AnalyticsScreen.DAILY_REGISTER
    is Screen.BuyEntry -> AnalyticsScreen.BUY_ENTRY
    is Screen.BuyPayment -> AnalyticsScreen.BUY_PAYMENT
    is Screen.BuyList -> AnalyticsScreen.BUY_LIST
    is Screen.BuyReceipt -> AnalyticsScreen.BUY_RECEIPT
    is Screen.SellEntry -> AnalyticsScreen.SELL_ENTRY
    is Screen.SellList -> AnalyticsScreen.SELL_LIST
    is Screen.SellReceipt -> AnalyticsScreen.SELL_RECEIPT
    is Screen.GrainLendEntry -> AnalyticsScreen.GRAIN_LEND_ENTRY
    is Screen.LendList -> AnalyticsScreen.LEND_LIST
    is Screen.UdhaarPerson -> AnalyticsScreen.UDHAAR_PERSON
    is Screen.ExpenseEntry -> AnalyticsScreen.EXPENSE_ENTRY
    is Screen.ExpenseList -> AnalyticsScreen.EXPENSE_LIST
    is Screen.StockSheet -> AnalyticsScreen.STOCK_SHEET
    is Screen.AllKhata -> AnalyticsScreen.ALL_KHATA
    is Screen.SubscriptionContact -> AnalyticsScreen.SUBSCRIPTION_CONTACT
}

object AppOpenTracker {
    private var recorded = false

    fun trackIfNeeded(analytics: Analytics?, scope: CoroutineScope) {
        if (analytics != null && !recorded) {
            recorded = true
            scope.launch {
                try {
                    analytics.track(AnalyticsEventName.APP_OPEN)
                } catch (_: Throwable) {
                }
            }
        }
    }

    internal fun resetForTesting() {
        recorded = false
    }
}

suspend fun <T> recordAction(
    analytics: Analytics?,
    eventName: AnalyticsEventName,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    action: suspend () -> T,
): T {
    val result = action()
    if (analytics != null) {
        scope.launch {
            try {
                analytics.track(eventName)
            } catch (_: Throwable) {
            }
        }
    }
    return result
}
