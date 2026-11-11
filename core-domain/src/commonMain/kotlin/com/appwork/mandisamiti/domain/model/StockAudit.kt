package com.appwork.mandisamiti.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class StockAudit(
    val commodityId: String,
    val physicalBags: Long,
    val bagWeightKg: Double = 50.0,
    val physicalGrams: Long,
    val diffGrams: Long,
    val reason: String,
    val auditedAt: Long,
    val farmerCustodyBags: Long = 0L
)

