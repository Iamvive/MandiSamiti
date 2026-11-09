package com.appwork.mandisamiti.ui.buy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyCropLot
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.receiptDateShort
import com.appwork.mandisamiti.domain.model.receiptDateTime
import com.appwork.mandisamiti.domain.model.slipLabel
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.components.MandiLinesCard
import com.appwork.mandisamiti.ui.components.MandiPrimaryButton
import com.appwork.mandisamiti.ui.components.MandiScreenHeader
import com.appwork.mandisamiti.ui.components.MandiSecondaryButton
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import kotlinx.serialization.json.Json

/**
 * Direction B final Purchase Slip (खरीद पर्ची).
 * Matches B final billBody & fBuyBill:
 * - Header: "खरीद पर्ची #<no>", Subtitle: farmer name.
 * - Indicator above card: "संशोधित · <date, time>" or "मूल पर्ची · <date, time>" (f12 w6).
 * - Card with lines:
 *   - Lots lines
 *   - Tare (if present on legacy slip)
 *   - Labour ("मजदूरी कटौती") above Commission ("कमीशन कटौती")
 *   - Slip value ("पर्ची राशि")
 *   - Advance ("अग्रिम / पेशगी") if any
 *   - Loan cut ("उधार कटौती") if any
 *   - Final payable ("अंतिम देय राशि") if advance or cut present
 *   - Cash payment ("नकद भुगतान · <date>") if paid
 *   - Left ("बाकी") if paid
 * - Bottom Bar:
 *   - Primary tall button: "पर्ची भेजें" with share icon (56dp).
 *   - Secondary button: "नई खरीद" (48dp).
 */
