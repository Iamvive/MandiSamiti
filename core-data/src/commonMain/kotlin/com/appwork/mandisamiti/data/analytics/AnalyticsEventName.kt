package com.appwork.mandisamiti.data.analytics

enum class AnalyticsEventName(val wire: String) {
    APP_OPEN("app_open"),
    SCREEN_VIEW("screen_view"),
    LOGIN_SUCCESS("login_success"),
    BUY_SAVED("buy_saved"),
    SELL_SAVED("sell_saved"),
    PAYMENT_SAVED("payment_saved"),
    EXPENSE_SAVED("expense_saved"),
    UDHAAR_SAVED("udhaar_saved"),
    SLIP_SHARED("slip_shared"),
    SLIP_PRINTED("slip_printed"),
    ENTRY_DELETED("entry_deleted"),
    SYNC_OK("sync_ok"),
    SYNC_FAILED("sync_failed"),
}
