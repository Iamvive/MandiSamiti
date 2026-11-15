package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.repository.*
import com.appwork.mandisamiti.domain.usecase.StockInventoryUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StockInventoryUseCaseTest {
    private class FakeStock(val totals: StockTotals) : ExpenseRepository {
        override fun getExpensesStream(shopId: String) = throw NotImplementedError()
        override suspend fun getExpenseById(id: String) = null
        override suspend fun saveExpense(entry: com.appwork.mandisamiti.domain.model.ExpenseEntry) = Unit
        override suspend fun editExpense(entry: com.appwork.mandisamiti.domain.model.ExpenseEntry, cashRemarks: String) = Unit
        override suspend fun voidExpense(id: String, reason: com.appwork.mandisamiti.domain.model.VoidReason) = Unit
        override suspend fun stockTotals(shopId: String) = totals
    }

    @Test
    fun onHandIsBoughtMinusSold_andIncludesCommoditiesOnlyBought() = runTest {
        val repo = FakeStock(StockTotals(mapOf("wheat" to 1_000_000L, "mustard" to 200_000L), mapOf("wheat" to 400_000L)))
        val lines = StockInventoryUseCase(repo).lines("shop").associateBy { it.commodityId }
        assertEquals(600_000L, lines.getValue("wheat").onHandGrams)
        assertEquals(200_000L, lines.getValue("mustard").onHandGrams)
    }

    @Test
    fun oversoldCommodityShowsNegative_notHidden() = runTest {
        val repo = FakeStock(StockTotals(mapOf("paddy" to 100_000L), mapOf("paddy" to 300_000L)))
        assertEquals(-200_000L, StockInventoryUseCase(repo).lines("shop").single().onHandGrams)
    }

    @Test
    fun stockSplitByVariety_aggregatesUnderCommodityWithSubRows() = runTest {
        val bought = mapOf("गेहूं" to 7_200_000L)
        val sold = mapOf("गेहूं लोकवान" to 500_000L, "गेहूं शरबती" to 1_000_000L)
        val repo = FakeStock(StockTotals(bought, sold))
        val lines = StockInventoryUseCase(repo).lines("shop")

        assertEquals(1, lines.size)
        val gehu = lines.single()
        assertEquals("गेहूं", gehu.commodityId)
        assertEquals(7_200_000L, gehu.boughtGrams)
        assertEquals(1_500_000L, gehu.soldGrams)
        assertEquals(5_700_000L, gehu.onHandGrams)

        assertEquals(3, gehu.varieties.size)
        val byVariety = gehu.varieties.associateBy { it.varietyName }
        assertEquals(7_200_000L, byVariety.getValue("सामान्य").onHandGrams)
        assertEquals(-500_000L, byVariety.getValue("लोकवान").onHandGrams)
        assertEquals(-1_000_000L, byVariety.getValue("शरबती").onHandGrams)
    }

    @Test fun khaateLineIsSeparateFromNormalBalance() = runTest {
        val repo = FakeStock(StockTotals(
            boughtGramsByCommodity = mapOf("गेहूं" to 5_000_000L),
            soldGramsByCommodity = emptyMap(),
            khaateBoughtGramsByCommodity = mapOf("गेहूं" to 250_000L),
            khaateSoldGramsByCommodity = emptyMap()
        ))
        val line = StockInventoryUseCase(repo).lines("s").single()
        assertEquals(100L, line.onHandBags)
        assertEquals(5L, line.khaateOnHandBags)
    }

    @Test fun khaateLineMayGoNegative() = runTest {
        val repo = FakeStock(StockTotals(emptyMap(), emptyMap(),
            mapOf("गेहूं" to 250_000L), mapOf("गेहूं" to 400_000L)))
        val line = StockInventoryUseCase(repo).lines("s").single()
        assertEquals(-150_000L, line.khaateOnHandGrams)
        assertTrue(line.isKhaateNegative)
    }

    @Test fun khaateOnlyCropStillGetsALine() = runTest {
        val repo = FakeStock(StockTotals(emptyMap(), emptyMap(), mapOf("जिंस" to 100_000L), emptyMap()))
        assertEquals(listOf("जिंस"), StockInventoryUseCase(repo).lines("s").map { it.commodityId })
    }
}
