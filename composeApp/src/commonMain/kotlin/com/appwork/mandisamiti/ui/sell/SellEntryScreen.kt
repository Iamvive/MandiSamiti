package com.appwork.mandisamiti.ui.sell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendStatus
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.model.TradeSettings
import com.appwork.mandisamiti.domain.repository.SellRepository
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.buy.BuyPartyResolver
import com.appwork.mandisamiti.ui.components.DiscardDraftDialog
import com.appwork.mandisamiti.ui.components.DuplicateEntryDialog
import com.appwork.mandisamiti.ui.components.MandiCard
import com.appwork.mandisamiti.ui.components.MandiChip
import com.appwork.mandisamiti.ui.components.MandiFlatTextField
import com.appwork.mandisamiti.ui.components.MandiLinesCard
import com.appwork.mandisamiti.ui.components.MandiPartyLookupField
import com.appwork.mandisamiti.ui.components.MandiPrimaryButton
import com.appwork.mandisamiti.ui.components.MandiScreenHeader
import com.appwork.mandisamiti.ui.components.MandiSecondaryButton
import com.appwork.mandisamiti.ui.components.NewPartyConfirmDialog
import com.appwork.mandisamiti.ui.components.TypedPartyCheck
import com.appwork.mandisamiti.ui.components.checkTypedParty
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import kotlinx.datetime.Clock

