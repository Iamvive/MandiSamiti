package com.appwork.mandisamiti.ui.buy

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.repository.BuyRepository
import com.appwork.mandisamiti.platform.MandiBackHandler
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
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import kotlinx.datetime.Clock

/**
 * BuyEntryScreen adhering strictly to Direction B final & NGDL guardrails:
 * - Header: "नई खरीद एंट्री", no subtitle.
 * - Flat grey-filled boxes for fields (MandiSurfaceElevated, 56dp tall, 20sp text, 8dp squircle).
 * - Farmer block: "मोबाइल नंबर या नाम" lookup first, then farmer name, then village.
 *   Hint line below for saved number, new number, or missing number on old party.
 * - Save stays off until there is a valid number.
 * - Lots: Bags, kg/bag and extra weight on one line (grid3), rate on the next line.
 * - Flat sections: no bordered cards around farmer or crop details.
 * - Slip: हिसाब पर्ची with lines (Lot lines, Labour, Commission, Slip value).
 * - After slip: पर्ची के बाद का हिसाब (Advance, Loan card, Deposit card, Summary lines).
 * - Always-visible sticky action dock: live final payable amount and "सुरक्षित करें" button.
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

    val actions = remember(formState, shopId, existingId, existingEntry, parties) {
        buyEntryActions(
            formState = formState,
            onSaveTriggered = {
                val typedCheck = checkTypedParty(formState.farmerName, formState.selectedFarmerId, parties)
                if (typedCheck is TypedPartyCheck.NeedsConfirm) {
                    newPartyCheck = typedCheck
                } else {
                    performSave()
                }
            }
        )
    }

    val uiState = formState.toUiState()

    BuyEntryScreenContent(
        state = uiState,
        actions = actions,
        existingId = existingId,
        parties = parties,
        isEnglish = isEnglish,
        isDirty = isDirty,
        onDiscardRequest = { showDiscardDialog = true },
        onBack = onBack,
        modifier = modifier
    )

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
                }
            },
            onDismiss = { showDuplicateDialog = false; pendingEntryToSave = null }
        )
    }

    if (showRateOutlierConfirmation && pendingEntryToSave != null) {
        val pending = pendingEntryToSave!!
        AlertDialog(
            onDismissRequest = { showRateOutlierConfirmation = false; pendingEntryToSave = null },
            title = { Text(if (isEnglish) "Unusual Rate Warning" else "असामान्य भाव की चेतावनी") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isEnglish) "The entered rate or weight is unusually high or low:" else "दर्ज किया गया भाव या वजन सामान्य सीमा से बाहर है:"
                    )
                    formState.sanityWarningMessage()?.let { msg ->
                        Text(msg, color = MandiAmberDark, fontSize = 13.sp)
                    }
                    Text(if (isEnglish) "Do you want to proceed with this purchase?" else "क्या आप इस सौदे को सुरक्षित करना चाहते हैं?")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRateOutlierConfirmation = false
                        commit(pending)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MandiPrimaryAction)
                ) {
                    Text(if (isEnglish) "Yes, Proceed" else "हाँ, आगे बढ़ें", color = MandiPrimaryActionText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRateOutlierConfirmation = false; pendingEntryToSave = null }) {
                    Text(if (isEnglish) "Review Again" else "जाँचें", color = MandiTextSecondary)
                }
            }
        )
    }

    if (showDiscardDialog) {
        DiscardDraftDialog(
            title = if (isEnglish) "Discard draft?" else "बदलाव रद्द करें?",
            message = if (isEnglish) "Do you want to exit without saving? Entered details will be lost." else "क्या आप बिना सुरक्षित किए बाहर जाना चाहते हैं? भरी गई जानकारी मिट जाएगी।",
            confirmText = if (isEnglish) "Yes, Exit" else "हाँ, बाहर निकलें",
            dismissText = if (isEnglish) "Continue" else "भरना जारी रखें",
            onConfirmDiscard = {
                showDiscardDialog = false
                formState.farmerName = ""
                formState.phone = ""
                formState.village = ""
                formState.selectedFarmerId = null
                onBack()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }
}

/**
 * Pure presentation composable that renders [BuyEntryUiState] according to Direction B final.
 */
