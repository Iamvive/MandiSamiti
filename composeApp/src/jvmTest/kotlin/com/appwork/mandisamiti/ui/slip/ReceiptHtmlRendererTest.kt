package com.appwork.mandisamiti.ui.slip

import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.BuyCropLot
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.SellCropLot
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.model.ShopProfile
import kotlin.test.Test
import kotlin.test.assertTrue

class ReceiptHtmlRendererTest {

    private val sampleShop = ShopProfile(
        id = "shop_1",
        shopName = "श्री गणेश आढ़त",
        ownerName = "विवेक कुमार",
        mandiName = "नवीन गल्ला मंडी, हरदोई",
        shopNumber = "B-12",
        phoneNumber = "9876543210",
        pinHash = "hash",
        createdAt = 1700000000000L,
        updatedAt = 1700000000000L,
        address = "दुकान नं. 12, ब्लॉक बी",
        gstin = "09AAAAA0000A1Z5",
        licenceNo = "LIC-HRD-2026-99"
    )

    @Test
    fun testBuyReceiptHtmlContainsShopHeadersAndMath() {
        val entry = BuyEntry(
            id = "buy_123456",
            shopId = "shop_1",
            farmerId = "farmer_1",
            commodityId = "गेहूं",
            entryDate = 1731670000000L,
            bagsCount = 10,
            weightPerBagGrams = 50_000,
            extraWeightGrams = 21_000,
            totalWeightGrams = 521_000, // 5.21 Qtl
            ratePaisaPerQuintal = 200_000, // ₹2,000 / Qtl
            commissionBps = 150,
            labourPerBagPaisa = 1_000, // ₹10 / bag
            advancePaidPaisa = 50_000, // ₹500 advance
            grossPaisa = 1_042_000, // ₹10,420
            commissionPaisa = 15_630, // ₹156.30
            labourPaisa = 11_000, // ₹110 (11 bags due to >20kg rule)
            netPayablePaisa = 1_015_370,
            paidPaisa = 0L,
            slipNo = 2L,
            vehicleNumber = "9988776655",
            remarks = "गाँव: माधौगंज",
            createdAt = 1731670000000L,
            updatedAt = 1731670000000L
        )

        val html = ReceiptHtmlRenderer.renderBuyReceiptHtml(
            entry = entry,
            shop = sampleShop,
            farmerName = "राम शरण",
            lots = emptyList()
        )

        // Verify A5 page constraint
        assertTrue(html.contains("@page { size: A5 portrait;"), "HTML must specify A5 portrait paper size")
        // Verify Shop info
        assertTrue(html.contains("श्री गणेश आढ़त"), "Must contain shop name")
        assertTrue(html.contains("नवीन गल्ला मंडी, हरदोई"), "Must contain mandi name")
        assertTrue(html.contains("09AAAAA0000A1Z5"), "Must contain GSTIN")
        assertTrue(html.contains("LIC-HRD-2026-99"), "Must contain licence number")
        // Verify Farmer and slip
        assertTrue(html.contains("राम शरण"), "Must contain farmer name")
        assertTrue(html.contains("माधौगंज"), "Must contain village")
        assertTrue(html.contains("9988776655"), "Must contain mobile")
        assertTrue(html.contains("#2"), "Must contain slip number")
        // Verify Equation and amounts
        assertTrue(html.contains("10 × 50 + 21 = 5.21 क्विंटल × ₹2,000"), "Must contain exact slip equation")
        assertTrue(html.contains("10,420"), "Must contain gross amount")
        assertTrue(html.contains("500"), "Must contain advance amount")
        assertTrue(html.contains("किसान खरीद पर्चा"), "Must contain slip title")
    }

    @Test
    fun testBuyReceiptHtmlWithMultipleLots() {
        val entry = BuyEntry(
            id = "buy_multi",
            shopId = "shop_1",
            farmerId = "farmer_1",
            commodityId = "गेहूं",
            entryDate = 1731670000000L,
            bagsCount = 20,
            weightPerBagGrams = 50_000,
            extraWeightGrams = 0,
            totalWeightGrams = 1_000_000,
            ratePaisaPerQuintal = 200_000,
            commissionBps = 150,
            labourPerBagPaisa = 1_000,
            advancePaidPaisa = 0,
            grossPaisa = 2_000_000,
            commissionPaisa = 30_000,
            labourPaisa = 20_000,
            netPayablePaisa = 1_950_000,
            createdAt = 1731670000000L,
            updatedAt = 1731670000000L
        )

        val lots = listOf(
            BuyCropLot(
                cropName = "गेहूं शरबती",
                bags = 12,
                weightPerBagGrams = 50_000,
                totalWeightGrams = 600_000,
                ratePaisaPerQuintal = 220_000,
                grossPaisa = 1_320_000
            ),
            BuyCropLot(
                cropName = "सरसों",
                bags = 8,
                weightPerBagGrams = 50_000,
                totalWeightGrams = 400_000,
                ratePaisaPerQuintal = 500_000,
                grossPaisa = 2_000_000
            )
        )

        val html = ReceiptHtmlRenderer.renderBuyReceiptHtml(
            entry = entry,
            shop = sampleShop,
            farmerName = "सुरेश कुमार",
            lots = lots
        )

        assertTrue(html.contains("गेहूं शरबती"), "Must list first lot crop name")
        assertTrue(html.contains("सरसों"), "Must list second lot crop name")
        assertTrue(html.contains("12 × 50 + 0 = 6 क्विंटल × ₹2,200"), "Must contain first lot equation")
        assertTrue(html.contains("8 × 50 + 0 = 4 क्विंटल × ₹5,000"), "Must contain second lot equation")
    }

