package com.appwork.mandisamiti.domain.model

import kotlinx.serialization.Serializable

enum class LendType { MONEY, GRAIN }
enum class LendStatus { OPEN, PARTIAL, SETTLED }
enum class LendDirection { GIVEN, TAKEN }
enum class LendKind { LOAN, PESGI, CASH }

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
    val monthlyRatePercent: Double = 0.0,
    val syncStatus: Long = 0L,
    val direction: LendDirection = LendDirection.GIVEN,
    val kind: LendKind = LendKind.LOAN,
    val receiverName: String? = null,
    val receiverPhone: String? = null,
)
