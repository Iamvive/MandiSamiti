package com.appwork.mandisamiti.ui.buy

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyCropLot
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.receiptDateTime
import com.appwork.mandisamiti.domain.model.slipLabel
import com.appwork.mandisamiti.ui.components.ReceiptAmountRow
import com.appwork.mandisamiti.ui.components.ReceiptCard
import com.appwork.mandisamiti.ui.components.ReceiptDivider
import com.appwork.mandisamiti.ui.components.ReceiptHero
import com.appwork.mandisamiti.ui.components.ReceiptLot
import com.appwork.mandisamiti.ui.components.ReceiptLotRow
import com.appwork.mandisamiti.ui.components.ReceiptScaffold
import kotlinx.serialization.json.Json

@Composable
fun BuyTransactionReceiptScreen(
    id: String,
    onBack: () -> Unit,
    onEdit: ((String) -> Unit)? = null,
    entry: BuyEntry? = null,
    farmerName: String = "किसान",
    shopName: String = "",
    mandiName: String = "",
    partyVillage: String? = null,
    onShareWhatsApp: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val displayName = farmerName.takeIf { it.isNotBlank() && it != "किसान" }
        ?: entry?.farmerId?.takeIf { it.isNotBlank() }
        ?: "किसान"
    val lots = entry?.let { extractLots(it) } ?: emptyList()
    val slipText = entry?.let { buildFarmerSlipText(it, displayName, lots) } ?: ""

    ReceiptScaffold(
        title = "किसान पर्चा",
        onBack = onBack,
        onEdit = if (entry != null && !entry.isVoid && onEdit != null) ({ onEdit(entry.id) }) else null,
        shareLabel = "व्हाट्सएप पर पर्चा भेजें",
        onShare = if (entry != null && onShareWhatsApp != null) ({ onShareWhatsApp(slipText) }) else null,
        modifier = modifier
    ) {
        if (entry == null) {
            Text("पर्चा लोड हो रहा है...")
            return@ReceiptScaffold
        }
        val village = partyVillage ?: extractVillage(entry.remarks)
        val meta = listOfNotNull(
            "#${slipLabel(entry.slipNo, entry.id)}",
            receiptDateTime(entry.entryDate),
            village?.takeIf { it.isNotBlank() },
            entry.vehicleNumber?.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
        val shop = listOf(shopName, mandiName).filter { it.isNotBlank() }.joinToString(", ")

        ReceiptCard {
            if (shop.isNotBlank()) Text(shop)
            val isRevised = entry.revision > 1 || (entry.paidPaisa > 0 && entry.paidPaisa != entry.advancePaidPaisa)
            val statusBadge = if (isRevised) "संशोधित" else "मूल पर्ची"
            Text(
                text = "$statusBadge • ${receiptDateTime(entry.updatedAt)}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            ReceiptHero(
                partyName = displayName,
                meta = meta,
                amountLabel = "अंतिम देय राशि",
                amount = MandiMathEngine.formatRupees(entry.netPayablePaisa),
                isVoid = entry.isVoid
            )
            if (lots.isNotEmpty()) {
                lots.forEachIndexed { idx, lot ->
                    ReceiptLotRow(
                        ReceiptLot(
                            title = "${idx + 1}. ${lot.cropName}",
                            detail = "${lot.bags} बोरी • ${MandiMathEngine.gramsToQuintals(lot.totalWeightGrams)} क्विंटल @ ₹${MandiMathEngine.paisaToRupeesString(lot.ratePaisaPerQuintal)}/क्विंटल",
                            amount = "₹${MandiMathEngine.paisaToRupeesString(lot.grossPaisa)}"
                        )
                    )
                }
            } else {
                ReceiptLotRow(
                    ReceiptLot(
                        title = "आवक",
                        detail = "${entry.bagsCount} बोरी • ${MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)} क्विंटल @ ₹${MandiMathEngine.paisaToRupeesString(entry.ratePaisaPerQuintal)}/क्विंटल",
                        amount = "₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}"
                    )
                )
            }
            ReceiptDivider()
            ReceiptAmountRow("कुल (${entry.bagsCount} बोरी, ${MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)} क्विंटल)", "₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}")
            ReceiptAmountRow("मजदूरी कटौती", "- ₹${MandiMathEngine.paisaToRupeesString(entry.labourPaisa)}")
            ReceiptAmountRow("कमीशन कटौती", "- ₹${MandiMathEngine.paisaToRupeesString(entry.commissionPaisa)}")
            val slipNet = (entry.grossPaisa - entry.labourPaisa - entry.commissionPaisa).coerceAtLeast(0L)
            ReceiptAmountRow("पर्ची राशि", "₹${MandiMathEngine.paisaToRupeesString(slipNet)}")
            if (entry.advancePaidPaisa > 0) {
                ReceiptAmountRow("अग्रिम / पेशगी", "- ₹${MandiMathEngine.paisaToRupeesString(entry.advancePaidPaisa)}")
            }
            ReceiptDivider()
            ReceiptAmountRow("अंतिम देय राशि", MandiMathEngine.formatRupees(entry.netPayablePaisa), emphasized = true)
            if (entry.paidPaisa > 0 && entry.paidPaisa != entry.advancePaidPaisa) {
                ReceiptAmountRow("नकद भुगतान", "- ₹${MandiMathEngine.paisaToRupeesString(entry.paidPaisa)}")
                val left = (entry.netPayablePaisa - entry.paidPaisa).coerceAtLeast(0L)
                ReceiptAmountRow("बाकी", MandiMathEngine.formatRupees(left), emphasized = true)
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
    appendLine("*किसान खरीद पर्चा*")
    appendLine("पर्चा #: ${slipLabel(entry.slipNo, entry.id)}")
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
    appendLine("मजदूरी कटौती: -₹${MandiMathEngine.paisaToRupeesString(entry.labourPaisa)}")
    appendLine("कमीशन कटौती: -₹${MandiMathEngine.paisaToRupeesString(entry.commissionPaisa)}")
    val slipNet = (entry.grossPaisa - entry.labourPaisa - entry.commissionPaisa).coerceAtLeast(0L)
    appendLine("पर्ची राशि: ₹${MandiMathEngine.paisaToRupeesString(slipNet)}")
    if (entry.advancePaidPaisa > 0) {
        appendLine("अग्रिम / पेशगी: -₹${MandiMathEngine.paisaToRupeesString(entry.advancePaidPaisa)}")
    }
    appendLine("--------------------")
    appendLine("*अंतिम देय राशि: ${MandiMathEngine.formatRupees(entry.netPayablePaisa)}*")
    if (entry.paidPaisa > 0 && entry.paidPaisa != entry.advancePaidPaisa) {
        appendLine("नकद भुगतान: -₹${MandiMathEngine.paisaToRupeesString(entry.paidPaisa)}")
        val left = (entry.netPayablePaisa - entry.paidPaisa).coerceAtLeast(0L)
        appendLine("*बाकी: ${MandiMathEngine.formatRupees(left)}*")
    }
}
