package com.appwork.mandisamiti.domain.model

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
    val defaultMonthlyInterestRate: Double = 1.5,
    val isSoundEnabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0
)
