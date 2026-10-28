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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendStatus
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.components.DiscardDraftDialog
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendEntryScreen(
    shopId: String = "",
    onSaved: (LendEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var lendType by remember { mutableStateOf(LendType.MONEY) }
    var partyPhoneOrName by remember { mutableStateOf("") }
    var amountRupees by remember { mutableStateOf("") }
    var commodityName by remember { mutableStateOf("") }
    var bagsCount by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var expectedReturnDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isDirty = partyPhoneOrName.isNotBlank() ||
        amountRupees.isNotBlank() ||
        commodityName.isNotBlank() ||
        bagsCount.isNotBlank() ||
        weightKg.isNotBlank() ||
        expectedReturnDate.isNotBlank() ||
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

            OutlinedTextField(
                value = partyPhoneOrName,
                onValueChange = { partyPhoneOrName = it },
                label = { Text("किसान / व्यक्ति का नाम या फोन *") },
                modifier = Modifier.fillMaxWidth()
            )

            if (isMoney) {
                OutlinedTextField(
                    value = amountRupees,
                    onValueChange = { amountRupees = it },
                    label = { Text("उधार राशि (रु) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = commodityName,
                    onValueChange = { commodityName = it },
                    label = { Text("जिंस / फसल का नाम (उदा. गेहूं, चना) *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bagsCount,
                        onValueChange = { bagsCount = it },
                        label = { Text("बोरी संख्या") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weightKg,
                        onValueChange = { weightKg = it },
                        label = { Text("कुल वजन (किग्रा) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = expectedReturnDate,
                onValueChange = { expectedReturnDate = it },
                label = { Text("अपेक्षित वापसी तारीख (उदा. 30 दिन या 15/11/2026)") },
                modifier = Modifier.fillMaxWidth()
            )

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
                        val returnDays = expectedReturnDate.trim().toLongOrNull()
                        val returnMillis = if (returnDays != null && returnDays > 0) {
                            now + returnDays * 24 * 3600 * 1000L
                        } else null

                        val entry = LendEntry(
                            id = IdGenerator.newId(),
                            shopId = shopId,
                            partyId = partyPhoneOrName.trim(),
                            lendType = lendType,
                            entryDate = now,
                            amountPaisa = if (isMoney) MandiMathEngine.parseRupeesToPaisa(amountRupees) else 0L,
                            commodityId = if (!isMoney) commodityName.trim() else null,
                            bagsCount = if (!isMoney) bagsCount.trim().toLongOrNull() ?: 0L else 0L,
                            weightGrams = if (!isMoney) MandiMathEngine.parseKgToGrams(weightKg) else 0L,
                            expectedReturnDate = returnMillis,
                            notes = notes.trim().ifEmpty { null },
                            status = LendStatus.OPEN,
                            createdAt = now,
                            updatedAt = now,
                        )
                        onSaved(entry)
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("उधार सुरक्षित करें")
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
