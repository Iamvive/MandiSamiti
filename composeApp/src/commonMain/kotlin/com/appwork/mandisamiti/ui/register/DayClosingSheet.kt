package com.appwork.mandisamiti.ui.register

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiCurrencyFormatter
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.ui.components.MandiKeyboard
import com.appwork.mandisamiti.ui.theme.*

/**
 * Close Day Sheet (Gaddi counter & Direct total).
 * Conforms to specs/2026-11-27-close-day-gaddi-counter-design.md
 */
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
    draft: GaddiDraft,
    onToggleDenominations: () -> Unit,
    onDraftChange: (GaddiDraft) -> Unit,
    onDismiss: () -> Unit,
    onSaveCount: (Long) -> Unit,
    onCloseDay: () -> Unit,
    onClearCount: () -> Unit,
) {
    val countedRs = parseDrawerRupees(cashInput)
    val diffPaisa = countedRs?.let { it * 100L - state.inHandCashDrawerPaisa }
    val keyboardController = LocalSoftwareKeyboardController.current

    var detailsOpen by remember { mutableStateOf(false) }
    var isReasonPickerOpen by remember { mutableStateOf(false) }

    // Close reason picker if count or mode changes
    LaunchedEffect(cashInput, useDenominations) {
        isReasonPickerOpen = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MandiSurface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        val imeBottomDp = com.appwork.mandisamiti.platform.rememberImeOrKeyboardHeight()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (imeBottomDp > 0.dp) Modifier.fillMaxHeight() else Modifier)
                .testTag("day-closing-sheet")
        ) {
            // 1. Header Bar (48 dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = if (isEnglish) "Close day" else "दिन बंद करें",
                        color = MandiTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = drawerChipDate(state.selectedDate, isEnglish, state.isToday),
                        color = MandiTextSecondary,
                        fontSize = 11.sp,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = if (isEnglish) "Close" else "बंद करें",
                        tint = MandiTextSecondary,
                    )
                }
            }

            // 2. Difference Hero (about 90 dp, polite live region)
            DifferenceHero(
                diffPaisa = diffPaisa,
                countedRs = countedRs,
                expectedPaisa = state.inHandCashDrawerPaisa,
                isEnglish = isEnglish,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // 3. Mode Switch (40 dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MandiSurfaceElevated)
                    .border(1.dp, MandiBorder, RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Denominations Tab
                val denomActive = useDenominations
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (denomActive) MandiPrimaryAction else Color.Transparent)
                        .clickable(enabled = !denomActive) {
                            keyboardController?.hide()
                            onToggleDenominations()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isEnglish) "Count notes and coins" else "नोट-सिक्के से गिनें",
                        fontSize = 12.sp,
                        fontWeight = if (denomActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (denomActive) MandiPrimaryActionText else MandiTextSecondary,
                    )
                }

                // Direct Total Tab
                val directActive = !useDenominations
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (directActive) MandiPrimaryAction else Color.Transparent)
                        .clickable(enabled = !directActive) {
                            onToggleDenominations()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isEnglish) "Enter total instead" else "सीधे कुल रकम भरें",
                        fontSize = 12.sp,
                        fontWeight = if (directActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (directActive) MandiPrimaryActionText else MandiTextSecondary,
                    )
                }
            }

            // 4. Middle Scrollable Area
            Column(
                modifier = Modifier
                    .then(if (imeBottomDp > 0.dp) Modifier.weight(1f) else Modifier.weight(1f, fill = false))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (useDenominations) {
                    DayClosingGaddiCounter(
                        draft = draft,
                        onDraftChange = { newDraft ->
                            isReasonPickerOpen = false
                            onDraftChange(newDraft)
                        },
                        isEnglish = isEnglish,
                    )
                } else {
                    // Direct Total Input Mode
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Counted cash ₹" else "गिनी हुई नकदी ₹",
                            fontSize = 12.sp,
                            color = MandiTextSecondary,
                        )
                        OutlinedTextField(
                            value = cashInput,
                            onValueChange = onCashInputChange,
                            keyboardOptions = MandiKeyboard.Whole,
                            isError = cashInput.isNotEmpty() && countedRs == null,
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MandiTextPrimary) },
                            label = { Text(if (isEnglish) "Counted cash ₹" else "गिनी हुई नकदी ₹") },
                            placeholder = { Text("0") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MandiBorderActive,
                                unfocusedBorderColor = MandiBorder,
                                focusedTextColor = MandiTextPrimary,
                                unfocusedTextColor = MandiTextPrimary,
                            ),
                        )
                    }
                }

                // Collapsible Day Details Accordion
                Surface(
                    onClick = { detailsOpen = !detailsOpen },
                    color = MandiSurfaceElevated,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, MandiBorder),
                    modifier = Modifier.fillMaxWidth().testTag("day-closing-details"),
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 26.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (isEnglish) "Details" else "विवरण",
                                color = MandiTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                if (detailsOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MandiTextPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        if (detailsOpen) {
                            listOf(
                                (if (isEnglish) "Opening" else "पिछला शेष") to "₹${MandiMathEngine.paisaToRupeesString(state.openingCashPaisa)}",
                                (if (isEnglish) "Cash in (+)" else "नकद आवक (+)") to "₹${MandiMathEngine.paisaToRupeesString(state.todayCashInPaisa)}",
                                (if (isEnglish) "Cash out (−)" else "नकद निकासी (−)") to "₹${MandiMathEngine.paisaToRupeesString(state.todayCashOutPaisa)}",
                                (if (isEnglish) "Mohari buy (Asal)" else "मोहरी खरीद (असल)") to "₹${MandiMathEngine.paisaToRupeesString(state.bahiDayPage?.mohariBuyAsalPaisa ?: state.bahiDayPage?.khaateBuyAsalPaisa ?: 0L)}",
                                (if (isEnglish) "Mohari sell (Asal)" else "मोहरी बिक्री (असल)") to "₹${MandiMathEngine.paisaToRupeesString(state.bahiDayPage?.khaateSellAsalPaisa ?: 0L)}",
                            ).forEach { (label, value) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(label, color = MandiTextSecondary, fontSize = 11.sp)
                                    Text(value, color = MandiTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (state.physicalCountPaisa != null) {
                                TextButton(onClick = onClearCount, contentPadding = PaddingValues(0.dp)) {
                                    Text(if (isEnglish) "Remove count" else "गिनती हटाएं", color = MandiRedPayable, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Fixed Number Pad (counter mode only, hidden when reason picker is open)
            if (useDenominations && !isReasonPickerOpen) {
                GaddiNumberPad(
                    draft = draft,
                    onDraftChange = { newDraft ->
                        isReasonPickerOpen = false
                        onDraftChange(newDraft)
                    },
                    isEnglish = isEnglish,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            // 6. Sticky Bottom Action Dock & Reason Picker
            Surface(
                color = MandiSurface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, MandiBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .then(if (imeBottomDp == 0.dp) Modifier.navigationBarsPadding() else Modifier),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Reason Picker (shown when difference exists and user tapped close button)
                    if (isReasonPickerOpen && diffPaisa != null && diffPaisa != 0L) {
                        ReasonPickerSection(
                            diffPaisa = diffPaisa,
                            reason = reason,
                            onReasonChange = onReasonChange,
                            isEnglish = isEnglish,
                        )
                    }

                    // Save count error text
                    if (state.countError != null) {
                        Text(
                            text = if (isEnglish) "Could not save count. Please retry." else "गिनती सहेजी नहीं गई। फिर कोशिश करें।",
                            color = MandiRedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Secondary CTA: Save count
                        OutlinedButton(
                            onClick = { countedRs?.let(onSaveCount) },
                            enabled = countedRs != null && !state.isSaving,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MandiBorder),
                        ) {
                            Text(
                                text = if (isEnglish) "Save count" else "गिनती सहेजें",
                                color = if (countedRs != null && !state.isSaving) MandiTextPrimary else MandiTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }

                        // Primary CTA: Close Day
                        val isDiff = diffPaisa != null && diffPaisa != 0L
                        val isMatch = diffPaisa == 0L

                        val closeButtonLabel = when {
                            countedRs == null -> if (isEnglish) "Count the cash first" else "पहले नकदी गिनें"
                            isMatch -> if (isEnglish) "Close day" else "दिन बंद करें"
                            diffPaisa != null && !isReasonPickerOpen -> {
                                val absDiff = if (diffPaisa < 0) -diffPaisa else diffPaisa
                                val diffFmt = MandiCurrencyFormatter.formatRupees(absDiff)
                                if (diffPaisa < 0) {
                                    if (isEnglish) "Close with $diffFmt short" else "$diffFmt कमी के साथ बंद करें"
                                } else {
                                    if (isEnglish) "Close with $diffFmt over" else "$diffFmt बढ़त के साथ बंद करें"
                                }
                            }
                            reason.isBlank() -> if (isEnglish) "Choose a reason" else "कारण चुनें"
                            diffPaisa != null -> {
                                val absDiff = if (diffPaisa < 0) -diffPaisa else diffPaisa
                                val diffFmt = MandiCurrencyFormatter.formatRupees(absDiff)
                                if (diffPaisa < 0) {
                                    if (isEnglish) "Close with $diffFmt short" else "$diffFmt कमी के साथ बंद करें"
                                } else {
                                    if (isEnglish) "Close with $diffFmt over" else "$diffFmt बढ़त के साथ बंद करें"
                                }
                            }
                            else -> if (isEnglish) "Close day" else "दिन बंद करें"
                        }

                        val closeButtonEnabled = when {
                            countedRs == null || state.isSaving -> false
                            isMatch -> true
                            !isReasonPickerOpen -> true
                            else -> reason.isNotBlank()
                        }

                        Button(
                            onClick = {
                                if (isMatch) {
                                    onCloseDay()
                                } else if (!isReasonPickerOpen) {
                                    isReasonPickerOpen = true
                                } else if (reason.isNotBlank()) {
                                    onCloseDay()
                                }
                            },
                            enabled = closeButtonEnabled,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MandiBtnSuccessBg,
                                contentColor = MandiBtnSuccessFg,
                                disabledContainerColor = MandiSurfaceElevated,
                                disabledContentColor = MandiTextSecondary,
                            ),
                        ) {
                            Text(
                                text = closeButtonLabel,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
            }

            if (imeBottomDp > 0.dp) {
                Spacer(modifier = Modifier.height(imeBottomDp))
            }
        }
    }
}

@Composable
private fun DifferenceHero(
    diffPaisa: Long?,
    countedRs: Long?,
    expectedPaisa: Long,
    isEnglish: Boolean,
    modifier: Modifier = Modifier,
) {
    val isNotCounted = countedRs == null
    val isMatch = diffPaisa == 0L
    val isShort = diffPaisa != null && diffPaisa < 0L
    val isSurplus = diffPaisa != null && diffPaisa > 0L

    val containerBg = when {
        isNotCounted -> MandiSurfaceElevated
        isMatch -> MandiGreenLight
        else -> MandiAmberLight
    }
    val borderColor = when {
        isNotCounted -> MandiBorder
        isMatch -> MandiGreenBorder
        else -> MandiAmberBorder
    }
    val contentColor = when {
        isNotCounted -> MandiTextSecondary
        isMatch -> MandiGreenText
        else -> MandiAmberDark
    }

    val verdict = when {
        isNotCounted -> if (isEnglish) "Not counted yet" else "अभी गिना नहीं"
        isShort -> {
            val amt = MandiCurrencyFormatter.formatRupees(-diffPaisa!!)
            if (isEnglish) "$amt short" else "$amt कम"
        }
        isSurplus -> {
            val amt = MandiCurrencyFormatter.formatRupees(diffPaisa!!)
            if (isEnglish) "$amt over" else "$amt ज़्यादा"
        }
        else -> if (isEnglish) "Cash matches" else "हिसाब बराबर"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = verdict
            },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Verdict Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = when {
                        isMatch -> Icons.Default.CheckCircle
                        isNotCounted -> Icons.Default.Info
                        else -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = verdict,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
            }

            // Progress bar (counted / expected, capped at 100%)
            // Hide when expected is zero or negative
            if (expectedPaisa > 0L && countedRs != null) {
                val progress = ((countedRs * 100f) / (expectedPaisa.toFloat())).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (isMatch) MandiGreenText else MandiAmberDark,
                    trackColor = MandiBorder,
                )
            }

            // Counted vs Expected Pair
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${if (isEnglish) "Counted" else "गिनी"} ${if (countedRs != null) "₹${MandiMathEngine.formatIndianGrouping(countedRs)}" else "—"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MandiTextPrimary,
                )
                Text(
                    text = "${if (isEnglish) "Expected" else "बही अनुसार"} ${MandiCurrencyFormatter.formatRupees(expectedPaisa)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = MandiTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun ReasonPickerSection(
    diffPaisa: Long,
    reason: String,
    onReasonChange: (String) -> Unit,
    isEnglish: Boolean,
) {
    val absDiff = if (diffPaisa < 0) -diffPaisa else diffPaisa
    val diffFmt = MandiCurrencyFormatter.formatRupees(absDiff)
    val chips = listOf(
        if (isEnglish) "Change shortfall" else "खुले पैसे",
        if (isEnglish) "Expense not entered" else "खर्च लिखना बाकी",
        if (isEnglish) "Payment not entered" else "भुगतान बाकी",
        if (isEnglish) "Other" else "अन्य",
    )
    val otherLabel = if (isEnglish) "Other" else "अन्य"
    var isOtherSelected by remember(reason) {
        mutableStateOf(reason !in chips.take(3) && reason.isNotBlank())
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = if (isEnglish) "Why is there a difference of $diffFmt?" else "$diffFmt का अंतर क्यों है?",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MandiTextPrimary,
        )

        // Reason chips (wrap in 2 rows or scroll)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            chips.forEach { chipLabel ->
                val isSelected = when (chipLabel) {
                    otherLabel -> isOtherSelected
                    else -> reason == chipLabel && !isOtherSelected
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) MandiPrimaryAction else MandiSurfaceElevated)
                        .border(1.dp, if (isSelected) MandiBorderActive else MandiBorder, RoundedCornerShape(6.dp))
                        .clickable {
                            if (chipLabel == otherLabel) {
                                isOtherSelected = true
                                if (reason in chips.take(3)) {
                                    onReasonChange("")
                                }
                            } else {
                                isOtherSelected = false
                                onReasonChange(chipLabel)
                            }
                        }
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = chipLabel,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MandiPrimaryActionText else MandiTextPrimary,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }

        // अन्य text field
        if (isOtherSelected) {
            OutlinedTextField(
                value = reason,
                onValueChange = onReasonChange,
                label = { Text(if (isEnglish) "Specify reason" else "कारण लिखें") },
                keyboardOptions = MandiKeyboard.Note,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MandiBorderActive,
                    unfocusedBorderColor = MandiBorder,
                    focusedTextColor = MandiTextPrimary,
                    unfocusedTextColor = MandiTextPrimary,
                ),
            )
        }
    }
}
