package com.appwork.mandisamiti.ui.sell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.repository.SellRepository
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.components.DiscardDraftDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellEntryScreen(
    shopId: String = "",
    existingId: String? = null,
    onSaved: (SellEntry) -> Unit,
    onBack: () -> Unit,
    formState: SellEntryFormState = remember { SellEntryFormState() },
    availableStockGrams: Long? = null,
    sellRepository: SellRepository? = null,
    modifier: Modifier = Modifier
) {
    var existingEntry by remember { mutableStateOf<SellEntry?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isDirty = formState.hasUnsavedChanges(existingEntry)

    MandiBackHandler(enabled = true) {
        if (isDirty) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    LaunchedEffect(existingId) {
        if (existingId != null && sellRepository != null) {
            val existing = sellRepository.getSellEntryById(existingId)
            if (existing != null) {
                existingEntry = existing
                formState.populateFrom(existing)
            }
        }
    }

    val preview = formState.aggregatePreview()
    val isBos = (formState.billMode == BillMode.BILL_OF_SUPPLY && formState.partyKind == SellPartyKind.MERCHANT)
    val isMerchant = formState.partyKind == SellPartyKind.MERCHANT
    val isOverselling = availableStockGrams != null && preview.totalWeightGrams > availableStockGrams
    val canSave = formState.canSave()
    val validationReason = formState.validationReason()

    val quickCrops = listOf("गेहूं शरबती", "गेहूं लोकवान", "चना", "सरसों", "सोयाबीन")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingId == null) "नई बिक्री एंट्री (जावक सौदा)" else "बिक्री सौदा बदलें") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isDirty) {
                                showDiscardDialog = true
                            } else {
                                onBack()
                            }
                        }
                    ) {
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
            // Category Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("क्रेता श्रेणी (Category):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = formState.partyKind == SellPartyKind.CUSTOMER,
                            onClick = { formState.setParty(SellPartyKind.CUSTOMER) },
                            label = { Text("ग्राहक (Customer)") }
                        )
                        FilterChip(
                            selected = formState.partyKind == SellPartyKind.MERCHANT,
                            onClick = { formState.setParty(SellPartyKind.MERCHANT) },
                            label = { Text("व्यापारी (Merchant)") }
                        )
                    }

                    // Invoice Mode Selector
                    Text("बिल प्रकार (Invoice Mode):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = formState.billMode == BillMode.NORMAL,
                            onClick = { formState.setMode(BillMode.NORMAL) },
                            label = { Text("सामान्य बिल (Normal)") }
                        )
                        if (isMerchant) {
                            FilterChip(
                                selected = formState.billMode == BillMode.BILL_OF_SUPPLY,
                                onClick = { formState.setMode(BillMode.BILL_OF_SUPPLY) },
                                label = { Text("सप्लाई बिल (Bill of Supply)") }
                            )
                        } else {
                            Text(
                                "सप्लाई बिल केवल व्यापारियों हेतु",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // Buyer Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        if (isMerchant) "व्यापारी विवरण (Buyer / Merchant)" else "ग्राहक विवरण (Buyer / Customer)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = formState.buyerName,
                        onValueChange = { formState.buyerName = it },
                        label = { Text(if (isMerchant) "व्यापारी / फर्म का नाम *" else "ग्राहक का नाम *") },
                        isError = !formState.buyerNameValidation.isValid && formState.buyerName.isNotEmpty(),
                        supportingText = formState.buyerNameValidation.errorOrNull?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = formState.phone,
                        onValueChange = { if (it.length <= 10) formState.phone = it.filter { c -> c.isDigit() } },
                        label = { Text("मोबाइल नंबर (वैकल्पिक)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = !formState.phoneValidation.isValid && formState.phone.isNotEmpty(),
                        supportingText = formState.phoneValidation.errorOrNull?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // BOS Specific Fields: GSTIN, StateCode, Calendar Date
                    if (isBos) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = formState.gstin,
                                onValueChange = { if (it.length <= 15) formState.gstin = it.uppercase() },
                                label = { Text("GSTIN (15 अक्षर) *") },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                isError = !formState.gstinValidation.isValid && formState.gstin.isNotEmpty(),
                                supportingText = formState.gstinValidation.errorOrNull?.let { { Text(it) } },
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = formState.stateCode,
                                onValueChange = { if (it.length <= 2) formState.stateCode = it.filter { c -> c.isDigit() } },
                                label = { Text("राज्य कोड *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = !formState.stateCodeValidation.isValid && formState.stateCode.isNotEmpty(),
                                supportingText = formState.stateCodeValidation.errorOrNull?.let { { Text(it) } },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = formState.invoiceDate.ifBlank { "आज की दिनांक" },
                            onValueChange = { formState.invoiceDate = it },
                            label = { Text("बिल दिनांक (Calendar Date)") },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Multi-Crop Lots Section
            Text(
                "फसल एवं तौल विवरण (Multi-Crop Lots)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            formState.lots.forEachIndexed { index, lot ->
                val lotNumber = index + 1
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                                "लॉट #$lotNumber: ${lot.cropName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (formState.lots.size > 1) {
                                IconButton(onClick = { formState.removeLot(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "लॉट हटाएं", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        // Quick-select chips
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            quickCrops.take(3).forEach { crop ->
                                SuggestionChip(
                                    onClick = { lot.cropName = crop },
                                    label = { Text(crop, style = MaterialTheme.typography.bodySmall) }
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
                                onValueChange = { lot.bags = it.filter { c -> c.isDigit() } },
                                label = { Text("बोरी संख्या *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = !lot.bagsValidation.isValid && lot.bags.isNotEmpty(),
                                supportingText = lot.bagsValidation.errorOrNull?.let { { Text(it) } },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lot.weightPerBagKg,
                                onValueChange = { lot.weightPerBagKg = it },
                                label = { Text("वजन/बोरी (किग्रा)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = !lot.weightValidation.isValid && lot.weightPerBagKg.isNotEmpty(),
                                supportingText = lot.weightValidation.errorOrNull?.let { { Text(it) } },
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
                                isError = !lot.rateValidation.isValid && lot.ratePerQuintal.isNotEmpty(),
                                supportingText = lot.rateValidation.errorOrNull?.let { { Text(it) } },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val lotCrop = lot.toCropLot()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "वजन: ${MandiMathEngine.gramsToQuintals(lotCrop.totalWeightGrams)} क्विंटल",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "सकल: ₹${MandiMathEngine.paisaToRupeesString(lotCrop.grossPaisa)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { formState.addLot() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("(+) अन्य फसल / वैरायटी जोड़ें")
            }

            // Deductions & Charges Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (isBos) "मंडी प्रभार एवं कर (Charges & Taxes)" else "कमीशन एवं मजदूरी (Charges)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (!isBos) {
                        // Normal Slip: Commission % and Labour
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
                    } else {
                        // BOS: Labour, Tulak, Mandi Tax (1.5%), Krishak Cess (0.5%)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = formState.labourPerBag,
                                onValueChange = { formState.labourPerBag = it },
                                label = { Text("हमाली / मजदूरी (रु/बोरी)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = formState.tulakPerQuintal,
                                onValueChange = { formState.tulakPerQuintal = it },
                                label = { Text("तुलाई (रु/क्विंटल)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("मंडी शुल्क: 1.5%") },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                            SuggestionChip(
                                onClick = {},
                                label = { Text("कृषक कल्याण उपकर: 0.5%") },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Overselling Warning
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

            // Calculation & Live Math Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        if (isBos) "सप्लाई बिल विवरण (GST Invoice)" else "बिक्री बिल विवरण (Buyer Slip)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Lot summaries
                    preview.lots.forEachIndexed { i, lot ->
                        if (lot.bags > 0) {
                            SellSummaryRow(
                                label = "फसल #${i + 1} (${lot.cropName}) [${lot.bags} बोरी]",
                                value = "₹${MandiMathEngine.paisaToRupeesString(lot.grossPaisa)}"
                            )
                        }
                    }

                    SellSummaryRow("कुल बोरी", "${preview.totalBags} बोरी")
                    SellSummaryRow("कुल वजन", "${MandiMathEngine.gramsToQuintals(preview.totalWeightGrams)} क्विंटल")
                    SellSummaryRow("कुल सकल राशि (Gross)", "₹${MandiMathEngine.paisaToRupeesString(preview.grossPaisa)}")

                    if (!isBos) {
                        if (preview.commissionPaisa > 0) {
                            SellSummaryRow("कमीशन जोड़ (+)", "+ ₹${MandiMathEngine.paisaToRupeesString(preview.commissionPaisa)}")
                        }
                        if (preview.labourPaisa > 0) {
                            SellSummaryRow("मजदूरी जोड़ (+)", "+ ₹${MandiMathEngine.paisaToRupeesString(preview.labourPaisa)}")
                        }
                    } else {
                        SellSummaryRow("मंडी शुल्क (1.5%)", "+ ₹${MandiMathEngine.paisaToRupeesString(preview.mandiShulkPaisa)}")
                        SellSummaryRow("कृषक कल्याण उपकर (0.5%)", "+ ₹${MandiMathEngine.paisaToRupeesString(preview.krishakCessPaisa)}")
                        if (preview.labourPaisa > 0) {
                            SellSummaryRow("हमाली / मजदूरी", "+ ₹${MandiMathEngine.paisaToRupeesString(preview.labourPaisa)}")
                        }
                        if (preview.tulakPaisa > 0) {
                            SellSummaryRow("तुलाई (Tulak)", "+ ₹${MandiMathEngine.paisaToRupeesString(preview.tulakPaisa)}")
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    SellSummaryRow(
                        label = "कुल देय राशि (Total Receivable)",
                        value = "₹${MandiMathEngine.paisaToRupeesString(preview.totalReceivablePaisa)}",
                        isHighlight = true
                    )
                }
            }

            // Validation Warning Banner
            AnimatedVisibility(visible = !canSave && validationReason != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = validationReason ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Save Action Button
            Button(
                onClick = { onSaved(formState.toSellEntry(existingId, shopId)) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "सुरक्षित करें (बिक्री सौदा पक्का)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (showDiscardDialog) {
                DiscardDraftDialog(
                    onConfirmDiscard = {
                        showDiscardDialog = false
                        onBack()
                    },
                    onDismiss = {
                        showDiscardDialog = false
                    }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SellSummaryRow(label: String, value: String, isHighlight: Boolean = false) {
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
