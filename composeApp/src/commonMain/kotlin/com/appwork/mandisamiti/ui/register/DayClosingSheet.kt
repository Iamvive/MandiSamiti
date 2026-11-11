package com.appwork.mandisamiti.ui.register

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiCurrencyFormatter
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.ui.theme.*

/** Day-closing bottom sheet. Input state is owned by the screen (it is keyed on the selected date). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DayClosingSheet(
    state: DailyRegisterUiState,
    isEnglish: Boolean,
    cashInput: String,
    onCashInputChange: (String) -> Unit,
    reason: String,
    onReasonChange: (String) -> Unit,
    useDenominations: Boolean,
    denominationQuantities: Map<Int, String>,
    onToggleDenominations: () -> Unit,
    onDenominationChange: (Int, String) -> Unit,
    onDismiss: () -> Unit,
    onSaveCount: (Long) -> Unit,
    onCloseDay: () -> Unit,
    onClearCount: () -> Unit,
) {
    val countedRs = parseDrawerRupees(cashInput)
    val diffPaisa = countedRs?.let { it * 100L - state.inHandCashDrawerPaisa }
    var detailsOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MandiSurface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().testTag("day-closing-sheet")
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding().imePadding()
                .padding(horizontal = 20.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (isEnglish) "Close day" else "दिन बंद करें", color = MandiTextPrimary,
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(drawerChipDate(state.selectedDate, isEnglish, state.isToday), color = MandiTextSecondary, fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = if (isEnglish) "Close" else "बंद करें", tint = MandiTextSecondary)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isEnglish) "Expected cash (per bahi)" else "बही के अनुसार नकद", color = MandiTextSecondary, fontSize = 13.sp)
                Text(MandiCurrencyFormatter.formatRupees(state.inHandCashDrawerPaisa), color = MandiTextPrimary,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = cashInput,
                onValueChange = onCashInputChange,
                readOnly = useDenominations,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = cashInput.isNotEmpty() && countedRs == null,
                label = { Text(if (isEnglish) "Counted cash ₹" else "गिनी हुई नकदी ₹") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onToggleDenominations) {
                Text(
                    if (useDenominations) {
                        if (isEnglish) "Enter total instead" else "सीधे कुल रकम भरें"
                    } else if (isEnglish) "Count notes and coins" else "नोट-सिक्के से गिनें",
                    color = MandiTextPrimary,
                )
            }
            if (useDenominations) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    drawerDenominations.forEach { denomination ->
                        val countStr = denominationQuantities[denomination].orEmpty()
                        val count = countStr.trim().toLongOrNull() ?: 0L
                        val subtotal = count * denomination

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MandiSurfaceElevated),
                            border = BorderStroke(1.dp, MandiBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (denomination <= 5) "₹$denomination (${if (isEnglish) "Coins" else "सिक्के"})" else "₹$denomination",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MandiTextPrimary,
                                    modifier = Modifier.width(76.dp)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val newCount = (count - 1).coerceAtLeast(0L)
                                            onDenominationChange(denomination, if (newCount > 0) newCount.toString() else "")
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "−", tint = MandiTextPrimary)
                                    }

                                    OutlinedTextField(
                                        value = countStr,
                                        onValueChange = { onDenominationChange(denomination, it) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(64.dp).heightIn(min = 40.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MandiTextPrimary
                                        )
                                    )

                                    IconButton(
                                        onClick = {
                                            val newCount = count + 1
                                            onDenominationChange(denomination, newCount.toString())
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "+", tint = MandiTextPrimary)
                                    }
                                }

                                Text(
                                    text = "₹${MandiMathEngine.formatIndianGrouping(subtotal)}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (subtotal > 0) MandiTextPrimary else MandiTextSecondary,
                                    modifier = Modifier.width(72.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                        }
                    }
                }
            }

            if (diffPaisa != null) {
                TarajuBalanceScale(diffPaisa = diffPaisa, isEnglish = isEnglish, modifier = Modifier.fillMaxWidth())
            }
            if (diffPaisa != null && diffPaisa != 0L) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text(if (isEnglish) "Reason (optional)" else "कारण (वैकल्पिक)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Surface(
                onClick = { detailsOpen = !detailsOpen },
                color = MandiSurfaceElevated,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("day-closing-details"),
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isEnglish) "Details" else "विवरण", color = MandiTextPrimary, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f))
                        Icon(if (detailsOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null, tint = MandiTextPrimary)
                    }
                    if (detailsOpen) {
                        listOf(
                            (if (isEnglish) "Opening" else "पिछला शेष") to "₹${com.appwork.mandisamiti.domain.math.MandiMathEngine.paisaToRupeesString(state.openingCashPaisa)}",
                            (if (isEnglish) "Cash in (+)" else "नकद आवक (+)") to "₹${com.appwork.mandisamiti.domain.math.MandiMathEngine.paisaToRupeesString(state.todayCashInPaisa)}",
                            (if (isEnglish) "Cash out (−)" else "नकद निकासी (−)") to "₹${com.appwork.mandisamiti.domain.math.MandiMathEngine.paisaToRupeesString(state.todayCashOutPaisa)}",
                        ).forEach { (label, value) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, color = MandiTextSecondary, fontSize = 13.sp)
                                Text(value, color = MandiTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        state.bahiDayPage?.takeIf { !it.isBalanced }?.let { page ->
                            Text(if (isEnglish) "Likely causes" else "संभावित कारण", color = MandiTextPrimary,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            val causes = page.likelyCauses.ifEmpty {
                                listOf(
                                    com.appwork.mandisamiti.domain.bahi.BahiLikelyCause("amount", if (isEnglish) "Amount differs from the party's usual" else "राशि पार्टी के सामान्य हिसाब से अलग"),
                                    com.appwork.mandisamiti.domain.bahi.BahiLikelyCause("late", if (isEnglish) "Entry saved after closing" else "दिन बंद होने के बाद प्रविष्टि"),
                                )
                            }
                            causes.take(3).forEach { cause ->
                                Text("• ${cause.label}", fontSize = 12.sp, color = MandiTextSecondary)
                            }
                        }
                        if (state.physicalCountPaisa != null) {
                            TextButton(onClick = onClearCount) {
                                Text(if (isEnglish) "Remove count" else "गिनती हटाएं", color = MandiTextPrimary)
                            }
                        }
                    }
                }
            }

            state.countError?.let {
                Text(if (isEnglish) "Could not save count. Please retry." else "गिनती सहेजी नहीं गई। फिर कोशिश करें।", color = MandiTextPrimary)
            }

            Button(
                onClick = onCloseDay,
                enabled = countedRs != null && !state.isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MandiBtnSuccessBg, contentColor = MandiBtnSuccessFg),
            ) {
                Text(if (isEnglish) "Close day" else "दिन बंद करें", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { countedRs?.let(onSaveCount) },
                enabled = countedRs != null && !state.isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(if (isEnglish) "Save count · keep day open" else "गिनती सहेजें · दिन खुला रखें", color = MandiTextPrimary,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
