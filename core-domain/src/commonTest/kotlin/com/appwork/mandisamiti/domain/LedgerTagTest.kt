package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.ledger.LedgerTag
import com.appwork.mandisamiti.domain.ledger.ledgerTagFor
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LedgerTagTest {
    private fun cash(sourceEntryId: String? = null, sourceEntryType: String? = null, lendId: String? = null) = CashTransaction(
        id = "tx", shopId = "shop", partyId = "p1", transactionType = TransactionType.UDHAR_GIVEN,
        amountPaisa = 100L, transactionDate = 1L, createdAt = 1L, updatedAt = 1L,
        sourceEntryType = sourceEntryType, sourceEntryId = sourceEntryId, lendId = lendId,
    )

    @Test
    fun slipRowIsTaggedWithSlipTypeAndId() {
        assertEquals(LedgerTag.Slip("BUY", "b1"), ledgerTagFor(cash(sourceEntryId = "b1", sourceEntryType = "BUY")))
    }

    @Test
    fun loanRowIsUdhaar() {
        assertEquals(LedgerTag.Udhaar, ledgerTagFor(cash(lendId = "l1")))
    }

    @Test
    fun plainRowHasNoTag() {
        assertNull(ledgerTagFor(cash()))
    }

    @Test
    fun rowWithBothIsSlipBecauseSourceWins() {
        assertEquals(
            LedgerTag.Slip("SELL", "s1"),
            ledgerTagFor(cash(sourceEntryId = "s1", sourceEntryType = "SELL", lendId = "l1")),
        )
    }
}
