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
    val varieties: List<StockVarietyLine> = emptyList()
) {
    val onHandGrams: Long get() = boughtGrams - soldGrams
}
