package com.appwork.mandisamiti.domain.model

import kotlinx.serialization.Serializable

enum class ExpenseCategory { HAMMALI, KIRAYA, BIJLI, CHAI_PANI, MAJDOORI, OTHER, DAY_CLOSE_DIFF }
enum class ExpensePaymentMode { CASH, RTGS, NEFT, CHEQUE, UPI, ONLINE }

@Serializable
data class ExpenseEntry(
    val id: String,
    val shopId: String,
    val entryDate: Long,
    val category: ExpenseCategory,
    val amountPaisa: Long,
    val paidTo: String? = null,
    val paymentMode: ExpensePaymentMode = ExpensePaymentMode.CASH,
    val remarks: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val isVoid: Boolean = false,
    val voidReason: String? = null,
    val syncStatus: Long = 0L,
)
