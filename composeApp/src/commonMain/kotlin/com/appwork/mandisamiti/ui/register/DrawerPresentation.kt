package com.appwork.mandisamiti.ui.register

import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType

/** Includes edits, voids, and prior-day carry-forward changes, even when the net balance is unchanged. */
internal fun drawerCashFingerprint(transactions: List<CashTransaction>, endMs: Long): Long =
    transactions.asSequence()
        .filter { it.paymentMode == PaymentMode.CASH && it.transactionDate < endMs }
        .filter { it.transactionType in drawerMovementTypes }
        .sortedBy { it.id }
        .fold(17L) { hash, tx ->
            listOf(tx.id, tx.amountPaisa, tx.transactionDate, tx.updatedAt, tx.revision, tx.isVoid, tx.isDeleted, tx.transactionType)
                .fold(hash) { value, field -> value * 31 + field.toString().hashCode() }
        }

internal val drawerMovementTypes = setOf(TransactionType.JAMA_RECEIVED, TransactionType.INTEREST_RECEIVED, TransactionType.UDHAR_GIVEN)

internal val drawerDenominations = listOf(500, 200, 100, 50, 20, 10, 5, 2, 1)

internal fun parseDrawerRupees(value: String): Long? = value.trim().let { text ->
    if (text.isEmpty() || text.any { it !in '0'..'9' }) null
    else text.toLongOrNull()?.takeIf { it <= Long.MAX_VALUE / 100 }
}

/** Empty quantities mean zero; malformed/overflowing input must never silently become zero. */
internal fun denominationTotal(quantities: Map<Int, String>): Long? {
    var total = 0L
    for (denomination in drawerDenominations) {
        val raw = quantities[denomination].orEmpty()
        val quantity = if (raw.isBlank()) 0L else parseDrawerRupees(raw) ?: return null
        if (quantity > (Long.MAX_VALUE / 100 - total) / denomination) return null
        total += quantity * denomination
    }
    return total
}

internal fun drawerCountStatus(state: DailyRegisterUiState, isEnglish: Boolean): String = when {
    state.needsRecount -> if (isEnglish) "Cash entries changed. Count again." else "नकद प्रविष्टियां बदली हैं। फिर से गिनें।"
    state.physicalCountPaisa == null -> if (isEnglish) "Physical cash not counted" else "नकद मिलान बाकी"
    else -> dayClosingCountLabel(state.physicalCountPaisa, state.inHandCashDrawerPaisa, isEnglish)
}
