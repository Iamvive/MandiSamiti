package com.appwork.mandisamiti.ui.buy

import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.receiptDateShort
import com.appwork.mandisamiti.ui.slip.ReceiptHtmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuyReceiptPaymentHistoryTest {

    private val sampleShop = ShopProfile(
        id = "shop_1",
        shopName = "Omveer Singh & Brothers",
        ownerName = "Omveer Singh",
        mandiName = "Hathras",
        phoneNumber = "9876543210",
        pinHash = "1234",
        createdAt = 1731670000000L,
        updatedAt = 1731670000000L
    )

    private val day1Millis = 1731670000000L // 15 Nov 2026
    private val day2Millis = 1731756400000L // 16 Nov 2026

    @Test
    fun multiDayPaymentHistoryRendersItemizedTimelineWithDates() {
        val entry = BuyEntry(
            id = "buy_monty_1",
            shopId = "shop_1",
            farmerId = "farmer_monty",
            commodityId = "बाजरा",
            entryDate = day1Millis,
            bagsCount = 25,
            weightPerBagGrams = 100_000,
            extraWeightGrams = 0,
            totalWeightGrams = 2_500_000, // 25 Qtl
            ratePaisaPerQuintal = 200_000, // ₹2,000/Qtl
            commissionBps = 150,
            labourPerBagPaisa = 1_000,
            advancePaidPaisa = 0L,
            grossPaisa = 5_000_000L, // ₹50,000
            commissionPaisa = 75_000L, // ₹750
            labourPaisa = 25_000L, // ₹250
            netPayablePaisa = 4_900_000L, // ₹49,000
            paidPaisa = 3_700_000L, // ₹25,000 + ₹12,000 = ₹37,000
            slipNo = 1L,
            remarks = "गाँव: Hathras | DATA: []",
            createdAt = day1Millis,
            updatedAt = day2Millis,
            revision = 2L
        )

        val payments = listOf(
            CashTransaction(
                id = "tx-1",
                shopId = "shop_1",
                partyId = "farmer_monty",
                transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 2_500_000L, // ₹25,000
                paymentMode = PaymentMode.CASH,
                transactionDate = day1Millis,
                remarks = "खरीद भुगतान: पर्ची #1",
                createdAt = day1Millis,
                updatedAt = day1Millis,
                sourceEntryType = "BUY",
                sourceEntryId = entry.id
            ),
            CashTransaction(
                id = "tx-2",
                shopId = "shop_1",
                partyId = "farmer_monty",
                transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 1_200_000L, // ₹12,000
                paymentMode = PaymentMode.CASH,
                transactionDate = day2Millis,
                remarks = "खरीद पर्ची #1 भुगतान",
                createdAt = day2Millis,
                updatedAt = day2Millis,
                sourceEntryType = "BUY",
                sourceEntryId = entry.id
            )
        )

        // 1. Verify WhatsApp share text
        val slipText = buildFarmerSlipText(
            entry = entry,
            farmerName = "Monty",
            payments = payments
        )

        // Privacy rule: कुल खर्चा combined, NO separate labour/commission split
        assertTrue(slipText.contains("कुल खर्चा: -₹1,000"), "Must contain combined कुल खर्चा (-₹1,000)")
        assertFalse(slipText.contains("मजदूरी कटौती"), "Must NOT split labour in share text")
        assertFalse(slipText.contains("कमीशन कटौती"), "Must NOT split commission in share text")

        // Sequence: Gross -> कुल खर्चा -> पर्ची राशि
        assertTrue(slipText.contains("कुल राशि: ₹50,000"), "Must contain gross")
        assertTrue(slipText.contains("पर्ची राशि: ₹49,000"), "Must contain slip value")

        // Payment installment timeline with date stamps
        val day1Date = receiptDateShort(day1Millis)
        val day2Date = receiptDateShort(day2Millis)
        assertTrue(slipText.contains("• नकद भुगतान · $day1Date: -₹25,000"), "Must contain Day 1 payment with date")
        assertTrue(slipText.contains("• नकद भुगतान · $day2Date: -₹12,000"), "Must contain Day 2 payment with date")
        assertTrue(slipText.contains("बाकी राशि: ₹12,000"), "Must show remaining balance ₹12,000")

        // 2. Verify A5 Print HTML
        val html = ReceiptHtmlRenderer.renderBuyReceiptHtml(
            entry = entry,
            shop = sampleShop,
            farmerName = "Monty",
            payments = payments
        )

        assertTrue(html.contains("<td>कुल खर्चा:</td>"), "A5 print must contain single 'कुल खर्चा:' row")
        assertFalse(html.contains("मजदूरी + आढ़त"), "A5 print must NOT mention 'मजदूरी + आढ़त'")
        assertTrue(html.contains("<td>पर्ची राशि:</td>"), "A5 print must contain 'पर्ची राशि:' row")
        assertTrue(html.contains("• नकद भुगतान · $day1Date:"), "A5 print must contain Day 1 payment with date")
        assertTrue(html.contains("- ₹25,000"), "A5 print must show ₹25,000 deduction")
        assertTrue(html.contains("• नकद भुगतान · $day2Date:"), "A5 print must contain Day 2 payment with date")
        assertTrue(html.contains("- ₹12,000"), "A5 print must show ₹12,000 deduction")
        assertTrue(html.contains("₹12,000"), "A5 print must show balance due")
    }

    @Test
    fun fullyPaidSlipShowsChuktaInShareTextAndHtml() {
        val entry = BuyEntry(
            id = "buy_paid",
            shopId = "shop_1",
            farmerId = "farmer_monty",
            commodityId = "गेहूं",
            entryDate = day1Millis,
            bagsCount = 10,
            weightPerBagGrams = 50_000,
            extraWeightGrams = 0,
            totalWeightGrams = 500_000,
            ratePaisaPerQuintal = 200_000,
            commissionBps = 0,
            labourPerBagPaisa = 0,
            advancePaidPaisa = 0L,
            grossPaisa = 1_000_000L,
            commissionPaisa = 0L,
            labourPaisa = 0L,
            netPayablePaisa = 1_000_000L,
            paidPaisa = 1_000_000L, // fully paid
            slipNo = 2L,
            createdAt = day1Millis,
            updatedAt = day1Millis
        )

        val payments = listOf(
            CashTransaction(
                id = "tx-full",
                shopId = "shop_1",
                partyId = "farmer_monty",
                transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 1_000_000L,
                paymentMode = PaymentMode.CASH,
                transactionDate = day1Millis,
                createdAt = day1Millis,
                updatedAt = day1Millis,
                sourceEntryType = "BUY",
                sourceEntryId = entry.id
            )
        )

        val slipText = buildFarmerSlipText(entry, "Monty", payments = payments)
        assertTrue(slipText.contains("पूर्ण चुकता"), "Must show 'पूर्ण चुकता' in share text")

        val html = ReceiptHtmlRenderer.renderBuyReceiptHtml(entry, sampleShop, "Monty", payments = payments)
        assertTrue(html.contains("पूर्ण चुकता"), "Must show 'पूर्ण चुकता' in print HTML")
    }
}
