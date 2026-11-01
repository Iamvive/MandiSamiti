package com.appwork.mandisamiti.ui.buy

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

        val tareQ = extractTare(entry.remarks)
        val isRevised = entry.revision > 1
        val revisionLabel = if (isRevised) {
            "संशोधित • ${receiptDateTime(entry.updatedAt)}"
        } else {
            "मूल पर्ची • ${receiptDateTime(entry.createdAt)}"
        }

        ReceiptCard {
            if (shop.isNotBlank()) Text(shop)
            ReceiptHero(
                partyName = displayName,
                meta = "$revisionLabel • $meta",
                amountLabel = "शुद्ध देय राशि",
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
            if (!tareQ.isNullOrBlank() && tareQ != "0") {
                ReceiptAmountRow("काट / बारदाना", "- $tareQ क्विंटल")
            }
            ReceiptAmountRow("मजदूरी", "- ₹${MandiMathEngine.paisaToRupeesString(entry.labourPaisa)}")
            ReceiptAmountRow("कमीशन", "- ₹${MandiMathEngine.paisaToRupeesString(entry.commissionPaisa)}")
            if (entry.advancePaidPaisa > 0) {
                ReceiptAmountRow("अग्रिम / पेशगी", "- ₹${MandiMathEngine.paisaToRupeesString(entry.advancePaidPaisa)}")
            }
            val loanCutPaisa = (entry.paidPaisa - entry.advancePaidPaisa).coerceAtLeast(0L)
            if (loanCutPaisa > 0) {
                ReceiptAmountRow("उधार कटौती", "- ₹${MandiMathEngine.paisaToRupeesString(loanCutPaisa)}")
            }
            ReceiptDivider()
            ReceiptAmountRow("शुद्ध देय राशि", MandiMathEngine.formatRupees(entry.netPayablePaisa), emphasized = true)
            if (entry.paidPaisa > 0) {
                val leftPaisa = (entry.netPayablePaisa - entry.paidPaisa).coerceAtLeast(0L)
                ReceiptAmountRow("भुगतान किया", "₹${MandiMathEngine.paisaToRupeesString(entry.paidPaisa)}")
                ReceiptAmountRow("बाकी राशि", MandiMathEngine.formatRupees(leftPaisa), emphasized = true)
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
    val isRevised = entry.revision > 1
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
    val tareQ = extractTare(entry.remarks)
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
    if (!tareQ.isNullOrBlank() && tareQ != "0") {
        appendLine("काट / बारदाना: -$tareQ क्विंटल")
    }
    appendLine("मजदूरी: -₹${MandiMathEngine.paisaToRupeesString(entry.labourPaisa)}")
    appendLine("कमीशन: -₹${MandiMathEngine.paisaToRupeesString(entry.commissionPaisa)}")
    if (entry.advancePaidPaisa > 0) {
        appendLine("अग्रिम: -₹${MandiMathEngine.paisaToRupeesString(entry.advancePaidPaisa)}")
    }
    val loanCutPaisa = (entry.paidPaisa - entry.advancePaidPaisa).coerceAtLeast(0L)
    if (loanCutPaisa > 0) {
        appendLine("उधार कटौती: -₹${MandiMathEngine.paisaToRupeesString(loanCutPaisa)}")
    }
    appendLine("--------------------")
    appendLine("*शुद्ध देय राशि: ${MandiMathEngine.formatRupees(entry.netPayablePaisa)}*")
    if (entry.paidPaisa > 0) {
        val leftPaisa = (entry.netPayablePaisa - entry.paidPaisa).coerceAtLeast(0L)
        appendLine("भुगतान किया: ₹${MandiMathEngine.paisaToRupeesString(entry.paidPaisa)}")
        appendLine("*बाकी राशि: ${MandiMathEngine.formatRupees(leftPaisa)}*")
    }
}