private fun String.errorIfFilled(error: String?): String? = if (isBlank()) null else error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellEntryScreen(
    shopId: String = "",
    existingId: String? = null,
    onSaved: (SellEntry, Party?) -> Unit,
    onBack: () -> Unit,
    tradeSettings: TradeSettings? = null,
    formState: SellEntryFormState = remember(tradeSettings) { SellEntryFormState(tradeSettings) },
    availableStockGrams: Long? = null,
    stockLines: List<com.appwork.mandisamiti.domain.model.StockLine> = emptyList(),
    sellRepository: SellRepository? = null,
    parties: List<Party> = emptyList(),
    lends: List<LendEntry> = emptyList(),
    cashTransactions: List<CashTransaction> = emptyList(),
    isEnglish: Boolean = false,
    modifier: Modifier = Modifier
) {
    var existingEntry by remember { mutableStateOf<SellEntry?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showSanityConfirmation by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var newPartyCheck by remember { mutableStateOf<TypedPartyCheck.NeedsConfirm?>(null) }
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
            val entry = sellRepository.getSellEntryById(existingId)
            if (entry != null) {
                existingEntry = entry
                formState.populateFrom(entry)
            }
        }
    }

    val commit: (SellEntry) -> Unit = { raw ->
        val existing = BuyPartyResolver.existing(parties, formState.selectedBuyerId, formState.phone)
            ?: parties.firstOrNull { it.id == raw.buyerId }
        val party = SellPartyResolver.partyToSave(formState, shopId, existing, Clock.System.now().toEpochMilliseconds())
        val partyId = existing?.id ?: party?.id
        onSaved(if (partyId != null) raw.copy(buyerId = partyId) else raw, party)
    }

    val performSave: () -> Unit = {
        val entry = formState.toSellEntry(existingId = existingId, shopId = shopId)
        val now = Clock.System.now().toEpochMilliseconds()
        val isDuplicate = existingId == null && recentSells.any { prev ->
            val isSameParty = prev.buyerId.trim().equals(entry.buyerId.trim(), ignoreCase = true)
            val isSameCommodity = prev.commodityId.trim().equals(entry.commodityId.trim(), ignoreCase = true)
            val isSameBags = prev.bagsCount == entry.bagsCount
            val isSameAmount = prev.totalReceivablePaisa == entry.totalReceivablePaisa
            val isRecent = kotlin.math.abs(now - prev.createdAt) <= 2 * 60 * 1000L
            !prev.isVoid && isSameParty && isSameCommodity && isSameBags && isSameAmount && isRecent
        }

        if (isDuplicate) {
            pendingEntryToSave = entry
            showDuplicateDialog = true
        } else if (formState.hasSanityWarning() && !showSanityConfirmation) {
            pendingEntryToSave = entry
            showSanityConfirmation = true
        } else {
            commit(entry)
        }
    }

    val triggerSave: () -> Unit = {
        val typedCheck = checkTypedParty(formState.buyerName, formState.selectedBuyerId, parties)
        if (typedCheck is TypedPartyCheck.NeedsConfirm) {
            newPartyCheck = typedCheck
        } else {
            performSave()
        }
    }

    val preview = formState.aggregatePreview()
    val isBos = formState.billMode == BillMode.BILL_OF_SUPPLY && formState.partyKind == SellPartyKind.MERCHANT
    val lotErrors = formState.validateLots()
    val canSave = formState.canSave()
    val validationReason = formState.validationReason()

    val existingParty = BuyPartyResolver.existing(parties, formState.selectedBuyerId, formState.phone)
    val isOldPartyWithoutPhone = formState.selectedBuyerId != null && formState.phone.isBlank()

    Scaffold(
        topBar = {
            MandiScreenHeader(
                title = if (existingId != null) {
                    if (isEnglish) "Edit Sale" else "बिक्री पर्ची संशोधन"
                } else {
                    if (isEnglish) "New Sale" else "नई बिक्री सौदा"
                },
                subtitle = existingEntry?.invoiceNumber ?: existingEntry?.slipNo?.let { "#$it" },
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
            // Direction B final: Always-visible sticky action dock
            Surface(
                color = MandiSurface,
                border = BorderStroke(1.dp, MandiBorder),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
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
                        Text(
                            text = if (isEnglish) "Balance receivable" else "बाकी लेना (व्यापारी से)",
                            fontSize = 14.sp,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = MandiMathEngine.formatRupees(preview.balanceDuePaisa),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                    }

                    val saveButtonHint = if (formState.buyerName.isBlank() || formState.village.isBlank()) {
                        if (isEnglish) "Add name and village to save" else "नाम और गांव डालने पर सुरक्षित होगा"
                    } else if (formState.phone.length < 10) {
                        if (isEnglish) "Enter 10-digit mobile number" else "10 अंकों का मोबाइल नंबर डालें"
                    } else {
                        validationReason ?: (if (isEnglish) "Add name and village to save" else "नाम और गांव डालने पर सुरक्षित होगा")
                    }

                    val buttonText = if (canSave) {
                        if (isEnglish) {
                            if (existingId == null) "Save" else "Save changes"
                        } else {
                            if (existingId == null) "सुरक्षित करें" else "बदलाव सुरक्षित करें"
                        }
                    } else {
                        saveButtonHint
                    }

                    MandiPrimaryButton(
                        text = buttonText,
                        height = 56.dp,
                        enabled = canSave,
                        onClick = triggerSave,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MandiBackground)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Category Selector: Customer vs Merchant
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isEnglish) "Buyer Category" else "खरीदार प्रकार",
                    fontSize = 12.sp,
                    color = MandiTextSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MandiChip(
                        label = if (isEnglish) "Customer (Retail)" else "ग्राहक (Customer)",
                        selected = formState.partyKind == SellPartyKind.CUSTOMER,
                        onClick = { formState.setParty(SellPartyKind.CUSTOMER) }
                    )
                    MandiChip(
                        label = if (isEnglish) "Merchant (Wholesale)" else "व्यापारी (Merchant)",
                        selected = formState.partyKind == SellPartyKind.MERCHANT,
                        onClick = { formState.setParty(SellPartyKind.MERCHANT) }
                    )
                }
            }

            // Invoice Mode: Normal Slip vs Bill of Supply
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isEnglish) "Invoice Mode" else "बिल प्रकार",
                    fontSize = 12.sp,
                    color = MandiTextSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MandiChip(
                        label = if (isEnglish) "Normal Slip" else "सामान्य बिल (Normal)",
                        selected = formState.billMode == BillMode.NORMAL,
                        onClick = { formState.setMode(BillMode.NORMAL) }
                    )
                    MandiChip(
                        label = if (isEnglish) "Bill of Supply" else "सप्लाई बिल (Bill of Supply)",
                        selected = formState.billMode == BillMode.BILL_OF_SUPPLY,
                        onClick = {
                            if (formState.partyKind == SellPartyKind.MERCHANT) {
                                formState.setMode(BillMode.BILL_OF_SUPPLY)
                            }
                        }
                    )
                }
                if (formState.partyKind == SellPartyKind.CUSTOMER) {
                    Text(
                        text = if (isEnglish) "Bill of Supply is only for Merchants" else "सप्लाई बिल केवल व्यापारियों हेतु मान्य है",
                        fontSize = 11.sp,
                        color = MandiTextSecondary
                    )
                }
            }

            // Section 1: Buyer block (Phone-first lookup, Name, Village, and BOS fields)
            MandiPartyLookupField(
                phoneValue = formState.phone,
                onPhoneChange = { formState.updatePhone(it) },
                onPartySelected = { party ->
                    SellPartyResolver.pick(formState, party)
                },
                parties = parties,
                label = if (isEnglish) "Mobile number" else "मोबाइल नंबर",
                placeholder = if (isOldPartyWithoutPhone) (if (isEnglish) "Enter 10-digit number" else "10 अंकों का नंबर डालें") else null,
                isError = formState.phone.errorIfFilled(formState.phoneValidation.errorOrNull) != null,
                errorMessage = formState.phone.errorIfFilled(formState.phoneValidation.errorOrNull)
            )

            MandiFlatTextField(
                value = formState.buyerName,
                onValueChange = { formState.buyerName = it },
                label = if (isEnglish) {
                    "Buyer Name"
                } else if (formState.partyKind == SellPartyKind.MERCHANT) {
                    "व्यापारी / फर्म का नाम"
                } else {
                    "ग्राहक का नाम"
                },
                isError = formState.buyerName.errorIfFilled(formState.buyerNameValidation.errorOrNull) != null,
                errorMessage = formState.buyerName.errorIfFilled(formState.buyerNameValidation.errorOrNull),
                isTall = true,
                modifier = Modifier.fillMaxWidth()
            )

            MandiFlatTextField(
                value = formState.village,
                onValueChange = { formState.village = it },
                label = if (isEnglish) "Village / City" else "गाँव / शहर",
                isError = formState.village.errorIfFilled(formState.villageValidation.errorOrNull) != null,
                errorMessage = formState.village.errorIfFilled(formState.villageValidation.errorOrNull),
                isTall = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Buyer Source Hint Line
            if (isOldPartyWithoutPhone) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MandiAmberDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isEnglish) "10-digit mobile number required before saving" else "सुरक्षित करने से पहले 10 अंकों का नंबर दर्ज करें",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiTextPrimary
                    )
                }
            } else if (existingParty != null && formState.phone.isNotBlank()) {
                Text(
                    text = if (isEnglish) {
                        "Filled from the saved number. Changing them updates this account everywhere."
                    } else {
                        "सेव नंबर से नाम और गांव भरे। बदलने पर यह खाते में हर जगह बदलेगा।"
                    },
                    fontSize = 12.sp,
                    color = MandiTextSecondary
                )
            } else if (existingParty == null && formState.phone.isNotBlank()) {
                Text(
                    text = if (isEnglish) {
                        "New number. The account is created when the entry is saved."
                    } else {
                        "नया नंबर। एंट्री सेव होते ही खाता बन जाएगा।"
                    },
                    fontSize = 12.sp,
                    color = MandiTextSecondary
                )
            }

            // BOS specific details
            if (isBos) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MandiFlatTextField(
                        value = formState.gstin,
                        onValueChange = { formState.gstin = it.uppercase() },
                        label = if (isEnglish) "Buyer GSTIN (15 chars)" else "क्रेता GSTIN (15 अंक)",
                        isError = formState.gstin.errorIfFilled(formState.gstinValidation.errorOrNull) != null,
                        errorMessage = formState.gstin.errorIfFilled(formState.gstinValidation.errorOrNull),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        ),
                        isTall = true,
                        modifier = Modifier.weight(2f)
                    )
                    MandiFlatTextField(
                        value = formState.stateCode,
                        onValueChange = { formState.stateCode = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = if (isEnglish) "State" else "राज्य कोड",
                        isError = formState.stateCode.errorIfFilled(formState.stateCodeValidation.errorOrNull) != null,
                        errorMessage = formState.stateCode.errorIfFilled(formState.stateCodeValidation.errorOrNull),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        isTall = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section 2: Crop Lots / Varieties
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isEnglish) "Crop / variety" else "जिंस / फसल वैरायटी",
                    fontSize = 12.sp,
                    color = MandiTextSecondary
                )

                val commonCrops = if (isEnglish) {
                    listOf("Wheat", "Wheat Sharbati", "Gram", "Mustard", "Soybean")
                } else {
                    listOf("गेहूं", "गेहूं शरबती", "चना", "सरसों", "सोयाबीन")
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    val currentLot = formState.lots.firstOrNull()
                    commonCrops.forEach { crop ->
                        MandiChip(
                            label = crop,
                            selected = currentLot?.cropName == crop,
                            onClick = { currentLot?.cropName = crop }
                        )
                    }
                }
            }

            // Crop input lines
            formState.lots.forEachIndexed { index, lot ->
                val v = lotErrors.getOrNull(index)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (formState.lots.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnglish) "Lot #${index + 1}: ${lot.cropName}" else "लॉट #${index + 1}: ${lot.cropName}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiTextPrimary
                            )
                            IconButton(
                                onClick = { formState.removeLot(index) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = if (isEnglish) "Remove Lot" else "लॉट हटाएं",
                                    tint = MandiTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    MandiFlatTextField(
                        value = lot.cropName,
                        onValueChange = { lot.cropName = it },
                        label = if (isEnglish) {
                            "Crop / variety${if (formState.lots.size > 1) " #${index + 1}" else ""}"
                        } else {
                            "जिंस / फसल वैरायटी${if (formState.lots.size > 1) " #${index + 1}" else ""}"
                        },
                        placeholder = if (isEnglish) "Enter crop name" else "फसल का नाम लिखें",
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        isTall = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Line 1: Bags, Weight/Bag, Extra weight in a 3-column row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MandiFlatTextField(
                            value = lot.bags,
                            onValueChange = { lot.bags = it.filter { ch -> ch.isDigit() }.take(4) },
                            label = if (isEnglish) "Bags" else "बोरी संख्या",
                            isError = lot.bags.errorIfFilled(v?.bagsError) != null,
                            errorMessage = lot.bags.errorIfFilled(v?.bagsError),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            isTall = true,
                            modifier = Modifier.weight(1f)
                        )
                        MandiFlatTextField(
                            value = lot.weightPerBagKg,
                            onValueChange = { lot.weightPerBagKg = it.filter { ch -> ch.isDigit() }.take(3) },
                            label = if (isEnglish) "Kg/bag" else "वजन/बोरी (किग्रा)",
                            isError = lot.weightPerBagKg.errorIfFilled(v?.weightError) != null,
                            errorMessage = lot.weightPerBagKg.errorIfFilled(v?.weightError),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            isTall = true,
                            modifier = Modifier.weight(1f)
                        )
                        MandiFlatTextField(
                            value = lot.extraWeightKg,
                            onValueChange = { lot.extraWeightKg = it },
                            label = if (isEnglish) "Extra (kg)" else "अतिरिक्त (किग्रा)",
                            placeholder = "0",
                            isError = lot.extraWeightKg.errorIfFilled(v?.extraError) != null,
                            errorMessage = lot.extraWeightKg.errorIfFilled(v?.extraError),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            isTall = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Line 2: Rate on next line
                    MandiFlatTextField(
                        value = lot.ratePerQuintal,
                        onValueChange = { lot.ratePerQuintal = it.filter { ch -> ch.isDigit() }.take(6) },
                        label = if (isEnglish) "Rate (₹/qtl)" else "भाव (रु/क्विंटल)",
                        isError = lot.ratePerQuintal.errorIfFilled(v?.rateError) != null,
                        errorMessage = lot.ratePerQuintal.errorIfFilled(v?.rateError),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        isTall = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Line 3: Subtotal row
                    val lotCrop = lot.toCropLot()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) {
                                "Weight: ${MandiMathEngine.gramsToQuintals(lotCrop.totalWeightGrams)} qtl"
                            } else {
                                "वजन: ${MandiMathEngine.gramsToQuintals(lotCrop.totalWeightGrams)} क्विंटल"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MandiTextPrimary
                        )
                        Text(
                            text = MandiMathEngine.formatRupees(lotCrop.grossPaisa),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiPrimaryAction
                        )
                    }
                }
            }

            // Add lot button
            MandiSecondaryButton(
                text = if (isEnglish) "+ Add another crop lot" else "+ दूसरी जिंस / फसल जोड़ें",
                onClick = { formState.addLot() },
                modifier = Modifier.fillMaxWidth()
            )

            // Section 3: Deductions & Charges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MandiFlatTextField(
                    value = formState.commissionPercent,
                    onValueChange = { formState.commissionPercent = it },
                    label = if (isEnglish) "Commission %" else "कमीशन %",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    isTall = true,
                    modifier = Modifier.weight(1f)
                )
                MandiFlatTextField(
                    value = formState.labourPerBag,
                    onValueChange = { formState.labourPerBag = it },
                    label = if (isEnglish) "Labour (₹/bag)" else "मजदूरी (रु/बोरी)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    isTall = true,
                    modifier = Modifier.weight(1f)
                )
            }

            if (isBos) {
                MandiFlatTextField(
                    value = formState.tulakPerQuintal,
                    onValueChange = { formState.tulakPerQuintal = it },
                    label = if (isEnglish) "Tulak (₹/qtl)" else "तुलाई (रु/क्विंटल)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    isTall = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Section 4: हिसाब पर्ची (Slip summary card with lines)
            Text(
                text = if (isEnglish) "Slip" else "हिसाब पर्ची",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MandiTextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )

            MandiLinesCard {
                formState.lots.forEachIndexed { i, lot ->
                    val lotCrop = lot.toCropLot()
                    Line(
                        label = if (isEnglish) "Lot #${i + 1}: ${lot.cropName}, ${lot.bags} bags" else "फसल #${i + 1}: ${lot.cropName}, ${lot.bags} बोरी",
                        amountText = MandiMathEngine.formatRupees(lotCrop.grossPaisa)
                    )
                }
                if (!isBos) {
                    if (preview.commissionPaisa > 0) {
                        Line(
                            label = if (isEnglish) "Commission" else "कमीशन जोड़ (+)",
                            amountText = "+ ₹${MandiMathEngine.paisaToRupeesString(preview.commissionPaisa)}"
                        )
                    }
                    if (preview.labourPaisa > 0) {
                        Line(
                            label = if (isEnglish) "Labour" else "मजदूरी जोड़ (+)",
                            amountText = "+ ₹${MandiMathEngine.paisaToRupeesString(preview.labourPaisa)}"
                        )
                    }
                    if (preview.tulakPaisa > 0) {
                        Line(
                            label = if (isEnglish) "Tulak" else "तुलाई (+)",
                            amountText = "+ ₹${MandiMathEngine.paisaToRupeesString(preview.tulakPaisa)}"
                        )
                    }
                } else {
                    Line(
                        label = if (isEnglish) "Mandi Tax (1.5%)" else "मंडी शुल्क (1.5%)",
                        amountText = "+ ₹${MandiMathEngine.paisaToRupeesString(preview.mandiShulkPaisa)}"
                    )
                    if (preview.krishakCessPaisa > 0) {
                        Line(
                            label = if (isEnglish) "Krishak Cess (0.5%)" else "कृषक कल्याण उपकर (0.5%)",
                            amountText = "+ ₹${MandiMathEngine.paisaToRupeesString(preview.krishakCessPaisa)}"
                        )
                    }
                    if (preview.labourPaisa > 0) {
                        Line(
                            label = if (isEnglish) "Hammali / Labour" else "हमाली / मजदूरी",
                            amountText = "+ ₹${MandiMathEngine.paisaToRupeesString(preview.labourPaisa)}"
                        )
                    }
                    if (preview.tulakPaisa > 0) {
                        Line(
                            label = if (isEnglish) "Tulak" else "तुलाई (Tulak)",
                            amountText = "+ ₹${MandiMathEngine.paisaToRupeesString(preview.tulakPaisa)}"
                        )
                    }
                }
                TotalLine(
                    label = if (isEnglish) "Total Receivable" else "कुल देय राशि (Total Receivable)",
                    amountText = MandiMathEngine.formatRupees(preview.totalReceivablePaisa),
                    largeAmount = true
                )
            }

            // Section 5: पर्ची के बाद का हिसाब (After the slip)
            Text(
                text = if (isEnglish) "After the slip" else "पर्ची के बाद का हिसाब",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MandiTextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Advance field
            MandiFlatTextField(
                value = formState.advanceReceived,
                onValueChange = { formState.advanceReceived = it.filter { ch -> ch.isDigit() } },
                label = if (isEnglish) "Advance received (₹)" else "अग्रिम / प्राप्त राशि (रु)",
                placeholder = if (isEnglish) "Optional" else "वैकल्पिक",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                isTall = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Read-only open loan balance card (no adjustment)
            if (formState.loanDebtPaisa > 0L) {
                MandiCard(padding = 14.dp, spacing = 8.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) "Open loan" else "पुराना उधार",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MandiTextPrimary
                        )
                        Text(
                            text = MandiMathEngine.formatRupees(formState.loanDebtPaisa),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                    }
                    Text(
                        text = if (isEnglish) {
                            "Note: Sell slip does not alter loan balance."
                        } else {
                            "नोट: बिक्री पर्ची से पुराने उधार में कोई कटौती नहीं होती।"
                        },
                        fontSize = 12.sp,
                        color = MandiTextSecondary
                    )
                }
            }

            // Read-only deposit balance card (no adjustment)
            if (formState.depositPaisa > 0L) {
                MandiCard(padding = 14.dp, spacing = 8.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) "Deposit with us" else "हमारे पास जमा",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MandiTextPrimary
                        )
                        Text(
                            text = MandiMathEngine.formatRupees(formState.depositPaisa),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                    }
                    Text(
                        text = if (isEnglish) {
                            "Note: Deposit remains safe in account (per decision, does not reduce slip)."
                        } else {
                            "नोट: खाते में जमा सुरक्षित है (निर्णय अनुसार, पर्ची में कटौती नहीं होती)।"
                        },
                        fontSize = 12.sp,
                        color = MandiTextSecondary
                    )
                }
            }

            // Summary Lines (After the slip calculation card)
            MandiLinesCard {
                Line(
                    label = if (isEnglish) "Slip value" else "पर्ची राशि",
                    amountText = MandiMathEngine.formatRupees(preview.totalReceivablePaisa)
                )
                if (preview.advanceReceivedPaisa > 0L) {
                    Line(
                        label = if (isEnglish) "Advance received" else "अग्रिम / प्राप्त राशि",
                        amountText = "− ₹${MandiMathEngine.paisaToRupeesString(preview.advanceReceivedPaisa)}"
                    )
                }
                TotalLine(
                    label = if (isEnglish) "Balance receivable" else "बाकी लेना (व्यापारी से)",
                    amountText = MandiMathEngine.formatRupees(preview.balanceDuePaisa),
                    largeAmount = true
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Confirmation & Discard Dialogs
    newPartyCheck?.let { check ->
        NewPartyConfirmDialog(
            check = check,
            isEnglish = isEnglish,
            onCreate = { newPartyCheck = null; performSave() },
            onDismiss = { newPartyCheck = null }
        )
    }

    if (showDuplicateDialog && pendingEntryToSave != null) {
        val pending = pendingEntryToSave!!
        DuplicateEntryDialog(
            details = "${formState.buyerName} • ${pending.commodityId} • ${pending.bagsCount} बोरी • ₹${MandiMathEngine.paisaToRupeesString(pending.totalReceivablePaisa)}",
            onConfirm = {
                showDuplicateDialog = false
                commit(pending)
            },
            onDismiss = {
                showDuplicateDialog = false
                pendingEntryToSave = null
            }
        )
    }

    if (showSanityConfirmation && pendingEntryToSave != null) {
        val pending = pendingEntryToSave!!
        AlertDialog(
            onDismissRequest = { showSanityConfirmation = false },
            title = {
                Text(
                    text = if (isEnglish) "Value Limit Warning" else "सावधानी: असामान्य प्रविष्टि",
                    fontWeight = FontWeight.Bold,
                    color = MandiAmberDark
                )
            },
            text = {
                Text(
                    text = formState.sanityWarningMessage()
                        ?: (if (isEnglish) "Some values look unusually high or low. Continue saving?" else "कुछ मान सामान्य सीमा से बाहर हैं। क्या आप सुरक्षित करना चाहते हैं?")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSanityConfirmation = false
                        commit(pending)
                    }
                ) {
                    Text(if (isEnglish) "Save Anyway" else "फिर भी सुरक्षित करें", color = MandiAmberDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSanityConfirmation = false }) {
                    Text(if (isEnglish) "Edit Details" else "संशोधन करें")
                }
            }
        )
    }

    if (showDiscardDialog) {
        DiscardDraftDialog(
            onConfirmDiscard = {
                showDiscardDialog = false
                onBack()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }
}
