package com.appwork.mandisamiti.domain.cash

import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class DayWindow(val startMs: Long, val endMs: Long)

data class DrawerSummary(val openingPaisa: Long, val cashInPaisa: Long, val cashOutPaisa: Long) {
    val closingPaisa: Long get() = openingPaisa + cashInPaisa - cashOutPaisa
}

/** The physical galla: only live CASH entries move money in or out of the drawer. */
object CashDrawer {

    fun dayWindow(now: Instant, timeZone: TimeZone): DayWindow {
        val today = now.toLocalDateTime(timeZone).date
        return DayWindow(
            startMs = today.atStartOfDayIn(timeZone).toEpochMilliseconds(),
            endMs = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone).toEpochMilliseconds()
        )
    }

    fun summarize(transactions: List<CashTransaction>, window: DayWindow): DrawerSummary {
        var opening = 0L
        var cashIn = 0L
        var cashOut = 0L
        transactions
            .filter { it.paymentMode == PaymentMode.CASH && !it.isVoid && !it.isDeleted }
            .forEach { tx ->
                val signed = when (tx.transactionType) {
                    TransactionType.JAMA_RECEIVED -> tx.amountPaisa
                    TransactionType.UDHAR_GIVEN -> -tx.amountPaisa
                    TransactionType.INTEREST_ADDED, TransactionType.DISCOUNT_GIVEN -> 0L
                }
                when {
                    tx.transactionDate < window.startMs -> opening += signed
                    tx.transactionDate < window.endMs -> if (signed >= 0) cashIn += signed else cashOut -= signed
                }
            }
        return DrawerSummary(opening, cashIn, cashOut)
    }
}
