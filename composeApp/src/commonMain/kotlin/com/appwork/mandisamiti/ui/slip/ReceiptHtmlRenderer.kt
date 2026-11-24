package com.appwork.mandisamiti.ui.slip

import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.math.sellSlipAmounts
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.BuyCropLot
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.SellCropLot
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.receiptDateShort
import com.appwork.mandisamiti.ui.buy.BuyPaymentCalculations
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object ReceiptHtmlRenderer {

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun formatDateTime(epochMillis: Long): String {
        return try {
            val instant = Instant.fromEpochMilliseconds(epochMillis)
            val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val day = ldt.dayOfMonth.toString().padStart(2, '0')
            val month = ldt.monthNumber.toString().padStart(2, '0')
            val year = ldt.year
            val hour = ldt.hour.toString().padStart(2, '0')
            val minute = ldt.minute.toString().padStart(2, '0')
            "$day/$month/$year $hour:$minute"
        } catch (_: Exception) {
            epochMillis.toString()
        }
    }

    private fun slipLabel(slipNo: Long?, id: String): String {
        return slipNo?.let { "#$it" } ?: ("#" + id.takeLast(6).uppercase())
    }

    private fun extractVillage(remarks: String?): String? {
        if (remarks.isNullOrBlank()) return null
        val match = Regex("""(?:गाँव|गांव|Village):\s*([^,\n]+)""", RegexOption.IGNORE_CASE).find(remarks)
        return match?.groupValues?.get(1)?.trim()
    }

    private fun extractLoanCut(remarks: String?): Long {
        if (remarks.isNullOrBlank()) return 0L
        val match = Regex("""LOAN_CUT:(\d+)""").find(remarks) ?: return 0L
        return match.groupValues[1].toLongOrNull() ?: 0L
    }

    private fun renderStyles(): String = """
        <style>
            @page { size: A5 portrait; margin: 10mm; }
            @media print {
                body {
                    -webkit-print-color-adjust: exact;
                    print-color-adjust: exact;
                }
            }
            * {
                box-sizing: border-box;
            }
            body {
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Noto Sans Devanagari", sans-serif;
                font-size: 13px;
                line-height: 1.35;
                color: #111;
                background: #fff;
                margin: 0;
                padding: 0;
            }
            .header {
                text-align: center;
                border-bottom: 2px solid #111;
                padding-bottom: 6px;
                margin-bottom: 10px;
            }
            .shop-name {
                font-size: 20px;
                font-weight: bold;
                margin: 0 0 4px 0;
            }
            .shop-sub {
                font-size: 12px;
                color: #333;
                margin: 2px 0;
            }
            .doc-title {
                font-size: 15px;
                font-weight: bold;
                margin-top: 6px;
                letter-spacing: 0.5px;
            }
            .meta-table {
                width: 100%;
                border-collapse: collapse;
                margin-bottom: 10px;
            }
            .meta-table td {
                padding: 2px 4px;
                font-size: 12px;
                vertical-align: top;
            }
            .meta-label {
                color: #555;
                width: 15%;
            }
            .meta-val {
                font-weight: 600;
                width: 35%;
            }
            .items-table {
                width: 100%;
                border-collapse: collapse;
                margin-bottom: 10px;
            }
            .items-table th {
                background: #f4f4f4;
                border: 1px solid #222;
                padding: 6px 6px;
                font-weight: bold;
                font-size: 12px;
                text-align: left;
            }
            .items-table td {
                border: 1px solid #222;
                padding: 5px 6px;
                font-size: 12px;
            }
            .text-right {
                text-align: right;
            }
            .text-center {
                text-align: center;
            }
            .nowrap {
                white-space: nowrap;
            }
            .summary-table {
                width: 100%;
                border-collapse: collapse;
                margin-top: 6px;
                margin-bottom: 14px;
            }
            .summary-table td {
                padding: 3px 6px;
                font-size: 13px;
            }
            .summary-table .border-top {
                border-top: 1px solid #444;
            }
            .summary-table .grand-total {
                font-size: 15px;
                font-weight: bold;
                background: #f0f0f0;
                border-top: 2px solid #000;
                border-bottom: 2px solid #000;
            }
            .summary-table .grand-total td {
                font-size: 15px;
                font-weight: bold;
            }
            .summary-table .parchi-rashi {
                font-size: 15px;
                font-weight: bold;
                background: #f4f4f4;
            }
            .summary-table .parchi-rashi td {
                font-size: 15px;
                font-weight: bold;
                padding: 5px 6px;
                border-top: 2px solid #000;
                border-bottom: 2px solid #000;
            }
            .footer {
                margin-top: 28px;
                display: flex;
                justify-content: space-between;
                font-size: 12px;
            }
            .sign-box {
                border-top: 1px dashed #444;
                width: 150px;
                text-align: center;
                padding-top: 4px;
                font-weight: 600;
            }
        </style>
    """.trimIndent()

    fun renderBuyReceiptHtml(
        entry: BuyEntry,
        shop: ShopProfile? = null,
        farmerName: String,
        lots: List<BuyCropLot> = emptyList(),
        payments: List<CashTransaction> = emptyList(),
        partyPhone: String? = null
    ): String {
        val isRevised = entry.revision > 1 || entry.paidPaisa > 0
        val title = if (isRevised) "किसान खरीद पर्चा (संशोधित)" else "किसान खरीद पर्चा"

        val shopName = shop?.shopName ?: ""
        val mandiName = shop?.mandiName ?: ""
        val address = shop?.address
        val gstin = shop?.gstin
        val licenceNo = shop?.licenceNo
        val phone = shop?.phoneNumber ?: ""

        val shopHeader = listOfNotNull(
            shopName.takeIf { it.isNotBlank() },
            mandiName.takeIf { it.isNotBlank() },
            address?.takeIf { it.isNotBlank() }
        ).joinToString(" • ")

        val village = extractVillage(entry.remarks)
        val loanCutPaisa = extractLoanCut(entry.remarks)
        val postDeduction = BuyPaymentCalculations.calculatePostDeductionPayable(
            entry.netPayablePaisa,
            entry.advancePaidPaisa,
            loanCutPaisa
        )
        val cashPaid = BuyPaymentCalculations.calculateCashPayment(
            entry.paidPaisa,
            entry.advancePaidPaisa,
            loanCutPaisa
        )
        val leftPaisa = BuyPaymentCalculations.calculateRemainingLeft(postDeduction, cashPaid)

        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"hi\">")
            appendLine("<head>")
            appendLine("<meta charset=\"UTF-8\">")
            appendLine("<title>${escapeHtml(title)} - ${slipLabel(entry.slipNo, entry.id)}</title>")
            appendLine(renderStyles())
            appendLine("</head>")
            appendLine("<body>")

            // Header
            appendLine("<div class=\"header\">")
            if (shopName.isNotBlank()) {
                appendLine("<div class=\"shop-name\">${escapeHtml(shopName)}</div>")
            }
            if (mandiName.isNotBlank() || !address.isNullOrBlank()) {
                val sub = listOfNotNull(mandiName.takeIf { it.isNotBlank() }, address?.takeIf { it.isNotBlank() }).joinToString(", ")
                appendLine("<div class=\"shop-sub\">${escapeHtml(sub)}</div>")
            }
            val taxLines = listOfNotNull(
                gstin?.takeIf { it.isNotBlank() }?.let { "GSTIN: $it" },
                licenceNo?.takeIf { it.isNotBlank() }?.let { "लाइसेंस नं.: $it" },
                phone.takeIf { it.isNotBlank() }?.let { "मो.: $it" }
            ).joinToString(" | ")
            if (taxLines.isNotBlank()) {
                appendLine("<div class=\"shop-sub\">${escapeHtml(taxLines)}</div>")
            }
            appendLine("<div class=\"doc-title\">${escapeHtml(title)}</div>")
            appendLine("</div>")

            // Meta Info
            appendLine("<table class=\"meta-table\">")
            appendLine("<tr>")
            appendLine("<td class=\"meta-label\">पर्चा सं.:</td>")
            appendLine("<td class=\"meta-val\">${escapeHtml(slipLabel(entry.slipNo, entry.id))}</td>")
            appendLine("<td class=\"meta-label\">दिनांक:</td>")
            appendLine("<td class=\"meta-val text-right\">${escapeHtml(formatDateTime(if (isRevised) entry.updatedAt else entry.createdAt))}</td>")
            appendLine("</tr>")
            appendLine("<tr>")
            appendLine("<td class=\"meta-label\">किसान:</td>")
            appendLine("<td class=\"meta-val\">${escapeHtml(farmerName)}</td>")
            appendLine("<td class=\"meta-label\">गाँव:</td>")
            appendLine("<td class=\"meta-val text-right\">${escapeHtml(village ?: "-")}</td>")
            appendLine("</tr>")
            if (!partyPhone.isNullOrBlank()) {
                appendLine("<tr>")
                appendLine("<td class=\"meta-label\">मोबाइल:</td>")
                appendLine("<td class=\"meta-val\" colspan=\"3\">${escapeHtml(partyPhone)}</td>")
                appendLine("</tr>")
            }
            // Buy slips store the kisan mobile in vehicleNumber; it is printed above as मोबाइल.
            appendLine("</table>")

            // Items Table
            appendLine("<table class=\"items-table\">")
            appendLine("<thead>")
            appendLine("<tr>")
            appendLine("<th class=\"text-center\" style=\"width: 30px;\">क्र.</th>")
            appendLine("<th>फसल</th>")
            appendLine("<th>तौल विवरण (बोरी × वजन + अतिरिक्त)</th>")
            appendLine("<th class=\"text-right\">रकम (₹)</th>")
            appendLine("</tr>")
            appendLine("</thead>")
            appendLine("<tbody>")

            if (lots.isNotEmpty()) {
                lots.forEachIndexed { idx, lot ->
                    val eq = slipEquation(lot.bags, lot.weightPerBagGrams, lot.totalWeightGrams, lot.ratePaisaPerQuintal)
                    appendLine("<tr>")
                    appendLine("<td class=\"text-center\">${idx + 1}</td>")
                    appendLine("<td><b>${escapeHtml(lot.cropName)}</b></td>")
                    appendLine("<td>${escapeHtml(eq)}</td>")
                    appendLine("<td class=\"text-right nowrap\">₹${MandiMathEngine.paisaToRupeesString(lot.grossPaisa)}</td>")
                    appendLine("</tr>")
                }
            } else {
                val eq = slipEquation(entry.bagsCount, entry.weightPerBagGrams, entry.totalWeightGrams, entry.ratePaisaPerQuintal)
                appendLine("<tr>")
                appendLine("<td class=\"text-center\">1</td>")
                appendLine("<td><b>${escapeHtml(entry.commodityId)}</b></td>")
                appendLine("<td>${escapeHtml(eq)}</td>")
                appendLine("<td class=\"text-right nowrap\">₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}</td>")
                appendLine("</tr>")
            }
            appendLine("</tbody>")
            appendLine("</table>")

            // Summary Table
            appendLine("<table class=\"summary-table\">")
            appendLine("<tr>")
            appendLine("<td>कुल असल राशि (Gross):</td>")
            appendLine("<td class=\"text-right\">₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}</td>")
            appendLine("</tr>")

            val totalDeductions = entry.labourPaisa + entry.commissionPaisa
            if (totalDeductions > 0L) {
                appendLine("<tr>")
                appendLine("<td>कुल खर्चा:</td>")
                appendLine("<td class=\"text-right\">- ₹${MandiMathEngine.paisaToRupeesString(totalDeductions)}</td>")
                appendLine("</tr>")
            }

            appendLine("<tr class=\"parchi-rashi\">")
            appendLine("<td>पर्ची राशि:</td>")
            appendLine("<td class=\"text-right\">₹${MandiMathEngine.paisaToRupeesString(entry.netPayablePaisa)}</td>")
            appendLine("</tr>")

            if (entry.advancePaidPaisa > 0L) {
                appendLine("<tr>")
                appendLine("<td>अग्रिम / पेशगी:</td>")
                appendLine("<td class=\"text-right\">- ₹${MandiMathEngine.paisaToRupeesString(entry.advancePaidPaisa)}</td>")
                appendLine("</tr>")
            }

            if (loanCutPaisa > 0L) {
                appendLine("<tr>")
                appendLine("<td>उधार कटौती:</td>")
                appendLine("<td class=\"text-right\">- ₹${MandiMathEngine.paisaToRupeesString(loanCutPaisa)}</td>")
                appendLine("</tr>")
            }

            if (entry.advancePaidPaisa > 0L || loanCutPaisa > 0L) {
                appendLine("<tr class=\"grand-total\">")
                appendLine("<td>अंतिम देय राशि (Final Payable):</td>")
                appendLine("<td class=\"text-right\">${MandiMathEngine.formatRupees(postDeduction)}</td>")
                appendLine("</tr>")
            }

            if (payments.isNotEmpty()) {
                payments.forEach { p ->
                    appendLine("<tr>")
                    appendLine("<td>• नकद भुगतान · ${receiptDateShort(p.transactionDate)}:</td>")
                    appendLine("<td class=\"text-right\">- ₹${MandiMathEngine.paisaToRupeesString(p.amountPaisa)}</td>")
                    appendLine("</tr>")
                }
            } else if (cashPaid > 0L) {
                appendLine("<tr>")
                appendLine("<td>• नकद भुगतान · ${receiptDateShort(entry.updatedAt)}:</td>")
                appendLine("<td class=\"text-right\">- ₹${MandiMathEngine.paisaToRupeesString(cashPaid)}</td>")
                appendLine("</tr>")
            }

            if (cashPaid > 0L || payments.isNotEmpty()) {
                if (leftPaisa > 0L) {
                    appendLine("<tr class=\"grand-total\">")
                    appendLine("<td>बाकी राशि (Balance Due):</td>")
                    appendLine("<td class=\"text-right\">${MandiMathEngine.formatRupees(leftPaisa)}</td>")
                    appendLine("</tr>")
                } else {
                    appendLine("<tr class=\"grand-total\">")
                    appendLine("<td>स्थिति:</td>")
                    appendLine("<td class=\"text-right\" style=\"color: #1b5e20;\">पूर्ण चुकता</td>")
                    appendLine("</tr>")
                }
            }

            appendLine("</table>")

            // Signatures
            appendLine("<div class=\"footer\">")
            appendLine("<div class=\"sign-box\">किसान के हस्ताक्षर</div>")
            appendLine("<div class=\"sign-box\">आढ़तिया / मुनीम हस्ताक्षर</div>")
            appendLine("</div>")

            appendLine("</body>")
            appendLine("</html>")
        }
    }

    fun renderSellReceiptHtml(
        entry: SellEntry,
        shop: ShopProfile? = null,
        buyerName: String,
        lots: List<SellCropLot> = emptyList(),
        partyPhone: String? = null
    ): String {
        val isBos = entry.billMode == BillMode.BILL_OF_SUPPLY
        val title = if (isBos) "सप्लाई बिल (Bill of Supply)" else "बिक्री पर्चा (Mandi Sale Bill)"

        val shopName = shop?.shopName ?: ""
        val mandiName = shop?.mandiName ?: ""
        val address = shop?.address
        val gstin = shop?.gstin
        val licenceNo = shop?.licenceNo
        val phone = shop?.phoneNumber ?: ""

        val amounts = sellSlipAmounts(entry)

        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"hi\">")
            appendLine("<head>")
            appendLine("<meta charset=\"UTF-8\">")
            appendLine("<title>${escapeHtml(title)} - ${slipLabel(entry.slipNo, entry.id)}</title>")
            appendLine(renderStyles())
            appendLine("</head>")
            appendLine("<body>")

            // Header
            appendLine("<div class=\"header\">")
            if (shopName.isNotBlank()) {
                appendLine("<div class=\"shop-name\">${escapeHtml(shopName)}</div>")
            }
            if (mandiName.isNotBlank() || !address.isNullOrBlank()) {
                val sub = listOfNotNull(mandiName.takeIf { it.isNotBlank() }, address?.takeIf { it.isNotBlank() }).joinToString(", ")
                appendLine("<div class=\"shop-sub\">${escapeHtml(sub)}</div>")
            }
            val taxLines = listOfNotNull(
                gstin?.takeIf { it.isNotBlank() }?.let { "विक्रेता GSTIN: $it" },
                licenceNo?.takeIf { it.isNotBlank() }?.let { "लाइसेंस: $it" },
                phone.takeIf { it.isNotBlank() }?.let { "मो.: $it" }
            ).joinToString(" | ")
            if (taxLines.isNotBlank()) {
                appendLine("<div class=\"shop-sub\">${escapeHtml(taxLines)}</div>")
            }
            appendLine("<div class=\"doc-title\">${escapeHtml(title)}</div>")
            appendLine("</div>")

            // Meta Info
            appendLine("<table class=\"meta-table\">")
            appendLine("<tr>")
            appendLine("<td class=\"meta-label\">बिल सं.:</td>")
            appendLine("<td class=\"meta-val\">${escapeHtml(slipLabel(entry.slipNo, entry.id))}</td>")
            appendLine("<td class=\"meta-label\">दिनांक:</td>")
            appendLine("<td class=\"meta-val text-right\">${escapeHtml(formatDateTime(entry.createdAt))}</td>")
            appendLine("</tr>")
            appendLine("<tr>")
            appendLine("<td class=\"meta-label\">क्रेता (Buyer):</td>")
            appendLine("<td class=\"meta-val\">${escapeHtml(buyerName)}</td>")
            appendLine("<td class=\"meta-label\">क्रेता GSTIN:</td>")
            appendLine("<td class=\"meta-val text-right\">${escapeHtml(entry.gstin ?: "-")}</td>")
            appendLine("</tr>")
            if (!partyPhone.isNullOrBlank()) {
                appendLine("<tr>")
                appendLine("<td class=\"meta-label\">मोबाइल:</td>")
                appendLine("<td class=\"meta-val\" colspan=\"3\">${escapeHtml(partyPhone)}</td>")
                appendLine("</tr>")
            }
            val vehicle = entry.vehicleNumber
            if (!vehicle.isNullOrBlank()) {
                appendLine("<tr>")
                appendLine("<td class=\"meta-label\">वाहन सं.:</td>")
                appendLine("<td class=\"meta-val\" colspan=\"3\">${escapeHtml(vehicle)}</td>")
                appendLine("</tr>")
            }
            appendLine("</table>")

            // Items Table
            appendLine("<table class=\"items-table\">")
            appendLine("<thead>")
            appendLine("<tr>")
            appendLine("<th class=\"text-center\" style=\"width: 30px;\">क्र.</th>")
            appendLine("<th>फसल</th>")
            appendLine("<th>तौल विवरण</th>")
            appendLine("<th class=\"text-right\">रकम (₹)</th>")
            appendLine("</tr>")
            appendLine("</thead>")
            appendLine("<tbody>")

            if (lots.isNotEmpty()) {
                lots.forEachIndexed { idx, lot ->
                    val eq = slipEquation(lot.bags, lot.weightPerBagGrams, lot.totalWeightGrams, lot.ratePaisaPerQuintal)
                    appendLine("<tr>")
                    appendLine("<td class=\"text-center\">${idx + 1}</td>")
                    appendLine("<td><b>${escapeHtml(lot.cropName)}</b></td>")
                    appendLine("<td>${escapeHtml(eq)}</td>")
                    appendLine("<td class=\"text-right nowrap\">₹${MandiMathEngine.paisaToRupeesString(lot.grossPaisa)}</td>")
                    appendLine("</tr>")
                }
            } else {
                val eq = slipEquation(entry.bagsCount, entry.weightPerBagGrams, entry.totalWeightGrams, entry.ratePaisaPerQuintal)
                appendLine("<tr>")
                appendLine("<td class=\"text-center\">1</td>")
                appendLine("<td><b>${escapeHtml(entry.commodityId)}</b></td>")
                appendLine("<td>${escapeHtml(eq)}</td>")
                appendLine("<td class=\"text-right nowrap\">₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}</td>")
                appendLine("</tr>")
            }
            appendLine("</tbody>")
            appendLine("</table>")

            // Summary Table
            appendLine("<table class=\"summary-table\">")
            appendLine("<tr>")
            appendLine("<td>कुल असल राशि (Gross):</td>")
            appendLine("<td class=\"text-right\">₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}</td>")
            appendLine("</tr>")

            if (amounts.commissionPaisa > 0L) {
                appendLine("<tr>")
                appendLine("<td>कमीशन जोड़:</td>")
                appendLine("<td class=\"text-right\">+ ₹${MandiMathEngine.paisaToRupeesString(amounts.commissionPaisa)}</td>")
                appendLine("</tr>")
            }

            if (amounts.mandiShulkPaisa > 0L) {
                appendLine("<tr>")
                appendLine("<td>मंडी शुल्क एवं उपकर:</td>")
                appendLine("<td class=\"text-right\">+ ₹${MandiMathEngine.paisaToRupeesString(amounts.mandiShulkPaisa)}</td>")
                appendLine("</tr>")
            }

            if (amounts.tulakPaisa > 0L) {
                appendLine("<tr>")
                appendLine("<td>तुलाई:</td>")
                appendLine("<td class=\"text-right\">+ ₹${MandiMathEngine.paisaToRupeesString(amounts.tulakPaisa)}</td>")
                appendLine("</tr>")
            }

            if (amounts.labourPaisa > 0L) {
                appendLine("<tr>")
                appendLine("<td>मजदूरी:</td>")
                appendLine("<td class=\"text-right\">+ ₹${MandiMathEngine.paisaToRupeesString(amounts.labourPaisa)}</td>")
                appendLine("</tr>")
            }

            val kharchaPaisa = amounts.commissionPaisa + amounts.labourPaisa
            if (kharchaPaisa > 0L) {
                appendLine("<tr>")
                appendLine("<td>कुल खर्चा (आढ़त + मजदूरी):</td>")
                appendLine("<td class=\"text-right\">₹${MandiMathEngine.paisaToRupeesString(kharchaPaisa)}</td>")
                appendLine("</tr>")
            }

            appendLine("<tr class=\"grand-total\">")
            appendLine("<td>कुल देय राशि (Total Receivable):</td>")
            appendLine("<td class=\"text-right\">${MandiMathEngine.formatRupees(entry.totalReceivablePaisa)}</td>")
            appendLine("</tr>")

            if (entry.receivedPaisa > 0L) {
                appendLine("<tr>")
                appendLine("<td>प्राप्त राशि (Received):</td>")
                appendLine("<td class=\"text-right\">- ₹${MandiMathEngine.paisaToRupeesString(entry.receivedPaisa)}</td>")
                appendLine("</tr>")
                appendLine("<tr class=\"grand-total\">")
                appendLine("<td>बाकी देय (Balance Due):</td>")
                appendLine("<td class=\"text-right\">${MandiMathEngine.formatRupees(entry.balanceDuePaisa)}</td>")
                appendLine("</tr>")
            }

            appendLine("</table>")

            // Signatures
            appendLine("<div class=\"footer\">")
            appendLine("<div class=\"sign-box\">क्रेता हस्ताक्षर</div>")
            appendLine("<div class=\"sign-box\">विक्रेता / आढ़तिया हस्ताक्षर</div>")
            appendLine("</div>")

            appendLine("</body>")
            appendLine("</html>")
        }
    }

    fun renderDealReceiptHtml(
        deal: Deal,
        shopProfile: ShopProfile,
        farmer: Party,
        buyer: Party?
    ): String {
        val rateStr = deal.ratePaisaPerUnit?.let { "₹${MandiMathEngine.paisaToRupeesString(it)} / क्विंटल" } ?: "बाजार भाव"
        val grossQ = slipQuintalText(deal.grossWeightGrams)
        val netQ = slipQuintalText(deal.netWeightGrams)
        val labourRs = MandiMathEngine.paisaToRupeesString(deal.labourChargePaisa)
        val commRs = MandiMathEngine.paisaToRupeesString(deal.farmerCommissionPaisa)

        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"hi\">")
            appendLine("<head>")
            appendLine("<meta charset=\"UTF-8\">")
            appendLine("<title>पक्का सौदा पर्चा - #${deal.id.takeLast(6).uppercase()}</title>")
            appendLine(renderStyles())
            appendLine("</head>")
            appendLine("<body>")

            // Header
            appendLine("<div class=\"header\">")
            appendLine("<div class=\"shop-name\">${escapeHtml(shopProfile.shopName)}</div>")
            val sub = listOfNotNull(shopProfile.mandiName.takeIf { it.isNotBlank() }, shopProfile.address?.takeIf { it.isNotBlank() }).joinToString(", ")
            if (sub.isNotBlank()) {
                appendLine("<div class=\"shop-sub\">${escapeHtml(sub)}</div>")
            }
            val taxLines = listOfNotNull(
                shopProfile.gstin?.takeIf { it.isNotBlank() }?.let { "GSTIN: $it" },
                shopProfile.licenceNo?.takeIf { it.isNotBlank() }?.let { "लाइसेंस नं.: $it" },
                shopProfile.phoneNumber.takeIf { it.isNotBlank() }?.let { "मो.: $it" }
            ).joinToString(" | ")
            if (taxLines.isNotBlank()) {
                appendLine("<div class=\"shop-sub\">${escapeHtml(taxLines)}</div>")
            }
            appendLine("<div class=\"doc-title\">पक्का सौदा पर्चा (Settlement Slip)</div>")
            appendLine("</div>")

            // Meta Info
            appendLine("<table class=\"meta-table\">")
            appendLine("<tr>")
            appendLine("<td class=\"meta-label\">सौदा सं.:</td>")
            appendLine("<td class=\"meta-val\">#${escapeHtml(deal.id.takeLast(6).uppercase())}</td>")
            appendLine("<td class=\"meta-label\">दिनांक:</td>")
            appendLine("<td class=\"meta-val text-right\">${escapeHtml(formatDateTime(deal.dealDate))}</td>")
            appendLine("</tr>")
            appendLine("<tr>")
            appendLine("<td class=\"meta-label\">किसान:</td>")
            appendLine("<td class=\"meta-val\">${escapeHtml(farmer.name)} (${escapeHtml(farmer.village ?: "-")})</td>")
            appendLine("<td class=\"meta-label\">व्यापारी:</td>")
            appendLine("<td class=\"meta-val text-right\">${escapeHtml(buyer?.name ?: "-")}</td>")
            appendLine("</tr>")
            val dealPhones = listOfNotNull(
                farmer.phone?.takeIf { it.isNotBlank() }?.let { "किसान: $it" },
                buyer?.phone?.takeIf { it.isNotBlank() }?.let { "व्यापारी: $it" }
            ).joinToString(" | ")
            if (dealPhones.isNotBlank()) {
                appendLine("<tr>")
                appendLine("<td class=\"meta-label\">मोबाइल:</td>")
                appendLine("<td class=\"meta-val\" colspan=\"3\">${escapeHtml(dealPhones)}</td>")
                appendLine("</tr>")
            }
            appendLine("</table>")

            // Items Table
            appendLine("<table class=\"items-table\">")
            appendLine("<thead>")
            appendLine("<tr>")
            appendLine("<th>फसल विवरण</th>")
            appendLine("<th class=\"text-center\">बोरी</th>")
            appendLine("<th class=\"text-center\">कुल वजन</th>")
            appendLine("<th class=\"text-center\">शुद्ध वजन</th>")
            appendLine("<th class=\"text-right\">भाव</th>")
            appendLine("<th class=\"text-right\">कुल रकम (₹)</th>")
            appendLine("</tr>")
            appendLine("</thead>")
            appendLine("<tbody>")
            appendLine("<tr>")
            appendLine("<td>कृषि उपज</td>")
            appendLine("<td class=\"text-center\">${deal.bagsCount}</td>")
            appendLine("<td class=\"text-center\">$grossQ क्विंटल</td>")
            appendLine("<td class=\"text-center\">$netQ क्विंटल</td>")
            appendLine("<td class=\"text-right\">$rateStr</td>")
            appendLine("<td class=\"text-right nowrap\">₹${MandiMathEngine.paisaToRupeesString(deal.grossAmountPaisa)}</td>")
            appendLine("</tr>")
            appendLine("</tbody>")
            appendLine("</table>")

            // Summary Table
            appendLine("<table class=\"summary-table\">")
            appendLine("<tr>")
            appendLine("<td>कुल रकम (Gross):</td>")
            appendLine("<td class=\"text-right\">₹${MandiMathEngine.paisaToRupeesString(deal.grossAmountPaisa)}</td>")
            appendLine("</tr>")
            appendLine("<tr>")
            appendLine("<td>पल्लेदारी / मजदूरी:</td>")
            appendLine("<td class=\"text-right\">- ₹$labourRs</td>")
            appendLine("</tr>")
            appendLine("<tr>")
            appendLine("<td>मंडी आढ़त (कमीशन):</td>")
            appendLine("<td class=\"text-right\">- ₹$commRs</td>")
            appendLine("</tr>")
            if (deal.buyerCommissionPaisa > 0L) {
                appendLine("<tr>")
                appendLine("<td>व्यापारी आढ़त:</td>")
                appendLine("<td class=\"text-right\">+ ₹${MandiMathEngine.paisaToRupeesString(deal.buyerCommissionPaisa)}</td>")
                appendLine("</tr>")
            }
            appendLine("<tr class=\"grand-total\">")
            appendLine("<td>किसान शुद्ध देय (Net Payable):</td>")
            appendLine("<td class=\"text-right\">${MandiMathEngine.formatRupees(deal.netFarmerPayablePaisa)}</td>")
            appendLine("</tr>")
            appendLine("</table>")

            // Signatures
            appendLine("<div class=\"footer\">")
            appendLine("<div class=\"sign-box\">किसान हस्ताक्षर</div>")
            appendLine("<div class=\"sign-box\">आढ़तिया हस्ताक्षर</div>")
            appendLine("</div>")

            appendLine("</body>")
            appendLine("</html>")
        }
    }
}
