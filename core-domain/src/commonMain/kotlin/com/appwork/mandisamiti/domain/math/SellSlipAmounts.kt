package com.appwork.mandisamiti.domain.math

import com.appwork.mandisamiti.domain.model.SellEntry

/**
 * Amount rows printed on a Sell slip (screen, WhatsApp text and print HTML all use this).
 * Labour is not stored on the entry, so it is the remainder after the stored charges.
 */
data class SellSlipAmounts(
    val commissionPaisa: Long,
    val mandiShulkPaisa: Long,
    val tulakPaisa: Long,
    val labourPaisa: Long,
)

fun sellSlipAmounts(entry: SellEntry): SellSlipAmounts {
    val labour = entry.totalReceivablePaisa - entry.grossPaisa - entry.aratPaisa - entry.mandiShulkPaisa - entry.tulakPaisa
    return SellSlipAmounts(
        commissionPaisa = entry.aratPaisa,
        mandiShulkPaisa = entry.mandiShulkPaisa,
        tulakPaisa = entry.tulakPaisa,
        labourPaisa = labour.coerceAtLeast(0L),
    )
}