    @Test
    fun testSellReceiptHtmlContainsBuyerAndCharges() {
        val entry = SellEntry(
            id = "sell_123456",
            shopId = "shop_1",
            buyerId = "buyer_1",
            commodityId = "धान 1509",
            entryDate = 1731670000000L,
            billMode = BillMode.NORMAL,
            partyKind = SellPartyKind.MERCHANT,
            gstin = "09BBBBB1111B1Z9",
            bagsCount = 50,
            weightPerBagGrams = 50_000,
            totalWeightGrams = 2_500_000, // 25 Qtl
            ratePaisaPerQuintal = 300_000, // ₹3,000 / Qtl
            aratBps = 150, // 1.5%
            tulakPaisa = 0,
            grossPaisa = 7_500_000, // ₹75,000
            aratPaisa = 112_500, // ₹1,125
            mandiShulkPaisa = 0,
            totalReceivablePaisa = 7_625_000, // ₹75,000 + ₹1,125 + ₹125 labour = ₹76,250
            receivedPaisa = 50_000_00,
            balanceDuePaisa = 26_250_00,
            slipNo = 1L,
            createdAt = 1731670000000L,
            updatedAt = 1731670000000L
        )

        val html = ReceiptHtmlRenderer.renderSellReceiptHtml(
            entry = entry,
            shop = sampleShop,
            buyerName = "गुप्ता ट्रेडर्स"
        )

        assertTrue(html.contains("@page { size: A5 portrait;"), "HTML must specify A5 portrait paper size")
        assertTrue(html.contains("बिक्री पर्चा (Mandi Sale Bill)"), "Must contain sale bill title")
        assertTrue(html.contains("गुप्ता ट्रेडर्स"), "Must contain buyer name")
        assertTrue(html.contains("09BBBBB1111B1Z9"), "Must contain buyer GSTIN")
        assertTrue(html.contains("धान 1509"), "Must contain crop name")
        assertTrue(html.contains("75,000"), "Must contain gross amount")
        assertTrue(html.contains("76,250"), "Must contain total receivable")
    }

    @Test
    fun testBuyReceiptHtmlFollowsSequenceWithCombinedKarchaAndPaymentTimeline() {
        val entry = BuyEntry(
            id = "buy_seq",
            shopId = "shop_1",
            farmerId = "farmer_1",
            commodityId = "बाजरा",
            entryDate = 1731670000000L,
            bagsCount = 25,
            weightPerBagGrams = 100_000,
            extraWeightGrams = 0,
            totalWeightGrams = 2_500_000,
            ratePaisaPerQuintal = 200_000,
            commissionBps = 150,
            labourPerBagPaisa = 1_000,
            advancePaidPaisa = 0L,
            grossPaisa = 5_000_000L,
            commissionPaisa = 75_000L,
            labourPaisa = 25_000L,
            netPayablePaisa = 4_900_000L,
            paidPaisa = 3_700_000L,
            slipNo = 1L,
            createdAt = 1731670000000L,
            updatedAt = 1731756400000L
        )

        val payments = listOf(
            com.appwork.mandisamiti.domain.model.CashTransaction(
                id = "tx1", shopId = "shop_1", partyId = "farmer_1",
                transactionType = com.appwork.mandisamiti.domain.model.TransactionType.UDHAR_GIVEN,
                amountPaisa = 2_500_000L, paymentMode = com.appwork.mandisamiti.domain.model.PaymentMode.CASH,
                transactionDate = 1731670000000L, createdAt = 1731670000000L, updatedAt = 1731670000000L,
                sourceEntryType = "BUY", sourceEntryId = entry.id
            ),
            com.appwork.mandisamiti.domain.model.CashTransaction(
                id = "tx2", shopId = "shop_1", partyId = "farmer_1",
                transactionType = com.appwork.mandisamiti.domain.model.TransactionType.UDHAR_GIVEN,
                amountPaisa = 1_200_000L, paymentMode = com.appwork.mandisamiti.domain.model.PaymentMode.CASH,
                transactionDate = 1731756400000L, createdAt = 1731756400000L, updatedAt = 1731756400000L,
                sourceEntryType = "BUY", sourceEntryId = entry.id
            )
        )

        val html = ReceiptHtmlRenderer.renderBuyReceiptHtml(
            entry = entry,
            shop = sampleShop,
            farmerName = "Monty",
            payments = payments
        )

        // 1. Combined खर्चा (no split)
        assertTrue(html.contains("<td>कुल खर्चा:</td>"), "Must have single 'कुल खर्चा:' line")
        assertTrue(!html.contains("मजदूरी + आढ़त"), "Must not split labour and commission in label")
        assertTrue(html.contains("- ₹1,000"), "Combined expense must be ₹1,000")

        // 2. पर्ची राशि
        assertTrue(html.contains("<td>पर्ची राशि:</td>"), "Must have 'पर्ची राशि:' line")
        assertTrue(html.contains("₹49,000"), "Slip value must be ₹49,000")

        // 3. Payment timeline with dates
        val date1 = com.appwork.mandisamiti.domain.model.receiptDateShort(1731670000000L)
        val date2 = com.appwork.mandisamiti.domain.model.receiptDateShort(1731756400000L)
        assertTrue(html.contains("• नकद भुगतान · $date1:"), "Must have Day 1 payment with date")
        assertTrue(html.contains("• नकद भुगतान · $date2:"), "Must have Day 2 payment with date")

        // 4. Remaining balance
        assertTrue(html.contains("₹12,000"), "Must show remaining balance ₹12,000")
    }
}
