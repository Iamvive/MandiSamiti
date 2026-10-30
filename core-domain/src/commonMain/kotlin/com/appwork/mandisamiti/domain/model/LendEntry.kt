package com.appwork.mandisamiti.domain.model

import kotlinx.serialization.Serializable

enum class LendType { MONEY, GRAIN }
enum class LendStatus { OPEN, PARTIAL, SETTLED }

@Serializable
data class LendEntry(
    val id: String,
    val shopId: String,
    val partyId: String,
    val lendType: LendType,
    val entryDate: Long,
    val amountPaisa: Long = 0L,
    val commodityId: String? = null,
    val bagsCount: Long = 0L,
    val weightGrams: Long = 0L,
    val expectedReturnDate: Long? = null,
    val slipNo: Long? = null,
    val notes: String? = null,
    val status: LendStatus = LendStatus.OPEN,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val isVoid: Boolean = false,
    val voidReason: String? = null,
    val syncStatus: Long = 0L,
)
