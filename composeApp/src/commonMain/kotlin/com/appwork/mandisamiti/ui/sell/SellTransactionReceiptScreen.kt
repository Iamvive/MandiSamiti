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
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.SellEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellTransactionReceiptScreen(
    id: String,
    onBack: () -> Unit,
    entry: SellEntry? = null,
    buyerName: String = "खरीदार",
    onShareWhatsApp: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val billText = entry?.let { buildSellBillText(it, buyerName) } ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entry?.billMode == BillMode.BILL_OF_SUPPLY) "सप्लाई बिल (Bill of Supply)" else "बिक्री रसीद") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "पीछे")
                    }
                },
                actions = {
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
                            Text(
                                if (entry.billMode == BillMode.BILL_OF_SUPPLY) "Bill of Supply" else "बिक्री रसीद",
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

                        ReceiptLine("खरीदार", buyerName)
                        ReceiptLine("बिल क्रमांक", "#${entry.invoiceNumber ?: entry.id.takeLast(6)}")
                        val gstin = entry.gstin
                        if (!gstin.isNullOrBlank()) {
                            ReceiptLine("GSTIN", gstin)
                        }
                        ReceiptLine("बोरी संख्या", "${entry.bagsCount} बोरी")
                        ReceiptLine("वजन", "${MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)} क्विंटल")
                        ReceiptLine("भाव", "₹${MandiMathEngine.paisaToRupeesString(entry.ratePaisaPerQuintal)} /क्विंटल")

                        Spacer(Modifier.height(8.dp))
                        ReceiptLine("मूल राशि (Gross)", "₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}")
                        if (entry.billMode == BillMode.BILL_OF_SUPPLY) {
                            ReceiptLine("आढ़त (Arat)", "+ ₹${MandiMathEngine.paisaToRupeesString(entry.aratPaisa)}")
                            ReceiptLine("तुलाई (Tulak)", "+ ₹${MandiMathEngine.paisaToRupeesString(entry.tulakPaisa)}")
                            ReceiptLine("मंडी शुल्क (1.5%)", "+ ₹${MandiMathEngine.paisaToRupeesString(entry.mandiShulkPaisa)}")
                        }

                        Spacer(Modifier.height(8.dp))
                        ReceiptLine("कुल प्राप्य (Total)", "₹${MandiMathEngine.paisaToRupeesString(entry.totalReceivablePaisa)}", isBold = true)
                        ReceiptLine("प्राप्त राशि", "₹${MandiMathEngine.paisaToRupeesString(entry.receivedPaisa)}")
                        ReceiptLine("बकाया राशि (Balance Due)", "₹${MandiMathEngine.paisaToRupeesString(entry.balanceDuePaisa)}", isBold = true)
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

fun buildSellBillText(entry: SellEntry, buyerName: String): String = """
*${if (entry.billMode == BillMode.BILL_OF_SUPPLY) "सप्लाई बिल (Bill of Supply)" else "बिक्री रसीद"}*
बिल #: ${entry.invoiceNumber ?: entry.id.takeLast(6)}
खरीदार: $buyerName
${if (!entry.gstin.isNullOrBlank()) "GSTIN: ${entry.gstin}\n" else ""}बोरी: ${entry.bagsCount}
वजन: ${MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)} क्विंटल
भाव: ₹${MandiMathEngine.paisaToRupeesString(entry.ratePaisaPerQuintal)}/क्विंटल
--------------------
मूल राशि: ₹${MandiMathEngine.paisaToRupeesString(entry.grossPaisa)}
${if (entry.billMode == BillMode.BILL_OF_SUPPLY) "आढ़त: +₹${MandiMathEngine.paisaToRupeesString(entry.aratPaisa)}\nतुलाई: +₹${MandiMathEngine.paisaToRupeesString(entry.tulakPaisa)}\nमंडी शुल्क (1.5%): +₹${MandiMathEngine.paisaToRupeesString(entry.mandiShulkPaisa)}\n" else ""}--------------------
*कुल बिल: ₹${MandiMathEngine.paisaToRupeesString(entry.totalReceivablePaisa)}*
प्राप्त: ₹${MandiMathEngine.paisaToRupeesString(entry.receivedPaisa)}
*बकाया: ₹${MandiMathEngine.paisaToRupeesString(entry.balanceDuePaisa)}*
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
