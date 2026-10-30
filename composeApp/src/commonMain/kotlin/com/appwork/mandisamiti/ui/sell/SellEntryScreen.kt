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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.repository.SellRepository
import com.appwork.mandisamiti.domain.validation.MandiValidators
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.components.DiscardDraftDialog
import com.appwork.mandisamiti.ui.components.DuplicateEntryDialog
import com.appwork.mandisamiti.ui.components.PartyAutocompleteField
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellEntryScreen(
    shopId: String = "",
    existingId: String? = null,
    onSaved: (SellEntry) -> Unit,
    onBack: () -> Unit,
    tradeSettings: com.appwork.mandisamiti.domain.model.TradeSettings? = null,
    formState: SellEntryFormState = remember(tradeSettings) { SellEntryFormState(tradeSettings) },
    availableStockGrams: Long? = null,
    stockLines: List<com.appwork.mandisamiti.domain.model.StockLine> = emptyList(),
    sellRepository: SellRepository? = null,
    parties: List<Party> = emptyList(),
    isEnglish: Boolean = false,
    modifier: Modifier = Modifier
) {
    var existingEntry by remember { mutableStateOf<SellEntry?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showSanityConfirmation by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var pendingEntryToSave by remember { mutableStateOf<SellEntry?>(null) }

    val recentSells by (sellRepository?.getSellEntriesStream(shopId) ?: kotlinx.coroutines.flow.emptyFlow()).collectAsState(initial = emptyList())

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

    LaunchedEffect(existingEntry, parties) {
        val entry = existingEntry ?: return@LaunchedEffect
        val party = parties.firstOrNull { it.id == entry.buyerId } ?: return@LaunchedEffect
        if (formState.buyerName == entry.buyerId) formState.buyerName = party.name
        if (formState.phone.isBlank()) formState.phone = party.phone.orEmpty()
    }

    val preview = formState.aggregatePreview()
    val isBos = (formState.billMode == BillMode.BILL_OF_SUPPLY && formState.partyKind == SellPartyKind.MERCHANT)
    val isMerchant = formState.partyKind == SellPartyKind.MERCHANT

    val oversoldLots = remember(preview, stockLines) {
        val warnings = mutableListOf<String>()
        if (stockLines.isNotEmpty()) {
            val weightsByBaseCommodity = mutableMapOf<String, Long>()
            preview.lots.forEach { lot ->
                val base = com.appwork.mandisamiti.domain.model.CommodityParser.parseBaseCommodity(lot.cropName)
                if (lot.totalWeightGrams > 0L) {
                    weightsByBaseCommodity[base] = (weightsByBaseCommodity[base] ?: 0L) + lot.totalWeightGrams
                }
            }

            weightsByBaseCommodity.forEach { (baseCrop, enteredGrams) ->
                val matchingStock = stockLines.find { it.commodityId.equals(baseCrop, ignoreCase = true) }
                val availableGrams = matchingStock?.onHandGrams ?: 0L
                if (enteredGrams > availableGrams) {
                    val enteredQtl = MandiMathEngine.gramsToQuintals(enteredGrams)
                    val availQtl = MandiMathEngine.gramsToQuintals(availableGrams.coerceAtLeast(0L))
                    warnings.add("चेतावनी: $baseCrop की बिक्री (${enteredQtl} qtl) गोदाम स्टॉक (${availQtl} qtl) से अधिक है")
                }
            }
        }
        warnings
    }
    val isOverselling = oversoldLots.isNotEmpty() || (availableStockGrams != null && preview.totalWeightGrams > availableStockGrams)
    val canSave = formState.canSave()
    val validationReason = formState.validationReason()

    val quickCrops = if (isEnglish) {
        listOf("Wheat Sharbati", "Wheat Lokwan", "Gram", "Mustard", "Soybean")
    } else {
        listOf("गेहूं शरबती", "गेहूं लोकवान", "चना", "सरसों", "सोयाबीन")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEnglish) {
                            if (existingId == null) "New Sale Entry" else "Edit Sale Entry"
                        } else {
                            if (existingId == null) "नई बिक्री एंट्री (जावक सौदा)" else "बिक्री सौदा बदलें"
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
            // Category Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isEnglish) "Buyer Category:" else "क्रेता श्रेणी (Category):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = formState.partyKind == SellPartyKind.CUSTOMER,
                            onClick = { formState.setParty(SellPartyKind.CUSTOMER) },
                            label = { Text(if (isEnglish) "Customer" else "ग्राहक (Customer)") }
                        )
                        FilterChip(
                            selected = formState.partyKind == SellPartyKind.MERCHANT,
                            onClick = { formState.setParty(SellPartyKind.MERCHANT) },
                            label = { Text(if (isEnglish) "Merchant" else "व्यापारी (Merchant)") }
                        )
                    }

                    // Invoice Mode Selector
                    Text(if (isEnglish) "Invoice Mode:" else "बिल प्रकार (Invoice Mode):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = formState.billMode == BillMode.NORMAL,
                            onClick = { formState.setMode(BillMode.NORMAL) },
                            label = { Text(if (isEnglish) "Normal Bill" else "सामान्य बिल (Normal)") }
                        )
                        if (isMerchant) {
                            FilterChip(
                                selected = formState.billMode == BillMode.BILL_OF_SUPPLY,
                                onClick = { formState.setMode(BillMode.BILL_OF_SUPPLY) },
                                label = { Text(if (isEnglish) "Bill of Supply" else "सप्लाई बिल (Bill of Supply)") }
                            )
                        } else {
                            Text(
                                if (isEnglish) "Bill of Supply is for merchants only" else "सप्लाई बिल केवल व्यापारियों हेतु",
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
                    val buyerCardTitle = if (isEnglish) {
                        if (isMerchant) "Merchant Details (Buyer)" else "Customer Details (Buyer)"
                    } else {
                        if (isMerchant) "व्यापारी विवरण (Buyer / Merchant)" else "ग्राहक विवरण (Buyer / Customer)"
                    }
                    Text(
                        buyerCardTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val buyerLabel = if (isEnglish) {
                        if (isMerchant) "Merchant / Firm Name *" else "Customer Name *"
                    } else {
                        if (isMerchant) "व्यापारी / फर्म का नाम *" else "ग्राहक का नाम *"
                    }
                    PartyAutocompleteField(
                        value = formState.buyerName,
                        onValueChange = {
                            formState.buyerName = it
                            formState.selectedBuyerId = null
                        },
                        label = buyerLabel,
                        parties = parties,
                        partyType = PartyType.BUYER,
                        onPartySelected = { selected ->
                            formState.selectedBuyerId = selected.id
                            formState.buyerName = selected.name
                            selected.phone?.let { formState.phone = it }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        isError = !formState.buyerNameValidation.isValid && formState.buyerName.isNotEmpty(),
                        supportingText = formState.buyerNameValidation.errorOrNull?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = formState.phone,
                        onValueChange = { if (it.length <= 10) formState.phone = it.filter { c -> c.isDigit() } },
                        label = { Text(if (isEnglish) "Mobile Number (Optional)" else "मोबाइल नंबर (वैकल्पिक)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
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
                                label = { Text(if (isEnglish) "GSTIN (15 chars) *" else "GSTIN (15 अक्षर) *") },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                                isError = !formState.gstinValidation.isValid && formState.gstin.isNotEmpty(),
                                supportingText = formState.gstinValidation.errorOrNull?.let { { Text(it) } },
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = formState.stateCode,
                                onValueChange = { if (it.length <= 2) formState.stateCode = it.filter { c -> c.isDigit() } },
                                label = { Text(if (isEnglish) "State Code *" else "राज्य कोड *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                isError = !formState.stateCodeValidation.isValid && formState.stateCode.isNotEmpty(),
                                supportingText = formState.stateCodeValidation.errorOrNull?.let { { Text(it) } },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val defaultDateText = if (isEnglish) "Today's Date" else "आज की दिनांक"
                        OutlinedTextField(
                            value = formState.invoiceDate.ifBlank { defaultDateText },
                            onValueChange = { formState.invoiceDate = it },
                            label = { Text(if (isEnglish) "Invoice Date" else "बिल दिनांक (Calendar Date)") },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Multi-Crop Lots Section
            Text(
                if (isEnglish) "Crop & Weighment Details (Lots)" else "फसल एवं तौल विवरण (Multi-Crop Lots)",
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
                                if (isEnglish) "Lot #$lotNumber: ${lot.cropName}" else "लॉट #$lotNumber: ${lot.cropName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (formState.lots.size > 1) {
                                IconButton(onClick = { formState.removeLot(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = if (isEnglish) "Delete Lot" else "लॉट हटाएं", tint = MaterialTheme.colorScheme.error)
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
                            label = { Text(if (isEnglish) "Crop / Variety Name" else "जिंस / फसल वैरायटी") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = lot.bags,
                                onValueChange = { lot.bags = it.filter { c -> c.isDigit() } },
                                label = { Text(if (isEnglish) "Bags Count *" else "बोरी संख्या *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                isError = !lot.bagsValidation.isValid && lot.bags.isNotEmpty(),
                                supportingText = lot.bagsValidation.errorOrNull?.let { { Text(it) } },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lot.weightPerBagKg,
                                onValueChange = { lot.weightPerBagKg = it },
                                label = { Text(if (isEnglish) "Weight/Bag (kg)" else "वजन/बोरी (किग्रा)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                isError = !lot.weightValidation.isValid && lot.weightPerBagKg.isNotEmpty(),
                                supportingText = lot.weightValidation.errorOrNull?.let { { Text(it) } },
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
                                isError = !lot.rateValidation.isValid && lot.ratePerQuintal.isNotEmpty(),
                                supportingText = {
                                    val ratePaisa = MandiMathEngine.parseRupeesToPaisa(lot.ratePerQuintal.ifBlank { "0" })
                                    val isOutlier = ratePaisa > 0L && MandiValidators.isRateOutlier(ratePaisa)
                                    if (!lot.rateValidation.isValid && lot.ratePerQuintal.isNotEmpty()) {
                                        Text(lot.rateValidation.errorOrNull ?: "", color = MaterialTheme.colorScheme.error)
                                    } else if (isOutlier) {
                                        Text(if (isEnglish) "⚠️ Unusual rate (< ₹500 or > ₹20,000/qtl)" else "⚠️ असामान्य भाव (< ₹500 या > ₹20,000/क्विंटल)", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val lotCrop = lot.toCropLot()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val qtlSuffix = if (isEnglish) "qtl" else "क्विंटल"
                            Text(
                                if (isEnglish) "Weight: ${MandiMathEngine.gramsToQuintals(lotCrop.totalWeightGrams)} $qtlSuffix" else "वजन: ${MandiMathEngine.gramsToQuintals(lotCrop.totalWeightGrams)} $qtlSuffix",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (isEnglish) "Gross: ₹${MandiMathEngine.paisaToRupeesString(lotCrop.grossPaisa)}" else "सकल: ₹${MandiMathEngine.paisaToRupeesString(lotCrop.grossPaisa)}",
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
                Text(if (isEnglish) "(+) Add Another Crop Lot" else "(+) अन्य फसल / वैरायटी जोड़ें")
            }

            // Deductions & Charges Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val chargesTitle = if (isEnglish) {
                        if (isBos) "Charges & Taxes" else "Charges"
                    } else {
                        if (isBos) "मंडी प्रभार एवं कर (Charges & Taxes)" else "कमीशन एवं मजदूरी (Charges)"
                    }
                    Text(
                        chargesTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val defaultComm = tradeSettings?.let { MandiMathEngine.paisaToInputString(it.buyerCommissionBps) } ?: "0"
                    val defaultLabour = tradeSettings?.let { MandiMathEngine.paisaToRupeesString(it.defaultLabourPaisa) } ?: "150"
                    val defaultTare = tradeSettings?.let { MandiMathEngine.gramsToQuintalsInputString(it.defaultTareGrams) } ?: ""

                    if (!isBos) {
                        // Normal Slip: Commission % and Labour
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
                        OutlinedTextField(
                            value = formState.tareQuintals,
                            onValueChange = { formState.tareQuintals = it },
                            label = { Text(if (isEnglish) "Tare / Bardana (qtl)" else "काट / बारदाना (कुंतल)") },
                            placeholder = { Text("0") },
                            supportingText = if (formState.tareQuintals != defaultTare) {
                                { Text(if (isEnglish) "Changed from default (${defaultTare.ifBlank { "0" }} qtl)" else "डिफ़ॉल्ट से बदला गया (${defaultTare.ifBlank { "0" }} qtl)", color = MandiAmberDark, fontSize = 11.sp) }
                            } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // BOS: Labour, Tulak, Mandi Tax (1.5%), Krishak Cess (0.5%)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = formState.labourPerBag,
                                onValueChange = { formState.labourPerBag = it },
                                label = { Text(if (isEnglish) "Hammali / Labour (₹/bag)" else "हमाली / मजदूरी (रु/बोरी)") },
                                supportingText = if (formState.labourPerBag != defaultLabour) {
                                    { Text(if (isEnglish) "Changed from default (₹$defaultLabour)" else "डिफ़ॉल्ट से बदला गया (₹$defaultLabour)", color = MandiAmberDark, fontSize = 11.sp) }
                                } else null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = formState.tulakPerQuintal,
                                onValueChange = { formState.tulakPerQuintal = it },
                                label = { Text(if (isEnglish) "Tulak (₹/qtl)" else "तुलाई (रु/क्विंटल)") },
                                supportingText = if (formState.tulakPerQuintal != "1") {
                                    { Text(if (isEnglish) "Changed from default (₹1)" else "डिफ़ॉल्ट से बदला गया (₹1)", color = MandiAmberDark, fontSize = 11.sp) }
                                } else null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = formState.tareQuintals,
                            onValueChange = { formState.tareQuintals = it },
                            label = { Text(if (isEnglish) "Tare / Bardana (qtl)" else "काट / बारदाना (कुंतल)") },
                            placeholder = { Text("0") },
                            supportingText = if (formState.tareQuintals != defaultTare) {
                                { Text(if (isEnglish) "Changed from default (${defaultTare.ifBlank { "0" }} qtl)" else "डिफ़ॉल्ट से बदला गया (${defaultTare.ifBlank { "0" }} qtl)", color = MandiAmberDark, fontSize = 11.sp) }
                            } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(if (isEnglish) "Mandi Tax: 1.5%" else "मंडी शुल्क: 1.5%") },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                            SuggestionChip(
                                onClick = {},
                                label = { Text(if (isEnglish) "Krishak Cess: 0.5%" else "कृषक कल्याण उपकर: 0.5%") },
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val messages = if (oversoldLots.isNotEmpty()) oversoldLots else listOf("चेतावनी: बिक्री वजन गोदाम में उपलब्ध स्टॉक से अधिक है")
                    messages.forEach { msg ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(msg) },
                            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }
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
                        if (isBos) {
                            if (isEnglish) "GST Supply Bill Summary" else "सप्लाई बिल विवरण (GST Invoice)"
                        } else {
                            if (isEnglish) "Sale Bill Summary" else "बिक्री बिल विवरण (Buyer Slip)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Lot summaries
                    preview.lots.forEachIndexed { i, lot ->
                        if (lot.bags > 0) {
                            val bagsSuffix = if (isEnglish) "bags" else "बोरी"
                            val lotLabel = if (isEnglish) "Lot #${i + 1} (${lot.cropName}) [${lot.bags} $bagsSuffix]" else "फसल #${i + 1} (${lot.cropName}) [${lot.bags} $bagsSuffix]"
                            SellSummaryRow(
                                label = lotLabel,
                                value = "₹${MandiMathEngine.paisaToRupeesString(lot.grossPaisa)}"
                            )
                        }
                    }

                    val bagsUnit = if (isEnglish) "bags" else "बोरी"
                    val qtlUnit = if (isEnglish) "qtl" else "क्विंटल"
                    SellSummaryRow(if (isEnglish) "Total Bags" else "कुल बोरी", "${MandiMathEngine.formatIndianGrouping(preview.totalBags)} $bagsUnit")
                    if (formState.tareQuintals.isNotBlank() && formState.tareQuintals != "0") {
                        SellSummaryRow(if (isEnglish) "Tare / Bardana" else "काट / बारदाना", "- ${formState.tareQuintals} $qtlUnit")
                    }
                    SellSummaryRow(if (isEnglish) "Total Weight" else "कुल वजन", "${MandiMathEngine.gramsToQuintals(preview.totalWeightGrams)} $qtlUnit")
                    SellSummaryRow(if (isEnglish) "Total Gross Amount" else "कुल सकल राशि (Gross)", MandiMathEngine.formatRupees(preview.grossPaisa))

                    if (!isBos) {
                        if (preview.commissionPaisa > 0) {
                            SellSummaryRow(if (isEnglish) "Add Commission (+)" else "कमीशन जोड़ (+)", "+ ₹${MandiMathEngine.paisaToRupeesString(preview.commissionPaisa)}")
                        }
                        if (preview.labourPaisa > 0) {
                            SellSummaryRow(if (isEnglish) "Add Labour (+)" else "मजदूरी जोड़ (+)", "+ ₹${MandiMathEngine.paisaToRupeesString(preview.labourPaisa)}")
                        }
                    } else {
                        SellSummaryRow(if (isEnglish) "Mandi Tax (1.5%)" else "मंडी शुल्क (1.5%)", "+ ₹${MandiMathEngine.paisaToRupeesString(preview.mandiShulkPaisa)}")
                        SellSummaryRow(if (isEnglish) "Krishak Cess (0.5%)" else "कृषक कल्याण उपकर (0.5%)", "+ ₹${MandiMathEngine.paisaToRupeesString(preview.krishakCessPaisa)}")
                        if (preview.labourPaisa > 0) {
                            SellSummaryRow(if (isEnglish) "Hammali / Labour" else "हमाली / मजदूरी", "+ ₹${MandiMathEngine.paisaToRupeesString(preview.labourPaisa)}")
                        }
                        if (preview.tulakPaisa > 0) {
                            SellSummaryRow(if (isEnglish) "Tulak" else "तुलाई (Tulak)", "+ ₹${MandiMathEngine.paisaToRupeesString(preview.tulakPaisa)}")
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    if (formState.lots.any { it.ratePerQuintal.isBlank() || it.ratePerQuintal == "0" }) {
                        SellSummaryRow(
                            label = if (isEnglish) "Total Receivable" else "कुल देय राशि (Total Receivable)",
                            value = if (isEnglish) "Enter Rate" else "भाव दर्ज करें",
                            isHighlight = true
                        )
                    } else {
                        SellSummaryRow(
                            label = if (isEnglish) "Total Receivable" else "कुल देय राशि (Total Receivable)",
                            value = MandiMathEngine.formatRupees(preview.totalReceivablePaisa),
                            isHighlight = true
                        )
                    }
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
                onClick = {
                    val entry = formState.toSellEntry(existingId, shopId)
                    val now = Clock.System.now().toEpochMilliseconds()
                    val isDuplicate = existingId == null && recentSells.any { prev ->
                        !prev.isVoid &&
                        prev.buyerId.trim().equals(entry.buyerId.trim(), ignoreCase = true) &&
                        prev.totalReceivablePaisa == entry.totalReceivablePaisa &&
                        prev.bagsCount == entry.bagsCount &&
                        prev.commodityId.trim().equals(entry.commodityId.trim(), ignoreCase = true) &&
                        kotlin.math.abs(now - prev.createdAt) <= 2 * 60 * 1000L
                    }

                    if (isDuplicate) {
                        pendingEntryToSave = entry
                        showDuplicateDialog = true
                    } else if (formState.hasSanityWarning()) {
                        pendingEntryToSave = entry
                        showSanityConfirmation = true
                    } else {
                        onSaved(entry)
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    if (isEnglish) "Save Sale Entry" else "सुरक्षित करें (बिक्री सौदा पक्का)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (showDuplicateDialog && pendingEntryToSave != null) {
                val pending = pendingEntryToSave!!
                val bagsSuffix = if (isEnglish) "bags" else "बोरी"
                DuplicateEntryDialog(
                    message = if (isEnglish) "Duplicate entry detected. Still want to save?" else "यह एंट्री पहले से है, फिर भी सुरक्षित करें?",
                    details = "${pending.buyerId} • ₹${MandiMathEngine.formatIndianGrouping(pending.totalReceivablePaisa / 100)} • ${pending.bagsCount} $bagsSuffix (${pending.commodityId})",
                    onConfirm = {
                        showDuplicateDialog = false
                        if (formState.hasSanityWarning()) {
                            showSanityConfirmation = true
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

            if (showSanityConfirmation && pendingEntryToSave != null) {
                AlertDialog(
                    onDismissRequest = {
                        showSanityConfirmation = false
                        pendingEntryToSave = null
                    },
                    icon = {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
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
                                showSanityConfirmation = false
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
                                showSanityConfirmation = false
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
