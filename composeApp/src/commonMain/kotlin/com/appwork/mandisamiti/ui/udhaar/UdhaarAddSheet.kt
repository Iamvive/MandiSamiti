package com.appwork.mandisamiti.ui.udhaar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.ui.lend.LendPartyResolver
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiBorderActive
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
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private object UdhaarAddFormStateSaver : Saver<UdhaarAddFormState, List<Any?>> {
    override fun SaverScope.save(value: UdhaarAddFormState): List<Any?> = listOf(
        value.direction.name,
        value.todayMs,
        value.phone,
        value.name,
        value.selectedPartyId,
        value.amountRupees,
        value.dateMs,
        value.interestOn,
        value.rateInput,
        value.giveToOther,
        value.receiverName,
        value.receiverPhone,
    )

    override fun restore(value: List<Any?>): UdhaarAddFormState {
        val direction = LendDirection.valueOf(value[0] as String)
        val todayMs = value[1] as Long
        val state = UdhaarAddFormState(direction, todayMs)
        state.phone = value[2] as String
        state.name = value[3] as String
        state.selectedPartyId = value[4] as? String
        state.amountRupees = value[5] as String
        state.dateMs = value[6] as Long
        state.interestOn = value[7] as Boolean
        state.rateInput = value[8] as String
        state.giveToOther = value[9] as Boolean
        state.receiverName = value[10] as String
        state.receiverPhone = value[11] as String
        return state
    }
}

