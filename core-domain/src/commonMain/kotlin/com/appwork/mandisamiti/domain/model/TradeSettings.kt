package com.appwork.mandisamiti.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TradeSettings(
    val farmerCommissionBps: Long = 150L,   // 1.5% default (basis points)
    val buyerCommissionBps: Long = 150L,    // 1.5% default (basis points)
    val defaultLabourPaisa: Long = 15000L,  // ₹150 default
    val defaultTareGrams: Long = 35000L,    // 0.35 quintals (35 kg) default
    val isEnglish: Boolean = false
)
