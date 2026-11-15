package com.appwork.mandisamiti.ui.print

import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.DealStatus
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.platform.ReceiptPrinter
import com.appwork.mandisamiti.ui.slip.ReceiptHtmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReceiptPrintIntegrationTest {

    private val sampleShop = ShopProfile(
        id = "shop_test",
        shopName = "कृष्णा ट्रेडर्स",
        ownerName = "विवेक कुमार",
        mandiName = "नवीन मंडी, हरदोई",
        shopNumber = "Shop 10",
        phoneNumber = "9876543210",
        pinHash = "pin_hash",
        createdAt = 1700000000000L,
        updatedAt = 1700000000000L,
        address = "मंडी प्रांगण, हरदोई",
        gstin = "09AAAAA0000A1Z5",
        licenceNo = "LIC-HRD-2026-01"
    )

    @Test
    fun testBuyReceiptPrintsA5HtmlViaReceiptPrinter() {
        val printer = ReceiptPrinter()
        val buy = BuyEntry(
            id = "buy_print_01",
            shopId = "shop_test",
            farmerId = "farmer_01",
            commodityId = "गेहूं",
            entryDate = 1731670000000L,
            bagsCount = 10,
            weightPerBagGrams = 50_000,
            extraWeightGrams = 21_000,
            totalWeightGrams = 521_000,
            ratePaisaPerQuintal = 200_000,
            commissionBps = 150,
            labourPerBagPaisa = 1_000,
            advancePaidPaisa = 0,
            grossPaisa = 1_042_000,
            commissionPaisa = 15_630,
            labourPaisa = 11_000,
            netPayablePaisa = 1_015_370,
            slipNo = 2L,
            createdAt = 1731670000000L,
            updatedAt = 1731670000000L
        )

        val html = ReceiptHtmlRenderer.renderBuyReceiptHtml(
            entry = buy,
            shop = sampleShop,
            farmerName = "श्याम लाल"
        )
        val jobName = "पर्चा_01"
        printer.printHtml(jobName, html)

        assertEquals(jobName, printer.lastPrintedJobName)
        val printedHtml = printer.lastPrintedHtml
        assertNotNull(printedHtml)
        assertTrue(printedHtml.contains("@page { size: A5 portrait;"))
        assertTrue(printedHtml.contains("कृष्णा ट्रेडर्स"))
        assertTrue(printedHtml.contains("श्याम लाल"))
        assertTrue(printedHtml.contains("#2"))
        assertTrue(printedHtml.contains("10 × 50 + 21 = 5.21 क्विंटल × ₹2,000"))
    }

    @Test
    fun testSellReceiptPrintsA5HtmlViaReceiptPrinter() {
        val printer = ReceiptPrinter()
        val sell = SellEntry(
            id = "sell_print_01",
            shopId = "shop_test",
            buyerId = "buyer_01",
            commodityId = "सरसों",
            entryDate = 1731670000000L,
            billMode = BillMode.NORMAL,
            partyKind = SellPartyKind.MERCHANT,
            bagsCount = 20,
            weightPerBagGrams = 50_000,
            totalWeightGrams = 1_000_000,
            ratePaisaPerQuintal = 550_000,
            aratBps = 150,
            tulakPaisa = 0,
            grossPaisa = 5_500_000,
            aratPaisa = 82_500,
            mandiShulkPaisa = 0,
            totalReceivablePaisa = 5_587_500,
            receivedPaisa = 0,
            balanceDuePaisa = 5_587_500,
            slipNo = 1L,
            createdAt = 1731670000000L,
            updatedAt = 1731670000000L
        )

        val html = ReceiptHtmlRenderer.renderSellReceiptHtml(
            entry = sell,
            shop = sampleShop,
            buyerName = "अग्रवाल ऑइल मिल"
        )
        val jobName = "बिल_01"
        printer.printHtml(jobName, html)

        assertEquals(jobName, printer.lastPrintedJobName)
        val printedHtml = printer.lastPrintedHtml
        assertNotNull(printedHtml)
        assertTrue(printedHtml.contains("@page { size: A5 portrait;"))
        assertTrue(printedHtml.contains("अग्रवाल ऑइल मिल"))
        assertTrue(printedHtml.contains("सरसों"))
        assertTrue(printedHtml.contains("बिक्री पर्चा (Mandi Sale Bill)"))
    }

    @Test
    fun testDealReceiptPrintsA5HtmlViaReceiptPrinter() {
        val printer = ReceiptPrinter()
        val deal = Deal(
            id = "deal_print_01",
            shopId = "shop_test",
            farmerId = "farmer_01",
            buyerId = "buyer_01",
            commodityId = "गेहूं",
            dealStatus = DealStatus.SETTLED,
            dealDate = 1731670000000L,
            bagsCount = 10,
            grossWeightGrams = 500_000,
            netWeightGrams = 500_000,
            ratePaisaPerUnit = 240_000,
            grossAmountPaisa = 1_200_000,
            labourChargePaisa = 10_000,
            farmerCommissionPaisa = 24_000,
            buyerCommissionPaisa = 0,
            netFarmerPayablePaisa = 1_166_000,
            createdAt = 1731670000000L,
            updatedAt = 1731670000000L
        )
        val farmer = Party(
            id = "farmer_01",
            shopId = "shop_test",
            name = "मुकेश कुमार",
            partyType = PartyType.FARMER,
            phone = "9988776655",
            village = "शाहाबाद",
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L
        )
        val buyer = Party(
            id = "buyer_01",
            shopId = "shop_test",
            name = "राजेश ट्रेडर्स",
            partyType = PartyType.BUYER,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L
        )

        val html = ReceiptHtmlRenderer.renderDealReceiptHtml(deal, sampleShop, farmer, buyer)
        val jobName = "सौदा_01"
        printer.printHtml(jobName, html)

        assertEquals(jobName, printer.lastPrintedJobName)
        val printedHtml = printer.lastPrintedHtml
        assertNotNull(printedHtml)
        assertTrue(printedHtml.contains("@page { size: A5 portrait;"))
        assertTrue(printedHtml.contains("पक्का सौदा पर्चा (Settlement Slip)"))
        assertTrue(printedHtml.contains("मुकेश कुमार"))
        assertTrue(printedHtml.contains("राजेश ट्रेडर्स"))
    }
}
