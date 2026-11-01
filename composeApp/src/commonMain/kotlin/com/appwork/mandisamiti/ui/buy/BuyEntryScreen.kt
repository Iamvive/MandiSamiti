package com.appwork.mandisamiti.ui.buy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.appwork.mandisamiti.ui.components.MandiCard
import com.appwork.mandisamiti.ui.components.MandiChip
import com.appwork.mandisamiti.ui.components.MandiMoneyRow
import com.appwork.mandisamiti.ui.components.MandiPrimaryButton
import com.appwork.mandisamiti.ui.components.MandiScreenHeader
import com.appwork.mandisamiti.ui.components.MandiSecondaryButton
import com.appwork.mandisamiti.ui.components.MandiSection
import com.appwork.mandisamiti.ui.components.NewPartyConfirmDialog
import com.appwork.mandisamiti.ui.components.PartyAutocompleteField
import com.appwork.mandisamiti.ui.components.TypedPartyCheck
import com.appwork.mandisamiti.ui.components.checkTypedParty
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import kotlinx.datetime.Clock

/**
 * Rebuilt BuyEntryScreen adhering strictly to Direction B final:
 * - Always-visible bottom sticky action dock eliminating the long scroll to Save.
 * - Conforms to NGDL tokens and typography scale (no bracketed English, no unapproved colors).
 * - Full multi-lot, commission, labour, advance calculation, sanity checks and duplicate warnings.
 * - Single-transaction party + slip persistence via saveBuyCheckout.
 */
