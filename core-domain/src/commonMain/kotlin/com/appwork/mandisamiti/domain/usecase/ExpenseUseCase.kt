package com.appwork.mandisamiti.domain.usecase

import com.appwork.mandisamiti.domain.model.*
import com.appwork.mandisamiti.domain.repository.ExpenseRepository

data class ExpenseForm(
    val shopId: String, val category: ExpenseCategory, val amountPaisa: Long,
    val paidTo: String? = null, val paymentMode: ExpensePaymentMode = ExpensePaymentMode.CASH,
    val remarks: String? = null,
)

class ExpenseUseCase(
    private val repo: ExpenseRepository,
    private val newId: () -> String,
    private val nowMillis: () -> Long,
) {
    suspend fun add(form: ExpenseForm): ExpenseEntry {
        require(form.amountPaisa > 0) { "Amount is required" }
        val now = nowMillis()
        val e = ExpenseEntry(newId(), form.shopId, now, form.category, form.amountPaisa, form.paidTo,
            form.paymentMode, form.remarks, now, now)
        repo.saveExpense(e)
        return e
    }
}
