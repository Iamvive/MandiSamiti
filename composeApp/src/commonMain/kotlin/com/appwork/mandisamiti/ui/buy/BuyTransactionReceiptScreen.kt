package com.appwork.mandisamiti.ui.buy

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.appwork.mandisamiti.domain.model.BuyEntry

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
    val slipText = entry?.let { buildFarmerSlipText(it, farmerName) } ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("किसान पर्चा") },
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
                            Text("किसान पर्चा (खरीद)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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

                        ReceiptLine("किसान नाम", farmerName)
                        ReceiptLine("पर्चा क्रमांक", "#${entry.slipNumber ?: entry.id.takeLast(6)}")
                        ReceiptLine("बोरी", "${entry.bagsCount} बोरी")
                        ReceiptLine("कुल वजन", "${MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)} क्विंटल")
                        ReceiptLine("भाव", "₹${MandiMathEngine.paisaToRupeesString(entry.ratePaisaPerQuintal)} /क्विंटल")

                        Spacer(Modifier.height(8.dp))
                        ReceiptLine("कुल राशि (Gross)", "₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}")
                        ReceiptLine("कमीशन कटौती", "- ₹${MandiMathEngine.paisaToRupeesString(entry.commissionPaisa)}")
                        ReceiptLine("मजदूरी कटौती", "- ₹${MandiMathEngine.paisaToRupeesString(entry.labourPaisa)}")
                        ReceiptLine("अग्रिम / पेशगी", "- ₹${MandiMathEngine.paisaToRupeesString(entry.advancePaidPaisa)}")

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

fun buildFarmerSlipText(entry: BuyEntry, farmerName: String): String = """
*किसान खरीद पर्चा*
पर्चा #: ${entry.slipNumber ?: entry.id.takeLast(6)}
किसान: $farmerName
बोरी: ${entry.bagsCount}
वजन: ${MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)} क्विंटल
भाव: ₹${MandiMathEngine.paisaToRupeesString(entry.ratePaisaPerQuintal)}/क्विंटल
--------------------
कुल राशि: ₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}
कमीशन: -₹${MandiMathEngine.paisaToRupeesString(entry.commissionPaisa)}
मजदूरी: -₹${MandiMathEngine.paisaToRupeesString(entry.labourPaisa)}
अग्रिम: -₹${MandiMathEngine.paisaToRupeesString(entry.advancePaidPaisa)}
--------------------
*शुद्ध देय राशि: ₹${MandiMathEngine.paisaToRupeesString(entry.netPayablePaisa)}*
""".trimIndent()

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
