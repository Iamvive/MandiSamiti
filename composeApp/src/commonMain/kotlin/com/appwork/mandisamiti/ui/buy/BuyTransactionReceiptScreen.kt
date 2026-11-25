package com.appwork.mandisamiti.ui.buy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.ui.slip.slipEquation
import com.appwork.mandisamiti.domain.model.BuyCropLot
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.receiptDateShort
import com.appwork.mandisamiti.domain.model.receiptDateTime
import com.appwork.mandisamiti.domain.model.slipLabel
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.components.DeleteConfirmDialog
import com.appwork.mandisamiti.ui.components.MandiLinesCard
import com.appwork.mandisamiti.ui.components.MandiPrimaryButton
import com.appwork.mandisamiti.ui.components.MandiScreenHeader
import com.appwork.mandisamiti.ui.components.MandiSecondaryButton
import com.appwork.mandisamiti.ui.parchi.FarmerSlipCashSettlementSheet
import com.appwork.mandisamiti.ui.slip.ReceiptHtmlRenderer
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiRedText
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
    onDelete: (() -> Unit)? = null,
    onNewBuy: (() -> Unit)? = null,
    entry: BuyEntry? = null,
    farmerName: String = "किसान",
    shopName: String = "",
    mandiName: String = "",
    shopAddress: String? = null,
    shopGstin: String? = null,
    shopLicenceNo: String? = null,
    partyVillage: String? = null,
    onShareWhatsApp: ((String) -> Unit)? = null,
    onPrint: ((String) -> Unit)? = null,
    isEnglish: Boolean = false,
    gallaCashPaisa: Long = 0L,
    onSettleCash: ((amountPaisa: Long) -> Unit)? = null,
    payments: List<CashTransaction> = emptyList(),
    modifier: Modifier = Modifier
) {
    MandiBackHandler { onBack() }

    var showSettlementSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val displayName = farmerName.takeIf { it.isNotBlank() && it != "किसान" }
        ?: entry?.farmerId?.takeIf { it.isNotBlank() }
        ?: if (isEnglish) "Farmer" else "किसान"
    val lots = entry?.let { extractLots(it) } ?: emptyList()
    val slipText = entry?.let { buildFarmerSlipText(it, displayName, lots, shopName, mandiName, shopAddress, shopGstin, shopLicenceNo, payments) } ?: ""
    val slipHtml = entry?.let {
        val tempShop = ShopProfile(
            id = "",
            shopName = shopName,
            ownerName = "",
            mandiName = mandiName,
            phoneNumber = "",
            pinHash = "",
            createdAt = 0L,
            updatedAt = 0L,
            address = shopAddress,
            gstin = shopGstin,
            licenceNo = shopLicenceNo
        )
        ReceiptHtmlRenderer.renderBuyReceiptHtml(it, tempShop, displayName, lots, payments, partyPhone = buyFarmerPhone(it))
    } ?: ""

    val slipNum = entry?.let { slipLabel(it.slipNo, it.id) } ?: ""

    val loanCutPaisa = extractLoanCut(entry?.remarks)
    val hasAdvance = (entry?.advancePaidPaisa ?: 0L) > 0L
    val hasLoanCut = loanCutPaisa > 0L
    val postDeductionPayable = if (entry != null) {
        BuyPaymentCalculations.calculatePostDeductionPayable(
            entry.netPayablePaisa,
            entry.advancePaidPaisa,
            loanCutPaisa
        )
    } else 0L

    val cashPaymentPaisa = if (entry != null) {
        BuyPaymentCalculations.calculateCashPayment(
            entry.paidPaisa,
            entry.advancePaidPaisa,
            loanCutPaisa
        )
    } else 0L
    val leftPaisa = BuyPaymentCalculations.calculateRemainingLeft(postDeductionPayable, cashPaymentPaisa)

    if (showSettlementSheet && entry != null) {
        FarmerSlipCashSettlementSheet(
            buy = entry,
            farmerName = displayName,
            gallaCashPaisa = gallaCashPaisa,
            isEnglish = isEnglish,
            onDismiss = { showSettlementSheet = false },
            onConfirmPayment = { amountPaisa ->
                onSettleCash?.invoke(amountPaisa)
                showSettlementSheet = false
            }
        )
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            isEnglish = isEnglish,
            onConfirm = {
                showDeleteConfirm = false
                onDelete?.invoke()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    Scaffold(
        topBar = {
            MandiScreenHeader(
                title = if (isEnglish) "Purchase slip #$slipNum" else "खरीद पर्ची #$slipNum",
                subtitle = buyFarmerPhone(entry)?.let { "$displayName  |  $it" } ?: displayName,
                onBack = onBack,
                actions = {
                    if (entry != null && !entry.isVoid && onDelete != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = if (isEnglish) "Delete slip" else "पर्ची हटाएं",
                                tint = MandiRedText
                            )
                        }
                    }
                }
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
                val hasPendingDue = entry != null && leftPaisa > 0L && onSettleCash != null

                if (hasPendingDue) {
                    MandiPrimaryButton(
                        text = if (isEnglish) "Pay Cash from Galla →" else "गल्ला से नकद भुगतान करें →",
                        height = 52.dp,
                        fontSize = 15.sp,
                        onClick = { showSettlementSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onPrint != null) {
                            MandiSecondaryButton(
                                text = if (isEnglish) "Print" else "प्रिंट करें",
                                leadingIcon = Icons.Default.Print,
                                height = 44.dp,
                                fontSize = 13.sp,
                                fullWidth = false,
                                onClick = { onPrint(slipHtml) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (onShareWhatsApp != null) {
                            MandiSecondaryButton(
                                text = if (isEnglish) "Share" else "पर्ची भेजें",
                                leadingIcon = Icons.Default.Share,
                                height = 44.dp,
                                fontSize = 13.sp,
                                fullWidth = false,
                                onClick = { onShareWhatsApp(slipText) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        MandiSecondaryButton(
                            text = if (isEnglish) "New" else "नई खरीद",
                            height = 44.dp,
                            fontSize = 13.sp,
                            fullWidth = false,
                            onClick = { onNewBuy?.invoke() ?: onBack() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    if (entry != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onPrint != null) {
                                MandiSecondaryButton(
                                    text = if (isEnglish) "Print" else "प्रिंट करें",
                                    leadingIcon = Icons.Default.Print,
                                    height = 56.dp,
                                    fontSize = 15.sp,
                                    fullWidth = false,
                                    onClick = { onPrint(slipHtml) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (onShareWhatsApp != null) {
                                MandiPrimaryButton(
                                    text = if (isEnglish) "Share slip" else "पर्ची भेजें",
                                    leadingIcon = Icons.Default.Share,
                                    height = 56.dp,
                                    fontSize = 15.sp,
                                    onClick = { onShareWhatsApp(slipText) },
                                    modifier = Modifier.weight(if (onPrint != null) 1f else 2f)
                                )
                            }
                        }
                    }

                    MandiSecondaryButton(
                        text = if (isEnglish) "New purchase" else "नई खरीद",
                        height = 48.dp,
                        fontSize = 14.sp,
                        onClick = { onNewBuy?.invoke() ?: onBack() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
            // Shop header if present
            val shopHeader = listOfNotNull(
                shopName.takeIf { it.isNotBlank() },
                mandiName.takeIf { it.isNotBlank() },
                shopAddress?.takeIf { it.isNotBlank() }
            ).joinToString(" • ")

            if (shopHeader.isNotBlank() || !shopGstin.isNullOrBlank() || !shopLicenceNo.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (shopHeader.isNotBlank()) {
                        Text(
                            text = shopHeader,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                    }
                    val taxLines = listOfNotNull(
                        shopGstin?.takeIf { it.isNotBlank() }?.let { "GSTIN: $it" },
                        shopLicenceNo?.takeIf { it.isNotBlank() }?.let { if (isEnglish) "Licence: $it" else "लाइसेंस: $it" }
                    ).joinToString("  |  ")
                    if (taxLines.isNotBlank()) {
                        Text(
                            text = taxLines,
                            fontSize = 11.sp,
                            color = MandiTextSecondary
                        )
                    }
                }
            }

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
                            label = "${if (isEnglish) "Lot" else "फसल"} #${idx + 1}: ${lot.cropName}\n${slipEquation(lot.bags, lot.weightPerBagGrams, lot.totalWeightGrams, lot.ratePaisaPerQuintal)}",
                            amountText = MandiMathEngine.formatRupees(lot.grossPaisa)
                        )
                    }
                } else {
                    Line(
                        label = "${if (isEnglish) "Crop" else "फसल"}: ${entry.commodityId}\n${slipEquation(entry.bagsCount, entry.weightPerBagGrams, entry.totalWeightGrams, entry.ratePaisaPerQuintal)}",
                        amountText = MandiMathEngine.formatRupees(entry.grossPaisa)
                    )
                }

                Line(
                    label = if (isEnglish) "Total weight" else "कुल वजन",
                    amountText = "${formatQuintals2(entry.totalWeightGrams)} ${if (isEnglish) "Qtl" else "क्विंटल"}"
                )

                // Labour strictly above Commission
                Line(
                    label = if (isEnglish) "Labour" else "मजदूरी कटौती",
                    amountText = "− ₹${MandiMathEngine.paisaToRupeesString(entry.labourPaisa)}"
                )
                Line(
                    label = if (isEnglish) "Commission" else "कमीशन कटौती",
                    amountText = "− ₹${MandiMathEngine.paisaToRupeesString(entry.commissionPaisa)}"
                )
                Line(
                    label = if (isEnglish) "Total charges" else "कुल खर्चा",
                    amountText = "− ₹${MandiMathEngine.paisaToRupeesString(entry.labourPaisa + entry.commissionPaisa)}"
                )

                TotalLine(
                    label = if (isEnglish) "Slip value" else "पर्ची राशि",
                    amountText = MandiMathEngine.formatRupees(entry.netPayablePaisa),
                    largeAmount = true,
                    isEmphasized = true
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

                if (cashPaymentPaisa > 0L || payments.isNotEmpty()) {
                    if (payments.isNotEmpty()) {
                        payments.forEach { p ->
                            Line(
                                label = if (isEnglish) "• Cash payment · ${receiptDateShort(p.transactionDate)}" else "• नकद भुगतान · ${receiptDateShort(p.transactionDate)}",
                                amountText = "− ₹${MandiMathEngine.paisaToRupeesString(p.amountPaisa)}"
                            )
                        }
                    } else {
                        Line(
                            label = if (isEnglish) "• Cash payment · ${receiptDateShort(entry.updatedAt)}" else "• नकद भुगतान · ${receiptDateShort(entry.updatedAt)}",
                            amountText = "− ₹${MandiMathEngine.paisaToRupeesString(cashPaymentPaisa)}"
                        )
                    }
                    if (leftPaisa > 0L) {
                        TotalLine(
                            label = if (isEnglish) "Left" else "बाकी",
                            amountText = MandiMathEngine.formatRupees(leftPaisa),
                            largeAmount = true
                        )
                    } else {
                        TotalLine(
                            label = if (isEnglish) "Status" else "स्थिति",
                            amountText = if (isEnglish) "Fully Settled" else "पूर्ण चुकता",
                            largeAmount = true
                        )
                    }
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

/** Kisan mobile of a buy slip. The entry stores it in vehicleNumber (non-khaate); null when absent. */
fun buyFarmerPhone(entry: BuyEntry?): String? =
    entry?.takeIf { !it.isKhaate }?.vehicleNumber?.trim()?.takeIf { it.isNotEmpty() }

/** Grams to quintals with exactly 2 decimals (half-up), e.g. 1_250_000 -> "12.50". */
fun formatQuintals2(grams: Long): String {
    val hundredths = (grams + 500L) / 1000L
    return "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}"
}

fun buildFarmerSlipText(
    entry: BuyEntry,
    farmerName: String,
    lots: List<BuyCropLot> = emptyList(),
    shopName: String = "",
    mandiName: String = "",
    shopAddress: String? = null,
    shopGstin: String? = null,
    shopLicenceNo: String? = null,
    payments: List<CashTransaction> = emptyList()
): String = buildString {
    val isRevised = entry.revision > 1 || entry.paidPaisa > 0
    val headerTitle = if (isRevised) "*किसान खरीद पर्चा (संशोधित)*" else "*किसान खरीद पर्चा*"
    appendLine(headerTitle)
    val shopHeader = listOfNotNull(
        shopName.takeIf { it.isNotBlank() },
        mandiName.takeIf { it.isNotBlank() },
        shopAddress?.takeIf { it.isNotBlank() }
    ).joinToString(" • ")
    if (shopHeader.isNotBlank()) {
        appendLine(shopHeader)
    }
    if (!shopGstin.isNullOrBlank()) {
        appendLine("GSTIN: $shopGstin")
    }
    if (!shopLicenceNo.isNullOrBlank()) {
        appendLine("लाइसेंस नं.: $shopLicenceNo")
    }
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
    buyFarmerPhone(entry)?.let { appendLine("मोबाइल: $it") }
    appendLine("--------------------")
    if (lots.isNotEmpty()) {
        lots.forEachIndexed { i, lot ->
            appendLine("फसल ${i + 1}: ${lot.cropName}")
            appendLine(slipEquation(lot.bags, lot.weightPerBagGrams, lot.totalWeightGrams, lot.ratePaisaPerQuintal))
            appendLine("रकम: ₹${MandiMathEngine.paisaToRupeesString(lot.grossPaisa)}")
            appendLine()
        }
        appendLine("--------------------")
    } else {
        appendLine(slipEquation(entry.bagsCount, entry.weightPerBagGrams, entry.totalWeightGrams, entry.ratePaisaPerQuintal))
        appendLine("रकम: ₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}")
        appendLine("--------------------")
    }
    appendLine("कुल राशि: ₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}")
    appendLine("--------------------")
    appendLine("*पर्ची राशि: ₹${MandiMathEngine.paisaToRupeesString(entry.netPayablePaisa)}*")
    appendLine("--------------------")
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
        appendLine("--------------------")
    }
    val cashPaid = BuyPaymentCalculations.calculateCashPayment(entry.paidPaisa, entry.advancePaidPaisa, loanCutPaisa)
    if (payments.isNotEmpty()) {
        payments.forEach { p ->
            appendLine("• नकद भुगतान · ${receiptDateShort(p.transactionDate)}: -₹${MandiMathEngine.paisaToRupeesString(p.amountPaisa)}")
        }
        appendLine("--------------------")
        val left = BuyPaymentCalculations.calculateRemainingLeft(postDeduction, cashPaid)
        if (left > 0) {
            appendLine("*बाकी राशि: ${MandiMathEngine.formatRupees(left)}*")
        } else {
            appendLine("*स्थिति: पूर्ण चुकता*")
        }
    } else if (cashPaid > 0) {
        val left = BuyPaymentCalculations.calculateRemainingLeft(postDeduction, cashPaid)
        appendLine("• नकद भुगतान · ${receiptDateShort(entry.updatedAt)}: -₹${MandiMathEngine.paisaToRupeesString(cashPaid)}")
        appendLine("--------------------")
        if (left > 0) {
            appendLine("*बाकी राशि: ${MandiMathEngine.formatRupees(left)}*")
        } else {
            appendLine("*स्थिति: पूर्ण चुकता*")
        }
    }
}