@Composable
fun BuyEntryScreenContent(
    state: BuyEntryUiState,
    actions: BuyEntryActions,
    existingId: String?,
    parties: List<Party>,
    isEnglish: Boolean,
    isDirty: Boolean,
    onDiscardRequest: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            MandiScreenHeader(
                title = if (isEnglish) {
                    if (existingId == null) "New purchase" else "Edit purchase"
                } else {
                    if (existingId == null) "नई खरीद एंट्री" else "खरीद बदलें"
                },
                subtitle = null,
                onBack = {
                    if (isDirty) {
                        onDiscardRequest()
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
                            text = if (isEnglish) "Final payable" else "अंतिम देय राशि",
                            fontSize = 14.sp,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = MandiMathEngine.formatRupees(state.payoutPaisa),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                    }

                    if (state.canSave) {
                        MandiPrimaryButton(
                            text = if (isEnglish) {
                                if (existingId == null) "Save" else "Save changes"
                            } else {
                                if (existingId == null) "सुरक्षित करें" else "बदलाव सुरक्षित करें"
                            },
                            height = 56.dp,
                            fontSize = 16.sp,
                            onClick = { actions.onSave() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val disabledText = if (state.party.needsPhonePrompt || state.party.phone.isBlank()) {
                            if (isEnglish) "Add number to save" else "नंबर डालने पर सुरक्षित होगा"
                        } else if (state.party.name.isBlank() || state.party.village.isBlank()) {
                            if (isEnglish) "Add name and village to save" else "नाम और गांव डालने पर सुरक्षित होगा"
                        } else {
                            state.saveBlockedReason ?: if (isEnglish) "Save" else "सुरक्षित करें"
                        }

                        MandiSecondaryButton(
                            text = disabledText,
                            enabled = false,
                            height = 56.dp,
                            fontSize = 16.sp,
                            onClick = {},
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize().background(MandiBackground)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section 1: Farmer block (Flat inputs, order: Mobile/Name lookup, Farmer name, Village)
            val existingParty = BuyPartyResolver.existing(parties, state.party.selectedPartyId, state.party.phone)
            val isOldPartyWithoutPhone = state.party.selectedPartyId != null && state.party.phone.isBlank()

            MandiPartyLookupField(
                phoneValue = state.party.phone,
                onPhoneChange = { actions.onPhoneChange(it) },
                onPartySelected = { party ->
                    actions.onPickParty(party.id)
                    actions.onNameChange(party.name)
                    party.phone?.let { actions.onPhoneChange(it) }
                    party.village?.let { actions.onVillageChange(it) }
                },
                parties = parties,
                label = if (isEnglish) "Mobile number" else "मोबाइल नंबर",
                placeholder = if (isOldPartyWithoutPhone) (if (isEnglish) "Enter 10-digit number" else "10 अंकों का नंबर डालें") else null,
                isError = state.party.phoneError != null,
                errorMessage = state.party.phoneError
            )

            MandiFlatTextField(
                value = state.party.name,
                onValueChange = { actions.onNameChange(it) },
                label = if (isEnglish) "Farmer name" else "किसान का नाम",
                placeholder = if (isEnglish) "Type name" else "नाम लिखें",
                isError = state.party.nameError != null,
                errorMessage = state.party.nameError,
                isTall = true,
                modifier = Modifier.fillMaxWidth()
            )

            MandiFlatTextField(
                value = state.party.village,
                onValueChange = { actions.onVillageChange(it) },
                label = if (isEnglish) "Village" else "गांव",
                placeholder = if (isEnglish) "Type village" else "गांव लिखें",
                isError = state.party.villageError != null,
                errorMessage = state.party.villageError,
                isTall = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Farmer Hint Line
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
                        text = if (isEnglish) "This account has no number. Add one before saving." else "इस खाते में नंबर नहीं है। सेव से पहले नंबर डालें।",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiTextPrimary
                    )
                }
            } else if (existingParty != null && state.party.phone.isNotBlank()) {
                Text(
                    text = if (isEnglish) "Filled from the saved number. Changing them updates this account everywhere." else "सेव नंबर से नाम और गांव भरे। बदलने पर यह खाते में हर जगह बदलेगा।",
                    fontSize = 12.sp,
                    color = MandiTextSecondary
                )
            } else if (existingParty == null && state.party.phone.isNotBlank()) {
                Text(
                    text = if (isEnglish) "New number. The account is created when the entry is saved." else "नया नंबर। एंट्री सेव होते ही खाता बन जाएगा।",
                    fontSize = 12.sp,
                    color = MandiTextSecondary
                )
            }

            // Section 2: Crop Lots / Varieties (Flat layout)
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
                    val currentLot = state.lots.firstOrNull()
                    commonCrops.forEach { crop ->
                        MandiChip(
                            label = crop,
                            selected = currentLot?.cropName == crop,
                            onClick = {
                                currentLot?.let { actions.onLotChange(it.id, BuyLotField.Crop, crop) }
                            }
                        )
                    }
                }
            }

            // Crop input lines
            state.lots.forEachIndexed { index, lot ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.lots.size > 1) {
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
                                onClick = { actions.onRemoveLot(lot.id) },
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

                    // B final Line 1: Bags, Weight/Bag, Extra weight in a 3-column row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MandiFlatTextField(
                            value = lot.bags,
                            onValueChange = { actions.onLotChange(lot.id, BuyLotField.Bags, it) },
                            label = if (isEnglish) "Bags" else "बोरी संख्या",
                            isError = lot.bagsError != null,
                            errorMessage = lot.bagsError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            isTall = true,
                            modifier = Modifier.weight(1f)
                        )
                        MandiFlatTextField(
                            value = lot.weightPerBagKg,
                            onValueChange = { actions.onLotChange(lot.id, BuyLotField.WeightPerBag, it) },
                            label = if (isEnglish) "Kg/bag" else "वजन/बोरी (किग्रा)",
                            isError = lot.weightError != null,
                            errorMessage = lot.weightError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            isTall = true,
                            modifier = Modifier.weight(1f)
                        )
                        MandiFlatTextField(
                            value = lot.extraWeightKg,
                            onValueChange = { actions.onLotChange(lot.id, BuyLotField.ExtraWeight, it) },
                            label = if (isEnglish) "Extra (kg)" else "अतिरिक्त (किग्रा)",
                            placeholder = "0",
                            isError = lot.extraError != null,
                            errorMessage = lot.extraError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            isTall = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // B final Line 2: Rate on next line
                    MandiFlatTextField(
                        value = lot.ratePerQuintal,
                        onValueChange = { actions.onLotChange(lot.id, BuyLotField.Rate, it) },
                        label = if (isEnglish) "Rate (₹/qtl)" else "भाव (रु/क्विंटल)",
                        isError = lot.rateError != null,
                        errorMessage = lot.rateError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        isTall = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // B final Line 3: Subtotal row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) "Weight: ${MandiMathEngine.gramsToQuintals(lot.netWeightGrams)} qtl" else "वजन: ${MandiMathEngine.gramsToQuintals(lot.netWeightGrams)} क्विंटल",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MandiTextPrimary
                        )
                        Text(
                            text = if (isEnglish) "Gross: ₹${MandiMathEngine.paisaToRupeesString(lot.grossPaisa)}" else "सकल: ₹${MandiMathEngine.paisaToRupeesString(lot.grossPaisa)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MandiTextPrimary
                        )
                    }
                }
            }

            // Add Another Crop Lot Button
            MandiSecondaryButton(
                text = if (isEnglish) "Add another crop lot" else "अन्य फसल / वैरायटी जोड़ें",
                leadingIcon = Icons.Default.Add,
                onClick = { actions.onAddLot() },
                modifier = Modifier.fillMaxWidth()
            )

            // Deductions Grid (Commission % and Labour in 2-column grid)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MandiFlatTextField(
                    value = state.rates.commissionPercent,
                    onValueChange = { actions.onCommissionChange(it) },
                    label = if (isEnglish) "Commission %" else "कमीशन %",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    isTall = true,
                    modifier = Modifier.weight(1f)
                )
                MandiFlatTextField(
                    value = state.rates.labourPerBag,
                    onValueChange = { actions.onLabourChange(it) },
                    label = if (isEnglish) "Labour (₹/bag)" else "मजदूरी (रु/बोरी)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    isTall = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Section 3: हिसाब पर्ची (Slip summary card with lines)
            Text(
                text = if (isEnglish) "Slip" else "हिसाब पर्ची",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MandiTextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )

            MandiLinesCard {
                state.lots.forEachIndexed { i, lot ->
                    Line(
                        label = if (isEnglish) "Lot #${i + 1}: ${lot.cropName}, ${lot.bags} bags" else "फसल #${i + 1}: ${lot.cropName}, ${lot.bags} बोरी",
                        amountText = MandiMathEngine.formatRupees(lot.grossPaisa)
                    )
                }
                Line(
                    label = if (isEnglish) "Labour" else "मजदूरी कटौती",
                    amountText = "− ₹${MandiMathEngine.paisaToRupeesString(state.slip.labourPaisa)}"
                )
                Line(
                    label = if (isEnglish) "Commission" else "कमीशन कटौती",
                    amountText = "− ₹${MandiMathEngine.paisaToRupeesString(state.slip.commissionPaisa)}"
                )
                TotalLine(
                    label = if (isEnglish) "Slip value" else "पर्ची राशि",
                    amountText = MandiMathEngine.formatRupees(state.slip.netPayablePaisa),
                    largeAmount = true
                )
            }

            // Section 4: पर्ची के बाद का हिसाब (After the slip)
            Text(
                text = if (isEnglish) "After the slip" else "पर्ची के बाद का हिसाब",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MandiTextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Advance field
            MandiFlatTextField(
                value = state.advanceText,
                onValueChange = { actions.onAdvanceChange(it) },
                label = if (isEnglish) "Advance (₹)" else "अग्रिम / पेशगी (रु)",
                placeholder = if (isEnglish) "Optional" else "वैकल्पिक",
                isError = state.advanceError != null,
                errorMessage = state.advanceError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                isTall = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Balances: Loan card (hidden when editing an existing entry per contract)
            if (existingId == null && state.loanDeposit.hasAnything &&
                BuyPaymentCalculations.hasLoanBalance(state.loanDeposit.loanPrincipalPaisa, state.loanDeposit.loanInterestDuePaisa)
            ) {
                MandiCard(padding = 14.dp, spacing = 10.dp) {
                    val totalDebtPaisa = BuyPaymentCalculations.calculateTotalDebt(
                        state.loanDeposit.loanPrincipalPaisa,
                        state.loanDeposit.loanInterestDuePaisa
                    )
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
                            text = MandiMathEngine.formatRupees(totalDebtPaisa),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                    }

                    val principalStr = MandiMathEngine.formatRupees(state.loanDeposit.loanPrincipalPaisa)
                    val interestStr = MandiMathEngine.formatRupees(state.loanDeposit.loanInterestDuePaisa)
                    Text(
                        text = if (isEnglish) "Principal $principalStr + interest $interestStr" else "मूल $principalStr + ब्याज $interestStr",
                        fontSize = 12.sp,
                        color = MandiTextSecondary
                    )

                    MandiFlatTextField(
                        value = state.loanDeposit.loanCutText,
                        onValueChange = { actions.onLoanCutChange(it) },
                        label = if (isEnglish) "Cut from loan (₹)" else "उधार से काटें (रु)",
                        placeholder = "0",
                        isError = state.loanDeposit.loanCutError != null,
                        errorMessage = state.loanDeposit.loanCutError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        isTall = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val cutPaisa = BuyPaymentCalculations.calculateTotalCut(
                        state.loanDeposit.cutToInterestPaisa,
                        state.loanDeposit.cutToPrincipalPaisa
                    )
                    if (cutPaisa > 0L) {
                        val remPrincipal = BuyPaymentCalculations.calculateRemainingPrincipal(
                            state.loanDeposit.loanPrincipalPaisa,
                            state.loanDeposit.cutToPrincipalPaisa
                        )
                        val cutIntStr = MandiMathEngine.formatRupees(state.loanDeposit.cutToInterestPaisa)
                        val cutPrincStr = MandiMathEngine.formatRupees(state.loanDeposit.cutToPrincipalPaisa)
                        val remPrincStr = MandiMathEngine.formatRupees(remPrincipal)
                        Text(
                            text = if (isEnglish) {
                                "Interest $cutIntStr first, then principal $cutPrincStr. Principal left $remPrincStr."
                            } else {
                                "पहले ब्याज $cutIntStr, फिर मूल $cutPrincStr। बाकी मूल $remPrincStr।"
                            },
                            fontSize = 12.sp,
                            color = MandiTextSecondary
                        )
                    } else {
                        Text(
                            text = if (isEnglish) "Takes interest first, then principal" else "पहले ब्याज कटेगा, फिर मूलधन",
                            fontSize = 12.sp,
                            color = MandiTextSecondary
                        )
                    }
                }
            }

            // Balances: Deposit card (deposit is read-only on slip per decision C6)
            if (state.loanDeposit.depositPaisa > 0L) {
                MandiCard(padding = 14.dp, spacing = 10.dp) {
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
                            text = MandiMathEngine.formatRupees(state.loanDeposit.depositPaisa),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                    }

                    MandiFlatTextField(
                        value = "",
                        onValueChange = {},
                        label = if (isEnglish) "Pay from deposit (₹)" else "जमा से दें (रु)",
                        placeholder = if (isEnglish) "Optional" else "वैकल्पिक",
                        readOnly = true,
                        enabled = false,
                        isTall = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = if (isEnglish) "Deposit remains safe in account (per decision, does not reduce slip)." else "खाते में जमा सुरक्षित है (निर्णय अनुसार, पर्ची में कटौती नहीं होती)।",
                        fontSize = 12.sp,
                        color = MandiTextSecondary
                    )
                }
            }

            // Summary Lines (After the slip calculation card)
            MandiLinesCard {
                Line(
                    label = if (isEnglish) "Slip value" else "पर्ची राशि",
                    amountText = MandiMathEngine.formatRupees(state.slip.netPayablePaisa)
                )
                val advPaisa = MandiMathEngine.parseRupeesToPaisa(state.advanceText)
                if (advPaisa > 0L) {
                    Line(
                        label = if (isEnglish) "Advance" else "अग्रिम / पेशगी",
                        amountText = "− ₹${MandiMathEngine.paisaToRupeesString(advPaisa)}"
                    )
                }
                val totalCut = BuyPaymentCalculations.calculateTotalCut(
                    state.loanDeposit.cutToInterestPaisa,
                    state.loanDeposit.cutToPrincipalPaisa
                )
                if (totalCut > 0L) {
                    Line(
                        label = if (isEnglish) "Loan cut" else "उधार कटौती",
                        amountText = "− ₹${MandiMathEngine.paisaToRupeesString(totalCut)}"
                    )
                }
                TotalLine(
                    label = if (isEnglish) "Final payable" else "अंतिम देय राशि",
                    amountText = MandiMathEngine.formatRupees(state.payoutPaisa),
                    largeAmount = true
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
