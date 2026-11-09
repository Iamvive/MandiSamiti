package com.appwork.mandisamiti.ui.lend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiBorder
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.repository.LendRepository
import com.appwork.mandisamiti.domain.util.LendDateFormatter
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.components.DiscardDraftDialog
import com.appwork.mandisamiti.ui.components.DuplicateEntryDialog
import com.appwork.mandisamiti.ui.components.MandiFlatTextField
import com.appwork.mandisamiti.ui.components.MandiPartyLookupField
import com.appwork.mandisamiti.ui.components.NewPartyConfirmDialog
import com.appwork.mandisamiti.ui.components.TypedPartyCheck
import com.appwork.mandisamiti.ui.components.checkTypedParty
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import kotlinx.datetime.Clock

private fun String.errorIfFilled(error: String?): String? = if (isBlank()) null else error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendEntryScreen(
    shopId: String = "",
    onSaved: (LendEntry, Party?) -> Unit,
    onBack: () -> Unit,
    parties: List<Party> = emptyList(),
    lendRepository: LendRepository? = null,
    isEnglish: Boolean = false,
    formState: LendEntryFormState = remember { LendEntryFormState() },
    modifier: Modifier = Modifier
) {
    var showCustomDatePicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var newPartyCheck by remember { mutableStateOf<TypedPartyCheck.NeedsConfirm?>(null) }
    var pendingEntryToSave by remember { mutableStateOf<LendEntry?>(null) }
    var pendingPartyToSave by remember { mutableStateOf<Party?>(null) }

    val recentLends by (lendRepository?.getLendEntriesStream(shopId) ?: kotlinx.coroutines.flow.emptyFlow()).collectAsState(initial = emptyList())

    val isDirty = formState.isDirty

    MandiBackHandler(enabled = true) {
        if (isDirty) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    val isMoney = formState.lendType == LendType.MONEY
    val canSave = formState.canSave()
    val disabledReason = formState.validationReason(isEnglish)

    val performSave: () -> Unit = {
        val now = Clock.System.now().toEpochMilliseconds()
        val existing = LendPartyResolver.existing(parties, formState.selectedPartyId, formState.phone)
        val party = LendPartyResolver.partyToSave(formState, shopId, existing, now)
        val partyId = existing?.id ?: party?.id ?: formState.partyName.trim()
        val entry = formState.toLendEntry(
            shopId = shopId,
            partyId = partyId,
            now = now
        )

        val isDuplicate = recentLends.any { prev ->
            !prev.isVoid &&
            prev.partyId.trim().equals(entry.partyId.trim(), ignoreCase = true) &&
            prev.lendType == entry.lendType &&
            (if (isMoney) prev.amountPaisa == entry.amountPaisa else (prev.commodityId == entry.commodityId && prev.weightGrams == entry.weightGrams)) &&
            kotlin.math.abs(now - prev.createdAt) <= 2 * 60 * 1000L
        }

        if (isDuplicate) {
            pendingEntryToSave = entry
            pendingPartyToSave = party
            showDuplicateDialog = true
        } else {
            onSaved(entry, party)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEnglish) "Record Loan / Advance" else "उधार दर्ज करें (Lend)") },
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
        },
        bottomBar = {
            Surface(
                color = MandiSurface,
                border = BorderStroke(1.dp, MandiBorder),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Button(
                        onClick = {
                            when (val check = checkTypedParty(formState.partyName, formState.selectedPartyId, parties)) {
                                is TypedPartyCheck.ExactMatch -> {
                                    formState.selectedPartyId = check.party.id
                                    performSave()
                                }
                                is TypedPartyCheck.NeedsConfirm -> {
                                    newPartyCheck = check
                                }
                                else -> performSave()
                            }
                        },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(if (canSave) (if (isEnglish) "Save Loan / Advance" else "उधार सुरक्षित करें") else disabledReason)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(if (isEnglish) "Lend Type" else "उधार का प्रकार (Lend Type)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isMoney,
                    onClick = { formState.lendType = LendType.MONEY },
                    label = { Text(if (isEnglish) "Cash / Money" else "रुपये / नकद (Money)") }
                )
                FilterChip(
                    selected = !isMoney,
                    onClick = { formState.lendType = LendType.GRAIN },
                    label = { Text(if (isEnglish) "Grain / Commodity" else "अनाज (Grain)") }
                )
            }

            // Phone-first party block
            val existingParty = LendPartyResolver.existing(parties, formState.selectedPartyId, formState.phone)
            val isOldPartyWithoutPhone = formState.selectedPartyId != null && formState.phone.isBlank()

            MandiPartyLookupField(
                phoneValue = formState.phone,
                onPhoneChange = { formState.updatePhone(it) },
                onPartySelected = { party ->
                    LendPartyResolver.pick(formState, party)
                },
                parties = parties,
                label = if (isEnglish) "Mobile number" else "मोबाइल नंबर",
                placeholder = if (isOldPartyWithoutPhone) (if (isEnglish) "Enter 10-digit number" else "10 अंकों का नंबर डालें") else null,
                isError = formState.phone.errorIfFilled(formState.phoneValidation.errorOrNull) != null,
                errorMessage = formState.phone.errorIfFilled(formState.phoneValidation.errorOrNull)
            )

            MandiFlatTextField(
                value = formState.partyName,
                onValueChange = { formState.partyName = it },
                label = if (isEnglish) "Farmer / Person Name" else "किसान / व्यक्ति का नाम",
                placeholder = if (isEnglish) "Type name" else "नाम लिखें",
                isError = formState.partyName.errorIfFilled(formState.nameValidation.errorOrNull) != null,
                errorMessage = formState.partyName.errorIfFilled(formState.nameValidation.errorOrNull),
                isTall = true,
                modifier = Modifier.fillMaxWidth()
            )

            MandiFlatTextField(
                value = formState.village,
                onValueChange = { formState.village = it },
                label = if (isEnglish) "Village" else "गांव",
                placeholder = if (isEnglish) "Type village" else "गांव लिखें",
                isError = formState.village.errorIfFilled(formState.villageValidation.errorOrNull) != null,
                errorMessage = formState.village.errorIfFilled(formState.villageValidation.errorOrNull),
                isTall = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Hint line
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

            if (isMoney) {
                OutlinedTextField(
                    value = formState.amountRupees,
                    onValueChange = { formState.amountRupees = it },
                    label = { Text(if (isEnglish) "Loan Amount (₹) *" else "उधार राशि (रु) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = formState.commodityName,
                    onValueChange = { formState.commodityName = it },
                    label = { Text(if (isEnglish) "Crop / Commodity Name (e.g. Wheat, Gram) *" else "जिंस / फसल का नाम (उदा. गेहूं, चना) *") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formState.bagsCount,
                        onValueChange = { formState.bagsCount = it },
                        label = { Text(if (isEnglish) "Bags Count" else "बोरी संख्या") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = formState.weightKg,
                        onValueChange = { formState.weightKg = it },
                        label = { Text(if (isEnglish) "Total Weight (kg) *" else "कुल वजन (किग्रा) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Return Date Chips & Date Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isEnglish) "Expected Return Date" else "अपेक्षित वापसी तारीख (Return Due Date)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val quickOptions = if (isEnglish) {
                        listOf(15 to "15 days", 30 to "30 days", 60 to "60 days")
                    } else {
                        listOf(15 to "15 दिन", 30 to "30 दिन", 60 to "60 दिन")
                    }
                    quickOptions.forEach { (days, label) ->
                        FilterChip(
                            selected = formState.selectedDaysChip == days,
                            onClick = {
                                val now = Clock.System.now().toEpochMilliseconds()
                                if (formState.selectedDaysChip == days) {
                                    formState.selectedDaysChip = null
                                    formState.expectedReturnMillis = null
                                } else {
                                    formState.selectedDaysChip = days
                                    formState.expectedReturnMillis = now + days * 24 * 3600 * 1000L
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                    FilterChip(
                        selected = formState.selectedDaysChip == -1,
                        onClick = {
                            showCustomDatePicker = true
                        },
                        label = {
                            Text(
                                if (formState.selectedDaysChip == -1 && formState.expectedReturnMillis != null) {
                                    if (isEnglish) "Custom Picked" else "कस्टम चुना"
                                } else {
                                    if (isEnglish) "Custom Date" else "कस्टम तारीख"
                                }
                            )
                        }
                    )
                }

                if (formState.expectedReturnMillis != null) {
                    val formatted = LendDateFormatter.formatDueDate(
                        returnEpochMillis = formState.expectedReturnMillis,
                        currentEpochMillis = Clock.System.now().toEpochMilliseconds()
                    )
                    if (!formatted.isNullOrBlank()) {
                        Text(
                            text = formatted,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            OutlinedTextField(
                value = formState.notes,
                onValueChange = { formState.notes = it },
                label = { Text(if (isEnglish) "Notes / Details (Optional)" else "टिप्पणी / विवरण (वैकल्पिक)") },
                modifier = Modifier.fillMaxWidth()
            )

            newPartyCheck?.let { check ->
                NewPartyConfirmDialog(
                    check = check,
                    isEnglish = isEnglish,
                    onCreate = {
                        newPartyCheck = null
                        performSave()
                    },
                    onDismiss = { newPartyCheck = null }
                )
            }

            if (showDuplicateDialog && pendingEntryToSave != null) {
                val pending = pendingEntryToSave!!
                val detailStr = if (pending.lendType == LendType.MONEY) {
                    "${pending.partyId} • ₹${MandiMathEngine.formatIndianGrouping(pending.amountPaisa / 100)}"
                } else {
                    val bagsSuffix = if (isEnglish) "bags" else "बोरी"
                    "${pending.partyId} • ${pending.commodityId} (${pending.bagsCount} $bagsSuffix)"
                }
                DuplicateEntryDialog(
                    message = if (isEnglish) "Duplicate entry detected. Still want to save?" else "यह एंट्री पहले से है, फिर भी सुरक्षित करें?",
                    details = detailStr,
                    onConfirm = {
                        showDuplicateDialog = false
                        onSaved(pending, pendingPartyToSave)
                        pendingEntryToSave = null
                        pendingPartyToSave = null
                    },
                    onDismiss = {
                        showDuplicateDialog = false
                        pendingEntryToSave = null
                        pendingPartyToSave = null
                    }
                )
            }

            if (showCustomDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = formState.expectedReturnMillis ?: Clock.System.now().toEpochMilliseconds()
                )
                DatePickerDialog(
                    onDismissRequest = { showCustomDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            formState.expectedReturnMillis = datePickerState.selectedDateMillis
                            formState.selectedDaysChip = -1
                            showCustomDatePicker = false
                        }) {
                            Text(if (isEnglish) "Select Date" else "तारीख चुनें")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomDatePicker = false }) {
                            Text(if (isEnglish) "Cancel" else "रद्द करें")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            if (showDiscardDialog) {
                DiscardDraftDialog(
                    title = if (isEnglish) "Discard Changes?" else "बदलाव रद्द करें?",
                    message = if (isEnglish) "Are you sure you want to exit without saving? All entered details will be lost." else "क्या आप बिना सुरक्षित किए बाहर जाना चाहते हैं? भरी गई जानकारी मिट जाएगी।",
                    confirmText = if (isEnglish) "Yes, Exit" else "हाँ, बाहर निकलें",
                    dismissText = if (isEnglish) "Continue Editing" else "भरना जारी रखें",
                    onConfirmDiscard = {
                        showDiscardDialog = false
                        formState.reset()
                        onBack()
                    },
                    onDismiss = {
                        showDiscardDialog = false
                    }
                )
            }
        }
    }
}
