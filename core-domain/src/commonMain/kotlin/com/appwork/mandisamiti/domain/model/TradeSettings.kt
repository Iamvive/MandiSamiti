package com.appwork.mandisamiti.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TradeSettings(
    val farmerCommissionBps: Long = 70L,    // 0.7% default (shop sheet) (basis points)
    val buyerCommissionBps: Long = 150L,    // 1.5% default (basis points)
    val defaultLabourPaisa: Long = 1000L,   // ₹10 per bag default (shop sheet)
    val defaultTareGrams: Long = 35000L,    // 0.35 quintals (35 kg) default
    val isEnglish: Boolean = false
)
