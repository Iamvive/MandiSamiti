package com.appwork.mandisamiti.domain.ledger

import com.appwork.mandisamiti.domain.model.CashTransaction

/** Which ledger source a cash row belongs to. Used for the row chip. */
sealed interface LedgerTag {
    data class Slip(val slipType: String, val slipId: String) : LedgerTag
    data object Udhaar : LedgerTag
}

/**
 * Slip if the row came from a slip, else Udhaar if it is linked to a loan, else null.
 * A row with both is a data error; the slip wins.
 */
fun ledgerTagFor(cash: CashTransaction): LedgerTag? = when {
    cash.sourceEntryId != null -> LedgerTag.Slip(cash.sourceEntryType ?: "", cash.sourceEntryId!!)
    cash.lendId != null -> LedgerTag.Udhaar
    else -> null
}
