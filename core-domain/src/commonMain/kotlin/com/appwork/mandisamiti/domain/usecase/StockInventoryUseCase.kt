package com.appwork.mandisamiti.domain.usecase

import com.appwork.mandisamiti.domain.model.CommodityParser
import com.appwork.mandisamiti.domain.model.StockLine
import com.appwork.mandisamiti.domain.model.StockVarietyLine
import com.appwork.mandisamiti.domain.repository.ExpenseRepository
import com.appwork.mandisamiti.domain.repository.StockAuditRepository

class StockInventoryUseCase(
    private val repo: ExpenseRepository,
    private val auditRepo: StockAuditRepository? = null
) {
    suspend fun lines(shopId: String): List<StockLine> {
        val t = repo.stockTotals(shopId)
        val audits = auditRepo?.loadAudits(shopId) ?: emptyMap()
        val allRawKeys = (t.boughtGramsByCommodity.keys + t.soldGramsByCommodity.keys + audits.keys).toSet()

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

            // Standard packaging defaults
            val defaultBagKg = if (baseCommodity.contains("सरसों") || baseCommodity.contains("mustard", ignoreCase = true)) 70.0 else 50.0

            val audit = audits[baseCommodity] ?: rawKeysForBase.firstNotNullOfOrNull { audits[it] }

            val physicalAdjustmentGrams = audit?.diffGrams ?: 0L
            val farmerCustodyGrams = audit?.let { (it.farmerCustodyBags * (it.bagWeightKg * 1000.0)).toLong() } ?: 0L
            val bagWeightKg = audit?.bagWeightKg ?: defaultBagKg

            StockLine(
                commodityId = baseCommodity,
                boughtGrams = totalBought,
                soldGrams = totalSold,
                varieties = displayVarieties,
                physicalAdjustmentGrams = physicalAdjustmentGrams,
                farmerCustodyGrams = farmerCustodyGrams,
                standardBagKg = bagWeightKg,
                lastAuditedAt = audit?.auditedAt,
                lastAuditReason = audit?.reason
            )
        }
    }
}