@Composable
fun BuyEntryScreen(
    shopId: String = "",
    existingId: String? = null,
    onSaved: (BuyEntry, Party?) -> Unit,
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
    var newPartyCheck by remember { mutableStateOf<TypedPartyCheck.NeedsConfirm?>(null) }
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

    val commit: (BuyEntry) -> Unit = { raw ->
        val existing = parties.firstOrNull { it.id == formState.selectedFarmerId || it.id == raw.farmerId }
            ?: formState.phone.takeIf { it.isNotBlank() }?.let { ph -> parties.firstOrNull { it.phone == ph } }
        val party = BuyPartyResolver.partyToSave(formState, shopId, existing, Clock.System.now().toEpochMilliseconds())
        val partyId = existing?.id ?: party?.id
        onSaved(if (partyId != null) raw.copy(farmerId = partyId) else raw, party)
    }

    val performSave: () -> Unit = {
        val entry = formState.toBuyEntry(
            shopId = shopId,
            existingId = existingId,
            existingEntry = existingEntry
        )
        val now = Clock.System.now().toEpochMilliseconds()
        val isDuplicate = existingId == null && recentBuys.any { prev ->
            val isSameParty = prev.farmerId.trim().equals(entry.farmerId.trim(), ignoreCase = true)
            val isSameCommodity = prev.commodityId.trim().equals(entry.commodityId.trim(), ignoreCase = true)
            val isSameBags = prev.bagsCount == entry.bagsCount
            val isSameAmount = prev.netPayablePaisa == entry.netPayablePaisa
            val isRecent = kotlin.math.abs(now - prev.createdAt) <= 2 * 60 * 1000L
            !prev.isVoid && isSameParty && isSameCommodity && isSameBags && isSameAmount && isRecent
        }

        if (isDuplicate) {
            pendingEntryToSave = entry
            showDuplicateDialog = true
        } else if (formState.hasSanityWarning()) {
            pendingEntryToSave = entry
            showRateOutlierConfirmation = true
        } else {
            commit(entry)
        }
    }

    val defaultComm = tradeSettings?.let { MandiMathEngine.paisaToInputString(it.farmerCommissionBps) } ?: "1.5"
    val defaultLabour = tradeSettings?.let { MandiMathEngine.paisaToRupeesString(it.defaultLabourPaisa) } ?: "150"
    val defaultTare = tradeSettings?.let { MandiMathEngine.gramsToQuintalsInputString(it.defaultTareGrams) } ?: ""

    Scaffold(
        topBar = {
            MandiScreenHeader(
                title = if (isEnglish) {
                    if (existingId == null) "New Purchase" else "Edit Purchase"
                } else {
                    if (existingId == null) "नई खरीद" else "खरीद बदलें"
                },
                subtitle = if (isEnglish) "Buy Entry & Farmer Bill" else "आवक सौदा व किसान पर्ची",
                onBack = {
                    if (isDirty) {
                        showDiscardDialog = true
                    } else {
                        onBack()
                    }
                }
            )
        },
        bottomBar = {
            // Direction B final: Always-visible sticky action dock (eliminating long scroll)
            Surface(
                color = MandiSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MandiBorder),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isEnglish) "Final Payable" else "अंतिम देय राशि",
                                fontSize = 12.sp,
                                color = MandiTextSecondary
                            )
                            val finalAmount = preview.payoutPaisa.coerceAtLeast(0L)
                            Text(
                                text = "₹${MandiMathEngine.formatIndianGrouping(finalAmount / 100)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiTextPrimary
                            )
                        }

                        if (!canSave && validationReason != null) {
                            Text(
                                text = validationReason,
                                fontSize = 11.sp,
                                color = MandiRedPayable,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(start = 12.dp)
                            )
                        }
                    }

                    MandiPrimaryButton(
                        text = if (isEnglish) "Save Purchase Entry" else "सुरक्षित करें (सौदा पक्का)",
                        onClick = {
                            when (val check = checkTypedParty(formState.farmerName, formState.selectedFarmerId, parties)) {
                                is TypedPartyCheck.ExactMatch -> { formState.selectedFarmerId = check.party.id; performSave() }
                                is TypedPartyCheck.NeedsConfirm -> { newPartyCheck = check }
                                else -> performSave()
                            }
                        },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MandiBackground)
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Farmer Details (Phone-First)
            MandiSection(title = if (isEnglish) "Farmer Details" else "किसान विवरण") {
                MandiCard {
                    val nameVal = formState.validateFarmerName()
                    PartyAutocompleteField(
                        value = formState.farmerName,
                        onValueChange = {
                            formState.farmerName = it
                            formState.selectedFarmerId = null
                        },
                        label = if (isEnglish) "Farmer Name *" else "किसान का नाम *",
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
                                Text(nameVal.errorOrNull ?: "", color = MandiRedPayable, fontSize = 11.sp)
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
                            label = { Text(if (isEnglish) "Mobile Number *" else "मोबाइल नंबर *", fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            isError = formState.phone.isNotEmpty() && !phoneVal.isValid,
                            supportingText = {
                                if (formState.phone.isNotEmpty() && !phoneVal.isValid) {
                                    Text(phoneVal.errorOrNull ?: "", color = MandiRedPayable, fontSize = 11.sp)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MandiBorder,
                                unfocusedBorderColor = MandiBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        val villageVal = formState.validateVillage()
                        OutlinedTextField(
                            value = formState.village,
                            onValueChange = { formState.village = it },
                            label = { Text(if (isEnglish) "Village *" else "गाँव *", fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            isError = formState.village.isNotEmpty() && !villageVal.isValid,
                            supportingText = {
                                if (formState.village.isNotEmpty() && !villageVal.isValid) {
                                    Text(villageVal.errorOrNull ?: "", color = MandiRedPayable, fontSize = 11.sp)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MandiBorder,
                                unfocusedBorderColor = MandiBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val activeLoanKey = formState.selectedFarmerId ?: formState.farmerName.trim()
                    val showActiveLoan = hasActiveLoan || (activeLoanChecker != null && activeLoanKey.isNotBlank() && activeLoanChecker(activeLoanKey))
                    if (showActiveLoan) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MandiSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MandiBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MandiAmberDark, modifier = Modifier.size(16.dp))
                                Text(
                                    if (isEnglish) "Active loan exists (can deduct in settlement)" else "सक्रिय उधार मौजूद है (हिसाब में कटौती संभव)",
                                    fontSize = 12.sp,
                                    color = MandiTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Crop Lots / Varieties
            MandiSection(title = if (isEnglish) "Crop Details" else "फसल विवरण") {
                val commonCrops = if (isEnglish) {
                    listOf("Wheat", "Wheat Sharbati", "Gram", "Mustard", "Soybean")
                } else {
                    listOf("गेहूं", "गेहूं शरबती", "चना", "सरसों", "सोयाबीन")
                }

                formState.lots.forEachIndexed { index, lot ->
                    val lotResult = lot.calculateLot()
                    val lotVal = lot.validate()

                    MandiCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val defaultCropLabel = if (isEnglish) "Crop" else "फसल"
                            Text(
                                text = if (isEnglish) "Lot #${index + 1}: ${lot.cropName.ifBlank { defaultCropLabel }}" else "लॉट #${index + 1}: ${lot.cropName.ifBlank { defaultCropLabel }}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiTextPrimary
                            )
                            if (formState.lots.size > 1) {
                                IconButton(
                                    onClick = { formState.removeLot(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = if (isEnglish) "Delete Lot" else "लॉट हटाएं",
                                        tint = MandiRedPayable
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
                                MandiChip(
                                    label = crop,
                                    selected = lot.cropName == crop,
                                    onClick = { lot.cropName = crop }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = lot.cropName,
                            onValueChange = { lot.cropName = it },
                            label = { Text(if (isEnglish) "Crop / Variety Name" else "जिंस / फसल वैरायटी", fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MandiBorder,
                                unfocusedBorderColor = MandiBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = lot.bags,
                                onValueChange = { lot.bags = it.filter { ch -> ch.isDigit() } },
                                label = { Text(if (isEnglish) "Bags Count *" else "बोरी संख्या *", fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                isError = lot.bags.isNotEmpty() && lotVal.bagsError != null,
                                supportingText = {
                                    if (lot.bags.isNotEmpty() && lotVal.bagsError != null) {
                                        Text(lotVal.bagsError, color = MandiRedPayable, fontSize = 11.sp)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MandiBorder,
                                    unfocusedBorderColor = MandiBorder
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lot.weightPerBagKg,
                                onValueChange = { lot.weightPerBagKg = it },
                                label = { Text(if (isEnglish) "Weight/Bag (kg)" else "वजन/बोरी (किग्रा)", fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                isError = lot.weightPerBagKg.isNotEmpty() && lotVal.weightError != null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MandiBorder,
                                    unfocusedBorderColor = MandiBorder
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = lot.extraWeightKg,
                                onValueChange = { lot.extraWeightKg = it },
                                label = { Text(if (isEnglish) "Extra (kg)" else "अतिरिक्त (किग्रा)", fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MandiBorder,
                                    unfocusedBorderColor = MandiBorder
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lot.ratePerQuintal,
                                onValueChange = { lot.ratePerQuintal = it },
                                label = { Text(if (isEnglish) "Rate (₹/qtl) *" else "भाव (रु/क्विंटल) *", fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                isError = lot.ratePerQuintal.isNotEmpty() && lotVal.rateError != null,
                                supportingText = {
                                    val ratePaisa = MandiMathEngine.parseRupeesToPaisa(lot.ratePerQuintal.ifBlank { "0" })
                                    val isOutlier = ratePaisa > 0L && MandiValidators.isRateOutlier(ratePaisa)
                                    if (lot.ratePerQuintal.isNotEmpty() && lotVal.rateError != null) {
                                        Text(lotVal.rateError, color = MandiRedPayable, fontSize = 11.sp)
                                    } else if (isOutlier) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = MandiAmberDark, modifier = Modifier.size(14.dp))
                                            Text(
                                                if (isEnglish) "Unusual rate (< ₹500 or > ₹20,000)" else "असामान्य भाव (< ₹500 या > ₹20,000)",
                                                color = MandiAmberDark,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MandiBorder,
                                    unfocusedBorderColor = MandiBorder
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Lot Subtotal Line
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MandiSurfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isEnglish) "Weight: ${MandiMathEngine.gramsToQuintals(lotResult.totalWeightGrams)} qtl" else "वजन: ${MandiMathEngine.gramsToQuintals(lotResult.totalWeightGrams)} क्विंटल",
                                    fontSize = 12.sp,
                                    color = MandiTextSecondary
                                )
                                Text(
                                    text = if (isEnglish) "Gross: ₹${MandiMathEngine.paisaToRupeesString(lotResult.grossPaisa)}" else "सकल: ₹${MandiMathEngine.paisaToRupeesString(lotResult.grossPaisa)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MandiTextPrimary
                                )
                            }
                        }
                    }
                }

                // Add Another Crop Lot Button
                MandiSecondaryButton(
                    text = if (isEnglish) "Add Another Crop Lot" else "अन्य फसल / वैरायटी जोड़ें",
                    leadingIcon = Icons.Default.Add,
                    onClick = { formState.addLot() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Section 3: Deductions & Optional Advance
            MandiSection(title = if (isEnglish) "Deductions & Advance" else "कटौती एवं अग्रिम") {
                MandiCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = formState.commissionPercent,
                            onValueChange = { formState.commissionPercent = it },
                            label = { Text(if (isEnglish) "Commission %" else "कमीशन %", fontSize = 13.sp) },
                            supportingText = if (formState.commissionPercent != defaultComm) {
                                { Text(if (isEnglish) "Default $defaultComm%" else "डिफ़ॉल्ट $defaultComm%", color = MandiAmberDark, fontSize = 11.sp) }
                            } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MandiBorder,
                                unfocusedBorderColor = MandiBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = formState.labourPerBag,
                            onValueChange = { formState.labourPerBag = it },
                            label = { Text(if (isEnglish) "Labour (₹/bag)" else "मजदूरी (रु/बोरी)", fontSize = 13.sp) },
                            supportingText = if (formState.labourPerBag != defaultLabour) {
                                { Text(if (isEnglish) "Default ₹$defaultLabour" else "डिफ़ॉल्ट ₹$defaultLabour", color = MandiAmberDark, fontSize = 11.sp) }
                            } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MandiBorder,
                                unfocusedBorderColor = MandiBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // For editing existing slips with stored tare only
                    if (existingId != null && formState.tareQuintals.isNotBlank() && formState.tareQuintals != "0") {
                        OutlinedTextField(
                            value = formState.tareQuintals,
                            onValueChange = { formState.tareQuintals = it },
                            label = { Text(if (isEnglish) "Tare / Bardana (qtl)" else "काट / बारदाना (कुंतल)", fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MandiBorder,
                                unfocusedBorderColor = MandiBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = formState.advancePaid,
                        onValueChange = { formState.advancePaid = it },
                        label = { Text(if (isEnglish) "Advance Paid (₹)" else "अग्रिम / पेशगी (रु)", fontSize = 13.sp) },
                        placeholder = { Text("0") },
                        supportingText = { Text(if (isEnglish) "Optional" else "वैकल्पिक", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MandiBorder,
                            unfocusedBorderColor = MandiBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Section 4: Live Parcha Receipt Summary
            MandiSection(title = if (isEnglish) "Slip Summary" else "हिसाब पर्ची") {
                MandiCard {
                    preview.lotsCalculations.forEachIndexed { idx, pair ->
                        val lot = pair.first
                        val res = pair.second
                        if (res.grossPaisa > 0) {
                            val bagsSuffix = if (isEnglish) "bags" else "बोरी"
                            val lotLabel = if (isEnglish) "Lot #${idx + 1}: ${lot.cropName} (${MandiMathEngine.formatIndianGrouping(res.bags)} $bagsSuffix)" else "फसल #${idx + 1}: ${lot.cropName} (${MandiMathEngine.formatIndianGrouping(res.bags)} $bagsSuffix)"
                            MandiMoneyRow(
                                label = lotLabel,
                                amountText = MandiMathEngine.formatRupees(res.grossPaisa)
                            )
                        }
                    }

                    val bagsUnit = if (isEnglish) "bags" else "बोरी"
                    val qtlUnit = if (isEnglish) "qtl" else "क्विंटल"
                    MandiMoneyRow(label = if (isEnglish) "Total Bags" else "कुल बोरी", amountText = "${MandiMathEngine.formatIndianGrouping(preview.totalBags)} $bagsUnit")
                    if (formState.tareQuintals.isNotBlank() && formState.tareQuintals != "0") {
                        MandiMoneyRow(label = if (isEnglish) "Tare / Bardana" else "काट / बारदाना", amountText = "- ${formState.tareQuintals} $qtlUnit")
                    }
                    MandiMoneyRow(label = if (isEnglish) "Total Weight" else "कुल वजन", amountText = "${MandiMathEngine.gramsToQuintals(preview.totalWeightGrams)} $qtlUnit")
                    MandiMoneyRow(label = if (isEnglish) "Gross Amount" else "सकल राशि", amountText = MandiMathEngine.formatRupees(preview.totalGrossPaisa))
                    // B final rule: Labour sits above Commission
                    MandiMoneyRow(label = if (isEnglish) "Labour" else "मजदूरी कटौती", amountText = "- ₹${MandiMathEngine.paisaToRupeesString(preview.labourPaisa)}")
                    MandiMoneyRow(label = if (isEnglish) "Commission" else "कमीशन कटौती", amountText = "- ₹${MandiMathEngine.paisaToRupeesString(preview.commissionPaisa)}")
                    if (preview.advancePaidPaisa > 0) {
                        MandiMoneyRow(label = if (isEnglish) "Advance Paid" else "अग्रिम / पेशगी", amountText = "- ₹${MandiMathEngine.paisaToRupeesString(preview.advancePaidPaisa)}")
                    }

                    Spacer(Modifier.height(4.dp))
                    val netDisplay = if (formState.lots.any { it.ratePerQuintal.isBlank() || it.ratePerQuintal == "0" }) {
                        if (isEnglish) "Enter Rate" else "भाव दर्ज करें"
                    } else {
                        MandiMathEngine.formatRupees(preview.netPayablePaisa)
                    }
                    MandiMoneyRow(
                        label = if (isEnglish) "Slip Value" else "पर्ची राशि",
                        amountText = netDisplay,
                        emphasized = true
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }

    // Confirmation & Discard Dialogs
    newPartyCheck?.let { check ->
        NewPartyConfirmDialog(
            check = check,
            isEnglish = isEnglish,
            onCreate = { newPartyCheck = null; performSave() },
            onDismiss = { newPartyCheck = null },
        )
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
                    commit(pending)
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
                    text = if (isEnglish) "Unusual Value Confirmation" else "असामान्य प्रविष्टि पुष्टि",
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
                        commit(entry)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MandiPrimaryAction, contentColor = MandiPrimaryActionText)
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
                    Text(if (isEnglish) "Edit" else "सुधारें", color = MandiTextPrimary)
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
}
