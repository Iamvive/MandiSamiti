package com.appwork.mandisamiti.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LendSettlement(
    val id: String,
    val shopId: String,
    val lendId: String,
    val settledAt: Long,
    val amountPaisa: Long = 0L,
    val weightGrams: Long = 0L,
    val bagsCount: Long = 0L,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val isVoid: Boolean = false,
    val voidReason: String? = null,
    val syncStatus: Long = 0L,
)
