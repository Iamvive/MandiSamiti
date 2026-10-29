package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.model.StockLine
import com.appwork.mandisamiti.domain.repository.ExpenseRepository
import com.appwork.mandisamiti.domain.repository.StockTotals
import com.appwork.mandisamiti.domain.usecase.StockInventoryUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DailyStockCarryForwardAndNegativeWarningTest {

    private class FakeStockRepo(val totals: StockTotals) : ExpenseRepository {
        override fun getExpensesStream(shopId: String) = throw NotImplementedError()
        override suspend fun getExpenseById(id: String) = null
        override suspend fun saveExpense(entry: com.appwork.mandisamiti.domain.model.ExpenseEntry) = Unit
        override suspend fun voidExpense(id: String, reason: com.appwork.mandisamiti.domain.model.VoidReason) = Unit
        override suspend fun stockTotals(shopId: String) = totals
    }

    @Test
    fun stockCarryForward_soldDrawsFromStock_overTwoDays() = runTest {
        // Day 1: Bought 100 quintals (10,000,000 grams) of Wheat, sold 20 quintals (2,000,000 grams).
        // Closing stock of Day 1 is 80 quintals (8,000,000 grams).
        // Day 2: Sold 30 quintals (3,000,000 grams) without any new purchase.
        // Total cumulative bought = 100 qtl, total cumulative sold = 50 qtl.
        // Closing stock on Day 2 = 50 quintals (5,000,000 grams).

        val day2Totals = StockTotals(
            boughtGramsByCommodity = mapOf("गेहूं" to 10_000_000L),
            soldGramsByCommodity = mapOf("गेहूं" to 5_000_000L)
        )

        val useCase = StockInventoryUseCase(FakeStockRepo(day2Totals))
        val lines = useCase.lines("shop-test")

        val wheat = lines.first { it.commodityId == "गेहूं" }
        assertEquals(10_000_000L, wheat.boughtGrams)
        assertEquals(5_000_000L, wheat.soldGrams)
        assertEquals(5_000_000L, wheat.onHandGrams)
    }

    @Test
    fun negativeStock_whenSoldMoreThanBought_isFlaggedAsSuspicious() = runTest {
        // Bought 10 quintals (1,000,000g), sold 15 quintals (1,500,000g).
        val oversoldTotals = StockTotals(
            boughtGramsByCommodity = mapOf("चना" to 1_000_000L),
            soldGramsByCommodity = mapOf("चना" to 1_500_000L)
        )

        val useCase = StockInventoryUseCase(FakeStockRepo(oversoldTotals))
        val lines = useCase.lines("shop-test")

        val chana = lines.first { it.commodityId == "चना" }
        assertEquals(-500_000L, chana.onHandGrams)
        assertTrue(chana.onHandGrams < 0, "Stock line must show negative onHandGrams to flag suspicious data")
    }
}
