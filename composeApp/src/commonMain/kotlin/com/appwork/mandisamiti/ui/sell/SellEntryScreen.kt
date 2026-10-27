package com.appwork.mandisamiti.ui.sell

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.SellPartyKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellEntryScreen(
    existingId: String?,
    onSaved: (String) -> Unit,
    onBack: () -> Unit,
    formState: SellEntryFormState = remember { SellEntryFormState() },
    availableStockGrams: Long? = null,
    modifier: Modifier = Modifier
) {
    val preview = formState.preview()
    val isOverselling = availableStockGrams != null && preview.totalWeightGrams > availableStockGrams
    val isBos = formState.billMode == BillMode.BILL_OF_SUPPLY
    val isMerchant = formState.partyKind == SellPartyKind.MERCHANT
    val canSave = preview.grossPaisa > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingId == null) "नई बिक्री एंट्री" else "बिक्री एंट्री बदलें") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "पीछे")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bill mode selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = formState.billMode == BillMode.NORMAL,
                    onClick = { formState.billMode = BillMode.NORMAL },
                    label = { Text("सामान्य बिल (Normal)") }
                )
                FilterChip(
                    selected = formState.billMode == BillMode.BILL_OF_SUPPLY,
                    onClick = { formState.billMode = BillMode.BILL_OF_SUPPLY },
                    label = { Text("सप्लाई बिल (Bill of Supply)") }
                )
            }

            // Party kind selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = formState.partyKind == SellPartyKind.CUSTOMER,
                    onClick = { formState.partyKind = SellPartyKind.CUSTOMER },
                    label = { Text("ग्राहक (Customer)") }
                )
                FilterChip(
                    selected = formState.partyKind == SellPartyKind.MERCHANT,
                    onClick = { formState.partyKind = SellPartyKind.MERCHANT },
                    label = { Text("व्यापारी (Merchant)") }
                )
            }

            if (isMerchant) {
                OutlinedTextField(
                    value = formState.gstin,
                    onValueChange = { formState.gstin = it },
                    label = { Text("व्यापारी GSTIN") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formState.bags,
                    onValueChange = { formState.bags = it },
                    label = { Text("बोरी संख्या") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = formState.weightPerBagKg,
                    onValueChange = { formState.weightPerBagKg = it },
                    label = { Text("वजन/बोरी (किग्रा)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = formState.ratePerQuintal,
                onValueChange = { formState.ratePerQuintal = it },
                label = { Text("भाव (रु/क्विंटल)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            if (isBos) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formState.aratPercent,
                        onValueChange = { formState.aratPercent = it },
                        label = { Text("आढ़त %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = formState.tulak,
                        onValueChange = { formState.tulak = it },
                        label = { Text("तुलाई (खाली = ₹1/qtl)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = formState.received,
                onValueChange = { formState.received = it },
                label = { Text("प्राप्त राशि (नकद/ऑनलाइन रु)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            if (isOverselling) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("चेतावनी: बिक्री वजन गोदाम में उपलब्ध स्टॉक से अधिक है") },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        labelColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }

            // Calculation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("बिक्री बिल विवरण", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SummaryRow("कुल वजन", "${MandiMathEngine.gramsToQuintals(preview.totalWeightGrams)} क्विंटल")
                    SummaryRow("मूल राशि (Gross)", "₹${MandiMathEngine.paisaToRupeesString(preview.grossPaisa)}")
                    if (isBos) {
                        SummaryRow("आढ़त", "+ ₹${MandiMathEngine.paisaToRupeesString(preview.aratPaisa)}")
                        SummaryRow("तुलाई (Tulak)", "+ ₹${MandiMathEngine.paisaToRupeesString(preview.tulakPaisa)}")
                        SummaryRow("मंडी शुल्क (1.5%)", "+ ₹${MandiMathEngine.paisaToRupeesString(preview.mandiShulkPaisa)}")
                    }
                    SummaryRow("कुल प्राप्य (Total)", "₹${MandiMathEngine.paisaToRupeesString(preview.totalReceivablePaisa)}", isHighlight = true)
                    SummaryRow("प्राप्त (Received)", "₹${formState.received.ifBlank { "0" }}")
                    Spacer(Modifier.height(4.dp))
                    SummaryRow("बकाया (Balance Due)", "₹${MandiMathEngine.paisaToRupeesString(preview.balanceDuePaisa)}", isHighlight = true)
                }
            }

            Button(
                onClick = { onSaved(existingId ?: "sell-${System.currentTimeMillis()}") },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("सुरक्षित करें")
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isHighlight) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = if (isHighlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
        )
    }
}
