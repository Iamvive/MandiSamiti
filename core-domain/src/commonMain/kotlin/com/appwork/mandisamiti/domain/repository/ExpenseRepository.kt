package com.appwork.mandisamiti.domain.repository

import com.appwork.mandisamiti.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getExpensesStream(shopId: String): Flow<List<ExpenseEntry>>
    suspend fun getExpenseById(id: String): ExpenseEntry?
    suspend fun saveExpense(entry: ExpenseEntry)
    /**
     * Edits category, amount, payee, mode and remarks of [entry] (matched by id) and keeps its linked
     * `[EXPENSE:id]` cash row in step: updated for CASH, created if the mode became CASH, voided if it left CASH.
     * [cashRemarks] is the display text for the cash row (the tag is appended here).
     */
    suspend fun editExpense(entry: ExpenseEntry, cashRemarks: String)
    suspend fun voidExpense(id: String, reason: VoidReason)
    suspend fun stockTotals(shopId: String): StockTotals
}

data class StockTotals(val boughtGramsByCommodity: Map<String, Long>, val soldGramsByCommodity: Map<String, Long>)
