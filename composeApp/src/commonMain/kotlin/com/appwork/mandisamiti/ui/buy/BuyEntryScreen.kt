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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyEntryScreen(
    existingId: String?,
    onSaved: (String) -> Unit,
    onBack: () -> Unit,
    formState: BuyEntryFormState = remember { BuyEntryFormState() },
    hasActiveLoan: Boolean = false,
    modifier: Modifier = Modifier
) {
    val preview = formState.preview()
    val isNetNegative = preview.netPayablePaisa < 0
    val canSave = preview.grossPaisa > 0 && !isNetNegative

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingId == null) "नई खरीद एंट्री" else "खरीद एंट्री बदलें") },
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
            OutlinedTextField(
                value = formState.phone,
                onValueChange = { formState.phone = it },
                label = { Text("किसान मोबाइल नंबर") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            if (hasActiveLoan) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("सक्रिय उधार मौजूद है") },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        labelColor = MaterialTheme.colorScheme.onErrorContainer
                    )
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formState.extraWeightKg,
                    onValueChange = { formState.extraWeightKg = it },
                    label = { Text("अतिरिक्त वजन (किग्रा)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = formState.ratePerQuintal,
                    onValueChange = { formState.ratePerQuintal = it },
                    label = { Text("भाव (रु/क्विंटल)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formState.commissionPercent,
                    onValueChange = { formState.commissionPercent = it },
                    label = { Text("आढ़त / कमीशन %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = formState.labourPerBag,
                    onValueChange = { formState.labourPerBag = it },
                    label = { Text("मजदूरी (रु/बोरी)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = formState.advancePaid,
                onValueChange = { formState.advancePaid = it },
                label = { Text("अग्रिम / पेशगी (रु)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Live Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("हिसाब पर्चा (लाइव प्रीव्यू)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SummaryRow("कुल वजन", "${MandiMathEngine.gramsToQuintals(preview.totalWeightGrams)} क्विंटल")
                    SummaryRow("कुल राशि (Gross)", "₹${MandiMathEngine.paisaToRupeesString(preview.grossPaisa)}")
                    SummaryRow("कमीशन कटौती", "- ₹${MandiMathEngine.paisaToRupeesString(preview.commissionPaisa)}")
                    SummaryRow("मजदूरी कटौती", "- ₹${MandiMathEngine.paisaToRupeesString(preview.labourPaisa)}")
                    SummaryRow("अग्रिम / पेशगी", "- ₹${formState.advancePaid.ifBlank { "0" }}")
                    Spacer(Modifier.height(4.dp))
                    SummaryRow(
                        "शुद्ध देय राशि (Net Payable)",
                        "₹${MandiMathEngine.paisaToRupeesString(preview.netPayablePaisa)}",
                        isHighlight = true
                    )
                }
            }

            if (isNetNegative) {
                Text(
                    text = "चेतावनी: अग्रिम राशि कुल हिसाब से ज्यादा है (देय राशि ऋणात्मक है)",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = { onSaved(existingId ?: "buy-${System.currentTimeMillis()}") },
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
