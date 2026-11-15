package com.appwork.mandisamiti.ui.sell

import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import kotlin.test.Test
import kotlin.test.assertTrue

class SellTransactionReceiptTest {

    @Test
    fun buildSellBillText_includesSlipEquationAndCharges() {
        val entry = SellEntry(
            id = "sell-1",
            shopId = "shop-1",
            buyerId = "Suresh Trader",
            commodityId = "गेहूं",
            entryDate = 1731672000000L,
            billMode = BillMode.BILL_OF_SUPPLY,
            partyKind = SellPartyKind.MERCHANT,
            gstin = "23AAAAA0000A1Z5",
            bagsCount = 10,
            weightPerBagGrams = 50_000L,
            totalWeightGrams = 521_000L, // 10 x 50 + 21 kg extra
            ratePaisaPerQuintal = 200_000L, // Rs 2000
            aratBps = 100L, // 1%
            tulakPaisa = 521L,
            grossPaisa = 1_042_000L,
            aratPaisa = 10_420L,
            mandiShulkPaisa = 15_630L, // 1.5%
            totalReceivablePaisa = 1_071_100L, // rounded whole rupee with labour
            receivedPaisa = 0L,
            balanceDuePaisa = 1_071_100L,
            createdAt = 1731672000000L,
            updatedAt = 1731672000000L
        )

        val billText = buildSellBillText(entry, "Suresh Trader")
        assertTrue(billText.contains("10 × 50 + 21 = 5.21 क्विंटल × ₹2,000"), "Must contain slip equation with extra weight")
        assertTrue(billText.contains("मंडी शुल्क एवं उपकर: +₹156.30"), "Must contain mandi tax")
        assertTrue(billText.contains("तुलाई: +₹5.21"), "Must contain tulak")
        assertTrue(billText.contains("मजदूरी: +₹25.29"), "Must contain labour line (remainder after commission, shulk, tulak)")
        assertTrue(billText.contains("कुल देय राशि: ₹10,711"), "Must contain total receivable")
    }

    @Test
    fun buildSellBillText_includesShopHeaderAndTaxDetails() {
        val entry = SellEntry(
            id = "sell-2",
            shopId = "shop-1",
            buyerId = "Ramesh Merchant",
            commodityId = "धान",
            entryDate = 1731672000000L,
            billMode = BillMode.BILL_OF_SUPPLY,
            partyKind = SellPartyKind.MERCHANT,
            bagsCount = 5,
            weightPerBagGrams = 50_000L,
            totalWeightGrams = 250_000L,
            ratePaisaPerQuintal = 180_000L,
            aratBps = 0L,
            tulakPaisa = 0L,
            grossPaisa = 450_000L,
            aratPaisa = 0L,
            mandiShulkPaisa = 0L,
            totalReceivablePaisa = 450_000L,
            receivedPaisa = 0L,
            balanceDuePaisa = 450_000L,
            createdAt = 1731672000000L,
            updatedAt = 1731672000000L
        )

        val billText = buildSellBillText(
            entry = entry,
            buyerName = "Ramesh Merchant",
            shopName = "कृष्णा ट्रेडर्स",
            mandiName = "मथुरा मंडी",
            shopAddress = "दुकान नं. 15",
            shopGstin = "09ABCDE1234F1Z5",
            shopLicenceNo = "LIC-999"
        )
        assertTrue(billText.contains("विक्रेता: कृष्णा ट्रेडर्स, मथुरा मंडी, दुकान नं. 15"))
        assertTrue(billText.contains("विक्रेता GSTIN: 09ABCDE1234F1Z5"))
        assertTrue(billText.contains("विक्रेता लाइसेंस: LIC-999"))
    }
}
