package com.appwork.mandisamiti.domain.model

data class StockVarietyLine(
    val varietyName: String,
    val boughtGrams: Long,
    val soldGrams: Long
) {
    val onHandGrams: Long get() = boughtGrams - soldGrams
}

data class StockLine(
    val commodityId: String,
    val boughtGrams: Long,
    val soldGrams: Long,
    val varieties: List<StockVarietyLine> = emptyList(),
    val physicalAdjustmentGrams: Long = 0L,
    val farmerCustodyGrams: Long = 0L,
    val standardBagKg: Double = 50.0,
    val lastAuditedAt: Long? = null,
    val lastAuditReason: String? = null
) {
    val bookOnHandGrams: Long get() = boughtGrams - soldGrams
    val onHandGrams: Long get() = bookOnHandGrams + physicalAdjustmentGrams
    val onHandBags: Long get() = if (standardBagKg > 0) (onHandGrams / (standardBagKg * 1000.0)).toLong() else 0L
    val ownStockGrams: Long get() = (onHandGrams - farmerCustodyGrams).coerceAtLeast(0L)
    val ownStockBags: Long get() = if (standardBagKg > 0) (ownStockGrams / (standardBagKg * 1000.0)).toLong() else 0L
    val farmerCustodyBags: Long get() = if (standardBagKg > 0) (farmerCustodyGrams / (standardBagKg * 1000.0)).toLong() else 0L
}
