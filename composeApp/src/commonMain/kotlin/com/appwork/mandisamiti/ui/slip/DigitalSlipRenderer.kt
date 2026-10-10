package com.appwork.mandisamiti.ui.slip

import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.ShopProfile

object DigitalSlipRenderer {

    /**
     * Formats a complete traditional Mandi settlement slip text optimized for WhatsApp sharing.
     */
    fun formatWhatsAppReceiptText(
        shopProfile: ShopProfile,
        farmer: Party,
        buyer: Party?,
        deal: Deal
    ): String {
        val dateStr = "दिनांक: ${kotlinx.datetime.Instant.fromEpochMilliseconds(deal.dealDate)}"
        val rateStr = deal.ratePaisaPerUnit?.let { "₹ ${MandiMathEngine.paisaToRupeesString(it)} / कुंतल" } ?: "बाजार भाव"
        val grossQ = MandiMathEngine.gramsToQuintals(deal.grossWeightGrams)
        val cutQ = MandiMathEngine.gramsToQuintals(deal.cutWeightGrams)
        val netQ = MandiMathEngine.gramsToQuintals(deal.netWeightGrams)

        val farmerPayableRs = MandiMathEngine.paisaToRupeesString(kotlin.math.abs(deal.netFarmerPayablePaisa))
        val labourRs = MandiMathEngine.paisaToRupeesString(deal.labourChargePaisa)
        val commRs = MandiMathEngine.paisaToRupeesString(deal.farmerCommissionPaisa)

        return buildString {
            appendLine("🌾 *${shopProfile.shopName}* 🌾")
            appendLine("📍 ${shopProfile.mandiName} • दूकान नं: ${shopProfile.shopNumber ?: "-"}")
            appendLine("📞 मो.: ${shopProfile.phoneNumber}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📜 *पक्का सौदा पर्चा (Settlement Slip)*")
            appendLine("👨‍🌾 *किसान:* ${farmer.name} (गांव: ${farmer.village ?: "-"})")
            if (buyer != null) {
                appendLine("🏭 *व्यापारी:* ${buyer.name}")
            }
            appendLine("📦 *बोरी:* ${deal.bagsCount} | *भाव:* $rateStr")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("⚖️ *कुल वजन (Gross):* $grossQ कुंतल")
            appendLine("✂️ *कटौती (Tare):* $cutQ कुंतल")
            appendLine("✨ *शुद्ध वजन (Net):* $netQ कुंतल")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("💵 *कुल रकम:* ₹ ${MandiMathEngine.paisaToRupeesString(deal.grossAmountPaisa)}")
            appendLine("➖ *पल्लेदारी / मजदूरी:* ₹ $labourRs")
            appendLine("➖ *मंडी आढ़त:* ₹ $commRs")
            if (deal.buyerCommissionPaisa > 0L) {
                val buyerCommRs = MandiMathEngine.paisaToRupeesString(deal.buyerCommissionPaisa)
                appendLine("➕ *व्यापारी आढ़त:* ₹ $buyerCommRs")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            if (deal.netFarmerPayablePaisa < 0) appendLine("*किसान से लेना है: ₹ $farmerPayableRs*")
            else appendLine("🟢 *शुद्ध देय भुगतान (Net Payable): ₹ $farmerPayableRs*")
            if (buyer != null) {
                val buyerRecRs = MandiMathEngine.paisaToRupeesString(deal.netBuyerReceivablePaisa)
                appendLine("🔵 *व्यापारी से वसूली: ₹ $buyerRecRs*")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("धन्यवाद! 🙏")
            appendLine("— ${shopProfile.ownerName} (${shopProfile.shopName})")
        }
    }
}
