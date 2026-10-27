package com.appwork.mandisamiti.domain.usecase

import com.appwork.mandisamiti.domain.model.StockLine
import com.appwork.mandisamiti.domain.repository.ExpenseRepository

class StockInventoryUseCase(private val repo: ExpenseRepository) {
    suspend fun lines(shopId: String): List<StockLine> {
        val t = repo.stockTotals(shopId)
        return (t.boughtGramsByCommodity.keys + t.soldGramsByCommodity.keys).sorted().map {
            StockLine(it, t.boughtGramsByCommodity[it] ?: 0L, t.soldGramsByCommodity[it] ?: 0L)
        }
    }
}
