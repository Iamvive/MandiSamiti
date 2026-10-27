package com.appwork.mandisamiti.domain.model

data class StockLine(val commodityId: String, val boughtGrams: Long, val soldGrams: Long) {
    val onHandGrams: Long get() = boughtGrams - soldGrams
}
