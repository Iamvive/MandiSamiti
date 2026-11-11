package com.appwork.mandisamiti.ui.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var description by remember { mutableStateOf(existing?.remarks?.substringBefore('\n').orEmpty()) }
    var note by remember { mutableStateOf(existing?.remarks?.substringAfter('\n', "").orEmpty()) }
    var amount by remember { mutableStateOf(existing?.let { MandiMathEngine.paisaToInputString(it.amountPaisa) } ?: "") }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isDirty = if (existing != null) {
        amount != MandiMathEngine.paisaToInputString(existing.amountPaisa) ||
            description != existing.remarks?.substringBefore('\n').orEmpty() ||
            note != existing.remarks?.substringAfter('\n', "").orEmpty()
    } else amount.isNotBlank() || description.isNotBlank() || note.isNotBlank()

    MandiBackHandler(enabled = true) {
        if (isDirty) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    val parsedAmount = MandiMathEngine.parseRupeesToPaisa(amount)
    val canSave = description.isNotBlank() && amount.isNotBlank() && parsedAmount > 0L

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
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(if (isEnglish) "Expense Description *" else "खर्च का विवरण *") },
                singleLine = true,
                isError = description.isBlank() && amount.isNotBlank(),
                supportingText = {
                    Text(if (isEnglish) "This label will appear in Galla Rokad." else "यह नाम गल्ला रोकड़ में दिखेगा।")
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(if (isEnglish) "Expense Amount (₹) *" else "खर्च राशि (रु) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amount.isNotBlank() && parsedAmount <= 0L,
                supportingText = if (amount.isNotBlank() && parsedAmount <= 0L) {
                    { Text(if (isEnglish) "Enter an amount greater than zero." else "राशि शून्य से अधिक दर्ज करें।") }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(if (isEnglish) "Note (Optional)" else "विवरण / टिप्पणी (वैकल्पिक)") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
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
                            category = existing?.category ?: ExpenseCategory.OTHER,
                            amountPaisa = parsedAmount,
                            paidTo = existing?.paidTo,
                            paymentMode = existing?.paymentMode ?: ExpensePaymentMode.CASH,
                            remarks = listOf(description.trim(), note.trim()).filter { it.isNotEmpty() }.joinToString("\n"),
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
