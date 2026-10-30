package com.appwork.mandisamiti.ui.sell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.SellCropLot
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.model.receiptDateTime
import com.appwork.mandisamiti.domain.model.slipLabel
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellTransactionReceiptScreen(
    id: String,
    onBack: () -> Unit,
    onEdit: ((String) -> Unit)? = null,
    entry: SellEntry? = null,
    buyerName: String = "खरीदार",
    shopName: String = "",
    mandiName: String = "",
    partyVillage: String? = null,
    onShareWhatsApp: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val billText = entry?.let { buildSellBillText(it, buyerName) } ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (entry?.billMode == BillMode.BILL_OF_SUPPLY) "सप्लाई बिल (Bill of Supply)"
                        else if (entry?.partyKind == SellPartyKind.MERCHANT) "व्यापारी बिक्री पर्चा"
                        else "ग्राहक बिक्री पर्चा"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "पीछे")
                    }
                },
                actions = {
                    if (entry != null && !entry.isVoid && onEdit != null) {
                        IconButton(onClick = { onEdit(entry.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "सुधारें (Edit)")
                        }
                    }
                    if (entry != null && onShareWhatsApp != null) {
                        IconButton(onClick = { onShareWhatsApp(billText) }) {
                            Icon(Icons.Default.Share, contentDescription = "व्हाट्सएप शेयर")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (entry == null) {
                Text("बिल लोड हो रहा है...")
            } else {
                val lots = try {
                    entry.remarks?.let { Json.decodeFromString<List<SellCropLot>>(it) }
                } catch (_: Exception) {
                    null
                }
                val isBos = entry.billMode == BillMode.BILL_OF_SUPPLY
                val phoneNum = entry.vehicleNumber

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isBos) "सप्लाई बिल (Bill of Supply)" else "बिक्री पर्चा (Buyer Slip)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (entry.isVoid) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("रद्द") },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        labelColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                )
                            }
                        }

                        if (shopName.isNotBlank()) ReceiptLine("दुकान", shopName, isBold = true)
                        if (mandiName.isNotBlank()) ReceiptLine("मंडी", mandiName)
                        ReceiptLine("दिनांक / समय", receiptDateTime(entry.entryDate))
                        ReceiptLine(
                            if (entry.partyKind == SellPartyKind.MERCHANT) "व्यापारी नाम" else "ग्राहक नाम",
                            buyerName.takeIf { it.isNotBlank() && it != "खरीदार" } ?: entry.buyerId.ifBlank { "खरीदार" }
                        )
                        if (!phoneNum.isNullOrBlank()) {
                            ReceiptLine("मोबाइल नंबर", phoneNum)
                        }
                        if (!partyVillage.isNullOrBlank()) ReceiptLine("गाँव", partyVillage)
                        ReceiptLine("बिल क्रमांक", "#${slipLabel(entry.slipNo, entry.id)}")

                        val gstin = entry.gstin
                        if (!gstin.isNullOrBlank()) {
                            ReceiptLine("GSTIN", gstin)
                        }

                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))

                        Text("फसल विक्रय विवरण:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        if (!lots.isNullOrEmpty()) {
                            lots.forEachIndexed { i, lot ->
                                val lotWeightQtl = MandiMathEngine.gramsToQuintals(lot.totalWeightGrams)
                                val lotRate = MandiMathEngine.paisaToRupeesString(lot.ratePaisaPerQuintal)
                                val lotGross = MandiMathEngine.paisaToRupeesString(lot.grossPaisa)
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${i + 1}. ${lot.cropName}", fontWeight = FontWeight.SemiBold)
                                        Text("₹$lotGross", fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        "${lot.bags} बोरी • $lotWeightQtl क्विंटल @ ₹$lotRate/क्विंटल",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            ReceiptLine("जिंस / फसल", entry.commodityId)
                            ReceiptLine("बोरी संख्या", "${entry.bagsCount} बोरी")
                            ReceiptLine("वजन", "${MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)} क्विंटल")
                            ReceiptLine("भाव", "₹${MandiMathEngine.paisaToRupeesString(entry.ratePaisaPerQuintal)} /क्विंटल")
                        }

                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))

                        ReceiptLine("कुल बोरी", "${entry.bagsCount} बोरी")
                        ReceiptLine("कुल वजन", "${MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)} क्विंटल")
                        ReceiptLine("कुल सकल राशि (Gross)", "₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}")

                        if (!isBos) {
                            if (entry.aratPaisa > 0) {
                                ReceiptLine("कमीशन जोड़ (+)", "+ ₹${MandiMathEngine.paisaToRupeesString(entry.aratPaisa)}")
                            }
                            // Labour = total - gross - arat
                            val labourAmt = entry.totalReceivablePaisa - entry.grossPaisa - entry.aratPaisa
                            if (labourAmt > 0) {
                                ReceiptLine("मजदूरी जोड़ (+)", "+ ₹${MandiMathEngine.paisaToRupeesString(labourAmt)}")
                            }
                        } else {
                            if (entry.mandiShulkPaisa > 0) {
                                ReceiptLine("मंडी प्रभार एवं उपकर (2%)", "+ ₹${MandiMathEngine.paisaToRupeesString(entry.mandiShulkPaisa)}")
                            }
                            if (entry.tulakPaisa > 0) {
                                ReceiptLine("तुलाई (Tulak)", "+ ₹${MandiMathEngine.paisaToRupeesString(entry.tulakPaisa)}")
                            }
                            val remainingCharges = entry.totalReceivablePaisa - entry.grossPaisa - entry.mandiShulkPaisa - entry.tulakPaisa
                            if (remainingCharges > 0) {
                                ReceiptLine("हमाली / मजदूरी", "+ ₹${MandiMathEngine.paisaToRupeesString(remainingCharges)}")
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        ReceiptLine(
                            "कुल देय राशि (Total Receivable)",
                            MandiMathEngine.formatRupees(entry.totalReceivablePaisa),
                            isBold = true
                        )
                    }
                }

                if (onShareWhatsApp != null) {
                    Button(
                        onClick = { onShareWhatsApp(billText) },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("व्हाट्सएप पर बिल भेजें")
                    }
                }
            }
        }
    }
}

