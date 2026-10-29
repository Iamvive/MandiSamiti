package com.appwork.mandisamiti.domain.usecase

import com.appwork.mandisamiti.domain.model.CommodityParser
import com.appwork.mandisamiti.domain.model.StockLine
import com.appwork.mandisamiti.domain.model.StockVarietyLine
import com.appwork.mandisamiti.domain.repository.ExpenseRepository

class StockInventoryUseCase(private val repo: ExpenseRepository) {
    suspend fun lines(shopId: String): List<StockLine> {
        val t = repo.stockTotals(shopId)
        val allRawKeys = (t.boughtGramsByCommodity.keys + t.soldGramsByCommodity.keys).toSet()

        val grouped = allRawKeys.groupBy { raw -> CommodityParser.parseBaseCommodity(raw) }

        return grouped.keys.sorted().map { baseCommodity ->
            val rawKeysForBase = grouped[baseCommodity] ?: emptyList()
            var totalBought = 0L
            var totalSold = 0L
            val varieties = mutableListOf<StockVarietyLine>()

            for (raw in rawKeysForBase.sorted()) {
                val b = t.boughtGramsByCommodity[raw] ?: 0L
                val s = t.soldGramsByCommodity[raw] ?: 0L
                totalBought += b
                totalSold += s

                val varietyName = CommodityParser.parseVarietyName(raw, baseCommodity)
                varieties.add(StockVarietyLine(varietyName, b, s))
            }

            val displayVarieties = if (varieties.size > 1 || (varieties.size == 1 && varieties.first().varietyName != "सामान्य")) {
                varieties
            } else {
                emptyList()
            }

            StockLine(
                commodityId = baseCommodity,
                boughtGrams = totalBought,
                soldGrams = totalSold,
                varieties = displayVarieties
            )
        }
    }
}
