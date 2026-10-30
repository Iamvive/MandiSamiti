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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.repository.BuyRepository
import com.appwork.mandisamiti.domain.validation.MandiValidators
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.components.DiscardDraftDialog
import com.appwork.mandisamiti.ui.components.DuplicateEntryDialog
import com.appwork.mandisamiti.ui.components.PartyAutocompleteField
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyEntryScreen(
    shopId: String = "",
    existingId: String? = null,
    onSaved: (BuyEntry) -> Unit,
    onBack: () -> Unit,
    tradeSettings: com.appwork.mandisamiti.domain.model.TradeSettings? = null,
    formState: BuyEntryFormState = remember(tradeSettings) { BuyEntryFormState(tradeSettings) },
    hasActiveLoan: Boolean = false,
    activeLoanChecker: ((String) -> Boolean)? = null,
    buyRepository: BuyRepository? = null,
    parties: List<Party> = emptyList(),
    isEnglish: Boolean = false,
    modifier: Modifier = Modifier
) {
    var existingEntry by remember { mutableStateOf<BuyEntry?>(null) }
    var showRateOutlierConfirmation by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var pendingEntryToSave by remember { mutableStateOf<BuyEntry?>(null) }

    val recentBuys by (buyRepository?.getBuyEntriesStream(shopId) ?: kotlinx.coroutines.flow.emptyFlow()).collectAsState(initial = emptyList())

    val isDirty = formState.hasUnsavedChanges(existingEntry)

    MandiBackHandler(enabled = true) {
        if (isDirty) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    LaunchedEffect(existingId) {
        if (existingId != null && buyRepository != null) {
            val entry = buyRepository.getBuyEntryById(existingId)
            if (entry != null) {
                existingEntry = entry
                formState.populateFrom(entry)
            }
        }
    }

    LaunchedEffect(existingEntry, parties) {
        val entry = existingEntry ?: return@LaunchedEffect
        val party = parties.firstOrNull { it.id == entry.farmerId } ?: return@LaunchedEffect
        if (formState.farmerName == entry.farmerId) formState.farmerName = party.name
        if (formState.phone.isBlank()) party.phone?.let(formState::updatePhone)
        if (formState.village.isBlank()) formState.village = party.village.orEmpty()
    }

    val preview = formState.aggregatePreview()
    val canSave = formState.canSave()
    val validationReason = formState.validationReason()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEnglish) {
                            if (existingId == null) "New Purchase Entry" else "Edit Purchase Entry"
                        } else {
                            if (existingId == null) "नई खरीद एंट्री (आवक सौदा)" else "खरीद एंट्री बदलें"
                        }
                    )
                },
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (isEnglish) "Back" else "पीछे")
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
                        text = if (isEnglish) "Farmer Details (Seller)" else "किसान विवरण (विक्रेता)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val nameVal = formState.validateFarmerName()
                    PartyAutocompleteField(
                        value = formState.farmerName,
                        onValueChange = {
                            formState.farmerName = it
                            formState.selectedFarmerId = null
                        },
                        label = if (isEnglish) "Farmer Name *" else "किसान नाम *",
                        parties = parties,
                        partyType = PartyType.FARMER,
                        onPartySelected = { selected ->
                            formState.selectedFarmerId = selected.id
                            formState.farmerName = selected.name
                            selected.phone?.let { formState.updatePhone(it) }
                            formState.village = selected.village ?: ""
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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
                            label = { Text(if (isEnglish) "Mobile Number (Optional)" else "मोबाइल नंबर (वैकल्पिक)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
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
                            label = { Text(if (isEnglish) "Village (Optional)" else "गाँव (वैकल्पिक)") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val activeLoanKey = formState.selectedFarmerId ?: formState.farmerName.trim()
                    val showActiveLoan = hasActiveLoan || (activeLoanChecker != null && activeLoanKey.isNotBlank() && activeLoanChecker(activeLoanKey))
                    if (showActiveLoan) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(if (isEnglish) "Active loan exists (can deduct in settlement)" else "सक्रिय उधार मौजूद है (हिसाब में कटौती संभव)") },
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
                text = if (isEnglish) "Crop & Weighment Details (Lots)" else "फसल एवं तौल विवरण (Multi-Crop Lots)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val commonCrops = if (isEnglish) {
                listOf("Wheat Sharbati", "Wheat Lokwan", "Gram", "Mustard", "Soybean")
            } else {
                listOf("गेहूं शरबती", "गेहूं लोकवान", "चना", "सरसों", "सोयाबीन")
            }

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
                            val defaultCropLabel = if (isEnglish) "Crop" else "फसल"
                            Text(
                                text = if (isEnglish) "Lot #${index + 1}: ${lot.cropName.ifBlank { defaultCropLabel }}" else "लॉट #${index + 1}: ${lot.cropName.ifBlank { defaultCropLabel }}",
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
                                        contentDescription = if (isEnglish) "Delete Lot" else "लॉट हटाएं",
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
                            label = { Text(if (isEnglish) "Crop / Variety Name" else "जिंस / फसल वैरायटी") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = lot.bags,
                                onValueChange = { lot.bags = it.filter { ch -> ch.isDigit() } },
                                label = { Text(if (isEnglish) "Bags Count *" else "बोरी संख्या *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
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
                                label = { Text(if (isEnglish) "Weight/Bag (kg)" else "वजन/बोरी (किग्रा)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                isError = lot.weightPerBagKg.isNotEmpty() && lotVal.weightError != null,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = lot.extraWeightKg,
                                onValueChange = { lot.extraWeightKg = it },
                                label = { Text(if (isEnglish) "Extra Weight (kg)" else "अतिरिक्त वजन (किग्रा)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lot.ratePerQuintal,
                                onValueChange = { lot.ratePerQuintal = it },
                                label = { Text(if (isEnglish) "Rate (₹/qtl) *" else "भाव (रु/क्विंटल) *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                isError = lot.ratePerQuintal.isNotEmpty() && lotVal.rateError != null,
                                supportingText = {
                                    val ratePaisa = MandiMathEngine.parseRupeesToPaisa(lot.ratePerQuintal.ifBlank { "0" })
                                    val isOutlier = ratePaisa > 0L && MandiValidators.isRateOutlier(ratePaisa)
                                    if (lot.ratePerQuintal.isNotEmpty() && lotVal.rateError != null) {
                                        Text(lotVal.rateError, color = MaterialTheme.colorScheme.error)
                                    } else if (isOutlier) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = MandiAmberDark,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                if (isEnglish) "Unusual rate (< ₹500 or > ₹20,000/qtl)" else "असामान्य भाव (< ₹500 या > ₹20,000/क्विंटल)",
                                                color = MandiAmberDark,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
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
                                    text = if (isEnglish) "Weight: ${MandiMathEngine.gramsToQuintals(lotResult.totalWeightGrams)} qtl" else "वजन: ${MandiMathEngine.gramsToQuintals(lotResult.totalWeightGrams)} क्विंटल",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isEnglish) "Gross: ₹${MandiMathEngine.paisaToRupeesString(lotResult.grossPaisa)}" else "सकल: ₹${MandiMathEngine.paisaToRupeesString(lotResult.grossPaisa)}",
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
                Text(if (isEnglish) "(+) Add Another Crop Lot" else "(+) अन्य फसल / वैरायटी जोड़ें")
            }

            // Section 3: Trade Deductions & Optional Advance
            Text(
                text = if (isEnglish) "Deductions & Advance" else "कटौती एवं अग्रिम (Deductions)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val defaultComm = tradeSettings?.let { MandiMathEngine.paisaToInputString(it.farmerCommissionBps) } ?: "1.5"
            val defaultLabour = tradeSettings?.let { MandiMathEngine.paisaToRupeesString(it.defaultLabourPaisa) } ?: "150"
            val defaultTare = tradeSettings?.let { MandiMathEngine.gramsToQuintalsInputString(it.defaultTareGrams) } ?: ""

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formState.commissionPercent,
                    onValueChange = { formState.commissionPercent = it },
                    label = { Text(if (isEnglish) "Commission %" else "कमीशन %") },
                    supportingText = if (formState.commissionPercent != defaultComm) {
                        { Text(if (isEnglish) "Changed from default ($defaultComm%)" else "डिफ़ॉल्ट से बदला गया ($defaultComm%)", color = MandiAmberDark, fontSize = 11.sp) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = formState.labourPerBag,
                    onValueChange = { formState.labourPerBag = it },
                    label = { Text(if (isEnglish) "Labour (₹/bag)" else "मजदूरी (रु/बोरी)") },
                    supportingText = if (formState.labourPerBag != defaultLabour) {
                        { Text(if (isEnglish) "Changed from default (₹$defaultLabour)" else "डिफ़ॉल्ट से बदला गया (₹$defaultLabour)", color = MandiAmberDark, fontSize = 11.sp) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formState.tareQuintals,
                    onValueChange = { formState.tareQuintals = it },
                    label = { Text(if (isEnglish) "Tare / Bardana (qtl)" else "काट / बारदाना (कुंतल)") },
                    placeholder = { Text("0") },
                    supportingText = if (formState.tareQuintals != defaultTare) {
                        { Text(if (isEnglish) "Changed from default (${defaultTare.ifBlank { "0" }} qtl)" else "डिफ़ॉल्ट से बदला गया (${defaultTare.ifBlank { "0" }} qtl)", color = MandiAmberDark, fontSize = 11.sp) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = formState.advancePaid,
                    onValueChange = { formState.advancePaid = it },
                    label = { Text(if (isEnglish) "Advance Paid (₹)" else "अग्रिम / पेशगी (रु)") },
                    placeholder = { Text("0") },
                    supportingText = { Text(if (isEnglish) "Optional" else "वैकल्पिक") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f)
                )
            }

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
                    Text(if (isEnglish) "Bill Receipt (Live Preview)" else "हिसाब पर्चा (लाइव प्रीव्यू)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // Lot breakdown lines
                    preview.lotsCalculations.forEachIndexed { idx, pair ->
                        val lot = pair.first
                        val res = pair.second
                        if (res.grossPaisa > 0) {
                            val bagsSuffix = if (isEnglish) "bags" else "बोरी"
                            val lotLabel = if (isEnglish) "Lot #${idx + 1} (${lot.cropName}) [${MandiMathEngine.formatIndianGrouping(res.bags)} $bagsSuffix]" else "फसल #${idx + 1} (${lot.cropName}) [${MandiMathEngine.formatIndianGrouping(res.bags)} $bagsSuffix]"
                            SummaryRow(
                                label = lotLabel,
                                value = MandiMathEngine.formatRupees(res.grossPaisa)
                            )
                        }
                    }

                    val bagsUnit = if (isEnglish) "bags" else "बोरी"
                    val qtlUnit = if (isEnglish) "qtl" else "क्विंटल"
                    SummaryRow(if (isEnglish) "Total Bags" else "कुल बोरी", "${MandiMathEngine.formatIndianGrouping(preview.totalBags)} $bagsUnit")
                    if (formState.tareQuintals.isNotBlank() && formState.tareQuintals != "0") {
                        SummaryRow(if (isEnglish) "Tare / Bardana" else "काट / बारदाना", "- ${formState.tareQuintals} $qtlUnit")
                    }
                    SummaryRow(if (isEnglish) "Total Weight" else "कुल वजन", "${MandiMathEngine.gramsToQuintals(preview.totalWeightGrams)} $qtlUnit")
                    SummaryRow(if (isEnglish) "Gross Amount" else "कुल राशि (Gross)", MandiMathEngine.formatRupees(preview.totalGrossPaisa))
                    SummaryRow(if (isEnglish) "Commission" else "कमीशन कटौती", "- ₹${MandiMathEngine.paisaToRupeesString(preview.commissionPaisa)}")
                    SummaryRow(if (isEnglish) "Labour" else "मजदूरी कटौती", "- ₹${MandiMathEngine.paisaToRupeesString(preview.labourPaisa)}")
                    if (preview.advancePaidPaisa > 0) {
                        SummaryRow(if (isEnglish) "Advance Paid" else "अग्रिम / पेशगी", "- ₹${MandiMathEngine.paisaToRupeesString(preview.advancePaidPaisa)}")
                    }
                    Spacer(Modifier.height(4.dp))
                    if (formState.lots.any { it.ratePerQuintal.isBlank() || it.ratePerQuintal == "0" }) {
                        SummaryRow(
                            if (isEnglish) "Net Payable" else "शुद्ध देय राशि (Net Payable)",
                            if (isEnglish) "Enter Rate" else "भाव दर्ज करें",
                            isHighlight = true
                        )
                    } else {
                        SummaryRow(
                            if (isEnglish) "Net Payable" else "शुद्ध देय राशि (Net Payable)",
                            MandiMathEngine.formatRupees(preview.netPayablePaisa),
                            isHighlight = true
                        )
                    }
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
                    val now = Clock.System.now().toEpochMilliseconds()
                    val isDuplicate = existingId == null && recentBuys.any { prev ->
                        !prev.isVoid &&
                        prev.farmerId.trim().equals(entry.farmerId.trim(), ignoreCase = true) &&
                        prev.netPayablePaisa == entry.netPayablePaisa &&
                        prev.bagsCount == entry.bagsCount &&
                        prev.commodityId.trim().equals(entry.commodityId.trim(), ignoreCase = true) &&
                        kotlin.math.abs(now - prev.createdAt) <= 2 * 60 * 1000L
                    }

                    if (isDuplicate) {
                        pendingEntryToSave = entry
                        showDuplicateDialog = true
                    } else if (formState.hasSanityWarning()) {
                        pendingEntryToSave = entry
                        showRateOutlierConfirmation = true
                    } else {
                        onSaved(entry)
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (isEnglish) "Save Purchase Entry" else "सुरक्षित करें (सौदा पक्का)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            if (showDuplicateDialog && pendingEntryToSave != null) {
                val pending = pendingEntryToSave!!
                val bagsSuffix = if (isEnglish) "bags" else "बोरी"
                DuplicateEntryDialog(
                    message = if (isEnglish) "Duplicate entry detected. Still want to save?" else "यह एंट्री पहले से है, फिर भी सुरक्षित करें?",
                    details = "${pending.farmerId} • ₹${MandiMathEngine.formatIndianGrouping(pending.netPayablePaisa / 100)} • ${pending.bagsCount} $bagsSuffix (${pending.commodityId})",
                    onConfirm = {
                        showDuplicateDialog = false
                        if (formState.hasSanityWarning()) {
                            showRateOutlierConfirmation = true
                        } else {
                            onSaved(pending)
                            pendingEntryToSave = null
                        }
                    },
                    onDismiss = {
                        showDuplicateDialog = false
                        pendingEntryToSave = null
                    }
                )
            }

            if (showRateOutlierConfirmation && pendingEntryToSave != null) {
                AlertDialog(
                    onDismissRequest = {
                        showRateOutlierConfirmation = false
                        pendingEntryToSave = null
                    },
                    icon = {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MandiAmberDark,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = {
                        Text(
                            text = if (isEnglish) "Unusual Value Confirmation (Sanity Check)" else "असामान्य प्रविष्टि पुष्टि (Sanity Check)",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        val baseMsg = formState.sanityWarningMessage() ?: if (isEnglish) "Unusual values detected in the entry." else "प्रविष्टि में असामान्य मान दर्ज हैं।"
                        val question = if (isEnglish) "\n\nIs this information correct or was there a typing mistake?" else "\n\nक्या विवरण सही है अथवा कोई अतिरिक्त अंक दर्ज हो गया है?"
                        Text(
                            text = baseMsg + question,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val entry = pendingEntryToSave!!
                                showRateOutlierConfirmation = false
                                pendingEntryToSave = null
                                onSaved(entry)
                            }
                        ) {
                            Text(if (isEnglish) "Yes, Correct (Save)" else "हाँ, विवरण सही है (सौदा सुरक्षित करें)")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showRateOutlierConfirmation = false
                                pendingEntryToSave = null
                            }
                        ) {
                            Text(if (isEnglish) "Edit" else "सुधारें (Edit)")
                        }
                    }
                )
            }

            if (showDiscardDialog) {
                DiscardDraftDialog(
                    title = if (isEnglish) "Discard Changes?" else "बदलाव रद्द करें?",
                    message = if (isEnglish) "Are you sure you want to exit without saving? All entered details will be lost." else "क्या आप बिना सुरक्षित किए बाहर जाना चाहते हैं? भरी गई जानकारी मिट जाएगी।",
                    confirmText = if (isEnglish) "Yes, Exit" else "हाँ, बाहर निकलें",
                    dismissText = if (isEnglish) "Continue Editing" else "भरना जारी रखें",
                    onConfirmDiscard = {
                        showDiscardDialog = false
                        onBack()
                    },
                    onDismiss = {
                        showDiscardDialog = false
                    }
                )
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
