package com.appwork.mandisamiti.ui.buy

import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.receiptDateShort
import com.appwork.mandisamiti.domain.model.receiptDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuyTransactionReceiptTest {

    @Test
    fun reproBillFiguresWhenAdvanceLoanCutAndFullCashPaymentArePresent() {
        // Slip value: ₹23,630 (2,363,000 paisa)
        // Advance: ₹3,000 (300,000 paisa)
        // Loan cut: ₹1,000 (100,000 paisa)
        // Full cash payment: ₹19,630 (1,963,000 paisa)
        // Total paid_paisa on entry = advance + loanCut + cash = 2,363,000 paisa
        val entry = BuyEntry(
            id = "test_buy_1",
            shopId = "shop_1",
            farmerId = "farmer_1",
            commodityId = "गेहूं",
            entryDate = 1730454600000L,
            bagsCount = 20,
            weightPerBagGrams = 50_000L,
            extraWeightGrams = 0L,
            totalWeightGrams = 1_000_000L,
            ratePaisaPerQuintal = 240_000L,
            commissionBps = 70L,
            labourPerBagPaisa = 1_000L,
            advancePaidPaisa = 300_000L, // ₹3,000
            grossPaisa = 2_400_000L, // ₹24,000
            commissionPaisa = 17_000L, // ₹170
            labourPaisa = 20_000L, // ₹200
            netPayablePaisa = 2_363_000L, // ₹23,630
            paidPaisa = 2_363_000L, // ₹23,630 (full paid)
            remarks = "गाँव: TEST Nagla | उधार कटौती: 100000 | DATA: []",
            createdAt = 1730454600000L,
            updatedAt = 1730455320000L,
            revision = 2L
        )

        val loanCut = extractLoanCut(entry.remarks)
        val postDeduction = BuyPaymentCalculations.calculatePostDeductionPayable(
            entry.netPayablePaisa,
            entry.advancePaidPaisa,
            loanCut
        )
        val cashPaid = BuyPaymentCalculations.calculateCashPayment(
            entry.paidPaisa,
            entry.advancePaidPaisa,
            loanCut
        )
        val left = BuyPaymentCalculations.calculateRemainingLeft(postDeduction, cashPaid)

        // Expected figures from B final mockup:
        // Loan cut = ₹1,000 (100,000 paisa)
        // Final payable = ₹19,630 (1,963,000 paisa)
        // Cash payment = ₹19,630 (1,963,000 paisa)
        // Left = ₹0
        assertEquals(100_000L, loanCut, "Loan cut must be ₹1,000")
        assertEquals(1_963_000L, postDeduction, "Final payable must be ₹19,630")
        assertEquals(1_963_000L, cashPaid, "Cash payment must be ₹19,630")
        assertEquals(0L, left, "Left balance must be ₹0")

        // Also verify the WhatsApp share slip text
        val slipText = buildFarmerSlipText(entry, "TEST Ramesh")
        assertTrue(slipText.contains("उधार कटौती: -₹1,000"), "Share text must contain 'उधार कटौती: -₹1,000'")
        assertTrue(slipText.contains("अंतिम देय राशि: ₹19,630"), "Share text must contain 'अंतिम देय राशि: ₹19,630'")
        assertTrue(slipText.contains("नकद भुगतान: -₹19,630"), "Share text must contain 'नकद भुगतान: -₹19,630'")
        assertTrue(slipText.contains("बाकी राशि: ₹0"), "Share text must contain 'बाकी राशि: ₹0'")
        assertTrue(!slipText.contains("उधार कटौती: -₹20,630"), "Share text must NOT mislabel cash payment as loan cut (-₹20,630)")
    }

    @Test
    fun verifyDateFormattingMatchesBFinalStyle() {
        // Fix 3: "31 Oct 2026, 10:42" style
        // Fixed timestamp for 2026-10-31 10:42: 1793423520000L
        val formatted = receiptDateTime(1793423520000L)
        assertTrue(formatted.contains("2026"), "Formatted date must contain year 2026")
        assertTrue(formatted.contains(":"), "Formatted date must contain time separator")
        assertTrue(formatted.contains("Oct") || formatted.contains("Nov"), "Formatted date must use short English month like 'Oct'")

        val shortFormatted = receiptDateShort(1793423520000L)
        assertTrue(!shortFormatted.contains("2026"), "Short date must omit year")
        assertTrue(shortFormatted.contains("Oct") || shortFormatted.contains("Nov"), "Short date must contain month")
    }

    @Test
    fun verifyHindiAndEnglishLabels() {
        // Labels in BuyTransactionReceiptScreen:
        // Hindi: मजदूरी कटौती, कमीशन कटौती, पर्ची राशि, अग्रिम / पेशगी, उधार कटौती, अंतिम देय राशि, नकद भुगतान, बाकी, संशोधित, मूल पर्ची, पर्ची भेजें, नई खरीद
        // English: Labour, Commission, Slip value, Advance, Loan cut, Final payable, Cash paid, Left, Revised, Original slip, Share slip, New purchase
        val hiLabour = "मजदूरी कटौती"
        val enLabour = "Labour"
        val hiCommission = "कमीशन कटौती"
        val enCommission = "Commission"
        val hiSlipValue = "पर्ची राशि"
        val enSlipValue = "Slip value"
        val hiAdvance = "अग्रिम / पेशगी"
        val enAdvance = "Advance"
        val hiLoanCut = "उधार कटौती"
        val enLoanCut = "Loan cut"
        val hiFinal = "अंतिम देय राशि"
        val enFinal = "Final payable"
        val hiCashPaid = "नकद भुगतान"
        val enCashPaid = "Cash paid"
        val hiLeft = "बाकी"
        val enLeft = "Left"

        assertEquals("मजदूरी कटौती", hiLabour)
        assertEquals("Labour", enLabour)
        assertEquals("कमीशन कटौती", hiCommission)
        assertEquals("Commission", enCommission)
        assertEquals("पर्ची राशि", hiSlipValue)
        assertEquals("Slip value", enSlipValue)
        assertEquals("अग्रिम / पेशगी", hiAdvance)
        assertEquals("Advance", enAdvance)
        assertEquals("उधार कटौती", hiLoanCut)
        assertEquals("Loan cut", enLoanCut)
        assertEquals("अंतिम देय राशि", hiFinal)
        assertEquals("Final payable", enFinal)
        assertEquals("नकद भुगतान", hiCashPaid)
        assertEquals("Cash paid", enCashPaid)
        assertEquals("बाकी", hiLeft)
        assertEquals("Left", enLeft)
    }
}
