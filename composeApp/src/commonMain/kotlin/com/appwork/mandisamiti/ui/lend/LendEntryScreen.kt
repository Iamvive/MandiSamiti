package com.appwork.mandisamiti.ui.lend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendStatus
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.repository.LendRepository
import com.appwork.mandisamiti.domain.util.LendDateFormatter
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.components.DiscardDraftDialog
import com.appwork.mandisamiti.ui.components.DuplicateEntryDialog
import com.appwork.mandisamiti.ui.components.PartyAutocompleteField
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendEntryScreen(
    shopId: String = "",
    onSaved: (LendEntry) -> Unit,
    onBack: () -> Unit,
    parties: List<Party> = emptyList(),
    lendRepository: LendRepository? = null,
    modifier: Modifier = Modifier
) {
    var lendType by remember { mutableStateOf(LendType.MONEY) }
    var partyPhoneOrName by remember { mutableStateOf("") }
    var selectedPartyId by remember { mutableStateOf<String?>(null) }
    var amountRupees by remember { mutableStateOf("") }
    var commodityName by remember { mutableStateOf("") }
    var bagsCount by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var selectedDaysChip by remember { mutableStateOf<Int?>(null) }
    var expectedReturnMillis by remember { mutableStateOf<Long?>(null) }
    var showCustomDatePicker by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var pendingEntryToSave by remember { mutableStateOf<LendEntry?>(null) }

    val recentLends by (lendRepository?.getLendEntriesStream(shopId) ?: kotlinx.coroutines.flow.emptyFlow()).collectAsState(initial = emptyList())

    val isDirty = partyPhoneOrName.isNotBlank() ||
        amountRupees.isNotBlank() ||
        commodityName.isNotBlank() ||
        bagsCount.isNotBlank() ||
        weightKg.isNotBlank() ||
        expectedReturnMillis != null ||
        notes.isNotBlank()

    MandiBackHandler(enabled = true) {
        if (isDirty) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    val isMoney = lendType == LendType.MONEY
    val canSave = partyPhoneOrName.isNotBlank() && if (isMoney) {
        amountRupees.isNotBlank() && MandiMathEngine.parseRupeesToPaisa(amountRupees) > 0L
    } else {
        commodityName.isNotBlank() && MandiMathEngine.parseKgToGrams(weightKg) > 0L
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("उधार दर्ज करें (Lend)") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("उधार का प्रकार (Lend Type)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isMoney,
                    onClick = { lendType = LendType.MONEY },
                    label = { Text("रुपये / नकद (Money)") }
                )
                FilterChip(
                    selected = !isMoney,
                    onClick = { lendType = LendType.GRAIN },
                    label = { Text("अनाज (Grain)") }
                )
            }

            PartyAutocompleteField(
                value = partyPhoneOrName,
                onValueChange = {
                    partyPhoneOrName = it
                    selectedPartyId = null
                },
                label = "किसान / व्यक्ति का नाम या फोन *",
                parties = parties,
                onPartySelected = { selected ->
                    selectedPartyId = selected.id
                    partyPhoneOrName = selected.name
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            if (isMoney) {
                OutlinedTextField(
                    value = amountRupees,
                    onValueChange = { amountRupees = it },
                    label = { Text("उधार राशि (रु) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = commodityName,
                    onValueChange = { commodityName = it },
                    label = { Text("जिंस / फसल का नाम (उदा. गेहूं, चना) *") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bagsCount,
                        onValueChange = { bagsCount = it },
                        label = { Text("बोरी संख्या") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weightKg,
                        onValueChange = { weightKg = it },
                        label = { Text("कुल वजन (किग्रा) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Return Date Chips & Date Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "अपेक्षित वापसी तारीख (Return Due Date)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val quickOptions = listOf(15 to "15 दिन", 30 to "30 दिन", 60 to "60 दिन")
                    quickOptions.forEach { (days, label) ->
                        FilterChip(
                            selected = selectedDaysChip == days,
                            onClick = {
                                val now = Clock.System.now().toEpochMilliseconds()
                                if (selectedDaysChip == days) {
                                    selectedDaysChip = null
                                    expectedReturnMillis = null
                                } else {
                                    selectedDaysChip = days
                                    expectedReturnMillis = now + days * 24 * 3600 * 1000L
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                    FilterChip(
                        selected = selectedDaysChip == -1,
                        onClick = {
                            showCustomDatePicker = true
                        },
                        label = { Text(if (selectedDaysChip == -1 && expectedReturnMillis != null) "कस्टम चुना" else "कस्टम तारीख") }
                    )
                }

                if (expectedReturnMillis != null) {
                    val formatted = LendDateFormatter.formatDueDate(
                        returnEpochMillis = expectedReturnMillis,
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
                value = notes,
                onValueChange = { notes = it },
                label = { Text("टिप्पणी / विवरण (वैकल्पिक)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (canSave) {
                        val now = Clock.System.now().toEpochMilliseconds()
                        val entryAmount = if (isMoney) MandiMathEngine.parseRupeesToPaisa(amountRupees) else 0L
                        val entry = LendEntry(
                            id = IdGenerator.newId(),
                            shopId = shopId,
                            partyId = selectedPartyId ?: partyPhoneOrName.trim(),
                            lendType = lendType,
                            entryDate = now,
                            amountPaisa = entryAmount,
                            commodityId = if (!isMoney) commodityName.trim() else null,
                            bagsCount = if (!isMoney) bagsCount.trim().toLongOrNull() ?: 0L else 0L,
                            weightGrams = if (!isMoney) MandiMathEngine.parseKgToGrams(weightKg) else 0L,
                            expectedReturnDate = expectedReturnMillis,
                            notes = notes.trim().ifEmpty { null },
                            status = LendStatus.OPEN,
                            createdAt = now,
                            updatedAt = now,
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
                            showDuplicateDialog = true
                        } else {
                            onSaved(entry)
                        }
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("उधार सुरक्षित करें")
            }

            if (showDuplicateDialog && pendingEntryToSave != null) {
                val pending = pendingEntryToSave!!
                val detailStr = if (pending.lendType == LendType.MONEY) {
                    "${pending.partyId} • ₹${MandiMathEngine.formatIndianGrouping(pending.amountPaisa / 100)}"
                } else {
                    "${pending.partyId} • ${pending.commodityId} (${pending.bagsCount} बोरी)"
                }
                DuplicateEntryDialog(
                    message = "यह एंट्री पहले से है, फिर भी सुरक्षित करें?",
                    details = detailStr,
                    onConfirm = {
                        showDuplicateDialog = false
                        onSaved(pending)
                        pendingEntryToSave = null
                    },
                    onDismiss = {
                        showDuplicateDialog = false
                        pendingEntryToSave = null
                    }
                )
            }

            if (showCustomDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = expectedReturnMillis ?: Clock.System.now().toEpochMilliseconds()
                )
                DatePickerDialog(
                    onDismissRequest = { showCustomDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            expectedReturnMillis = datePickerState.selectedDateMillis
                            selectedDaysChip = -1
                            showCustomDatePicker = false
                        }) {
                            Text("तारीख चुनें")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomDatePicker = false }) {
                            Text("रद्द करें")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
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
        }
    }
}
