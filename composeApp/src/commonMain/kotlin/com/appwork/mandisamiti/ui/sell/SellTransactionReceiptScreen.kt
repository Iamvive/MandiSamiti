package com.appwork.mandisamiti.ui.sell

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.math.sellSlipAmounts
import com.appwork.mandisamiti.ui.slip.slipEquation
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.SellCropLot
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.receiptDateTime
import com.appwork.mandisamiti.domain.model.slipLabel
import com.appwork.mandisamiti.ui.components.ReceiptAmountRow
import com.appwork.mandisamiti.ui.components.ReceiptCard
import com.appwork.mandisamiti.ui.components.ReceiptDivider
import com.appwork.mandisamiti.ui.components.ReceiptHero
import com.appwork.mandisamiti.ui.components.ReceiptLot
import com.appwork.mandisamiti.ui.components.ReceiptLotRow
import com.appwork.mandisamiti.ui.components.MandiPrimaryButton
import com.appwork.mandisamiti.ui.components.ReceiptScaffold
import com.appwork.mandisamiti.ui.parchi.TraderSlipCashSettlementSheet
import com.appwork.mandisamiti.ui.slip.ReceiptHtmlRenderer
import kotlinx.serialization.json.Json

@Composable
fun SellTransactionReceiptScreen(
    id: String,
    onBack: () -> Unit,
    onEdit: ((String) -> Unit)? = null,
    entry: SellEntry? = null,
    buyerName: String = "खरीदार",
    shopName: String = "",
    mandiName: String = "",
    shopAddress: String? = null,
    shopGstin: String? = null,
    shopLicenceNo: String? = null,
    partyVillage: String? = null,
    partyPhone: String? = null,
    onShareWhatsApp: ((String) -> Unit)? = null,
    onPrint: ((String) -> Unit)? = null,
    isEnglish: Boolean = false,
    gallaCashPaisa: Long = 0L,
    onSettleCash: ((amountPaisa: Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showSettlementSheet by remember { mutableStateOf(false) }
    val name = buyerName.takeIf { it.isNotBlank() && it != "खरीदार" } ?: entry?.buyerId?.ifBlank { "खरीदार" } ?: "खरीदार"
    val lots = try {
        entry?.remarks?.let { Json.decodeFromString<List<SellCropLot>>(it) }
    } catch (_: Exception) {
        null
    }
    val billText = entry?.let { buildSellBillText(it, buyerName, shopName, mandiName, shopAddress, shopGstin, shopLicenceNo, partyPhone) } ?: ""
    val billHtml = entry?.let {
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
        ReceiptHtmlRenderer.renderSellReceiptHtml(it, tempShop, name, lots ?: emptyList(), partyPhone = partyPhone)
    } ?: ""
    val isBos = entry?.billMode == BillMode.BILL_OF_SUPPLY
    ReceiptScaffold(
        title = when {
            isBos -> "सप्लाई बिल"
            entry?.partyKind == SellPartyKind.MERCHANT -> "व्यापारी बिल"
            else -> "ग्राहक बिल"
        },
        onBack = onBack,
        onEdit = if (entry != null && !entry.isVoid && onEdit != null) ({ onEdit(entry.id) }) else null,
        shareLabel = "व्हाट्सएप पर बिल भेजें",
        onShare = if (entry != null && onShareWhatsApp != null) ({ onShareWhatsApp(billText) }) else null,
        onPrint = if (entry != null && onPrint != null) ({ onPrint(billHtml) }) else null,
        printLabel = if (isEnglish) "Print bill" else "प्रिंट करें",
        modifier = modifier
    ) {
        if (entry == null) {
            Text("बिल लोड हो रहा है...")
            return@ReceiptScaffold
        }
        val meta = listOfNotNull(
            "#${slipLabel(entry.slipNo, entry.id)}",
            receiptDateTime(entry.entryDate),
            partyVillage?.takeIf { it.isNotBlank() },
            entry.vehicleNumber?.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
        val shop = listOfNotNull(
            shopName.takeIf { it.isNotBlank() },
            mandiName.takeIf { it.isNotBlank() },
            shopAddress?.takeIf { it.isNotBlank() }
        ).joinToString(", ")

        ReceiptCard {
            if (shop.isNotBlank()) Text(shop)
            if (!shopGstin.isNullOrBlank()) ReceiptAmountRow("दुकान GSTIN", shopGstin)
            if (!shopLicenceNo.isNullOrBlank()) ReceiptAmountRow(if (isEnglish) "Shop Licence" else "मंडी लाइसेंस", shopLicenceNo)
            ReceiptHero(
                partyName = name,
                meta = meta,
                amountLabel = "कुल देय राशि",
                amount = MandiMathEngine.formatRupees(entry.totalReceivablePaisa),
                isVoid = entry.isVoid
            )
            if (!entry.gstin.isNullOrBlank()) ReceiptAmountRow("GSTIN", entry.gstin!!)
            if (!partyPhone.isNullOrBlank()) ReceiptAmountRow(if (isEnglish) "Mobile" else "मोबाइल", partyPhone)

            if (!lots.isNullOrEmpty()) {
                lots.forEachIndexed { i, lot ->
                    ReceiptLotRow(
                        ReceiptLot(
                            title = "${i + 1}. ${lot.cropName}",
                            detail = slipEquation(lot.bags, lot.weightPerBagGrams, lot.totalWeightGrams, lot.ratePaisaPerQuintal),
                            amount = "₹${MandiMathEngine.paisaToRupeesString(lot.grossPaisa)}"
                        )
                    )
                }
            } else {
                ReceiptLotRow(
                    ReceiptLot(
                        title = entry.commodityId,
                        detail = slipEquation(entry.bagsCount, entry.weightPerBagGrams, entry.totalWeightGrams, entry.ratePaisaPerQuintal),
                        amount = "₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}"
                    )
                )
            }

            ReceiptDivider()
            ReceiptAmountRow("कुल (${entry.bagsCount} बोरी, ${formatQuintals2(entry.totalWeightGrams)} क्विंटल)", "₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}")
            val amounts = sellSlipAmounts(entry)
            if (amounts.mandiShulkPaisa > 0) ReceiptAmountRow("मंडी प्रभार एवं उपकर (2%)", "+ ₹${MandiMathEngine.paisaToRupeesString(amounts.mandiShulkPaisa)}")
            if (amounts.commissionPaisa > 0) ReceiptAmountRow("कमीशन", "+ ₹${MandiMathEngine.paisaToRupeesString(amounts.commissionPaisa)}")
            if (amounts.tulakPaisa > 0) ReceiptAmountRow("तुलाई", "+ ₹${MandiMathEngine.paisaToRupeesString(amounts.tulakPaisa)}")
            if (amounts.labourPaisa > 0) ReceiptAmountRow("मजदूरी", "+ ₹${MandiMathEngine.paisaToRupeesString(amounts.labourPaisa)}")
            val kharchaPaisa = amounts.commissionPaisa + amounts.labourPaisa
            if (kharchaPaisa > 0) ReceiptAmountRow(if (isEnglish) "Total kharcha (arat + labour)" else "कुल खर्चा (आढ़त + मजदूरी)", "₹${MandiMathEngine.paisaToRupeesString(kharchaPaisa)}")
            ReceiptDivider()
            ReceiptAmountRow("कुल देय राशि", MandiMathEngine.formatRupees(entry.totalReceivablePaisa), emphasized = true)
        }

        if (!entry.isVoid && entry.balanceDuePaisa > 0L && onSettleCash != null) {
            MandiPrimaryButton(
                text = if (isEnglish) "Cash in →" else "नकद लें →",
                onClick = { showSettlementSheet = true },
                height = 52.dp
            )
        }
        if (showSettlementSheet) {
            TraderSlipCashSettlementSheet(
                sell = entry,
                traderName = name,
                gallaCashPaisa = gallaCashPaisa,
                isEnglish = isEnglish,
                onDismiss = { showSettlementSheet = false },
                onConfirmReceipt = { amountPaisa ->
                    onSettleCash?.invoke(amountPaisa)
                    showSettlementSheet = false
                }
            )
        }
    }
}

fun buildSellBillText(
    entry: SellEntry,
    buyerName: String,
    shopName: String = "",
    mandiName: String = "",
    shopAddress: String? = null,
    shopGstin: String? = null,
    shopLicenceNo: String? = null,
    buyerPhone: String? = null
): String {
    val lots = try {
        entry.remarks?.let { Json.decodeFromString<List<SellCropLot>>(it) }
    } catch (_: Exception) {
        null
    }
    val isBos = entry.billMode == BillMode.BILL_OF_SUPPLY
    val title = if (isBos) "सप्लाई बिल (Bill of Supply)" else "बिक्री पर्चा (Mandi Sale Bill)"

    val shopHeader = listOfNotNull(
        shopName.takeIf { it.isNotBlank() },
        mandiName.takeIf { it.isNotBlank() },
        shopAddress?.takeIf { it.isNotBlank() }
    ).joinToString(", ")
    val shopLine = if (shopHeader.isNotBlank()) "विक्रेता: $shopHeader\n" else ""
    val shopGstinLine = if (!shopGstin.isNullOrBlank()) "विक्रेता GSTIN: $shopGstin\n" else ""
    val shopLicLine = if (!shopLicenceNo.isNullOrBlank()) "विक्रेता लाइसेंस: $shopLicenceNo\n" else ""

    val lotsText = if (!lots.isNullOrEmpty()) {
        lots.mapIndexed { i, lot ->
            "${i + 1}. ${lot.cropName}\n${slipEquation(lot.bags, lot.weightPerBagGrams, lot.totalWeightGrams, lot.ratePaisaPerQuintal)}\nरकम: ₹${MandiMathEngine.paisaToRupeesString(lot.grossPaisa)}"
        }.joinToString("\n")
    } else {
        "जिंस: ${entry.commodityId}\n${slipEquation(entry.bagsCount, entry.weightPerBagGrams, entry.totalWeightGrams, entry.ratePaisaPerQuintal)}\nरकम: ₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}"
    }

    val amounts = sellSlipAmounts(entry)
    val amountLines = buildString {
        if (amounts.commissionPaisa > 0) appendLine("कमीशन जोड़: +₹${MandiMathEngine.paisaToRupeesString(amounts.commissionPaisa)}")
        if (amounts.mandiShulkPaisa > 0) appendLine("मंडी शुल्क एवं उपकर: +₹${MandiMathEngine.paisaToRupeesString(amounts.mandiShulkPaisa)}")
        if (amounts.tulakPaisa > 0) appendLine("तुलाई: +₹${MandiMathEngine.paisaToRupeesString(amounts.tulakPaisa)}")
        if (amounts.labourPaisa > 0) appendLine("मजदूरी: +₹${MandiMathEngine.paisaToRupeesString(amounts.labourPaisa)}")
        val kharchaPaisa = amounts.commissionPaisa + amounts.labourPaisa
        if (kharchaPaisa > 0) appendLine("कुल खर्चा (आढ़त + मजदूरी): ₹${MandiMathEngine.paisaToRupeesString(kharchaPaisa)}")
    }

    return """
*$title*
$shopLine$shopGstinLine${shopLicLine}बिल #: ${slipLabel(entry.slipNo, entry.id)}
क्रेता: ${buyerName.takeIf { it.isNotBlank() && it != "खरीदार" } ?: entry.buyerId.ifBlank { "खरीदार" }}
${if (!entry.gstin.isNullOrBlank()) "GSTIN: ${entry.gstin}\n" else ""}${if (!buyerPhone.isNullOrBlank()) "मोबाइल: $buyerPhone\n" else ""}--------------------
$lotsText
--------------------
कुल बोरी: ${entry.bagsCount}
कुल वजन: ${formatQuintals2(entry.totalWeightGrams)} क्विंटल
कुल असल राशि: ₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}
$amountLines--------------------
*कुल देय राशि: ${MandiMathEngine.formatRupees(entry.totalReceivablePaisa)}*
    """.trimIndent()
}

/** Grams to quintals with exactly 2 decimals (half-up), integer math: 1_250_000 g -> "12.50". */
internal fun formatQuintals2(grams: Long): String {
    val hundredths = (grams + 500L) / 1_000L
    return "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}"
}
