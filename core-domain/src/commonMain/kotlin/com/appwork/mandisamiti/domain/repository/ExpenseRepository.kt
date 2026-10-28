package com.appwork.mandisamiti.domain.repository

import com.appwork.mandisamiti.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getExpensesStream(shopId: String): Flow<List<ExpenseEntry>>
    suspend fun getExpenseById(id: String): ExpenseEntry?
    suspend fun saveExpense(entry: ExpenseEntry)
    suspend fun voidExpense(id: String, reason: VoidReason)
    suspend fun stockTotals(shopId: String): StockTotals
}

data class StockTotals(val boughtGramsByCommodity: Map<String, Long>, val soldGramsByCommodity: Map<String, Long>)
