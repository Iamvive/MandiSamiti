package com.appwork.mandisamiti.domain.cash

import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
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
        return dayWindow(today, timeZone)
    }

    fun dayWindow(date: LocalDate, timeZone: TimeZone): DayWindow {
        return DayWindow(
            startMs = date.atStartOfDayIn(timeZone).toEpochMilliseconds(),
            endMs = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone).toEpochMilliseconds()
        )
    }

    fun isSameDay(ms1: Long, ms2: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): Boolean {
        val date1 = Instant.fromEpochMilliseconds(ms1).toLocalDateTime(timeZone).date
        val date2 = Instant.fromEpochMilliseconds(ms2).toLocalDateTime(timeZone).date
        return date1 == date2
    }

    /**
     * In Mandi accounting, a buy slip created on the current date that is fully paid
     * is accounted for in Day-End Mohari (procurement settlement) and does not deduct
     * physical currency from the daily cash drawer (Galla Rokad).
     */
    fun isMohariBuyTransaction(
        tx: CashTransaction,
        buysById: Map<String, BuyEntry>,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Boolean {
        if (tx.sourceEntryType != "BUY" || tx.sourceEntryId == null) return false
        val buy = buysById[tx.sourceEntryId] ?: return false
        if (buy.isVoid || buy.isKhaate || buy.netPayablePaisa <= 0L) return false
        if (buy.paidPaisa < buy.netPayablePaisa) return false
        return isSameDay(buy.entryDate, tx.transactionDate, timeZone)
    }

    /** All-time drawer balance by the book: same rules as [summarize], no day window. */
    fun balancePaisa(
        transactions: List<CashTransaction>,
        buys: List<BuyEntry> = emptyList(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Long =
        summarize(transactions, DayWindow(startMs = Long.MAX_VALUE, endMs = Long.MAX_VALUE), buys, timeZone).openingPaisa

    fun summarize(
        transactions: List<CashTransaction>,
        window: DayWindow,
        buys: List<BuyEntry> = emptyList(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): DrawerSummary {
        val buysById = if (buys.isEmpty()) emptyMap() else buys.associateBy { it.id }
        var opening = 0L
        var cashIn = 0L
        var cashOut = 0L
        transactions
            .filter { it.paymentMode == PaymentMode.CASH && !it.isVoid && !it.isDeleted }
            .filterNot { buysById.isNotEmpty() && isMohariBuyTransaction(it, buysById, timeZone) }
            .forEach { tx ->
                val signed = when (tx.transactionType) {
                    TransactionType.JAMA_RECEIVED, TransactionType.INTEREST_RECEIVED -> tx.amountPaisa
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
