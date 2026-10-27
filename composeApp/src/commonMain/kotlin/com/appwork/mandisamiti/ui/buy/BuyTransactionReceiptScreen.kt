package com.appwork.mandisamiti.ui.buy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyCropLot
import com.appwork.mandisamiti.domain.model.BuyEntry
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyTransactionReceiptScreen(
    id: String,
    onBack: () -> Unit,
    entry: BuyEntry? = null,
    farmerName: String = "किसान",
    onShareWhatsApp: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val displayName = entry?.farmerId?.takeIf { it.isNotBlank() } ?: farmerName
    val lots = entry?.let { extractLots(it) } ?: emptyList()
    val slipText = entry?.let { buildFarmerSlipText(it, displayName, lots) } ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("किसान पर्चा (खरीद)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "पीछे")
                    }
                },
                actions = {
                    if (entry != null && onShareWhatsApp != null) {
                        IconButton(onClick = { onShareWhatsApp(slipText) }) {
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
                Text("पर्चा लोड हो रहा है...")
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            Text("किसान खरीद पर्चा", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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

                        ReceiptLine("किसान नाम", displayName, isBold = true)
                        val phoneNum = entry.vehicleNumber
                        if (!phoneNum.isNullOrBlank()) {
                            ReceiptLine("मोबाइल नंबर", phoneNum)
                        }
                        ReceiptLine("पर्चा क्रमांक", "#${entry.slipNumber ?: entry.id.takeLast(6)}")

                        // Multi-crop lots section
                        if (lots.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text("फसल आवक विवरण:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            lots.forEachIndexed { idx, lot ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("${idx + 1}. ${lot.cropName}", fontWeight = FontWeight.SemiBold)
                                            Text("₹${MandiMathEngine.paisaToRupeesString(lot.grossPaisa)}", fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            "${lot.bags} बोरी • ${MandiMathEngine.gramsToQuintals(lot.totalWeightGrams)} क्विंटल @ ₹${MandiMathEngine.paisaToRupeesString(lot.ratePaisaPerQuintal)}/क्विंटल",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            ReceiptLine("बोरी", "${entry.bagsCount} बोरी")
                            ReceiptLine("कुल वजन", "${MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)} क्विंटल")
                            ReceiptLine("भाव", "₹${MandiMathEngine.paisaToRupeesString(entry.ratePaisaPerQuintal)} /क्विंटल")
                        }

                        Spacer(Modifier.height(8.dp))
                        ReceiptLine("कुल बोरी", "${entry.bagsCount} बोरी")
                        ReceiptLine("कुल वजन", "${MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)} क्विंटल")
                        ReceiptLine("कुल राशि (Gross)", "₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}")
                        ReceiptLine("कमीशन कटौती", "- ₹${MandiMathEngine.paisaToRupeesString(entry.commissionPaisa)}")
                        ReceiptLine("मजदूरी कटौती", "- ₹${MandiMathEngine.paisaToRupeesString(entry.labourPaisa)}")
                        if (entry.advancePaidPaisa > 0) {
                            ReceiptLine("अग्रिम / पेशगी", "- ₹${MandiMathEngine.paisaToRupeesString(entry.advancePaidPaisa)}")
                        }

                        Spacer(Modifier.height(8.dp))
                        ReceiptLine(
                            "शुद्ध देय राशि",
                            "₹${MandiMathEngine.paisaToRupeesString(entry.netPayablePaisa)}",
                            isBold = true
                        )
                    }
                }

                if (onShareWhatsApp != null) {
                    Button(
                        onClick = { onShareWhatsApp(slipText) },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("व्हाट्सएप पर पर्चा भेजें")
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

fun buildFarmerSlipText(entry: BuyEntry, farmerName: String, lots: List<BuyCropLot> = emptyList()): String = buildString {
    appendLine("*किसान खरीद पर्चा*")
    appendLine("पर्चा #: ${entry.slipNumber ?: entry.id.takeLast(6)}")
    appendLine("किसान: $farmerName")
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
    appendLine("कमीशन: -₹${MandiMathEngine.paisaToRupeesString(entry.commissionPaisa)}")
    appendLine("मजदूरी: -₹${MandiMathEngine.paisaToRupeesString(entry.labourPaisa)}")
    if (entry.advancePaidPaisa > 0) {
        appendLine("अग्रिम: -₹${MandiMathEngine.paisaToRupeesString(entry.advancePaidPaisa)}")
    }
    appendLine("--------------------")
    appendLine("*शुद्ध देय राशि: ₹${MandiMathEngine.paisaToRupeesString(entry.netPayablePaisa)}*")
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
