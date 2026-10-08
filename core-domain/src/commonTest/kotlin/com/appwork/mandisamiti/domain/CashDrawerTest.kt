package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.cash.CashDrawer
import com.appwork.mandisamiti.domain.cash.DayWindow
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class CashDrawerTest {
    private val ist = TimeZone.of("Asia/Kolkata")

    private fun tx(type: TransactionType, rupees: Long, at: Long, mode: PaymentMode = PaymentMode.CASH, void: Boolean = false) =
        CashTransaction(
            id = "tx-$at-$rupees", shopId = "shop-1", partyId = "p", transactionType = type,
            amountPaisa = rupees * 100, paymentMode = mode, transactionDate = at,
            createdAt = at, updatedAt = at, isVoid = void
        )

    @Test
    fun dayWindowFollowsIndianMidnightNotUtc() {
        // 00:10 IST on 8 Oct = 18:40 UTC on 7 Oct
        val window = CashDrawer.dayWindow(Instant.parse("2026-10-07T18:40:00Z"), ist)
        assertEquals(Instant.parse("2026-10-07T18:30:00Z").toEpochMilliseconds(), window.startMs)
        assertEquals(Instant.parse("2026-10-08T18:30:00Z").toEpochMilliseconds(), window.endMs)
    }

    @Test
    fun summaryCountsOnlyTodaysLiveCashAndCarriesYesterdayForward() {
        val window = DayWindow(startMs = 1_000, endMs = 2_000)
        val summary = CashDrawer.summarize(
            listOf(
                tx(TransactionType.JAMA_RECEIVED, 10_000, at = 500),                      // yesterday -> opening
                tx(TransactionType.UDHAR_GIVEN, 2_000, at = 600),                         // yesterday -> opening
                tx(TransactionType.JAMA_RECEIVED, 20_000, at = 1_100),                    // today in
                tx(TransactionType.UDHAR_GIVEN, 5_000, at = 1_200),                       // today out
                tx(TransactionType.JAMA_RECEIVED, 3_000, at = 1_300, mode = PaymentMode.UPI), // not drawer cash
                tx(TransactionType.JAMA_RECEIVED, 1_000, at = 1_400, void = true),        // voided
                tx(TransactionType.INTEREST_ADDED, 500, at = 1_500),                      // book entry, no cash moves
                tx(TransactionType.JAMA_RECEIVED, 9_999, at = 2_100)                      // tomorrow
            ),
            window
        )
        assertEquals(800_000L, summary.openingPaisa)
        assertEquals(2_000_000L, summary.cashInPaisa)
        assertEquals(500_000L, summary.cashOutPaisa)
        assertEquals(2_300_000L, summary.closingPaisa)
    }
}