fun buildSellBillText(entry: SellEntry, buyerName: String): String {
    val lots = try {
        entry.remarks?.let { Json.decodeFromString<List<SellCropLot>>(it) }
    } catch (_: Exception) {
        null
    }
    val isBos = entry.billMode == BillMode.BILL_OF_SUPPLY
    val title = if (isBos) "सप्लाई बिल (Bill of Supply)" else "बिक्री पर्चा (Mandi Sale Bill)"

    val lotsText = if (!lots.isNullOrEmpty()) {
        lots.mapIndexed { i, lot ->
            "${i + 1}. ${lot.cropName}: ₹${MandiMathEngine.paisaToRupeesString(lot.grossPaisa)}\n   (${lot.bags} बोरी • ${MandiMathEngine.gramsToQuintals(lot.totalWeightGrams)} क्विंटल @ ₹${MandiMathEngine.paisaToRupeesString(lot.ratePaisaPerQuintal)}/qtl)"
        }.joinToString("\n")
    } else {
        "जिंस: ${entry.commodityId}\nबोरी: ${entry.bagsCount} • वजन: ${MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)} क्विंटल @ ₹${MandiMathEngine.paisaToRupeesString(entry.ratePaisaPerQuintal)}/qtl"
    }

    return """
*$title*
बिल #: ${slipLabel(entry.slipNo, entry.id)}
क्रेता: ${buyerName.takeIf { it.isNotBlank() && it != "खरीदार" } ?: entry.buyerId.ifBlank { "खरीदार" }}
${if (!entry.gstin.isNullOrBlank()) "GSTIN: ${entry.gstin}\n" else ""}--------------------
$lotsText
--------------------
कुल बोरी: ${entry.bagsCount}
कुल वजन: ${MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)} क्विंटल
कुल सकल राशि: ₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}
${if (!isBos && entry.aratPaisa > 0) "कमीशन जोड़: +₹${MandiMathEngine.paisaToRupeesString(entry.aratPaisa)}\n" else ""}${if (isBos && entry.mandiShulkPaisa > 0) "मंडी शुल्क एवं उपकर: +₹${MandiMathEngine.paisaToRupeesString(entry.mandiShulkPaisa)}\n" else ""}${if (isBos && entry.tulakPaisa > 0) "तुलाई: +₹${MandiMathEngine.paisaToRupeesString(entry.tulakPaisa)}\n" else ""}--------------------
*कुल देय राशि: ${MandiMathEngine.formatRupees(entry.totalReceivablePaisa)}*
    """.trimIndent()
}

@Composable
private fun ReceiptLine(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}
