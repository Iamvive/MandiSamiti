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
import androidx.compose.material.icons.automirrored.filled.List
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

fun ExpenseCategory.labelEn(): String = when (this) {
    ExpenseCategory.HAMMALI -> "Labour / Hammali"
    ExpenseCategory.KIRAYA -> "Rent"
    ExpenseCategory.BIJLI -> "Electricity"
    ExpenseCategory.CHAI_PANI -> "Refreshment"
    ExpenseCategory.MAJDOORI -> "Wages"
    ExpenseCategory.OTHER -> "Other"
    ExpenseCategory.DAY_CLOSE_DIFF -> "Day-Close Diff"
}

fun ExpenseCategory.label(isEnglish: Boolean): String = if (isEnglish) labelEn() else labelHi()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseEntryScreen(
    shopId: String = "",
    onSaved: (ExpenseEntry) -> Unit,
    onBack: () -> Unit,
    isEnglish: Boolean = false,
    existing: ExpenseEntry? = null,
    onOpenList: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(existing?.category ?: ExpenseCategory.HAMMALI) }
    var amount by remember { mutableStateOf(existing?.let { MandiMathEngine.paisaToInputString(it.amountPaisa) } ?: "") }
    var paidTo by remember { mutableStateOf(existing?.paidTo.orEmpty()) }
    var paymentMode by remember { mutableStateOf(existing?.paymentMode ?: ExpensePaymentMode.CASH) }
    var remarks by remember { mutableStateOf(existing?.remarks.orEmpty()) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isDirty = if (existing != null) {
        amount != MandiMathEngine.paisaToInputString(existing.amountPaisa) || selectedCategory != existing.category ||
            paidTo != existing.paidTo.orEmpty() || remarks != existing.remarks.orEmpty() || paymentMode != existing.paymentMode
    } else amount.isNotBlank() || paidTo.isNotBlank() || remarks.isNotBlank()

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
                title = { Text(if (existing != null) (if (isEnglish) "Edit Expense" else "खर्च बदलें") else if (isEnglish) "Record Expense" else "खर्च दर्ज करें (Expense)") },
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
                },
                actions = {
                    if (existing == null && onOpenList != null) {
                        IconButton(onClick = onOpenList) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = if (isEnglish) "Past expenses" else "पिछले खर्च")
                        }
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
            Text(if (isEnglish) "Expense Category" else "खर्च श्रेणी (Category)")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExpenseCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.label(isEnglish)) }
                    )
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(if (isEnglish) "Expense Amount (₹) *" else "खर्च राशि (रु) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = paidTo,
                onValueChange = { paidTo = it },
                label = { Text(if (isEnglish) "Paid To (Name / Description)" else "किसे दिया (नाम / विवरण)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(if (isEnglish) "Payment Mode" else "भुगतान का माध्यम (Payment Mode)")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ExpensePaymentMode.CASH to (if (isEnglish) "Cash" else "नकद"),
                    ExpensePaymentMode.RTGS to "RTGS",
                    ExpensePaymentMode.NEFT to "NEFT",
                    ExpensePaymentMode.CHEQUE to (if (isEnglish) "Cheque" else "चेक"),
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
                label = { Text(if (isEnglish) "Remarks / Note (Optional)" else "विवरण / टिप्पणी (वैकल्पिक)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (canSave) {
                        val now = Clock.System.now().toEpochMilliseconds()
                        val entry = ExpenseEntry(
                            id = existing?.id ?: IdGenerator.newId(),
                            shopId = shopId,
                            entryDate = existing?.entryDate ?: now,
                            category = selectedCategory,
                            amountPaisa = MandiMathEngine.parseRupeesToPaisa(amount),
                            paidTo = paidTo.trim().ifEmpty { null },
                            paymentMode = paymentMode,
                            remarks = remarks.trim().ifEmpty { null },
                            createdAt = existing?.createdAt ?: now,
                            updatedAt = now,
                        )
                        onSaved(entry)
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (existing != null) (if (isEnglish) "Save Changes" else "बदलाव सुरक्षित करें") else if (isEnglish) "Save Expense" else "खर्च सुरक्षित करें")
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
    }
}
