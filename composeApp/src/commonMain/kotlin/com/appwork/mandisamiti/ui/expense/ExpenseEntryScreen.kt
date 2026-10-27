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
import com.appwork.mandisamiti.domain.model.ExpenseCategory
import com.appwork.mandisamiti.domain.model.ExpensePaymentMode

fun ExpenseCategory.labelHi(): String = when (this) {
    ExpenseCategory.HAMMALI -> "हम्माली"
    ExpenseCategory.KIRAYA -> "किराया"
    ExpenseCategory.BIJLI -> "बिजली"
    ExpenseCategory.CHAI_PANI -> "चाय-पानी"
    ExpenseCategory.MAJDOORI -> "मजदूरी"
    ExpenseCategory.OTHER -> "अन्य खर्च"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseEntryScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.HAMMALI) }
    var amount by remember { mutableStateOf("") }
    var paidTo by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf(ExpensePaymentMode.CASH) }
    var remarks by remember { mutableStateOf("") }

    val canSave = amount.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("खर्च दर्ज करें") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                label = { Text("खर्च राशि (रु)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = paidTo,
                onValueChange = { paidTo = it },
                label = { Text("किसे दिया (नाम)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("भुगतान का माध्यम (Payment Mode)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = paymentMode == ExpensePaymentMode.CASH,
                    onClick = { paymentMode = ExpensePaymentMode.CASH },
                    label = { Text("नकद (Cash)") }
                )
                FilterChip(
                    selected = paymentMode == ExpensePaymentMode.ONLINE,
                    onClick = { paymentMode = ExpensePaymentMode.ONLINE },
                    label = { Text("ऑनलाइन / UPI") }
                )
                FilterChip(
                    selected = paymentMode == ExpensePaymentMode.CHEQUE,
                    onClick = { paymentMode = ExpensePaymentMode.CHEQUE },
                    label = { Text("चेक (Cheque)") }
                )
            }

            OutlinedTextField(
                value = remarks,
                onValueChange = { remarks = it },
                label = { Text("विवरण / टिप्पणी (वैकल्पिक)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onSaved,
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("खर्च सुरक्षित करें")
            }
        }
    }
}