@Composable
fun BuyTransactionReceiptScreen(
    id: String,
    onBack: () -> Unit,
    onEdit: ((String) -> Unit)? = null,
    onNewBuy: (() -> Unit)? = null,
    entry: BuyEntry? = null,
    farmerName: String = "किसान",
    shopName: String = "",
    mandiName: String = "",
    partyVillage: String? = null,
    onShareWhatsApp: ((String) -> Unit)? = null,
    isEnglish: Boolean = false,
    modifier: Modifier = Modifier
) {
    MandiBackHandler { onBack() }

    val displayName = farmerName.takeIf { it.isNotBlank() && it != "किसान" }
        ?: entry?.farmerId?.takeIf { it.isNotBlank() }
        ?: if (isEnglish) "Farmer" else "किसान"
    val lots = entry?.let { extractLots(it) } ?: emptyList()
    val slipText = entry?.let { buildFarmerSlipText(it, displayName, lots) } ?: ""

    val slipNum = entry?.let { slipLabel(it.slipNo, it.id) } ?: ""

    Scaffold(
        topBar = {
            MandiScreenHeader(
                title = if (isEnglish) "Purchase slip #$slipNum" else "खरीद पर्ची #$slipNum",
                subtitle = displayName,
                onBack = onBack
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (entry != null && onShareWhatsApp != null) {
                    MandiPrimaryButton(
                        text = if (isEnglish) "Share slip" else "पर्ची भेजें",
                        leadingIcon = Icons.Default.Share,
                        height = 56.dp,
                        fontSize = 16.sp,
                        onClick = { onShareWhatsApp(slipText) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                MandiSecondaryButton(
                    text = if (isEnglish) "New purchase" else "नई खरीद",
                    height = 48.dp,
                    fontSize = 14.sp,
                    onClick = { onNewBuy?.invoke() ?: onBack() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        modifier = modifier.fillMaxSize().background(MandiBackground)
    ) { padding ->
        if (entry == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
            ) {
                Text(
                    text = if (isEnglish) "Loading slip..." else "पर्ची लोड हो रही है...",
                    color = MandiTextSecondary
                )
            }
            return@Scaffold
        }

        val isRevised = entry.revision > 1 || entry.paidPaisa > 0
        val revisionText = if (isRevised) {
            if (isEnglish) "Revised · ${receiptDateTime(entry.updatedAt)}" else "संशोधित · ${receiptDateTime(entry.updatedAt)}"
        } else {
            if (isEnglish) "Original slip · ${receiptDateTime(entry.createdAt)}" else "मूल पर्ची · ${receiptDateTime(entry.createdAt)}"
        }

        val loanCutPaisa = extractLoanCut(entry.remarks)
        val hasAdvance = entry.advancePaidPaisa > 0L
        val hasLoanCut = loanCutPaisa > 0L
        val postDeductionPayable = BuyPaymentCalculations.calculatePostDeductionPayable(
            entry.netPayablePaisa,
            entry.advancePaidPaisa,
            loanCutPaisa
        )

        val cashPaymentPaisa = BuyPaymentCalculations.calculateCashPayment(
            entry.paidPaisa,
            entry.advancePaidPaisa,
            loanCutPaisa
        )
        val leftPaisa = BuyPaymentCalculations.calculateRemainingLeft(postDeductionPayable, cashPaymentPaisa)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Revision / Original indicator (f12 w6)
            Text(
                text = revisionText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MandiTextPrimary
            )

            // Bill lines card
            MandiLinesCard {
                if (lots.isNotEmpty()) {
                    lots.forEachIndexed { idx, lot ->
                        Line(
                            label = if (isEnglish) "Lot #${idx + 1}: ${lot.cropName}, ${lot.bags} bags" else "फसल #${idx + 1}: ${lot.cropName}, ${lot.bags} बोरी",
                            amountText = MandiMathEngine.formatRupees(lot.grossPaisa)
                        )
                    }
                } else {
                    Line(
                        label = if (isEnglish) "Crop: ${entry.commodityId}, ${entry.bagsCount} bags" else "फसल: ${entry.commodityId}, ${entry.bagsCount} बोरी",
                        amountText = MandiMathEngine.formatRupees(entry.grossPaisa)
                    )
                }

                // Labour strictly above Commission
                Line(
                    label = if (isEnglish) "Labour" else "मजदूरी कटौती",
                    amountText = "− ₹${MandiMathEngine.paisaToRupeesString(entry.labourPaisa)}"
                )
                Line(
                    label = if (isEnglish) "Commission" else "कमीशन कटौती",
                    amountText = "− ₹${MandiMathEngine.paisaToRupeesString(entry.commissionPaisa)}"
                )

                TotalLine(
                    label = if (isEnglish) "Slip value" else "पर्ची राशि",
                    amountText = MandiMathEngine.formatRupees(entry.netPayablePaisa),
                    largeAmount = !hasAdvance && !hasLoanCut && cashPaymentPaisa == 0L
                )

                if (hasAdvance) {
                    Line(
                        label = if (isEnglish) "Advance" else "अग्रिम / पेशगी",
                        amountText = "− ₹${MandiMathEngine.paisaToRupeesString(entry.advancePaidPaisa)}"
                    )
                }

                if (hasLoanCut) {
                    Line(
                        label = if (isEnglish) "Loan cut" else "उधार कटौती",
                        amountText = "− ₹${MandiMathEngine.paisaToRupeesString(loanCutPaisa)}"
                    )
                }

                if (hasAdvance || hasLoanCut) {
                    TotalLine(
                        label = if (isEnglish) "Final payable" else "अंतिम देय राशि",
                        amountText = MandiMathEngine.formatRupees(postDeductionPayable),
                        largeAmount = cashPaymentPaisa == 0L
                    )
                }

                if (cashPaymentPaisa > 0L) {
                    Line(
                        label = if (isEnglish) "Cash paid · ${receiptDateShort(entry.updatedAt)}" else "नकद भुगतान · ${receiptDateShort(entry.updatedAt)}",
                        amountText = "− ₹${MandiMathEngine.paisaToRupeesString(cashPaymentPaisa)}"
                    )
                    TotalLine(
                        label = if (isEnglish) "Left" else "बाकी",
                        amountText = MandiMathEngine.formatRupees(leftPaisa),
                        largeAmount = true
                    )
                }
            }
        }
    }
}

private fun extractLots(entry: BuyEntry): List<BuyCropLot> {
    val remarks = entry.remarks ?: return emptyList()
    if (!remarks.contains("DATA: ")) return emptyList()
    return try {
        val jsonStr = remarks.substringAfter("DATA: ").trim()
        Json.decodeFromString<List<BuyCropLot>>(jsonStr)
    } catch (_: Exception) {
        emptyList()
    }
}

fun buildFarmerSlipText(entry: BuyEntry, farmerName: String, lots: List<BuyCropLot> = emptyList()): String = buildString {
    val isRevised = entry.revision > 1 || entry.paidPaisa > 0
    val headerTitle = if (isRevised) "*किसान खरीद पर्चा (संशोधित)*" else "*किसान खरीद पर्चा*"
    appendLine(headerTitle)
    appendLine("पर्चा #: ${slipLabel(entry.slipNo, entry.id)}")
    if (isRevised) {
        appendLine("संशोधन समय: ${receiptDateTime(entry.updatedAt)}")
    } else {
        appendLine("दिनांक: ${receiptDateTime(entry.createdAt)}")
    }
    appendLine("किसान: $farmerName")
    val village = extractVillage(entry.remarks)
    if (!village.isNullOrBlank()) {
        appendLine("गाँव: $village")
    }
    if (!entry.vehicleNumber.isNullOrBlank()) {
        appendLine("मोबाइल: ${entry.vehicleNumber}")
    }
    appendLine("--------------------")
    if (lots.isNotEmpty()) {
        lots.forEachIndexed { i, lot ->
            appendLine("फसल ${i + 1}: ${lot.cropName}")
            appendLine("${lot.bags} बोरी | ${MandiMathEngine.gramsToQuintals(lot.totalWeightGrams)} क्विंटल @ ₹${MandiMathEngine.paisaToRupeesString(lot.ratePaisaPerQuintal)} = ₹${MandiMathEngine.paisaToRupeesString(lot.grossPaisa)}")
            appendLine()
        }
        appendLine("--------------------")
    } else {
        appendLine("बोरी: ${entry.bagsCount}")
        appendLine("वजन: ${MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)} क्विंटल")
        appendLine("भाव: ₹${MandiMathEngine.paisaToRupeesString(entry.ratePaisaPerQuintal)}/क्विंटल")
        appendLine("--------------------")
    }
    appendLine("कुल राशि: ₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}")
    appendLine("मजदूरी: -₹${MandiMathEngine.paisaToRupeesString(entry.labourPaisa)}")
    appendLine("कमीशन: -₹${MandiMathEngine.paisaToRupeesString(entry.commissionPaisa)}")
    if (entry.advancePaidPaisa > 0) {
        appendLine("अग्रिम: -₹${MandiMathEngine.paisaToRupeesString(entry.advancePaidPaisa)}")
    }
    val loanCutPaisa = extractLoanCut(entry.remarks)
    if (loanCutPaisa > 0) {
        appendLine("उधार कटौती: -₹${MandiMathEngine.paisaToRupeesString(loanCutPaisa)}")
    }
    val postDeduction = BuyPaymentCalculations.calculatePostDeductionPayable(entry.netPayablePaisa, entry.advancePaidPaisa, loanCutPaisa)
    if (entry.advancePaidPaisa > 0 || loanCutPaisa > 0) {
        appendLine("--------------------")
        appendLine("*अंतिम देय राशि: ${MandiMathEngine.formatRupees(postDeduction)}*")
    }
    val cashPaid = BuyPaymentCalculations.calculateCashPayment(entry.paidPaisa, entry.advancePaidPaisa, loanCutPaisa)
    if (cashPaid > 0) {
        val left = BuyPaymentCalculations.calculateRemainingLeft(postDeduction, cashPaid)
        appendLine("नकद भुगतान: -₹${MandiMathEngine.paisaToRupeesString(cashPaid)}")
        appendLine("*बाकी राशि: ${MandiMathEngine.formatRupees(left)}*")
    }
}