private fun formatDisplayDate(epochMs: Long): String {
    val date = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthNumber.toString().padStart(2, '0')}/${date.year}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdhaarAddSheet(
    direction: LendDirection,
    parties: List<Party>,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onSave: (formState: UdhaarAddFormState, onComplete: (Result<Unit>) -> Unit) -> Unit,
) {
    val state = rememberSaveable(direction, saver = UdhaarAddFormStateSaver) {
        UdhaarAddFormState(
            direction = direction,
            todayMs = Clock.System.now().toEpochMilliseconds()
        )
    }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val imeBottomDp = with(LocalDensity.current) {
        WindowInsets.ime.getBottom(this).toDp()
    }

    // Exact phone match lookup
    val matchingParty = remember(state.phone, parties) {
        if (state.phone.length == 10) {
            LendPartyResolver.existing(parties, state.selectedPartyId, state.phone)
        } else null
    }

    // Auto-fill name if blank when phone matches 10 digits
    LaunchedEffect(matchingParty) {
        if (matchingParty != null) {
            state.selectedPartyId = matchingParty.id
            if (state.name.isBlank()) {
                state.name = matchingParty.name
            }
        }
    }

    val willRename = matchingParty != null && state.name.trim().isNotBlank() && state.name.trim() != matchingParty.name

    val suggestions = remember(state.phone, state.name, parties) {
        if (matchingParty == null && (state.phone.length in 3..9 || (state.name.length >= 2 && state.selectedPartyId == null))) {
            LendPartyResolver.matches(parties, state.phone, state.name).take(3)
        } else emptyList()
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MandiSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MandiBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .then(if (imeBottomDp > 0.dp) Modifier.padding(bottom = imeBottomDp) else Modifier.navigationBarsPadding())
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (direction == LendDirection.GIVEN) {
                            if (isEnglish) "Record Udhaar Given" else "उधार दिया दर्ज करें"
                        } else {
                            if (isEnglish) "Record Udhaar Taken" else "उधार लिया दर्ज करें"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                    Text(
                        text = if (direction == LendDirection.GIVEN) {
                            if (isEnglish) "Money given to borrower" else "पैसा उधार दिया (लेना है)"
                        } else {
                            if (isEnglish) "Money taken from lender" else "पैसा उधार लिया (देना है)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MandiTextSecondary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    enabled = !isSaving
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MandiTextSecondary)
                }
            }

            // 1. Mobile Number Field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = {
                        state.updatePhone(it)
                        errorMessage = null
                    },
                    label = { Text(if (isEnglish) "Mobile number *" else "मोबाइल नंबर *") },
                    placeholder = { Text(if (isEnglish) "10-digit mobile" else "10 अंकों का मोबाइल नंबर") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (matchingParty != null) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Contact found",
                                tint = MandiGreenReceivable
                            )
                        }
                    },
                    isError = state.phone.isNotBlank() && !state.isPhoneValid,
                    supportingText = if (state.phone.isNotBlank() && !state.isPhoneValid) {
                        { Text(if (isEnglish) "Must be exactly 10 digits" else "पूरे 10 अंक दर्ज करें", color = MandiRedPayable) }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MandiBorderActive,
                        unfocusedBorderColor = MandiBorder,
                        focusedTextColor = MandiTextPrimary,
                        unfocusedTextColor = MandiTextPrimary
                    )
                )

                // Suggestions chips
                if (suggestions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { party ->
                            SuggestionChip(
                                onClick = {
                                    state.selectedPartyId = party.id
                                    state.name = party.name
                                    state.updatePhone(party.phone.orEmpty())
                                },
                                label = {
                                    Text(
                                        text = "${party.name} (${party.phone.orEmpty()})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MandiTextPrimary
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 2. Name Field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = {
                        state.name = it
                        errorMessage = null
                    },
                    label = { Text(if (isEnglish) "Name *" else "नाम *") },
                    placeholder = { Text(if (isEnglish) "Person name" else "व्यक्ति का नाम") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.name.isNotBlank() && !state.isNameValid,
                    supportingText = if (willRename) {
                        {
                            Text(
                                text = if (isEnglish) "Name will be updated for saved contact" else "सुरक्षित संपर्क का नाम बदल जाएगा",
                                color = MandiAmberDark,
                                fontSize = 11.sp
                            )
                        }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MandiBorderActive,
                        unfocusedBorderColor = MandiBorder,
                        focusedTextColor = MandiTextPrimary,
                        unfocusedTextColor = MandiTextPrimary
                    )
                )
            }

            // 3. Amount Field
            OutlinedTextField(
                value = state.amountRupees,
                onValueChange = { input ->
                    val digitsOnly = input.filter { it.isDigit() }
                    if (digitsOnly.length <= 8) {
                        state.amountRupees = digitsOnly
                        errorMessage = null
                    }
                },
                label = { Text(if (isEnglish) "Amount (₹) *" else "उधार राशि (₹) *") },
                placeholder = { Text("0") },
                prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MandiTextPrimary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = state.amountRupees.isNotBlank() && !state.isAmountValid,
                supportingText = {
                    val parsed = state.parsedAmountRupees
                    if (parsed != null && parsed > 0L) {
                        Text(
                            text = MandiMathEngine.formatRupees(parsed * 100L),
                            style = MaterialTheme.typography.bodySmall,
                            color = MandiGreenReceivable
                        )
                    } else if (state.amountRupees.isNotBlank() && !state.isAmountValid) {
                        Text(
                            text = state.validationReason(isEnglish) ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MandiRedPayable
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MandiBorderActive,
                    unfocusedBorderColor = MandiBorder,
                    focusedTextColor = MandiTextPrimary,
                    unfocusedTextColor = MandiTextPrimary
                )
            )

            // 4. Date Row
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MandiSurfaceElevated,
                border = BorderStroke(1.dp, MandiBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = MandiTextSecondary)
                        Column {
                            Text(
                                text = if (isEnglish) "Transaction Date" else "लेनदेन की तारीख",
                                style = MaterialTheme.typography.bodySmall,
                                color = MandiTextSecondary
                            )
                            Text(
                                text = formatDisplayDate(state.dateMs),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MandiTextPrimary
                            )
                        }
                    }
                    Text(
                        text = if (isEnglish) "Change" else "बदलें",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MandiGreenReceivable
                    )
                }
            }

            // 5. Interest Toggle Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MandiSurfaceElevated,
                border = BorderStroke(1.dp, MandiBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isEnglish) "Add Interest" else "ब्याज जोड़ें",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MandiTextPrimary
                            )
                            Text(
                                text = if (isEnglish) "Monthly rate calculation" else "प्रति माह ब्याज दर लगाएं",
                                style = MaterialTheme.typography.bodySmall,
                                color = MandiTextSecondary
                            )
                        }
                        Switch(
                            checked = state.interestOn,
                            onCheckedChange = { state.interestOn = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MandiGreenReceivable,
                                uncheckedThumbColor = MandiTextSecondary,
                                uncheckedTrackColor = MandiSurface
                            )
                        )
                    }

                    if (state.interestOn) {
                        OutlinedTextField(
                            value = state.rateInput,
                            onValueChange = {
                                state.rateInput = it
                                errorMessage = null
                            },
                            label = { Text(if (isEnglish) "Monthly Rate (%)" else "मासिक ब्याज दर (%)") },
                            placeholder = { Text("2") },
                            suffix = { Text(if (isEnglish) "% / mo" else "% / माह") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = state.parsedRate == null,
                            supportingText = if (state.parsedRate == null) {
                                { Text(if (isEnglish) "Enter valid rate (above 0 up to 100)" else "मान्य दर दर्ज करें (0 से अधिक और 100 तक)", color = MandiRedPayable) }
                            } else null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MandiBorderActive,
                                unfocusedBorderColor = MandiBorder,
                                focusedTextColor = MandiTextPrimary,
                                unfocusedTextColor = MandiTextPrimary
                            )
                        )
                    }
                }
            }

            // 6. Give to Other Card (दिया tab only)
            if (state.direction == LendDirection.GIVEN) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MandiSurfaceElevated,
                    border = BorderStroke(1.dp, MandiBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isEnglish) "Given to someone else" else "किसी और को दिया",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MandiTextPrimary
                                )
                                Text(
                                    text = if (isEnglish) "Physical cash receiver" else "पैसा किसी अन्य व्यक्ति को दिया",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MandiTextSecondary
                                )
                            }
                            Switch(
                                checked = state.giveToOther,
                                onCheckedChange = { state.giveToOther = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MandiGreenReceivable,
                                    uncheckedThumbColor = MandiTextSecondary,
                                    uncheckedTrackColor = MandiSurface
                                )
                            )
                        }

                        if (state.giveToOther) {
                            OutlinedTextField(
                                value = state.receiverName,
                                onValueChange = {
                                    state.receiverName = it
                                    errorMessage = null
                                },
                                label = { Text(if (isEnglish) "Recipient name *" else "पैसा लेने वाले का नाम *") },
                                placeholder = { Text(if (isEnglish) "Name" else "नाम") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                isError = state.receiverName.trim().isBlank(),
                                supportingText = if (state.receiverName.trim().isBlank()) {
                                    { Text(if (isEnglish) "Recipient name is required" else "प्राप्तकर्ता का नाम अनिवार्य है", color = MandiRedPayable) }
                                } else null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MandiBorderActive,
                                    unfocusedBorderColor = MandiBorder,
                                    focusedTextColor = MandiTextPrimary,
                                    unfocusedTextColor = MandiTextPrimary
                                )
                            )

                            OutlinedTextField(
                                value = state.receiverPhone,
                                onValueChange = {
                                    state.updateReceiverPhone(it)
                                    errorMessage = null
                                },
                                label = { Text(if (isEnglish) "Recipient mobile (optional)" else "प्राप्तकर्ता का मोबाइल (वैकल्पिक)") },
                                placeholder = { Text(if (isEnglish) "10-digit mobile" else "10 अंकों का मोबाइल") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                isError = state.receiverPhone.isNotBlank() && state.receiverPhone.length != 10,
                                supportingText = if (state.receiverPhone.isNotBlank() && state.receiverPhone.length != 10) {
                                    { Text(if (isEnglish) "Must be 10 digits" else "10 अंक होने चाहिए", color = MandiRedPayable) }
                                } else null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MandiBorderActive,
                                    unfocusedBorderColor = MandiBorder,
                                    focusedTextColor = MandiTextPrimary,
                                    unfocusedTextColor = MandiTextPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Error display
            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MandiRedPayable,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // 7. Save Button
            Button(
                onClick = {
                    if (!state.isValid) {
                        errorMessage = state.validationReason(isEnglish)
                        return@Button
                    }
                    errorMessage = null
                    isSaving = true
                    onSave(state) { result ->
                        isSaving = false
                        result.onFailure { e ->
                            errorMessage = when (e) {
                                is com.appwork.mandisamiti.domain.model.KhataPartyPhoneException -> {
                                    if (isEnglish) "This number belongs to a Khata account" else "यह नंबर खाता (Khata) अकाउंट का है"
                                }
                                else -> {
                                    println("Failed to save udhaar loan: ${e.message}")
                                    if (isEnglish) "Failed to save loan. Please try again." else "उधार सेव करने में विफल। कृपया पुनः प्रयास करें।"
                                }
                            }
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MandiPrimaryAction,
                    contentColor = MandiPrimaryActionText,
                    disabledContainerColor = MandiBorder,
                    disabledContentColor = MandiTextMuted
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MandiPrimaryActionText,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isEnglish) "Save" else "सेव करें",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localInstantToPickerUtc(state.dateMs),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    isSameOrPastDay(normalisePickerDate(utcTimeMillis), state.todayMs)
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { pickedDayMs ->
                        state.dateMs = normalisePickerDate(pickedDayMs)
                    }
                    showDatePicker = false
                }) {
                    Text(if (isEnglish) "Select date" else "तारीख चुनें")
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
}
