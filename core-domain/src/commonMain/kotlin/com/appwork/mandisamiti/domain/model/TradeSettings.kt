package com.appwork.mandisamiti.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TradeSettings(
    val farmerCommissionBps: Long = 70L,    // 0.7% default (shop sheet) (basis points)
    val buyerCommissionBps: Long = 150L,    // MERCHANT buyer rate, 1.5% default (basis points)
    val customerCommissionBps: Long = 150L, // CUSTOMER buyer rate, 1.5% default (basis points)
    val defaultLabourPaisa: Long = 1000L,   // ₹10 per bag default (shop sheet)
    val traderLabourPaisaPerQuintal: Long = 0L, // trader Sell labour per quintal; 0 = not set (no default)
    val isEnglish: Boolean = false
)
