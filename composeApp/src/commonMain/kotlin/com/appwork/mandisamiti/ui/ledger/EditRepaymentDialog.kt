package com.appwork.mandisamiti.ui.ledger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.math.RepaymentEditPlan
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessBg
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessFg
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRepaymentDialog(
    settlement: LendSettlement,
    isEnglish: Boolean,
    preview: RepaymentEditPlan?,
    errorCode: String?,
    onPreview: (amountPaisa: Long, settledAt: Long) -> Unit,
    onConfirm: (amountPaisa: Long, settledAt: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val isReadOnlyAmount = settlement.amountPaisa % 100L != 0L
    var amountInput by remember {
        mutableStateOf((settlement.amountPaisa / 100L).toString())
    }
    var dateMs by remember { mutableStateOf(settlement.settledAt) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amountRs = amountInput.toLongOrNull() ?: 0L
    val amountPaisa = if (isReadOnlyAmount) settlement.amountPaisa else amountRs * 100L

    LaunchedEffect(amountRs, dateMs) {
        if (amountPaisa > 0L) {
            onPreview(amountPaisa, dateMs)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { pickedDayMs ->
                        dateMs = pickedDayMs + (settlement.settledAt % 86_400_000L)
                    }
                    showDatePicker = false
                }) {
                    Text(if (isEnglish) "Select Date" else "तारीख चुनें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(if (isEnglish) "Cancel" else "रद्द करें")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MandiSurface,
        title = {
            Text(
                text = if (isEnglish) "Edit loan repayment" else "उधार वापसी सुधारें",
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isReadOnlyAmount) {
                    Text(
                        text = if (isEnglish) {
                            "This payment has paise (₹${MandiMathEngine.paisaToRupeesString(settlement.amountPaisa)}) and cannot change amount. Only date can be edited."
                        } else {
                            "इस वापसी में पैसे हैं (₹${MandiMathEngine.paisaToRupeesString(settlement.amountPaisa)}) इसलिए राशि नहीं बदली जा सकती। सिर्फ तारीख बदलें।"
                        },
                        fontSize = 12.sp,
                        color = MandiTextSecondary
                    )
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { v ->
                        if (!isReadOnlyAmount) {
                            amountInput = v.filter { it.isDigit() }.take(9)
                        }
                    },
                    readOnly = isReadOnlyAmount,
                    label = { Text(if (isEnglish) "Amount received (₹)" else "प्राप्त राशि (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date row
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MandiSurfaceElevated,
                    border = BorderStroke(1.dp, MandiBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isEnglish) "Payment date" else "वापसी की तारीख",
                                fontSize = 11.sp,
                                color = MandiTextSecondary
                            )
                            Text(
                                text = formatEpochDate(dateMs),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MandiTextPrimary
                            )
                        }
                        Text(
                            text = if (isEnglish) "Change" else "बदलें",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiBtnSuccessBg
                        )
                    }
                }

                // Error text
                if (errorCode != null) {
                    val errText = when (errorCode) {
                        "OVER" -> if (isEnglish) {
                            "This change makes a later payment more than the loan balance"
                        } else {
                            "इस बदलाव से बाद की वापसी बकाया मूल से ज़्यादा हो जाती है"
                        }
                        else -> if (isEnglish) {
                            "Check the amount and date"
                        } else {
                            "राशि और तारीख जाँचें"
                        }
                    }
                    Text(
                        text = errText,
                        fontSize = 12.sp,
                        color = MandiAmberDark,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Preview section
                if (preview != null && errorCode == null) {
                    if (preview.gallaDeltaPaisa != 0L) {
                        val deltaRs = preview.gallaDeltaPaisa / 100L
                        val sign = if (deltaRs > 0) "+₹" else "-₹"
                        val deltaFormatted = "$sign${MandiMathEngine.formatIndianGrouping(abs(deltaRs))}"
                        val gallaText = if (isEnglish) {
                            "Galla cash will change by $deltaFormatted"
                        } else {
                            "गल्ला नकद $deltaFormatted बदलेगा"
                        }
                        Text(
                            text = gallaText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MandiTextPrimary
                        )
                    }

                    val laterChanges = preview.changes.filter { it.settlementId != settlement.id }
                    if (laterChanges.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            laterChanges.forEach { ch ->
                                val dateStr = formatEpochDate(ch.newSettledAt)
                                val oldIntRs = MandiMathEngine.formatIndianGrouping(ch.oldInterestPaisa / 100L)
                                val newIntRs = MandiMathEngine.formatIndianGrouping(ch.newInterestPaisa / 100L)
                                val chText = if (isEnglish) {
                                    "$dateStr: interest ₹$oldIntRs to ₹$newIntRs"
                                } else {
                                    "$dateStr: ब्याज ₹$oldIntRs से ₹$newIntRs"
                                }
                                Text(
                                    text = chText,
                                    fontSize = 12.sp,
                                    color = MandiTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amountPaisa, dateMs) },
                enabled = preview != null && preview.changes.isNotEmpty() && errorCode == null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MandiBtnSuccessBg,
                    contentColor = MandiBtnSuccessFg
                )
            ) {
                Text(
                    text = if (isEnglish) "Save" else "दर्ज करें",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isEnglish) "Cancel" else "रद्द करें",
                    color = MandiTextSecondary
                )
            }
        }
    )
}
