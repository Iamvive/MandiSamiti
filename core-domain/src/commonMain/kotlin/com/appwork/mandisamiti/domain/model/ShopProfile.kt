package com.appwork.mandisamiti.domain.model

import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import kotlinx.serialization.Serializable

@Serializable
data class ShopProfile(
    val id: String,
    val shopName: String,
    val ownerName: String,
    val mandiName: String,
    val shopNumber: String? = null,
    val phoneNumber: String,
    val pinHash: String,
    val defaultMonthlyInterestRate: Double = RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT,
    val isSoundEnabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0
)
