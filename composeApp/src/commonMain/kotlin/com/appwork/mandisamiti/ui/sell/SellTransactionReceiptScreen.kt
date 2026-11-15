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
import com.appwork.mandisamiti.ui.slip.slipEquation
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.SellCropLot
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
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
    onShareWhatsApp: ((String) -> Unit)? = null,
    isEnglish: Boolean = false,
    gallaCashPaisa: Long = 0L,
    onSettleCash: ((amountPaisa: Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showSettlementSheet by remember { mutableStateOf(false) }
    val billText = entry?.let { buildSellBillText(it, buyerName, shopName, mandiName, shopAddress, shopGstin, shopLicenceNo) } ?: ""
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
        modifier = modifier
    ) {
        if (entry == null) {
            Text("बिल लोड हो रहा है...")
            return@ReceiptScaffold
        }
        val lots = try {
            entry.remarks?.let { Json.decodeFromString<List<SellCropLot>>(it) }
        } catch (_: Exception) {
            null
        }
        val name = buyerName.takeIf { it.isNotBlank() && it != "खरीदार" } ?: entry.buyerId.ifBlank { "खरीदार" }
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
            ReceiptAmountRow("कुल (${entry.bagsCount} बोरी, ${MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)} क्विंटल)", "₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}")
            if (!isBos) {
                if (entry.aratPaisa > 0) ReceiptAmountRow("कमीशन", "+ ₹${MandiMathEngine.paisaToRupeesString(entry.aratPaisa)}")
                val labourAmt = entry.totalReceivablePaisa - entry.grossPaisa - entry.aratPaisa
                if (labourAmt > 0) ReceiptAmountRow("मजदूरी", "+ ₹${MandiMathEngine.paisaToRupeesString(labourAmt)}")
            } else {
                if (entry.mandiShulkPaisa > 0) ReceiptAmountRow("मंडी प्रभार एवं उपकर (2%)", "+ ₹${MandiMathEngine.paisaToRupeesString(entry.mandiShulkPaisa)}")
                if (entry.tulakPaisa > 0) ReceiptAmountRow("तुलाई", "+ ₹${MandiMathEngine.paisaToRupeesString(entry.tulakPaisa)}")
                val rest = entry.totalReceivablePaisa - entry.grossPaisa - entry.mandiShulkPaisa - entry.tulakPaisa
                if (rest > 0) ReceiptAmountRow("हमाली / मजदूरी", "+ ₹${MandiMathEngine.paisaToRupeesString(rest)}")
            }
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
    shopLicenceNo: String? = null
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

    val labourAmt = if (!isBos) {
        entry.totalReceivablePaisa - entry.grossPaisa - entry.aratPaisa
    } else {
        entry.totalReceivablePaisa - entry.grossPaisa - entry.mandiShulkPaisa - entry.tulakPaisa
    }

    return """
*$title*
$shopLine$shopGstinLine${shopLicLine}बिल #: ${slipLabel(entry.slipNo, entry.id)}
क्रेता: ${buyerName.takeIf { it.isNotBlank() && it != "खरीदार" } ?: entry.buyerId.ifBlank { "खरीदार" }}
${if (!entry.gstin.isNullOrBlank()) "GSTIN: ${entry.gstin}\n" else ""}--------------------
$lotsText
--------------------
कुल बोरी: ${entry.bagsCount}
कुल वजन: ${MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)} क्विंटल
कुल असल राशि: ₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}
${if (!isBos && entry.aratPaisa > 0) "कमीशन जोड़: +₹${MandiMathEngine.paisaToRupeesString(entry.aratPaisa)}\n" else ""}${if (isBos && entry.mandiShulkPaisa > 0) "मंडी शुल्क एवं उपकर: +₹${MandiMathEngine.paisaToRupeesString(entry.mandiShulkPaisa)}\n" else ""}${if (isBos && entry.tulakPaisa > 0) "तुलाई: +₹${MandiMathEngine.paisaToRupeesString(entry.tulakPaisa)}\n" else ""}${if (labourAmt > 0) "हमाली / मजदूरी: +₹${MandiMathEngine.paisaToRupeesString(labourAmt)}\n" else ""}--------------------
*कुल देय राशि: ${MandiMathEngine.formatRupees(entry.totalReceivablePaisa)}*
    """.trimIndent()
}
