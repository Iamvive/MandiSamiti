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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.repository.BuyRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyEntryScreen(
    shopId: String = "",
    existingId: String? = null,
    onSaved: (BuyEntry) -> Unit,
    onBack: () -> Unit,
    formState: BuyEntryFormState = remember { BuyEntryFormState() },
    hasActiveLoan: Boolean = false,
    activeLoanChecker: ((String) -> Boolean)? = null,
    buyRepository: BuyRepository? = null,
    modifier: Modifier = Modifier
) {
    var existingEntry by remember { mutableStateOf<BuyEntry?>(null) }
    LaunchedEffect(existingId) {
        if (existingId != null && buyRepository != null) {
            val entry = buyRepository.getBuyEntryById(existingId)
            if (entry != null) {
                existingEntry = entry
                formState.populateFrom(entry)
            }
        }
    }

    val preview = formState.aggregatePreview()
    val canSave = formState.canSave()
    val validationReason = formState.validationReason()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingId == null) "नई खरीद एंट्री (आवक सौदा)" else "खरीद एंट्री बदलें") },
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section 1: Farmer Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "किसान विवरण (विक्रेता)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val nameVal = formState.validateFarmerName()
                    OutlinedTextField(
                        value = formState.farmerName,
                        onValueChange = { formState.farmerName = it },
                        label = { Text("किसान नाम *") },
                        isError = !nameVal.isValid && formState.farmerName.isNotEmpty(),
                        supportingText = {
                            if (!nameVal.isValid && formState.farmerName.isNotEmpty()) {
                                Text(nameVal.errorOrNull ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val phoneVal = formState.validatePhone()
                        OutlinedTextField(
                            value = formState.phone,
                            onValueChange = { formState.updatePhone(it) },
                            label = { Text("मोबाइल नंबर (वैकल्पिक)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = formState.phone.isNotEmpty() && !phoneVal.isValid,
                            supportingText = {
                                if (formState.phone.isNotEmpty() && !phoneVal.isValid) {
                                    Text(phoneVal.errorOrNull ?: "", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = formState.village,
                            onValueChange = { formState.village = it },
                            label = { Text("गाँव (वैकल्पिक)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val showActiveLoan = hasActiveLoan || (activeLoanChecker != null && formState.farmerName.isNotBlank() && activeLoanChecker(formState.farmerName.trim()))
                    if (showActiveLoan) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("सक्रिय उधार मौजूद है (हिसाब में कटौती संभव)") },
                            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }
            }

            // Section 2: Crop Lots / Variants
            Text(
                text = "फसल एवं तौल विवरण (Multi-Crop Lots)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val commonCrops = listOf("गेहूं शरबती", "गेहूं लोकवान", "चना", "सरसों", "सोयाबीन")

            formState.lots.forEachIndexed { index, lot ->
                val lotResult = lot.calculateLot()
                val lotVal = lot.validate()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "लॉट #${index + 1}: ${lot.cropName.ifBlank { "फसल" }}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (formState.lots.size > 1) {
                                IconButton(
                                    onClick = { formState.removeLot(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "लॉट हटाएं",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        // Quick crop chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            commonCrops.take(3).forEach { crop ->
                                FilterChip(
                                    selected = lot.cropName == crop,
                                    onClick = { lot.cropName = crop },
                                    label = { Text(crop, fontSize = 12.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = lot.cropName,
                            onValueChange = { lot.cropName = it },
                            label = { Text("जिंस / फसल वैरायटी") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = lot.bags,
                                onValueChange = { lot.bags = it.filter { ch -> ch.isDigit() } },
                                label = { Text("बोरी संख्या *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = lot.bags.isNotEmpty() && lotVal.bagsError != null,
                                supportingText = {
                                    if (lot.bags.isNotEmpty() && lotVal.bagsError != null) {
                                        Text(lotVal.bagsError, color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lot.weightPerBagKg,
                                onValueChange = { lot.weightPerBagKg = it },
                                label = { Text("वजन/बोरी (किग्रा)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = lot.weightPerBagKg.isNotEmpty() && lotVal.weightError != null,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = lot.extraWeightKg,
                                onValueChange = { lot.extraWeightKg = it },
                                label = { Text("अतिरिक्त वजन (किग्रा)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lot.ratePerQuintal,
                                onValueChange = { lot.ratePerQuintal = it },
                                label = { Text("भाव (रु/क्विंटल) *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = lot.ratePerQuintal.isNotEmpty() && lotVal.rateError != null,
                                supportingText = {
                                    if (lot.ratePerQuintal.isNotEmpty() && lotVal.rateError != null) {
                                        Text(lotVal.rateError, color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Lot Subtotal
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "वजन: ${MandiMathEngine.gramsToQuintals(lotResult.totalWeightGrams)} क्विंटल",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "सकल: ₹${MandiMathEngine.paisaToRupeesString(lotResult.grossPaisa)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Add Another Crop Lot Button
            OutlinedButton(
                onClick = { formState.addLot() },
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("(+) अन्य फसल / वैरायटी जोड़ें")
            }

            // Section 3: Trade Deductions & Optional Advance
            Text(
                text = "कटौती एवं अग्रिम (Deductions)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formState.commissionPercent,
                    onValueChange = { formState.commissionPercent = it },
                    label = { Text("कमीशन %") },
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
                label = { Text("अग्रिम / पेशगी (रु) (वैकल्पिक)") },
                placeholder = { Text("0") },
                supportingText = { Text("यदि किसान को पहले नकद पेशगी दी गई हो") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Section 4: Live Parcha Preview Card
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

                    // Lot breakdown lines
                    preview.lotsCalculations.forEachIndexed { idx, pair ->
                        val lot = pair.first
                        val res = pair.second
                        if (res.grossPaisa > 0) {
                            SummaryRow(
                                label = "फसल #${idx + 1} (${lot.cropName}) [${res.bags} बोरी]",
                                value = "₹${MandiMathEngine.paisaToRupeesString(res.grossPaisa)}"
                            )
                        }
                    }

                    SummaryRow("कुल बोरी", "${preview.totalBags} बोरी")
                    SummaryRow("कुल वजन", "${MandiMathEngine.gramsToQuintals(preview.totalWeightGrams)} क्विंटल")
                    SummaryRow("कुल राशि (Gross)", "₹${MandiMathEngine.paisaToRupeesString(preview.totalGrossPaisa)}")
                    SummaryRow("कमीशन कटौती", "- ₹${MandiMathEngine.paisaToRupeesString(preview.commissionPaisa)}")
                    SummaryRow("मजदूरी कटौती", "- ₹${MandiMathEngine.paisaToRupeesString(preview.labourPaisa)}")
                    if (preview.advancePaidPaisa > 0) {
                        SummaryRow("अग्रिम / पेशगी", "- ₹${MandiMathEngine.paisaToRupeesString(preview.advancePaidPaisa)}")
                    }
                    Spacer(Modifier.height(4.dp))
                    SummaryRow(
                        "शुद्ध देय राशि (Net Payable)",
                        "₹${MandiMathEngine.paisaToRupeesString(preview.netPayablePaisa)}",
                        isHighlight = true
                    )
                }
            }

            // Reason / Validation banner when not eligible to save
            if (!canSave && validationReason != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = validationReason,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val entry = formState.toBuyEntry(
                        shopId = shopId,
                        existingId = existingId,
                        existingEntry = existingEntry
                    )
                    onSaved(entry)
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("सुरक्षित करें (सौदा पक्का)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(80.dp))
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
