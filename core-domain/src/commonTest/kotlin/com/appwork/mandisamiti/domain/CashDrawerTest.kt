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

    @Test
    fun balanceIgnoresBookEntriesDeletedRowsAndNonCash() {
        val balance = CashDrawer.balancePaisa(
            listOf(
                tx(TransactionType.JAMA_RECEIVED, 10_000, at = 1),
                tx(TransactionType.UDHAR_GIVEN, 2_000, at = 2),
                tx(TransactionType.INTEREST_ADDED, 500, at = 3),   // book entry, moves no cash
                tx(TransactionType.DISCOUNT_GIVEN, 300, at = 4),   // book entry, moves no cash
                tx(TransactionType.JAMA_RECEIVED, 700, at = 5, mode = PaymentMode.UPI),
                tx(TransactionType.JAMA_RECEIVED, 900, at = 6, void = true),
                tx(TransactionType.JAMA_RECEIVED, 100, at = 7).copy(isDeleted = true)
            )
        )
        assertEquals(800_000L, balance)
    }

    @Test
    fun sameDayFullPaidBuyDoesNotDeductFromCashDrawer() {
        val window = DayWindow(startMs = 1_000, endMs = 2_000)
        val fullPaidBuy = com.appwork.mandisamiti.domain.model.BuyEntry(
            id = "buy-1", shopId = "shop-1", farmerId = "f-1", commodityId = "c-1",
            entryDate = 1_200L, bagsCount = 10, weightPerBagGrams = 50_000L, extraWeightGrams = 0L,
            totalWeightGrams = 500_000L, ratePaisaPerQuintal = 200_000L, commissionBps = 0L,
            labourPerBagPaisa = 0L, advancePaidPaisa = 0L, grossPaisa = 100_000L, commissionPaisa = 0L,
            labourPaisa = 0L, netPayablePaisa = 100_000L, paidPaisa = 100_000L, createdAt = 1_200L,
            updatedAt = 1_200L
        )
        val buyCashTx = tx(TransactionType.UDHAR_GIVEN, 1_000, at = 1_200).copy(
            sourceEntryType = "BUY",
            sourceEntryId = "buy-1"
        )
        val summary = CashDrawer.summarize(
            transactions = listOf(
                tx(TransactionType.JAMA_RECEIVED, 5_000, at = 1_100),
                buyCashTx
            ),
            window = window,
            buys = listOf(fullPaidBuy)
        )
        // Cash in is 500_000L; the 100_000L buy payout is Mohari (absorbed at day end), NOT deducted from Galla Rokad.
        assertEquals(500_000L, summary.cashInPaisa)
        assertEquals(0L, summary.cashOutPaisa)
        assertEquals(500_000L, summary.closingPaisa)

        // All-time balance also excludes it
        val balance = CashDrawer.balancePaisa(listOf(buyCashTx), listOf(fullPaidBuy))
        assertEquals(0L, balance)
    }

    @Test
    fun sameDayPartialPaidBuyDeductsFromCashDrawer() {
        val window = DayWindow(startMs = 1_000, endMs = 2_000)
        val partialBuy = com.appwork.mandisamiti.domain.model.BuyEntry(
            id = "buy-2", shopId = "shop-1", farmerId = "f-1", commodityId = "c-1",
            entryDate = 1_200L, bagsCount = 10, weightPerBagGrams = 50_000L, extraWeightGrams = 0L,
            totalWeightGrams = 500_000L, ratePaisaPerQuintal = 200_000L, commissionBps = 0L,
            labourPerBagPaisa = 0L, advancePaidPaisa = 0L, grossPaisa = 100_000L, commissionPaisa = 0L,
            labourPaisa = 0L, netPayablePaisa = 100_000L, paidPaisa = 40_000L, createdAt = 1_200L,
            updatedAt = 1_200L
        )
        val partialCashTx = tx(TransactionType.UDHAR_GIVEN, 400, at = 1_200).copy(
            sourceEntryType = "BUY",
            sourceEntryId = "buy-2"
        )
        val summary = CashDrawer.summarize(
            transactions = listOf(
                tx(TransactionType.JAMA_RECEIVED, 5_000, at = 1_100),
                partialCashTx
            ),
            window = window,
            buys = listOf(partialBuy)
        )
        // Partial payment DOES deduct from Galla Rokad
        assertEquals(500_000L, summary.cashInPaisa)
        assertEquals(40_000L, summary.cashOutPaisa)
        assertEquals(460_000L, summary.closingPaisa)
    }

    @Test
    fun previousDayBuyPaidTodayDeductsFromCashDrawer() {
        val dayMs = 86_400_000L
        val yesterdayMs = 1_000_000L
        val todayMs = yesterdayMs + dayMs
        val window = DayWindow(startMs = todayMs - 1000L, endMs = todayMs + dayMs)
        val oldBuy = com.appwork.mandisamiti.domain.model.BuyEntry(
            id = "buy-old", shopId = "shop-1", farmerId = "f-1", commodityId = "c-1",
            entryDate = yesterdayMs, // yesterday
            bagsCount = 10, weightPerBagGrams = 50_000L, extraWeightGrams = 0L,
            totalWeightGrams = 500_000L, ratePaisaPerQuintal = 200_000L, commissionBps = 0L,
            labourPerBagPaisa = 0L, advancePaidPaisa = 0L, grossPaisa = 100_000L, commissionPaisa = 0L,
            labourPaisa = 0L, netPayablePaisa = 100_000L, paidPaisa = 100_000L, createdAt = yesterdayMs,
            updatedAt = todayMs
        )
        val settleTodayTx = tx(TransactionType.UDHAR_GIVEN, 1_000, at = todayMs).copy(
            sourceEntryType = "BUY",
            sourceEntryId = "buy-old"
        )
        val summary = CashDrawer.summarize(
            transactions = listOf(
                tx(TransactionType.JAMA_RECEIVED, 5_000, at = todayMs),
                settleTodayTx
            ),
            window = window,
            buys = listOf(oldBuy)
        )
        // Settling an old slip today is a drawer debt repayment, so it DOES deduct today
        assertEquals(500_000L, summary.cashInPaisa)
        assertEquals(100_000L, summary.cashOutPaisa)
        assertEquals(400_000L, summary.closingPaisa)
    }
}
