package com.appwork.mandisamiti

import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals

class SellDepositFilterTest {
    private fun tx(id: String, amount: Long, lendId: String? = null, void: Boolean = false) = CashTransaction(
        id = id, shopId = "s", partyId = "p", transactionType = TransactionType.JAMA_RECEIVED,
        amountPaisa = amount, transactionDate = 1L, createdAt = 1L, updatedAt = 1L,
        isVoid = void, lendId = lendId,
    )

    @Test
    fun deposit_excludes_udhaar_repayment_rows() {
        val rows = listOf(tx("dep", 500_00), tx("repay", 300_00, lendId = "lend-1"))
        assertEquals(500_00L, sellDepositPaisa(rows))
    }

    @Test
    fun deposit_excludes_void_rows() {
        assertEquals(500_00L, sellDepositPaisa(listOf(tx("a", 500_00), tx("b", 100_00, void = true))))
    }
}
