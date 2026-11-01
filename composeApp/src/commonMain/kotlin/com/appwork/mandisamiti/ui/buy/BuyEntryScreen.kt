package com.appwork.mandisamiti.ui.buy

import androidx.compose.foundation.background
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
import com.appwork.mandisamiti.ui.theme.MandiGreenReceivable
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import kotlinx.datetime.Clock

/**
 * Rebuilt BuyEntryScreen adhering strictly to Direction B final & NGDL guardrails:
 * - Renders only [BuyEntryUiState] mapped via [BuyEntryFormState.toUiState] and dispatches events via [BuyEntryActions].
 * - Always-visible bottom sticky action dock showing live calculated final payable amount & Save button.
 * - Dedicated loan & deposit section: displays outstanding principal, calculated interest due,
 *   cut input field, split preview (interest-first, then principal), and validation errors.
 * - Deposit is strictly read-only per Vivek decision (2026-11-01).
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
        tareQuintals = formState.tareQuintals,
        onTareChange = { formState.tareQuintals = it },
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
 * Pure presentation composable that renders [BuyEntryUiState] and communicates strictly via [BuyEntryActions].
 */
@Composable
fun BuyEntryScreenContent(
    state: BuyEntryUiState,
    actions: BuyEntryActions,
    existingId: String?,
    tareQuintals: String,
    onTareChange: (String) -> Unit,
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
                    if (existingId == null) "New Purchase" else "Edit Purchase"
                } else {
                    if (existingId == null) "नई खरीद" else "खरीद बदलें"
                },
                subtitle = if (isEnglish) "Buy Entry & Farmer Bill" else "आवक सौदा व किसान पर्ची",
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
                            Text(
                                text = MandiMathEngine.formatRupees(state.payoutPaisa),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiGreenReceivable
                            )
                        }

                        if (!state.canSave && state.saveBlockedReason != null) {
                            Text(
                                text = state.saveBlockedReason,
                                fontSize = 12.sp,
                                color = MandiRedPayable,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false).padding(start = 12.dp)
                            )
                        }
                    }

                    MandiPrimaryButton(
                        text = if (isEnglish) {
                            if (existingId == null) "Save (Deal Confirmed)" else "Save Changes"
                        } else {
                            if (existingId == null) "सुरक्षित करें (सौदा पक्का)" else "बदलाव सुरक्षित करें"
                        },
                        onClick = { actions.onSave() },
                        enabled = state.canSave,
                        modifier = Modifier.fillMaxWidth()
                    )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Farmer Details
            MandiSection(title = if (isEnglish) "Farmer Details" else "किसान विवरण") {
                MandiCard {
                    PartyAutocompleteField(
                        value = state.party.name,
                        onValueChange = { actions.onNameChange(it) },
                        label = if (isEnglish) "Farmer Name *" else "किसान का नाम *",
                        parties = parties,
                        partyType = PartyType.FARMER,
                        onPartySelected = { party ->
                            actions.onPickParty(party.id)
                            actions.onNameChange(party.name)
                            party.phone?.let { actions.onPhoneChange(it) }
                            party.village?.let { actions.onVillageChange(it) }
                        },
                        isError = state.party.nameError != null,
                        supportingText = state.party.nameError?.let {
                            { Text(it, color = MandiRedPayable, fontSize = 11.sp) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.party.phone,
                            onValueChange = { actions.onPhoneChange(it) },
                            label = { Text(if (isEnglish) "Mobile Number *" else "मोबाइल नंबर *", fontSize = 13.sp) },
                            isError = state.party.phoneError != null,
                            supportingText = state.party.phoneError?.let {
                                { Text(it, color = MandiRedPayable, fontSize = 11.sp) }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MandiBorder,
                                unfocusedBorderColor = MandiBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = state.party.village,
                            onValueChange = { actions.onVillageChange(it) },
                            label = { Text(if (isEnglish) "Village *" else "गाँव *", fontSize = 13.sp) },
                            isError = state.party.villageError != null,
                            supportingText = state.party.villageError?.let {
                                { Text(it, color = MandiRedPayable, fontSize = 11.sp) }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MandiBorder,
                                unfocusedBorderColor = MandiBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (state.loanDeposit.hasAnything) {
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
                                    if (isEnglish) "Active loan / balance exists (can deduct in settlement below)" else "सक्रिय उधार मौजूद है (नीचे हिसाब में कटौती संभव)",
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

                state.lots.forEachIndexed { index, lot ->
                    MandiCard(
                        modifier = Modifier.fillMaxWidth(),
                        isElevated = index % 2 == 1
                    ) {
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

                            if (state.lots.size > 1) {
                                IconButton(
                                    onClick = { actions.onRemoveLot(lot.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = if (isEnglish) "Remove Lot" else "लॉट हटाएं",
                                        tint = MandiRedPayable,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Quick-Select Crop Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            commonCrops.forEach { crop ->
                                MandiChip(
                                    label = crop,
                                    selected = lot.cropName == crop,
                                    onClick = { actions.onLotChange(lot.id, BuyLotField.Crop, crop) }
                                )
                            }
                        }

                        // Lot Input Fields
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = lot.bags,
                                onValueChange = { actions.onLotChange(lot.id, BuyLotField.Bags, it) },
                                label = { Text(if (isEnglish) "Bags *" else "बोरी संख्या *", fontSize = 13.sp) },
                                isError = lot.bagsError != null,
                                supportingText = lot.bagsError?.let { { Text(it, color = MandiRedPayable, fontSize = 11.sp) } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MandiBorder,
                                    unfocusedBorderColor = MandiBorder
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lot.weightPerBagKg,
                                onValueChange = { actions.onLotChange(lot.id, BuyLotField.WeightPerBag, it) },
                                label = { Text(if (isEnglish) "Weight/Bag (kg)" else "वजन/बोरी (किग्रा)", fontSize = 13.sp) },
                                isError = lot.weightError != null,
                                supportingText = lot.weightError?.let { { Text(it, color = MandiRedPayable, fontSize = 11.sp) } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
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
                                onValueChange = { actions.onLotChange(lot.id, BuyLotField.ExtraWeight, it) },
                                label = { Text(if (isEnglish) "Extra (kg)" else "अतिरिक्त (किग्रा)", fontSize = 13.sp) },
                                placeholder = { Text("0") },
                                isError = lot.extraError != null,
                                supportingText = lot.extraError?.let { { Text(it, color = MandiRedPayable, fontSize = 11.sp) } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MandiBorder,
                                    unfocusedBorderColor = MandiBorder
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lot.ratePerQuintal,
                                onValueChange = { actions.onLotChange(lot.id, BuyLotField.Rate, it) },
                                label = { Text(if (isEnglish) "Rate (₹/qtl) *" else "भाव (रु/क्विंटल) *", fontSize = 13.sp) },
                                isError = lot.rateError != null,
                                supportingText = lot.rateError?.let {
                                    { Text(it, color = MandiRedPayable, fontSize = 11.sp) }
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
                                    text = if (isEnglish) "Weight: ${MandiMathEngine.gramsToQuintals(lot.netWeightGrams)} qtl" else "वजन: ${MandiMathEngine.gramsToQuintals(lot.netWeightGrams)} क्विंटल",
                                    fontSize = 12.sp,
                                    color = MandiTextSecondary
                                )
                                Text(
                                    text = if (isEnglish) "Gross: ₹${MandiMathEngine.paisaToRupeesString(lot.grossPaisa)}" else "सकल: ₹${MandiMathEngine.paisaToRupeesString(lot.grossPaisa)}",
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
                    onClick = { actions.onAddLot() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Section 3: Deductions & Optional Advance
            MandiSection(title = if (isEnglish) "Deductions & Advance" else "कटौती एवं अग्रिम") {
                MandiCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.rates.commissionPercent,
                            onValueChange = { actions.onCommissionChange(it) },
                            label = { Text(if (isEnglish) "Commission %" else "कमीशन %", fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MandiBorder,
                                unfocusedBorderColor = MandiBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.rates.labourPerBag,
                            onValueChange = { actions.onLabourChange(it) },
                            label = { Text(if (isEnglish) "Labour (₹/bag)" else "मजदूरी (रु/बोरी)", fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MandiBorder,
                                unfocusedBorderColor = MandiBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // For editing existing slips with stored tare only
                    if (existingId != null && tareQuintals.isNotBlank() && tareQuintals != "0") {
                        OutlinedTextField(
                            value = tareQuintals,
                            onValueChange = onTareChange,
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
                        value = state.advanceText,
                        onValueChange = { actions.onAdvanceChange(it) },
                        label = { Text(if (isEnglish) "Advance Paid (₹)" else "अग्रिम / पेशगी (रु)", fontSize = 13.sp) },
                        placeholder = { Text("0") },
                        isError = state.advanceError != null,
                        supportingText = {
                            if (state.advanceError != null) {
                                Text(state.advanceError, color = MandiRedPayable, fontSize = 11.sp)
                            } else {
                                Text(if (isEnglish) "Optional" else "वैकल्पिक", fontSize = 11.sp)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MandiBorder,
                            unfocusedBorderColor = MandiBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Section 4: Loan & Deposit Settlement Section
            if (state.loanDeposit.hasAnything) {
                MandiSection(title = if (isEnglish) "Loan Deduction" else "उधार कटौती") {
                    MandiCard {
                        // Principal & Interest Due Display
                        if (state.loanDeposit.loanPrincipalPaisa > 0L || state.loanDeposit.loanInterestDuePaisa > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = (if (isEnglish) "Principal Due: " else "बकाया मूल: ") + MandiMathEngine.formatRupees(state.loanDeposit.loanPrincipalPaisa),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MandiGreenReceivable
                                )
                                Text(
                                    text = (if (isEnglish) "Interest Due: " else "ब्याज बकाया: ") + MandiMathEngine.formatRupees(state.loanDeposit.loanInterestDuePaisa),
                                    fontSize = 13.sp,
                                    color = MandiTextSecondary
                                )
                            }
                        }

                        // Read-only deposit balance if any (per Vivek decision 2026-11-01, deposit is not used on slip)
                        if (state.loanDeposit.depositPaisa > 0L) {
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
                                        text = if (isEnglish) "Existing Deposit (Safe in Khata)" else "जमा राशि (खाते में सुरक्षित)",
                                        fontSize = 12.sp,
                                        color = MandiTextSecondary
                                    )
                                    Text(
                                        text = MandiMathEngine.formatRupees(state.loanDeposit.depositPaisa),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiTextPrimary
                                    )
                                }
                            }
                        }

                        // Cut Input Field
                        OutlinedTextField(
                            value = state.loanDeposit.loanCutText,
                            onValueChange = { actions.onLoanCutChange(it) },
                            label = { Text(if (isEnglish) "Deduct From Loan (₹)" else "उधार से कटौती (रु)", fontSize = 13.sp) },
                            placeholder = { Text("0") },
                            isError = state.loanDeposit.loanCutError != null,
                            supportingText = {
                                if (state.loanDeposit.loanCutError != null) {
                                    Text(state.loanDeposit.loanCutError, color = MandiRedPayable, fontSize = 11.sp)
                                } else {
                                    Text(if (isEnglish) "Takes interest first, then principal" else "पहले ब्याज कटेगा, फिर मूलधन", fontSize = 11.sp)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MandiBorder,
                                unfocusedBorderColor = MandiBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Interest / Principal Split Preview
                        if (state.loanDeposit.cutToInterestPaisa > 0L || state.loanDeposit.cutToPrincipalPaisa > 0L) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MandiSurfaceElevated,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) "Cut Split Preview:" else "कटौती का विवरण:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MandiTextPrimary
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isEnglish) "To Interest: ₹${MandiMathEngine.paisaToRupeesString(state.loanDeposit.cutToInterestPaisa)}" else "ब्याज में जमा: ₹${MandiMathEngine.paisaToRupeesString(state.loanDeposit.cutToInterestPaisa)}",
                                            fontSize = 12.sp,
                                            color = MandiTextSecondary
                                        )
                                        Text(
                                            text = if (isEnglish) "To Principal: ₹${MandiMathEngine.paisaToRupeesString(state.loanDeposit.cutToPrincipalPaisa)}" else "मूलधन में जमा: ₹${MandiMathEngine.paisaToRupeesString(state.loanDeposit.cutToPrincipalPaisa)}",
                                            fontSize = 12.sp,
                                            color = MandiTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 5: Live Parcha Receipt Summary
            MandiSection(title = if (isEnglish) "Slip Summary" else "हिसाब पर्ची") {
                MandiCard {
                    val bagsUnit = if (isEnglish) "bags" else "बोरी"
                    val qtlUnit = if (isEnglish) "qtl" else "क्विंटल"
                    MandiMoneyRow(label = if (isEnglish) "Total Bags" else "कुल बोरी", amountText = "${MandiMathEngine.formatIndianGrouping(state.slip.totalBags)} $bagsUnit")
                    if (tareQuintals.isNotBlank() && tareQuintals != "0") {
                        MandiMoneyRow(label = if (isEnglish) "Tare / Bardana" else "काट / बारदाना", amountText = "- $tareQuintals $qtlUnit")
                    }
                    MandiMoneyRow(label = if (isEnglish) "Total Weight" else "कुल वजन", amountText = "${MandiMathEngine.gramsToQuintals(state.slip.totalWeightGrams)} $qtlUnit")
                    MandiMoneyRow(label = if (isEnglish) "Gross Amount" else "सकल राशि", amountText = MandiMathEngine.formatRupees(state.slip.grossPaisa))
                    // B final rule: Labour sits above Commission
                    MandiMoneyRow(label = if (isEnglish) "Labour" else "मजदूरी कटौती", amountText = "- ₹${MandiMathEngine.paisaToRupeesString(state.slip.labourPaisa)}")
                    MandiMoneyRow(label = if (isEnglish) "Commission" else "कमीशन कटौती", amountText = "- ₹${MandiMathEngine.paisaToRupeesString(state.slip.commissionPaisa)}")
                    if (state.slip.netPayablePaisa > 0) {
                        MandiMoneyRow(
                            label = if (isEnglish) "Slip Value" else "पर्ची राशि (सौदा)",
                            amountText = MandiMathEngine.formatRupees(state.slip.netPayablePaisa),
                            emphasized = true
                        )
                    }
                    if (state.loanDeposit.cutToInterestPaisa > 0L || state.loanDeposit.cutToPrincipalPaisa > 0L) {
                        val totalCut = state.loanDeposit.cutToInterestPaisa + state.loanDeposit.cutToPrincipalPaisa
                        MandiMoneyRow(label = if (isEnglish) "Loan Cut" else "उधार कटौती", amountText = "- ₹${MandiMathEngine.paisaToRupeesString(totalCut)}")
                    }
                    if (state.advanceText.isNotBlank()) {
                        val advPaisa = MandiMathEngine.parseRupeesToPaisa(state.advanceText)
                        if (advPaisa > 0) {
                            MandiMoneyRow(label = if (isEnglish) "Advance Paid" else "अग्रिम / पेशगी", amountText = "- ₹${MandiMathEngine.paisaToRupeesString(advPaisa)}")
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    MandiMoneyRow(
                        label = if (isEnglish) "Final Payout" else "अंतिम देय राशि",
                        amountText = MandiMathEngine.formatRupees(state.payoutPaisa),
                        emphasized = true
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
