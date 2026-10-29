package com.appwork.mandisamiti.ui.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.appwork.mandisamiti.domain.model.ExpenseCategory
import com.appwork.mandisamiti.domain.model.ExpenseEntry
import com.appwork.mandisamiti.domain.model.ExpensePaymentMode
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.components.DiscardDraftDialog
import kotlinx.datetime.Clock

fun ExpenseCategory.labelHi(): String = when (this) {
    ExpenseCategory.HAMMALI -> "हम्माली"
    ExpenseCategory.KIRAYA -> "किराया"
    ExpenseCategory.BIJLI -> "बिजली"
    ExpenseCategory.CHAI_PANI -> "चाय-पानी"
    ExpenseCategory.MAJDOORI -> "मजदूरी"
    ExpenseCategory.OTHER -> "अन्य खर्च"
    ExpenseCategory.DAY_CLOSE_DIFF -> "दिन-बंदी अंतर"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseEntryScreen(
    shopId: String = "",
    onSaved: (ExpenseEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.HAMMALI) }
    var amount by remember { mutableStateOf("") }
    var paidTo by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf(ExpensePaymentMode.CASH) }
    var remarks by remember { mutableStateOf("") }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isDirty = amount.isNotBlank() || paidTo.isNotBlank() || remarks.isNotBlank()

    MandiBackHandler(enabled = true) {
        if (isDirty) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    val canSave = amount.isNotBlank() && MandiMathEngine.parseRupeesToPaisa(amount) > 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("खर्च दर्ज करें (Expense)") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("खर्च श्रेणी (Category)")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExpenseCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.labelHi()) }
                    )
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("खर्च राशि (रु) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = paidTo,
                onValueChange = { paidTo = it },
                label = { Text("किसे दिया (नाम / विवरण)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("भुगतान का माध्यम (Payment Mode)")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ExpensePaymentMode.CASH to "नकद",
                    ExpensePaymentMode.RTGS to "RTGS",
                    ExpensePaymentMode.NEFT to "NEFT",
                    ExpensePaymentMode.CHEQUE to "चेक",
                    ExpensePaymentMode.UPI to "UPI",
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = paymentMode == mode,
                        onClick = { paymentMode = mode },
                        label = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = remarks,
                onValueChange = { remarks = it },
                label = { Text("विवरण / टिप्पणी (वैकल्पिक)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (canSave) {
                        val now = Clock.System.now().toEpochMilliseconds()
                        val entry = ExpenseEntry(
                            id = IdGenerator.newId(),
                            shopId = shopId,
                            entryDate = now,
                            category = selectedCategory,
                            amountPaisa = MandiMathEngine.parseRupeesToPaisa(amount),
                            paidTo = paidTo.trim().ifEmpty { null },
                            paymentMode = paymentMode,
                            remarks = remarks.trim().ifEmpty { null },
                            createdAt = now,
                            updatedAt = now,
                        )
                        onSaved(entry)
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("खर्च सुरक्षित करें")
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
